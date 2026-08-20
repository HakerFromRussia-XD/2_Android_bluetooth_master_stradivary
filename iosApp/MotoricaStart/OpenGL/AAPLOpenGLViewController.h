/*
See LICENSE folder for this sample’s licensing information.

Abstract:
Header for the cross-platform view controller and cross-platform view that displays OpenGL content.
*/
@import UIKit;
#define PlatformViewBase UIView
#define PlatformViewController UIViewController

@interface AAPLOpenGLViewV3 : PlatformViewBase

@end

@interface AAPLOpenGLViewControllerV3 : PlatformViewController

+ (Class)rendererClassForV3Mode:(BOOL)useV3Mode;
- (NSInteger) someMethod;
- (void)setNumberGesture:(NSInteger)number;
@property (nonatomic, assign) NSInteger gestureNumber;
@property (nonatomic, assign) BOOL useV3Mode;
@property (nonatomic, assign) BOOL useV3GestureProtocol;
@property (nonatomic, assign) BOOL modelTestMode;
@property (nonatomic, assign) BOOL cardPreviewMode;
@property (nonatomic, assign) CGSize cardPreviewSize;
- (void)playGestureKeyClip;
- (void)setCardPreviewEditingKey:(BOOL)editingKey;
- (void)stopCardPreview;

@end
