#import "V3ModelResourceCache.h"

#import "V3ModelResourceCacheInternal.hpp"
#import "Common/AAPLMathUtilities.h"
#include "V3VolumeRodDeformer.hpp"

#import <GLKit/GLKTextureLoader.h>
#import <OpenGLES/ES2/glext.h>
#import <QuartzCore/QuartzCore.h>
#import <os/log.h>
#import <os/signpost.h>

#include <algorithm>
#include <array>
#include <cmath>
#include <cstring>
#include <limits>
#include <map>
#include <mutex>
#include <set>
#include <sstream>
#include <utility>

#ifndef GL_COMPRESSED_RGBA_ASTC_4x4_KHR
#define GL_COMPRESSED_RGBA_ASTC_4x4_KHR 0x93B0
#define GL_COMPRESSED_RGBA_ASTC_5x4_KHR 0x93B1
#define GL_COMPRESSED_RGBA_ASTC_5x5_KHR 0x93B2
#define GL_COMPRESSED_RGBA_ASTC_6x5_KHR 0x93B3
#define GL_COMPRESSED_RGBA_ASTC_6x6_KHR 0x93B4
#define GL_COMPRESSED_RGBA_ASTC_8x5_KHR 0x93B5
#define GL_COMPRESSED_RGBA_ASTC_8x6_KHR 0x93B6
#define GL_COMPRESSED_RGBA_ASTC_8x8_KHR 0x93B7
#define GL_COMPRESSED_RGBA_ASTC_10x5_KHR 0x93B8
#define GL_COMPRESSED_RGBA_ASTC_10x6_KHR 0x93B9
#define GL_COMPRESSED_RGBA_ASTC_10x8_KHR 0x93BA
#define GL_COMPRESSED_RGBA_ASTC_10x10_KHR 0x93BB
#define GL_COMPRESSED_RGBA_ASTC_12x10_KHR 0x93BC
#define GL_COMPRESSED_RGBA_ASTC_12x12_KHR 0x93BD
#endif

NSNotificationName const V3ModelResourceCacheDidBecomeReadyNotification = @"V3ModelResourceCacheDidBecomeReady";
NSNotificationName const V3ModelResourceCacheDidFailNotification = @"V3ModelResourceCacheDidFail";
NSNotificationName const V3ModelFirstFramePresentedNotification = @"V3ModelFirstFramePresented";

namespace {

using motorica::v3::DeformationData;
using motorica::v3::DeformationKind;
using motorica::v3::ModelPart;
using motorica::v3::ModelResources;
using motorica::v3::ModelResourcesPtr;
using motorica::v3::VolumeRodDeformer;

NSString *const V3ModelErrorDomain = @"com.bailout.stickk.v3-model";
constexpr size_t kPartsBundleHeaderBytes = 48;
constexpr size_t kPartHeaderBytes = 24;
constexpr size_t kModelPartHeaderBytes = 44;
constexpr size_t kDeformationHeaderBytes = 16;
constexpr size_t kMaximumPartCount = 512;
constexpr size_t kMaximumVertexCount = 5'000'000;
constexpr size_t kMaximumIndexCount = 20'000'000;

os_log_t V3Log() {
    static os_log_t log = os_log_create("com.bailout.stickk", "V3Model");
    return log;
}

NSError *MakeError(NSInteger code, NSString *description) {
    return [NSError errorWithDomain:V3ModelErrorDomain
                               code:code
                           userInfo:@{NSLocalizedDescriptionKey: description ?: @"Unknown V3 model error"}];
}

#if DEBUG
vector_float4 TransformTestPosition(matrix_float4x4 matrix, const float *position) {
    return matrix_multiply(matrix, (vector_float4){position[0], position[1], position[2], 1.0f});
}

double TestPositionDistance(const float *position, vector_float4 expected) {
    double dx = static_cast<double>(position[0]) - expected.x;
    double dy = static_cast<double>(position[1]) - expected.y;
    double dz = static_cast<double>(position[2]) - expected.z;
    return std::sqrt(dx * dx + dy * dy + dz * dz);
}

std::array<uint32_t, 3> TestPositionKey(const float *position) {
    std::array<uint32_t, 3> result{};
    std::memcpy(&result[0], &position[0], sizeof(uint32_t));
    std::memcpy(&result[1], &position[1], sizeof(uint32_t));
    std::memcpy(&result[2], &position[2], sizeof(uint32_t));
    return result;
}

bool TestDeformLinearPart(const ModelPart &part,
                          const std::array<matrix_float4x4, motorica::v3::kInfluenceCount> &matrices,
                          std::vector<float> &target) {
    if (!part.deformation ||
        part.deformation->weights.size() != part.vertexCount() * motorica::v3::kInfluenceCount) {
        return false;
    }
    target = part.vertices;
    for (size_t vertex = 0; vertex < part.vertexCount(); ++vertex) {
        size_t vertexOffset = vertex * motorica::v3::kFloatsPerVertex;
        size_t weightOffset = vertex * motorica::v3::kInfluenceCount;
        vector_float4 source = {
            part.vertices[vertexOffset],
            part.vertices[vertexOffset + 1],
            part.vertices[vertexOffset + 2],
            1.0f,
        };
        vector_float4 position = {0.0f, 0.0f, 0.0f, 0.0f};
        for (int slot = 0; slot < motorica::v3::kInfluenceCount; ++slot) {
            float weight = part.deformation->weights[weightOffset + slot];
            if (weight > 0.0f) position += matrix_multiply(matrices[slot], source) * weight;
        }
        target[vertexOffset] = position.x;
        target[vertexOffset + 1] = position.y;
        target[vertexOffset + 2] = position.z;
    }
    return true;
}

NSDictionary<NSString *, NSNumber *> *TestDeformationDiagnostics(const ModelPart &part) {
    const DeformationData &data = *part.deformation;
    std::array<matrix_float4x4, motorica::v3::kInfluenceCount> identityMatrices;
    std::array<matrix_float4x4, motorica::v3::kInfluenceCount> movedMatrices;
    for (int slot = 0; slot < motorica::v3::kInfluenceCount; ++slot) {
        identityMatrices[slot] = matrix4x4_identity();
        movedMatrices[slot] = slot == 0
            ? matrix4x4_identity()
            : matrix4x4_translation(slot * 1.25f, slot * -0.65f, slot * 0.35f);
    }

    int topSlot = -1;
    std::vector<float> identityTarget;
    std::vector<float> movedTarget;
    bool valid = false;
    if (data.kind == DeformationKind::volumeRod) {
        VolumeRodDeformer deformer(part);
        topSlot = deformer.topInfluenceSlot();
        matrix_float4x4 finger = matrix4x4_identity();
        if (data.centerline.size() >= 6) {
            size_t end = data.centerline.size() - 3;
            vector_float3 pivot = {
                data.centerline[end],
                data.centerline[end + 1],
                data.centerline[end + 2],
            };
            matrix_float4x4 aroundEnd = matrix_multiply(
                matrix4x4_translation(pivot),
                matrix_multiply(
                    matrix4x4_rotation(20.0f * static_cast<float>(M_PI / 180.0), 0.0f, 0.0f, 1.0f),
                    matrix4x4_translation(-pivot)
                )
            );
            finger = matrix_multiply(matrix4x4_translation(3.0f, -2.0f, 1.0f), aroundEnd);
        }
        if (topSlot >= 1 && topSlot < motorica::v3::kInfluenceCount) {
            movedMatrices[topSlot] = finger;
            valid = deformer.isValid()
                && deformer.deform(part, matrix4x4_identity(), matrix4x4_identity(), identityTarget)
                && deformer.deform(part, matrix4x4_identity(), finger, movedTarget);
        }
    } else {
        valid = TestDeformLinearPart(part, identityMatrices, identityTarget)
            && TestDeformLinearPart(part, movedMatrices, movedTarget);
    }

    size_t finiteVertexCount = 0;
    size_t bottomAnchorCount = 0;
    size_t topAnchorCount = 0;
    double identityMaxPositionError = 0.0;
    double bottomAnchorMaxError = 0.0;
    double topAnchorMaxError = 0.0;
    double minimumNormalLength = std::numeric_limits<double>::infinity();
    double maximumCoincidentPositionSeparation = 0.0;
    std::map<std::array<uint32_t, 3>, vector_float3> coincidentPositions;

    if (valid) {
        for (size_t vertex = 0; vertex < part.vertexCount(); ++vertex) {
            size_t vertexOffset = vertex * motorica::v3::kFloatsPerVertex;
            size_t weightOffset = vertex * motorica::v3::kInfluenceCount;
            const float *source = &part.vertices[vertexOffset];
            const float *identity = &identityTarget[vertexOffset];
            const float *moved = &movedTarget[vertexOffset];
            bool finite = true;
            for (int component = 0; component < motorica::v3::kFloatsPerVertex; ++component) {
                finite = finite && std::isfinite(moved[component]);
            }
            if (finite) ++finiteVertexCount;

            identityMaxPositionError = std::max(
                identityMaxPositionError,
                TestPositionDistance(identity, (vector_float4){source[0], source[1], source[2], 1.0f})
            );
            double normalLength = std::sqrt(
                static_cast<double>(moved[3]) * moved[3]
                + static_cast<double>(moved[4]) * moved[4]
                + static_cast<double>(moved[5]) * moved[5]
            );
            minimumNormalLength = std::min(minimumNormalLength, normalLength);

            bool volumeRod = data.kind == DeformationKind::volumeRod;
            float progress = volumeRod && topSlot >= 1
                ? data.weights[weightOffset + topSlot]
                : 0.0f;
            bool isBottomAnchor = volumeRod
                ? progress <= 0.0001f
                : data.weights[weightOffset] >= 0.9999f;
            if (isBottomAnchor) {
                ++bottomAnchorCount;
                bottomAnchorMaxError = std::max(
                    bottomAnchorMaxError,
                    TestPositionDistance(moved, TransformTestPosition(movedMatrices[0], source))
                );
            }
            for (int slot = 1; slot < motorica::v3::kInfluenceCount; ++slot) {
                bool isTopAnchor = volumeRod
                    ? slot == topSlot && progress >= 0.9999f
                    : data.weights[weightOffset + slot] >= 0.9999f;
                if (!isTopAnchor) continue;
                ++topAnchorCount;
                topAnchorMaxError = std::max(
                    topAnchorMaxError,
                    TestPositionDistance(moved, TransformTestPosition(movedMatrices[slot], source))
                );
                break;
            }

            std::array<uint32_t, 3> key = TestPositionKey(source);
            vector_float3 movedPosition = {moved[0], moved[1], moved[2]};
            auto inserted = coincidentPositions.emplace(key, movedPosition);
            if (!inserted.second) {
                vector_float3 difference = movedPosition - inserted.first->second;
                maximumCoincidentPositionSeparation = std::max(
                    maximumCoincidentPositionSeparation,
                    static_cast<double>(simd_length(difference))
                );
            }
        }
    }

    if (!std::isfinite(minimumNormalLength)) minimumNormalLength = 0.0;
    return @{
        @"kind": @(data.kind == DeformationKind::volumeRod ? 2 : 1),
        @"valid": @(valid),
        @"vertexCount": @(part.vertexCount()),
        @"finiteVertexCount": @(finiteVertexCount),
        @"identityMaxPositionError": @(identityMaxPositionError),
        @"bottomAnchorCount": @(bottomAnchorCount),
        @"bottomAnchorMaxError": @(bottomAnchorMaxError),
        @"topAnchorCount": @(topAnchorCount),
        @"topAnchorMaxError": @(topAnchorMaxError),
        @"minimumNormalLength": @(minimumNormalLength),
        @"maximumCoincidentPositionSeparation": @(maximumCoincidentPositionSeparation),
    };
}
#endif

bool CheckedMultiply(size_t a, size_t b, size_t &result) {
    if (a != 0 && b > std::numeric_limits<size_t>::max() / a) {
        return false;
    }
    result = a * b;
    return true;
}

bool CheckedAdd(size_t a, size_t b, size_t &result) {
    if (b > std::numeric_limits<size_t>::max() - a) {
        return false;
    }
    result = a + b;
    return true;
}

class BinaryReader {
public:
    BinaryReader(NSData *data, NSString *label)
        : bytes_(static_cast<const uint8_t *>(data.bytes)), size_(data.length), label_([label copy]) {}

    bool readMagic(const char expected[4], NSError **error) {
        if (!require(4, error)) return false;
        if (std::memcmp(bytes_ + offset_, expected, 4) != 0) {
            if (error) *error = MakeError(10, [NSString stringWithFormat:@"Invalid magic in %@", label_]);
            return false;
        }
        offset_ += 4;
        return true;
    }

    bool readInt32(int32_t &value, NSError **error) {
        if (!require(sizeof(uint32_t), error)) return false;
        uint32_t raw = 0;
        std::memcpy(&raw, bytes_ + offset_, sizeof(raw));
        value = static_cast<int32_t>(CFSwapInt32LittleToHost(raw));
        offset_ += sizeof(raw);
        return true;
    }

    bool readString(size_t length, std::string &value, NSError **error) {
        if (!require(length, error)) return false;
        value.assign(reinterpret_cast<const char *>(bytes_ + offset_), length);
        offset_ += length;
        NSString *decoded = [[NSString alloc] initWithBytes:value.data()
                                                    length:value.size()
                                                  encoding:NSUTF8StringEncoding];
        if (decoded.length == 0) {
            if (error) *error = MakeError(11, [NSString stringWithFormat:@"Invalid UTF-8 part name in %@", label_]);
            return false;
        }
        return true;
    }

    bool readFloats(size_t count, std::vector<float> &values, NSError **error) {
        size_t byteCount = 0;
        if (!CheckedMultiply(count, sizeof(uint32_t), byteCount) || !require(byteCount, error)) return false;
        values.resize(count);
        for (size_t i = 0; i < count; ++i) {
            uint32_t raw = 0;
            std::memcpy(&raw, bytes_ + offset_ + i * sizeof(raw), sizeof(raw));
            raw = CFSwapInt32LittleToHost(raw);
            std::memcpy(&values[i], &raw, sizeof(raw));
            if (!std::isfinite(values[i])) {
                if (error) *error = MakeError(12, [NSString stringWithFormat:@"Non-finite float at %zu in %@", i, label_]);
                return false;
            }
        }
        offset_ += byteCount;
        return true;
    }

    bool readUInt32s(size_t count, std::vector<uint32_t> &values, NSError **error) {
        size_t byteCount = 0;
        if (!CheckedMultiply(count, sizeof(uint32_t), byteCount) || !require(byteCount, error)) return false;
        values.resize(count);
        for (size_t i = 0; i < count; ++i) {
            uint32_t raw = 0;
            std::memcpy(&raw, bytes_ + offset_ + i * sizeof(raw), sizeof(raw));
            values[i] = CFSwapInt32LittleToHost(raw);
        }
        offset_ += byteCount;
        return true;
    }

    bool readInt32s(size_t count, std::vector<int32_t> &values, NSError **error) {
        std::vector<uint32_t> raw;
        if (!readUInt32s(count, raw, error)) return false;
        values.resize(count);
        for (size_t i = 0; i < count; ++i) values[i] = static_cast<int32_t>(raw[i]);
        return true;
    }

    bool atEnd(NSError **error) const {
        if (offset_ == size_) return true;
        if (error) {
            *error = MakeError(13, [NSString stringWithFormat:@"Unexpected trailing bytes in %@: read %zu of %zu", label_, offset_, size_]);
        }
        return false;
    }

    size_t remaining() const { return size_ - offset_; }
    size_t offset() const { return offset_; }
    size_t size() const { return size_; }

private:
    bool require(size_t count, NSError **error) const {
        if (count <= size_ - offset_) return true;
        if (error) {
            *error = MakeError(14, [NSString stringWithFormat:@"Unexpected end of %@ at byte %zu (need %zu, have %zu)", label_, offset_, count, size_ - offset_]);
        }
        return false;
    }

    const uint8_t *bytes_ = nullptr;
    size_t size_ = 0;
    size_t offset_ = 0;
    NSString *label_ = nil;
};

bool ValidateCounts(int32_t vertexCount, int32_t indexCount, NSString *label, NSError **error) {
    if (vertexCount < 0 || indexCount < 0 ||
        static_cast<size_t>(vertexCount) > kMaximumVertexCount ||
        static_cast<size_t>(indexCount) > kMaximumIndexCount) {
        if (error) *error = MakeError(15, [NSString stringWithFormat:@"Invalid vertex/index counts %d/%d in %@", vertexCount, indexCount, label]);
        return false;
    }
    return true;
}

bool ValidateIndices(const std::vector<uint32_t> &indices, size_t vertexCount, NSString *label, NSError **error) {
    for (size_t i = 0; i < indices.size(); ++i) {
        if (indices[i] >= vertexCount) {
            if (error) *error = MakeError(16, [NSString stringWithFormat:@"Index %u at %zu exceeds %zu vertices in %@", indices[i], i, vertexCount, label]);
            return false;
        }
    }
    return true;
}

bool ReadModelPart(BinaryReader &reader, NSString *label, ModelPart &part, NSError **error) {
    int32_t version = 0, floatsPerVertex = 0, vertexCount = 0, indexCount = 0;
    int32_t ignored = 0;
    if (!reader.readMagic("V3MB", error) ||
        !reader.readInt32(version, error) ||
        !reader.readInt32(floatsPerVertex, error) ||
        !reader.readInt32(vertexCount, error) ||
        !reader.readInt32(indexCount, error)) return false;
    for (int i = 0; i < 6; ++i) if (!reader.readInt32(ignored, error)) return false;
    if ((version != 1 && version != 2) || floatsPerVertex != motorica::v3::kFloatsPerVertex) {
        if (error) *error = MakeError(17, [NSString stringWithFormat:@"Unsupported V3MB version/layout %d/%d in %@", version, floatsPerVertex, label]);
        return false;
    }
    if (!ValidateCounts(vertexCount, indexCount, label, error)) return false;
    size_t floatCount = 0;
    if (!CheckedMultiply(static_cast<size_t>(vertexCount), motorica::v3::kFloatsPerVertex, floatCount)) {
        if (error) *error = MakeError(18, [NSString stringWithFormat:@"Vertex data overflow in %@", label]);
        return false;
    }
    if (!reader.readFloats(floatCount, part.vertices, error) ||
        !reader.readUInt32s(static_cast<size_t>(indexCount), part.indices, error) ||
        !reader.atEnd(error)) return false;
    return ValidateIndices(part.indices, part.vertexCount(), label, error);
}

bool ReadPartsBundle(BinaryReader &reader, std::vector<ModelPart> *parts, NSError **error) {
    int32_t version = 0, floatsPerVertex = 0, partCount = 0;
    int32_t totalVertices = 0, totalIndices = 0, ignored = 0;
    if (!reader.readMagic("V3PB", error) ||
        !reader.readInt32(version, error) ||
        !reader.readInt32(floatsPerVertex, error) ||
        !reader.readInt32(partCount, error) ||
        !reader.readInt32(totalVertices, error) ||
        !reader.readInt32(totalIndices, error)) return false;
    for (int i = 0; i < 6; ++i) if (!reader.readInt32(ignored, error)) return false;
    if (version != 1 || floatsPerVertex != motorica::v3::kFloatsPerVertex ||
        partCount < 0 || static_cast<size_t>(partCount) > kMaximumPartCount ||
        !ValidateCounts(totalVertices, totalIndices, @"V3PB", error)) {
        if (error && !*error) *error = MakeError(19, [NSString stringWithFormat:@"Unsupported V3PB version/layout/counts %d/%d/%d", version, floatsPerVertex, partCount]);
        return false;
    }

    std::set<std::string> names;
    std::vector<ModelPart> parsed;
    parsed.reserve(static_cast<size_t>(partCount));
    size_t parsedVertices = 0, parsedIndices = 0;
    for (int32_t i = 0; i < partCount; ++i) {
        int32_t nameLength = 0, vertexCount = 0, indexCount = 0;
        int32_t faceCount = 0, triangleCount = 0, expandedCount = 0;
        if (!reader.readInt32(nameLength, error) ||
            !reader.readInt32(vertexCount, error) ||
            !reader.readInt32(indexCount, error) ||
            !reader.readInt32(faceCount, error) ||
            !reader.readInt32(triangleCount, error) ||
            !reader.readInt32(expandedCount, error)) return false;
        if (nameLength <= 0 || nameLength > 4096 || !ValidateCounts(vertexCount, indexCount, @"V3PB part", error)) {
            if (error && !*error) *error = MakeError(20, [NSString stringWithFormat:@"Invalid V3PB part header at %d", i]);
            return false;
        }
        ModelPart part;
        if (!reader.readString(static_cast<size_t>(nameLength), part.id, error)) return false;
        if (!names.insert(part.id).second) {
            if (error) *error = MakeError(21, [NSString stringWithFormat:@"Duplicate V3 part id %s", part.id.c_str()]);
            return false;
        }
        size_t floatCount = 0;
        if (!CheckedMultiply(static_cast<size_t>(vertexCount), motorica::v3::kFloatsPerVertex, floatCount) ||
            !reader.readFloats(floatCount, part.vertices, error) ||
            !reader.readUInt32s(static_cast<size_t>(indexCount), part.indices, error) ||
            !ValidateIndices(part.indices,
                             part.vertexCount(),
                             [NSString stringWithUTF8String:part.id.c_str()],
                             error)) return false;
        parsedVertices += part.vertexCount();
        parsedIndices += part.indices.size();
        parsed.push_back(std::move(part));
    }
    if (!reader.atEnd(error) || parsedVertices != static_cast<size_t>(totalVertices) || parsedIndices != static_cast<size_t>(totalIndices)) {
        if (error && !*error) *error = MakeError(22, [NSString stringWithFormat:@"V3PB totals mismatch: header %d/%d, parsed %zu/%zu", totalVertices, totalIndices, parsedVertices, parsedIndices]);
        return false;
    }
    if (parts) *parts = std::move(parsed);
    return true;
}

bool ReadDeformation(BinaryReader &reader,
                     NSInteger expectedVertexCount,
                     bool requiresCenterline,
                     std::shared_ptr<DeformationData> *output,
                     NSError **error) {
    int32_t version = 0, vertexCount = 0, influenceCount = 0;
    if (!reader.readMagic("V3DF", error) ||
        !reader.readInt32(version, error) ||
        !reader.readInt32(vertexCount, error) ||
        !reader.readInt32(influenceCount, error)) return false;
    if ((version < 1 || version > 3) || vertexCount != expectedVertexCount ||
        influenceCount != motorica::v3::kInfluenceCount || vertexCount < 0) {
        if (error) *error = MakeError(23, [NSString stringWithFormat:@"Unsupported V3DF version/counts %d/%d/%d (expected %ld)", version, vertexCount, influenceCount, (long)expectedVertexCount]);
        return false;
    }
    if (requiresCenterline != (version == 3)) {
        if (error) *error = MakeError(24, requiresCenterline
            ? @"Volume-rod deformation requires V3DF version 3"
            : @"V3DF version 3 can only be used by volume-rod deformation");
        return false;
    }
    auto result = std::make_shared<DeformationData>();
    size_t weightCount = 0;
    if (!CheckedMultiply(static_cast<size_t>(vertexCount), static_cast<size_t>(influenceCount), weightCount) ||
        !reader.readFloats(weightCount, result->weights, error)) return false;
    for (size_t vertex = 0; vertex < static_cast<size_t>(vertexCount); ++vertex) {
        float sum = 0.0f;
        for (int influence = 0; influence < influenceCount; ++influence) {
            float weight = result->weights[vertex * influenceCount + influence];
            if (weight < -0.0001f || weight > 1.0001f) {
                if (error) *error = MakeError(25, [NSString stringWithFormat:@"Invalid V3DF weight %.6f at vertex %zu", weight, vertex]);
                return false;
            }
            sum += weight;
        }
        // V3DF v3 stores rod progress in an influence channel. Android clamps
        // that channel directly and does not treat all six values as skin weights.
        if (version < 3 && std::fabs(sum - 1.0f) > 0.01f) {
            if (error) *error = MakeError(26, [NSString stringWithFormat:@"V3DF weights sum to %.6f at vertex %zu", sum, vertex]);
            return false;
        }
    }
    if (version >= 2) {
        if (!reader.readInt32s(static_cast<size_t>(vertexCount), result->selectionInfluences, error)) return false;
        for (int32_t influence : result->selectionInfluences) {
            if (influence < 0 || influence >= influenceCount) {
                if (error) *error = MakeError(27, [NSString stringWithFormat:@"Invalid V3DF selection influence %d", influence]);
                return false;
            }
        }
    }
    if (version == 3) {
        int32_t nodeCount = 0;
        if (!reader.readInt32(nodeCount, error)) return false;
        if (nodeCount < 5 || nodeCount > 33 ||
            !reader.readFloats(static_cast<size_t>(nodeCount) * 3, result->centerline, error)) {
            if (error && !*error) *error = MakeError(28, [NSString stringWithFormat:@"Invalid V3DF centerline node count %d", nodeCount]);
            return false;
        }
    }
    if (!reader.atEnd(error)) return false;
    if (output) *output = std::move(result);
    return true;
}

NSURL *BundleURLForAssetPath(NSString *assetPath) {
    NSString *fileName = assetPath.lastPathComponent;
    NSString *extension = fileName.pathExtension;
    NSString *baseName = fileName.stringByDeletingPathExtension;
    NSURL *url = [[NSBundle mainBundle] URLForResource:baseName withExtension:extension.length > 0 ? extension : nil];
    if (url) return url;
    return [[NSBundle mainBundle] URLForResource:fileName withExtension:nil];
}

NSData *ReadBundledAsset(NSString *assetPath, NSError **error) {
    NSURL *url = BundleURLForAssetPath(assetPath);
    if (!url) {
        if (error) *error = MakeError(30, [NSString stringWithFormat:@"Missing bundled V3 asset %@", assetPath]);
        return nil;
    }
    NSData *data = [NSData dataWithContentsOfURL:url options:NSDataReadingMappedIfSafe error:error];
    if (!data && error && !*error) *error = MakeError(31, [NSString stringWithFormat:@"Could not read V3 asset %@", assetPath]);
    return data;
}

NSString *RequiredString(NSDictionary *dictionary, NSString *key, NSString *owner, NSError **error) {
    id value = dictionary[key];
    if (![value isKindOfClass:NSString.class] || [(NSString *)value length] == 0) {
        if (error) *error = MakeError(32, [NSString stringWithFormat:@"Missing %@.%@", owner, key]);
        return nil;
    }
    return value;
}

int InfluenceIndexForFinger(NSString *finger) {
    if ([finger isEqualToString:@"index"]) return 1;
    if ([finger isEqualToString:@"middle"]) return 2;
    if ([finger isEqualToString:@"ring"]) return 3;
    if ([finger isEqualToString:@"little"]) return 4;
    if ([finger isEqualToString:@"thumb"]) return 5;
    return -1;
}

bool ParseDeformationSpec(NSDictionary *json, DeformationData &data, NSError **error) {
    NSString *type = RequiredString(json, @"type", @"deformation", error);
    NSDictionary *bottom = [json[@"bottom"] isKindOfClass:NSDictionary.class] ? json[@"bottom"] : nil;
    NSArray *tops = [json[@"tops"] isKindOfClass:NSArray.class] ? json[@"tops"] : nil;
    if (!type || !bottom || tops.count == 0) {
        if (error && !*error) *error = MakeError(33, @"Incomplete V3 deformation specification");
        return false;
    }
    if ([type isEqualToString:@"multi_top_one_bottom"]) data.kind = DeformationKind::linear;
    else if ([type isEqualToString:@"volume_invariant_rod"]) data.kind = DeformationKind::volumeRod;
    else {
        if (error) *error = MakeError(34, [NSString stringWithFormat:@"Unsupported deformation type %@", type]);
        return false;
    }
    NSString *bottomTransform = RequiredString(bottom, @"transformId", @"deformation.bottom", error);
    if (!bottomTransform) return false;
    data.transformIds[0] = bottomTransform.UTF8String;
    std::array<bool, motorica::v3::kInfluenceCount> assigned{};
    assigned[0] = true;
    for (id candidate in tops) {
        if (![candidate isKindOfClass:NSDictionary.class]) {
            if (error) *error = MakeError(35, @"Invalid V3 deformation top entry");
            return false;
        }
        NSString *finger = RequiredString(candidate, @"finger", @"deformation.top", error);
        NSString *transform = RequiredString(candidate, @"transformId", @"deformation.top", error);
        int influence = InfluenceIndexForFinger(finger);
        if (!finger || !transform || influence < 1 || assigned[influence]) {
            if (error && !*error) *error = MakeError(36, [NSString stringWithFormat:@"Invalid/duplicate deformation finger %@", finger]);
            return false;
        }
        assigned[influence] = true;
        data.transformIds[influence] = transform.UTF8String;
    }
    if (data.kind == DeformationKind::volumeRod && tops.count != 1) {
        if (error) *error = MakeError(37, @"Volume-rod deformation must have exactly one top");
        return false;
    }
    return true;
}

void PrepareCenterline(DeformationData &data) {
    const size_t nodeCount = data.centerline.size() / 3;
    if (nodeCount < 2) return;
    data.centerlineTangents.assign(nodeCount * 3, 0.0f);
    data.centerlineSegmentLengths.assign(nodeCount - 1, 0.0f);
    auto normalized = [](float x, float y, float z) {
        float length = std::sqrt(x * x + y * y + z * z);
        if (length < 1.0e-7f) return std::array<float, 3>{1.0f, 0.0f, 0.0f};
        return std::array<float, 3>{x / length, y / length, z / length};
    };
    for (size_t i = 0; i + 1 < nodeCount; ++i) {
        float dx = data.centerline[(i + 1) * 3] - data.centerline[i * 3];
        float dy = data.centerline[(i + 1) * 3 + 1] - data.centerline[i * 3 + 1];
        float dz = data.centerline[(i + 1) * 3 + 2] - data.centerline[i * 3 + 2];
        data.centerlineSegmentLengths[i] = std::sqrt(dx * dx + dy * dy + dz * dz);
    }
    for (size_t i = 0; i < nodeCount; ++i) {
        size_t previous = i == 0 ? 0 : i - 1;
        size_t next = i + 1 < nodeCount ? i + 1 : nodeCount - 1;
        auto tangent = normalized(data.centerline[next * 3] - data.centerline[previous * 3],
                                  data.centerline[next * 3 + 1] - data.centerline[previous * 3 + 1],
                                  data.centerline[next * 3 + 2] - data.centerline[previous * 3 + 2]);
        std::copy(tangent.begin(), tangent.end(), data.centerlineTangents.begin() + i * 3);
    }
}

bool CompileShader(GLenum type, NSString *source, GLuint &shader, NSError **error) {
    const GLchar *bytes = source.UTF8String;
    GLint length = static_cast<GLint>(std::strlen(bytes));
    shader = glCreateShader(type);
    glShaderSource(shader, 1, &bytes, &length);
    glCompileShader(shader);
    GLint status = GL_FALSE;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &status);
    if (status == GL_TRUE) return true;
    GLint logLength = 0;
    glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &logLength);
    std::vector<GLchar> log(static_cast<size_t>(std::max(logLength, 1)));
    glGetShaderInfoLog(shader, logLength, nullptr, log.data());
    if (error) *error = MakeError(40, [NSString stringWithFormat:@"V3 shader compile failed: %s", log.data()]);
    glDeleteShader(shader);
    shader = 0;
    return false;
}

bool LinkProgram(NSString *vertexSource, NSString *fragmentSource, GLuint &program, NSError **error) {
    GLuint vertex = 0, fragment = 0;
    if (!CompileShader(GL_VERTEX_SHADER, vertexSource, vertex, error) ||
        !CompileShader(GL_FRAGMENT_SHADER, fragmentSource, fragment, error)) {
        if (vertex) glDeleteShader(vertex);
        if (fragment) glDeleteShader(fragment);
        return false;
    }
    program = glCreateProgram();
    glAttachShader(program, vertex);
    glAttachShader(program, fragment);
    const char *attributes[] = {"a_Position", "a_Normal", "a_Color", "a_TexCoordinate", "a_TangentIn", "a_BitangentIn"};
    for (GLuint index = 0; index < 6; ++index) glBindAttribLocation(program, index, attributes[index]);
    glLinkProgram(program);
    glDeleteShader(vertex);
    glDeleteShader(fragment);
    GLint status = GL_FALSE;
    glGetProgramiv(program, GL_LINK_STATUS, &status);
    if (status == GL_TRUE) return true;
    GLint logLength = 0;
    glGetProgramiv(program, GL_INFO_LOG_LENGTH, &logLength);
    std::vector<GLchar> log(static_cast<size_t>(std::max(logLength, 1)));
    glGetProgramInfoLog(program, logLength, nullptr, log.data());
    if (error) *error = MakeError(41, [NSString stringWithFormat:@"V3 program link failed: %s", log.data()]);
    glDeleteProgram(program);
    program = 0;
    return false;
}

NSString *ReadShader(NSString *fileName, NSError **error) {
    NSData *data = ReadBundledAsset(fileName, error);
    if (!data) return nil;
    NSString *source = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
    if (!source && error) *error = MakeError(42, [NSString stringWithFormat:@"Invalid shader text %@", fileName]);
    return source;
}

NSString *HighPrecisionFragmentShader(NSString *source) {
    static NSString *const mediumPrecision = @"precision mediump float;";
    static NSString *const highPrecision = @"precision highp float;";
    NSRange declaration = [source rangeOfString:mediumPrecision];
    if (declaration.location == NSNotFound) return source;
    return [source stringByReplacingCharactersInRange:declaration withString:highPrecision];
}

GLenum ASTCInternalFormat(uint8_t x, uint8_t y) {
    if (x == 4 && y == 4) return GL_COMPRESSED_RGBA_ASTC_4x4_KHR;
    if (x == 5 && y == 4) return GL_COMPRESSED_RGBA_ASTC_5x4_KHR;
    if (x == 5 && y == 5) return GL_COMPRESSED_RGBA_ASTC_5x5_KHR;
    if (x == 6 && y == 5) return GL_COMPRESSED_RGBA_ASTC_6x5_KHR;
    if (x == 6 && y == 6) return GL_COMPRESSED_RGBA_ASTC_6x6_KHR;
    if (x == 8 && y == 5) return GL_COMPRESSED_RGBA_ASTC_8x5_KHR;
    if (x == 8 && y == 6) return GL_COMPRESSED_RGBA_ASTC_8x6_KHR;
    if (x == 8 && y == 8) return GL_COMPRESSED_RGBA_ASTC_8x8_KHR;
    if (x == 10 && y == 5) return GL_COMPRESSED_RGBA_ASTC_10x5_KHR;
    if (x == 10 && y == 6) return GL_COMPRESSED_RGBA_ASTC_10x6_KHR;
    if (x == 10 && y == 8) return GL_COMPRESSED_RGBA_ASTC_10x8_KHR;
    if (x == 10 && y == 10) return GL_COMPRESSED_RGBA_ASTC_10x10_KHR;
    if (x == 12 && y == 10) return GL_COMPRESSED_RGBA_ASTC_12x10_KHR;
    if (x == 12 && y == 12) return GL_COMPRESSED_RGBA_ASTC_12x12_KHR;
    return 0;
}

uint32_t ReadUInt24(const uint8_t *bytes) {
    return static_cast<uint32_t>(bytes[0]) |
           (static_cast<uint32_t>(bytes[1]) << 8) |
           (static_cast<uint32_t>(bytes[2]) << 16);
}

bool UploadASTC(NSString *asset, GLuint &texture, NSError **error) {
    NSData *data = ReadBundledAsset(asset, error);
    if (!data || data.length < 16) {
        if (error && !*error) *error = MakeError(43, [NSString stringWithFormat:@"Invalid ASTC asset %@", asset]);
        return false;
    }
    const uint8_t *bytes = static_cast<const uint8_t *>(data.bytes);
    const uint8_t expectedMagic[4] = {0x13, 0xAB, 0xA1, 0x5C};
    if (std::memcmp(bytes, expectedMagic, 4) != 0 || bytes[6] != 1) {
        if (error) *error = MakeError(44, [NSString stringWithFormat:@"Unsupported ASTC header in %@", asset]);
        return false;
    }
    GLenum format = ASTCInternalFormat(bytes[4], bytes[5]);
    uint32_t width = ReadUInt24(bytes + 7);
    uint32_t height = ReadUInt24(bytes + 10);
    if (format == 0 || width == 0 || height == 0) {
        if (error) *error = MakeError(45, [NSString stringWithFormat:@"Unsupported ASTC dimensions/block in %@", asset]);
        return false;
    }
    glGenTextures(1, &texture);
    glBindTexture(GL_TEXTURE_2D, texture);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
    glCompressedTexImage2D(GL_TEXTURE_2D, 0, format, static_cast<GLsizei>(width), static_cast<GLsizei>(height), 0,
                           static_cast<GLsizei>(data.length - 16), bytes + 16);
    GLenum glError = glGetError();
    if (glError == GL_NO_ERROR) return true;
    glDeleteTextures(1, &texture);
    texture = 0;
    if (error) *error = MakeError(46, [NSString stringWithFormat:@"ASTC upload failed for %@: 0x%x", asset, glError]);
    return false;
}

bool UploadPNG(NSString *asset, GLuint &texture, NSError **error) {
    NSURL *url = BundleURLForAssetPath(asset);
    if (!url) {
        if (error) *error = MakeError(47, [NSString stringWithFormat:@"Missing PNG fallback %@", asset]);
        return false;
    }
    NSDictionary *options = @{GLKTextureLoaderOriginBottomLeft: @YES};
    GLKTextureInfo *info = [GLKTextureLoader textureWithContentsOfURL:url options:options error:error];
    if (!info) return false;
    texture = info.name;
    glBindTexture(GL_TEXTURE_2D, texture);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    return true;
}

bool ExtensionPresent(const char *name) {
    const char *extensions = reinterpret_cast<const char *>(glGetString(GL_EXTENSIONS));
    if (!extensions || !name) return false;
    std::string all(extensions);
    std::string needle(name);
    size_t position = all.find(needle);
    while (position != std::string::npos) {
        bool starts = position == 0 || all[position - 1] == ' ';
        size_t end = position + needle.size();
        bool ends = end == all.size() || all[end] == ' ';
        if (starts && ends) return true;
        position = all.find(needle, position + 1);
    }
    return false;
}

void DeleteGPUResources(ModelResources &resources) {
    for (ModelPart &part : resources.parts) {
        if (part.vertexBuffer) glDeleteBuffers(1, &part.vertexBuffer);
        if (part.indexBuffer) glDeleteBuffers(1, &part.indexBuffer);
        part.vertexBuffer = 0;
        part.indexBuffer = 0;
    }
    GLuint programs[] = {resources.gpu.materialProgram, resources.gpu.highlightedMaterialProgram, resources.gpu.pickingProgram};
    for (GLuint program : programs) if (program) glDeleteProgram(program);
    GLuint textures[] = {resources.gpu.grayTexture, resources.gpu.metalTexture};
    glDeleteTextures(2, textures);
    resources.gpu = {};
}

std::mutex gResourcesMutex;
ModelResourcesPtr gResources;

} // namespace

namespace motorica {
namespace v3 {

ModelResourcesPtr SharedModelResources() {
    std::lock_guard<std::mutex> lock(gResourcesMutex);
    return gResources;
}

} // namespace v3
} // namespace motorica

@interface V3ModelResourceCache () {
    dispatch_queue_t _queue;
    NSMutableArray *_callbacks;
    EAGLContext *_preloadContext;
    V3ModelResourceCacheState _state;
    NSError *_lastError;
    NSDictionary<NSString *, NSNumber *> *_latestMetrics;
    CFTimeInterval _openRequestTimestamp;
    NSUInteger _recordedFrameCount;
    NSUInteger _slowFrameCount;
    double _maximumFrameMilliseconds;
}
@end

@implementation V3ModelResourceCache

+ (instancetype)sharedCache {
    static V3ModelResourceCache *cache;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{ cache = [[self alloc] initPrivate]; });
    return cache;
}

- (instancetype)init {
    return [V3ModelResourceCache sharedCache];
}

- (instancetype)initPrivate {
    self = [super init];
    if (self) {
        _queue = dispatch_queue_create("com.bailout.stickk.v3-model-preload", DISPATCH_QUEUE_SERIAL);
        _callbacks = [NSMutableArray array];
        _state = V3ModelResourceCacheStateIdle;
        _latestMetrics = @{};
    }
    return self;
}

- (V3ModelResourceCacheState)state { @synchronized (self) { return _state; } }
- (BOOL)isReady { return self.state == V3ModelResourceCacheStateReady; }
- (NSError *)lastError { @synchronized (self) { return _lastError; } }
- (EAGLSharegroup *)sharegroup { @synchronized (self) { return _preloadContext.sharegroup; } }
- (NSDictionary<NSString *,NSNumber *> *)latestMetrics { @synchronized (self) { return [_latestMetrics copy]; } }

- (void)mark3DOpenRequested {
    @synchronized (self) {
        _openRequestTimestamp = CACurrentMediaTime();
    }
    NSLog(@"[V3OpenTrace] event=mark3DOpenRequestedRecorded thread=main cacheState=%ld",
          (long)self.state);
    os_signpost_event_emit(V3Log(), OS_SIGNPOST_ID_EXCLUSIVE, "V3OpenRequested");
}

- (void)recordFirstPresentedFrame {
    CFTimeInterval requestedAt = 0.0;
    @synchronized (self) {
        requestedAt = _openRequestTimestamp;
        _openRequestTimestamp = 0.0;
        if (requestedAt > 0.0) {
            NSMutableDictionary *metrics = [_latestMetrics mutableCopy] ?: [NSMutableDictionary dictionary];
            metrics[@"tapToFirstFrameMs"] = @((CACurrentMediaTime() - requestedAt) * 1000.0);
            _latestMetrics = [metrics copy];
        }
    }
    if (requestedAt > 0.0) {
        double milliseconds = (CACurrentMediaTime() - requestedAt) * 1000.0;
        NSLog(@"[V3OpenTrace] event=firstPresentedFrame thread=main tapToFirstFrameMs=%.3f cacheState=%ld",
              milliseconds,
              (long)self.state);
        os_log_info(V3Log(), "[V3Metrics] tapToFirstFrameMs=%{public}.3f", milliseconds);
        os_signpost_event_emit(V3Log(), OS_SIGNPOST_ID_EXCLUSIVE, "V3TapToFirstFrame",
                               "milliseconds=%{public}.3f", milliseconds);
    }
    [[NSNotificationCenter defaultCenter] postNotificationName:V3ModelFirstFramePresentedNotification
                                                        object:self
                                                      userInfo:@{@"metrics": self.latestMetrics ?: @{}}];
}

- (void)recordFrameDurationMilliseconds:(double)milliseconds {
    @synchronized (self) {
        _recordedFrameCount += 1;
        if (milliseconds > 16.67) _slowFrameCount += 1;
        _maximumFrameMilliseconds = std::max(_maximumFrameMilliseconds, milliseconds);
        NSMutableDictionary *metrics = [_latestMetrics mutableCopy] ?: [NSMutableDictionary dictionary];
        metrics[@"lastFrameMs"] = @(milliseconds);
        metrics[@"maxFrameMs"] = @(_maximumFrameMilliseconds);
        metrics[@"frameCount"] = @(_recordedFrameCount);
        metrics[@"slowFrameCount"] = @(_slowFrameCount);
        _latestMetrics = [metrics copy];
    }
}

- (void)preloadWithCompletion:(void (^)(BOOL, NSError * _Nullable))completion {
    NSLog(@"[V3OpenTrace] event=preloadCalled thread=%@ cacheState=%ld hasCompletion=%d",
          NSThread.isMainThread ? @"main" : @"preload",
          (long)self.state,
          completion != nil);
    BOOL start = NO;
    NSError *knownError = nil;
    @synchronized (self) {
        if (_state == V3ModelResourceCacheStateReady) {
            NSLog(@"[V3OpenTrace] event=preloadReadyImmediate thread=%@ cacheState=%ld",
                  NSThread.isMainThread ? @"main" : @"preload",
                  (long)_state);
            if (completion) dispatch_async(dispatch_get_main_queue(), ^{ completion(YES, nil); });
            return;
        }
        if (completion) [_callbacks addObject:[completion copy]];
        if (_state != V3ModelResourceCacheStateLoading) {
            _state = V3ModelResourceCacheStateLoading;
            _lastError = nil;
            start = YES;
        } else {
            knownError = _lastError;
        }
    }
    if (!start) {
        NSLog(@"[V3OpenTrace] event=preloadJoinedLoading thread=%@ cacheState=%ld error=%@",
              NSThread.isMainThread ? @"main" : @"preload",
              (long)self.state,
              knownError.localizedDescription ?: @"none");
        os_log_info(V3Log(), "preload joined existing request error=%{public}@", knownError);
        return;
    }

    NSLog(@"[V3OpenTrace] event=preloadStartedLoading thread=%@ cacheState=%ld",
          NSThread.isMainThread ? @"main" : @"preload",
          (long)self.state);
    dispatch_async(_queue, ^{ [self performPreload]; });
}

- (void)performPreload {
    CFTimeInterval totalStart = CACurrentMediaTime();
    NSLog(@"[V3OpenTrace] event=performPreloadBegin thread=preload cacheState=%ld",
          (long)self.state);
    os_signpost_id_t signpost = os_signpost_id_generate(V3Log());
    os_signpost_interval_begin(V3Log(), signpost, "V3Preload");
    NSError *error = nil;
    NSMutableDictionary<NSString *, NSNumber *> *metrics = [NSMutableDictionary dictionary];

    CFTimeInterval cpuStart = CACurrentMediaTime();
    ModelResourcesPtr resources = [self loadCPUResources:&error];
    metrics[@"cpuDecodeMs"] = @((CACurrentMediaTime() - cpuStart) * 1000.0);
    NSLog(@"[V3OpenTrace] event=cpuDecodeEnd thread=preload durationMs=%.3f success=%d error=%@",
          [metrics[@"cpuDecodeMs"] doubleValue],
          resources != nullptr,
          error.localizedDescription ?: @"none");

    if (resources) {
        CFTimeInterval deformationStart = CACurrentMediaTime();
        for (ModelPart &part : resources->parts) {
            if (part.deformation && part.deformation->kind == DeformationKind::volumeRod) {
                PrepareCenterline(*part.deformation);
            }
        }
        metrics[@"deformationPreparationMs"] = @((CACurrentMediaTime() - deformationStart) * 1000.0);
        NSLog(@"[V3OpenTrace] event=deformationPreparationEnd thread=preload durationMs=%.3f",
              [metrics[@"deformationPreparationMs"] doubleValue]);
    }

    if (resources) {
        CFTimeInterval contextStart = CACurrentMediaTime();
        NSLog(@"[V3OpenTrace] event=preloadContextBegin thread=preload");
        _preloadContext = [[EAGLContext alloc] initWithAPI:kEAGLRenderingAPIOpenGLES2];
        if (!_preloadContext || ![EAGLContext setCurrentContext:_preloadContext]) {
            error = MakeError(50, @"Could not create the V3 OpenGL ES 2 preload context");
            resources.reset();
        }
        NSLog(@"[V3OpenTrace] event=preloadContextEnd thread=preload durationMs=%.3f success=%d error=%@",
              (CACurrentMediaTime() - contextStart) * 1000.0,
              resources != nullptr,
              error.localizedDescription ?: @"none");
    }

    if (resources && ![self prepareGPUResources:*resources metrics:metrics error:&error]) {
        DeleteGPUResources(*resources);
        resources.reset();
    }
    [EAGLContext setCurrentContext:nil];

    if (resources) {
        std::lock_guard<std::mutex> lock(gResourcesMutex);
        gResources = resources;
    }
    metrics[@"totalMs"] = @((CACurrentMediaTime() - totalStart) * 1000.0);
    metrics[@"parts"] = @(resources ? resources->parts.size() : 0);
    metrics[@"vertices"] = @(resources ? resources->totalVertexCount : 0);
    metrics[@"indices"] = @(resources ? resources->totalIndexCount : 0);
    metrics[@"sourceBytes"] = @(resources ? resources->sourceBytes : 0);

    NSArray *callbacks;
    @synchronized (self) {
        _latestMetrics = [metrics copy];
        _lastError = error;
        _state = resources ? V3ModelResourceCacheStateReady : V3ModelResourceCacheStateFailed;
        callbacks = [_callbacks copy];
        [_callbacks removeAllObjects];
    }
    os_signpost_interval_end(V3Log(), signpost, "V3Preload");
    NSLog(@"[V3OpenTrace] event=performPreloadEnd thread=preload ready=%d totalMs=%.3f cpuDecodeMs=%.3f deformationMs=%.3f shaderMs=%.3f textureMs=%.3f gpuBuffersMs=%.3f cacheState=%ld error=%@",
          resources != nullptr,
          [metrics[@"totalMs"] doubleValue],
          [metrics[@"cpuDecodeMs"] doubleValue],
          [metrics[@"deformationPreparationMs"] doubleValue],
          [metrics[@"shaderCompilationMs"] doubleValue],
          [metrics[@"textureUploadMs"] doubleValue],
          [metrics[@"gpuBuffersMs"] doubleValue],
          (long)self.state,
          error.localizedDescription ?: @"none");
    os_log_info(V3Log(), "[V3Metrics] preload ready=%{public}d metrics=%{public}@ error=%{public}@",
                resources != nullptr, metrics, error);
    dispatch_async(dispatch_get_main_queue(), ^{
        NSNotificationName name = resources ? V3ModelResourceCacheDidBecomeReadyNotification : V3ModelResourceCacheDidFailNotification;
        NSLog(@"[V3OpenTrace] event=preloadNotification thread=main ready=%d cacheState=%ld",
              resources != nullptr,
              (long)self.state);
        [[NSNotificationCenter defaultCenter] postNotificationName:name object:self userInfo:error ? @{NSUnderlyingErrorKey: error} : nil];
        for (id value in callbacks) {
            void (^callback)(BOOL, NSError *) = value;
            callback(resources != nullptr, error);
        }
    });
}

- (ModelResourcesPtr)loadCPUResources:(NSError **)error {
    NSData *manifestData = ReadBundledAsset(@"festh3_test3_manifest.json", error);
    if (!manifestData) return nullptr;
    id object = [NSJSONSerialization JSONObjectWithData:manifestData options:0 error:error];
    if (![object isKindOfClass:NSDictionary.class]) {
        if (error && !*error) *error = MakeError(60, @"V3 manifest root must be an object");
        return nullptr;
    }
    NSDictionary *manifest = object;
    if ([manifest[@"version"] integerValue] != 2 || ![manifest[@"format"] isEqual:@"V3PB"] || [manifest[@"allowLegacyFallbacks"] boolValue]) {
        if (error) *error = MakeError(61, @"V3 manifest must require version 2 V3PB without legacy fallbacks");
        return nullptr;
    }
    NSDictionary *bundle = [manifest[@"bundle"] isKindOfClass:NSDictionary.class] ? manifest[@"bundle"] : nil;
    NSString *bundleAsset = RequiredString(bundle, @"asset", @"bundle", error);
    if (!bundleAsset) return nullptr;
    NSData *bundleData = ReadBundledAsset(bundleAsset, error);
    if (!bundleData) return nullptr;

    auto resources = std::make_shared<ModelResources>();
    BinaryReader bundleReader(bundleData, bundleAsset);
    if (!ReadPartsBundle(bundleReader, &resources->parts, error)) return nullptr;
    resources->sourceBytes += bundleData.length;

    std::unordered_map<std::string, size_t> indexes;
    auto registerPart = [&](size_t index) -> bool {
        const std::string &id = resources->parts[index].id;
        if (!indexes.emplace(id, index).second) {
            if (error) *error = MakeError(62, [NSString stringWithFormat:@"Duplicate part id %s", id.c_str()]);
            return false;
        }
        resources->groups["all"].push_back(index);
        resources->groups[id].push_back(index);
        return true;
    };
    for (size_t i = 0; i < resources->parts.size(); ++i) if (!registerPart(i)) return nullptr;

    NSArray *extraParts = [manifest[@"parts"] isKindOfClass:NSArray.class] ? manifest[@"parts"] : @[];
    for (id candidate in extraParts) {
        if (![candidate isKindOfClass:NSDictionary.class]) {
            if (error) *error = MakeError(63, @"V3 manifest part entry must be an object");
            return nullptr;
        }
        NSDictionary *partJSON = candidate;
        NSString *partID = RequiredString(partJSON, @"partId", @"part", error);
        NSString *binaryAsset = RequiredString(partJSON, @"binaryAsset", partID ?: @"part", error);
        if (!partID || !binaryAsset) return nullptr;
        NSData *partData = ReadBundledAsset(binaryAsset, error);
        if (!partData) return nullptr;
        ModelPart part;
        part.id = partID.UTF8String;
        BinaryReader partReader(partData, binaryAsset);
        if (!ReadModelPart(partReader, binaryAsset, part, error)) return nullptr;

        NSDictionary *deformationJSON = [partJSON[@"deformation"] isKindOfClass:NSDictionary.class] ? partJSON[@"deformation"] : nil;
        if (deformationJSON) {
            auto deformation = std::make_shared<DeformationData>();
            if (!ParseDeformationSpec(deformationJSON, *deformation, error)) return nullptr;
            NSString *deformationAsset = RequiredString(deformationJSON, @"asset", [partID stringByAppendingString:@".deformation"], error);
            NSData *deformationBytes = ReadBundledAsset(deformationAsset, error);
            if (!deformationBytes) return nullptr;
            BinaryReader deformationReader(deformationBytes, deformationAsset);
            std::shared_ptr<DeformationData> parsed;
            if (!ReadDeformation(deformationReader, static_cast<NSInteger>(part.vertexCount()),
                                 deformation->kind == DeformationKind::volumeRod, &parsed, error)) return nullptr;
            parsed->kind = deformation->kind;
            parsed->transformIds = deformation->transformIds;
            part.deformation = std::move(parsed);
            part.dynamic = true;
            resources->sourceBytes += deformationBytes.length;
        }
        resources->sourceBytes += partData.length;
        size_t index = resources->parts.size();
        resources->parts.push_back(std::move(part));
        if (!registerPart(index)) return nullptr;
        NSArray *partGroups = [partJSON[@"groups"] isKindOfClass:NSArray.class] ? partJSON[@"groups"] : @[];
        for (id group in partGroups) {
            if ([group isKindOfClass:NSString.class]) resources->groups[[group UTF8String]].push_back(index);
        }
    }

    NSDictionary *groupsJSON = [manifest[@"groups"] isKindOfClass:NSDictionary.class] ? manifest[@"groups"] : @{};
    for (NSString *groupName in groupsJSON) {
        NSArray *references = [groupsJSON[groupName] isKindOfClass:NSArray.class] ? groupsJSON[groupName] : nil;
        if (!references) {
            if (error) *error = MakeError(64, [NSString stringWithFormat:@"V3 group %@ must be an array", groupName]);
            return nullptr;
        }
        std::vector<size_t> resolved;
        std::set<size_t> unique;
        for (id reference in references) {
            if ([reference isKindOfClass:NSNumber.class]) {
                NSInteger index = [reference integerValue];
                if (index < 0 || static_cast<size_t>(index) >= resources->parts.size()) {
                    if (error) *error = MakeError(65, [NSString stringWithFormat:@"Part index %@ is outside group %@", reference, groupName]);
                    return nullptr;
                }
                unique.insert(static_cast<size_t>(index));
                continue;
            }
            if (![reference isKindOfClass:NSString.class]) {
                if (error) *error = MakeError(66, [NSString stringWithFormat:@"Invalid reference in V3 group %@", groupName]);
                return nullptr;
            }
            std::string key = [reference UTF8String];
            auto part = indexes.find(key);
            if (part != indexes.end()) {
                unique.insert(part->second);
                continue;
            }
            auto existingGroup = resources->groups.find(key);
            if (existingGroup != resources->groups.end()) {
                unique.insert(existingGroup->second.begin(), existingGroup->second.end());
                continue;
            }
            if (error) *error = MakeError(67, [NSString stringWithFormat:@"Unknown V3 group reference %@ in %@", reference, groupName]);
            return nullptr;
        }
        resolved.assign(unique.begin(), unique.end());
        resources->groups[groupName.UTF8String] = std::move(resolved);
    }
    for (const ModelPart &part : resources->parts) {
        resources->totalVertexCount += part.vertexCount();
        resources->totalIndexCount += part.indices.size();
    }
    return resources;
}

- (BOOL)prepareGPUResources:(ModelResources &)resources
                    metrics:(NSMutableDictionary<NSString *, NSNumber *> *)metrics
                      error:(NSError **)error {
    CFTimeInterval shaderStart = CACurrentMediaTime();
    NSString *vertex = ReadShader(@"v3_per_pixel_vertex_shader_tex_and_light.glsl", error);
    NSString *material = ReadShader(@"v3_per_pixel_fragment_shader_general.glsl", error);
    NSString *highlight = ReadShader(@"v3_per_pixel_fragment_shader_selection.glsl", error);
    NSString *pickVertex = ReadShader(@"v3_select_vertex_shader.glsl", error);
    NSString *pickFragment = ReadShader(@"v3_select_fragment_shader.glsl", error);
    material = HighPrecisionFragmentShader(material);
    highlight = HighPrecisionFragmentShader(highlight);
	    if (!vertex || !material || !highlight || !pickVertex || !pickFragment ||
	        !LinkProgram(vertex, material, resources.gpu.materialProgram, error) ||
	        !LinkProgram(vertex, highlight, resources.gpu.highlightedMaterialProgram, error) ||
	        !LinkProgram(pickVertex, pickFragment, resources.gpu.pickingProgram, error)) return NO;
	    metrics[@"shaderCompilationMs"] = @((CACurrentMediaTime() - shaderStart) * 1000.0);
	    NSLog(@"[V3OpenTrace] event=shaderCompilationEnd thread=preload durationMs=%.3f",
	          [metrics[@"shaderCompilationMs"] doubleValue]);

    CFTimeInterval textureStart = CACurrentMediaTime();
    BOOL astc = ExtensionPresent("GL_KHR_texture_compression_astc_ldr");
    NSError *astcError = nil;
    if (astc) {
        astc = UploadASTC(@"gray.astc", resources.gpu.grayTexture, &astcError) &&
               UploadASTC(@"metal_color2.astc", resources.gpu.metalTexture, &astcError);
        if (!astc) {
            if (resources.gpu.grayTexture) glDeleteTextures(1, &resources.gpu.grayTexture);
            if (resources.gpu.metalTexture) glDeleteTextures(1, &resources.gpu.metalTexture);
            resources.gpu.grayTexture = resources.gpu.metalTexture = 0;
            os_log_error(V3Log(), "ASTC fallback: %{public}@", astcError);
        }
    }
    if (!astc && (!UploadPNG(@"gray.png", resources.gpu.grayTexture, error) ||
                  !UploadPNG(@"metal_color2.png", resources.gpu.metalTexture, error))) return NO;
	    resources.gpu.astcTextures = astc;
	    metrics[@"textureUploadMs"] = @((CACurrentMediaTime() - textureStart) * 1000.0);
	    metrics[@"astc"] = @(astc);
	    NSLog(@"[V3OpenTrace] event=textureUploadEnd thread=preload durationMs=%.3f astc=%d",
	          [metrics[@"textureUploadMs"] doubleValue],
	          astc);

    CFTimeInterval bufferStart = CACurrentMediaTime();
    resources.gpu.uintIndices = ExtensionPresent("GL_OES_element_index_uint");
    if (!resources.gpu.uintIndices) {
        if (error) *error = MakeError(70, @"This OpenGL ES 2 device does not support 32-bit element indices");
        return NO;
    }
    for (ModelPart &part : resources.parts) {
        glGenBuffers(1, &part.vertexBuffer);
        glBindBuffer(GL_ARRAY_BUFFER, part.vertexBuffer);
        glBufferData(GL_ARRAY_BUFFER,
                     static_cast<GLsizeiptr>(part.vertices.size() * sizeof(float)),
                     part.vertices.data(),
                     part.dynamic ? GL_DYNAMIC_DRAW : GL_STATIC_DRAW);
        glGenBuffers(1, &part.indexBuffer);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, part.indexBuffer);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER,
                     static_cast<GLsizeiptr>(part.indices.size() * sizeof(uint32_t)),
                     part.indices.data(),
                     GL_STATIC_DRAW);
        GLenum glError = glGetError();
        if (glError != GL_NO_ERROR) {
            if (error) *error = MakeError(71, [NSString stringWithFormat:@"V3 GPU buffer upload failed for %s: 0x%x", part.id.c_str(), glError]);
            return NO;
        }
    }
    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
	    glFinish();
	    metrics[@"gpuBuffersMs"] = @((CACurrentMediaTime() - bufferStart) * 1000.0);
	    NSLog(@"[V3OpenTrace] event=gpuBuffersEnd thread=preload durationMs=%.3f parts=%zu vertices=%zu indices=%zu",
	          [metrics[@"gpuBuffersMs"] doubleValue],
	          resources.parts.size(),
	          resources.totalVertexCount,
	          resources.totalIndexCount);
	    return YES;
	}

- (EAGLContext *)newSharedContext {
    EAGLSharegroup *group = self.sharegroup;
    if (!group || !self.ready) return nil;
    return [[EAGLContext alloc] initWithAPI:kEAGLRenderingAPIOpenGLES2 sharegroup:group];
}

+ (BOOL)validatePartsBundleData:(NSData *)data error:(NSError **)error {
    BinaryReader reader(data, @"test V3PB");
    return ReadPartsBundle(reader, nullptr, error);
}

+ (BOOL)validateModelPartData:(NSData *)data error:(NSError **)error {
    BinaryReader reader(data, @"test V3MB");
    ModelPart part;
    return ReadModelPart(reader, @"test V3MB", part, error);
}

+ (BOOL)validateDeformationData:(NSData *)data
            expectedVertexCount:(NSInteger)expectedVertexCount
              requiresCenterline:(BOOL)requiresCenterline
                           error:(NSError **)error {
    BinaryReader reader(data, @"test V3DF");
    return ReadDeformation(reader, expectedVertexCount, requiresCenterline, nullptr, error);
}

#if DEBUG
- (NSDictionary<NSString *, NSArray<NSString *> *> *)resolvedGroupPartIDsForTesting {
    ModelResourcesPtr resources = motorica::v3::SharedModelResources();
    if (!resources) return @{};

    NSMutableDictionary<NSString *, NSArray<NSString *> *> *result = [NSMutableDictionary dictionary];
    for (const auto &entry : resources->groups) {
        NSMutableArray<NSString *> *partIDs = [NSMutableArray arrayWithCapacity:entry.second.size()];
        for (size_t index : entry.second) {
            if (index >= resources->parts.size()) continue;
            [partIDs addObject:[NSString stringWithUTF8String:resources->parts[index].id.c_str()]];
        }
        result[[NSString stringWithUTF8String:entry.first.c_str()]] = [partIDs copy];
    }
    return [result copy];
}

- (NSDictionary<NSString *, NSDictionary<NSString *, NSNumber *> *> *)deformationDiagnosticsForTesting {
    ModelResourcesPtr resources = motorica::v3::SharedModelResources();
    if (!resources) return @{};

    NSMutableDictionary<NSString *, NSDictionary<NSString *, NSNumber *> *> *result = [NSMutableDictionary dictionary];
    for (const ModelPart &part : resources->parts) {
        if (!part.dynamic || !part.deformation) continue;
        NSString *partID = [NSString stringWithUTF8String:part.id.c_str()];
        result[partID] = TestDeformationDiagnostics(part);
    }
    return [result copy];
}

- (void)resetForTesting {
    dispatch_sync(_queue, ^{
        if (_preloadContext) {
            [EAGLContext setCurrentContext:_preloadContext];
            std::lock_guard<std::mutex> lock(gResourcesMutex);
            if (gResources) DeleteGPUResources(*gResources);
            gResources.reset();
            [EAGLContext setCurrentContext:nil];
        }
        _preloadContext = nil;
        @synchronized (self) {
            _state = V3ModelResourceCacheStateIdle;
            _lastError = nil;
            _latestMetrics = @{};
            _openRequestTimestamp = 0.0;
            _recordedFrameCount = 0;
            _slowFrameCount = 0;
            _maximumFrameMilliseconds = 0.0;
            [_callbacks removeAllObjects];
        }
    });
}
#endif

@end
