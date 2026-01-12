/*
See LICENSE folder for this sample’s licensing information.

Abstract:
Implementation of the cross-platform view controller and cross-platform view that displays OpenGL content.
*/
#import "AAPLOpenGLViewController.h"
#import "AAPLOpenGLRenderer.h"
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
    AAPLOpenGLRenderer *_openGLRenderer;
    GestureService *gestureService;
    PlatformGLContext *_context;
    GLuint _defaultFBOName;
    
    GLuint _colorRenderbuffer;
    GLuint _depthRenderbuffer;
    CADisplayLink *_displayLink;
    __weak IBOutlet UIButton *state_btn;
    __weak IBOutlet UIButton *fingers_delay_btn;
    __weak IBOutlet UITextField *text_field;
    __weak IBOutlet UILabel *deviceName;
    __weak IBOutlet UIImageView *statusConnection;
    __weak IBOutlet UIButton *renameBtn;
    UIImage *connectStatus;
    UIImage *disconnectStatus;
    
    NSInteger _typeMultigribNewVM;
    NSInteger _gestureNumber;
    NSInteger _gestureTable[84];
    NSString *_gestureTableStr;
    float _previousX;
    float _previousY;
    bool _stop;
    bool state;
    bool showRenameTextField;
    
    int openStage1;
    int openStage2;
    int openStage3;
    int openStage4;
    int openStage5;
    int openStage6;
    
    int closeStage1;
    int closeStage2;
    int closeStage3;
    int closeStage4;
    int closeStage5;
    int closeStage6;
}

- (void)viewWillDisappear:(BOOL)animated {
    [super viewWillDisappear:animated];
    NSLog(@"viewWillDisappear");
}


- (void)viewDidLoad
{
    [super viewDidLoad];
    NSLog(@"Отсюда мы начинаем исполнение программы");
//    gestureVC = [[WidgetsListTableViewController alloc]init];
    gestureService = [[GestureService alloc] init];
//    testVC = [[GestureListItemViewModel alloc]init];
    UIImage *connectStatus = [UIImage imageNamed: @"connect_status.png"];
    UIImage *disconnectStatus = [UIImage imageNamed: @"disconnect_status.png"];
//    [gestureVC savingDeviceName];
    [gestureService getDeviceName];
//    if ([gestureVC getStatusConnection] == 1) {
//        statusConnection.image = connectStatus;
//    } else {
//        statusConnection.image = disconnectStatus;
//    }
    [self stylizationStateBtn];
    openStage1 = 0;
    openStage2 = 0;
    openStage3 = 0;
    openStage4 = 0;
    openStage5 = 0;
    openStage6 = 0;
    closeStage1 = 0;
    closeStage2 = 0;
    closeStage3 = 0;
    closeStage4 = 0;
    closeStage5 = 0;
    closeStage6 = 0;
    
//    _gestureNumber = [gestureVC getGestureNum];
//    _gestureNumber = [GestureListItemViewModel getGestureNum];
//    _gestureNumber = [testVC getGestureNum];
    _gestureNumber = [gestureService getGestureNum];
    if (_gestureNumber == 0) {
        NSLog(@"Вызвана функция _gestureNumber == 0");
        deviceName.text = [gestureService getGestureNameWithNumberGesture: _gestureNumber];
    } else {
        NSLog(@"Вызвана функция _gestureNumber = %d", _gestureNumber);
        NSLog(@"Вызвана функция getGestureNameWithNumberGesture _gestureNumber = %@", [gestureService getGestureNameWithNumberGesture: _gestureNumber]);
        deviceName.text = [gestureService getGestureNameWithNumberGesture: _gestureNumber];
    }
    _gestureTableStr = [gestureService getGestureTable];
    _typeMultigribNewVM = [gestureService getUseFestX];
    
    
    state = 0;
    showRenameTextField = false;

    _stop = false;
    _previousX = 0.0f;
    _previousY = 0.0f;
    
    _view = (AAPLOpenGLView *)self.view;
    
    [self prepareView];

    [self makeCurrentContext];

    _openGLRenderer = [[AAPLOpenGLRenderer alloc] initWithDefaultFBOName:_defaultFBOName];

    if(!_openGLRenderer)
    {
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
    
    [_openGLRenderer saveStateData: @"0"];
}



- (IBAction)unwindToOpenGLVC:(UIStoryboardSegue *)segue {
    
}

- (IBAction)perehod:(UIButton *)sender {
    _stop = true;
    NSLog(@"Переход назад 1");
    // возобновляем работу протеза от датчиков
    uint8_t data[]   = { 0x01 };
//    if (_typeMultigribNewVM) {
////        [self sendDataToFest:data :sampleGattAtributes.SENS_ENABLED_NEW_VM :sizeof(data)];
//    } else {
////        [self sendDataToFest:data :sampleGattAtributes.SENS_ENABLED_NEW :sizeof(data)];
//    }
    
    [_openGLRenderer stopVC];
    
    if (showRenameTextField) {
        NSString *result = @"";
        result = [result stringByAppendingString:text_field.text];
        [gestureService setNameGestureWithNumberGesture: _gestureNumber name:result];
    }
}

- (IBAction)chageState:(UIButton *)sender {
    if (state == 0 ) {
        state = 1;
        [state_btn setTitle:NSLocalizedString(@"close_state", nil) forState:UIControlStateNormal];
        [_openGLRenderer changeState:state];
    } else {
        state = 0;
        [state_btn setTitle:NSLocalizedString(@"open_state", nil) forState:UIControlStateNormal];
        [_openGLRenderer changeState:state];
    }
}

- (IBAction)openFingersDealyDialog:(UIButton *)sender {
    [_openGLRenderer savesAllData];
}

- (IBAction)renameGesture:(UIButton *)sender {
    if (showRenameTextField) {
        text_field.hidden = YES;
        [text_field resignFirstResponder];
        showRenameTextField = false;
        deviceName.text = text_field.text;
        NSString *result = @"";
        result = [result stringByAppendingString:text_field.text];
        [gestureService setNameGestureWithNumberGesture: _gestureNumber name:result];
        [renameBtn setImage:[UIImage imageNamed:@"rename.png"]   forState:UIControlStateNormal];
    } else {
        text_field.hidden = NO;
        [text_field becomeFirstResponder];
        showRenameTextField = true;
        text_field.text = deviceName.text;
        [renameBtn setImage:[UIImage imageNamed:@"ok.png"]   forState:UIControlStateNormal];
    }
}

- (void)stylizationStateBtn {
    state_btn.layer.cornerRadius = 21;
    state_btn.layer.borderWidth = 2;
    state_btn.layer.borderColor = UIColor.whiteColor.CGColor;
}

- (void)prepareView
{
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
    
    if ([gestureService getFingersDelaySwitch] && [gestureService getUseFestX]) {
        [fingers_delay_btn setAlpha:1];
    } else { [fingers_delay_btn setAlpha:0]; }
    
}

- (void)makeCurrentContext
{
    NSLog(@"2 - Создаём контекст этого вью");
    [EAGLContext setCurrentContext:_context];
}

- (CGSize)drawableSize
{
    GLint backingWidth, backingHeight;
    glBindRenderbuffer(GL_RENDERBUFFER, _colorRenderbuffer);
    glGetRenderbufferParameteriv(GL_RENDERBUFFER, GL_RENDERBUFFER_WIDTH, &backingWidth);
    glGetRenderbufferParameteriv(GL_RENDERBUFFER, GL_RENDERBUFFER_HEIGHT, &backingHeight);
    CGSize drawableSize = {backingWidth, backingHeight};
    NSLog(@"3 - Подгонка размера вью под размер экрана backingWidth: %d  backingHeight: %d", backingWidth, backingHeight);
    return drawableSize;
}

- (void)resizeDrawable
{
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

- (void)draw:(id)sender
{
    if (!_stop) {
        [EAGLContext setCurrentContext:_context];
            [_openGLRenderer draw];

            glBindRenderbuffer(GL_RENDERBUFFER, _colorRenderbuffer);
            [_context presentRenderbuffer:GL_RENDERBUFFER];
    }
}

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event
{
    UITouch *touch = [touches anyObject];
    CGPoint newCoords = [touch locationInView:self.view];
    [_openGLRenderer touchIvent:newCoords.x :newCoords.y :0 :0];
    [_openGLRenderer beginTouchIvent];
    _previousX = newCoords.x;
    _previousY = newCoords.y;
}

- (void)touchesMoved:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event
{
    UITouch *touch = [touches anyObject];
    CGPoint newCoords = [touch locationInView:self.view];
    float deltaX = (newCoords.x - _previousX) / 6.0f;
    float deltaY = (newCoords.y - _previousY) / 6.0f;
    
    [_openGLRenderer touchIvent:newCoords.x :newCoords.y :deltaX :deltaY];
    
    _previousX = newCoords.x;
    _previousY = newCoords.y;
}

- (void)touchesEnded:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [_openGLRenderer endTouchIvent];
}

- (void)sendDataToFest :(uint8_t*) dataForWrite :(NSString*) characteristic  :(NSInteger) lenght {
    NSData *nsdataObj = [NSData dataWithBytes:dataForWrite length:lenght];
    if (_typeMultigribNewVM) {
        [gestureService sendDataToFestWithDataForWrite:nsdataObj characteristic:characteristic typeFestX:true];
    } else{
        [gestureService sendDataToFestWithDataForWrite:nsdataObj characteristic:characteristic typeFestX:false];
        
    }
}

- (void)setNumberGesture:(NSInteger)number {
    _gestureNumber = (int)number;
    NSLog(@"gestureNumber=%ld", (long)number);
}

@end
