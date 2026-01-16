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

//@interface GestureSettingsParameterData : NSObject
//@property (nonatomic, readonly) NSString * _Nonnull data;
//@end

static const CGSize AAPLInteropTextureSize = {1024, 1024};

@interface AAPLOpenGLRenderer : NSObject

- (instancetype _Nullable )initWithDefaultFBOName:(GLuint)defaultFBOName
                        gestureNumber:(NSInteger)gestureNumber;

- (void)draw;

- (void)resize:(CGSize)size;

- (void)stopVC;
- (void)savesAllData;
- (void)beginTouchIvent;
- (void)touchIvent:(CGFloat) X  :(CGFloat) Y :(CGFloat) deltaX :(CGFloat) deltaY;
- (void)endTouchIvent;
- (void)changeState :(BOOL) state;

- (void)calculationOfCoefficients:(CGFloat) width  :(CGFloat) height;
- (void)saveStateData:(NSString*_Nullable) dataForWrite;
//- (void)updateGestureSettingsData:(NSInteger)data;
- (void)updateGestureSettings:(SharedParameterRef *_Nullable)parameterRef
                parameterData:(NSString *_Nullable)parameterData;
//- (void)loadFingersDelayTable;
@end
