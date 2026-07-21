/*
See LICENSE folder for this sample’s licensing information.

Abstract:
Implementation of the renderer class that performs OpenGL state setup and per-frame rendering.
*/

#import "AAPLOpenGLRendererV3.h"
#import "AAPLMathUtilities.h"
#import "V3ModelResourceCache.h"
#import "V3ModelResourceCacheInternal.hpp"
#include "V3VolumeRodDeformer.hpp"
#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import <CoreBluetooth/CoreBluetooth.h>
#import <CoreData/CoreData.h>
#import <QuartzCore/QuartzCore.h>
#import <os/log.h>
#import <os/signpost.h>
#import <simd/simd.h>
#import "MotoricaStart-Swift.h"
#import <shared/shared.h>

#include <algorithm>
#include <array>
#include <cmath>
#include <memory>
#include <string>
#include <unordered_map>
#include <vector>

namespace {

using motorica::v3::DeformationKind;
using motorica::v3::ModelPart;
using motorica::v3::ModelResourcesPtr;
using motorica::v3::VolumeRodDeformer;

constexpr int kV3FingerCount = 6;
constexpr int kV3InfluenceCount = 6;
constexpr int kV3PalmInfluence = 0;
constexpr int kV3IndexInfluence = 1;
constexpr int kV3MiddleInfluence = 2;
constexpr int kV3RingInfluence = 3;
constexpr int kV3LittleInfluence = 4;
constexpr int kV3ThumbInfluence = 5;
constexpr float kV3ThumbSecondAxisInitialDegrees = -34.0f;
constexpr GLsizei kV3VertexStride = motorica::v3::kFloatsPerVertex * sizeof(float);

enum class V3Material {
    whitePlastic,
    rubber,
    chrome,
};

struct V3ProgramLocations {
    GLuint program = 0;
    GLint mvp = -1;
    GLint mv = -1;
    GLint light = -1;
    GLint texture = -1;
    GLint normalMap = -1;
    GLint useNormalMap = -1;
    GLint specular = -1;
    GLint lightPower = -1;
    GLint ambient = -1;
    GLint materialMode = -1;
    GLint chromeStrength = -1;
    GLint fillDirection = -1;
    GLint rimDirection = -1;
    GLint fillStrength = -1;
    GLint rimStrength = -1;
    GLint chromeToneMapStrength = -1;
    GLint mirrored = -1;
    GLint useSolidColor = -1;
    GLint solidColor = -1;
    GLint useBlueSelection = -1;
    GLint code = -1;
    GLint position = -1;
    GLint normal = -1;
    GLint color = -1;
    GLint texcoord = -1;
    GLint tangent = -1;
    GLint bitangent = -1;
};

struct V3TransitionState {
    bool active = false;
    bool targetClosed = false;
    CFTimeInterval startedAt = 0.0;
    std::array<float, kV3FingerCount> start{};
    std::array<float, kV3FingerCount> target{};
    std::array<CFTimeInterval, kV3FingerCount> delay{};
};

struct V3DigitMatrices {
    matrix_float4x4 proximal = matrix_identity_float4x4;
    matrix_float4x4 distal = matrix_identity_float4x4;
};

struct V3RendererState {
    ModelResourcesPtr resources;
    V3ProgramLocations material;
    V3ProgramLocations highlighted;
    V3ProgramLocations picking;
    matrix_float4x4 projection = matrix_identity_float4x4;
    matrix_float4x4 view = matrix_identity_float4x4;
    matrix_float4x4 generalRotation = matrix_identity_float4x4;
    std::array<matrix_float4x4, kV3InfluenceCount> anchors{};
    std::array<matrix_float4x4, kV3InfluenceCount> inverseBind{};
    std::array<matrix_float4x4, kV3InfluenceCount> skin{};
    std::array<float, kV3FingerCount> positions{};
    std::vector<std::vector<float>> dynamicVertices;
    std::vector<std::unique_ptr<VolumeRodDeformer>> volumeRodDeformers;
    V3TransitionState transition;
    CGSize size = CGSizeZero;
    NSInteger handSide = 1;
    int selectedFinger = 0;
    float touchX = 0.0f;
    float touchY = 0.0f;
    float pendingDeltaX = 0.0f;
    float pendingDeltaY = 0.0f;
    bool bindCaptured = false;
    bool deformationDirty = true;
    bool firstFrameRendered = false;
    CFTimeInterval createdAt = CACurrentMediaTime();
    GLuint pickingFramebuffer = 0;
    GLuint pickingTexture = 0;
    GLuint pickingDepth = 0;
    GLsizei pickingWidth = 0;
    GLsizei pickingHeight = 0;
};

static os_log_t V3RendererLog() {
    static os_log_t log = os_log_create("com.bailout.stickk", "V3Renderer");
    return log;
}

static float V3Radians(float degrees) {
    return degrees * static_cast<float>(M_PI / 180.0);
}

static float V3Clamp(float value, float minimum, float maximum) {
    return std::max(minimum, std::min(maximum, value));
}

static matrix_float4x4 V3Multiply(matrix_float4x4 left, matrix_float4x4 right) {
    return matrix_multiply(left, right);
}

static vector_float4 V3Multiply(matrix_float4x4 matrix, vector_float4 vector) {
    return matrix_multiply(matrix, vector);
}

static matrix_float4x4 V3Rotation(float degrees, float x, float y, float z) {
    return matrix4x4_rotation(V3Radians(degrees), x, y, z);
}

static matrix_float4x4 V3InitialGeneralRotation(NSInteger handSide) {
    float initialY = handSide == 0 ? 95.0f : -95.0f;
    return V3Multiply(V3Rotation(initialY, 0.0f, 1.0f, 0.0f),
                      V3Rotation(90.0f, 0.0f, 0.0f, 1.0f));
}

static matrix_float4x4 V3TiltedZRotation(float angleDegrees,
                                         float tiltXDegrees,
                                         float tiltYDegrees,
                                         bool mirrored) {
    float transformedTiltX = mirrored ? -tiltXDegrees : tiltXDegrees;
    float transformedAngle = mirrored ? -angleDegrees : angleDegrees;
    matrix_float4x4 result = matrix4x4_identity();
    result = V3Multiply(result, V3Rotation(transformedTiltX, 1.0f, 0.0f, 0.0f));
    result = V3Multiply(result, V3Rotation(tiltYDegrees, 0.0f, 1.0f, 0.0f));
    result = V3Multiply(result, V3Rotation(transformedAngle, 0.0f, 0.0f, 1.0f));
    result = V3Multiply(result, V3Rotation(-tiltYDegrees, 0.0f, 1.0f, 0.0f));
    result = V3Multiply(result, V3Rotation(-transformedTiltX, 1.0f, 0.0f, 0.0f));
    return result;
}

static V3ProgramLocations V3LoadProgramLocations(GLuint program) {
    V3ProgramLocations locations;
    locations.program = program;
    locations.mvp = glGetUniformLocation(program, "u_MVPMatrix");
    locations.mv = glGetUniformLocation(program, "u_MVMatrix");
    locations.light = glGetUniformLocation(program, "u_LightPos");
    locations.texture = glGetUniformLocation(program, "u_Texture");
    locations.normalMap = glGetUniformLocation(program, "u_normalMap");
    locations.useNormalMap = glGetUniformLocation(program, "u_isUsingNormalMap");
    locations.specular = glGetUniformLocation(program, "u_specularFactor");
    locations.lightPower = glGetUniformLocation(program, "u_lightPower");
    locations.ambient = glGetUniformLocation(program, "u_ambientFactor");
    locations.materialMode = glGetUniformLocation(program, "u_MaterialMode");
    locations.chromeStrength = glGetUniformLocation(program, "u_ChromeStrength");
    locations.fillDirection = glGetUniformLocation(program, "u_MetalFillLightDirection");
    locations.rimDirection = glGetUniformLocation(program, "u_MetalRimLightDirection");
    locations.fillStrength = glGetUniformLocation(program, "u_MetalFillLightStrength");
    locations.rimStrength = glGetUniformLocation(program, "u_MetalRimLightStrength");
    locations.chromeToneMapStrength = glGetUniformLocation(program, "u_ChromeToneMapStrength");
    locations.mirrored = glGetUniformLocation(program, "u_FrontFaceMirrored");
    locations.useSolidColor = glGetUniformLocation(program, "u_UseSolidColor");
    locations.solidColor = glGetUniformLocation(program, "u_SolidColor");
    locations.useBlueSelection = glGetUniformLocation(program, "u_UseBlueSelection");
    locations.code = glGetUniformLocation(program, "u_Code");
    locations.position = glGetAttribLocation(program, "a_Position");
    locations.normal = glGetAttribLocation(program, "a_Normal");
    locations.color = glGetAttribLocation(program, "a_Color");
    locations.texcoord = glGetAttribLocation(program, "a_TexCoordinate");
    locations.tangent = glGetAttribLocation(program, "a_TangentIn");
    locations.bitangent = glGetAttribLocation(program, "a_BitangentIn");
    return locations;
}

static matrix_float4x4 V3MatrixAroundPivot(matrix_float4x4 rotation,
                                            vector_float3 pivot) {
    return V3Multiply(matrix4x4_translation(pivot),
                      V3Multiply(rotation, matrix4x4_translation(-pivot)));
}

static float V3TransitionPosition(float start,
                                  float target,
                                  CFTimeInterval delay,
                                  CFTimeInterval elapsed,
                                  bool *complete) {
    CFTimeInterval duration = std::fabs(target - start) * 0.005;
    float progress = duration <= 0.0
        ? 1.0f
        : static_cast<float>((elapsed - delay) / duration);
    progress = V3Clamp(progress, 0.0f, 1.0f);
    if (complete && progress < 1.0f) *complete = false;
    float eased = (std::cos((progress + 1.0f) * static_cast<float>(M_PI)) * 0.5f) + 0.5f;
    return start + (target - start) * eased;
}

static NSArray<NSNumber *> *V3MatrixNumbers(matrix_float4x4 matrix) {
    const float *values = reinterpret_cast<const float *>(&matrix);
    NSMutableArray<NSNumber *> *result = [NSMutableArray arrayWithCapacity:16];
    for (int index = 0; index < 16; ++index) {
        [result addObject:@(values[index])];
    }
    return [result copy];
}

static int V3SelectionCodeForInfluence(int influence) {
    switch (influence) {
        case kV3IndexInfluence: return 4;
        case kV3MiddleInfluence: return 3;
        case kV3RingInfluence: return 2;
        case kV3LittleInfluence: return 1;
        case kV3ThumbInfluence: return 5;
        default: return 0;
    }
}

} // namespace

@implementation AAPLOpenGLRendererV3
{
    std::unique_ptr<V3RendererState> _v3;
    GLuint _defaultFBOName;
    CGSize _viewSize;
    float _xCoefficient;
    float _yCoefficient;

    GestureService *_gestureService;
    SharedGestureWithAddress *_gestureWithAddress;
    NSInteger _gestureNumber;
    NSString *_gestureSettingsParameterData;
    NSInteger _handSide;

    NSInteger openStage1;
    NSInteger openStage2;
    NSInteger openStage3;
    NSInteger openStage4;
    NSInteger openStage5;
    NSInteger openStage6;

    NSInteger closeStage1;
    NSInteger closeStage2;
    NSInteger closeStage3;
    NSInteger closeStage4;
    NSInteger closeStage5;
    NSInteger closeStage6;

    NSInteger openToCloseTimeShift1;
    NSInteger openToCloseTimeShift2;
    NSInteger openToCloseTimeShift3;
    NSInteger openToCloseTimeShift4;
    NSInteger openToCloseTimeShift5;
    NSInteger openToCloseTimeShift6;

    NSInteger closeToOpenTimeShift1;
    NSInteger closeToOpenTimeShift2;
    NSInteger closeToOpenTimeShift3;
    NSInteger closeToOpenTimeShift4;
    NSInteger closeToOpenTimeShift5;
    NSInteger closeToOpenTimeShift6;

    bool stateGesture;
}

- (instancetype) initWithDefaultFBOName:(GLuint)defaultFBOName
                        gestureNumber:(NSInteger)gestureNumber {
    self = [super init];
    if (!self) return nil;

    _gestureNumber = gestureNumber;
    _defaultFBOName = defaultFBOName;
    _gestureService = [[GestureService alloc] init];
    [[V3HandSideProvider shared] startObserving];
    _handSide = [V3HandSideProvider shared].currentSide;
    if (_handSide != 0 && _handSide != 1) {
        os_log_error(V3RendererLog(), "V3 hand side is missing from ParameterStoreV3");
        return nil;
    }

    NSString *emptyGestureData = @"{}";
    SharedGesture *emptyGesture = [_gestureService decodeGestureSettingsV3WithRaw:emptyGestureData];
    if (emptyGesture == nil) {
        os_log_error(V3RendererLog(), "Could not create empty V3 gesture state");
        return nil;
    }
    _gestureWithAddress = [[SharedGestureWithAddress alloc] initWithAddressDevice:0
                                                               parameterID:0
                                                                   gesture:emptyGesture
                                                              gestureState:0];

    _v3 = std::make_unique<V3RendererState>();
    _v3->resources = motorica::v3::SharedModelResources();
    if (!_v3->resources) {
        os_log_error(V3RendererLog(), "V3 renderer created before resource cache became ready");
        return nil;
    }
    _v3->handSide = _handSide == 0 ? 0 : 1;
    _v3->view = matrix_look_at_right_hand((vector_float3){0.0f, 0.0f, 160.0f},
                                          (vector_float3){0.0f, 0.0f, 0.0f},
                                          (vector_float3){0.0f, 1.0f, 0.0f});
    _v3->generalRotation = V3InitialGeneralRotation(_v3->handSide);
    _v3->material = V3LoadProgramLocations(_v3->resources->gpu.materialProgram);
    _v3->highlighted = V3LoadProgramLocations(_v3->resources->gpu.highlightedMaterialProgram);
    _v3->picking = V3LoadProgramLocations(_v3->resources->gpu.pickingProgram);
    _v3->dynamicVertices.resize(_v3->resources->parts.size());
    _v3->volumeRodDeformers.resize(_v3->resources->parts.size());
    for (size_t index = 0; index < _v3->resources->parts.size(); ++index) {
        ModelPart &part = _v3->resources->parts[index];
        if (part.dynamic) {
            _v3->dynamicVertices[index] = part.vertices;
        }
        if (part.deformation && part.deformation->kind == DeformationKind::volumeRod) {
            std::unique_ptr<VolumeRodDeformer> deformer(new VolumeRodDeformer(part));
            if (deformer->isValid()) {
                _v3->volumeRodDeformers[index] = std::move(deformer);
            } else {
                os_log_error(V3RendererLog(), "Invalid volume rod data for part %{public}s", part.id.c_str());
            }
        }
    }
    for (int index = 0; index < kV3InfluenceCount; ++index) {
        _v3->anchors[index] = matrix4x4_identity();
        _v3->inverseBind[index] = matrix4x4_identity();
        _v3->skin[index] = matrix4x4_identity();
    }
    NSLog(@"[V3Renderer] initialized gesture=%ld side=%@ parts=%zu vertices=%zu indices=%zu",
          (long)_gestureNumber,
          _v3->handSide == 0 ? @"left" : @"right",
          _v3->resources->parts.size(),
          _v3->resources->totalVertexCount,
          _v3->resources->totalIndexCount);
    return self;
}

- (void)setHandSide:(NSInteger)side {
    if (!_v3) return;
    if (side != 0 && side != 1) return;
    NSInteger normalized = side;
    NSInteger previous = _v3->handSide;
    _v3->handSide = normalized;
    _handSide = normalized;
    _v3->generalRotation = V3InitialGeneralRotation(_v3->handSide);
    _v3->bindCaptured = false;
    _v3->deformationDirty = true;
    NSLog(@"[V3HandSide] source=renderer applied previous=%@ side=%@",
          previous == 0 ? @"left" : @"right",
          normalized == 0 ? @"left" : @"right");
}

- (void)v3SynchronizeHandSideFromProvider {
    if (!_v3) return;
    NSInteger providerSide = [V3HandSideProvider shared].currentSide;
    if (providerSide != 0 && providerSide != 1) return;
    if (providerSide != _v3->handSide) {
        NSLog(@"[V3HandSide] source=renderer sync provider=%@ renderer=%@",
              providerSide == 0 ? @"left" : @"right",
              _v3->handSide == 0 ? @"left" : @"right");
        [self setHandSide:providerSide];
    }
}

- (BOOL)isAnimating {
    return _v3 && _v3->transition.active;
}

- (matrix_float4x4)v3MirrorMatrix {
    return _v3->handSide == 0
        ? matrix4x4_scale(1.0f, -1.0f, 1.0f)
        : matrix4x4_identity();
}

- (matrix_float4x4)v3HandBaseMatrix {
    return V3Multiply(_v3->generalRotation, [self v3MirrorMatrix]);
}

- (V3DigitMatrices)v3DigitMatricesWithPercent:(float)percent
                                  proximalStart:(vector_float3)proximalStart
                                    distalStart:(vector_float3)distalStart
                                      linkOffset:(vector_float3)linkOffset
                                     finalOffset:(vector_float3)finalOffset
                                          tiltX1:(float)tiltX1
                                          tiltY1:(float)tiltY1
                                          tiltX2:(float)tiltX2
                                          tiltY2:(float)tiltY2 {
    bool mirrored = _v3->handSide == 0;
    matrix_float4x4 mirror = [self v3MirrorMatrix];
    matrix_float4x4 firstRotation = V3TiltedZRotation(-percent, tiltX1, tiltY1, mirrored);
    matrix_float4x4 secondRotation = V3TiltedZRotation(-percent, tiltX2, tiltY2, mirrored);

    V3DigitMatrices matrices;
    matrices.proximal = V3Multiply(
        _v3->generalRotation,
        V3Multiply(matrix4x4_translation(finalOffset),
                   V3Multiply(firstRotation,
                              V3Multiply(mirror, matrix4x4_translation(proximalStart)))));
    matrices.distal = V3Multiply(
        _v3->generalRotation,
        V3Multiply(matrix4x4_translation(finalOffset),
                   V3Multiply(firstRotation,
                              V3Multiply(matrix4x4_translation(linkOffset),
                                         V3Multiply(secondRotation,
                                                    V3Multiply(mirror, matrix4x4_translation(distalStart)))))));
    return matrices;
}

- (V3DigitMatrices)v3IndexMatrices {
    float finalY = _v3->handSide == 0 ? 2.0f : -2.0f;
    return [self v3DigitMatricesWithPercent:_v3->positions[3]
                              proximalStart:(vector_float3){-10.0f, 2.0f, 29.0f}
                                distalStart:(vector_float3){-41.0f, 2.0f, 29.0f}
                                  linkOffset:(vector_float3){31.0f, 0.0f, 0.0f}
                                 finalOffset:(vector_float3){10.0f, finalY, -29.0f}
                                      tiltX1:-4.0f
                                      tiltY1:4.0f
                                      tiltX2:-4.0f
                                      tiltY2:4.0f];
}

- (V3DigitMatrices)v3MiddleMatrices {
    return [self v3DigitMatricesWithPercent:_v3->positions[2]
                              proximalStart:(vector_float3){-12.0f, 0.0f, -11.0f}
                                distalStart:(vector_float3){-46.5f, 0.0f, -11.0f}
                                  linkOffset:(vector_float3){34.5f, 0.0f, 0.0f}
                                 finalOffset:(vector_float3){12.0f, 0.0f, 11.0f}
                                      tiltX1:0.0f
                                      tiltY1:-1.0f
                                      tiltX2:0.0f
                                      tiltY2:-1.0f];
}

- (V3DigitMatrices)v3RingMatrices {
    return [self v3DigitMatricesWithPercent:_v3->positions[1]
                              proximalStart:(vector_float3){-9.0f, 0.0f, 8.0f}
                                distalStart:(vector_float3){-43.0f, 0.0f, 8.0f}
                                  linkOffset:(vector_float3){34.0f, 0.0f, 0.0f}
                                 finalOffset:(vector_float3){9.0f, 0.0f, -8.0f}
                                      tiltX1:7.0f
                                      tiltY1:-6.0f
                                      tiltX2:6.0f
                                      tiltY2:-3.0f];
}

- (V3DigitMatrices)v3LittleMatrices {
    float finalY = _v3->handSide == 0 ? -10.0f : 10.0f;
    return [self v3DigitMatricesWithPercent:_v3->positions[0]
                              proximalStart:(vector_float3){-6.0f, -10.0f, 25.0f}
                                distalStart:(vector_float3){-39.0f, -10.0f, 25.0f}
                                  linkOffset:(vector_float3){33.0f, 0.0f, 0.0f}
                                 finalOffset:(vector_float3){6.0f, finalY, -25.0f}
                                      tiltX1:16.0f
                                      tiltY1:-8.0f
                                      tiltX2:16.0f
                                      tiltY2:-8.0f];
}

- (float)v3ThumbAngleForPercent:(float)percent minimum:(float)minimum maximum:(float)maximum {
    return roundf(maximum - V3Clamp(percent, 0.0f, 100.0f) * (maximum - minimum) / 100.0f);
}

- (matrix_float4x4)v3ThumbCorrectedZRotation:(float)angle {
    float correction = _v3->handSide == 0 ? -34.0f : 34.0f;
    matrix_float4x4 result = matrix4x4_identity();
    result = V3Multiply(result, V3Rotation(correction, 1.0f, 0.0f, 0.0f));
    result = V3Multiply(result, V3Rotation(angle, 0.0f, 0.0f, -1.0f));
    result = V3Multiply(result, V3Rotation(-correction, 1.0f, 0.0f, 0.0f));
    return result;
}

- (matrix_float4x4)v3ThumbSecondAxisRotation:(float)angle {
    matrix_float4x4 result = matrix4x4_identity();
    result = V3Multiply(result, V3Rotation(34.0f, 1.0f, 0.0f, 0.0f));
    result = V3Multiply(result, V3Rotation(angle, 1.0f, 0.0f, 0.0f));
    result = V3Multiply(result, V3Rotation(-34.0f, 1.0f, 0.0f, 0.0f));
    return result;
}

- (matrix_float4x4)v3ThumbMatrixIncludingSecondPhalanx:(BOOL)includeSecondPhalanx {
    bool mirrored = _v3->handSide == 0;
    float firstAngle = [self v3ThumbAngleForPercent:_v3->positions[4] minimum:-35.0f maximum:49.0f];
    float secondAngle = [self v3ThumbAngleForPercent:_v3->positions[5] minimum:-68.0f maximum:22.0f];
    float secondRotationAngle = secondAngle - kV3ThumbSecondAxisInitialDegrees;
    float phalanxAngle = [self v3ThumbAngleForPercent:_v3->positions[4] minimum:-25.0f maximum:20.0f];
    float sideSign = mirrored ? -1.0f : 1.0f;

    matrix_float4x4 model = [self v3MirrorMatrix];
    if (includeSecondPhalanx) {
        vector_float3 pivot = {-18.0f, mirrored ? 52.430767f : -52.430767f, -49.062533f};
        matrix_float4x4 staticOffset = [self v3ThumbCorrectedZRotation:mirrored ? 20.0f : -20.0f];
        matrix_float4x4 dynamicRotation = [self v3ThumbCorrectedZRotation:sideSign * phalanxAngle];
        model = V3Multiply(V3MatrixAroundPivot(staticOffset, pivot), model);
        model = V3Multiply(V3MatrixAroundPivot(dynamicRotation, pivot), model);
    }

    vector_float3 firstPivot = {-40.648183f, mirrored ? 27.336317f : -27.336317f, -31.565383f};
    vector_float3 secondPivot = {-65.678083f, mirrored ? 18.191633f : -18.191633f, -28.560333f};
    matrix_float4x4 firstRotation = [self v3ThumbCorrectedZRotation:sideSign * firstAngle];
    matrix_float4x4 secondRotation = [self v3ThumbSecondAxisRotation:sideSign * secondRotationAngle];
    model = V3Multiply(V3MatrixAroundPivot(firstRotation, firstPivot), model);
    model = V3Multiply(V3MatrixAroundPivot(secondRotation, secondPivot), model);
    return V3Multiply(_v3->generalRotation, model);
}

- (int)v3InfluenceIndexForTransformId:(const std::string &)transformId {
    if (transformId == "index_upper") return kV3IndexInfluence;
    if (transformId == "middle_upper") return kV3MiddleInfluence;
    if (transformId == "ring_upper") return kV3RingInfluence;
    if (transformId == "little_upper") return kV3LittleInfluence;
    if (transformId == "thumb_upper") return kV3ThumbInfluence;
    return kV3PalmInfluence;
}

- (void)v3PopulateCurrentAnchors {
    matrix_float4x4 inverseBase = simd_inverse([self v3HandBaseMatrix]);
    V3DigitMatrices index = [self v3IndexMatrices];
    V3DigitMatrices middle = [self v3MiddleMatrices];
    V3DigitMatrices ring = [self v3RingMatrices];
    V3DigitMatrices little = [self v3LittleMatrices];
    _v3->anchors[kV3PalmInfluence] = matrix4x4_identity();
    _v3->anchors[kV3IndexInfluence] = V3Multiply(inverseBase, index.proximal);
    _v3->anchors[kV3MiddleInfluence] = V3Multiply(inverseBase, middle.proximal);
    _v3->anchors[kV3RingInfluence] = V3Multiply(inverseBase, ring.proximal);
    _v3->anchors[kV3LittleInfluence] = V3Multiply(inverseBase, little.proximal);
    _v3->anchors[kV3ThumbInfluence] = V3Multiply(inverseBase, [self v3ThumbMatrixIncludingSecondPhalanx:NO]);
}

- (void)v3UpdateSkinMatrices {
    if (!_v3->bindCaptured) {
        std::array<float, kV3FingerCount> saved = _v3->positions;
        _v3->positions.fill(0.0f);
        [self v3PopulateCurrentAnchors];
        for (int influence = 0; influence < kV3InfluenceCount; ++influence) {
            // Android captures the exported thumb/bellows bind pose before any
            // mapped thumb angle is accumulated.
            _v3->inverseBind[influence] = influence == kV3ThumbInfluence
                ? matrix4x4_identity()
                : simd_inverse(_v3->anchors[influence]);
        }
        _v3->positions = saved;
        _v3->bindCaptured = true;
    }
    [self v3PopulateCurrentAnchors];
    for (int influence = 0; influence < kV3InfluenceCount; ++influence) {
        _v3->skin[influence] = V3Multiply(_v3->anchors[influence], _v3->inverseBind[influence]);
    }
}

- (void)v3TransformDirectionFrom:(const float *)source
                              by:(matrix_float4x4)matrix
                              to:(float *)target {
    vector_float4 transformed = V3Multiply(matrix, (vector_float4){source[0], source[1], source[2], 0.0f});
    vector_float3 direction = transformed.xyz;
    float length = simd_length(direction);
    if (length <= 0.000001f) {
        target[0] = source[0];
        target[1] = source[1];
        target[2] = source[2];
        return;
    }
    direction /= length;
    target[0] = direction.x;
    target[1] = direction.y;
    target[2] = direction.z;
}

- (void)v3WriteSelectionColorsForPart:(const ModelPart &)part
                                target:(std::vector<float> &)target {
    const auto &data = *part.deformation;
    for (size_t vertex = 0; vertex < part.vertexCount(); ++vertex) {
        size_t vertexOffset = vertex * motorica::v3::kFloatsPerVertex;
        size_t weightOffset = vertex * kV3InfluenceCount;
        int selectionInfluence = kV3PalmInfluence;
        if (vertex < data.selectionInfluences.size()) {
            selectionInfluence = data.selectionInfluences[vertex];
        } else {
            float dominant = 0.0001f;
            for (int slot = 1; slot < kV3InfluenceCount; ++slot) {
                if (data.weights[weightOffset + slot] > dominant) {
                    dominant = data.weights[weightOffset + slot];
                    selectionInfluence = [self v3InfluenceIndexForTransformId:data.transformIds[slot]];
                }
            }
        }
        target[vertexOffset + 6] = V3SelectionCodeForInfluence(selectionInfluence) / 255.0f;
        target[vertexOffset + 7] = 0.0f;
        target[vertexOffset + 8] = 0.0f;
        target[vertexOffset + 9] = 1.0f;
    }
}

- (void)v3UpdateLinearPart:(ModelPart &)part target:(std::vector<float> &)target {
    const auto &data = *part.deformation;
    target = part.vertices;
    for (size_t vertex = 0; vertex < part.vertexCount(); ++vertex) {
        size_t vertexOffset = vertex * motorica::v3::kFloatsPerVertex;
        size_t weightOffset = vertex * kV3InfluenceCount;
        vector_float4 sourcePosition = {
            part.vertices[vertexOffset], part.vertices[vertexOffset + 1],
            part.vertices[vertexOffset + 2], 1.0f
        };
        vector_float4 position = {0.0f, 0.0f, 0.0f, 0.0f};
        vector_float3 normal = {0.0f, 0.0f, 0.0f};
        vector_float3 tangent = {0.0f, 0.0f, 0.0f};
        vector_float3 bitangent = {0.0f, 0.0f, 0.0f};
        for (int slot = 0; slot < kV3InfluenceCount; ++slot) {
            float weight = data.weights[weightOffset + slot];
            if (weight <= 0.0f) continue;
            int influence = [self v3InfluenceIndexForTransformId:data.transformIds[slot]];
            matrix_float4x4 skin = _v3->skin[influence];
            position += V3Multiply(skin, sourcePosition) * weight;
            normal += V3Multiply(skin, (vector_float4){part.vertices[vertexOffset + 3], part.vertices[vertexOffset + 4], part.vertices[vertexOffset + 5], 0.0f}).xyz * weight;
            tangent += V3Multiply(skin, (vector_float4){part.vertices[vertexOffset + 12], part.vertices[vertexOffset + 13], part.vertices[vertexOffset + 14], 0.0f}).xyz * weight;
            bitangent += V3Multiply(skin, (vector_float4){part.vertices[vertexOffset + 15], part.vertices[vertexOffset + 16], part.vertices[vertexOffset + 17], 0.0f}).xyz * weight;
        }
        target[vertexOffset] = position.x;
        target[vertexOffset + 1] = position.y;
        target[vertexOffset + 2] = position.z;
        normal = simd_length_squared(normal) > 0.000001f ? simd_normalize(normal) : (vector_float3){0.0f, 0.0f, 1.0f};
        tangent = simd_length_squared(tangent) > 0.000001f ? simd_normalize(tangent) : (vector_float3){1.0f, 0.0f, 0.0f};
        bitangent = simd_length_squared(bitangent) > 0.000001f ? simd_normalize(bitangent) : simd_cross(normal, tangent);
        target[vertexOffset + 3] = normal.x;
        target[vertexOffset + 4] = normal.y;
        target[vertexOffset + 5] = normal.z;
        target[vertexOffset + 12] = tangent.x;
        target[vertexOffset + 13] = tangent.y;
        target[vertexOffset + 14] = tangent.z;
        target[vertexOffset + 15] = bitangent.x;
        target[vertexOffset + 16] = bitangent.y;
        target[vertexOffset + 17] = bitangent.z;

    }
    [self v3WriteSelectionColorsForPart:part target:target];
}

- (void)v3UpdateDeformablePartsIfNeeded {
    if (!_v3->deformationDirty) return;
    CFTimeInterval startedAt = CACurrentMediaTime();
    [self v3UpdateSkinMatrices];
    for (size_t index = 0; index < _v3->resources->parts.size(); ++index) {
        ModelPart &part = _v3->resources->parts[index];
        if (!part.dynamic || !part.deformation) continue;
        std::vector<float> &target = _v3->dynamicVertices[index];
        VolumeRodDeformer *volumeRod = _v3->volumeRodDeformers[index].get();
        if (volumeRod != nullptr) {
            int topSlot = volumeRod->topInfluenceSlot();
            int palmInfluence = [self v3InfluenceIndexForTransformId:part.deformation->transformIds[0]];
            int fingerInfluence = topSlot >= 0
                ? [self v3InfluenceIndexForTransformId:part.deformation->transformIds[topSlot]]
                : kV3PalmInfluence;
            if (!volumeRod->deform(part,
                                   _v3->skin[palmInfluence],
                                   _v3->skin[fingerInfluence],
                                   target)) {
                os_log_error(V3RendererLog(), "Volume rod deformation failed for %{public}s", part.id.c_str());
                [self v3UpdateLinearPart:part target:target];
            } else {
                [self v3WriteSelectionColorsForPart:part target:target];
            }
        } else {
            [self v3UpdateLinearPart:part target:target];
        }
        glBindBuffer(GL_ARRAY_BUFFER, part.vertexBuffer);
        glBufferSubData(GL_ARRAY_BUFFER,
                        0,
                        static_cast<GLsizeiptr>(target.size() * sizeof(float)),
                        target.data());
    }
    glBindBuffer(GL_ARRAY_BUFFER, 0);
    _v3->deformationDirty = false;
    NSLog(@"[V3Metrics] deformationMs=%.3f", (CACurrentMediaTime() - startedAt) * 1000.0);
}

- (void)v3ApplyMaterial:(V3Material)material
              locations:(const V3ProgramLocations &)locations {
    GLuint texture = _v3->resources->gpu.grayTexture;
    float specular = 1.5f;
    float lightPower = 650.0f;
    float ambient = 0.92f;
    GLint materialMode = 0;
    GLint solid = 0;
    vector_float4 solidColor = {1.0f, 1.0f, 1.0f, 1.0f};
    if (material == V3Material::whitePlastic) {
        specular = 8.0f;
        lightPower = 700.0f;
        ambient = 0.82f;
        solid = 1;
    } else if (material == V3Material::chrome) {
        texture = _v3->resources->gpu.metalTexture;
        specular = 40.0f;
        lightPower = 3600.0f;
        ambient = 1.5f;
        materialMode = 1;
    }
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, texture);
    if (locations.texture >= 0) glUniform1i(locations.texture, 0);
    if (locations.normalMap >= 0) glUniform1i(locations.normalMap, 0);
    if (locations.useNormalMap >= 0) glUniform1i(locations.useNormalMap, 0);
    if (locations.specular >= 0) glUniform1f(locations.specular, specular);
    if (locations.lightPower >= 0) glUniform1f(locations.lightPower, lightPower);
    if (locations.ambient >= 0) glUniform1f(locations.ambient, ambient);
    if (locations.materialMode >= 0) glUniform1i(locations.materialMode, materialMode);
    if (locations.chromeStrength >= 0) glUniform1f(locations.chromeStrength, materialMode == 1 ? 0.72f : 0.0f);
    if (locations.fillDirection >= 0) glUniform3f(locations.fillDirection, -0.74f, 0.46f, 0.49f);
    if (locations.rimDirection >= 0) glUniform3f(locations.rimDirection, 0.78f, 0.44f, -0.45f);
    if (locations.fillStrength >= 0) glUniform1f(locations.fillStrength, materialMode == 1 ? 0.82f : 0.0f);
    if (locations.rimStrength >= 0) glUniform1f(locations.rimStrength, materialMode == 1 ? 1.08f : 0.0f);
    if (locations.chromeToneMapStrength >= 0) glUniform1f(locations.chromeToneMapStrength, materialMode == 1 ? 1.0f : 0.0f);
    if (locations.mirrored >= 0) glUniform1i(locations.mirrored, _v3->handSide == 0 ? 1 : 0);
    if (locations.useSolidColor >= 0) glUniform1i(locations.useSolidColor, solid);
    if (locations.solidColor >= 0) glUniform4fv(locations.solidColor, 1, reinterpret_cast<const GLfloat *>(&solidColor));
}

- (void)v3BindAttributesForPart:(const ModelPart &)part
                      locations:(const V3ProgramLocations &)locations {
    glBindBuffer(GL_ARRAY_BUFFER, part.vertexBuffer);
    if (locations.position >= 0) {
        glVertexAttribPointer(locations.position, 3, GL_FLOAT, GL_FALSE, kV3VertexStride, reinterpret_cast<const GLvoid *>(0));
        glEnableVertexAttribArray(locations.position);
    }
    if (locations.normal >= 0) {
        glVertexAttribPointer(locations.normal, 3, GL_FLOAT, GL_FALSE, kV3VertexStride, reinterpret_cast<const GLvoid *>(3 * sizeof(float)));
        glEnableVertexAttribArray(locations.normal);
    }
    if (locations.color >= 0) {
        glVertexAttribPointer(locations.color, 4, GL_FLOAT, GL_FALSE, kV3VertexStride, reinterpret_cast<const GLvoid *>(6 * sizeof(float)));
        glEnableVertexAttribArray(locations.color);
    }
    if (locations.texcoord >= 0) {
        glVertexAttribPointer(locations.texcoord, 2, GL_FLOAT, GL_FALSE, kV3VertexStride, reinterpret_cast<const GLvoid *>(10 * sizeof(float)));
        glEnableVertexAttribArray(locations.texcoord);
    }
    if (locations.tangent >= 0) {
        glVertexAttribPointer(locations.tangent, 3, GL_FLOAT, GL_FALSE, kV3VertexStride, reinterpret_cast<const GLvoid *>(12 * sizeof(float)));
        glEnableVertexAttribArray(locations.tangent);
    }
    if (locations.bitangent >= 0) {
        glVertexAttribPointer(locations.bitangent, 3, GL_FLOAT, GL_FALSE, kV3VertexStride, reinterpret_cast<const GLvoid *>(15 * sizeof(float)));
        glEnableVertexAttribArray(locations.bitangent);
    }
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, part.indexBuffer);
}

- (void)v3SetMatricesForModel:(matrix_float4x4)model
                    locations:(const V3ProgramLocations &)locations {
    matrix_float4x4 mv = V3Multiply(_v3->view, model);
    matrix_float4x4 mvp = V3Multiply(_v3->projection, mv);
    if (locations.mv >= 0) glUniformMatrix4fv(locations.mv, 1, GL_FALSE, reinterpret_cast<const GLfloat *>(&mv));
    if (locations.mvp >= 0) glUniformMatrix4fv(locations.mvp, 1, GL_FALSE, reinterpret_cast<const GLfloat *>(&mvp));
    vector_float4 light = V3Multiply(_v3->view, (vector_float4){0.0f, 0.0f, 180.0f, 1.0f});
    if (locations.light >= 0) glUniform3f(locations.light, light.x, light.y, light.z);
}

- (void)v3DrawGroup:(const char *)groupName
               model:(matrix_float4x4)model
            material:(V3Material)material
          fingerCode:(int)fingerCode
             picking:(BOOL)picking {
    auto group = _v3->resources->groups.find(groupName);
    if (group == _v3->resources->groups.end()) return;
    bool highlighted = !picking && fingerCode > 0 && _v3->selectedFinger == fingerCode;
    const V3ProgramLocations &locations = picking
        ? _v3->picking
        : (highlighted ? _v3->highlighted : _v3->material);
    glUseProgram(locations.program);
    [self v3SetMatricesForModel:model locations:locations];
    if (picking) {
        if (locations.code >= 0) glUniform1f(locations.code, static_cast<float>(fingerCode));
    } else {
        [self v3ApplyMaterial:material locations:locations];
        if (locations.useBlueSelection >= 0) glUniform1i(locations.useBlueSelection, highlighted ? 1 : 0);
    }
    for (size_t partIndex : group->second) {
        if (partIndex >= _v3->resources->parts.size()) continue;
        const ModelPart &part = _v3->resources->parts[partIndex];
        [self v3BindAttributesForPart:part locations:locations];
        glDrawElements(GL_TRIANGLES,
                       static_cast<GLsizei>(part.indices.size()),
                       GL_UNSIGNED_INT,
                       nullptr);
    }
}

- (void)v3DrawRigidScenePicking:(BOOL)picking {
    V3DigitMatrices index = [self v3IndexMatrices];
    V3DigitMatrices middle = [self v3MiddleMatrices];
    V3DigitMatrices ring = [self v3RingMatrices];
    V3DigitMatrices little = [self v3LittleMatrices];
    matrix_float4x4 thumbFirst = [self v3ThumbMatrixIncludingSecondPhalanx:NO];
    matrix_float4x4 thumbSecond = [self v3ThumbMatrixIncludingSecondPhalanx:YES];

    [self v3DrawGroup:"little_rubber" model:little.distal material:V3Material::rubber fingerCode:1 picking:picking];
    [self v3DrawGroup:"little_upper_white_plastic" model:little.distal material:V3Material::whitePlastic fingerCode:1 picking:picking];
    [self v3DrawGroup:"little_upper_metal" model:little.distal material:V3Material::chrome fingerCode:1 picking:picking];
    [self v3DrawGroup:"little_lower_plastic" model:little.proximal material:V3Material::whitePlastic fingerCode:1 picking:picking];
    [self v3DrawGroup:"little_lower_metal" model:little.proximal material:V3Material::chrome fingerCode:1 picking:picking];

    [self v3DrawGroup:"ring_rubber" model:ring.distal material:V3Material::rubber fingerCode:2 picking:picking];
    [self v3DrawGroup:"ring_upper_white_plastic" model:ring.distal material:V3Material::whitePlastic fingerCode:2 picking:picking];
    [self v3DrawGroup:"ring_upper_metal" model:ring.distal material:V3Material::chrome fingerCode:2 picking:picking];
    [self v3DrawGroup:"ring_lower_plastic" model:ring.proximal material:V3Material::whitePlastic fingerCode:2 picking:picking];
    [self v3DrawGroup:"ring_lower_metal" model:ring.proximal material:V3Material::chrome fingerCode:2 picking:picking];

    [self v3DrawGroup:"middle_rubber" model:middle.distal material:V3Material::rubber fingerCode:3 picking:picking];
    [self v3DrawGroup:"middle_upper_white_plastic" model:middle.distal material:V3Material::whitePlastic fingerCode:3 picking:picking];
    [self v3DrawGroup:"middle_upper_metal" model:middle.distal material:V3Material::chrome fingerCode:3 picking:picking];
    [self v3DrawGroup:"middle_lower_plastic" model:middle.proximal material:V3Material::whitePlastic fingerCode:3 picking:picking];
    [self v3DrawGroup:"middle_lower_metal" model:middle.proximal material:V3Material::chrome fingerCode:3 picking:picking];

    [self v3DrawGroup:"index_rubber" model:index.distal material:V3Material::rubber fingerCode:4 picking:picking];
    [self v3DrawGroup:"index_upper_white_plastic" model:index.distal material:V3Material::whitePlastic fingerCode:4 picking:picking];
    [self v3DrawGroup:"index_upper_metal" model:index.distal material:V3Material::chrome fingerCode:4 picking:picking];
    [self v3DrawGroup:"index_lower_plastic" model:index.proximal material:V3Material::whitePlastic fingerCode:4 picking:picking];
    [self v3DrawGroup:"index_lower_metal" model:index.proximal material:V3Material::chrome fingerCode:4 picking:picking];

    [self v3DrawGroup:"thumb_white_plastic" model:thumbFirst material:V3Material::whitePlastic fingerCode:5 picking:picking];
    [self v3DrawGroup:"thumb_first_metal" model:thumbFirst material:V3Material::chrome fingerCode:5 picking:picking];
    [self v3DrawGroup:"thumb_second_metal" model:thumbSecond material:V3Material::chrome fingerCode:5 picking:picking];
    [self v3DrawGroup:"thumb_crown_white_plastic" model:thumbSecond material:V3Material::whitePlastic fingerCode:5 picking:picking];
    [self v3DrawGroup:"thumb_rubber" model:thumbSecond material:V3Material::rubber fingerCode:5 picking:picking];
}

- (void)v3DrawDeformablePicking:(BOOL)picking {
    matrix_float4x4 base = [self v3HandBaseMatrix];
    [self v3DrawGroup:"deformable_rubber"
                  model:base
               material:V3Material::rubber
             fingerCode:picking ? 0 : -1
                picking:picking];
}

- (void)v3RenderVisibleScene {
    glBindFramebuffer(GL_FRAMEBUFFER, _defaultFBOName);
    glViewport(0, 0, static_cast<GLsizei>(_v3->size.width), static_cast<GLsizei>(_v3->size.height));
    glEnable(GL_DEPTH_TEST);
    glDepthMask(GL_TRUE);
    glDepthFunc(GL_LESS);
    glColorMask(GL_TRUE, GL_TRUE, GL_TRUE, GL_TRUE);
    glDisable(GL_BLEND);
    glEnable(GL_DITHER);
    glDisable(GL_CULL_FACE);
    glClearColor(42.0f / 255.0f, 42.0f / 255.0f, 42.0f / 255.0f, 1.0f);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    [self v3DrawRigidScenePicking:NO];
    matrix_float4x4 base = [self v3HandBaseMatrix];
    [self v3DrawGroup:"base_white_plastic" model:base material:V3Material::whitePlastic fingerCode:-1 picking:NO];
    [self v3DrawGroup:"base_rubber" model:base material:V3Material::rubber fingerCode:-1 picking:NO];
    [self v3DrawGroup:"gofra_static" model:base material:V3Material::rubber fingerCode:-1 picking:NO];
    [self v3DrawDeformablePicking:NO];
}

- (BOOL)v3EnsurePickingFramebuffer {
    GLsizei width = static_cast<GLsizei>(_v3->size.width);
    GLsizei height = static_cast<GLsizei>(_v3->size.height);
    if (width <= 0 || height <= 0) return NO;
    if (_v3->pickingFramebuffer != 0 && _v3->pickingWidth == width && _v3->pickingHeight == height) return YES;
    if (_v3->pickingTexture) glDeleteTextures(1, &_v3->pickingTexture);
    if (_v3->pickingDepth) glDeleteRenderbuffers(1, &_v3->pickingDepth);
    if (_v3->pickingFramebuffer) glDeleteFramebuffers(1, &_v3->pickingFramebuffer);
    _v3->pickingTexture = _v3->pickingDepth = _v3->pickingFramebuffer = 0;

    glGenFramebuffers(1, &_v3->pickingFramebuffer);
    glBindFramebuffer(GL_FRAMEBUFFER, _v3->pickingFramebuffer);
    glGenTextures(1, &_v3->pickingTexture);
    glBindTexture(GL_TEXTURE_2D, _v3->pickingTexture);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, nullptr);
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, _v3->pickingTexture, 0);
    glGenRenderbuffers(1, &_v3->pickingDepth);
    glBindRenderbuffer(GL_RENDERBUFFER, _v3->pickingDepth);
    glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT16, width, height);
    glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, _v3->pickingDepth);
    BOOL complete = glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE;
    glBindFramebuffer(GL_FRAMEBUFFER, _defaultFBOName);
    if (complete) {
        _v3->pickingWidth = width;
        _v3->pickingHeight = height;
    }
    return complete;
}

- (int)v3SelectObject {
    if (![self v3EnsurePickingFramebuffer]) return 0;
    [self v3UpdateDeformablePartsIfNeeded];
    glBindFramebuffer(GL_FRAMEBUFFER, _v3->pickingFramebuffer);
    glViewport(0, 0, _v3->pickingWidth, _v3->pickingHeight);
    glEnable(GL_DEPTH_TEST);
    glDisable(GL_BLEND);
    glDisable(GL_DITHER);
    glDisable(GL_CULL_FACE);
    glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    [self v3DrawRigidScenePicking:YES];
    [self v3DrawDeformablePicking:YES];
    [self v3DrawGroup:"selection_surface"
                  model:[self v3HandBaseMatrix]
               material:V3Material::rubber
             fingerCode:51
                picking:YES];

    GLint x = static_cast<GLint>(V3Clamp(_v3->touchX * _xCoefficient, 0.0f, _v3->pickingWidth - 1.0f));
    GLint y = static_cast<GLint>(V3Clamp(_v3->pickingHeight - _v3->touchY * _yCoefficient, 0.0f, _v3->pickingHeight - 1.0f));
    uint8_t pixel[4] = {0, 0, 0, 0};
    glReadPixels(x, y, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, pixel);
    glBindFramebuffer(GL_FRAMEBUFFER, _defaultFBOName);
    return pixel[0] >= 1 && pixel[0] <= 5 ? pixel[0] : 0;
}

- (void)v3AdvanceTransitionAtTime:(CFTimeInterval)timestamp {
    if (!_v3->transition.active) return;
    bool complete = true;
    for (int finger = 0; finger < kV3FingerCount; ++finger) {
        _v3->positions[finger] = V3TransitionPosition(
            _v3->transition.start[finger],
            _v3->transition.target[finger],
            _v3->transition.delay[finger],
            timestamp - _v3->transition.startedAt,
            &complete
        );
    }
    _v3->deformationDirty = true;
    if (complete) _v3->transition.active = false;
}

- (void)v3Draw {
    if (!_v3 || !_v3->resources) return;
    [self v3SynchronizeHandSideFromProvider];
    [self v3AdvanceTransitionAtTime:CACurrentMediaTime()];
    [self v3UpdateDeformablePartsIfNeeded];
    [self v3RenderVisibleScene];
    if (!_v3->firstFrameRendered) {
        _v3->firstFrameRendered = true;
        CFTimeInterval firstFrameMs = (CACurrentMediaTime() - _v3->createdAt) * 1000.0;
        NSLog(@"[V3Metrics] rendererToFirstFrameMs=%.3f", firstFrameMs);
        os_signpost_event_emit(OS_LOG_DEFAULT, OS_SIGNPOST_ID_EXCLUSIVE, "V3FirstFrame", "milliseconds=%{public}.3f", firstFrameMs);
    }
}

- (void) resize:(CGSize)size {
    if (!_v3) return;
    _viewSize = size;
    _v3->size = size;
    float aspect = size.height > 0.0 ? static_cast<float>(size.width / size.height) : 1.0f;
    _v3->projection = matrix_perspective_frustum_right_hand(-aspect, aspect, -1.0f, 1.0f, 1.0f, 300.0f);
}

- (void)setDefaultFBOName:(GLuint)defaultFBOName {
    _defaultFBOName = defaultFBOName;
}

- (void)draw {
    [self v3Draw];
}

- (void) beginTouchIvent {
    if (!_v3) return;
    _v3->selectedFinger = [self v3SelectObject];
}

- (void) touchIvent:(CGFloat) X  :(CGFloat) Y :(CGFloat) deltaX :(CGFloat) deltaY {
    if (!_v3) return;
    _v3->touchX = static_cast<float>(X);
    _v3->touchY = static_cast<float>(Y);

    if (_v3->selectedFinger >= 1 && _v3->selectedFinger <= 4) {
        int positionIndex = _v3->selectedFinger - 1;
        _v3->positions[positionIndex] = V3Clamp(_v3->positions[positionIndex] + static_cast<float>(deltaY), 0.0f, 100.0f);
        _v3->deformationDirty = true;
        return;
    }
    if (_v3->selectedFinger == 5) {
        float firstAngle = [self v3ThumbAngleForPercent:_v3->positions[4] minimum:-35.0f maximum:49.0f];
        firstAngle = V3Clamp(firstAngle + static_cast<float>(deltaY), -35.0f, 49.0f);
        _v3->positions[4] = V3Clamp((49.0f - firstAngle) * 100.0f / 84.0f, 0.0f, 100.0f);

        float secondAngle = [self v3ThumbAngleForPercent:_v3->positions[5] minimum:-68.0f maximum:22.0f];
        secondAngle += (_v3->handSide == 0 ? -1.0f : 1.0f) * static_cast<float>(deltaX);
        secondAngle = V3Clamp(secondAngle, -68.0f, 22.0f);
        _v3->positions[5] = V3Clamp((22.0f - secondAngle) * 100.0f / 90.0f, 0.0f, 100.0f);
        _v3->deformationDirty = true;
        return;
    }

    matrix_float4x4 touchRotation = V3Rotation(static_cast<float>(deltaX), 0.0f, 1.0f, 0.0f);
    _v3->generalRotation = V3Multiply(touchRotation, _v3->generalRotation);
}

+ (NSInteger)validationRange:(NSInteger)inputNumber {
    NSInteger value = inputNumber;
    if (value > 100) {
        value = 100;
    }
    if (value < 0) {
        value = 0;
    }
    return value;
}

+ (NSInteger)rangeConversionWithInput:(NSInteger)inputNumber range:(NSInteger)range offset:(NSInteger)offset {
    NSInteger validatedInput = [self validationRange:inputNumber];
    float result = (float)validatedInput / 100.0f * (float)range;
    result = (float)range - result;
    result += (float)offset;
    return (NSInteger)result;
}

+ (NSInteger)inverseRangeConversionWithInput:(NSInteger)inputNumber range:(NSInteger)range offset:(NSInteger)offset {
    float result = (float)inputNumber / (float)range * 100.0f;
    result = (float)range - result;
    result += (float)offset;
    return (NSInteger)result;
}

+ (NSInteger)rawStageForThumbFlexTransfer:(NSInteger)transfer {
    NSInteger emitted = 100 - (NSInteger)(((float)(transfer + 60) / 90.0f) * 100.0f);
    NSInteger angleFromRenderer = 88 - emitted;
    NSInteger stateValue = (NSInteger)(((float)angleFromRenderer / 100.0f) * 91.0f) - 49;
    NSInteger rawStage = [self inverseRangeConversionWithInput:stateValue range:85 offset:-53];
    return [self validationRange:rawStage];
}

+ (NSInteger)rawStageForThumbRotationTransfer:(NSInteger)transfer {
    NSInteger emitted = 100 - (NSInteger)(((float)transfer / 90.0f) * 100.0f);
    NSInteger angleFromRenderer = 98 - emitted;
    NSInteger stateValue = (NSInteger)(((float)angleFromRenderer / 100.0f) * 90.0f);
    NSInteger rawStage = [self inverseRangeConversionWithInput:stateValue range:85 offset:15];
    return [self validationRange:rawStage];
}

+ (NSInteger)thumbFlexTransferForRawStage:(NSInteger)rawStage {
    return [self rangeConversionWithInput:rawStage range:90 offset:-59];
}

+ (NSInteger)thumbRotationTransferForRawStage:(NSInteger)rawStage {
    return [self rangeConversionWithInput:rawStage range:92 offset:-1];
}

+ (int32_t)runtimeGestureStateForClosed:(BOOL)isClosed {
    return isClosed ? 1 : 0;
}

+ (int32_t)transitionGestureStateForClosed:(BOOL)isClosed {
    return [self runtimeGestureStateForClosed:isClosed] + 128;
}

+ (int32_t)saveGestureState {
    return 255;
}

#if DEBUG
+ (NSDictionary<NSString *, NSArray<NSNumber *> *> *)matrixSnapshotsForTestingWithHandSide:(NSInteger)handSide
                                                                                   positions:(NSArray<NSNumber *> *)positions {
    if (positions.count < kV3FingerCount) return @{};
    AAPLOpenGLRendererV3 *renderer = [[AAPLOpenGLRendererV3 alloc] init];
    renderer->_v3 = std::make_unique<V3RendererState>();
    renderer->_v3->handSide = handSide == 0 ? 0 : 1;
    for (int index = 0; index < kV3FingerCount; ++index) {
        renderer->_v3->positions[index] = V3Clamp(positions[index].floatValue, 0.0f, 100.0f);
    }
    float initialY = renderer->_v3->handSide == 0 ? 95.0f : -95.0f;
    renderer->_v3->generalRotation = V3Multiply(
        V3Rotation(initialY, 0.0f, 1.0f, 0.0f),
        V3Rotation(90.0f, 0.0f, 0.0f, 1.0f)
    );

    V3DigitMatrices index = [renderer v3IndexMatrices];
    V3DigitMatrices middle = [renderer v3MiddleMatrices];
    V3DigitMatrices ring = [renderer v3RingMatrices];
    V3DigitMatrices little = [renderer v3LittleMatrices];
    return @{
        @"base": V3MatrixNumbers([renderer v3HandBaseMatrix]),
        @"indexProximal": V3MatrixNumbers(index.proximal),
        @"indexDistal": V3MatrixNumbers(index.distal),
        @"middleProximal": V3MatrixNumbers(middle.proximal),
        @"middleDistal": V3MatrixNumbers(middle.distal),
        @"ringProximal": V3MatrixNumbers(ring.proximal),
        @"ringDistal": V3MatrixNumbers(ring.distal),
        @"littleProximal": V3MatrixNumbers(little.proximal),
        @"littleDistal": V3MatrixNumbers(little.distal),
        @"thumbFirst": V3MatrixNumbers([renderer v3ThumbMatrixIncludingSecondPhalanx:NO]),
        @"thumbSecond": V3MatrixNumbers([renderer v3ThumbMatrixIncludingSecondPhalanx:YES]),
    };
}

+ (NSArray<NSNumber *> *)transitionPositionsForTestingFrom:(NSArray<NSNumber *> *)start
                                                     target:(NSArray<NSNumber *> *)target
                                                     delays:(NSArray<NSNumber *> *)delays
                                                    elapsed:(NSTimeInterval)elapsed {
    if (start.count < kV3FingerCount || target.count < kV3FingerCount || delays.count < kV3FingerCount) {
        return @[];
    }
    NSMutableArray<NSNumber *> *result = [NSMutableArray arrayWithCapacity:kV3FingerCount];
    for (int index = 0; index < kV3FingerCount; ++index) {
        [result addObject:@(V3TransitionPosition(
            start[index].floatValue,
            target[index].floatValue,
            std::max(0.0, delays[index].doubleValue) * 0.010,
            elapsed,
            nullptr
        ))];
    }
    return [result copy];
}
#endif

- (NSString *)v3GestureSendSituationForState:(int32_t)gestureState {
    switch (gestureState) {
        case 0:
            return @"RUNTIME_OPEN(0)";
        case 1:
            return @"RUNTIME_CLOSE(1)";
        case 128:
            return @"TRANSITION_OPEN(128)";
        case 129:
            return @"TRANSITION_CLOSE(129)";
        case 255:
            return @"SAVE(255)";
        default:
            return nil;
    }
}

- (void)logV3GestureObjectIfNeededBeforeSend {
    NSString *situation = [self v3GestureSendSituationForState:_gestureWithAddress.gestureState];
    if (situation == nil) {
        return;
    }
    SharedGesture *gesture = _gestureWithAddress.gesture;
    if (gesture == nil) {
        NSLog(@"[V3][BLE_SEND] situation=%@ gesture=nil addressDevice=%d parameterID=%d gestureState=%d",
              situation,
              _gestureWithAddress.addressDevice,
              _gestureWithAddress.parameterID,
              _gestureWithAddress.gestureState);
        return;
    }
    NSLog(@"[V3][BLE_SEND] situation=%@ gesture={gestureId=%d addressDevice=%d parameterID=%d open=[%d,%d,%d,%d,%d,%d] close=[%d,%d,%d,%d,%d,%d] openToClose=[%d,%d,%d,%d,%d,%d] closeToOpen=[%d,%d,%d,%d,%d,%d] gestureState=%d}",
          situation,
          gesture.gestureId, _gestureWithAddress.addressDevice, _gestureWithAddress.parameterID,
          gesture.openPosition1, gesture.openPosition2, gesture.openPosition3, gesture.openPosition4, gesture.openPosition5, gesture.openPosition6,
          gesture.closePosition1, gesture.closePosition2, gesture.closePosition3, gesture.closePosition4, gesture.closePosition5, gesture.closePosition6,
          gesture.openToCloseTimeShift1, gesture.openToCloseTimeShift2, gesture.openToCloseTimeShift3, gesture.openToCloseTimeShift4, gesture.openToCloseTimeShift5, gesture.openToCloseTimeShift6,
          gesture.closeToOpenTimeShift1, gesture.closeToOpenTimeShift2, gesture.closeToOpenTimeShift3, gesture.closeToOpenTimeShift4, gesture.closeToOpenTimeShift5, gesture.closeToOpenTimeShift6,
          _gestureWithAddress.gestureState);
}

- (void)v3CommitSelectedFinger {
    if (!_v3 || _gestureWithAddress.gesture == nil || _v3->selectedFinger == 0) return;
    SharedGesture *gesture = _gestureWithAddress.gesture;
    int little = static_cast<int>(lroundf(_v3->positions[0]));
    int ring = static_cast<int>(lroundf(_v3->positions[1]));
    int middle = static_cast<int>(lroundf(_v3->positions[2]));
    int index = static_cast<int>(lroundf(_v3->positions[3]));
    int thumbFlex = static_cast<int>(lroundf(_v3->positions[4]));
    int thumbRotation = static_cast<int>(lroundf(_v3->positions[5]));
    if (!stateGesture) {
        if (_v3->selectedFinger == 1) gesture.openPosition4 = little;
        if (_v3->selectedFinger == 2) gesture.openPosition3 = ring;
        if (_v3->selectedFinger == 3) gesture.openPosition2 = middle;
        if (_v3->selectedFinger == 4) gesture.openPosition1 = index;
        if (_v3->selectedFinger == 5) {
            gesture.openPosition5 = thumbFlex;
            gesture.openPosition6 = thumbRotation;
        }
    } else {
        if (_v3->selectedFinger == 1) gesture.closePosition4 = little;
        if (_v3->selectedFinger == 2) gesture.closePosition3 = ring;
        if (_v3->selectedFinger == 3) gesture.closePosition2 = middle;
        if (_v3->selectedFinger == 4) gesture.closePosition1 = index;
        if (_v3->selectedFinger == 5) {
            gesture.closePosition5 = thumbFlex;
            gesture.closePosition6 = thumbRotation;
        }
    }
    openStage1 = gesture.openPosition4;
    openStage2 = gesture.openPosition3;
    openStage3 = gesture.openPosition2;
    openStage4 = gesture.openPosition1;
    openStage5 = gesture.openPosition5;
    openStage6 = gesture.openPosition6;
    closeStage1 = gesture.closePosition4;
    closeStage2 = gesture.closePosition3;
    closeStage3 = gesture.closePosition2;
    closeStage4 = gesture.closePosition1;
    closeStage5 = gesture.closePosition5;
    closeStage6 = gesture.closePosition6;
    [self sendDataToFest];
}

- (void)endTouchIvent {
    [self v3CommitSelectedFinger];
}

- (void) calculationOfCoefficients:(CGFloat) width  :(CGFloat) height {
    _xCoefficient = _viewSize.width/width;
    _yCoefficient = _viewSize.height/height;
}

- (void) changeState :(BOOL) state {
    [self changeState:state sendTransitionCommand:YES];
}

- (void)v3StartTransitionToClosed:(BOOL)closed sendTransitionCommand:(BOOL)sendTransitionCommand {
    if (!_v3) return;
    stateGesture = closed;
    std::array<float, kV3FingerCount> target = closed
        ? std::array<float, kV3FingerCount>{
            static_cast<float>(closeStage1), static_cast<float>(closeStage2),
            static_cast<float>(closeStage3), static_cast<float>(closeStage4),
            static_cast<float>(closeStage5), static_cast<float>(closeStage6)}
        : std::array<float, kV3FingerCount>{
            static_cast<float>(openStage1), static_cast<float>(openStage2),
            static_cast<float>(openStage3), static_cast<float>(openStage4),
            static_cast<float>(openStage5), static_cast<float>(openStage6)};
    std::array<NSInteger, kV3FingerCount> shifts = closed
        ? std::array<NSInteger, kV3FingerCount>{
            openToCloseTimeShift4, openToCloseTimeShift3, openToCloseTimeShift2,
            openToCloseTimeShift1, openToCloseTimeShift5, openToCloseTimeShift6}
        : std::array<NSInteger, kV3FingerCount>{
            closeToOpenTimeShift4, closeToOpenTimeShift3, closeToOpenTimeShift2,
            closeToOpenTimeShift1, closeToOpenTimeShift5, closeToOpenTimeShift6};

    _v3->transition.active = true;
    _v3->transition.targetClosed = closed;
    _v3->transition.startedAt = CACurrentMediaTime();
    _v3->transition.start = _v3->positions;
    for (int finger = 0; finger < kV3FingerCount; ++finger) {
        _v3->transition.target[finger] = V3Clamp(target[finger], 0.0f, 100.0f);
        _v3->transition.delay[finger] = std::max<NSInteger>(0, shifts[finger]) * 0.010;
    }
    if (sendTransitionCommand) {
        _gestureWithAddress.gestureState = [AAPLOpenGLRendererV3 transitionGestureStateForClosed:closed];
        [self sendDataToFestPreservingGestureState];
    }
}

- (void)changeState:(BOOL)state sendTransitionCommand:(BOOL)sendTransitionCommand {
    [self v3StartTransitionToClosed:state sendTransitionCommand:sendTransitionCommand];
}

- (void) sendDataToFest {
    _gestureWithAddress.gestureState = [AAPLOpenGLRendererV3 runtimeGestureStateForClosed:stateGesture];
    [self sendDataToFestPreservingGestureState];
}

- (void) sendDataToFestPreservingGestureState {
    [self logV3GestureObjectIfNeededBeforeSend];
    SharedKotlinByteArray *command = [[SharedBLECommandsV3 shared] sendGestureInfoGestureWithAddress:_gestureWithAddress];
    [_gestureService sendDataToFestV3WithDataForWrite:command];
}

- (void) stopVC {
    NSLog(@"AAPLOpenGLRenderer   отправка изменения положения пальца (выход без сохраниения данных)");
    _gestureWithAddress.gestureState = [AAPLOpenGLRendererV3 runtimeGestureStateForClosed:NO];
    [self printGestureSettingsWithAddress];
    [self sendDataToFestPreservingGestureState];
    [self releaseGLResources];
}
- (void) stopVCWithSaveData {
    NSLog(@"AAPLOpenGLRenderer   отправка изменения положения пальца (выход с сохраниением данных)");
    _gestureWithAddress.gestureState = [AAPLOpenGLRendererV3 saveGestureState];
    [self printGestureSettingsWithAddress];
    [self sendDataToFestPreservingGestureState];
    [self releaseGLResources];
}
- (void) openFingersDelayDialog {

}
- (BOOL) currentGestureState {
    return stateGesture;
}
- (NSArray<NSNumber *> *)currentOpenToCloseShifts {
    return @[
        @(openToCloseTimeShift1),
        @(openToCloseTimeShift2),
        @(openToCloseTimeShift3),
        @(openToCloseTimeShift4),
        @(openToCloseTimeShift5),
        @(openToCloseTimeShift6)
    ];
}
- (void)applyOpenToCloseShifts:(NSArray<NSNumber *> *)values {
    if (values.count < 6) {
        return;
    }
    
    openToCloseTimeShift1 = values[0].intValue;
    openToCloseTimeShift2 = values[1].intValue;
    openToCloseTimeShift3 = values[2].intValue;
    openToCloseTimeShift4 = values[3].intValue;
    openToCloseTimeShift5 = values[4].intValue;
    openToCloseTimeShift6 = values[5].intValue;
    
    // синхронизация с моделью жеста (KMM / Swift)
    _gestureWithAddress.gesture.openToCloseTimeShift1 = (int32_t)openToCloseTimeShift1;
    _gestureWithAddress.gesture.openToCloseTimeShift2 = (int32_t)openToCloseTimeShift2;
    _gestureWithAddress.gesture.openToCloseTimeShift3 = (int32_t)openToCloseTimeShift3;
    _gestureWithAddress.gesture.openToCloseTimeShift4 = (int32_t)openToCloseTimeShift4;
    _gestureWithAddress.gesture.openToCloseTimeShift5 = (int32_t)openToCloseTimeShift5;
    _gestureWithAddress.gesture.openToCloseTimeShift6 = (int32_t)openToCloseTimeShift6;
    
    [self sendDataToFest];
}
- (NSArray<NSNumber *> *)currentCloseToOpenShifts {
    return @[
        @(closeToOpenTimeShift1),
        @(closeToOpenTimeShift2),
        @(closeToOpenTimeShift3),
        @(closeToOpenTimeShift4),
        @(closeToOpenTimeShift5),
        @(closeToOpenTimeShift6)
    ];
}
- (void)applyCloseToOpenShifts:(NSArray<NSNumber *> *)values {
    if (values.count < 6) {
        return;
    }
    
    closeToOpenTimeShift1 = values[0].intValue;
    closeToOpenTimeShift2 = values[1].intValue;
    closeToOpenTimeShift3 = values[2].intValue;
    closeToOpenTimeShift4 = values[3].intValue;
    closeToOpenTimeShift5 = values[4].intValue;
    closeToOpenTimeShift6 = values[5].intValue;
    
    // синхронизация с моделью жеста
    _gestureWithAddress.gesture.closeToOpenTimeShift1 = (int32_t)closeToOpenTimeShift1;
    _gestureWithAddress.gesture.closeToOpenTimeShift2 = (int32_t)closeToOpenTimeShift2;
    _gestureWithAddress.gesture.closeToOpenTimeShift3 = (int32_t)closeToOpenTimeShift3;
    _gestureWithAddress.gesture.closeToOpenTimeShift4 = (int32_t)closeToOpenTimeShift4;
    _gestureWithAddress.gesture.closeToOpenTimeShift5 = (int32_t)closeToOpenTimeShift5;
    _gestureWithAddress.gesture.closeToOpenTimeShift6 = (int32_t)closeToOpenTimeShift6;
    
    [self sendDataToFest];
}
- (void)releaseGLResources {
    if (!_v3) return;
    if (_v3->pickingTexture) glDeleteTextures(1, &_v3->pickingTexture);
    if (_v3->pickingDepth) glDeleteRenderbuffers(1, &_v3->pickingDepth);
    if (_v3->pickingFramebuffer) glDeleteFramebuffers(1, &_v3->pickingFramebuffer);
    _v3->pickingTexture = 0;
    _v3->pickingDepth = 0;
    _v3->pickingFramebuffer = 0;
    _v3.reset();
}

- (void)dealloc {
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

- (void) updateGestureSettings:(SharedParameterRef *)parameterRef
                 parameterData:(NSString *)parameterData {
    if (parameterRef == nil) {
        return;
    }
    _gestureWithAddress.addressDevice = parameterRef.addressDevice;
    _gestureWithAddress.parameterID = parameterRef.parameterID;
    _gestureSettingsParameterData = parameterData;
    SharedGesture *decodedGesture = [_gestureService decodeGestureSettingsV3WithRaw:_gestureSettingsParameterData];
    if (decodedGesture == nil) {
        return;
    }
    _gestureWithAddress.gesture = decodedGesture;
    NSDictionary<NSString *, NSNumber *> *distribution = [AAPLOpenGLRendererV3 stageDistributionForGesture:_gestureWithAddress.gesture];
    openStage1 = distribution[@"openStage1"].integerValue;
    openStage2 = distribution[@"openStage2"].integerValue;
    openStage3 = distribution[@"openStage3"].integerValue;
    openStage4 = distribution[@"openStage4"].integerValue;
    openStage5 = distribution[@"openStage5"].integerValue;
    openStage6 = distribution[@"openStage6"].integerValue;
    
    closeStage1 = distribution[@"closeStage1"].integerValue;
    closeStage2 = distribution[@"closeStage2"].integerValue;
    closeStage3 = distribution[@"closeStage3"].integerValue;
    closeStage4 = distribution[@"closeStage4"].integerValue;
    closeStage5 = distribution[@"closeStage5"].integerValue;
    closeStage6 = distribution[@"closeStage6"].integerValue;
    
    openToCloseTimeShift1 = _gestureWithAddress.gesture.openToCloseTimeShift1;
    openToCloseTimeShift2 = _gestureWithAddress.gesture.openToCloseTimeShift2;
    openToCloseTimeShift3 = _gestureWithAddress.gesture.openToCloseTimeShift3;
    openToCloseTimeShift4 = _gestureWithAddress.gesture.openToCloseTimeShift4;
    openToCloseTimeShift5 = _gestureWithAddress.gesture.openToCloseTimeShift5;
    openToCloseTimeShift6 = _gestureWithAddress.gesture.openToCloseTimeShift6;
    
    closeToOpenTimeShift1 = _gestureWithAddress.gesture.closeToOpenTimeShift1;
    closeToOpenTimeShift2 = _gestureWithAddress.gesture.closeToOpenTimeShift2;
    closeToOpenTimeShift3 = _gestureWithAddress.gesture.closeToOpenTimeShift3;
    closeToOpenTimeShift4 = _gestureWithAddress.gesture.closeToOpenTimeShift4;
    closeToOpenTimeShift5 = _gestureWithAddress.gesture.closeToOpenTimeShift5;
    closeToOpenTimeShift6 = _gestureWithAddress.gesture.closeToOpenTimeShift6;
    
    [self changeState:stateGesture sendTransitionCommand:NO];
    [self printGestureSettingsWithAddress];
}

+ (NSDictionary<NSString *, NSNumber *> *)stageDistributionForGesture:(SharedGesture *)gesture {
    if (gesture == nil) {
        return @{};
    }
    return @{
        @"openStage1": @(gesture.openPosition4),
        @"openStage2": @(gesture.openPosition3),
        @"openStage3": @(gesture.openPosition2),
        @"openStage4": @(gesture.openPosition1),
        @"openStage5": @(gesture.openPosition5),
        @"openStage6": @(gesture.openPosition6),
        @"closeStage1": @(gesture.closePosition4),
        @"closeStage2": @(gesture.closePosition3),
        @"closeStage3": @(gesture.closePosition2),
        @"closeStage4": @(gesture.closePosition1),
        @"closeStage5": @(gesture.closePosition5),
        @"closeStage6": @(gesture.closePosition6)
    };
}

- (void) printGestureSettingsWithAddress {
    NSLog(@"Gesture object: gestureId = %d "
          "addressDevice = %d parameterID = %d "
          "open = [%d,%d,%d,%d,%d,%d] "
          "close = [%d,%d,%d,%d,%d,%d] "
          "openToClose = [%d,%d,%d,%d,%d,%d] "
          "closeToOpen = [%d,%d,%d,%d,%d,%d] "
          "gestureState = %d",
          _gestureWithAddress.gesture.gestureId, _gestureWithAddress.addressDevice, _gestureWithAddress.parameterID,
          _gestureWithAddress.gesture.openPosition1, _gestureWithAddress.gesture.openPosition2, _gestureWithAddress.gesture.openPosition3,
          _gestureWithAddress.gesture.openPosition4, _gestureWithAddress.gesture.openPosition5, _gestureWithAddress.gesture.openPosition6,
          _gestureWithAddress.gesture.closePosition1, _gestureWithAddress.gesture.closePosition2, _gestureWithAddress.gesture.closePosition3,
          _gestureWithAddress.gesture.closePosition4, _gestureWithAddress.gesture.closePosition5, _gestureWithAddress.gesture.closePosition6,
          _gestureWithAddress.gesture.openToCloseTimeShift1, _gestureWithAddress.gesture.openToCloseTimeShift2, _gestureWithAddress.gesture.openToCloseTimeShift3,
          _gestureWithAddress.gesture.openToCloseTimeShift4, _gestureWithAddress.gesture.openToCloseTimeShift5, _gestureWithAddress.gesture.openToCloseTimeShift6,
          _gestureWithAddress.gesture.closeToOpenTimeShift1, _gestureWithAddress.gesture.closeToOpenTimeShift2, _gestureWithAddress.gesture.closeToOpenTimeShift3,
          _gestureWithAddress.gesture.closeToOpenTimeShift4, _gestureWithAddress.gesture.closeToOpenTimeShift5, _gestureWithAddress.gesture.closeToOpenTimeShift6,
          _gestureWithAddress.gestureState);
}
@end
