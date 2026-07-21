#pragma once

#include "V3ModelResources.hpp"

#include <simd/simd.h>

#include <memory>
#include <vector>

namespace motorica {
namespace v3 {

class VolumeRodDeformer {
public:
    explicit VolumeRodDeformer(const ModelPart &part);
    ~VolumeRodDeformer();

    VolumeRodDeformer(const VolumeRodDeformer &) = delete;
    VolumeRodDeformer &operator=(const VolumeRodDeformer &) = delete;

    bool isValid() const;
    int topInfluenceSlot() const;

    bool deform(const ModelPart &part,
                matrix_float4x4 palmMatrix,
                matrix_float4x4 fingerMatrix,
                std::vector<float> &target);

private:
    struct Runtime;
    std::unique_ptr<Runtime> runtime_;
};

} // namespace v3
} // namespace motorica
