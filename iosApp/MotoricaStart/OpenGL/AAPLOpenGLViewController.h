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
/// Enables hand scale and two-finger translation for a selected calibration card.
@property (nonatomic, assign) BOOL cardPreviewEditingEnabled;
/// 0 = Key, 1 = Cup, 2 = Board, 3 = Natural, 4 = Fist, 5 = Pointing, 6 = Pinch.
@property (nonatomic, assign) NSInteger cardPreviewClipKind;
@property (nonatomic, assign) CGSize cardPreviewSize;
- (void)playGestureKeyClip;
- (void)playCupGripClip;
- (void)playBoardGripClip;
- (void)playNaturalPositionClip;
- (void)playFistClip;
- (void)playPointingClip;
- (void)playPinchClip;
- (void)configureAdditionalFixedCardPreview:(NSInteger)kind;
- (void)playAdditionalFixedClip:(NSInteger)kind;
- (void)setCardPreviewEditingKey:(BOOL)editingKey;
- (void)stopCardPreview;

@end
