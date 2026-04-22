/*
See LICENSE folder for this sample’s licensing information.

Abstract:
Header for the renderer class that performs OpenGL state setup and per-frame rendering.
*/

#import <Foundation/Foundation.h>
#include <CoreGraphics/CoreGraphics.h>
#include "AAPLGLHeaders.h"
#import <GLKit/GLKTextureLoader.h>
#import <shared/shared.h>

@interface AAPLOpenGLRendererV3 : NSObject
- (instancetype _Nullable )initWithDefaultFBOName:(GLuint)defaultFBOName
                        gestureNumber:(NSInteger)gestureNumber;

- (void)draw;
- (void)resize:(CGSize)size;

- (void)stopVC;
- (void)stopVCWithSaveData;
- (void)openFingersDelayDialog;
- (void)beginTouchIvent;
- (void)touchIvent:(CGFloat) X  :(CGFloat) Y :(CGFloat) deltaX :(CGFloat) deltaY;
- (void)endTouchIvent;
- (void)changeState :(BOOL) state;

- (void)calculationOfCoefficients:(CGFloat) width  :(CGFloat) height;
- (void)updateGestureSettings:(SharedParameterRef *_Nullable)parameterRef
                parameterData:(NSString *_Nullable)parameterData;
+ (NSDictionary<NSString *, NSNumber *> *_Nonnull)stageDistributionForGesture:(SharedGesture *_Nonnull)gesture;
+ (NSInteger)rawStageForThumbFlexTransfer:(NSInteger)transfer;
+ (NSInteger)rawStageForThumbRotationTransfer:(NSInteger)transfer;
+ (NSInteger)thumbFlexTransferForRawStage:(NSInteger)rawStage;
+ (NSInteger)thumbRotationTransferForRawStage:(NSInteger)rawStage;
+ (int32_t)runtimeGestureStateForClosed:(BOOL)isClosed;
+ (int32_t)transitionGestureStateForClosed:(BOOL)isClosed;
+ (int32_t)saveGestureState;
- (BOOL)currentGestureState;
- (NSArray<NSNumber *> *_Nullable)currentOpenToCloseShifts;
- (NSArray<NSNumber *> *_Nullable)currentCloseToOpenShifts;
- (void)applyOpenToCloseShifts:(NSArray<NSNumber *> *_Nullable)values;
- (void)applyCloseToOpenShifts:(NSArray<NSNumber *> *_Nullable)values;
@end
