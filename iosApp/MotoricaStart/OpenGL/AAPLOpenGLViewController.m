/*
See LICENSE folder for this sample’s licensing information.

Abstract:
Implementation of the cross-platform view controller and cross-platform view that displays OpenGL content.
*/
#import "AAPLOpenGLViewController.h"
#import "AAPLOpenGLRenderer.h"
#import "AAPLOpenGLRendererV3.h"
#import "MotoricaStart-Swift.h"

#import <UIKit/UIKit.h>
#define PlatformGLContext EAGLContext


@implementation AAPLOpenGLView

+ (Class) layerClass
{
    return [CAEAGLLayer class];
}

@end

@implementation AAPLOpenGLViewController
{
    AAPLOpenGLView *_view;
    id _openGLRenderer;
    GestureService *gestureService;
    PlatformGLContext *_context;
    GLuint _defaultFBOName;
    
    GLuint _colorRenderbuffer;
    GLuint _depthRenderbuffer;
    CADisplayLink *_displayLink;
    __weak IBOutlet UIButton *saveBtn;
    UIView *segmentContainer;
    UIView *stateSegmentBackgroundView;
    UIView *stateSegmentContentView;
    UIView *stateSegmentHighlightView;
    UIButton *stateOpenButton;
    UIButton *stateCloseButton;
    NSLayoutConstraint *stateHighlightLeadingConstraint;
    NSInteger selectedStateSegmentIndex;
    __weak IBOutlet UIButton *fingersDelayBtn;
    __weak IBOutlet UITextField *textField;
    __weak IBOutlet UILabel *deviceName;
    __weak IBOutlet UIImageView *statusConnection;
    __weak IBOutlet UIButton *renameBtn;
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
}


static NSString *const GestureSettingsViewModelDidUpdateNotification = @"GestureSettingsViewModelDidUpdate";
static NSString *const GestureSettingsViewModelDidUpdateV3Notification = @"GestureSettingsV3ViewModelDidUpdate";
static NSString *const GestureSettingsScreenAccessibilityIdentifier = @"AccessibilityIdentifierGestureSettingsScreen";
static NSString *const GestureSettingsUITestExposeStateFlag = @"-ui-test-expose-gesture-settings-state";
static NSString *const GestureSettingsUITestInjectGesture70Flag = @"-ui-test-inject-v3-gesture-70";
static NSString *const GestureSettingsUITestGesture70Payload = @"46006164000000646464646400000000000000000000000000";

- (void)registerGestureSettingsObserverIfNeeded {
    if (_gestureSettingsObserverRegistered) {
        return;
    }
    NSString *notificationName = self.useV3Mode
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
    self.view.accessibilityValue = summary;
}

- (void)injectGestureSettingsV3ForUITestIfNeeded {
    if (_didInjectGestureSettingsV3ForUITest ||
        !self.useV3Mode ||
        _gestureNumber != 70 ||
        ![self hasLaunchArgument:GestureSettingsUITestInjectGesture70Flag]) {
        return;
    }

    _didInjectGestureSettingsV3ForUITest = YES;
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(0.25 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
        SharedParameterRef *parameterRef = [[SharedParameterRef alloc] initWithAddressDevice:0 parameterID:0 dataCode:0];
        [self applyGestureSettingsUpdate:parameterRef parameterData:GestureSettingsUITestGesture70Payload];
    });
}

- (void)viewWillDisappear:(BOOL)animated {
    [super viewWillDisappear:animated];
    NSLog(@"viewWillDisappear");
    [self unregisterGestureSettingsObserver];
}

- (void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
    [self injectGestureSettingsV3ForUITestIfNeeded];
}

- (void)viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];
    [self registerGestureSettingsObserverIfNeeded];
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
    [super viewDidLoad];
    NSLog(@"Отсюда мы начинаем исполнение программы");
    gestureService = [[GestureService alloc] init];
    [self registerGestureSettingsObserverIfNeeded];
    UIImage *connectStatus = [UIImage imageNamed: @"connect_status.png"];
    UIImage *disconnectStatus = [UIImage imageNamed: @"disconnect_status.png"];
    [gestureService getDeviceName];
    state = 0;
    [self setupStateSegmentedControl];

    
    
    NSInteger selectedGestureNumber = self.gestureNumber;
    if (selectedGestureNumber == 0) { selectedGestureNumber = 64; }
    _gestureNumber = selectedGestureNumber;
    deviceName.text = [gestureService getGestureNameWithNumberGesture: _gestureNumber];
    
    showRenameTextField = false;

    _stop = false;
    _previousX = 0.0f;
    _previousY = 0.0f;
    _didInjectGestureSettingsV3ForUITest = NO;
    self.view.accessibilityIdentifier = GestureSettingsScreenAccessibilityIdentifier;
    
    _view = (AAPLOpenGLView *)self.view;
    
    [self prepareView];

    [self makeCurrentContext];

    if (self.useV3Mode) {
        _openGLRenderer = [[AAPLOpenGLRendererV3 alloc] initWithDefaultFBOName:_defaultFBOName
                                                                  gestureNumber:_gestureNumber];
    } else {
        _openGLRenderer = [[AAPLOpenGLRenderer alloc] initWithDefaultFBOName:_defaultFBOName
                                                               gestureNumber:_gestureNumber];
    }

    if(!_openGLRenderer) {
        NSLog(@"OpenGL renderer failed initialization.");
        return;
    }

    [_openGLRenderer resize:self.drawableSize];
    
    // Расчёт коэффициентов для верного пересчёта координат пальца на экране в координаты эекрана OpenGL
    CGRect screenRect = [[UIScreen mainScreen] bounds];
    CGFloat screenWidth = screenRect.size.width;
    CGFloat screenHeight = screenRect.size.height;
    NSLog(@"Размер экрана   screenWidth: %f   screenHeight: %f", screenWidth, screenHeight);
    [_openGLRenderer calculationOfCoefficients:screenWidth :screenHeight];
    if (self.useV3Mode && _gestureNumber > 0) {
        [gestureService requestGestureSettingsV3WithGestureId:(int)_gestureNumber];
    }
}

- (void)viewDidLayoutSubviews {
    [super viewDidLayoutSubviews];
    [self selectStateSegmentIndex:selectedStateSegmentIndex animated:NO notifyRenderer:NO];
}

- (IBAction)unwindToOpenGLVC:(UIStoryboardSegue *)segue {}

- (IBAction)perehod:(UIButton *)sender {
    _stop = true;
    [_openGLRenderer stopVC];
    
    if (showRenameTextField) {
        NSString *result = @"";
        result = [result stringByAppendingString:textField.text];
        [gestureService setNameGestureWithNumberGesture: _gestureNumber name:result];
    }
}
- (IBAction)perehodWithSaveData:(UIButton *)sender {
    _stop = true;
    [_openGLRenderer stopVCWithSaveData];
}

- (void)stateSegmentChanged:(UISegmentedControl *)sender {
    [self selectStateSegmentIndex:sender.selectedSegmentIndex animated:YES notifyRenderer:YES];
}

- (void)stateSegmentButtonTapped:(UIButton *)sender {
    [self selectStateSegmentIndex:sender.tag animated:YES notifyRenderer:YES];
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
        [_openGLRenderer changeState:state];
    }
}

- (IBAction)openFingersDelayDialog:(UIButton *)sender {
    [_openGLRenderer openFingersDelayDialog];
    
    
    if ([_openGLRenderer currentGestureState]) {
        // закрытое состояние
        NSArray<NSNumber *> *delayValues = [_openGLRenderer currentOpenToCloseShifts];
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
            [strongSelf->_openGLRenderer applyOpenToCloseShifts:updatedValues];
        }];
    } else {
        // открытое состояние
        NSArray<NSNumber *> *delayValues = [_openGLRenderer currentCloseToOpenShifts];
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
            [strongSelf->_openGLRenderer applyCloseToOpenShifts:updatedValues];
        }];
    }
}

- (IBAction)renameGesture:(UIButton *)sender {
    if (showRenameTextField) {
        textField.hidden = YES;
        [textField resignFirstResponder];
        showRenameTextField = false;
        deviceName.text = textField.text;
        NSString *result = @"";
        result = [result stringByAppendingString:textField.text];
        [gestureService setNameGestureWithNumberGesture: _gestureNumber name:result];
        [renameBtn setImage:[UIImage imageNamed:@"rename.png"]   forState:UIControlStateNormal];
    } else {
        textField.hidden = NO;
        [textField becomeFirstResponder];
        showRenameTextField = true;
        textField.text = deviceName.text;
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
    NSLog(@"1 - Подготавливаем вью");
    CAEAGLLayer *eaglLayer = (CAEAGLLayer *)self.view.layer;

    eaglLayer.drawableProperties = @{kEAGLDrawablePropertyRetainedBacking : @NO,
                                     kEAGLDrawablePropertyColorFormat     : kEAGLColorFormatSRGBA8 };
    eaglLayer.opaque = YES;
    

    _context = [[EAGLContext alloc] initWithAPI:kEAGLRenderingAPIOpenGLES2];

    if (!_context || ![EAGLContext setCurrentContext:_context])
    {
        NSLog(@"Could not create an OpenGL ES context.");
        return;
    }

    [self makeCurrentContext];

    self.view.contentScaleFactor = [UIScreen mainScreen].nativeScale;

    // In iOS & tvOS, you must create an FBO and attach a drawable texture allocated by
    // Core Animation to use as the default FBO for a view.
    glGenFramebuffers(1, &_defaultFBOName);
    glBindFramebuffer(GL_FRAMEBUFFER, _defaultFBOName);

    glGenRenderbuffers(1, &_colorRenderbuffer);

    glGenRenderbuffers(1, &_depthRenderbuffer);

    [self resizeDrawable];

    glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, _colorRenderbuffer);
    glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, _depthRenderbuffer);

    // Create the display link so you render at 60 frames per second (FPS).
    _displayLink = [CADisplayLink displayLinkWithTarget:self selector:@selector(draw:)];

    _displayLink.preferredFramesPerSecond = 60;

    // Set the display link to run on the default run loop (and the main thread).
    [_displayLink addToRunLoop:[NSRunLoop currentRunLoop] forMode:NSDefaultRunLoopMode];
    
    if ([gestureService getFingersDelaySwitch]) {
        [fingersDelayBtn setAlpha:1];
    } else { [fingersDelayBtn setAlpha:0]; }
    
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
    [self makeCurrentContext];

    // First, ensure that you have a render buffer.
    assert(_colorRenderbuffer != 0);

    glBindRenderbuffer(GL_RENDERBUFFER, _colorRenderbuffer);
    [_context renderbufferStorage:GL_RENDERBUFFER fromDrawable:(id<EAGLDrawable>)_view.layer];

    CGSize drawableSize = self.drawableSize;

    glBindRenderbuffer(GL_RENDERBUFFER, _depthRenderbuffer);

    glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, drawableSize.width, drawableSize.height);

    GetGLError();

    [_openGLRenderer resize:self.drawableSize];
}

- (void)draw:(id)sender {
    if (!_stop) {
        [EAGLContext setCurrentContext:_context];
            [_openGLRenderer draw];

            glBindRenderbuffer(GL_RENDERBUFFER, _colorRenderbuffer);
            [_context presentRenderbuffer:GL_RENDERBUFFER];
    }
}

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    NSLog(@"Дебаг касания touchesBegan");
    UITouch *touch = [touches anyObject];
    CGPoint newCoords = [touch locationInView:self.view];
    [_openGLRenderer touchIvent:newCoords.x :newCoords.y :0 :0];
    [_openGLRenderer beginTouchIvent];
    _previousX = newCoords.x;
    _previousY = newCoords.y;
}

- (void)touchesMoved:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    NSLog(@"Дебаг касания touchesMoved");
    UITouch *touch = [touches anyObject];
    CGPoint newCoords = [touch locationInView:self.view];
    float deltaX = (newCoords.x - _previousX) / 7.0f;
    float deltaY = (newCoords.y - _previousY) / 7.0f;
    
    [_openGLRenderer touchIvent:newCoords.x :newCoords.y :deltaX :deltaY];
    
    _previousX = newCoords.x;
    _previousY = newCoords.y;
}

- (void)touchesEnded:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    NSLog(@"Дебаг касания touchesEnded");
    [_openGLRenderer endTouchIvent];
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
    SharedGesture *gestureSettings = self.useV3Mode
        ? [gestureService decodeGestureSettingsV3WithRaw:resolvedParameterData]
        : [gestureService decodeGestureSettingsWithRaw:resolvedParameterData];
    if (gestureSettings == nil) {
        return;
    }
    [self updateGestureSettingsAccessibilityWithGesture:gestureSettings];
    NSLog(@"GestureSettings update (VC) requestGestureSettings gestureId=%ld", (long)gestureSettings.gestureId);
    [_openGLRenderer updateGestureSettings: parameterRef
                             parameterData: resolvedParameterData];
}
@end
