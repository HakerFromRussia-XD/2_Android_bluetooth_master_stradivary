/*
See LICENSE folder for this sample’s licensing information.

Abstract:
Implementation of the cross-platform view controller and cross-platform view that displays OpenGL content.
*/
#import "AAPLOpenGLViewController.h"
#import "AAPLOpenGLRenderer.h"
#import "AAPLOpenGLRendererV3.h"
#import "V3ModelResourceCache.h"
#import "MotoricaStart-Swift.h"

#import <UIKit/UIKit.h>
#import <OpenGLES/ES2/glext.h>
#import <objc/message.h>
#import <os/log.h>
#import <os/signpost.h>
#define PlatformGLContext EAGLContext


@implementation AAPLOpenGLViewV3

+ (Class) layerClass
{
    return [CAEAGLLayer class];
}

@end

@interface AAPLOpenGLViewControllerV3 () <UIGestureRecognizerDelegate>
@end

@implementation AAPLOpenGLViewControllerV3
{
    AAPLOpenGLViewV3 *_view;
    id _openGLRenderer;
    GestureService *gestureService;
    PlatformGLContext *_context;
    GLuint _defaultFBOName;
    GLuint _presentationFBOName;
    
    GLuint _colorRenderbuffer;
    GLuint _depthRenderbuffer;
    GLuint _multisampleFBOName;
    GLuint _multisampleColorRenderbuffer;
    GLuint _multisampleDepthRenderbuffer;
    GLint _multisampleSamples;
    CADisplayLink *_displayLink;
    dispatch_queue_t _v3RenderQueue;
    BOOL _v3FramePending;
    BOOL _v3TouchActive;
    BOOL _v3GestureSettingsReady;
    BOOL _v3FirstFrameReady;
    BOOL _v3InitialOpenSent;
    UIPanGestureRecognizer *_cardRotationPan;
    UIPanGestureRecognizer *_cardTranslationPan;
    UIRotationGestureRecognizer *_cardRollGesture;
    UIPanGestureRecognizer *_cardDepthPan;
    CGSize _lastDrawableBoundsSize;
    __weak IBOutlet UIButton *saveBtn;
    __weak IBOutlet UIButton *state_btn;
    UIView *segmentContainer;
    UIView *stateSegmentBackgroundView;
    UIView *stateSegmentContentView;
    UIView *stateSegmentHighlightView;
    UIButton *stateOpenButton;
    UIButton *stateCloseButton;
    NSLayoutConstraint *stateHighlightLeadingConstraint;
    NSInteger selectedStateSegmentIndex;
    __weak IBOutlet UIButton *fingers_delay_btn;
    __weak IBOutlet UIButton *fingersDelayBtn;
    __weak IBOutlet UITextField *text_field;
    __weak IBOutlet UITextField *textField;
    __weak IBOutlet UILabel *deviceName;
    __weak IBOutlet UIImageView *statusConnection;
    __weak IBOutlet UIButton *renameBtn;
    StatusBarConnectionIndicatorHostView *connectionStatusIndicatorView;
    UIImage *connectStatus;
    UIImage *disconnectStatus;
    
    NSInteger _gestureNumber;
    float _previousX;
    float _previousY;
    bool _stop;
    bool state;
    bool showRenameTextField;
    BOOL _gestureSettingsObserverRegistered;
    BOOL _didInjectGestureSettingsV3ForUITest;
    BOOL _gestureNameTextFieldLayoutConfigured;
}

- (void)loadView {
    if (self.cardPreviewMode) {
        CGSize size = self.cardPreviewSize;
        if (size.width <= 1.0 || size.height <= 1.0) size = UIScreen.mainScreen.bounds.size;
        self.view = [[AAPLOpenGLViewV3 alloc] initWithFrame:(CGRect){CGPointZero, size}];
        return;
    }
    [super loadView];
}

+ (Class)rendererClassForV3Mode:(BOOL)useV3Mode {
    return useV3Mode ? AAPLOpenGLRendererV3.class : AAPLOpenGLRenderer.class;
}


static NSString *const GestureSettingsViewModelDidUpdateNotification = @"GestureSettingsViewModelDidUpdate";
static NSString *const GestureSettingsViewModelDidUpdateV3Notification = @"GestureSettingsV3ViewModelDidUpdate";
static NSString *const GestureSettingsScreenAccessibilityIdentifier = @"AccessibilityIdentifierGestureSettingsScreen";
static NSString *const V3FirstFrameReadyAccessibilityIdentifier = @"AccessibilityIdentifierV3FirstFrameReady";
static NSString *const GestureSettingsUITestExposeStateFlag = @"-ui-test-expose-gesture-settings-state";
static NSString *const GestureSettingsUITestInjectGesture70Flag = @"-ui-test-inject-v3-gesture-70";
static NSString *const GestureSettingsUITestGesture70Payload =
    @"{\"gestureId\":70,"
     "\"openPosition1\":0,\"openPosition2\":97,\"openPosition3\":100,\"openPosition4\":0,\"openPosition5\":0,\"openPosition6\":0,"
     "\"closePosition1\":100,\"closePosition2\":100,\"closePosition3\":100,\"closePosition4\":100,\"closePosition5\":100,\"closePosition6\":0,"
     "\"openToCloseTimeShift1\":0,\"openToCloseTimeShift2\":0,\"openToCloseTimeShift3\":0,\"openToCloseTimeShift4\":0,\"openToCloseTimeShift5\":0,\"openToCloseTimeShift6\":0,"
     "\"closeToOpenTimeShift1\":0,\"closeToOpenTimeShift2\":0,\"closeToOpenTimeShift3\":0,\"closeToOpenTimeShift4\":0,\"closeToOpenTimeShift5\":0,\"closeToOpenTimeShift6\":0}";
static const void *V3RenderQueueSpecificKey = &V3RenderQueueSpecificKey;

static os_log_t V3FrameLog(void) {
    static os_log_t log;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        log = os_log_create("com.bailout.stickk", "V3Frame");
    });
    return log;
}

- (void)performV3RenderSync:(dispatch_block_t)block {
    CFTimeInterval startedAt = CACurrentMediaTime();
    if (_v3RenderQueue == nil || dispatch_get_specific(V3RenderQueueSpecificKey) != NULL) {
        block();
        double durationMs = (CACurrentMediaTime() - startedAt) * 1000.0;
        if (durationMs > 8.0) {
            NSLog(@"[V3OpenTrace] event=performV3RenderSyncSlow thread=%@ mode=direct durationMs=%.3f useV3Mode=%d gestureId=%ld",
                  NSThread.isMainThread ? @"main" : @"render",
                  durationMs,
                  self.useV3Mode,
                  (long)_gestureNumber);
        }
        return;
    }
    dispatch_sync(_v3RenderQueue, block);
    double durationMs = (CACurrentMediaTime() - startedAt) * 1000.0;
    if (durationMs > 8.0) {
        NSLog(@"[V3OpenTrace] event=performV3RenderSyncSlow thread=main mode=dispatch durationMs=%.3f useV3Mode=%d gestureId=%ld",
              durationMs,
              self.useV3Mode,
              (long)_gestureNumber);
    }
}

- (void)performV3RenderAsync:(dispatch_block_t)block {
    if (_v3RenderQueue == nil || dispatch_get_specific(V3RenderQueueSpecificKey) != NULL) {
        block();
        return;
    }
    dispatch_async(_v3RenderQueue, block);
}

- (void)requestV3Frame {
    if (!self.useV3Mode || _stop || _displayLink == nil) {
        NSLog(@"[V3OpenTrace] event=requestV3FrameSkipped thread=%@ useV3Mode=%d stop=%d hasDisplayLink=%d gestureId=%ld",
              NSThread.isMainThread ? @"main" : @"render",
              self.useV3Mode,
              _stop,
              _displayLink != nil,
              (long)_gestureNumber);
        return;
    }
    if (![NSThread isMainThread]) {
        dispatch_async(dispatch_get_main_queue(), ^{ [self requestV3Frame]; });
        return;
    }
    BOOL wasPaused = _displayLink.paused;
    _displayLink.paused = NO;
    NSLog(@"[V3OpenTrace] event=requestV3Frame thread=main wasPaused=%d nowPaused=%d gestureId=%ld",
          wasPaused,
          _displayLink.paused,
          (long)_gestureNumber);
}

- (void)setRendererClosedState:(BOOL)closed {
    if (!self.useV3Mode) {
        [_openGLRenderer changeState:closed];
        return;
    }
    [self performV3RenderAsync:^{
        [EAGLContext setCurrentContext:self->_context];
        [(AAPLOpenGLRendererV3 *)self->_openGLRenderer changeState:closed];
    }];
    [self requestV3Frame];
}

- (void)stopRendererSavingData:(BOOL)saveData {
    _stop = true;
    _displayLink.paused = YES;
    if (!self.useV3Mode) {
        saveData ? [_openGLRenderer stopVCWithSaveData] : [_openGLRenderer stopVC];
        return;
    }
    if (saveData) {
        [self performV3RenderSync:^{
            [EAGLContext setCurrentContext:self->_context];
            [(AAPLOpenGLRendererV3 *)self->_openGLRenderer stopVCWithSaveData];
        }];
    }
    [self destroyV3Resources];
}

- (void)destroyV3Resources {
    if (!self.useV3Mode || _v3RenderQueue == nil) return;
    [_displayLink invalidate];
    _displayLink = nil;
    [self performV3RenderSync:^{
        [EAGLContext setCurrentContext:self->_context];
        if (self->_openGLRenderer != nil) {
            [(AAPLOpenGLRendererV3 *)self->_openGLRenderer releaseGLResources];
            self->_openGLRenderer = nil;
        }
        if (self->_multisampleColorRenderbuffer) glDeleteRenderbuffers(1, &self->_multisampleColorRenderbuffer);
        if (self->_multisampleDepthRenderbuffer) glDeleteRenderbuffers(1, &self->_multisampleDepthRenderbuffer);
        if (self->_multisampleFBOName) glDeleteFramebuffers(1, &self->_multisampleFBOName);
        if (self->_depthRenderbuffer) glDeleteRenderbuffers(1, &self->_depthRenderbuffer);
        if (self->_colorRenderbuffer) glDeleteRenderbuffers(1, &self->_colorRenderbuffer);
        if (self->_presentationFBOName) glDeleteFramebuffers(1, &self->_presentationFBOName);
        self->_multisampleColorRenderbuffer = 0;
        self->_multisampleDepthRenderbuffer = 0;
        self->_multisampleFBOName = 0;
        self->_depthRenderbuffer = 0;
        self->_colorRenderbuffer = 0;
        self->_presentationFBOName = 0;
        self->_defaultFBOName = 0;
        self->_lastDrawableBoundsSize = CGSizeZero;
        [EAGLContext setCurrentContext:nil];
    }];
    _context = nil;
    _v3RenderQueue = nil;
}

- (void)attemptInitialV3Open {
    NSLog(@"[V3OpenTrace] event=attemptInitialOpen thread=main modelTestMode=%d useV3Mode=%d initialOpenSent=%d gestureReady=%d firstFrameReady=%d gestureId=%ld",
          self.modelTestMode,
          self.useV3Mode,
          _v3InitialOpenSent,
          _v3GestureSettingsReady,
          _v3FirstFrameReady,
          (long)_gestureNumber);
    if (self.modelTestMode || !self.useV3Mode || _v3InitialOpenSent || !_v3GestureSettingsReady || !_v3FirstFrameReady) {
        return;
    }
    _v3InitialOpenSent = YES;
    selectedStateSegmentIndex = 0;
    state = NO;
    [self selectStateSegmentIndex:0 animated:NO notifyRenderer:NO];
    NSLog(@"[V3OpenTrace] event=sendInitialOpen thread=main gestureId=%ld state=128", (long)_gestureNumber);
    NSLog(@"[V3BLE] first ready frame and gesture settings received; sending OPEN state=128");
    [self setRendererClosedState:NO];
}

- (void)handleV3HandSideChange:(NSNotification *)notification {
    if (!self.useV3Mode || !self.useV3GestureProtocol) return;
    NSNumber *side = notification.userInfo[@"side"];
    if (side == nil) return;
    [self performV3RenderAsync:^{
        [(AAPLOpenGLRendererV3 *)self->_openGLRenderer setHandSide:side.integerValue];
    }];
    [self requestV3Frame];
}

- (void)registerGestureSettingsObserverIfNeeded {
    if (_gestureSettingsObserverRegistered) {
        return;
    }
    NSString *notificationName = self.useV3GestureProtocol
        ? GestureSettingsViewModelDidUpdateV3Notification
        : GestureSettingsViewModelDidUpdateNotification;
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(handleGestureSettingsUpdate:)
                                                 name:notificationName
                                               object:nil];
    _gestureSettingsObserverRegistered = YES;
}

- (void)unregisterGestureSettingsObserver {
    if (!_gestureSettingsObserverRegistered) {
        return;
    }
    [[NSNotificationCenter defaultCenter] removeObserver:self
                                          name:GestureSettingsViewModelDidUpdateNotification
                                          object:nil];
    [[NSNotificationCenter defaultCenter] removeObserver:self
                                          name:GestureSettingsViewModelDidUpdateV3Notification
                                          object:nil];
    _gestureSettingsObserverRegistered = NO;
}

- (BOOL)hasLaunchArgument:(NSString *)argument {
    return [NSProcessInfo.processInfo.arguments containsObject:argument];
}

- (void)updateGestureSettingsAccessibilityWithGesture:(SharedGesture *)gesture {
    if (gesture == nil || ![self hasLaunchArgument:GestureSettingsUITestExposeStateFlag]) {
        return;
    }

    NSString *summary = [NSString stringWithFormat:
        @"gestureId=%d;"
        "openStage1=%d;openStage2=%d;openStage3=%d;openStage4=%d;openStage5=%d;openStage6=%d;"
        "closeStage1=%d;closeStage2=%d;closeStage3=%d;closeStage4=%d;closeStage5=%d;closeStage6=%d",
        gesture.gestureId,
        gesture.openPosition4, gesture.openPosition3, gesture.openPosition2, gesture.openPosition1, gesture.openPosition5, gesture.openPosition6,
        gesture.closePosition4, gesture.closePosition3, gesture.closePosition2, gesture.closePosition1, gesture.closePosition5, gesture.closePosition6
    ];
    deviceName.accessibilityValue = summary;
    NSLog(@"[UI-TEST][GestureSettings] accessibilityValue=%@", summary);
}

- (void)injectGestureSettingsV3ForUITestIfNeeded {
    if (_didInjectGestureSettingsV3ForUITest ||
        !self.useV3GestureProtocol ||
        _gestureNumber != 70 ||
        ![self hasLaunchArgument:GestureSettingsUITestInjectGesture70Flag]) {
        NSLog(@"[UI-TEST][GestureSettings] skip inject useV3=%d gestureNumber=%ld didInject=%d hasFlag=%d",
        self.useV3GestureProtocol,
              (long)_gestureNumber,
              _didInjectGestureSettingsV3ForUITest,
              [self hasLaunchArgument:GestureSettingsUITestInjectGesture70Flag]);
        return;
    }

    _didInjectGestureSettingsV3ForUITest = YES;
    NSLog(@"[UI-TEST][GestureSettings] injecting payload for gesture 70");
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(0.25 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
        SharedParameterRef *parameterRef = [[SharedParameterRef alloc] initWithAddressDevice:0 parameterID:0 dataCode:0];
        [self applyGestureSettingsUpdate:parameterRef parameterData:GestureSettingsUITestGesture70Payload];
    });
}

- (UIButton *)resolvedFingersDelayButton {
    return fingersDelayBtn ?: fingers_delay_btn;
}

- (UITextField *)resolvedTextField {
    return textField ?: text_field;
}

- (void)configureGestureHeaderAppearance {
    UIFont *headerFont = [UIFont systemFontOfSize:14.0 weight:UIFontWeightMedium];
    UIColor *headerTextColor = [UIColor colorNamed:@"ubi4_white"] ?: UIColor.whiteColor;
    UIColor *fieldBackgroundColor = [UIColor colorNamed:@"ubi4_gray"] ?: [UIColor colorWithWhite:0.24 alpha:1.0];
    UIColor *fieldBorderColor = [UIColor colorNamed:@"ubi4_gray_border"] ?: [UIColor colorWithWhite:1.0 alpha:0.18];

    deviceName.font = headerFont;
    deviceName.textColor = headerTextColor;
    deviceName.textAlignment = NSTextAlignmentCenter;
    deviceName.numberOfLines = 1;
    deviceName.lineBreakMode = NSLineBreakByTruncatingTail;
    [deviceName setContentHuggingPriority:UILayoutPriorityRequired forAxis:UILayoutConstraintAxisHorizontal];

    UITextField *editableTextField = [self resolvedTextField];
    editableTextField.font = headerFont;
    editableTextField.textColor = headerTextColor;
    editableTextField.textAlignment = NSTextAlignmentCenter;
    editableTextField.contentVerticalAlignment = UIControlContentVerticalAlignmentCenter;
    editableTextField.borderStyle = UITextBorderStyleNone;
    editableTextField.backgroundColor = fieldBackgroundColor;
    editableTextField.layer.cornerRadius = 8.0;
    editableTextField.layer.borderWidth = 1.0 / UIScreen.mainScreen.scale;
    editableTextField.layer.borderColor = fieldBorderColor.CGColor;
    editableTextField.clipsToBounds = YES;
    editableTextField.adjustsFontSizeToFitWidth = YES;
    editableTextField.minimumFontSize = 10.0;

    UIView *leftPaddingView = [[UIView alloc] initWithFrame:CGRectMake(0, 0, 10, 1)];
    UIView *rightPaddingView = [[UIView alloc] initWithFrame:CGRectMake(0, 0, 10, 1)];
    editableTextField.leftView = leftPaddingView;
    editableTextField.leftViewMode = UITextFieldViewModeAlways;
    editableTextField.rightView = rightPaddingView;
    editableTextField.rightViewMode = UITextFieldViewModeAlways;
    [editableTextField addTarget:self action:@selector(gestureNameTextFieldEditingChanged:) forControlEvents:UIControlEventEditingChanged];

    [self configureGestureNameTextFieldLayoutIfNeeded];
}

- (void)configureGestureNameTextFieldLayoutIfNeeded {
    if (_gestureNameTextFieldLayoutConfigured) {
        return;
    }

    UITextField *editableTextField = [self resolvedTextField];
    if (editableTextField == nil || deviceName == nil) {
        return;
    }

    NSMutableArray<NSLayoutConstraint *> *constraintsToDeactivate = [NSMutableArray array];
    for (NSLayoutConstraint *constraint in self.view.constraints) {
        BOOL textFieldCenteredOnTitle =
            constraint.firstItem == editableTextField &&
            constraint.firstAttribute == NSLayoutAttributeCenterX &&
            constraint.secondItem == deviceName &&
            constraint.secondAttribute == NSLayoutAttributeCenterX;

        if (textFieldCenteredOnTitle) {
            [constraintsToDeactivate addObject:constraint];
        }
    }

    if (constraintsToDeactivate.count > 0) {
        [NSLayoutConstraint deactivateConstraints:constraintsToDeactivate];
    }

    NSLayoutConstraint *coverTitleStartConstraint =
        [editableTextField.leadingAnchor constraintLessThanOrEqualToAnchor:deviceName.leadingAnchor constant:-8.0];
    coverTitleStartConstraint.priority = UILayoutPriorityDefaultHigh;

    NSLayoutConstraint *screenLeadingLimitConstraint =
        [editableTextField.leadingAnchor constraintGreaterThanOrEqualToAnchor:self.view.safeAreaLayoutGuide.leadingAnchor constant:64.0];
    screenLeadingLimitConstraint.priority = UILayoutPriorityRequired;

    [NSLayoutConstraint activateConstraints:@[
        coverTitleStartConstraint,
        screenLeadingLimitConstraint
    ]];

    [editableTextField setContentHuggingPriority:UILayoutPriorityRequired forAxis:UILayoutConstraintAxisHorizontal];
    [editableTextField setContentCompressionResistancePriority:UILayoutPriorityDefaultLow forAxis:UILayoutConstraintAxisHorizontal];
    _gestureNameTextFieldLayoutConfigured = YES;
}

- (void)gestureNameTextFieldEditingChanged:(UITextField *)sender {
    [sender invalidateIntrinsicContentSize];
    [self.view setNeedsLayout];
    [UIView performWithoutAnimation:^{
        [self.view layoutIfNeeded];
    }];
}

- (void)updateStatusConnectionSizeTo:(CGFloat)size {
    for (NSLayoutConstraint *constraint in statusConnection.constraints) {
        if (constraint.firstItem == statusConnection &&
            constraint.secondItem == nil &&
            (constraint.firstAttribute == NSLayoutAttributeWidth ||
             constraint.firstAttribute == NSLayoutAttributeHeight)) {
            constraint.constant = size;
        }
    }
}

- (void)setupConnectionStatusIndicatorView {
    if (statusConnection == nil || connectionStatusIndicatorView != nil) {
        return;
    }

    [self updateStatusConnectionSizeTo:14.0];

    statusConnection.image = nil;
    statusConnection.backgroundColor = UIColor.clearColor;
    statusConnection.contentMode = UIViewContentModeScaleAspectFit;
    statusConnection.clipsToBounds = NO;

    connectionStatusIndicatorView = [[StatusBarConnectionIndicatorHostView alloc] initWithFrame:CGRectZero];
    connectionStatusIndicatorView.translatesAutoresizingMaskIntoConstraints = NO;
    [statusConnection addSubview:connectionStatusIndicatorView];

    [NSLayoutConstraint activateConstraints:@[
        [connectionStatusIndicatorView.leadingAnchor constraintEqualToAnchor:statusConnection.leadingAnchor],
        [connectionStatusIndicatorView.trailingAnchor constraintEqualToAnchor:statusConnection.trailingAnchor],
        [connectionStatusIndicatorView.topAnchor constraintEqualToAnchor:statusConnection.topAnchor],
        [connectionStatusIndicatorView.bottomAnchor constraintEqualToAnchor:statusConnection.bottomAnchor]
    ]];
}

- (BOOL)isLegacyOpenGLStoryboard {
    return state_btn != nil;
}

- (void)stylizeLegacyStateButton {
    state_btn.layer.cornerRadius = 21;
    state_btn.layer.borderWidth = 2;
    state_btn.layer.borderColor = UIColor.whiteColor.CGColor;
}

- (id)legacyGestureSettingsController {
    Class controllerClass = NSClassFromString(@"GestureSettingsViewController");
    if (controllerClass == nil) {
        controllerClass = NSClassFromString(@"OldMotoricaStart.GestureSettingsViewController");
    }
    if (controllerClass == nil) {
        return nil;
    }
    return [[controllerClass alloc] init];
}

- (NSInteger)legacyIntegerValueFromSelector:(SEL)selector fallback:(NSInteger)fallback {
    id controller = [self legacyGestureSettingsController];
    if (controller == nil || ![controller respondsToSelector:selector]) {
        return fallback;
    }
    typedef NSInteger (*IntegerGetter)(id, SEL);
    IntegerGetter getter = (IntegerGetter)objc_msgSend;
    return getter(controller, selector);
}

- (NSString *)legacyGestureNameForNumber:(NSInteger)number {
    id controller = [self legacyGestureSettingsController];
    SEL selector = @selector(getGestureNameWithNumberGesture:);
    if (controller == nil || ![controller respondsToSelector:selector]) {
        return nil;
    }
    typedef NSString *(*NameGetter)(id, SEL, NSInteger);
    NameGetter getter = (NameGetter)objc_msgSend;
    return getter(controller, selector, number);
}

- (void)legacySetGestureName:(NSString *)name number:(NSInteger)number {
    id controller = [self legacyGestureSettingsController];
    SEL selector = @selector(setNameGestureWithNumberGesture:name:);
    if (controller == nil || ![controller respondsToSelector:selector]) {
        return;
    }
    typedef void (*NameSetter)(id, SEL, NSInteger, NSString *);
    NameSetter setter = (NameSetter)objc_msgSend;
    setter(controller, selector, number, name);
}

- (void)viewWillDisappear:(BOOL)animated {
    [super viewWillDisappear:animated];
    NSLog(@"viewWillDisappear");
    [self unregisterGestureSettingsObserver];
    BOOL leavesScreen = self.isMovingFromParentViewController ||
        self.isBeingDismissed ||
        self.navigationController.isBeingDismissed;
    if (self.useV3Mode && leavesScreen && !_stop) {
        [self stopRendererSavingData:NO];
    }
}

- (void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
    if (self.cardPreviewMode && self.cardPreviewEditingEnabled &&
        _cardRotationPan != nil && _cardTranslationPan != nil) {
        UIView *ancestor = self.view.superview;
        while (ancestor != nil) {
            if ([ancestor isKindOfClass:UIScrollView.class]) {
                UIScrollView *scrollView = (UIScrollView *)ancestor;
                [scrollView.panGestureRecognizer requireGestureRecognizerToFail:_cardRotationPan];
                [scrollView.panGestureRecognizer requireGestureRecognizerToFail:_cardTranslationPan];
                [scrollView.panGestureRecognizer requireGestureRecognizerToFail:_cardRollGesture];
                [scrollView.panGestureRecognizer requireGestureRecognizerToFail:_cardDepthPan];
            }
            ancestor = ancestor.superview;
        }
    }
    if (!self.modelTestMode) {
        [self injectGestureSettingsV3ForUITestIfNeeded];
    }
}

- (void)viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];
    if (!self.modelTestMode) {
        [self registerGestureSettingsObserverIfNeeded];
    }
//    SharedParameterRef *latestParameterRef = [GestureSettingsViewModel shared].latestParameterRef;
//    if (latestParameterRef != nil) {
//        [self applyGestureSettingsUpdate:latestParameterRef];1
//    }
//    GestureSettingsViewModel *viewModel = [GestureSettingsViewModel shared];
//    SharedParameterRef *latestParameterRef = viewModel.latestParameterRef;
//    if (latestParameterRef != nil) {
//        NSDictionary *userInfo = @{@"data": latestParameterRef};
//        NSNotification *notification = [NSNotification notificationWithName:GestureSettingsViewModelDidUpdateNotification
//                                                                      object:viewModel
//                                                                    userInfo:userInfo];
//        [self handleGestureSettingsUpdate:notification];
//    }
}
- (void)viewDidLoad {
    CFTimeInterval viewDidLoadStartedAt = CACurrentMediaTime();
    [super viewDidLoad];
    NSString *localizedSaveTitle = NSLocalizedString(@"save", nil);
    if (@available(iOS 15.0, *)) {
        UIButtonConfiguration *configuration = saveBtn.configuration;
        NSAttributedString *currentTitle = configuration.attributedTitle;
        NSDictionary<NSAttributedStringKey, id> *attributes =
            currentTitle.length > 0 ? [currentTitle attributesAtIndex:0 effectiveRange:NULL] : @{};
        configuration.attributedTitle =
            [[NSAttributedString alloc] initWithString:localizedSaveTitle attributes:attributes];
        saveBtn.configuration = configuration;
    } else {
        NSAttributedString *currentTitle = [saveBtn attributedTitleForState:UIControlStateNormal];
        NSDictionary<NSAttributedStringKey, id> *attributes =
            currentTitle.length > 0 ? [currentTitle attributesAtIndex:0 effectiveRange:NULL] : @{};
        [saveBtn setAttributedTitle:[[NSAttributedString alloc] initWithString:localizedSaveTitle
                                                                    attributes:attributes]
                          forState:UIControlStateNormal];
    }
    NSLog(@"[V3OpenTrace] event=viewDidLoadBegin thread=main useV3Mode=%d modelTestMode=%d gestureId=%ld",
          self.useV3Mode,
          self.modelTestMode,
          (long)self.gestureNumber);
    NSLog(@"Отсюда мы начинаем исполнение программы");
    if (self.modelTestMode) {
        NSLog(@"[V3TestMetrics] controllerViewDidLoad");
    }
    if (self.cardPreviewMode && self.cardPreviewEditingEnabled) {
        _cardRotationPan = [[UIPanGestureRecognizer alloc] initWithTarget:self action:@selector(handleCardPreviewRotation:)];
        _cardRotationPan.minimumNumberOfTouches = 1;
        _cardRotationPan.maximumNumberOfTouches = 1;
        _cardRotationPan.cancelsTouchesInView = YES;
        _cardRotationPan.delegate = self;
        [self.view addGestureRecognizer:_cardRotationPan];
        UIPinchGestureRecognizer *pinch = [[UIPinchGestureRecognizer alloc] initWithTarget:self action:@selector(handleCardPreviewPinch:)];
        pinch.delegate = self;
        [self.view addGestureRecognizer:pinch];
        _cardTranslationPan = [[UIPanGestureRecognizer alloc] initWithTarget:self action:@selector(handleCardPreviewPan:)];
        _cardTranslationPan.minimumNumberOfTouches = 2;
        _cardTranslationPan.maximumNumberOfTouches = 2;
        _cardTranslationPan.cancelsTouchesInView = YES;
        _cardTranslationPan.delegate = self;
        [self.view addGestureRecognizer:_cardTranslationPan];
        _cardRollGesture = [[UIRotationGestureRecognizer alloc] initWithTarget:self action:@selector(handleCardPreviewRoll:)];
        _cardRollGesture.delegate = self;
        [self.view addGestureRecognizer:_cardRollGesture];
        _cardDepthPan = [[UIPanGestureRecognizer alloc] initWithTarget:self action:@selector(handleCardPreviewDepth:)];
        _cardDepthPan.minimumNumberOfTouches = 3;
        _cardDepthPan.maximumNumberOfTouches = 3;
        _cardDepthPan.delegate = self;
        [self.view addGestureRecognizer:_cardDepthPan];
        self.view.multipleTouchEnabled = YES;
    }
    if (self.useV3Mode) {
        _v3RenderQueue = dispatch_queue_create("com.bailout.stickk.v3-render", DISPATCH_QUEUE_SERIAL);
        dispatch_queue_set_specific(_v3RenderQueue,
                                    V3RenderQueueSpecificKey,
                                    (void *)V3RenderQueueSpecificKey,
                                    NULL);
        _v3FramePending = NO;
        _v3TouchActive = NO;
        _v3GestureSettingsReady = NO;
        _v3FirstFrameReady = NO;
        _v3InitialOpenSent = NO;
        if (!self.modelTestMode && self.useV3GestureProtocol) {
            [[NSNotificationCenter defaultCenter] addObserver:self
                                                     selector:@selector(handleV3HandSideChange:)
                                                         name:@"V3HandSideDidChange"
                                                       object:nil];
        }
    }
    if (!self.modelTestMode) {
        gestureService = [[GestureService alloc] init];
        [self registerGestureSettingsObserverIfNeeded];
        connectStatus = [UIImage imageNamed: @"connect_status.png"];
        disconnectStatus = [UIImage imageNamed: @"disconnect_status.png"];
        [gestureService getDeviceName];
    }
    state = 0;
    if (self.modelTestMode) {
        for (UIView *subview in self.view.subviews) {
            subview.hidden = YES;
        }
    } else if ([self isLegacyOpenGLStoryboard]) {
        [self stylizeLegacyStateButton];
        statusConnection.image = ([self legacyIntegerValueFromSelector:@selector(getStatusConnection) fallback:0] == 1)
            ? connectStatus
            : disconnectStatus;
    } else {
        [self configureGestureHeaderAppearance];
        [self setupConnectionStatusIndicatorView];
        [self setupStateSegmentedControl];
    }

    
    
    NSInteger selectedGestureNumber = self.modelTestMode ? 0 : self.gestureNumber;
    if (!self.modelTestMode && [self isLegacyOpenGLStoryboard]) {
        selectedGestureNumber = [self legacyIntegerValueFromSelector:@selector(getGestureNum) fallback:0];
        if (selectedGestureNumber == 0) { selectedGestureNumber = 1; }
    } else if (!self.modelTestMode && selectedGestureNumber == 0) {
        selectedGestureNumber = 64;
    }
    _gestureNumber = selectedGestureNumber;
    if (self.modelTestMode) {
        deviceName.text = @"";
    } else if ([self isLegacyOpenGLStoryboard]) {
        NSString *legacyName = [self legacyGestureNameForNumber:_gestureNumber];
        if (_gestureNumber != 0 && legacyName.length > 4) {
            legacyName = [legacyName substringFromIndex:4];
        }
        deviceName.text = legacyName ?: [NSString stringWithFormat:@"gesture %ld", (long)_gestureNumber];
    } else {
        deviceName.text = [gestureService getGestureNameWithNumberGesture: _gestureNumber];
    }
    
    showRenameTextField = false;

    _stop = false;
    _previousX = 0.0f;
    _previousY = 0.0f;
    _didInjectGestureSettingsV3ForUITest = NO;
    if (!self.modelTestMode) {
        deviceName.accessibilityIdentifier = GestureSettingsScreenAccessibilityIdentifier;
    }
    
    _view = (AAPLOpenGLViewV3 *)self.view;
    
    CFTimeInterval prepareStartedAt = CACurrentMediaTime();
    [self prepareView];
    NSLog(@"[V3OpenTrace] event=prepareViewReturned thread=main useV3Mode=%d durationMs=%.3f gestureId=%ld",
          self.useV3Mode,
          (CACurrentMediaTime() - prepareStartedAt) * 1000.0,
          (long)_gestureNumber);

    CFTimeInterval rendererStartedAt = CACurrentMediaTime();
    void (^createRenderer)(void) = ^{
        CFTimeInterval blockStartedAt = CACurrentMediaTime();
        NSLog(@"[V3OpenTrace] event=rendererInitBegin thread=%@ useV3Mode=%d gestureId=%ld defaultFBO=%u",
              NSThread.isMainThread ? @"main" : @"render",
              self.useV3Mode,
              (long)self->_gestureNumber,
              self->_defaultFBOName);
        [self makeCurrentContext];
        Class rendererClass = [AAPLOpenGLViewControllerV3 rendererClassForV3Mode:self.useV3Mode];
        if (self.useV3Mode) {
            NSInteger handSide = self.cardPreviewMode
                ? 1
                : (self.useV3GestureProtocol
                ? [V3HandSideProvider shared].currentSide
                : [gestureService getLegacyHandSide]);
            self->_openGLRenderer = [[AAPLOpenGLRendererV3 alloc]
                initWithDefaultFBOName:self->_defaultFBOName
                gestureNumber:self->_gestureNumber
                useV3GestureProtocol:self.useV3GestureProtocol
                handSide:handSide];
        } else {
            self->_openGLRenderer = [[rendererClass alloc]
                initWithDefaultFBOName:self->_defaultFBOName
                gestureNumber:self->_gestureNumber];
        }
        if (!self->_openGLRenderer) return;
        if (self.cardPreviewMode && self.useV3Mode) {
            if (self.cardPreviewClipKind == 1) {
                [(AAPLOpenGLRendererV3 *)self->_openGLRenderer configureCupGripCardPreview];
            } else {
                [(AAPLOpenGLRendererV3 *)self->_openGLRenderer configureGestureKeyCardPreview];
            }
        }
        [self->_openGLRenderer resize:self.drawableSize];
        CGRect screenRect = UIScreen.mainScreen.bounds;
        [self->_openGLRenderer calculationOfCoefficients:screenRect.size.width :screenRect.size.height];
        NSLog(@"[V3OpenTrace] event=rendererInitEnd thread=%@ useV3Mode=%d durationMs=%.3f gestureId=%ld",
              NSThread.isMainThread ? @"main" : @"render",
              self.useV3Mode,
              (CACurrentMediaTime() - blockStartedAt) * 1000.0,
              (long)self->_gestureNumber);
    };
    if (self.useV3Mode) {
        [self performV3RenderSync:createRenderer];
    } else {
        createRenderer();
    }

    if (!_openGLRenderer) {
        NSLog(@"OpenGL renderer failed initialization.");
        return;
    }
    if (self.modelTestMode) {
        NSLog(@"[V3TestMetrics] rendererReady controllerToRendererMs=%.3f",
              (CACurrentMediaTime() - rendererStartedAt) * 1000.0);
    }

    NSLog(@"Размер экрана screenWidth: %f screenHeight: %f",
          UIScreen.mainScreen.bounds.size.width,
          UIScreen.mainScreen.bounds.size.height);
    if (self.useV3Mode) {
        if (!self.modelTestMode && !self.useV3GestureProtocol) {
            SharedParameterRef *latestParameterRef = [GestureSettingsViewModel shared].latestParameterRef;
            if (latestParameterRef != nil) {
                [self applyGestureSettingsUpdate:latestParameterRef parameterData:nil];
            }
        }
        if (!self.modelTestMode && self.useV3GestureProtocol && _gestureNumber > 0) {
            NSLog(@"[V3OpenTrace] event=requestGestureSettings thread=main gestureId=%ld useV3Mode=%d",
                  (long)_gestureNumber,
                  self.useV3Mode);
            [gestureService requestGestureSettingsV3WithGestureId:(int)_gestureNumber];
        }
        [self requestV3Frame];
    }
    NSLog(@"[V3OpenTrace] event=viewDidLoadEnd thread=main useV3Mode=%d durationMs=%.3f gestureId=%ld",
          self.useV3Mode,
          (CACurrentMediaTime() - viewDidLoadStartedAt) * 1000.0,
          (long)_gestureNumber);
}

- (void)handleCardPreviewPinch:(UIPinchGestureRecognizer *)recognizer {
    if (!self.cardPreviewMode || _openGLRenderer == nil) return;
    CGFloat factor = recognizer.scale;
    recognizer.scale = 1.0;
    BOOL finished = recognizer.state == UIGestureRecognizerStateEnded ||
                    recognizer.state == UIGestureRecognizerStateCancelled;
    [self performV3RenderAsync:^{
        [EAGLContext setCurrentContext:self->_context];
        [(AAPLOpenGLRendererV3 *)self->_openGLRenderer adjustCardPreviewScaleByFactor:factor finished:finished];
    }];
    [self requestV3Frame];
}

- (void)handleCardPreviewRotation:(UIPanGestureRecognizer *)recognizer {
    if (!self.cardPreviewMode || _openGLRenderer == nil) return;
    CGPoint delta = [recognizer translationInView:self.view];
    [recognizer setTranslation:CGPointZero inView:self.view];
    BOOL finished = recognizer.state == UIGestureRecognizerStateEnded ||
                    recognizer.state == UIGestureRecognizerStateCancelled;
    [self performV3RenderAsync:^{
        [EAGLContext setCurrentContext:self->_context];
        [(AAPLOpenGLRendererV3 *)self->_openGLRenderer adjustCardPreviewRotationByX:delta.x / 3.0
                                                                                y:delta.y / 3.0
                                                                         finished:finished];
    }];
    [self requestV3Frame];
}

- (void)handleCardPreviewPan:(UIPanGestureRecognizer *)recognizer {
    if (!self.cardPreviewMode || _openGLRenderer == nil) return;
    CGPoint delta = [recognizer translationInView:self.view];
    [recognizer setTranslation:CGPointZero inView:self.view];
    BOOL finished = recognizer.state == UIGestureRecognizerStateEnded ||
                    recognizer.state == UIGestureRecognizerStateCancelled;
    [self performV3RenderAsync:^{
        [EAGLContext setCurrentContext:self->_context];
        [(AAPLOpenGLRendererV3 *)self->_openGLRenderer adjustCardPreviewPositionByX:delta.x * 0.5
                                                                                 y:delta.y * 0.5
                                                                          finished:finished];
    }];
    [self requestV3Frame];
}

- (void)handleCardPreviewRoll:(UIRotationGestureRecognizer *)recognizer {
    if (!self.cardPreviewMode || _openGLRenderer == nil) return;
    CGFloat radians = recognizer.rotation;
    recognizer.rotation = 0.0;
    BOOL finished = recognizer.state == UIGestureRecognizerStateEnded || recognizer.state == UIGestureRecognizerStateCancelled;
    [self performV3RenderAsync:^{
        [EAGLContext setCurrentContext:self->_context];
        [(AAPLOpenGLRendererV3 *)self->_openGLRenderer adjustCardPreviewRollByRadians:radians finished:finished];
    }];
    [self requestV3Frame];
}

- (void)handleCardPreviewDepth:(UIPanGestureRecognizer *)recognizer {
    if (!self.cardPreviewMode || _openGLRenderer == nil) return;
    CGPoint delta = [recognizer translationInView:self.view];
    [recognizer setTranslation:CGPointZero inView:self.view];
    BOOL finished = recognizer.state == UIGestureRecognizerStateEnded || recognizer.state == UIGestureRecognizerStateCancelled;
    [self performV3RenderAsync:^{
        [EAGLContext setCurrentContext:self->_context];
        [(AAPLOpenGLRendererV3 *)self->_openGLRenderer adjustCardPreviewDepthBy:-delta.y * 0.5 finished:finished];
    }];
    [self requestV3Frame];
}

- (BOOL)gestureRecognizer:(UIGestureRecognizer *)gestureRecognizer
        shouldRecognizeSimultaneouslyWithGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer {
    if (gestureRecognizer.view != self.view || otherGestureRecognizer.view != self.view) return NO;
    return gestureRecognizer != _cardRotationPan && otherGestureRecognizer != _cardRotationPan;
}

- (void)setCardPreviewEditingKey:(BOOL)editingKey {
    if (!self.cardPreviewMode || _openGLRenderer == nil) return;
    [self performV3RenderAsync:^{
        [(AAPLOpenGLRendererV3 *)self->_openGLRenderer setCardPreviewEditingKey:editingKey];
    }];
}

- (void)playGestureKeyClip {
    NSLog(@"[GestureKeyTrace] event=controllerPlay hasRenderer=%d useV3=%d stop=%d", _openGLRenderer != nil, self.useV3Mode, _stop);
    if (!self.useV3Mode || _openGLRenderer == nil) return;
    [self performV3RenderAsync:^{
        [EAGLContext setCurrentContext:self->_context];
        [(AAPLOpenGLRendererV3 *)self->_openGLRenderer playGestureKeyClip];
    }];
    [self requestV3Frame];
}

- (void)playCupGripClip {
    NSLog(@"[CupGripTrace] event=controllerPlay hasRenderer=%d useV3=%d stop=%d", _openGLRenderer != nil, self.useV3Mode, _stop);
    if (!self.useV3Mode || _openGLRenderer == nil) return;
    [self performV3RenderAsync:^{
        [EAGLContext setCurrentContext:self->_context];
        [(AAPLOpenGLRendererV3 *)self->_openGLRenderer playCupGripClip];
    }];
    [self requestV3Frame];
}

- (void)stopCardPreview {
    if (!_stop) [self stopRendererSavingData:NO];
}

- (void)viewDidLayoutSubviews {
    [super viewDidLayoutSubviews];
    if (segmentContainer != nil) {
        [self selectStateSegmentIndex:selectedStateSegmentIndex animated:NO notifyRenderer:NO];
    }
    CGSize boundsSize = self.view.bounds.size;
    if (!self.useV3Mode || _openGLRenderer == nil || boundsSize.width <= 0.0 || boundsSize.height <= 0.0 ||
        CGSizeEqualToSize(boundsSize, _lastDrawableBoundsSize)) {
        return;
    }
    _lastDrawableBoundsSize = boundsSize;
    [self performV3RenderSync:^{
        [self resizeDrawable];
        CGRect screenRect = UIScreen.mainScreen.bounds;
        [(AAPLOpenGLRendererV3 *)self->_openGLRenderer calculationOfCoefficients:screenRect.size.width
                                                                                   :screenRect.size.height];
    }];
    [self requestV3Frame];
}

- (IBAction)unwindToOpenGLVC:(UIStoryboardSegue *)segue {}

- (IBAction)perehod:(UIButton *)sender {
    [self stopRendererSavingData:NO];
    
    if (showRenameTextField) {
        NSString *result = @"";
        result = [result stringByAppendingString:[self resolvedTextField].text];
        if ([self isLegacyOpenGLStoryboard]) {
            NSString *legacyResult = [@"    " stringByAppendingString:result];
            [self legacySetGestureName:legacyResult number:_gestureNumber];
        } else {
            [gestureService setNameGestureWithNumberGesture: _gestureNumber name:result];
        }
    }
}
- (IBAction)perehodWithSaveData:(UIButton *)sender {
    [self stopRendererSavingData:YES];
}

- (void)stateSegmentChanged:(UISegmentedControl *)sender {
    [self selectStateSegmentIndex:sender.selectedSegmentIndex animated:YES notifyRenderer:YES];
}

- (void)stateSegmentButtonTapped:(UIButton *)sender {
    [self selectStateSegmentIndex:sender.tag animated:YES notifyRenderer:YES];
}

- (IBAction)chageState:(UIButton *)sender {
    state = !state;
    [state_btn setTitle:(state ? [gestureService gestureStateClose] : [gestureService gestureStateOpen])
               forState:UIControlStateNormal];
    if (_openGLRenderer != nil) {
        [self setRendererClosedState:state];
    }
}

- (UIButton *)makeStateSegmentButtonWithTitle:(NSString *)title tag:(NSInteger)tag {
    UIButton *button = [UIButton buttonWithType:UIButtonTypeCustom];
    button.translatesAutoresizingMaskIntoConstraints = NO;
    [button setTitle:title forState:UIControlStateNormal];
    button.titleLabel.font = [UIFont systemFontOfSize:12 weight:UIFontWeightLight];
    button.tag = tag;
    [button addTarget:self action:@selector(stateSegmentButtonTapped:) forControlEvents:UIControlEventTouchUpInside];
    return button;
}

- (void)updateStateSegmentButtonColors {
    UIColor *activeColor = UIColor.whiteColor;
    UIColor *inactiveColor = [UIColor colorNamed:@"ubi4_deactivate_text"] ?: UIColor.lightGrayColor;
    [stateOpenButton setTitleColor:(selectedStateSegmentIndex == 0 ? activeColor : inactiveColor) forState:UIControlStateNormal];
    [stateCloseButton setTitleColor:(selectedStateSegmentIndex == 1 ? activeColor : inactiveColor) forState:UIControlStateNormal];
}

- (CGFloat)stateSegmentTargetOffsetForIndex:(NSInteger)index {
    [segmentContainer layoutIfNeeded];
    CGFloat segmentWidth = stateSegmentContentView.bounds.size.width / 2.0;
    return MAX(0.0, segmentWidth * index);
}

- (void)selectStateSegmentIndex:(NSInteger)index animated:(BOOL)animated notifyRenderer:(BOOL)notifyRenderer {
    if (segmentContainer == nil) {
        return;
    }
    NSInteger clampedIndex = MAX(0, MIN(1, index));
    selectedStateSegmentIndex = clampedIndex;
    state = (clampedIndex == 1);

    [segmentContainer layoutIfNeeded];
    CGFloat targetOffset = [self stateSegmentTargetOffsetForIndex:clampedIndex];

    void (^applySelectionLayout)(void) = ^{
        stateHighlightLeadingConstraint.constant = targetOffset;
        [self updateStateSegmentButtonColors];
        [segmentContainer layoutIfNeeded];
    };

    if (animated) {
        [UIView animateWithDuration:0.30
                              delay:0.0
                            options:UIViewAnimationOptionCurveEaseInOut | UIViewAnimationOptionAllowUserInteraction
                         animations:applySelectionLayout
                         completion:nil];
    } else {
        applySelectionLayout();
    }

    if (notifyRenderer && _openGLRenderer != nil) {
        [self setRendererClosedState:state];
    }
}

- (IBAction)openFingersDelayDialog:(UIButton *)sender {
    __block BOOL currentState = NO;
    __block NSArray<NSNumber *> *openToClose = nil;
    __block NSArray<NSNumber *> *closeToOpen = nil;
    void (^readRendererState)(void) = ^{
        [self->_openGLRenderer openFingersDelayDialog];
        currentState = [self->_openGLRenderer currentGestureState];
        openToClose = [self->_openGLRenderer currentOpenToCloseShifts];
        closeToOpen = [self->_openGLRenderer currentCloseToOpenShifts];
    };
    self.useV3Mode ? [self performV3RenderSync:readRendererState] : readRendererState();

    if (currentState) {
        // закрытое состояние
        NSArray<NSNumber *> *delayValues = openToClose;
        __weak typeof(self) weakSelf = self;
        [FingersDelayDialogPresenter presentFrom:self
                                           title:[KmmLocalizedStrings delayStateTitle]
                                        subTitle:[KmmLocalizedStrings delayStateCloseDescription]
                                       saveTitle:[KmmLocalizedStrings dialogSave]
                                     cancelTitle:[KmmLocalizedStrings dialogCancel]
                                      delayValues:delayValues
                                           onSave:^(NSArray<NSNumber *> *updatedValues) {
            __strong typeof(weakSelf) strongSelf = weakSelf;
            if (!strongSelf || updatedValues.count < 6) { return; }
            void (^applyValues)(void) = ^{
                [strongSelf->_openGLRenderer applyOpenToCloseShifts:updatedValues];
            };
            strongSelf.useV3Mode ? [strongSelf performV3RenderAsync:applyValues] : applyValues();
        }];
    } else {
        // открытое состояние
        NSArray<NSNumber *> *delayValues = closeToOpen;
        __weak typeof(self) weakSelf = self;
        [FingersDelayDialogPresenter presentFrom:self
                                           title:[KmmLocalizedStrings delayStateTitle]
                                        subTitle:[KmmLocalizedStrings delayStateOpenDescription]
                                       saveTitle:[KmmLocalizedStrings dialogSave]
                                     cancelTitle:[KmmLocalizedStrings dialogCancel]
                                      delayValues:delayValues
                                           onSave:^(NSArray<NSNumber *> *updatedValues) {
            __strong typeof(weakSelf) strongSelf = weakSelf;
            if (!strongSelf || updatedValues.count < 6) { return; }
            void (^applyValues)(void) = ^{
                [strongSelf->_openGLRenderer applyCloseToOpenShifts:updatedValues];
            };
            strongSelf.useV3Mode ? [strongSelf performV3RenderAsync:applyValues] : applyValues();
        }];
    }
}

- (IBAction)openFingersDealyDialog:(UIButton *)sender {
    if (_openGLRenderer != nil) {
        [_openGLRenderer openFingersDelayDialog];
    }
}

- (IBAction)renameGesture:(UIButton *)sender {
    UITextField *editableTextField = [self resolvedTextField];
    if (showRenameTextField) {
        editableTextField.hidden = YES;
        [editableTextField resignFirstResponder];
        showRenameTextField = false;
        deviceName.text = editableTextField.text;
        NSString *result = @"";
        result = [result stringByAppendingString:editableTextField.text];
        if ([self isLegacyOpenGLStoryboard]) {
            NSString *legacyResult = [@"    " stringByAppendingString:result];
            [self legacySetGestureName:legacyResult number:_gestureNumber];
        } else {
            [gestureService setNameGestureWithNumberGesture: _gestureNumber name:result];
        }
        [renameBtn setImage:[UIImage imageNamed:@"rename.png"]   forState:UIControlStateNormal];
    } else {
        editableTextField.hidden = NO;
        [editableTextField becomeFirstResponder];
        showRenameTextField = true;
        editableTextField.text = deviceName.text;
        [self gestureNameTextFieldEditingChanged:editableTextField];
        [renameBtn setImage:[UIImage imageNamed:@"ok.png"]   forState:UIControlStateNormal];
    }
}

- (void)setupStateSegmentedControl {
    segmentContainer = [[UIView alloc] init];
    segmentContainer.translatesAutoresizingMaskIntoConstraints = NO;
    [self.view addSubview:segmentContainer];
    segmentContainer.backgroundColor = UIColor.clearColor;
    
    NSLayoutConstraint *leading = [segmentContainer.leadingAnchor constraintEqualToAnchor:self.view.leadingAnchor constant:48];
    NSLayoutConstraint *trailing = [segmentContainer.trailingAnchor constraintEqualToAnchor:self.view.trailingAnchor constant:-48];
    NSLayoutConstraint *bottom = [segmentContainer.bottomAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.bottomAnchor constant:-48];
    NSLayoutConstraint *height = [segmentContainer.heightAnchor constraintEqualToConstant:48];
    [NSLayoutConstraint activateConstraints:@[leading, trailing, bottom, height]];

    stateSegmentBackgroundView = [[UIView alloc] init];
    stateSegmentBackgroundView.translatesAutoresizingMaskIntoConstraints = NO;
    stateSegmentBackgroundView.backgroundColor = [UIColor colorNamed:@"ubi4_gray"] ?: UIColor.darkGrayColor;
    stateSegmentBackgroundView.layer.cornerRadius = 12;
    stateSegmentBackgroundView.layer.borderWidth = 1;
    stateSegmentBackgroundView.layer.borderColor = ([UIColor colorNamed:@"ubi4_gray_border"] ?: UIColor.clearColor).CGColor;
    stateSegmentBackgroundView.layer.shadowColor = UIColor.blackColor.CGColor;
    stateSegmentBackgroundView.layer.shadowOpacity = 0.25;
    stateSegmentBackgroundView.layer.shadowOffset = CGSizeMake(0, 2);
    stateSegmentBackgroundView.layer.shadowRadius = 3;
    stateSegmentBackgroundView.layer.masksToBounds = NO;
    [segmentContainer addSubview:stateSegmentBackgroundView];
    
    [NSLayoutConstraint activateConstraints:@[
        [stateSegmentBackgroundView.leadingAnchor constraintEqualToAnchor:segmentContainer.leadingAnchor],
        [stateSegmentBackgroundView.trailingAnchor constraintEqualToAnchor:segmentContainer.trailingAnchor],
        [stateSegmentBackgroundView.topAnchor constraintEqualToAnchor:segmentContainer.topAnchor],
        [stateSegmentBackgroundView.bottomAnchor constraintEqualToAnchor:segmentContainer.bottomAnchor]
    ]];

    stateSegmentContentView = [[UIView alloc] init];
    stateSegmentContentView.translatesAutoresizingMaskIntoConstraints = NO;
    stateSegmentContentView.backgroundColor = UIColor.clearColor;
    [stateSegmentBackgroundView addSubview:stateSegmentContentView];
    [NSLayoutConstraint activateConstraints:@[
        [stateSegmentContentView.leadingAnchor constraintEqualToAnchor:stateSegmentBackgroundView.leadingAnchor constant:1],
        [stateSegmentContentView.trailingAnchor constraintEqualToAnchor:stateSegmentBackgroundView.trailingAnchor constant:-1],
        [stateSegmentContentView.topAnchor constraintEqualToAnchor:stateSegmentBackgroundView.topAnchor constant:1],
        [stateSegmentContentView.bottomAnchor constraintEqualToAnchor:stateSegmentBackgroundView.bottomAnchor constant:-1]
    ]];

    stateSegmentHighlightView = [[UIView alloc] init];
    stateSegmentHighlightView.translatesAutoresizingMaskIntoConstraints = NO;
    stateSegmentHighlightView.backgroundColor = [UIColor colorNamed:@"ubi4_back"] ?: UIColor.blackColor;
    stateSegmentHighlightView.layer.cornerRadius = 10;
    stateSegmentHighlightView.layer.masksToBounds = YES;
    [stateSegmentContentView addSubview:stateSegmentHighlightView];

    stateOpenButton = [self makeStateSegmentButtonWithTitle:[gestureService gestureStateOpen] tag:0];
    stateCloseButton = [self makeStateSegmentButtonWithTitle:[gestureService gestureStateClose] tag:1];

    UIStackView *buttonsStackView = [[UIStackView alloc] initWithArrangedSubviews:@[stateOpenButton, stateCloseButton]];
    buttonsStackView.translatesAutoresizingMaskIntoConstraints = NO;
    buttonsStackView.axis = UILayoutConstraintAxisHorizontal;
    buttonsStackView.alignment = UIStackViewAlignmentFill;
    buttonsStackView.distribution = UIStackViewDistributionFillEqually;
    buttonsStackView.spacing = 0;
    [stateSegmentContentView addSubview:buttonsStackView];
    [NSLayoutConstraint activateConstraints:@[
        [buttonsStackView.leadingAnchor constraintEqualToAnchor:stateSegmentContentView.leadingAnchor],
        [buttonsStackView.trailingAnchor constraintEqualToAnchor:stateSegmentContentView.trailingAnchor],
        [buttonsStackView.topAnchor constraintEqualToAnchor:stateSegmentContentView.topAnchor],
        [buttonsStackView.bottomAnchor constraintEqualToAnchor:stateSegmentContentView.bottomAnchor]
    ]];

    stateHighlightLeadingConstraint = [stateSegmentHighlightView.leadingAnchor constraintEqualToAnchor:stateSegmentContentView.leadingAnchor constant:0.0];
    [NSLayoutConstraint activateConstraints:@[
        [stateSegmentHighlightView.topAnchor constraintEqualToAnchor:stateSegmentContentView.topAnchor],
        [stateSegmentHighlightView.bottomAnchor constraintEqualToAnchor:stateSegmentContentView.bottomAnchor],
        [stateSegmentHighlightView.widthAnchor constraintEqualToAnchor:stateOpenButton.widthAnchor],
        stateHighlightLeadingConstraint
    ]];

    selectedStateSegmentIndex = state ? 1 : 0;
    [self selectStateSegmentIndex:selectedStateSegmentIndex animated:NO notifyRenderer:NO];
}

- (void)prepareView {
    CFTimeInterval prepareStartedAt = CACurrentMediaTime();
    NSLog(@"[V3OpenTrace] event=prepareViewBegin thread=main useV3Mode=%d gestureId=%ld",
          self.useV3Mode,
          (long)_gestureNumber);
    NSLog(@"1 - Подготавливаем вью");
    CAEAGLLayer *eaglLayer = (CAEAGLLayer *)self.view.layer;
    NSString *colorFormat = self.useV3Mode ? kEAGLColorFormatRGBA8 : kEAGLColorFormatSRGBA8;

    eaglLayer.drawableProperties = @{kEAGLDrawablePropertyRetainedBacking : @NO,
                                     kEAGLDrawablePropertyColorFormat     : colorFormat };
    eaglLayer.opaque = self.cardPreviewMode ? NO : YES;
    

    CFTimeInterval contextStartedAt = CACurrentMediaTime();
    NSLog(@"[V3OpenTrace] event=newSharedContextBegin thread=main useV3Mode=%d cacheState=%ld gestureId=%ld",
          self.useV3Mode,
          (long)[V3ModelResourceCache sharedCache].state,
          (long)_gestureNumber);
    _context = self.useV3Mode
        ? [[V3ModelResourceCache sharedCache] newSharedContext]
        : [[EAGLContext alloc] initWithAPI:kEAGLRenderingAPIOpenGLES2];
    NSLog(@"[V3OpenTrace] event=newSharedContextEnd thread=main useV3Mode=%d durationMs=%.3f success=%d cacheState=%ld gestureId=%ld",
          self.useV3Mode,
          (CACurrentMediaTime() - contextStartedAt) * 1000.0,
          _context != nil,
          (long)[V3ModelResourceCache sharedCache].state,
          (long)_gestureNumber);

    if (!_context) {
        NSLog(@"Could not create an OpenGL ES context.");
        return;
    }

    self.view.contentScaleFactor = [UIScreen mainScreen].nativeScale;

    void (^createDrawableResources)(void) = ^{
        CFTimeInterval drawableStartedAt = CACurrentMediaTime();
        NSLog(@"[V3OpenTrace] event=createDrawableResourcesBegin thread=%@ useV3Mode=%d gestureId=%ld",
              NSThread.isMainThread ? @"main" : @"render",
              self.useV3Mode,
              (long)self->_gestureNumber);
        [self makeCurrentContext];
        glGenFramebuffers(1, &self->_presentationFBOName);
        glGenRenderbuffers(1, &self->_colorRenderbuffer);
        glGenRenderbuffers(1, &self->_depthRenderbuffer);
        [self resizeDrawable];
        NSLog(@"[V3OpenTrace] event=createDrawableResourcesEnd thread=%@ useV3Mode=%d durationMs=%.3f defaultFBO=%u msaa=%d gestureId=%ld",
              NSThread.isMainThread ? @"main" : @"render",
              self.useV3Mode,
              (CACurrentMediaTime() - drawableStartedAt) * 1000.0,
              self->_defaultFBOName,
              self->_multisampleSamples,
              (long)self->_gestureNumber);
    };
    if (self.useV3Mode) {
        [self performV3RenderSync:createDrawableResources];
    } else {
        createDrawableResources();
    }
    _lastDrawableBoundsSize = self.view.bounds.size;

    // Create the display link so you render at 60 frames per second (FPS).
    _displayLink = [CADisplayLink displayLinkWithTarget:self selector:@selector(draw:)];

    _displayLink.preferredFramesPerSecond = 60;

    // Set the display link to run on the default run loop (and the main thread).
    [_displayLink addToRunLoop:[NSRunLoop currentRunLoop] forMode:NSDefaultRunLoopMode];
    _displayLink.paused = self.useV3Mode;
    NSLog(@"[V3OpenTrace] event=displayLinkCreated thread=main paused=%d useV3Mode=%d gestureId=%ld",
          _displayLink.paused,
          self.useV3Mode,
          (long)_gestureNumber);
    
    if (!self.modelTestMode) {
        UIButton *delayButton = [self resolvedFingersDelayButton];
        BOOL shouldShowDelayButton = [self isLegacyOpenGLStoryboard]
            ? ([self legacyIntegerValueFromSelector:@selector(getFingersDelaySwitch) fallback:0] &&
               [self legacyIntegerValueFromSelector:@selector(getUseFestX) fallback:0])
            : [gestureService getFingersDelaySwitch];
        if (shouldShowDelayButton) {
            [delayButton setAlpha:1];
        } else { [delayButton setAlpha:0]; }
    }
    NSLog(@"[V3OpenTrace] event=prepareViewEnd thread=main useV3Mode=%d durationMs=%.3f gestureId=%ld",
          self.useV3Mode,
          (CACurrentMediaTime() - prepareStartedAt) * 1000.0,
          (long)_gestureNumber);
}

- (void)makeCurrentContext {
    NSLog(@"2 - Создаём контекст этого вью");
    [EAGLContext setCurrentContext:_context];
}

- (CGSize)drawableSize {
    GLint backingWidth, backingHeight;
    glBindRenderbuffer(GL_RENDERBUFFER, _colorRenderbuffer);
    glGetRenderbufferParameteriv(GL_RENDERBUFFER, GL_RENDERBUFFER_WIDTH, &backingWidth);
    glGetRenderbufferParameteriv(GL_RENDERBUFFER, GL_RENDERBUFFER_HEIGHT, &backingHeight);
    CGSize drawableSize = {backingWidth, backingHeight};
    NSLog(@"3 - Подгонка размера вью под размер экрана backingWidth: %d  backingHeight: %d", backingWidth, backingHeight);
    return drawableSize;
}

- (void)resizeDrawable {
    CFTimeInterval resizeStartedAt = CACurrentMediaTime();
    NSLog(@"[V3OpenTrace] event=resizeDrawableBegin thread=%@ useV3Mode=%d gestureId=%ld",
          NSThread.isMainThread ? @"main" : @"render",
          self.useV3Mode,
          (long)_gestureNumber);
    [self makeCurrentContext];
    if (_colorRenderbuffer == 0 || _presentationFBOName == 0) {
        NSLog(@"[V3OpenTrace] event=resizeDrawableSkipped thread=%@ useV3Mode=%d colorRB=%u presentationFBO=%u gestureId=%ld",
              NSThread.isMainThread ? @"main" : @"render",
              self.useV3Mode,
              _colorRenderbuffer,
              _presentationFBOName,
              (long)_gestureNumber);
        return;
    }
    glBindRenderbuffer(GL_RENDERBUFFER, _colorRenderbuffer);
    [_context renderbufferStorage:GL_RENDERBUFFER fromDrawable:(id<EAGLDrawable>)_view.layer];
    CGSize drawableSize = self.drawableSize;
    GLsizei width = (GLsizei)drawableSize.width;
    GLsizei height = (GLsizei)drawableSize.height;

    glBindFramebuffer(GL_FRAMEBUFFER, _presentationFBOName);
    glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, _colorRenderbuffer);

    _multisampleSamples = 0;
    if (self.useV3Mode) {
        if (_multisampleColorRenderbuffer) glDeleteRenderbuffers(1, &_multisampleColorRenderbuffer);
        if (_multisampleDepthRenderbuffer) glDeleteRenderbuffers(1, &_multisampleDepthRenderbuffer);
        if (_multisampleFBOName) glDeleteFramebuffers(1, &_multisampleFBOName);
        _multisampleColorRenderbuffer = 0;
        _multisampleDepthRenderbuffer = 0;
        _multisampleFBOName = 0;

        GLint maximumSamples = 0;
        glGetIntegerv(GL_MAX_SAMPLES_APPLE, &maximumSamples);
        const GLint candidates[] = {4, 2};
        for (NSUInteger candidateIndex = 0; candidateIndex < 2; candidateIndex++) {
            GLint samples = candidates[candidateIndex];
            if (samples > maximumSamples) continue;
            glGenFramebuffers(1, &_multisampleFBOName);
            glBindFramebuffer(GL_FRAMEBUFFER, _multisampleFBOName);
            glGenRenderbuffers(1, &_multisampleColorRenderbuffer);
            glBindRenderbuffer(GL_RENDERBUFFER, _multisampleColorRenderbuffer);
            glRenderbufferStorageMultisampleAPPLE(GL_RENDERBUFFER, samples, GL_RGBA8_OES, width, height);
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, _multisampleColorRenderbuffer);
            glGenRenderbuffers(1, &_multisampleDepthRenderbuffer);
            glBindRenderbuffer(GL_RENDERBUFFER, _multisampleDepthRenderbuffer);
            glRenderbufferStorageMultisampleAPPLE(GL_RENDERBUFFER, samples, GL_DEPTH_COMPONENT24_OES, width, height);
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, _multisampleDepthRenderbuffer);
            if (glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE) {
                _multisampleSamples = samples;
                break;
            }
            glDeleteRenderbuffers(1, &_multisampleColorRenderbuffer);
            glDeleteRenderbuffers(1, &_multisampleDepthRenderbuffer);
            glDeleteFramebuffers(1, &_multisampleFBOName);
            _multisampleColorRenderbuffer = 0;
            _multisampleDepthRenderbuffer = 0;
            _multisampleFBOName = 0;
        }
    }

    if (_multisampleSamples > 0) {
        _defaultFBOName = _multisampleFBOName;
    } else {
        glBindFramebuffer(GL_FRAMEBUFFER, _presentationFBOName);
        glBindRenderbuffer(GL_RENDERBUFFER, _depthRenderbuffer);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24_OES, width, height);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, _depthRenderbuffer);
        _defaultFBOName = _presentationFBOName;
    }
    glBindFramebuffer(GL_FRAMEBUFFER, _defaultFBOName);
    NSLog(@"[V3Renderer] drawable=%dx%d msaa=%dx", width, height, _multisampleSamples);
    if (self.useV3Mode && [_openGLRenderer respondsToSelector:@selector(setDefaultFBOName:)]) {
        [(AAPLOpenGLRendererV3 *)_openGLRenderer setDefaultFBOName:_defaultFBOName];
    }
    [_openGLRenderer resize:self.drawableSize];
    NSLog(@"[V3OpenTrace] event=resizeDrawableEnd thread=%@ useV3Mode=%d durationMs=%.3f drawable=%dx%d msaa=%d defaultFBO=%u gestureId=%ld",
          NSThread.isMainThread ? @"main" : @"render",
          self.useV3Mode,
          (CACurrentMediaTime() - resizeStartedAt) * 1000.0,
          width,
          height,
          _multisampleSamples,
          _defaultFBOName,
          (long)_gestureNumber);
}

- (void)draw:(id)sender {
    if (_stop || _openGLRenderer == nil) return;
    if (!self.useV3Mode) {
        [EAGLContext setCurrentContext:_context];
        [_openGLRenderer draw];
        glBindRenderbuffer(GL_RENDERBUFFER, _colorRenderbuffer);
        [_context presentRenderbuffer:GL_RENDERBUFFER];
        return;
    }
    if (_v3FramePending) return;
    _v3FramePending = YES;
    [self performV3RenderAsync:^{
        @autoreleasepool {
            [EAGLContext setCurrentContext:self->_context];
            CFTimeInterval frameStart = CACurrentMediaTime();
            os_signpost_id_t frameSignpost = os_signpost_id_generate(V3FrameLog());
            os_signpost_interval_begin(V3FrameLog(), frameSignpost, "V3Frame");
            [(AAPLOpenGLRendererV3 *)self->_openGLRenderer draw];
            if (self->_multisampleSamples > 0) {
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER_APPLE, self->_presentationFBOName);
                glBindFramebuffer(GL_READ_FRAMEBUFFER_APPLE, self->_multisampleFBOName);
                glResolveMultisampleFramebufferAPPLE();
                const GLenum attachments[] = {GL_COLOR_ATTACHMENT0, GL_DEPTH_ATTACHMENT};
                glDiscardFramebufferEXT(GL_READ_FRAMEBUFFER_APPLE, 2, attachments);
            }
            glBindRenderbuffer(GL_RENDERBUFFER, self->_colorRenderbuffer);
            [self->_context presentRenderbuffer:GL_RENDERBUFFER];
            BOOL animating = [(AAPLOpenGLRendererV3 *)self->_openGLRenderer isAnimating];
            double frameMs = (CACurrentMediaTime() - frameStart) * 1000.0;
            os_signpost_interval_end(V3FrameLog(), frameSignpost, "V3Frame", "milliseconds=%{public}.3f", frameMs);
            [[V3ModelResourceCache sharedCache] recordFrameDurationMilliseconds:frameMs];
            if (frameMs > 16.67) {
                NSLog(@"[V3OpenTrace] event=slowFrame thread=render frameMs=%.3f gestureId=%ld", frameMs, (long)self->_gestureNumber);
                NSLog(@"[V3Metrics] slowFrameMs=%.3f", frameMs);
            }
            dispatch_async(dispatch_get_main_queue(), ^{
                self->_v3FramePending = NO;
                if (!self->_v3FirstFrameReady) {
                    self->_v3FirstFrameReady = YES;
                    NSLog(@"[V3OpenTrace] event=firstFrameReady thread=main frameMs=%.3f gestureId=%ld",
                          frameMs,
                          (long)self->_gestureNumber);
#if DEBUG
                    self.view.accessibilityIdentifier = V3FirstFrameReadyAccessibilityIdentifier;
#endif
                    [[V3ModelResourceCache sharedCache] recordFirstPresentedFrame];
                    [self attemptInitialV3Open];
                }
                BOOL shouldPause = !(animating || self->_v3TouchActive);
                BOOL wasPaused = self->_displayLink.paused;
                self->_displayLink.paused = shouldPause;
                if (wasPaused != shouldPause) {
                    NSLog(@"[V3OpenTrace] event=displayLinkPausedChanged thread=main wasPaused=%d nowPaused=%d animating=%d touchActive=%d gestureId=%ld",
                          wasPaused,
                          shouldPause,
                          animating,
                          self->_v3TouchActive,
                          (long)self->_gestureNumber);
                }
            });
        }
    }];
}

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    NSLog(@"Дебаг касания touchesBegan");
    UITouch *touch = [touches anyObject];
    CGPoint newCoords = [touch locationInView:self.view];
    if (self.useV3Mode) {
        _v3TouchActive = YES;
        [self performV3RenderSync:^{
            [EAGLContext setCurrentContext:self->_context];
            [self->_openGLRenderer touchIvent:newCoords.x :newCoords.y :0 :0];
            [self->_openGLRenderer beginTouchIvent];
        }];
        [self requestV3Frame];
    } else {
        [_openGLRenderer touchIvent:newCoords.x :newCoords.y :0 :0];
        [_openGLRenderer beginTouchIvent];
    }
    _previousX = newCoords.x;
    _previousY = newCoords.y;
}

- (void)touchesMoved:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    NSLog(@"Дебаг касания touchesMoved");
    UITouch *touch = [touches anyObject];
    CGPoint newCoords = [touch locationInView:self.view];
    float deltaX = (newCoords.x - _previousX) / 3.0f;
    float deltaY = (newCoords.y - _previousY) / 3.0f;
    
    if (self.useV3Mode) {
        [self performV3RenderAsync:^{
            [EAGLContext setCurrentContext:self->_context];
            [self->_openGLRenderer touchIvent:newCoords.x :newCoords.y :deltaX :deltaY];
        }];
        [self requestV3Frame];
    } else {
        [_openGLRenderer touchIvent:newCoords.x :newCoords.y :deltaX :deltaY];
    }
    
    _previousX = newCoords.x;
    _previousY = newCoords.y;
}

- (void)touchesEnded:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    NSLog(@"Дебаг касания touchesEnded");
    if (self.useV3Mode) {
        _v3TouchActive = NO;
        [self performV3RenderAsync:^{
            [EAGLContext setCurrentContext:self->_context];
            [self->_openGLRenderer endTouchIvent];
        }];
        [self requestV3Frame];
    } else {
        [_openGLRenderer endTouchIvent];
    }
}

- (void)touchesCancelled:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [self touchesEnded:touches withEvent:event];
}

- (void)sendDataToFest :(uint8_t*) dataForWrite :(NSString*) characteristic  :(NSInteger) lenght {
    NSData *nsdataObj = [NSData dataWithBytes:dataForWrite length:lenght];
//    if (_typeMultigribNewVM) {
//        [gestureService sendDataToFestWithDataForWrite:nsdataObj characteristic:characteristic typeFestX:true];
//    } else{
//        [gestureService sendDataToFestWithDataForWrite:nsdataObj characteristic:characteristic typeFestX:false];
//        
//    }
}

- (void)setNumberGesture:(NSInteger)number {
    _gestureNumber = (int)number;
    NSLog(@"gestureNumber=%ld", (long)number);
}

- (void)handleGestureSettingsUpdate:(NSNotification *)notification {
    SharedParameterRef *parameterRef = notification.userInfo[@"data"];
    if (parameterRef == nil) {
        return;
    }
    NSString *parameterData = notification.userInfo[@"parameterData"];
    [self applyGestureSettingsUpdate:parameterRef
                       parameterData:parameterData];
}

- (void)applyGestureSettingsUpdate:(SharedParameterRef *)parameterRef
                     parameterData:(NSString *)parameterData {
    NSString *resolvedParameterData = parameterData;
    if (resolvedParameterData == nil) {
        resolvedParameterData = [gestureService getParameterDataWithDeviceAddress: parameterRef.addressDevice
                                                                       parameterID: parameterRef.parameterID];
    }
    if (resolvedParameterData == nil) {
        return;
    }
    SharedGesture *gestureSettings = self.useV3GestureProtocol
        ? [gestureService decodeGestureSettingsV3WithRaw:resolvedParameterData]
        : [gestureService decodeGestureSettingsWithRaw:resolvedParameterData];
    if (gestureSettings == nil) {
        NSLog(@"[UI-TEST][GestureSettings] decode returned nil useV3=%d data=%@", self.useV3GestureProtocol, resolvedParameterData);
        return;
    }
    if (_gestureNumber > 0 && gestureSettings.gestureId != _gestureNumber) {
        NSLog(@"[V3OpenTrace] event=gestureSettingsIgnored reason=gestureMismatch expected=%ld actual=%d useV3Protocol=%d",
              (long)_gestureNumber,
              gestureSettings.gestureId,
              self.useV3GestureProtocol);
        return;
    }
    NSLog(@"[UI-TEST][GestureSettings] decoded gestureId=%d useV3=%d", gestureSettings.gestureId, self.useV3GestureProtocol);
    NSLog(@"[V3OpenTrace] event=gestureSettingsDecoded thread=main decodedGestureId=%d useV3Mode=%d controllerGestureId=%ld",
          gestureSettings.gestureId,
          self.useV3Mode,
          (long)_gestureNumber);
    [self updateGestureSettingsAccessibilityWithGesture:gestureSettings];
    NSLog(@"GestureSettings update (VC) requestGestureSettings gestureId=%ld", (long)gestureSettings.gestureId);
    if (self.useV3Mode) {
        [self performV3RenderAsync:^{
            [self->_openGLRenderer updateGestureSettings:parameterRef
                                           parameterData:resolvedParameterData];
            dispatch_async(dispatch_get_main_queue(), ^{
                self->_v3GestureSettingsReady = YES;
                NSLog(@"[V3OpenTrace] event=gestureSettingsReady thread=main gestureId=%ld firstFrameReady=%d",
                      (long)self->_gestureNumber,
                      self->_v3FirstFrameReady);
                [self attemptInitialV3Open];
                [self requestV3Frame];
            });
        }];
    } else {
        [_openGLRenderer updateGestureSettings:parameterRef
                                 parameterData:resolvedParameterData];
    }
}

- (void)dealloc {
    [self unregisterGestureSettingsObserver];
    [[NSNotificationCenter defaultCenter] removeObserver:self
                                                    name:@"V3HandSideDidChange"
                                                  object:nil];
    if (self.useV3Mode) {
        [self destroyV3Resources];
    } else {
        [_displayLink invalidate];
    }
}
@end
