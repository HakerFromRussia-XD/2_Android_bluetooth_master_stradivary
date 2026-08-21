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
    gestureObject,
    wood,
};

enum class V3CardClip {
    gestureKey,
    cupGrip,
    boardGrip,
    naturalPosition,
    fist,
    pointing,
    pinch,
    fist2,
    goat,
    tweezers,
    ok,
    classic,
    pinch2,
    callMe,
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
    GLint cardContrast = -1;
    GLint baseColorMultiplier = -1;
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
    GLint useVertexColor = -1;
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

struct V3GestureObjectState {
    bool cardMode = false;
    bool editingKey = false;
    bool clipActive = false;
    CFTimeInterval clipStartedAt = 0.0;
    GLuint vertexBuffer = 0;
    GLsizei vertexCount = 0;
    vector_float3 position = {21.17f, -26.50f, -32.22f};
    vector_float3 rotation = {-9.56f, 171.56f, 199.94f};
    float scale = 0.800f;
    float cardRotationX = -18.0f;
    float cardRotationY = -112.0f;
    float cardRotationZ = -76.0f;
    float cardScale = 1.405f;
    float cardPositionX = -25.00f;
    float cardPositionY = -40.66f;
    matrix_float4x4 cardRotationMatrix = matrix_identity_float4x4;
    bool cardRotationInitialized = false;
    V3CardClip cardClip = V3CardClip::gestureKey;
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
    V3GestureObjectState gestureObject;
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
    locations.cardContrast = glGetUniformLocation(program, "u_CardContrast");
    locations.baseColorMultiplier = glGetUniformLocation(program, "u_BaseColorMultiplier");
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
    locations.useVertexColor = glGetUniformLocation(program, "u_UseVertexColor");
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

static float V3Linear(float start, float target, float progress) {
    return start + (target - start) * V3Clamp(progress, 0.0f, 1.0f);
}

static NSDictionary<NSString *, id> *V3GestureKeyClipSample(double milliseconds) {
    double t = std::max(0.0, std::min(900.0, milliseconds));
    float thumb = -35.0f;
    if (t <= 300.0) {
        thumb = V3Linear(-35.0f, 7.0f, t / 300.0);
    } else if (t <= 600.0) {
        thumb = 7.0f;
    } else {
        thumb = V3Linear(7.0f, -35.0f, (t - 600.0) / 300.0);
    }
    return @{
        @"fingers": @[@100.0f, @100.0f, @100.0f, @100.0f, @(thumb), @22.0f],
        @"hand": @[@0, @0, @0, @0, @0, @0, @1],
        @"object": @[@(-60.0f), @(-22.0f), @(-30.0f), @0, @0, @180.0f, @1],
        @"complete": @(t >= 900.0)
    };
}

static NSDictionary<NSString *, id> *V3CupGripClipSample(double milliseconds) {
    double t = std::max(0.0, std::min(900.0, milliseconds));
    float progress = t <= 300.0 ? static_cast<float>(t / 300.0)
        : (t <= 600.0 ? 1.0f : static_cast<float>(1.0 - (t - 600.0) / 300.0));
    float index = V3Linear(55.0f, 0.0f, progress);
    float middle = V3Linear(58.0f, 0.0f, progress);
    float ring = V3Linear(60.0f, 0.0f, progress);
    float thumb = V3Linear(50.0f, 0.0f, progress);
    return @{
        // Internal order: little, ring, middle, index, thumb flex, thumb rotation.
        @"fingers": @[@100.0f, @(ring), @(middle), @(index), @(thumb), @100.0f],
        @"hand": @[@0, @0, @0, @0, @0, @0, @1],
        @"object": @[@0, @0, @0, @0, @0, @0, @1],
        @"complete": @(t >= 900.0)
    };
}

static NSDictionary<NSString *, id> *V3BoardGripClipSample(double milliseconds) {
    double t = std::max(0.0, std::min(900.0, milliseconds));
    float progress = t <= 300.0 ? static_cast<float>(t / 300.0)
        : (t <= 600.0 ? 1.0f : static_cast<float>(1.0 - (t - 600.0) / 300.0));
    float little = V3Linear(50.0f, 0.0f, progress);
    float ring = V3Linear(55.0f, 0.0f, progress);
    float middle = V3Linear(55.0f, 0.0f, progress);
    float index = V3Linear(50.0f, 0.0f, progress);
    return @{
        @"fingers": @[@(little), @(ring), @(middle), @(index), @100.0f, @0.0f],
        @"hand": @[@0, @0, @0, @0, @0, @0, @1],
        @"object": @[@0, @0, @0, @0, @0, @0, @1],
        @"complete": @(t >= 900.0)
    };
}

static NSDictionary<NSString *, id> *V3NaturalPositionClipSample(double milliseconds) {
    double t = std::max(0.0, std::min(900.0, milliseconds));
    float progress = t <= 300.0 ? static_cast<float>(t / 300.0)
        : (t <= 600.0 ? 1.0f : static_cast<float>(1.0 - (t - 600.0) / 300.0));
    return @{
        @"fingers": @[
            @(V3Linear(13.0f, 100.0f, progress)),
            @(V3Linear(11.0f, 100.0f, progress)),
            @(V3Linear(10.0f, 100.0f, progress)),
            @(V3Linear(12.0f, 100.0f, progress)),
            @(V3Linear(60.0f, 100.0f, progress)),
            @67.0f
        ],
        @"hand": @[@0, @0, @0, @0, @0, @0, @1],
        @"object": @[@0, @0, @0, @0, @0, @0, @1],
        @"complete": @(t >= 900.0)
    };
}

static NSDictionary<NSString *, id> *V3FixedHandClipSample(
    double milliseconds,
    const std::array<float, 6> &start,
    const std::array<float, 6> &target) {
    double t = std::max(0.0, std::min(900.0, milliseconds));
    float progress = t <= 300.0 ? static_cast<float>(t / 300.0)
        : (t <= 600.0 ? 1.0f : static_cast<float>(1.0 - (t - 600.0) / 300.0));
    NSMutableArray<NSNumber *> *fingers = [NSMutableArray arrayWithCapacity:6];
    for (int index = 0; index < 6; ++index) {
        [fingers addObject:@(V3Linear(start[index], target[index], progress))];
    }
    return @{
        @"fingers": [fingers copy],
        @"hand": @[@0, @0, @0, @0, @0, @0, @1],
        @"object": @[@0, @0, @0, @0, @0, @0, @1],
        @"complete": @(t >= 900.0)
    };
}

static NSDictionary<NSString *, id> *V3FistClipSample(double milliseconds) {
    return V3FixedHandClipSample(milliseconds,
        {100.0f, 100.0f, 100.0f, 100.0f, 100.0f, 0.0f},
        {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f});
}

static NSDictionary<NSString *, id> *V3PointingClipSample(double milliseconds) {
    // Internal order: little, ring, middle, index, thumb flex, thumb rotation.
    return V3FixedHandClipSample(milliseconds,
        {100.0f, 100.0f, 100.0f, 0.0f, 60.0f, 67.0f},
        {100.0f, 100.0f, 100.0f, 100.0f, 100.0f, 0.0f});
}

static NSDictionary<NSString *, id> *V3PinchClipSample(double milliseconds) {
    return V3FixedHandClipSample(milliseconds,
        {0.0f, 0.0f, 55.0f, 50.0f, 75.0f, 100.0f},
        {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 100.0f});
}

static NSDictionary<NSString *, id> *V3AdditionalFixedClipSample(double milliseconds, const float start[6], const float target[6]) {
    return V3FixedHandClipSample(milliseconds, start, target);
}

static NSDictionary<NSString *, id> *V3ExtraClipSample(V3CardClip clip, double ms) {
    const float *s = nullptr; const float *t = nullptr;
    static const float fist2s[] = {100,100,100,100,100,100}, fist2t[] = {0,0,0,0,0,100};
    static const float goats[] = {0,100,100,0,60,67}, goatst[] = {0,0,0,0,100,0};
    static const float tweezerss[] = {100,100,100,50,75,0}, tweezerst[] = {0,0,0,0,0,0};
    static const float oks[] = {0,0,0,50,75,0}, okt[] = {0,0,0,0,0,0};
    static const float classics[] = {100,100,100,100,100,100}, classict[] = {100,100,100,100,0,100};
    static const float pinch2s[] = {100,100,0,0,0,0}, pinch2t[] = {100,100,55,50,75,0};
    static const float calls[] = {0,100,100,100,0,100}, callt[] = {100,100,100,100,100,100};
    switch (clip) { case V3CardClip::fist2:s=fist2s;t=fist2t;break; case V3CardClip::goat:s=goats;t=goatst;break; case V3CardClip::tweezers:s=tweezerss;t=tweezerst;break; case V3CardClip::ok:s=oks;t=okt;break; case V3CardClip::classic:s=classics;t=classict;break; case V3CardClip::pinch2:s=pinch2s;t=pinch2t;break; case V3CardClip::callMe:s=calls;t=callt;break; default: return V3GestureKeyClipSample(ms); }
    return V3AdditionalFixedClipSample(ms, s, t);
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
    BOOL _useV3GestureProtocol;

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
    NSInteger handSide = [V3HandSideProvider shared].currentSide;
    return [self initWithDefaultFBOName:defaultFBOName
                         gestureNumber:gestureNumber
                  useV3GestureProtocol:YES
                              handSide:handSide];
}

- (instancetype)initWithDefaultFBOName:(GLuint)defaultFBOName
                         gestureNumber:(NSInteger)gestureNumber
                  useV3GestureProtocol:(BOOL)useV3GestureProtocol
                              handSide:(NSInteger)handSide {
    self = [super init];
    if (!self) return nil;

    _gestureNumber = gestureNumber;
    _defaultFBOName = defaultFBOName;
    _useV3GestureProtocol = useV3GestureProtocol;
    _gestureService = [[GestureService alloc] init];
    if (_useV3GestureProtocol) {
        [[V3HandSideProvider shared] startObserving];
        _handSide = [V3HandSideProvider shared].currentSide;
    } else {
        _handSide = handSide;
    }
    if (_handSide != 0 && _handSide != 1) {
        os_log_error(V3RendererLog(), "Hand side is unavailable for gesture protocol");
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

- (void)setUseV3GestureProtocol:(BOOL)useV3GestureProtocol {
    _useV3GestureProtocol = useV3GestureProtocol;
}

- (void)v3SynchronizeHandSideFromProvider {
    if (!_v3 || !_useV3GestureProtocol) return;
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
    return _v3 && (_v3->transition.active || _v3->gestureObject.clipActive);
}

- (void)v3LoadGestureObjectMeshNamed:(NSString *)resourceName {
    if (!_v3 || _v3->gestureObject.vertexBuffer != 0) return;
    NSURL *url = [NSBundle.mainBundle URLForResource:resourceName withExtension:@"v3object" subdirectory:@"Meshes"];
    if (url == nil) url = [NSBundle.mainBundle URLForResource:resourceName withExtension:@"v3object"];
    NSData *data = url ? [NSData dataWithContentsOfURL:url] : nil;
    if (data.length < 12 || memcmp(data.bytes, "V3OB", 4) != 0) {
        os_log_error(V3RendererLog(), "Gesture object mesh is unavailable: %{public}@", resourceName);
        return;
    }
    const uint8_t *bytes = static_cast<const uint8_t *>(data.bytes);
    uint32_t version = 0, vertexCount = 0;
    memcpy(&version, bytes + 4, 4);
    memcpy(&vertexCount, bytes + 8, 4);
    size_t expected = 12 + static_cast<size_t>(vertexCount) * kV3VertexStride;
    if (version != 1 || data.length != expected) return;
    glGenBuffers(1, &_v3->gestureObject.vertexBuffer);
    glBindBuffer(GL_ARRAY_BUFFER, _v3->gestureObject.vertexBuffer);
    glBufferData(GL_ARRAY_BUFFER, expected - 12, bytes + 12, GL_STATIC_DRAW);
    _v3->gestureObject.vertexCount = static_cast<GLsizei>(vertexCount);
}

- (void)configureGestureKeyCardPreview {
    if (!_v3) return;
    _v3->gestureObject.cardMode = true;
    if (!_v3->gestureObject.cardRotationInitialized) {
        // Approved Gesture Key card calibration captured on the real iPhone.
        // Matrix is stored column-major to match simd_float4x4.
        _v3->gestureObject.cardRotationMatrix.columns[0] = {-0.93031f, -0.17699f, 0.32119f, 0.0f};
        _v3->gestureObject.cardRotationMatrix.columns[1] = {-0.36322f, 0.32390f, -0.87357f, 0.0f};
        _v3->gestureObject.cardRotationMatrix.columns[2] = {0.05058f, -0.92938f, -0.36561f, 0.0f};
        _v3->gestureObject.cardRotationMatrix.columns[3] = {0.0f, 0.0f, 0.0f, 1.0f};
        _v3->gestureObject.cardRotationInitialized = true;
    }
    [self v3ApplyCardEditorTransform];
    _v3->view = matrix_look_at_right_hand((vector_float3){0.0f, 0.0f, 160.0f},
                                          (vector_float3){0.0f, 0.0f, 0.0f},
                                          (vector_float3){0.0f, 1.0f, 0.0f});
    _v3->positions = {100.0f, 100.0f, 100.0f, 100.0f, 100.0f, 0.0f};
    _v3->deformationDirty = true;
    _v3->gestureObject.cardClip = V3CardClip::gestureKey;
    [self v3LoadGestureObjectMeshNamed:@"gesture_key"];
}

- (void)configureCupGripCardPreview {
    if (!_v3) return;
    _v3->gestureObject.cardMode = true;
    _v3->gestureObject.cardClip = V3CardClip::cupGrip;
    if (!_v3->gestureObject.cardRotationInitialized) {
        // Approved Cup Grip calibration captured from the Xcode device console.
        _v3->gestureObject.cardRotationMatrix.columns[0] = {-0.96502f, -0.10436f, 0.24028f, 0.0f};
        _v3->gestureObject.cardRotationMatrix.columns[1] = {-0.24496f, 0.03430f, -0.96886f, 0.0f};
        _v3->gestureObject.cardRotationMatrix.columns[2] = {0.09287f, -0.99390f, -0.05867f, 0.0f};
        _v3->gestureObject.cardRotationMatrix.columns[3] = {0.0f, 0.0f, 0.0f, 1.0f};
        _v3->gestureObject.cardRotationInitialized = true;
    }
    _v3->gestureObject.cardScale = 1.291f;
    _v3->gestureObject.cardPositionX = -18.58f;
    _v3->gestureObject.cardPositionY = -36.74f;
    _v3->gestureObject.position = {-1.42f, -30.17f, -30.28f};
    _v3->gestureObject.rotation = {-92.67f, 143.44f, 7.71f};
    _v3->gestureObject.scale = 1.154f;
    [self v3ApplyCardEditorTransform];
    _v3->view = matrix_look_at_right_hand((vector_float3){0.0f, 0.0f, 160.0f},
                                          (vector_float3){0.0f, 0.0f, 0.0f},
                                          (vector_float3){0.0f, 1.0f, 0.0f});
    _v3->positions = {100.0f, 60.0f, 58.0f, 55.0f, 50.0f, 100.0f};
    _v3->deformationDirty = true;
    [self v3LoadGestureObjectMeshNamed:@"gesture_cup"];
}

- (void)v3PersistBoardCalibrationIfNeeded {
    if (!_v3) return;
    if (_v3->gestureObject.cardClip == V3CardClip::naturalPosition) {
        matrix_float4x4 matrix = _v3->gestureObject.cardRotationMatrix;
        NSDictionary *calibration = @{
            @"matrix": V3MatrixNumbers(matrix),
            @"cardScale": @(_v3->gestureObject.cardScale),
            @"cardPositionX": @(_v3->gestureObject.cardPositionX),
            @"cardPositionY": @(_v3->gestureObject.cardPositionY),
        };
        [[NSUserDefaults standardUserDefaults] setObject:calibration forKey:@"V3NaturalPositionCalibrationV1"];
        NSLog(@"[NaturalCalibration] matrix=[%.5f,%.5f,%.5f; %.5f,%.5f,%.5f; %.5f,%.5f,%.5f] scale=%.3f position=(%.2f,%.2f)",
              matrix.columns[0].x, matrix.columns[1].x, matrix.columns[2].x,
              matrix.columns[0].y, matrix.columns[1].y, matrix.columns[2].y,
              matrix.columns[0].z, matrix.columns[1].z, matrix.columns[2].z,
              _v3->gestureObject.cardScale, _v3->gestureObject.cardPositionX, _v3->gestureObject.cardPositionY);
        return;
    }
    if (_v3->gestureObject.cardClip != V3CardClip::boardGrip) return;
    matrix_float4x4 matrix = _v3->gestureObject.cardRotationMatrix;
    NSDictionary *calibration = @{
        @"matrix": V3MatrixNumbers(matrix),
        @"cardScale": @(_v3->gestureObject.cardScale),
        @"cardPositionX": @(_v3->gestureObject.cardPositionX),
        @"cardPositionY": @(_v3->gestureObject.cardPositionY),
        @"objectPosition": @[@(_v3->gestureObject.position.x), @(_v3->gestureObject.position.y), @(_v3->gestureObject.position.z)],
        @"objectRotation": @[@(_v3->gestureObject.rotation.x), @(_v3->gestureObject.rotation.y), @(_v3->gestureObject.rotation.z)],
        @"objectScale": @(_v3->gestureObject.scale),
    };
    [[NSUserDefaults standardUserDefaults] setObject:calibration forKey:@"V3BoardGripCalibrationV1"];
    NSLog(@"[BoardCalibration] matrix=[%.5f,%.5f,%.5f; %.5f,%.5f,%.5f; %.5f,%.5f,%.5f] handScale=%.3f handPosition=(%.2f,%.2f) boardPosition=(%.2f,%.2f,%.2f) boardRotation=(%.2f,%.2f,%.2f) boardScale=%.3f",
          matrix.columns[0].x, matrix.columns[1].x, matrix.columns[2].x,
          matrix.columns[0].y, matrix.columns[1].y, matrix.columns[2].y,
          matrix.columns[0].z, matrix.columns[1].z, matrix.columns[2].z,
          _v3->gestureObject.cardScale, _v3->gestureObject.cardPositionX, _v3->gestureObject.cardPositionY,
          _v3->gestureObject.position.x, _v3->gestureObject.position.y, _v3->gestureObject.position.z,
          _v3->gestureObject.rotation.x, _v3->gestureObject.rotation.y, _v3->gestureObject.rotation.z,
          _v3->gestureObject.scale);
}

- (void)v3RestoreNaturalCalibrationIfAvailable {
    NSDictionary *calibration = [[NSUserDefaults standardUserDefaults] dictionaryForKey:@"V3NaturalPositionCalibrationV1"];
    if (!calibration) return;
    NSArray<NSNumber *> *matrixValues = calibration[@"matrix"];
    if (matrixValues.count == 16) {
        float *values = reinterpret_cast<float *>(&_v3->gestureObject.cardRotationMatrix);
        for (NSInteger index = 0; index < 16; ++index) values[index] = matrixValues[index].floatValue;
        _v3->gestureObject.cardRotationInitialized = true;
    }
    _v3->gestureObject.cardScale = [calibration[@"cardScale"] floatValue];
    _v3->gestureObject.cardPositionX = [calibration[@"cardPositionX"] floatValue];
    _v3->gestureObject.cardPositionY = [calibration[@"cardPositionY"] floatValue];
}

- (void)v3RestoreBoardCalibrationIfAvailable {
    NSDictionary *calibration = [[NSUserDefaults standardUserDefaults] dictionaryForKey:@"V3BoardGripCalibrationV1"];
    NSArray<NSNumber *> *matrixValues = calibration[@"matrix"];
    if (matrixValues.count == 16) {
        float *values = reinterpret_cast<float *>(&_v3->gestureObject.cardRotationMatrix);
        for (NSInteger index = 0; index < 16; ++index) values[index] = matrixValues[index].floatValue;
        _v3->gestureObject.cardRotationInitialized = true;
    }
    if (!calibration) return;
    _v3->gestureObject.cardScale = [calibration[@"cardScale"] floatValue];
    _v3->gestureObject.cardPositionX = [calibration[@"cardPositionX"] floatValue];
    _v3->gestureObject.cardPositionY = [calibration[@"cardPositionY"] floatValue];
    NSArray<NSNumber *> *position = calibration[@"objectPosition"];
    NSArray<NSNumber *> *rotation = calibration[@"objectRotation"];
    if (position.count == 3) _v3->gestureObject.position = {position[0].floatValue, position[1].floatValue, position[2].floatValue};
    if (rotation.count == 3) _v3->gestureObject.rotation = {rotation[0].floatValue, rotation[1].floatValue, rotation[2].floatValue};
    _v3->gestureObject.scale = [calibration[@"objectScale"] floatValue];
}

- (void)configureBoardGripCardPreview {
    if (!_v3) return;
    _v3->gestureObject.cardMode = true;
    _v3->gestureObject.cardClip = V3CardClip::boardGrip;
    // Board Grip is approved and must always start from its own calibration.
    // Do not inherit a matrix left by another card renderer configuration.
    _v3->gestureObject.cardRotationMatrix.columns[0] = {0.15785858f, -0.98728758f, -0.01507142f, 0.0f};
    _v3->gestureObject.cardRotationMatrix.columns[1] = {0.30507889f, 0.03425055f, 0.95163691f, 0.0f};
    _v3->gestureObject.cardRotationMatrix.columns[2] = {-0.93909848f, -0.15484019f, 0.30660513f, 0.0f};
    _v3->gestureObject.cardRotationMatrix.columns[3] = {0.0f, 0.0f, 0.0f, 1.0f};
    _v3->gestureObject.cardRotationInitialized = true;
    _v3->gestureObject.cardScale = 1.53305185f;
    _v3->gestureObject.cardPositionX = 29.66664886f;
    _v3->gestureObject.cardPositionY = -52.32666779f;
    _v3->gestureObject.position = {-32.41667938f, -31.66667557f, -7.33333158f};
    _v3->gestureObject.rotation = {-13.88885307f, 83.99994659f, 18.99203110f};
    _v3->gestureObject.scale = 2.14293671f;
    [self v3ApplyCardEditorTransform];
    _v3->view = matrix_look_at_right_hand((vector_float3){0.0f, 0.0f, 160.0f},
                                          (vector_float3){0.0f, 0.0f, 0.0f},
                                          (vector_float3){0.0f, 1.0f, 0.0f});
    _v3->positions = {50.0f, 55.0f, 55.0f, 50.0f, 100.0f, 0.0f};
    _v3->deformationDirty = true;
    [self v3LoadGestureObjectMeshNamed:@"gesture_board"];
}

- (void)configureNaturalPositionCardPreview {
    if (!_v3) return;
    _v3->gestureObject.cardMode = true;
    _v3->gestureObject.cardClip = V3CardClip::naturalPosition;
    _v3->gestureObject.cardRotationMatrix.columns[0] = {-0.13039173f, 0.98521471f, -0.11089332f, 0.0f};
    _v3->gestureObject.cardRotationMatrix.columns[1] = {-0.35133705f, -0.15051003f, -0.92403454f, 0.0f};
    _v3->gestureObject.cardRotationMatrix.columns[2] = {-0.92709583f, -0.08152037f, 0.36578980f, 0.0f};
    _v3->gestureObject.cardRotationMatrix.columns[3] = {0.0f, 0.0f, 0.0f, 1.0f};
    _v3->gestureObject.cardRotationInitialized = true;
    _v3->gestureObject.cardScale = 1.29046917f;
    _v3->gestureObject.cardPositionX = -25.50000381f;
    _v3->gestureObject.cardPositionY = -1.32666779f;
    [self v3ApplyCardEditorTransform];
    _v3->view = matrix_look_at_right_hand((vector_float3){0.0f, 0.0f, 160.0f},
                                          (vector_float3){0.0f, 0.0f, 0.0f},
                                          (vector_float3){0.0f, 1.0f, 0.0f});
    _v3->positions = {13.0f, 11.0f, 10.0f, 12.0f, 60.0f, 67.0f};
    _v3->deformationDirty = true;
}

- (void)v3ConfigureFixedHandCardPreview:(V3CardClip)clip {
    if (!_v3) return;
    _v3->gestureObject.cardMode = true;
    _v3->gestureObject.cardClip = clip;
    _v3->gestureObject.cardRotationMatrix.columns[0] = {-0.13039173f, 0.98521471f, -0.11089332f, 0.0f};
    _v3->gestureObject.cardRotationMatrix.columns[1] = {-0.35133705f, -0.15051003f, -0.92403454f, 0.0f};
    _v3->gestureObject.cardRotationMatrix.columns[2] = {-0.92709583f, -0.08152037f, 0.36578980f, 0.0f};
    _v3->gestureObject.cardRotationMatrix.columns[3] = {0.0f, 0.0f, 0.0f, 1.0f};
    _v3->gestureObject.cardRotationInitialized = true;
    _v3->gestureObject.cardScale = 1.29046917f;
    _v3->gestureObject.cardPositionX = -25.50000381f;
    _v3->gestureObject.cardPositionY = -1.32666779f;
    [self v3ApplyCardEditorTransform];
    _v3->view = matrix_look_at_right_hand((vector_float3){0.0f, 0.0f, 160.0f},
                                          (vector_float3){0.0f, 0.0f, 0.0f},
                                          (vector_float3){0.0f, 1.0f, 0.0f});
    if (clip == V3CardClip::fist) {
        _v3->positions = {100.0f, 100.0f, 100.0f, 100.0f, 100.0f, 0.0f};
    } else if (clip == V3CardClip::pointing) {
        _v3->positions = {100.0f, 100.0f, 100.0f, 0.0f, 60.0f, 67.0f};
    } else {
        _v3->positions = {0.0f, 0.0f, 50.0f, 50.0f, 60.0f, 100.0f};
    }
    _v3->deformationDirty = true;
}

- (void)configureFistCardPreview { [self v3ConfigureFixedHandCardPreview:V3CardClip::fist]; }
- (void)configurePointingCardPreview { [self v3ConfigureFixedHandCardPreview:V3CardClip::pointing]; }
- (void)configurePinchCardPreview { [self v3ConfigureFixedHandCardPreview:V3CardClip::pinch]; }

- (void)v3ApplyCardEditorTransform {
    if (!_v3 || !_v3->gestureObject.cardMode) return;
    float scale = _v3->gestureObject.cardScale;
    matrix_float4x4 translation = matrix4x4_translation(_v3->gestureObject.cardPositionX,
                                                        _v3->gestureObject.cardPositionY,
                                                        0.0f);
    _v3->generalRotation = V3Multiply(translation,
                                      V3Multiply(_v3->gestureObject.cardRotationMatrix,
                                                 matrix4x4_scale(scale, scale, scale)));
    _v3->deformationDirty = true;
}

- (void)adjustCardPreviewScaleByFactor:(CGFloat)factor finished:(BOOL)finished {
    if (!_v3 || !_v3->gestureObject.cardMode) return;
    if (_v3->gestureObject.editingKey) {
        _v3->gestureObject.scale = V3Clamp(_v3->gestureObject.scale * static_cast<float>(factor), 0.1f, 5.0f);
        if (finished) {
            NSLog(@"[GestureKeyEditor] target=key position=(%.2f,%.2f,%.2f) rotation=(%.2f,%.2f,%.2f) scale=%.3f",
                  _v3->gestureObject.position.x, _v3->gestureObject.position.y, _v3->gestureObject.position.z,
                  _v3->gestureObject.rotation.x, _v3->gestureObject.rotation.y, _v3->gestureObject.rotation.z,
                  _v3->gestureObject.scale);
            [self v3PersistBoardCalibrationIfNeeded];
        }
        return;
    }
    _v3->gestureObject.cardScale = V3Clamp(_v3->gestureObject.cardScale * static_cast<float>(factor), 0.5f, 4.0f);
    [self v3ApplyCardEditorTransform];
    if (finished) {
        NSLog(@"[GestureKeyEditor] rotation=(%.2f,%.2f,%.2f) scale=%.3f position=(%.2f,%.2f)",
              _v3->gestureObject.cardRotationX, _v3->gestureObject.cardRotationY,
              _v3->gestureObject.cardRotationZ, _v3->gestureObject.cardScale,
              _v3->gestureObject.cardPositionX, _v3->gestureObject.cardPositionY);
        [self v3PersistBoardCalibrationIfNeeded];
    }
}

- (void)adjustCardPreviewRotationByX:(CGFloat)deltaX y:(CGFloat)deltaY finished:(BOOL)finished {
    if (!_v3 || !_v3->gestureObject.cardMode) return;
    if (_v3->gestureObject.editingKey) {
        _v3->gestureObject.rotation.y += static_cast<float>(deltaX);
        _v3->gestureObject.rotation.x += static_cast<float>(deltaY);
        if (finished) {
            NSLog(@"[GestureKeyEditor] target=key position=(%.2f,%.2f,%.2f) rotation=(%.2f,%.2f,%.2f) scale=%.3f",
                  _v3->gestureObject.position.x, _v3->gestureObject.position.y, _v3->gestureObject.position.z,
                  _v3->gestureObject.rotation.x, _v3->gestureObject.rotation.y, _v3->gestureObject.rotation.z,
                  _v3->gestureObject.scale);
            [self v3PersistBoardCalibrationIfNeeded];
        }
        return;
    }
    // Apply rotations in view space, like a virtual trackball. Pre-multiplication
    // keeps horizontal and vertical drags aligned with the screen after any turn.
    matrix_float4x4 screenRotation =
        V3Multiply(V3Rotation(static_cast<float>(deltaX), 0.0f, 1.0f, 0.0f),
                   V3Rotation(static_cast<float>(deltaY), 1.0f, 0.0f, 0.0f));
    _v3->gestureObject.cardRotationMatrix =
        V3Multiply(screenRotation, _v3->gestureObject.cardRotationMatrix);
    [self v3ApplyCardEditorTransform];
    if (finished) {
        matrix_float4x4 m = _v3->gestureObject.cardRotationMatrix;
        NSLog(@"[GestureKeyEditor] matrix=[%.5f,%.5f,%.5f; %.5f,%.5f,%.5f; %.5f,%.5f,%.5f] scale=%.3f position=(%.2f,%.2f)",
              m.columns[0].x, m.columns[1].x, m.columns[2].x,
              m.columns[0].y, m.columns[1].y, m.columns[2].y,
              m.columns[0].z, m.columns[1].z, m.columns[2].z,
              _v3->gestureObject.cardScale, _v3->gestureObject.cardPositionX,
              _v3->gestureObject.cardPositionY);
        [self v3PersistBoardCalibrationIfNeeded];
    }
}

- (void)adjustCardPreviewPositionByX:(CGFloat)deltaX y:(CGFloat)deltaY finished:(BOOL)finished {
    if (!_v3 || !_v3->gestureObject.cardMode) return;
    if (_v3->gestureObject.editingKey) {
        _v3->gestureObject.position.x += static_cast<float>(deltaX);
        _v3->gestureObject.position.y -= static_cast<float>(deltaY);
        if (finished) {
            NSLog(@"[GestureKeyEditor] target=key position=(%.2f,%.2f,%.2f) rotation=(%.2f,%.2f,%.2f) scale=%.3f",
                  _v3->gestureObject.position.x, _v3->gestureObject.position.y, _v3->gestureObject.position.z,
                  _v3->gestureObject.rotation.x, _v3->gestureObject.rotation.y, _v3->gestureObject.rotation.z,
                  _v3->gestureObject.scale);
            [self v3PersistBoardCalibrationIfNeeded];
        }
        return;
    }
    _v3->gestureObject.cardPositionX += static_cast<float>(deltaX);
    _v3->gestureObject.cardPositionY -= static_cast<float>(deltaY);
    [self v3ApplyCardEditorTransform];
    if (finished) {
        NSLog(@"[GestureKeyEditor] rotation=(%.2f,%.2f,%.2f) scale=%.3f position=(%.2f,%.2f)",
              _v3->gestureObject.cardRotationX, _v3->gestureObject.cardRotationY,
              _v3->gestureObject.cardRotationZ, _v3->gestureObject.cardScale,
              _v3->gestureObject.cardPositionX, _v3->gestureObject.cardPositionY);
        [self v3PersistBoardCalibrationIfNeeded];
    }
}

- (void)adjustCardPreviewRollByRadians:(CGFloat)radians finished:(BOOL)finished {
    if (!_v3 || !_v3->gestureObject.cardMode || !_v3->gestureObject.editingKey) return;
    _v3->gestureObject.rotation.z += static_cast<float>(radians * 180.0 / M_PI);
    if (finished) {
        NSLog(@"[GestureKeyEditor] target=key position=(%.2f,%.2f,%.2f) rotation=(%.2f,%.2f,%.2f) scale=%.3f",
              _v3->gestureObject.position.x, _v3->gestureObject.position.y, _v3->gestureObject.position.z,
              _v3->gestureObject.rotation.x, _v3->gestureObject.rotation.y, _v3->gestureObject.rotation.z,
              _v3->gestureObject.scale);
        [self v3PersistBoardCalibrationIfNeeded];
    }
}

- (void)adjustCardPreviewDepthBy:(CGFloat)delta finished:(BOOL)finished {
    if (!_v3 || !_v3->gestureObject.cardMode || !_v3->gestureObject.editingKey) return;
    _v3->gestureObject.position.z += static_cast<float>(delta);
    if (finished) {
        NSLog(@"[GestureKeyEditor] target=key position=(%.2f,%.2f,%.2f) rotation=(%.2f,%.2f,%.2f) scale=%.3f",
              _v3->gestureObject.position.x, _v3->gestureObject.position.y, _v3->gestureObject.position.z,
              _v3->gestureObject.rotation.x, _v3->gestureObject.rotation.y, _v3->gestureObject.rotation.z,
              _v3->gestureObject.scale);
        [self v3PersistBoardCalibrationIfNeeded];
    }
}

- (void)setCardPreviewEditingKey:(BOOL)editingKey {
    if (!_v3) return;
    _v3->gestureObject.editingKey = editingKey;
    NSLog(@"[GestureKeyEditor] target=%@", editingKey ? @"key" : @"hand");
    if (editingKey) {
        matrix_float4x4 m = _v3->gestureObject.cardRotationMatrix;
        NSLog(@"[GestureKeyEditor] target=hand-final matrix=[%.5f,%.5f,%.5f; %.5f,%.5f,%.5f; %.5f,%.5f,%.5f] scale=%.3f position=(%.2f,%.2f)",
              m.columns[0].x, m.columns[1].x, m.columns[2].x,
              m.columns[0].y, m.columns[1].y, m.columns[2].y,
              m.columns[0].z, m.columns[1].z, m.columns[2].z,
              _v3->gestureObject.cardScale, _v3->gestureObject.cardPositionX,
              _v3->gestureObject.cardPositionY);
    }
    [self v3PersistBoardCalibrationIfNeeded];
}

- (void)playGestureKeyClip {
    if (!_v3) return;
    NSLog(@"[GestureKeyTrace] event=rendererPlay vertexCount=%d", _v3->gestureObject.vertexCount);
    [self configureGestureKeyCardPreview];
    _v3->gestureObject.clipStartedAt = CACurrentMediaTime();
    _v3->gestureObject.clipActive = true;
}

- (void)playCupGripClip {
    if (!_v3) return;
    NSLog(@"[CupGripTrace] event=rendererPlay vertexCount=%d", _v3->gestureObject.vertexCount);
    // Do not call configureCupGripCardPreview here: playback must preserve the
    // hand and cup transforms currently being calibrated.
    _v3->gestureObject.clipStartedAt = CACurrentMediaTime();
    _v3->gestureObject.clipActive = true;
}

- (void)playBoardGripClip {
    if (!_v3) return;
    NSLog(@"[BoardGripTrace] event=rendererPlay vertexCount=%d", _v3->gestureObject.vertexCount);
    [self v3PersistBoardCalibrationIfNeeded];
    _v3->gestureObject.clipStartedAt = CACurrentMediaTime();
    _v3->gestureObject.clipActive = true;
}

- (void)playNaturalPositionClip {
    if (!_v3) return;
    NSLog(@"[NaturalPositionTrace] event=rendererPlay");
    [self v3PersistBoardCalibrationIfNeeded];
    _v3->gestureObject.clipStartedAt = CACurrentMediaTime();
    _v3->gestureObject.clipActive = true;
}

- (void)v3PlayFixedHandClipNamed:(NSString *)name {
    if (!_v3) return;
    NSLog(@"[FixedHandClipTrace] event=rendererPlay clip=%@", name);
    _v3->gestureObject.clipStartedAt = CACurrentMediaTime();
    _v3->gestureObject.clipActive = true;
}

- (void)playFistClip { [self v3PlayFixedHandClipNamed:@"fist"]; }
- (void)playPointingClip { [self v3PlayFixedHandClipNamed:@"pointing"]; }
- (void)playPinchClip { [self v3PlayFixedHandClipNamed:@"pinch"]; }

#if DEBUG
+ (NSDictionary<NSString *, id> *)gestureKeyClipStateForTestingAtMilliseconds:(NSTimeInterval)milliseconds {
    return V3GestureKeyClipSample(milliseconds);
}

+ (NSDictionary<NSString *, id> *)cupGripClipStateForTestingAtMilliseconds:(NSTimeInterval)milliseconds {
    return V3CupGripClipSample(milliseconds);
}

+ (NSDictionary<NSString *, id> *)boardGripClipStateForTestingAtMilliseconds:(NSTimeInterval)milliseconds {
    return V3BoardGripClipSample(milliseconds);
}
#endif

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
    GLint vertexColor = 0;
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
    } else if (material == V3Material::gestureObject) {
        specular = 1.0f;
        lightPower = 760.0f;
        ambient = 0.34f;
        materialMode = 3;
        vertexColor = 1;
    } else if (material == V3Material::wood) {
        specular = 5.0f;
        lightPower = 900.0f;
        ambient = 0.48f;
        materialMode = 4;
        vertexColor = 1;
    }
    if (_v3->gestureObject.cardMode) {
        // Keep the original materials and change lighting only for the small
        // collection preview. Lower fill preserves black; stronger side light
        // makes the existing ribs readable through their normals.
        if (material == V3Material::whitePlastic) {
            ambient = 0.66f;
            lightPower = 1180.0f;
        } else if (material == V3Material::rubber) {
            ambient = 0.24f;
            lightPower = 1450.0f;
            materialMode = 2;
        } else {
            ambient = 1.10f;
            lightPower *= 1.08f;
        }
    }
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, texture);
    if (locations.texture >= 0) glUniform1i(locations.texture, 0);
    if (locations.normalMap >= 0) glUniform1i(locations.normalMap, 0);
    if (locations.useNormalMap >= 0) glUniform1i(locations.useNormalMap, 0);
    if (locations.specular >= 0) glUniform1f(locations.specular, specular);
    if (locations.lightPower >= 0) glUniform1f(locations.lightPower, lightPower);
    if (locations.ambient >= 0) glUniform1f(locations.ambient, ambient);
    if (locations.cardContrast >= 0) glUniform1f(locations.cardContrast, _v3->gestureObject.cardMode ? 1.85f : 1.0f);
    if (locations.baseColorMultiplier >= 0) {
        float multiplier = (_v3->gestureObject.cardMode && material == V3Material::rubber) ? 0.42f : 1.0f;
        glUniform3f(locations.baseColorMultiplier, multiplier, multiplier, multiplier);
    }
    if (locations.materialMode >= 0) glUniform1i(locations.materialMode, materialMode);
    if (locations.chromeStrength >= 0) glUniform1f(locations.chromeStrength, materialMode == 1 ? 0.72f : 0.0f);
    if (locations.fillDirection >= 0) glUniform3f(locations.fillDirection, -0.74f, 0.46f, 0.49f);
    if (locations.rimDirection >= 0) glUniform3f(locations.rimDirection, 0.78f, 0.44f, -0.45f);
    if (locations.fillStrength >= 0) glUniform1f(locations.fillStrength, materialMode == 1 ? 0.82f : (materialMode == 2 ? 1.0f : 0.0f));
    if (locations.rimStrength >= 0) glUniform1f(locations.rimStrength, materialMode == 1 ? 1.08f : (materialMode == 2 ? 1.0f : 0.0f));
    if (locations.chromeToneMapStrength >= 0) glUniform1f(locations.chromeToneMapStrength, materialMode == 1 ? 1.0f : 0.0f);
    if (locations.mirrored >= 0) glUniform1i(locations.mirrored, _v3->handSide == 0 ? 1 : 0);
    if (locations.useSolidColor >= 0) glUniform1i(locations.useSolidColor, solid);
    if (locations.solidColor >= 0) glUniform4fv(locations.solidColor, 1, reinterpret_cast<const GLfloat *>(&solidColor));
    if (locations.useVertexColor >= 0) glUniform1i(locations.useVertexColor, vertexColor);
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
    vector_float4 worldLight = _v3->gestureObject.cardMode
        ? (vector_float4){-85.0f, 105.0f, 145.0f, 1.0f}
        : (vector_float4){0.0f, 0.0f, 180.0f, 1.0f};
    vector_float4 light = V3Multiply(_v3->view, worldLight);
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
    if (_v3->gestureObject.cardMode) {
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    } else {
        glClearColor(42.0f / 255.0f, 42.0f / 255.0f, 42.0f / 255.0f, 1.0f);
    }
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    [self v3DrawRigidScenePicking:NO];
    matrix_float4x4 base = [self v3HandBaseMatrix];
    [self v3DrawGroup:"base_white_plastic" model:base material:V3Material::whitePlastic fingerCode:-1 picking:NO];
    [self v3DrawGroup:"base_rubber" model:base material:V3Material::rubber fingerCode:-1 picking:NO];
    [self v3DrawGroup:"gofra_static" model:base material:V3Material::rubber fingerCode:-1 picking:NO];
    [self v3DrawDeformablePicking:NO];
    if (_v3->gestureObject.cardMode && _v3->gestureObject.vertexBuffer != 0) {
        matrix_float4x4 object = matrix4x4_translation(_v3->gestureObject.position);
        object = V3Multiply(object, V3Rotation(_v3->gestureObject.rotation.x, 1, 0, 0));
        object = V3Multiply(object, V3Rotation(_v3->gestureObject.rotation.y, 0, 1, 0));
        object = V3Multiply(object, V3Rotation(_v3->gestureObject.rotation.z, 0, 0, 1));
        object = V3Multiply(_v3->generalRotation, V3Multiply(object, matrix4x4_scale(_v3->gestureObject.scale, _v3->gestureObject.scale, _v3->gestureObject.scale)));
        const V3ProgramLocations &locations = _v3->material;
        glUseProgram(locations.program);
        [self v3SetMatricesForModel:object locations:locations];
        V3Material objectMaterial = _v3->gestureObject.cardClip == V3CardClip::gestureKey
            ? V3Material::chrome
            : (_v3->gestureObject.cardClip == V3CardClip::boardGrip
                ? V3Material::wood
                : V3Material::gestureObject);
        [self v3ApplyMaterial:objectMaterial locations:locations];
        glBindBuffer(GL_ARRAY_BUFFER, _v3->gestureObject.vertexBuffer);
        const GLint offsets[] = {0, 3, 6, 10, 12, 15};
        const GLint sizes[] = {3, 3, 4, 2, 3, 3};
        const GLint attributes[] = {locations.position, locations.normal, locations.color, locations.texcoord, locations.tangent, locations.bitangent};
        for (int index = 0; index < 6; ++index) {
            if (attributes[index] < 0) continue;
            glVertexAttribPointer(attributes[index], sizes[index], GL_FLOAT, GL_FALSE, kV3VertexStride, reinterpret_cast<const GLvoid *>(offsets[index] * sizeof(float)));
            glEnableVertexAttribArray(attributes[index]);
        }
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        glDrawArrays(GL_TRIANGLES, 0, _v3->gestureObject.vertexCount);
    }
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
    CFTimeInterval now = CACurrentMediaTime();
    [self v3AdvanceTransitionAtTime:now];
    if (_v3->gestureObject.clipActive) {
        static int gestureKeyFrameLogCounter = 0;
        if ((gestureKeyFrameLogCounter++ % 30) == 0) {
            NSLog(@"[GestureKeyTrace] event=clipFrame elapsedMs=%.1f", (now - _v3->gestureObject.clipStartedAt) * 1000.0);
        }
        double elapsedMs = (now - _v3->gestureObject.clipStartedAt) * 1000.0;
        NSDictionary<NSString *, id> *sample = _v3->gestureObject.cardClip == V3CardClip::cupGrip
            ? V3CupGripClipSample(elapsedMs)
            : (_v3->gestureObject.cardClip == V3CardClip::boardGrip
                ? V3BoardGripClipSample(elapsedMs)
                : (_v3->gestureObject.cardClip == V3CardClip::naturalPosition
                    ? V3NaturalPositionClipSample(elapsedMs)
                    : (_v3->gestureObject.cardClip == V3CardClip::fist
                        ? V3FistClipSample(elapsedMs)
                        : (_v3->gestureObject.cardClip == V3CardClip::pointing
                            ? V3PointingClipSample(elapsedMs)
                            : (_v3->gestureObject.cardClip == V3CardClip::pinch
                                ? V3PinchClipSample(elapsedMs)
                                : V3GestureKeyClipSample(elapsedMs))))));
        NSArray<NSNumber *> *fingers = sample[@"fingers"];
        if (_v3->gestureObject.cardClip != V3CardClip::gestureKey) {
            _v3->positions = {fingers[0].floatValue, fingers[1].floatValue,
                fingers[2].floatValue, fingers[3].floatValue,
                fingers[4].floatValue, fingers[5].floatValue};
        } else {
            _v3->positions = {100.0f, 100.0f, 100.0f, 100.0f,
                (49.0f - fingers[4].floatValue) * 100.0f / 84.0f,
                (22.0f - fingers[5].floatValue) * 100.0f / 90.0f};
        }
        // Gesture Key keeps the object fixed in the grasp. In card mode its
        // transform may be interactively tuned, so playback must animate only
        // the fingers and never overwrite the current key transform.
        if (!_v3->gestureObject.cardMode) {
            NSArray<NSNumber *> *object = sample[@"object"];
            _v3->gestureObject.position = {object[0].floatValue, object[1].floatValue, object[2].floatValue};
            _v3->gestureObject.rotation = {object[3].floatValue, object[4].floatValue, object[5].floatValue};
            _v3->gestureObject.scale = object[6].floatValue;
        }
        _v3->deformationDirty = true;
        if ([sample[@"complete"] boolValue]) _v3->gestureObject.clipActive = false;
    }
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
    if (_v3->gestureObject.cardMode) {
        _v3->selectedFinger = 0;
        return;
    }
    _v3->selectedFinger = [self v3SelectObject];
}

- (void) touchIvent:(CGFloat) X  :(CGFloat) Y :(CGFloat) deltaX :(CGFloat) deltaY {
    if (!_v3) return;
    _v3->touchX = static_cast<float>(X);
    _v3->touchY = static_cast<float>(Y);

    if (_v3->gestureObject.cardMode) {
        // Card mode is controlled by its gesture recognizers. Letting the
        // legacy raw-touch path run as well makes the hand drift while the key
        // is being edited.
        return;
    }

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
    if (_v3 && _v3->gestureObject.cardMode) {
        NSLog(@"[GestureKeyEditor] rotation=(%.2f,%.2f,%.2f) scale=%.3f position=(%.2f,%.2f)",
              _v3->gestureObject.cardRotationX, _v3->gestureObject.cardRotationY,
              _v3->gestureObject.cardRotationZ, _v3->gestureObject.cardScale,
              _v3->gestureObject.cardPositionX, _v3->gestureObject.cardPositionY);
        return;
    }
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
    if (_useV3GestureProtocol) {
        SharedKotlinByteArray *command = [[SharedBLECommandsV3 shared] sendGestureInfoGestureWithAddress:_gestureWithAddress];
        [_gestureService sendDataToFestV3WithDataForWrite:command];
    } else {
        SharedKotlinByteArray *command = [[SharedBLECommands shared] sendGestureInfoGestureWithAddress:_gestureWithAddress];
        [_gestureService sendDataToFestWithDataForWrite:command];
    }
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
    if (_v3->gestureObject.vertexBuffer) glDeleteBuffers(1, &_v3->gestureObject.vertexBuffer);
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
    SharedGesture *decodedGesture = _useV3GestureProtocol
        ? [_gestureService decodeGestureSettingsV3WithRaw:_gestureSettingsParameterData]
        : [_gestureService decodeGestureSettingsWithRaw:_gestureSettingsParameterData];
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
