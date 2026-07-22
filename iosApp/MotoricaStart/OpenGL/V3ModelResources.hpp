#pragma once

#import <OpenGLES/ES2/gl.h>

#include <array>
#include <cstdint>
#include <memory>
#include <string>
#include <unordered_map>
#include <vector>

namespace motorica {
namespace v3 {

constexpr int kFloatsPerVertex = 18;
constexpr int kInfluenceCount = 6;

enum class DeformationKind {
    none,
    linear,
    volumeRod,
};

struct DeformationData {
    DeformationKind kind = DeformationKind::none;
    std::array<std::string, kInfluenceCount> transformIds;
    std::vector<float> weights;
    std::vector<int32_t> selectionInfluences;
    std::vector<float> centerline;
    std::vector<float> centerlineTangents;
    std::vector<float> centerlineSegmentLengths;
};

struct ModelPart {
    std::string id;
    std::vector<float> vertices;
    std::vector<uint32_t> indices;
    std::shared_ptr<DeformationData> deformation;
    GLuint vertexBuffer = 0;
    GLuint indexBuffer = 0;
    bool dynamic = false;

    size_t vertexCount() const {
        return vertices.size() / kFloatsPerVertex;
    }
};

struct GPUResources {
    GLuint materialProgram = 0;
    GLuint highlightedMaterialProgram = 0;
    GLuint pickingProgram = 0;
    GLuint grayTexture = 0;
    GLuint metalTexture = 0;
    bool astcTextures = false;
    bool uintIndices = false;
};

struct ModelResources {
    std::vector<ModelPart> parts;
    std::unordered_map<std::string, std::vector<size_t>> groups;
    GPUResources gpu;
    size_t totalVertexCount = 0;
    size_t totalIndexCount = 0;
    size_t sourceBytes = 0;
};

using ModelResourcesPtr = std::shared_ptr<ModelResources>;

} // namespace v3
} // namespace motorica
