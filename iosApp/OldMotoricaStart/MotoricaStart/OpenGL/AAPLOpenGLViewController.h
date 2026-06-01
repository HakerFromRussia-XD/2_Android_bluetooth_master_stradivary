/*
See LICENSE folder for this sample’s licensing information.

Abstract:
Header for the cross-platform view controller and cross-platform view that displays OpenGL content.
*/
@import UIKit;
#define PlatformViewBase UIView
#define PlatformViewController UIViewController

@interface AAPLOpenGLView : PlatformViewBase

@end

@interface AAPLOpenGLViewController : PlatformViewController

- (NSInteger) someMethod;

@end
