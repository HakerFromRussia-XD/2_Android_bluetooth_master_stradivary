#include "V3VolumeRodDeformer.hpp"

#include <algorithm>
#include <array>
#include <cmath>
#include <limits>

namespace motorica {
namespace v3 {
namespace {

constexpr float kEpsilon = 0.000001f;
constexpr float kFingerWeightEpsilon = 0.0001f;
constexpr float kPalmAnchorBlend = 0.24f;
constexpr float kFingerAnchorBlend = 0.55f;
constexpr float kMinimumRadialScale = 1.0f;
constexpr float kMinimumAxialScale = 0.35f;
constexpr float kMaximumAxialScale = 2.5f;
constexpr float kPalmHandleRatio = 0.275f;
constexpr float kFingerHandleRatio = 0.50f;
constexpr float kMinimumHandleScale = 0.65f;
constexpr float kMaximumHandleScale = 1.20f;
constexpr float kBendingStrainGain = 0.0f;
constexpr float kPalmStrainBlend = 0.35f;
constexpr float kFingerStrainBlend = 0.32f;
constexpr float kMaximumCombinedRadialScale = 1.85f;
constexpr int kStretchSmoothingPasses = 16;
constexpr int kCompressionSmoothingPasses = 40;
constexpr float kCompressionRadialGain = 0.0f;
constexpr float kMaximumCompressionRadialScale = 1.04f;
constexpr float kMaximumCompressionCombinedRadialScale = 1.15f;

using Vec3 = std::array<float, 3>;
using Quat = std::array<float, 4>;

float clampValue(float value, float minimum, float maximum) {
    return std::max(minimum, std::min(maximum, value));
}

float lerpValue(float start, float end, float amount) {
    return start + (end - start) * amount;
}

float smoothstepValue(float value) {
    float clamped = clampValue(value, 0.0f, 1.0f);
    return clamped * clamped * (3.0f - 2.0f * clamped);
}

float dot3(const Vec3 &first, const Vec3 &second) {
    return first[0] * second[0] + first[1] * second[1] + first[2] * second[2];
}

float length3(const Vec3 &value) {
    return std::sqrt(dot3(value, value));
}

Vec3 normalize3(Vec3 value) {
    float length = length3(value);
    if (length <= kEpsilon) {
        return {1.0f, 0.0f, 0.0f};
    }
    value[0] /= length;
    value[1] /= length;
    value[2] /= length;
    return value;
}

Vec3 add3(const Vec3 &first, const Vec3 &second) {
    return {first[0] + second[0], first[1] + second[1], first[2] + second[2]};
}

Vec3 subtract3(const Vec3 &first, const Vec3 &second) {
    return {first[0] - second[0], first[1] - second[1], first[2] - second[2]};
}

Vec3 multiply3(const Vec3 &value, float scale) {
    return {value[0] * scale, value[1] * scale, value[2] * scale};
}

Vec3 lerp3(const Vec3 &start, const Vec3 &end, float amount) {
    return {
        lerpValue(start[0], end[0], amount),
        lerpValue(start[1], end[1], amount),
        lerpValue(start[2], end[2], amount),
    };
}

Vec3 cross3(const Vec3 &first, const Vec3 &second) {
    return {
        first[1] * second[2] - first[2] * second[1],
        first[2] * second[0] - first[0] * second[2],
        first[0] * second[1] - first[1] * second[0],
    };
}

Vec3 removeTangentComponent(Vec3 value, const Vec3 &tangent) {
    return subtract3(value, multiply3(tangent, dot3(value, tangent)));
}

float dotQuaternion(const Quat &first, const Quat &second) {
    return first[0] * second[0] + first[1] * second[1]
        + first[2] * second[2] + first[3] * second[3];
}

Quat normalizeQuaternion(Quat value) {
    float length = std::sqrt(dotQuaternion(value, value));
    if (length <= kEpsilon) {
        return {0.0f, 0.0f, 0.0f, 1.0f};
    }
    for (float &component : value) {
        component /= length;
    }
    return value;
}

Vec3 rotateByQuaternion(const Quat &quaternion, const Vec3 &value) {
    float tx = 2.0f * (quaternion[1] * value[2] - quaternion[2] * value[1]);
    float ty = 2.0f * (quaternion[2] * value[0] - quaternion[0] * value[2]);
    float tz = 2.0f * (quaternion[0] * value[1] - quaternion[1] * value[0]);
    return {
        value[0] + quaternion[3] * tx + quaternion[1] * tz - quaternion[2] * ty,
        value[1] + quaternion[3] * ty + quaternion[2] * tx - quaternion[0] * tz,
        value[2] + quaternion[3] * tz + quaternion[0] * ty - quaternion[1] * tx,
    };
}

Quat shortestArcQuaternion(const Vec3 &first, const Vec3 &second) {
    float dot = clampValue(dot3(first, second), -1.0f, 1.0f);
    if (dot < -0.999999f) {
        Quat result = {0.0f, first[2], -first[1], 0.0f};
        if (length3({result[0], result[1], result[2]}) <= kEpsilon) {
            result[0] = -first[2];
            result[1] = 0.0f;
            result[2] = first[0];
        }
        return normalizeQuaternion(result);
    }
    Vec3 cross = cross3(first, second);
    return normalizeQuaternion({cross[0], cross[1], cross[2], 1.0f + dot});
}

Vec3 matrixVector(matrix_float4x4 matrix, const Vec3 &value, bool position) {
    vector_float4 result = matrix_multiply(
        matrix,
        (vector_float4){value[0], value[1], value[2], position ? 1.0f : 0.0f}
    );
    Vec3 output = {result.x, result.y, result.z};
    return position ? output : normalize3(output);
}

void matrixToDualQuaternion(matrix_float4x4 matrix, Quat &real, Quat &dual) {
    Vec3 x = {matrix.columns[0].x, matrix.columns[0].y, matrix.columns[0].z};
    x = normalize3(x);
    Vec3 y = {matrix.columns[1].x, matrix.columns[1].y, matrix.columns[1].z};
    y = subtract3(y, multiply3(x, dot3(x, y)));
    y = normalize3(y);
    Vec3 z = cross3(x, y);

    float trace = x[0] + y[1] + z[2];
    if (trace > 0.0f) {
        float scale = std::sqrt(trace + 1.0f) * 2.0f;
        real[3] = 0.25f * scale;
        real[0] = (y[2] - z[1]) / scale;
        real[1] = (z[0] - x[2]) / scale;
        real[2] = (x[1] - y[0]) / scale;
    } else if (x[0] > y[1] && x[0] > z[2]) {
        float scale = std::sqrt(1.0f + x[0] - y[1] - z[2]) * 2.0f;
        real[3] = (y[2] - z[1]) / scale;
        real[0] = 0.25f * scale;
        real[1] = (y[0] + x[1]) / scale;
        real[2] = (z[0] + x[2]) / scale;
    } else if (y[1] > z[2]) {
        float scale = std::sqrt(1.0f + y[1] - x[0] - z[2]) * 2.0f;
        real[3] = (z[0] - x[2]) / scale;
        real[0] = (y[0] + x[1]) / scale;
        real[1] = 0.25f * scale;
        real[2] = (z[1] + y[2]) / scale;
    } else {
        float scale = std::sqrt(1.0f + z[2] - x[0] - y[1]) * 2.0f;
        real[3] = (x[1] - y[0]) / scale;
        real[0] = (z[0] + x[2]) / scale;
        real[1] = (z[1] + y[2]) / scale;
        real[2] = 0.25f * scale;
    }
    real = normalizeQuaternion(real);

    float tx = matrix.columns[3].x;
    float ty = matrix.columns[3].y;
    float tz = matrix.columns[3].z;
    dual[0] = 0.5f * (tx * real[3] + ty * real[2] - tz * real[1]);
    dual[1] = 0.5f * (-tx * real[2] + ty * real[3] + tz * real[0]);
    dual[2] = 0.5f * (tx * real[1] - ty * real[0] + tz * real[3]);
    dual[3] = -0.5f * (tx * real[0] + ty * real[1] + tz * real[2]);
}

void blendDualQuaternion(const Quat &firstReal,
                         const Quat &firstDual,
                         const Quat &secondReal,
                         const Quat &secondDual,
                         float amount,
                         Quat &resultReal,
                         Quat &resultDual,
                         Vec3 &translation) {
    for (int component = 0; component < 4; ++component) {
        resultReal[component] = lerpValue(firstReal[component], secondReal[component], amount);
        resultDual[component] = lerpValue(firstDual[component], secondDual[component], amount);
    }
    float length = std::sqrt(dotQuaternion(resultReal, resultReal));
    if (length <= kEpsilon) {
        resultReal = {0.0f, 0.0f, 0.0f, 1.0f};
        resultDual = {0.0f, 0.0f, 0.0f, 0.0f};
        translation = {0.0f, 0.0f, 0.0f};
        return;
    }
    for (int component = 0; component < 4; ++component) {
        resultReal[component] /= length;
        resultDual[component] /= length;
    }
    float dualProjection = dotQuaternion(resultReal, resultDual);
    for (int component = 0; component < 4; ++component) {
        resultDual[component] -= resultReal[component] * dualProjection;
    }

    float bx = -resultReal[0];
    float by = -resultReal[1];
    float bz = -resultReal[2];
    float bw = resultReal[3];
    translation[0] = 2.0f * (resultDual[3] * bx + resultDual[0] * bw
        + resultDual[1] * bz - resultDual[2] * by);
    translation[1] = 2.0f * (resultDual[3] * by - resultDual[0] * bz
        + resultDual[1] * bw + resultDual[2] * bx);
    translation[2] = 2.0f * (resultDual[3] * bz + resultDual[0] * by
        - resultDual[1] * bx + resultDual[2] * bw);
}

Vec3 vectorAt(const std::vector<float> &values, size_t index) {
    size_t offset = index * 3;
    return {values[offset], values[offset + 1], values[offset + 2]};
}

void setVectorAt(std::vector<float> &values, size_t index, const Vec3 &value) {
    size_t offset = index * 3;
    values[offset] = value[0];
    values[offset + 1] = value[1];
    values[offset + 2] = value[2];
}

Quat quaternionAt(const std::vector<float> &values, size_t index) {
    size_t offset = index * 4;
    return {values[offset], values[offset + 1], values[offset + 2], values[offset + 3]};
}

void setQuaternionAt(std::vector<float> &values, size_t index, const Quat &value) {
    size_t offset = index * 4;
    for (int component = 0; component < 4; ++component) {
        values[offset + component] = value[component];
    }
}

void computePolylineTangents(const std::vector<float> &centers,
                             std::vector<float> &tangents,
                             std::vector<float> *segmentLengths) {
    size_t nodeCount = centers.size() / 3;
    if (nodeCount < 2) return;
    if (segmentLengths != nullptr) {
        segmentLengths->resize(nodeCount - 1);
        for (size_t segment = 0; segment + 1 < nodeCount; ++segment) {
            (*segmentLengths)[segment] = length3(subtract3(
                vectorAt(centers, segment + 1),
                vectorAt(centers, segment)
            ));
        }
    }
    tangents.resize(nodeCount * 3);
    for (size_t node = 0; node < nodeCount; ++node) {
        Vec3 tangent;
        if (node == 0) {
            tangent = subtract3(vectorAt(centers, 1), vectorAt(centers, 0));
        } else if (node + 1 == nodeCount) {
            tangent = subtract3(vectorAt(centers, node), vectorAt(centers, node - 1));
        } else {
            Vec3 previous = subtract3(vectorAt(centers, node), vectorAt(centers, node - 1));
            Vec3 next = subtract3(vectorAt(centers, node + 1), vectorAt(centers, node));
            float previousLength = length3(previous);
            float nextLength = length3(next);
            if (previousLength > kEpsilon) previous = multiply3(previous, 1.0f / previousLength);
            if (nextLength > kEpsilon) next = multiply3(next, 1.0f / nextLength);
            tangent = add3(previous, next);
        }
        setVectorAt(tangents, node, normalize3(tangent));
    }
}

Vec3 evaluateGuide(const Vec3 &start,
                   const Vec3 &startTangent,
                   float startHandle,
                   const Vec3 &end,
                   const Vec3 &endTangent,
                   float endHandle,
                   float progress) {
    float t = clampValue(progress, 0.0f, 1.0f);
    float remaining = 1.0f - t;
    float startWeight = remaining * remaining * remaining;
    float startControlWeight = 3.0f * remaining * remaining * t;
    float endControlWeight = 3.0f * remaining * t * t;
    float endWeight = t * t * t;
    Vec3 startControl = add3(start, multiply3(startTangent, startHandle));
    Vec3 endControl = subtract3(end, multiply3(endTangent, endHandle));
    return {
        start[0] * startWeight + startControl[0] * startControlWeight
            + endControl[0] * endControlWeight + end[0] * endWeight,
        start[1] * startWeight + startControl[1] * startControlWeight
            + endControl[1] * endControlWeight + end[1] * endWeight,
        start[2] * startWeight + startControl[2] * startControlWeight
            + endControl[2] * endControlWeight + end[2] * endWeight,
    };
}

} // namespace

struct VolumeRodDeformer::Runtime {
    size_t nodeCount = 0;
    int topInfluence = -1;
    std::vector<float> currentCenters;
    std::vector<float> nodeRotations;
    std::vector<float> restTangents;
    std::vector<float> currentTangents;
    std::vector<float> restSegmentLengths;
    std::vector<float> segmentAxialScales;
    std::vector<float> segmentRadialScales;
    std::vector<float> segmentScaleScratch;
    std::vector<float> normalSums;
    float totalRestLength = 0.0f;
    float restChordLength = 0.0f;

    Quat bottomReal{};
    Quat bottomDual{};
    Quat topReal{};
    Quat topDual{};
    Quat vertexRotation{};
    Quat frameAlignmentRotation{};
    Vec3 restTangent{};
    Vec3 currentTangent{};
    Vec3 referenceTangent{};
    Vec3 bendNormal{};
    Vec3 curvatureNormal{};
    Vec3 restCenter{};
    Vec3 currentCenter{};
    std::array<float, 4> frameScalars{};

    Runtime(const ModelPart &part) {
        if (!part.deformation || part.deformation->kind != DeformationKind::volumeRod) return;
        const DeformationData &data = *part.deformation;
        if (data.centerline.size() < 6 || data.centerline.size() % 3 != 0) return;
        nodeCount = data.centerline.size() / 3;
        for (int influence = 1; influence < kInfluenceCount; ++influence) {
            if (!data.transformIds[influence].empty()) {
                topInfluence = influence;
                break;
            }
        }
        if (topInfluence < 1) {
            nodeCount = 0;
            return;
        }

        currentCenters.resize(nodeCount * 3);
        nodeRotations.resize(nodeCount * 4);
        computePolylineTangents(data.centerline, restTangents, &restSegmentLengths);
        currentTangents.resize(nodeCount * 3);
        segmentAxialScales.resize(nodeCount - 1, 1.0f);
        segmentRadialScales.resize(nodeCount - 1, 1.0f);
        segmentScaleScratch.resize(nodeCount - 1, 1.0f);
        normalSums.resize(part.vertexCount() * 3);
        for (float segmentLength : restSegmentLengths) {
            totalRestLength += segmentLength;
        }
        restChordLength = length3(subtract3(
            vectorAt(data.centerline, nodeCount - 1),
            vectorAt(data.centerline, 0)
        ));
    }

    bool valid() const {
        return nodeCount >= 2 && topInfluence >= 1 && totalRestLength > kEpsilon;
    }

    void smoothSegmentScales(int passes) {
        for (int pass = 0; pass < passes; ++pass) {
            for (size_t segment = 0; segment < segmentAxialScales.size(); ++segment) {
                float current = segmentAxialScales[segment];
                float previous = segment > 0 ? segmentAxialScales[segment - 1] : current;
                float next = segment + 1 < segmentAxialScales.size()
                    ? segmentAxialScales[segment + 1] : current;
                segmentScaleScratch[segment] = previous * 0.25f + current * 0.5f + next * 0.25f;
            }
            segmentAxialScales.swap(segmentScaleScratch);
        }
    }

    void prepare(const DeformationData &data,
                 matrix_float4x4 palmMatrix,
                 matrix_float4x4 fingerMatrix) {
        matrixToDualQuaternion(palmMatrix, bottomReal, bottomDual);
        matrixToDualQuaternion(fingerMatrix, topReal, topDual);
        if (dotQuaternion(bottomReal, topReal) < 0.0f) {
            for (int component = 0; component < 4; ++component) {
                topReal[component] = -topReal[component];
                topDual[component] = -topDual[component];
            }
        }

        Vec3 restStart = vectorAt(data.centerline, 0);
        Vec3 restEnd = vectorAt(data.centerline, nodeCount - 1);
        Vec3 guideStart = matrixVector(palmMatrix, restStart, true);
        Vec3 guideEnd = matrixVector(fingerMatrix, restEnd, true);
        Vec3 guideStartTangent = normalize3(rotateByQuaternion(bottomReal, vectorAt(restTangents, 0)));
        Vec3 guideEndTangent = normalize3(rotateByQuaternion(topReal, vectorAt(restTangents, nodeCount - 1)));
        float currentChordLength = length3(subtract3(guideEnd, guideStart));
        float handleScale = restChordLength > kEpsilon
            ? clampValue(currentChordLength / restChordLength,
                         kMinimumHandleScale,
                         kMaximumHandleScale)
            : 1.0f;
        float restPalmHandle = totalRestLength * kPalmHandleRatio;
        float restFingerHandle = totalRestLength * kFingerHandleRatio;
        float currentPalmHandle = restPalmHandle * handleScale;
        float currentFingerHandle = restFingerHandle * handleScale;

        Vec3 restStartTangent = vectorAt(restTangents, 0);
        Vec3 restEndTangent = vectorAt(restTangents, nodeCount - 1);
        for (size_t node = 0; node < nodeCount; ++node) {
            float progress = static_cast<float>(node) / static_cast<float>(nodeCount - 1);
            Quat blendedReal;
            Quat blendedDual;
            Vec3 blendedTranslation;
            blendDualQuaternion(bottomReal, bottomDual, topReal, topDual,
                                progress, blendedReal, blendedDual, blendedTranslation);
            setQuaternionAt(nodeRotations, node, blendedReal);
            Vec3 restGuidePoint = evaluateGuide(
                restStart, restStartTangent, restPalmHandle,
                restEnd, restEndTangent, restFingerHandle,
                progress
            );
            Vec3 currentGuidePoint = evaluateGuide(
                guideStart, guideStartTangent, currentPalmHandle,
                guideEnd, guideEndTangent, currentFingerHandle,
                progress
            );
            Vec3 centerCorrection = rotateByQuaternion(
                blendedReal,
                subtract3(vectorAt(data.centerline, node), restGuidePoint)
            );
            setVectorAt(currentCenters, node, add3(currentGuidePoint, centerCorrection));
        }

        computePolylineTangents(currentCenters, currentTangents, nullptr);
        for (size_t segment = 0; segment + 1 < nodeCount; ++segment) {
            float currentLength = length3(subtract3(
                vectorAt(currentCenters, segment + 1),
                vectorAt(currentCenters, segment)
            ));
            float restLength = restSegmentLengths[segment];
            float axialScale = restLength > kEpsilon ? currentLength / restLength : 1.0f;
            segmentAxialScales[segment] = clampValue(
                axialScale,
                kMinimumAxialScale,
                kMaximumAxialScale
            );
        }
        smoothSegmentScales(currentChordLength < restChordLength
            ? kCompressionSmoothingPasses
            : kStretchSmoothingPasses);
        for (size_t segment = 0; segment + 1 < nodeCount; ++segment) {
            float axialScale = segmentAxialScales[segment];
            float volumeScale = std::sqrt(1.0f / axialScale);
            if (axialScale < 1.0f) {
                volumeScale = lerpValue(1.0f, volumeScale, kCompressionRadialGain);
                volumeScale = std::min(volumeScale, kMaximumCompressionRadialScale);
            }
            segmentRadialScales[segment] = std::max(kMinimumRadialScale, volumeScale);
        }
    }

    float nodeScale(const std::vector<float> &segmentScales, size_t node) const {
        if (node == 0) return segmentScales.front();
        if (node >= segmentScales.size()) return segmentScales.back();
        return (segmentScales[node - 1] + segmentScales[node]) * 0.5f;
    }

    Quat nlerpNodeRotation(size_t segment, float amount) const {
        Quat first = quaternionAt(nodeRotations, segment);
        Quat second = quaternionAt(nodeRotations, segment + 1);
        float sign = dotQuaternion(first, second) < 0.0f ? -1.0f : 1.0f;
        Quat result;
        for (int component = 0; component < 4; ++component) {
            result[component] = lerpValue(first[component], second[component] * sign, amount);
        }
        return normalizeQuaternion(result);
    }

    void fillFrame(const DeformationData &data, float progress) {
        float nodePosition = progress * static_cast<float>(nodeCount - 1);
        size_t segment = std::min(static_cast<size_t>(std::floor(nodePosition)), nodeCount - 2);
        float amount = nodePosition - static_cast<float>(segment);
        restCenter = lerp3(vectorAt(data.centerline, segment),
                           vectorAt(data.centerline, segment + 1), amount);
        currentCenter = lerp3(vectorAt(currentCenters, segment),
                              vectorAt(currentCenters, segment + 1), amount);
        restTangent = normalize3(lerp3(vectorAt(restTangents, segment),
                                       vectorAt(restTangents, segment + 1), amount));
        currentTangent = normalize3(lerp3(vectorAt(currentTangents, segment),
                                          vectorAt(currentTangents, segment + 1), amount));

        curvatureNormal = subtract3(
            vectorAt(currentTangents, segment + 1),
            vectorAt(currentTangents, segment)
        );
        curvatureNormal = removeTangentComponent(curvatureNormal, currentTangent);
        float tangentTurn = length3(curvatureNormal);
        curvatureNormal = tangentTurn <= kEpsilon
            ? Vec3{0.0f, 0.0f, 0.0f}
            : multiply3(curvatureNormal, 1.0f / tangentTurn);

        vertexRotation = nlerpNodeRotation(segment, amount);
        referenceTangent = normalize3(rotateByQuaternion(vertexRotation, restTangent));
        frameAlignmentRotation = shortestArcQuaternion(referenceTangent, currentTangent);
        float tangentDot = clampValue(dot3(referenceTangent, currentTangent), -1.0f, 1.0f);
        bendNormal = subtract3(referenceTangent, multiply3(currentTangent, tangentDot));
        float bendAmount = length3(bendNormal);
        bendNormal = bendAmount > kEpsilon
            ? multiply3(bendNormal, 1.0f / bendAmount)
            : Vec3{0.0f, 0.0f, 0.0f};

        float startAxialScale = nodeScale(segmentAxialScales, segment);
        float endAxialScale = nodeScale(segmentAxialScales, segment + 1);
        float startRadialScale = nodeScale(segmentRadialScales, segment);
        float endRadialScale = nodeScale(segmentRadialScales, segment + 1);
        float anchorBlend = std::min(
            smoothstepValue(progress / kPalmAnchorBlend),
            smoothstepValue((1.0f - progress) / kFingerAnchorBlend)
        );
        float axialScale = lerpValue(startAxialScale, endAxialScale, amount);
        float radialScale = lerpValue(startRadialScale, endRadialScale, amount);
        frameScalars[0] = lerpValue(1.0f, axialScale, anchorBlend);
        frameScalars[1] = lerpValue(1.0f, radialScale, anchorBlend);
        frameScalars[2] = anchorBlend;
        float strainBlend = std::min(
            smoothstepValue(progress / kPalmStrainBlend),
            smoothstepValue((1.0f - progress) / kFingerStrainBlend)
        );
        float requestedStrainScale = 1.0f + kBendingStrainGain * bendAmount * strainBlend;
        float maximumCombinedRadialScale = axialScale < 1.0f
            ? kMaximumCompressionCombinedRadialScale
            : kMaximumCombinedRadialScale;
        float maximumStrainScale = std::max(
            1.0f,
            maximumCombinedRadialScale / frameScalars[1]
        );
        frameScalars[3] = std::min(requestedStrainScale, maximumStrainScale);
    }

    float curvatureComponent(const Vec3 &radialOffset) const {
        return dot3(radialOffset, curvatureNormal);
    }

    Vec3 applyBendScale(Vec3 value, float scale) const {
        float component = dot3(value, bendNormal);
        return add3(value, multiply3(bendNormal, component * (scale - 1.0f)));
    }

    Vec3 limitInnerExpansion(Vec3 radialOffset, float baseInnerOffset) const {
        if (baseInnerOffset <= 0.0f) return radialOffset;
        float innerOffset = curvatureComponent(radialOffset);
        if (innerOffset <= baseInnerOffset) return radialOffset;
        return subtract3(radialOffset,
                         multiply3(curvatureNormal, innerOffset - baseInnerOffset));
    }

    Vec3 transformPosition(const Vec3 &source) const {
        Vec3 offset = subtract3(source, restCenter);
        float axialOffset = dot3(offset, restTangent);
        Vec3 radial = subtract3(offset, multiply3(restTangent, axialOffset));
        float radialLength = length3(radial);
        Vec3 rotatedRadial = rotateByQuaternion(vertexRotation, radial);
        rotatedRadial = rotateByQuaternion(frameAlignmentRotation, rotatedRadial);
        rotatedRadial = removeTangentComponent(rotatedRadial, currentTangent);
        float rotatedRadialLength = length3(rotatedRadial);
        float baseInnerOffset = 0.0f;
        if (radialLength > kEpsilon && rotatedRadialLength > kEpsilon) {
            rotatedRadial = multiply3(rotatedRadial, radialLength / rotatedRadialLength);
            baseInnerOffset = curvatureComponent(rotatedRadial);
            rotatedRadial = multiply3(rotatedRadial, frameScalars[1]);
        } else {
            rotatedRadial = {0.0f, 0.0f, 0.0f};
        }
        rotatedRadial = applyBendScale(rotatedRadial, frameScalars[3]);
        rotatedRadial = limitInnerExpansion(rotatedRadial, baseInnerOffset);
        float transformedAxialOffset = axialOffset * frameScalars[0];
        return add3(
            add3(currentCenter, multiply3(currentTangent, transformedAxialOffset)),
            rotatedRadial
        );
    }

    Vec3 transformDirection(const Vec3 &source, bool inverseScale) const {
        float axial = dot3(source, restTangent);
        Vec3 radial = subtract3(source, multiply3(restTangent, axial));
        float radialLength = length3(radial);
        Vec3 rotatedRadial = rotateByQuaternion(vertexRotation, radial);
        rotatedRadial = rotateByQuaternion(frameAlignmentRotation, rotatedRadial);
        rotatedRadial = removeTangentComponent(rotatedRadial, currentTangent);
        float rotatedRadialLength = length3(rotatedRadial);
        float radialScale = inverseScale ? 1.0f / frameScalars[1] : frameScalars[1];
        if (radialLength > kEpsilon && rotatedRadialLength > kEpsilon) {
            rotatedRadial = multiply3(
                rotatedRadial,
                radialLength * radialScale / rotatedRadialLength
            );
        } else {
            rotatedRadial = {0.0f, 0.0f, 0.0f};
        }
        float bendScale = inverseScale ? 1.0f / frameScalars[3] : frameScalars[3];
        rotatedRadial = applyBendScale(rotatedRadial, bendScale);
        float axialScale = inverseScale ? 1.0f / frameScalars[0] : frameScalars[0];
        return normalize3(add3(multiply3(currentTangent, axial * axialScale), rotatedRadial));
    }

    Vec3 blendWithAnchor(const Vec3 &source,
                         matrix_float4x4 anchorMatrix,
                         bool position,
                         const Vec3 &rodValue,
                         float rodBlend) const {
        Vec3 rigid = matrixVector(anchorMatrix, source, position);
        Vec3 result = lerp3(rigid, rodValue, rodBlend);
        return position ? result : normalize3(result);
    }

    void recalculateSurfaceFrame(std::vector<float> &vertices,
                                 const std::vector<uint32_t> &indices) {
        std::fill(normalSums.begin(), normalSums.end(), 0.0f);
        for (size_t index = 0; index + 2 < indices.size(); index += 3) {
            uint32_t firstVertex = indices[index];
            uint32_t secondVertex = indices[index + 1];
            uint32_t thirdVertex = indices[index + 2];
            if (firstVertex * kFloatsPerVertex + 2 >= vertices.size()
                || secondVertex * kFloatsPerVertex + 2 >= vertices.size()
                || thirdVertex * kFloatsPerVertex + 2 >= vertices.size()) {
                continue;
            }
            Vec3 first = {
                vertices[firstVertex * kFloatsPerVertex],
                vertices[firstVertex * kFloatsPerVertex + 1],
                vertices[firstVertex * kFloatsPerVertex + 2],
            };
            Vec3 second = {
                vertices[secondVertex * kFloatsPerVertex],
                vertices[secondVertex * kFloatsPerVertex + 1],
                vertices[secondVertex * kFloatsPerVertex + 2],
            };
            Vec3 third = {
                vertices[thirdVertex * kFloatsPerVertex],
                vertices[thirdVertex * kFloatsPerVertex + 1],
                vertices[thirdVertex * kFloatsPerVertex + 2],
            };
            Vec3 normal = cross3(subtract3(second, first), subtract3(third, first));
            float normalLength = length3(normal);
            if (normalLength <= kEpsilon) continue;
            normal = multiply3(normal, 1.0f / normalLength);
            for (uint32_t vertex : {firstVertex, secondVertex, thirdVertex}) {
                size_t normalOffset = static_cast<size_t>(vertex) * 3;
                normalSums[normalOffset] += normal[0];
                normalSums[normalOffset + 1] += normal[1];
                normalSums[normalOffset + 2] += normal[2];
            }
        }

        size_t vertexCount = vertices.size() / kFloatsPerVertex;
        for (size_t vertex = 0; vertex < vertexCount; ++vertex) {
            size_t vertexOffset = vertex * kFloatsPerVertex;
            size_t normalOffset = vertex * 3;
            Vec3 normal = {
                normalSums[normalOffset],
                normalSums[normalOffset + 1],
                normalSums[normalOffset + 2],
            };
            float normalLength = length3(normal);
            if (normalLength <= kEpsilon) continue;
            normal = multiply3(normal, 1.0f / normalLength);
            vertices[vertexOffset + 3] = normal[0];
            vertices[vertexOffset + 4] = normal[1];
            vertices[vertexOffset + 5] = normal[2];

            Vec3 tangent = {
                vertices[vertexOffset + 12],
                vertices[vertexOffset + 13],
                vertices[vertexOffset + 14],
            };
            tangent = removeTangentComponent(tangent, normal);
            if (length3(tangent) <= kEpsilon) {
                tangent = std::fabs(normal[2]) < 0.9f
                    ? Vec3{normal[1], -normal[0], 0.0f}
                    : Vec3{-normal[2], 0.0f, normal[0]};
            }
            tangent = normalize3(tangent);
            vertices[vertexOffset + 12] = tangent[0];
            vertices[vertexOffset + 13] = tangent[1];
            vertices[vertexOffset + 14] = tangent[2];

            Vec3 previousBitangent = {
                vertices[vertexOffset + 15],
                vertices[vertexOffset + 16],
                vertices[vertexOffset + 17],
            };
            Vec3 bitangent = cross3(normal, tangent);
            float handedness = dot3(bitangent, previousBitangent) < 0.0f ? -1.0f : 1.0f;
            bitangent = multiply3(bitangent, handedness);
            vertices[vertexOffset + 15] = bitangent[0];
            vertices[vertexOffset + 16] = bitangent[1];
            vertices[vertexOffset + 17] = bitangent[2];
        }
    }
};

VolumeRodDeformer::VolumeRodDeformer(const ModelPart &part)
    : runtime_(new Runtime(part)) {}

VolumeRodDeformer::~VolumeRodDeformer() = default;

bool VolumeRodDeformer::isValid() const {
    return runtime_ && runtime_->valid();
}

int VolumeRodDeformer::topInfluenceSlot() const {
    return runtime_ ? runtime_->topInfluence : -1;
}

bool VolumeRodDeformer::deform(const ModelPart &part,
                               matrix_float4x4 palmMatrix,
                               matrix_float4x4 fingerMatrix,
                               std::vector<float> &target) {
    if (!isValid() || !part.deformation) return false;
    const DeformationData &data = *part.deformation;
    size_t vertexCount = part.vertexCount();
    if (data.weights.size() != vertexCount * kInfluenceCount) return false;

    target = part.vertices;
    runtime_->prepare(data, palmMatrix, fingerMatrix);
    for (size_t vertex = 0; vertex < vertexCount; ++vertex) {
        size_t vertexOffset = vertex * kFloatsPerVertex;
        size_t weightOffset = vertex * kInfluenceCount;
        float progress = clampValue(
            data.weights[weightOffset + runtime_->topInfluence],
            0.0f,
            1.0f
        );
        Vec3 position = {
            part.vertices[vertexOffset],
            part.vertices[vertexOffset + 1],
            part.vertices[vertexOffset + 2],
        };
        Vec3 normal = {
            part.vertices[vertexOffset + 3],
            part.vertices[vertexOffset + 4],
            part.vertices[vertexOffset + 5],
        };
        Vec3 tangent = {
            part.vertices[vertexOffset + 12],
            part.vertices[vertexOffset + 13],
            part.vertices[vertexOffset + 14],
        };
        Vec3 bitangent = {
            part.vertices[vertexOffset + 15],
            part.vertices[vertexOffset + 16],
            part.vertices[vertexOffset + 17],
        };

        if (progress <= kFingerWeightEpsilon) {
            position = matrixVector(palmMatrix, position, true);
            normal = matrixVector(palmMatrix, normal, false);
            tangent = matrixVector(palmMatrix, tangent, false);
            bitangent = matrixVector(palmMatrix, bitangent, false);
        } else if (progress >= 1.0f - kFingerWeightEpsilon) {
            position = matrixVector(fingerMatrix, position, true);
            normal = matrixVector(fingerMatrix, normal, false);
            tangent = matrixVector(fingerMatrix, tangent, false);
            bitangent = matrixVector(fingerMatrix, bitangent, false);
        } else {
            runtime_->fillFrame(data, progress);
            Vec3 rodPosition = runtime_->transformPosition(position);
            Vec3 rodNormal = runtime_->transformDirection(normal, true);
            Vec3 rodTangent = runtime_->transformDirection(tangent, false);
            Vec3 rodBitangent = runtime_->transformDirection(bitangent, false);
            float rodBlend = runtime_->frameScalars[2];
            if (rodBlend < 1.0f) {
                matrix_float4x4 anchor = progress < 0.5f ? palmMatrix : fingerMatrix;
                rodPosition = runtime_->blendWithAnchor(position, anchor, true, rodPosition, rodBlend);
                rodNormal = runtime_->blendWithAnchor(normal, anchor, false, rodNormal, rodBlend);
                rodTangent = runtime_->blendWithAnchor(tangent, anchor, false, rodTangent, rodBlend);
                rodBitangent = runtime_->blendWithAnchor(bitangent, anchor, false, rodBitangent, rodBlend);
            }
            position = rodPosition;
            normal = rodNormal;
            tangent = rodTangent;
            bitangent = rodBitangent;
        }

        target[vertexOffset] = position[0];
        target[vertexOffset + 1] = position[1];
        target[vertexOffset + 2] = position[2];
        target[vertexOffset + 3] = normal[0];
        target[vertexOffset + 4] = normal[1];
        target[vertexOffset + 5] = normal[2];
        target[vertexOffset + 12] = tangent[0];
        target[vertexOffset + 13] = tangent[1];
        target[vertexOffset + 14] = tangent[2];
        target[vertexOffset + 15] = bitangent[0];
        target[vertexOffset + 16] = bitangent[1];
        target[vertexOffset + 17] = bitangent[2];
    }
    runtime_->recalculateSurfaceFrame(target, part.indices);
    return true;
}

} // namespace v3
} // namespace motorica
