/*
See LICENSE folder for this sample’s licensing information.

Abstract:
Implementation of the renderer class that performs OpenGL state setup and per-frame rendering.
*/

#import "AAPLOpenGLRenderer.h"
#import "AAPLMathUtilities.h"
#import "AAPLMeshData.h"
#import "AAPLCommonDefinitions.h"
#import <Foundation/Foundation.h>
#import <simd/simd.h>
#import "MotoricaStart-Swift.h"
//#import "shared.h"

@implementation AAPLOpenGLRenderer
{
    GLuint _defaultFBOName;
    CGSize _viewSize;

    GLint _mvpUniformIndex;
    GLint _uniformBufferIndex;

    matrix_float4x4 _projectionMatrix;
    // Open GL Objects you use to render the temple mesh.
    GLuint _templeVAO;
    GLuint _templeVertexPositions;
    GLuint _templeVertexGenerics;
    GLuint _templeProgram;
    GLuint _selectProgram;
    GLuint _templeMVPUniformLocation;
    matrix_float4x4 _templeCameraMVPMatrix;

    // Arrays of submesh index buffers and textures for temple mesh.
    NSUInteger _numTempleSubmeshes;
    GLuint *_templeIndexBufferCounts;
    GLuint *_templeIndexBuffers;
    GLuint *_templeTextures;

    GLuint _codeSelectUniformLocation;
    GLuint _templeNormalMatrixUniformLocation;
    GLuint _ambientLightColorUniformLocation;
    GLuint _directionalLightInvDirectionUniformLocation;
    GLuint _directionalLightColorUniformLocation;

    matrix_float3x3 _templeNormalMatrix;
    vector_float3 _ambientLightColor;
    vector_float3 _directionalLightInvDirection;
    vector_float3 _directionalLightColor;
    
    matrix_float4x4 _modelMatrix;
    matrix_float4x4 _accumulateRotationGeneral;
    matrix_float4x4 _accumulateRotationForeFinger;
    matrix_float4x4 _accumulateRotationForeFinger2;
    matrix_float4x4 _accumulateRotationMiddleFinger;
    matrix_float4x4 _accumulateRotationMiddleFinger2;
    matrix_float4x4 _accumulateRotationRingFinger;
    matrix_float4x4 _accumulateRotationRingFinger2;
    matrix_float4x4 _accumulateRotationLittleFinger;
    matrix_float4x4 _accumulateRotationLittleFinger2;
    matrix_float4x4 _accumulateRotationBigFinger;
    matrix_float4x4 _accumulateRotationBigFinger2;
    

    float _rotation_x;
    float _rotation_y;
    float _rotation_z;
    
    float _X;
    float _Y;
    float _deltaX;
    float _deltaY;
    float _startAngle;
    float _startAngleZ;
    
    float _angleForeFingerFloat;
    float _angleMiddleFingerFloat;
    float _angleRingFingerFloat;
    float _angleLittleFingerFloat;
    float _angleBigFingerFloat;
    float _angle_2_BigFingerFloat;


    int _angleForeFingerTransfer;
    int _angleMiddleFingerTransfer;
    int _angleRingFingerTransfer;
    int _angleLittleFingerTransfer;
    int _angleBigFingerTransfer1;
    int _angleBigFingerTransfer2;
    int _angleForeFingerTransferOld;
    int _angleMiddleFingerTransferOld;
    int _angleRingFingerTransferOld;
    int _angleLittleFingerTransferOld;
    int _angleBigFingerTransfer1Old;
    int _angleBigFingerTransfer2Old;
    
    bool _directionForeFinger;
    
    
    float _xCoefficient;
    float _yCoefficient;
    
    int _selectedObject;
    float angle;
    bool stateGesture;
    
    GLuint i;

    // Open GL Objects to select meshes
    GLuint _selectionMVPUniformLocation;
    
    GestureService *gestureService;
    NSString *_gestureTableStr;
    NSInteger _fingersDelayTable[12];
    NSInteger _gestureTable[87];
//    NSString *_gestureTableBigStr;
//    NSInteger _gestureTableBig[160];
    NSInteger _gestureNumber;
    NSInteger _gestureSettingsParameterID;
    NSString *_gestureSettingsParameterData;
    NSInteger _handSide;
    NSInteger _gestureSettingsData;
    
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
    
    NSTimer *timer;
}

- (instancetype) initWithDefaultFBOName:(GLuint)defaultFBOName
                        gestureNumber:(NSInteger)gestureNumber {
    self = [super init];
    if(self)
    {
        _gestureNumber = gestureNumber;
        NSLog(@"gestureNumber from AAPLOpenGLRenderer = %ld", (long)_gestureNumber);
//        _handSide = [gestureService getHandSide];
        gestureService = [[GestureService alloc] init];
        _handSide = [gestureService getHandSide];
        
        if (_handSide == 1) {
            NSLog(@"Сторона руки правая");
        } else {
            NSLog(@"Сторона руки левая");
        }
        
        _gestureTableStr = [gestureService getGestureTable];
        NSArray *_gestureTableSecond = [_gestureTableStr componentsSeparatedByString:@" "];
        if (_gestureTableSecond.count > 84) {
            for  (int i = 0; i <= 86; i++) {
                _gestureTable[i] = [[_gestureTableSecond objectAtIndex:i] intValue];
                if (i%6 == 0 ) {NSLog(@"------------------------------------");}
                if (i%12 == 0 ) {NSLog(@"%d  ====================================", (i/12));}
                NSLog(@"Распаршенные данные по жестам №%d %d", i, [[_gestureTableSecond objectAtIndex:i] intValue]);
            }
        } else {
            for  (int i = 0; i <= 86; i++) { _gestureTable[i] = 0; }
        }
        
        
//        _gestureTableBigStr = [gestureService getGestureTableBig];
//        NSArray *_gestureTableSecondBig = [_gestureTableBigStr componentsSeparatedByString:@" "];
//        NSLog(@"Парсинг 00 _gestureTableSecondBig.count: %lu", (unsigned long)_gestureTableSecondBig.count);
//        if (_gestureTableSecondBig.count > 157) {
//            for  (int i = 0; i <= 159; i++) {
//                _gestureTableBig[i] = [[_gestureTableSecondBig objectAtIndex:i] intValue];
//            }
//        } else {
//            for  (int i = 0; i <= 159; i++) { _gestureTableBig[i] = 0; }
//        }
        
        
        NSLog(@"Парсинг 1 _gestureNumber: %ld", (long)_gestureNumber);
//        openStage4 = _gestureTableBig[12*(_gestureNumber-2)+0];
//        openStage3 = _gestureTableBig[12*(_gestureNumber-2)+1];
//        openStage2 = _gestureTableBig[12*(_gestureNumber-2)+2];
//        openStage1 = _gestureTableBig[12*(_gestureNumber-2)+3];
//        openStage5 = _gestureTableBig[12*(_gestureNumber-2)+4];
//        openStage6 = _gestureTableBig[12*(_gestureNumber-2)+5];
//        
//        closeStage4 = _gestureTableBig[12*(_gestureNumber-2)+6];
//        closeStage3 = _gestureTableBig[12*(_gestureNumber-2)+7];
//        closeStage2 = _gestureTableBig[12*(_gestureNumber-2)+8];
//        closeStage1 = _gestureTableBig[12*(_gestureNumber-2)+9];
//        closeStage5 = _gestureTableBig[12*(_gestureNumber-2)+10];
//        closeStage6 = _gestureTableBig[12*(_gestureNumber-2)+11];
        
        
        _accumulateRotationGeneral       = matrix4x4_identity();
        _accumulateRotationForeFinger    = matrix4x4_identity();
        _accumulateRotationForeFinger2   = matrix4x4_identity();
        _accumulateRotationMiddleFinger  = matrix4x4_identity();
        _accumulateRotationMiddleFinger2 = matrix4x4_identity();
        _accumulateRotationLittleFinger  = matrix4x4_identity();
        _accumulateRotationLittleFinger2 = matrix4x4_identity();
        _accumulateRotationBigFinger     = matrix4x4_identity();
        _accumulateRotationBigFinger2    = matrix4x4_identity();
        
        _angleForeFingerFloat = (openStage4*1.0f/100*97.9)/180*M_PI+0.0175;                    //  <0.0174 _angleForeFingerFloat   >1.727
        _angleMiddleFingerFloat = (openStage3*1.0f/100*97.9)/180*M_PI+0.0175;                  //  <0.0174 _angleMiddleFingerFloat >1.727
        _angleRingFingerFloat = (openStage2*1.0f/100*97.9)/180*M_PI+0.0175;                    //  <0.0174 _angleRingFingerFloat   >1.727
        _angleLittleFingerFloat = (openStage1*1.0f/100*97.9)/180*M_PI+0.0175;                  //  <0.0174 _angleLittleFingerFloat >1.727  1,7096  98-диапазон в градусах
        _angleBigFingerFloat = ((100-openStage5)*1.0f/100*87)/180*M_PI-1.028;                        //  <-1.029 _angleBigFingerFloat    >0.5059 1,5396  88-диапазон в градусах
        _angle_2_BigFingerFloat = (openStage6*1.0f/100*90)/180*M_PI;                           //  <0      _angle_2_BigFingerFloat >1.58   1,58    90-диапазон в градусах
        [self checkingAnglesForValidValues];
        
        _angleForeFingerTransferOld     = (int) (_angleForeFingerFloat/M_PI*180);
        _angleMiddleFingerTransferOld   = (int) (_angleMiddleFingerFloat/M_PI*180);
        _angleRingFingerTransferOld     = (int) (_angleRingFingerFloat/M_PI*180);
        _angleLittleFingerTransferOld   = (int) (_angleLittleFingerFloat/M_PI*180);
        _angleBigFingerTransfer1Old     = (int) (_angleBigFingerFloat/M_PI*180);
        _angleBigFingerTransfer2Old     = (int) (_angle_2_BigFingerFloat/M_PI*180);
        
        
//        if (_typeMultigribNewVM == 1 || [gestureService getVersionDriverGreaterThan237]) {
//            uint8_t data[]   = { openStage4,openStage3,openStage2,openStage1,openStage5,openStage6 };
////            [self sendDataToFest :data :sampleGattAtributes.MOVE_ALL_FINGERS_NEW_VM :sizeof(data)];
//            [self sendGestureNumberFestX];
//        } else {
//            uint8_t data[]   = { openStage1,openStage2,openStage3,openStage4,openStage5,openStage6 };
////            [self sendDataToFest :data :sampleGattAtributes.MOVE_ALL_FINGERS_NEW :sizeof(data)];
//        }
        
        
        _startAngle = 1.55f;//1.55f;
        _startAngleZ = 3.14f;
        _selectedObject = 0;
        
        // Build all of your objects and setup initial state here.
        _defaultFBOName = defaultFBOName;

        [self buildTempleObjects: 1 :@"Temple.obj"];
    }
    return self;
}

- (void) buildTempleObjects: (int)numberMesh :(NSString*)nameLoadMesh; {
    NSLog(@"Доставание данных из obj файла");
    // Load the mesh data from a file.
    NSError *error;

    NSURL *modelFileURL = [[NSBundle mainBundle] URLForResource:[NSString stringWithFormat:@"Meshes/%@", nameLoadMesh]
                                                  withExtension:nil];

    NSAssert(modelFileURL, @"Could not find model (%@) file in the bundle.", modelFileURL.absoluteString);

    // Load mesh data from a file into memory.
    // This only loads data from the bundle and does not create any OpenGL objects.

    AAPLMeshData *meshData = [[AAPLMeshData alloc] initWithURL:modelFileURL error:&error];

    NSAssert(meshData, @"Could not load mesh from model file (%@), error: %@.", modelFileURL.absoluteString, error);

    // Extract the vertex data, reconfigure the layout for the vertex shader, and place the data into
    // an OpenGL vertex buffer.
    {
        NSUInteger positionElementSize = sizeof(vector_float3);
        NSUInteger positionDataSize    = positionElementSize * meshData.vertexCount;

        NSUInteger genericElementSize = sizeof(AAPLVertexGenericData);
        NSUInteger genericsDataSize   = genericElementSize * meshData.vertexCount;

        vector_float3         *positionsArray = (vector_float3 *)malloc(positionDataSize);
        AAPLVertexGenericData *genericsArray = (AAPLVertexGenericData *)malloc(genericsDataSize);

        // Extract vertex data from the buffer and lay it out for OpenGL buffers.
        struct AAPLVertexData *vertexData = meshData.vertexData;

        for(unsigned long vertex = 0; vertex < meshData.vertexCount; vertex++)
        {
            positionsArray[vertex] = vertexData[vertex].position;
            genericsArray[vertex].texcoord = vertexData[vertex].texcoord;
            genericsArray[vertex].normal.x = vertexData[vertex].normal.x;
            genericsArray[vertex].normal.y = vertexData[vertex].normal.y;
            genericsArray[vertex].normal.z = vertexData[vertex].normal.z;
        }

        // Place formatted vertex data into OpenGL buffers.
        glGenBuffers(1, &_templeVertexPositions);

        glBindBuffer(GL_ARRAY_BUFFER, _templeVertexPositions);

        glBufferData(GL_ARRAY_BUFFER, positionDataSize, positionsArray, GL_STATIC_DRAW);

        glGenBuffers(1, &_templeVertexGenerics);

        glBindBuffer(GL_ARRAY_BUFFER, _templeVertexGenerics);

        glBufferData(GL_ARRAY_BUFFER, genericsDataSize, genericsArray, GL_STATIC_DRAW);

        glGenVertexArrays(1, &_templeVAO);

        glBindVertexArray(_templeVAO);

        // Setup buffer with positions.
        glBindBuffer(GL_ARRAY_BUFFER, _templeVertexPositions);
        glVertexAttribPointer(AAPLVertexAttributePosition, 3, GL_FLOAT, GL_FALSE, sizeof(vector_float3), BUFFER_OFFSET(0));
        glEnableVertexAttribArray(AAPLVertexAttributePosition);

        // Setup buffer with normals and texture coordinates.
        glBindBuffer(GL_ARRAY_BUFFER, _templeVertexGenerics);

        glVertexAttribPointer(AAPLVertexAttributeTexcoord, 2, GL_FLOAT, GL_FALSE, sizeof(AAPLVertexGenericData), BUFFER_OFFSET(0));
        glEnableVertexAttribArray(AAPLVertexAttributeTexcoord);

        glVertexAttribPointer(AAPLVertexAttributeNormal, 3, GL_FLOAT, GL_FALSE, sizeof(AAPLVertexGenericData), BUFFER_OFFSET(sizeof(vector_float2)));
        glEnableVertexAttribArray(AAPLVertexAttributeNormal);
    }

    // Load submesh data into index buffers and textures.
    {
//        NSLog(@"Загрузка данных о сабмэшах модели");
        _numTempleSubmeshes = (NSUInteger)meshData.submeshes.allValues.count;
//        NSLog(@"Описание сабмэшей в модели: %@", meshData.submeshes.description);
//        NSLog(@"Количество сабмэшей в модели: %lu", (unsigned long)(NSUInteger)meshData.submeshes.count);
//        NSLog(@"Количество переменных в сабмэшах модели: %lu", (unsigned long)_numTempleSubmeshes);
        _templeIndexBuffers = (GLuint*)malloc(sizeof(GLuint*) * _numTempleSubmeshes);
        _templeIndexBufferCounts = (GLuint*)malloc(sizeof(GLuint*) * _numTempleSubmeshes);
        _templeTextures = (GLuint*)malloc(sizeof(GLuint*) * _numTempleSubmeshes);

        NSDictionary *loaderOptions =
        @{
          GLKTextureLoaderGenerateMipmaps : @YES,
          GLKTextureLoaderOriginBottomLeft : @YES,
          };

        for(NSUInteger index = 0; index < _numTempleSubmeshes; index++)
        {
            AAPLSubmeshData *submeshData = meshData.submeshes.allValues[index];
//            NSLog(@"В цикле вынимаем переменные сабмеша: %@", meshData.submeshes.allValues[index]);

            _templeIndexBufferCounts[index] = (GLuint)submeshData.indexCount;
//            NSLog(@"Количество данных сабмеша: %u", (GLuint)submeshData.indexCount);

            NSUInteger indexBufferSize = sizeof(uint32_t) * submeshData.indexCount;

            GLuint indexBufferName;

            glGenBuffers(1, &indexBufferName);

            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBufferName);

            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBufferSize, submeshData.indexData, GL_STATIC_DRAW);

            _templeIndexBuffers[index] = indexBufferName;

            GLKTextureInfo *texInfo = [GLKTextureLoader textureWithContentsOfURL:submeshData.baseColorMapURL
                                                                         options:loaderOptions
                                                                           error:&error];

            NSAssert(texInfo, @"Could not load image (%@) into OpenGL texture, error: %@.",
                     submeshData.baseColorMapURL.absoluteString, error);

            _templeTextures[index] = texInfo.name;
//            NSLog(@"Индекс текстуры сабмэша: %u", texInfo.name);
        }
    }

    // Create program object and setup for uniforms.
    {
//        NSLog(@"Линкуем в программу фрагментный и вершинный шейдеры");
        //Тут указывается имена вершинного и фрагментного шейдеров, используемых для отрисовки
        NSURL *vertexSourceURL = [[NSBundle mainBundle] URLForResource:@"temple" withExtension:@"vsh"];
        NSURL *fragmentSourceURL = [[NSBundle mainBundle] URLForResource:@"temple" withExtension:@"fsh"];

        _templeProgram = [AAPLOpenGLRenderer buildProgramWithVertexSourceURL:vertexSourceURL
                                                       withFragmentSourceURL:fragmentSourceURL
                                                                  hasNormals:YES
                                                             hasBaseColorMap:YES];

        _templeNormalMatrixUniformLocation = glGetUniformLocation(_templeProgram, "templeNormalMatrix");
        _ambientLightColorUniformLocation = glGetUniformLocation(_templeProgram, "ambientLightColor");
        _directionalLightInvDirectionUniformLocation = glGetUniformLocation(_templeProgram, "directionalLightInvDirection");
        _directionalLightColorUniformLocation = glGetUniformLocation(_templeProgram, "directionalLightColor");

        _templeMVPUniformLocation = glGetUniformLocation(_templeProgram, "modelViewProjectionMatrix");
    }
    
    // Create select program ojbect and setupe for uniforms.
    {
        [self createSelectedProgram];
    }
}

- (void) createSelectedProgram {
    NSURL *vertexSelectSourceURL = [[NSBundle mainBundle] URLForResource:@"select_vertex_shader" withExtension:@"vsh"];
    NSURL *fragmentSelectSourceURL = [[NSBundle mainBundle] URLForResource:@"select_fragment_shader" withExtension:@"fsh"];

    _selectProgram = [AAPLOpenGLRenderer buildProgramWithVertexSourceURL:vertexSelectSourceURL
                                                         withFragmentSourceURL:fragmentSelectSourceURL
                                                                    hasNormals:NO
                                                               hasBaseColorMap:NO];
    
    GLint location = -1;
    location = glGetUniformLocation(_selectProgram, "u_Code");
    NSAssert(location >= 0, @"No location for `u_Code`.");
    _codeSelectUniformLocation = (GLuint)location;

    _selectionMVPUniformLocation = glGetUniformLocation(_selectProgram, "u_MVPMatrix");
}

- (void) updateFrameState {
//    NSLog(@"Вызываем функцию обновления состояния кадра");
    
    const vector_float3 ambientLightColor = {0.2, 0.2, 0.2};
    const vector_float3 directionalLightDirection = vector_normalize ((vector_float3){0.0, 0.0, 1.0});
    const vector_float3 directionalLightInvDirection = -directionalLightDirection;
    const vector_float3 directionalLightColor = {.9, .9, .9};

    const vector_float3   cameraPosition = {0.0, -10.0, -230.0};
    const matrix_float4x4 cameraViewMatrix  = matrix4x4_translation(-cameraPosition);

    const vector_float3   templeRotationAxis_x      = {1, 0, 0};
    const vector_float3   templeRotationAxis_y      = {0, 1, 0};
    const vector_float3   templeRotationAxis_z      = {0, 0, 1};
    
    const matrix_float4x4 startRotationMatrix_x   = matrix4x4_rotation (-_startAngle, templeRotationAxis_x);
    const matrix_float4x4 startRotationMatrix_y   = matrix4x4_rotation (-_startAngle, templeRotationAxis_y);
    const matrix_float4x4 startRotationMatrix_z   = matrix4x4_rotation (-_startAngleZ, templeRotationAxis_z);
    _startAngle = 0.0f;
    _startAngleZ = 0.0f;
    matrix_float4x4 templeRotationMatrix_x   = matrix4x4_identity();
    matrix_float4x4 templeRotationMatrix_y   = matrix4x4_identity();
    matrix_float4x4 templeRotationMatrix_z   = matrix4x4_identity();
    if (_selectedObject == 124 || _selectedObject == 253 || _selectedObject == 0) {
        if (_handSide == 1){
            templeRotationMatrix_y   = matrix4x4_rotation (_deltaX/10, templeRotationAxis_y);
        } else {
            templeRotationMatrix_y   = matrix4x4_rotation (-_deltaX/10, templeRotationAxis_y);
        }
        
    }
    
    
    matrix_float4x4 currentRotation          = matrix_multiply(startRotationMatrix_y, _accumulateRotationGeneral);
    currentRotation                          = matrix_multiply(startRotationMatrix_z, currentRotation);
    currentRotation                          = matrix_multiply(startRotationMatrix_x, currentRotation);
    currentRotation                          = matrix_multiply(templeRotationMatrix_x, currentRotation);
    _accumulateRotationGeneral               = matrix_multiply(templeRotationMatrix_y, currentRotation);
    
    const matrix_float4x4 templeModelViewMatrix   = matrix_multiply (cameraViewMatrix, _accumulateRotationGeneral);
    const matrix_float3x3 templeNormalMatrix      = matrix3x3_upper_left(_accumulateRotationGeneral);
    
    _templeNormalMatrix           = templeNormalMatrix;
    _ambientLightColor            = ambientLightColor;
    _directionalLightInvDirection = directionalLightInvDirection;
    _directionalLightColor        = directionalLightColor;

    _templeCameraMVPMatrix        = matrix_multiply(_projectionMatrix, templeModelViewMatrix);
    
    _deltaX = 0;
    _deltaY = 0;
}

- (void) resize:(CGSize)size {
    NSLog(@"Вызываем функцию изменения размера прямоугольника отрисовки");
    // Handle the resize of the draw rectangle. In particular, update the perspective projection matrix
    // with a new aspect ratio because the view orientation, layout, or size has changed.
    _viewSize = size;
    float aspect = (float)size.width / size.height;
    if (_handSide == 1) {
        _projectionMatrix = matrix_perspective_right_hand_gl(65.0f * (M_PI / 180.0f), aspect, 1.0f, 5000.0);
    } else {
        _projectionMatrix = matrix_perspective_left_hand_gl(65.0f * (M_PI / 180.0f), aspect, 1.0f, 5000.0);
    }
    
    NSLog(@"Вызываем функцию изменения размера прямоугольника отрисовки size.width: %f  size.height: %f", size.width, size.height);

}

- (void) draw
{
    glUniform3fv(_ambientLightColorUniformLocation, 1, (GLvoid*)&_ambientLightColor);
    glUniform3fv(_directionalLightInvDirectionUniformLocation, 1, (GLvoid*)&_directionalLightInvDirection);
    glUniform3fv(_directionalLightColorUniformLocation, 1, (GLvoid*)&_directionalLightColor);

    glEnable(GL_DEPTH_TEST);

    glFrontFace(GL_CW);

    glCullFace(GL_BACK);

    
    // Bind the default FBO to render to the screen.
    glBindFramebuffer(GL_FRAMEBUFFER, _defaultFBOName);

    glViewport(0, 0, _viewSize.width, _viewSize.height);
    
    glClearColor(0.023f, 0.023f, 0.023f, 1.0f);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    
    // Use the program that renders the temple.
    glUseProgram(_templeProgram);
    
//    glUniformMatrix4fv(_templeMVPUniformLocation, 1, GL_FALSE, (const GLfloat*)&_templeCameraMVPMatrix);

    // Bind the vertex array object with the temple mesh vertices.
    glBindVertexArray(_templeVAO);

    // Draw the temple object to the drawable. _numTempleSubmeshes - количество полигональных сеток в разными материалами (регулируются материалами каждого мэша в файле mtl и obj)
    
    [self drawBigFinger];
    [self drawLittleFinger];
    [self drawRingFinger];
    [self drawMiddleFinger];
    [self drawForeFinger];
    
    
    [self updateFrameState];
    float packed3x3NormalMatrix[9] =
    {
        _templeNormalMatrix.columns[0].x,
        _templeNormalMatrix.columns[0].y,
        _templeNormalMatrix.columns[0].z,
        _templeNormalMatrix.columns[1].x,
        _templeNormalMatrix.columns[1].y,
        _templeNormalMatrix.columns[1].z,
        _templeNormalMatrix.columns[2].x,
        _templeNormalMatrix.columns[2].y,
        _templeNormalMatrix.columns[2].z,
    };
    glUniformMatrix3fv(_templeNormalMatrixUniformLocation, 1, GL_FALSE, packed3x3NormalMatrix);
    glUniformMatrix4fv(_templeMVPUniformLocation, 1, GL_FALSE, (const GLfloat*)&_templeCameraMVPMatrix);
    [self drawPalm];
}

-(void) drawForeFinger {
    const float x_traslate_1 = -37.2;
    const float y_traslate_1 = -0.2;
    const float z_traslate_1 = 22.257;
    const float r_x_traslate_1 = fabsf(y_traslate_1)*sinf(_angleForeFingerFloat) + fabsf(x_traslate_1)*cosf(_angleForeFingerFloat);
    const float r_y_traslate_1 = fabsf(x_traslate_1)*sinf(_angleForeFingerFloat) - fabsf(y_traslate_1)*cosf(_angleForeFingerFloat);
    const float r_z_traslate_1 = -z_traslate_1 - (0.9*sinf(_angleForeFingerFloat*2));
    
    const vector_float3   cameraPosition = {0.0, -10.0, -230.0};
    const matrix_float4x4 cameraViewMatrix  = matrix4x4_translation(-cameraPosition);
    
    const vector_float3   templeRotationAxis_x      = {1, 0, 0};
    const vector_float3   templeRotationAxis_y      = {0, 1, 0};
    const vector_float3   templeRotationAxis_z      = {0, 0, 1};

    const matrix_float4x4 nullTranslationMatrix = matrix4x4_translation(0.0, 0.0, 0.0);
    const matrix_float4x4 templeTranslationMatrix = matrix4x4_translation(x_traslate_1, y_traslate_1, z_traslate_1);
    const matrix_float4x4 rTranslationMatrix   = matrix4x4_translation(r_x_traslate_1, r_y_traslate_1, r_z_traslate_1);
    
    const matrix_float4x4 templeRotationMatrix_1   = matrix4x4_rotation (0.04, templeRotationAxis_x);  // 0.035    2°    0.087  5°
    const matrix_float4x4 templeRotationMatrix_2   = matrix4x4_rotation (-0.035, templeRotationAxis_y); //-0.052   -3°
    const matrix_float4x4 templeRotationMatrix_3   = matrix4x4_rotation (-_angleForeFingerFloat, templeRotationAxis_z);
    const matrix_float4x4 templeRotationMatrix_4   = matrix4x4_rotation (0.035, templeRotationAxis_y);
    const matrix_float4x4 templeRotationMatrix_5   = matrix4x4_rotation (-0.034, templeRotationAxis_x);

    // Вторая фаланга
    // Вращение относительно середины пальца
    if (_selectedObject == 4) { _angleForeFingerFloat += _deltaY/10; }
    if((_angleForeFingerFloat < 0.0174 || _angleForeFingerFloat > 1.727)) { // 0.0174 - 1°   1.727 - 99°  -59° - -1.029   29° - 0.5059
        _angleForeFingerFloat -= _deltaY/10;
    }
    matrix_float4x4 currentRotation_my = matrix4x4_identity();
    if((_angleForeFingerTransfer >= 0 && _angleForeFingerTransfer <= 100)) { //  -60° - -1.047  30° - 0.523
        currentRotation_my                 = matrix_multiply(templeRotationMatrix_1, templeTranslationMatrix);
        currentRotation_my                 = matrix_multiply(templeRotationMatrix_2, currentRotation_my);
        currentRotation_my                 = matrix_multiply(templeRotationMatrix_3, currentRotation_my);
        currentRotation_my                 = matrix_multiply(templeRotationMatrix_4, currentRotation_my);
        _accumulateRotationForeFinger2     = matrix_multiply(templeRotationMatrix_5, currentRotation_my);
    }
    _angleForeFingerTransfer = (int) (_angleForeFingerFloat/M_PI*180);
//    NSLog(@"Отправляемый угол  _angleForeFingerTransfer: %d", _angleForeFingerTransfer);
    
    // Перемещение ко второй оси вращения
    currentRotation_my                 = matrix_multiply(_accumulateRotationForeFinger, _accumulateRotationForeFinger2);
    // Перемещение в сборку
    const matrix_float4x4 currentRotation_my5                 = matrix_multiply(currentRotation_my, rTranslationMatrix);
    // Вращение в составе всей сборки
    const matrix_float4x4 currentRotation_my6                 = matrix_multiply(_accumulateRotationGeneral, currentRotation_my5);// TODO попробовать использовать currentRotation при финальной отрисовке, сравнить с
    // Отрисовка второй фаланги
    const matrix_float4x4 templeModelViewMatrix   = matrix_multiply (cameraViewMatrix, currentRotation_my6);
    const matrix_float3x3 templeNormalMatrix      = matrix3x3_upper_left(currentRotation_my6);
    
    _templeCameraMVPMatrix        = matrix_multiply(_projectionMatrix, templeModelViewMatrix);
    _templeNormalMatrix           = templeNormalMatrix;
    
    float packed3x3NormalMatrix[9] =
    {
        _templeNormalMatrix.columns[0].x,
        _templeNormalMatrix.columns[0].y,
        _templeNormalMatrix.columns[0].z,
        _templeNormalMatrix.columns[1].x,
        _templeNormalMatrix.columns[1].y,
        _templeNormalMatrix.columns[1].z,
        _templeNormalMatrix.columns[2].x,
        _templeNormalMatrix.columns[2].y,
        _templeNormalMatrix.columns[2].z,
    };
    glUniformMatrix3fv(_templeNormalMatrixUniformLocation, 1, GL_FALSE, packed3x3NormalMatrix);
    glUniformMatrix4fv(_templeMVPUniformLocation, 1, GL_FALSE, (const GLfloat*)&_templeCameraMVPMatrix);
    
    int sketchableParts[] = {12,8};
    for(GLuint i = 0; i < 2 ; i++)
    {
        glBindTexture(GL_TEXTURE_2D, _templeTextures[sketchableParts[i]]);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, _templeIndexBuffers[sketchableParts[i]]);
        glDrawElements(GL_TRIANGLES, _templeIndexBufferCounts[sketchableParts[i]], GL_UNSIGNED_INT, 0);
    }
    
    // Первая фаланга
    // Вращение относительно костяшки пальца
    const matrix_float4x4 templeRotationMatrix2_1   = matrix4x4_rotation (0.035, templeRotationAxis_x);  //0.035
    const matrix_float4x4 templeRotationMatrix2_2   = matrix4x4_rotation (-0.052, templeRotationAxis_y); //0.052
    const matrix_float4x4 templeRotationMatrix2_3   = matrix4x4_rotation (-_angleForeFingerFloat, templeRotationAxis_z);
    const matrix_float4x4 templeRotationMatrix2_4   = matrix4x4_rotation (0.052, templeRotationAxis_y);
    const matrix_float4x4 templeRotationMatrix2_5   = matrix4x4_rotation (-0.035, templeRotationAxis_x);
    
    matrix_float4x4 currentRotation2 = matrix4x4_identity();
    if((_angleForeFingerTransfer >= 0 && _angleForeFingerTransfer <= 100)) {
        currentRotation2                 = matrix_multiply(templeRotationMatrix2_1, nullTranslationMatrix);
        currentRotation2                 = matrix_multiply(templeRotationMatrix2_2, currentRotation2);
        currentRotation2                 = matrix_multiply(templeRotationMatrix2_3, currentRotation2);
        currentRotation2                 = matrix_multiply(templeRotationMatrix2_4, currentRotation2);
        _accumulateRotationForeFinger    = matrix_multiply(templeRotationMatrix2_5, currentRotation2);
    }
    // Вращение в составе всей сборки
    currentRotation2                 = matrix_multiply(_accumulateRotationGeneral, _accumulateRotationForeFinger);
    // Отрисовка первой фаланги
    const matrix_float4x4 templeModelViewMatrix2   = matrix_multiply (cameraViewMatrix, currentRotation2);
    const matrix_float3x3 templeNormalMatrix2     = matrix3x3_upper_left(currentRotation2);

    _templeCameraMVPMatrix        = matrix_multiply(_projectionMatrix, templeModelViewMatrix2);
    _templeNormalMatrix           = templeNormalMatrix2;

    float packed3x3NormalMatrix2[9] =
    {
        _templeNormalMatrix.columns[0].x,
        _templeNormalMatrix.columns[0].y,
        _templeNormalMatrix.columns[0].z,
        _templeNormalMatrix.columns[1].x,
        _templeNormalMatrix.columns[1].y,
        _templeNormalMatrix.columns[1].z,
        _templeNormalMatrix.columns[2].x,
        _templeNormalMatrix.columns[2].y,
        _templeNormalMatrix.columns[2].z,
    };
    glUniformMatrix3fv(_templeNormalMatrixUniformLocation, 1, GL_FALSE, packed3x3NormalMatrix2);
    glUniformMatrix4fv(_templeMVPUniformLocation, 1, GL_FALSE, (const GLfloat*)&_templeCameraMVPMatrix);
    
    sketchableParts[0] = 4;
    for(GLuint i = 0; i < 1 ; i++)
    {
        glBindTexture(GL_TEXTURE_2D, _templeTextures[sketchableParts[i]]);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, _templeIndexBuffers[sketchableParts[i]]);
        glDrawElements(GL_TRIANGLES, _templeIndexBufferCounts[sketchableParts[i]], GL_UNSIGNED_INT, 0);
    }
}
-(void) drawMiddleFinger {
    const float x_traslate_1 = -37.0;
    const float y_traslate_1 = -0.2;
    const float z_traslate_1 = 5.03;
    const float r_x_traslate_1 = fabsf(y_traslate_1)*sinf(_angleMiddleFingerFloat) + fabsf(x_traslate_1)*cosf(_angleMiddleFingerFloat);
    const float r_y_traslate_1 = fabsf(x_traslate_1)*sinf(_angleMiddleFingerFloat) - fabsf(y_traslate_1)*cosf(_angleMiddleFingerFloat);
    const float r_z_traslate_1 = -z_traslate_1 ;//+ (0.5*sinf(_angleMiddleFingerFloat));
    const vector_float3   cameraPosition = {0.0, -10.0, -230.0};
    const matrix_float4x4 cameraViewMatrix  = matrix4x4_translation(-cameraPosition);
    
    const vector_float3   templeRotationAxis_x      = {1, 0, 0};
    const vector_float3   templeRotationAxis_y      = {0, 1, 0};
    const vector_float3   templeRotationAxis_z      = {0, 0, 1};

    const matrix_float4x4 nullTranslationMatrix = matrix4x4_translation(0.0, 0.0, 0.0);
    const matrix_float4x4 templeTranslationMatrix = matrix4x4_translation(x_traslate_1, y_traslate_1, z_traslate_1);
    const matrix_float4x4 rTranslationMatrix   = matrix4x4_translation(r_x_traslate_1, r_y_traslate_1, r_z_traslate_1);
    
    const matrix_float4x4 templeRotationMatrix_1   = matrix4x4_rotation (0.005, templeRotationAxis_x);  // 0.035    3°  //
    const matrix_float4x4 templeRotationMatrix_2   = matrix4x4_rotation (-0.002, templeRotationAxis_y); //-0.052   -5°
    const matrix_float4x4 templeRotationMatrix_3   = matrix4x4_rotation (-_angleMiddleFingerFloat, templeRotationAxis_z);
    const matrix_float4x4 templeRotationMatrix_4   = matrix4x4_rotation (0.002, templeRotationAxis_y);
    const matrix_float4x4 templeRotationMatrix_5   = matrix4x4_rotation (-0.005, templeRotationAxis_x);

    // Вторая фаланга
    // Вращение относительно середины пальца
    if (_selectedObject == 3) { _angleMiddleFingerFloat += _deltaY/10; }
    if((_angleMiddleFingerFloat < 0.0174 || _angleMiddleFingerFloat > 1.727)) { // 0.0174 - 1°   1.727 - 99°  -59° - -1.029   29° - 0.5059
        _angleMiddleFingerFloat -= _deltaY/10;
    }
    matrix_float4x4 currentRotation_my = matrix4x4_identity();
    if((_angleMiddleFingerTransfer >= 0 && _angleMiddleFingerTransfer <= 100)) {
        currentRotation_my                 = matrix_multiply(templeRotationMatrix_1, templeTranslationMatrix);
        currentRotation_my                 = matrix_multiply(templeRotationMatrix_2, currentRotation_my);
        currentRotation_my                 = matrix_multiply(templeRotationMatrix_3, currentRotation_my);
        currentRotation_my                 = matrix_multiply(templeRotationMatrix_4, currentRotation_my);
        _accumulateRotationMiddleFinger2   = matrix_multiply(templeRotationMatrix_5, currentRotation_my);
    }
    _angleMiddleFingerTransfer = (int) (_angleMiddleFingerFloat/M_PI*180);
//    NSLog(@"Отправляемый угол  _angleMiddleFingerTransfer: %d", _angleMiddleFingerTransfer);
    
    // Перемещение ко второй оси вращения
    currentRotation_my                 = matrix_multiply(_accumulateRotationMiddleFinger, _accumulateRotationMiddleFinger2);
    // Перемещение в сборку
    const matrix_float4x4 currentRotation_my5                 = matrix_multiply(currentRotation_my, rTranslationMatrix);
    // Вращение в составе всей сборки
    const matrix_float4x4 currentRotation_my6                 = matrix_multiply(_accumulateRotationGeneral, currentRotation_my5);// TODO попробовать использовать currentRotation при финальной отрисовке, сравнить с
    // Отрисовка второй фаланги
    const matrix_float4x4 templeModelViewMatrix   = matrix_multiply (cameraViewMatrix, currentRotation_my6);
    const matrix_float3x3 templeNormalMatrix      = matrix3x3_upper_left(currentRotation_my6);
    
    _templeCameraMVPMatrix        = matrix_multiply(_projectionMatrix, templeModelViewMatrix);
    _templeNormalMatrix           = templeNormalMatrix;
    
    float packed3x3NormalMatrix[9] =
    {
        _templeNormalMatrix.columns[0].x,
        _templeNormalMatrix.columns[0].y,
        _templeNormalMatrix.columns[0].z,
        _templeNormalMatrix.columns[1].x,
        _templeNormalMatrix.columns[1].y,
        _templeNormalMatrix.columns[1].z,
        _templeNormalMatrix.columns[2].x,
        _templeNormalMatrix.columns[2].y,
        _templeNormalMatrix.columns[2].z,
    };
    glUniformMatrix3fv(_templeNormalMatrixUniformLocation, 1, GL_FALSE, packed3x3NormalMatrix);
    glUniformMatrix4fv(_templeMVPUniformLocation, 1, GL_FALSE, (const GLfloat*)&_templeCameraMVPMatrix);
    
    int sketchableParts[] = {6,11};
    for(GLuint i = 0; i < 2 ; i++)
    {
        glBindTexture(GL_TEXTURE_2D, _templeTextures[sketchableParts[i]]);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, _templeIndexBuffers[sketchableParts[i]]);
        glDrawElements(GL_TRIANGLES, _templeIndexBufferCounts[sketchableParts[i]], GL_UNSIGNED_INT, 0);
    }
    
    // Первая фаланга
    // Вращение относительно костяшки пальца
    const matrix_float4x4 templeRotationMatrix2_1   = matrix4x4_rotation (0.035, templeRotationAxis_x);  //0.035
    const matrix_float4x4 templeRotationMatrix2_2   = matrix4x4_rotation (-0.052, templeRotationAxis_y); //0.052
    const matrix_float4x4 templeRotationMatrix2_3   = matrix4x4_rotation (-_angleMiddleFingerFloat, templeRotationAxis_z);
    const matrix_float4x4 templeRotationMatrix2_4   = matrix4x4_rotation (0.052, templeRotationAxis_y);
    const matrix_float4x4 templeRotationMatrix2_5   = matrix4x4_rotation (-0.035, templeRotationAxis_x);
    
    matrix_float4x4 currentRotation2 = matrix4x4_identity();
    if((_angleMiddleFingerTransfer >= 0 && _angleMiddleFingerTransfer <= 100)) {
        currentRotation2                 = matrix_multiply(templeRotationMatrix2_1, nullTranslationMatrix);
        currentRotation2                 = matrix_multiply(templeRotationMatrix2_2, currentRotation2);
        currentRotation2                 = matrix_multiply(templeRotationMatrix2_3, currentRotation2);
        currentRotation2                 = matrix_multiply(templeRotationMatrix2_4, currentRotation2);
        _accumulateRotationMiddleFinger  = matrix_multiply(templeRotationMatrix2_5, currentRotation2);
    }
    // Вращение в составе всей сборки
    currentRotation2                 = matrix_multiply(_accumulateRotationGeneral, _accumulateRotationMiddleFinger);
    // Отрисовка первой фаланги
    const matrix_float4x4 templeModelViewMatrix2   = matrix_multiply (cameraViewMatrix, currentRotation2);
    const matrix_float3x3 templeNormalMatrix2     = matrix3x3_upper_left(currentRotation2);

    _templeCameraMVPMatrix        = matrix_multiply(_projectionMatrix, templeModelViewMatrix2);
    _templeNormalMatrix           = templeNormalMatrix2;

    float packed3x3NormalMatrix2[9] =
    {
        _templeNormalMatrix.columns[0].x,
        _templeNormalMatrix.columns[0].y,
        _templeNormalMatrix.columns[0].z,
        _templeNormalMatrix.columns[1].x,
        _templeNormalMatrix.columns[1].y,
        _templeNormalMatrix.columns[1].z,
        _templeNormalMatrix.columns[2].x,
        _templeNormalMatrix.columns[2].y,
        _templeNormalMatrix.columns[2].z,
    };
    glUniformMatrix3fv(_templeNormalMatrixUniformLocation, 1, GL_FALSE, packed3x3NormalMatrix2);
    glUniformMatrix4fv(_templeMVPUniformLocation, 1, GL_FALSE, (const GLfloat*)&_templeCameraMVPMatrix);
    
    sketchableParts[0] = 15;
    for(GLuint i = 0; i < 1 ; i++)
    {
        glBindTexture(GL_TEXTURE_2D, _templeTextures[sketchableParts[i]]);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, _templeIndexBuffers[sketchableParts[i]]);
        glDrawElements(GL_TRIANGLES, _templeIndexBufferCounts[sketchableParts[i]], GL_UNSIGNED_INT, 0);
    }
}
-(void) drawRingFinger {
    const float x_traslate_1 = -37.3;
    const float y_traslate_1 = -0.3;
    const float z_traslate_1 = -10.6;
    const float r_x_traslate_1 = fabsf(y_traslate_1)*sinf(_angleRingFingerFloat) + fabsf(x_traslate_1)*cosf(_angleRingFingerFloat);
    const float r_y_traslate_1 = fabsf(x_traslate_1)*sinf(_angleRingFingerFloat) - fabsf(y_traslate_1)*cosf(_angleRingFingerFloat);
    const float r_z_traslate_1 = -z_traslate_1;// + (0.5*sinf(_angleRingFingerFloat));
    const vector_float3   cameraPosition = {0.0, -10.0, -230.0};
    const matrix_float4x4 cameraViewMatrix  = matrix4x4_translation(-cameraPosition);
    
    const vector_float3   templeRotationAxis_x      = {1, 0, 0};
    const vector_float3   templeRotationAxis_y      = {0, 1, 0};
    const vector_float3   templeRotationAxis_z      = {0, 0, 1};

    const matrix_float4x4 nullTranslationMatrix = matrix4x4_translation(0.0, 0.0, 0.0);
    const matrix_float4x4 templeTranslationMatrix = matrix4x4_translation(x_traslate_1, y_traslate_1, z_traslate_1);
    const matrix_float4x4 rTranslationMatrix   = matrix4x4_translation(r_x_traslate_1, r_y_traslate_1, r_z_traslate_1);
    
    const matrix_float4x4 templeRotationMatrix_1   = matrix4x4_rotation (0.000, templeRotationAxis_x);  // 0.034    2°
    const matrix_float4x4 templeRotationMatrix_2   = matrix4x4_rotation (-0.0, templeRotationAxis_y); //-0.052   -5°
    const matrix_float4x4 templeRotationMatrix_3   = matrix4x4_rotation (-_angleRingFingerFloat, templeRotationAxis_z);
    const matrix_float4x4 templeRotationMatrix_4   = matrix4x4_rotation (0.0, templeRotationAxis_y);
    const matrix_float4x4 templeRotationMatrix_5   = matrix4x4_rotation (-0.000, templeRotationAxis_x);

    // Вторая фаланга
    // Вращение относительно середины пальца
    if (_selectedObject == 2) { _angleRingFingerFloat += _deltaY/10; }
    if((_angleRingFingerFloat < 0.0174 || _angleRingFingerFloat > 1.727)) { // 0.0174 - 1°   1.727 - 99°  -59° - -1.029   29° - 0.5059
        _angleRingFingerFloat -= _deltaY/10;
    }
    matrix_float4x4 currentRotation_my = matrix4x4_identity();
    if((_angleRingFingerTransfer >= 0 && _angleRingFingerTransfer <= 100)) {
        currentRotation_my                 = matrix_multiply(templeRotationMatrix_1, templeTranslationMatrix);
        currentRotation_my                 = matrix_multiply(templeRotationMatrix_2, currentRotation_my);
        currentRotation_my                 = matrix_multiply(templeRotationMatrix_3, currentRotation_my);
        currentRotation_my                 = matrix_multiply(templeRotationMatrix_4, currentRotation_my);
        _accumulateRotationRingFinger2     = matrix_multiply(templeRotationMatrix_5, currentRotation_my);
    }
    _angleRingFingerTransfer = (int) (_angleRingFingerFloat/M_PI*180);
//    NSLog(@"Отправляемый угол  _angleRingFingerTransfer: %d", _angleRingFingerTransfer);
    
    // Перемещение ко второй оси вращения
    currentRotation_my                 = matrix_multiply(_accumulateRotationRingFinger, _accumulateRotationRingFinger2);
    // Перемещение в сборку
    const matrix_float4x4 currentRotation_my5                 = matrix_multiply(currentRotation_my, rTranslationMatrix);
    // Вращение в составе всей сборки
    const matrix_float4x4 currentRotation_my6                 = matrix_multiply(_accumulateRotationGeneral, currentRotation_my5);// TODO попробовать использовать currentRotation при финальной отрисовке, сравнить с
    // Отрисовка второй фаланги
    const matrix_float4x4 templeModelViewMatrix   = matrix_multiply (cameraViewMatrix, currentRotation_my6);
    const matrix_float3x3 templeNormalMatrix      = matrix3x3_upper_left(currentRotation_my6);
    
    _templeCameraMVPMatrix        = matrix_multiply(_projectionMatrix, templeModelViewMatrix);
    _templeNormalMatrix           = templeNormalMatrix;
    
    float packed3x3NormalMatrix[9] =
    {
        _templeNormalMatrix.columns[0].x,
        _templeNormalMatrix.columns[0].y,
        _templeNormalMatrix.columns[0].z,
        _templeNormalMatrix.columns[1].x,
        _templeNormalMatrix.columns[1].y,
        _templeNormalMatrix.columns[1].z,
        _templeNormalMatrix.columns[2].x,
        _templeNormalMatrix.columns[2].y,
        _templeNormalMatrix.columns[2].z,
    };
    glUniformMatrix3fv(_templeNormalMatrixUniformLocation, 1, GL_FALSE, packed3x3NormalMatrix);
    glUniformMatrix4fv(_templeMVPUniformLocation, 1, GL_FALSE, (const GLfloat*)&_templeCameraMVPMatrix);
    
    int sketchableParts[] = {16,10};
    for(GLuint i = 0; i < 2 ; i++)
    {
        glBindTexture(GL_TEXTURE_2D, _templeTextures[sketchableParts[i]]);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, _templeIndexBuffers[sketchableParts[i]]);
        glDrawElements(GL_TRIANGLES, _templeIndexBufferCounts[sketchableParts[i]], GL_UNSIGNED_INT, 0);
    }
    
    // Первая фаланга
    // Вращение относительно костяшки пальца
    const matrix_float4x4 templeRotationMatrix2_1   = matrix4x4_rotation (0.034, templeRotationAxis_x);
    const matrix_float4x4 templeRotationMatrix2_2   = matrix4x4_rotation (0, templeRotationAxis_y);
    const matrix_float4x4 templeRotationMatrix2_3   = matrix4x4_rotation (-_angleRingFingerFloat, templeRotationAxis_z);
    const matrix_float4x4 templeRotationMatrix2_4   = matrix4x4_rotation (0, templeRotationAxis_y);
    const matrix_float4x4 templeRotationMatrix2_5   = matrix4x4_rotation (-0.034, templeRotationAxis_x);
    
    matrix_float4x4 currentRotation2 = matrix4x4_identity();
    if((_angleRingFingerTransfer >= 0 && _angleRingFingerTransfer <= 100)) {
        currentRotation2                 = matrix_multiply(templeRotationMatrix2_1, nullTranslationMatrix);
        currentRotation2                 = matrix_multiply(templeRotationMatrix2_2, currentRotation2);
        currentRotation2                 = matrix_multiply(templeRotationMatrix2_3, currentRotation2);
        currentRotation2                 = matrix_multiply(templeRotationMatrix2_4, currentRotation2);
        _accumulateRotationRingFinger    = matrix_multiply(templeRotationMatrix2_5, currentRotation2);
    }
    // Вращение в составе всей сборки
    currentRotation2                 = matrix_multiply(_accumulateRotationGeneral, _accumulateRotationRingFinger);
    // Отрисовка первой фаланги
    const matrix_float4x4 templeModelViewMatrix2   = matrix_multiply (cameraViewMatrix, currentRotation2);
    const matrix_float3x3 templeNormalMatrix2     = matrix3x3_upper_left(currentRotation2);

    _templeCameraMVPMatrix        = matrix_multiply(_projectionMatrix, templeModelViewMatrix2);
    _templeNormalMatrix           = templeNormalMatrix2;

    float packed3x3NormalMatrix2[9] =
    {
        _templeNormalMatrix.columns[0].x,
        _templeNormalMatrix.columns[0].y,
        _templeNormalMatrix.columns[0].z,
        _templeNormalMatrix.columns[1].x,
        _templeNormalMatrix.columns[1].y,
        _templeNormalMatrix.columns[1].z,
        _templeNormalMatrix.columns[2].x,
        _templeNormalMatrix.columns[2].y,
        _templeNormalMatrix.columns[2].z,
    };
    glUniformMatrix3fv(_templeNormalMatrixUniformLocation, 1, GL_FALSE, packed3x3NormalMatrix2);
    glUniformMatrix4fv(_templeMVPUniformLocation, 1, GL_FALSE, (const GLfloat*)&_templeCameraMVPMatrix);
    
    sketchableParts[0] = 3;
    for(GLuint i = 0; i < 1 ; i++)
    {
        glBindTexture(GL_TEXTURE_2D, _templeTextures[sketchableParts[i]]);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, _templeIndexBuffers[sketchableParts[i]]);
        glDrawElements(GL_TRIANGLES, _templeIndexBufferCounts[sketchableParts[i]], GL_UNSIGNED_INT, 0);
    }
}
-(void) drawLittleFinger {
    const float x_traslate_1 = -36;
    const float y_traslate_1 = 1;
    const float z_traslate_1 = -28.43;
    const float r_x_traslate_1 = fabsf(y_traslate_1)*sinf(_angleLittleFingerFloat) + fabsf(x_traslate_1)*cosf(_angleLittleFingerFloat);
    const float r_y_traslate_1 = fabsf(x_traslate_1)*sinf(_angleLittleFingerFloat) - fabsf(y_traslate_1)*cosf(_angleLittleFingerFloat);
    const float r_z_traslate_1 = -z_traslate_1 + (1.5*sinf(_angleLittleFingerFloat));
    const vector_float3   cameraPosition = {0.0, -10.0, -230.0};
    const matrix_float4x4 cameraViewMatrix  = matrix4x4_translation(-cameraPosition);
    
    const vector_float3   templeRotationAxis_x      = {1, 0, 0};
    const vector_float3   templeRotationAxis_y      = {0, 1, 0};
    const vector_float3   templeRotationAxis_z      = {0, 0, 1};

    const matrix_float4x4 nullTranslationMatrix = matrix4x4_translation(0.0, 0.0, 0.0);
    const matrix_float4x4 templeTranslationMatrix = matrix4x4_translation(x_traslate_1, y_traslate_1, z_traslate_1);
    const matrix_float4x4 rTranslationMatrix   = matrix4x4_translation(r_x_traslate_1, r_y_traslate_1, r_z_traslate_1);
    
    const matrix_float4x4 templeRotationMatrix_1   = matrix4x4_rotation (-0.05, templeRotationAxis_x);  // 0.035    3°  //
    const matrix_float4x4 templeRotationMatrix_2   = matrix4x4_rotation (0.017, templeRotationAxis_y); //-0.052   -5°
    const matrix_float4x4 templeRotationMatrix_3   = matrix4x4_rotation (-_angleLittleFingerFloat, templeRotationAxis_z);
    const matrix_float4x4 templeRotationMatrix_4   = matrix4x4_rotation (-0.017, templeRotationAxis_y);
    const matrix_float4x4 templeRotationMatrix_5   = matrix4x4_rotation (0.05, templeRotationAxis_x);

    // Вторая фаланга
    // Вращение относительно середины пальца
    if (_selectedObject == 1) { _angleLittleFingerFloat += _deltaY/10; }
    if((_angleLittleFingerFloat < 0.0174 || _angleLittleFingerFloat > 1.727)) { // 0.0174 - 1°   1.727 - 99°
        _angleLittleFingerFloat -= _deltaY/10;
    }
    matrix_float4x4 currentRotation_my = matrix4x4_identity();
    if((_angleLittleFingerTransfer >= 0 && _angleLittleFingerTransfer <= 100)) {
        currentRotation_my                 = matrix_multiply(templeRotationMatrix_1, templeTranslationMatrix);
        currentRotation_my                 = matrix_multiply(templeRotationMatrix_2, currentRotation_my);
        currentRotation_my                 = matrix_multiply(templeRotationMatrix_3, currentRotation_my);
        currentRotation_my                 = matrix_multiply(templeRotationMatrix_4, currentRotation_my);
        _accumulateRotationLittleFinger2   = matrix_multiply(templeRotationMatrix_5, currentRotation_my);
    }
    _angleLittleFingerTransfer = (int) (_angleLittleFingerFloat/M_PI*180);
//    NSLog(@"Отправляемый угол  _angleLittleFingerTransfer: %d", _angleLittleFingerTransfer);
    
    // Перемещение ко второй оси вращения
    currentRotation_my                 = matrix_multiply(_accumulateRotationLittleFinger, _accumulateRotationLittleFinger2);
    // Перемещение в сборку
    const matrix_float4x4 currentRotation_my5                 = matrix_multiply(currentRotation_my, rTranslationMatrix);
    // Вращение в составе всей сборки
    const matrix_float4x4 currentRotation_my6                 = matrix_multiply(_accumulateRotationGeneral, currentRotation_my5);
    // Отрисовка второй фаланги
    const matrix_float4x4 templeModelViewMatrix   = matrix_multiply (cameraViewMatrix, currentRotation_my6);
    const matrix_float3x3 templeNormalMatrix      = matrix3x3_upper_left(currentRotation_my6);
    
    _templeCameraMVPMatrix        = matrix_multiply(_projectionMatrix, templeModelViewMatrix);
    _templeNormalMatrix           = templeNormalMatrix;
    
    float packed3x3NormalMatrix[9] =
    {
        _templeNormalMatrix.columns[0].x,
        _templeNormalMatrix.columns[0].y,
        _templeNormalMatrix.columns[0].z,
        _templeNormalMatrix.columns[1].x,
        _templeNormalMatrix.columns[1].y,
        _templeNormalMatrix.columns[1].z,
        _templeNormalMatrix.columns[2].x,
        _templeNormalMatrix.columns[2].y,
        _templeNormalMatrix.columns[2].z,
    };
    glUniformMatrix3fv(_templeNormalMatrixUniformLocation, 1, GL_FALSE, packed3x3NormalMatrix);
    glUniformMatrix4fv(_templeMVPUniformLocation, 1, GL_FALSE, (const GLfloat*)&_templeCameraMVPMatrix);
    
    int sketchableParts[] = {17,5};
    for(GLuint i = 0; i < 2 ; i++)
    {
        glBindTexture(GL_TEXTURE_2D, _templeTextures[sketchableParts[i]]);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, _templeIndexBuffers[sketchableParts[i]]);
        glDrawElements(GL_TRIANGLES, _templeIndexBufferCounts[sketchableParts[i]], GL_UNSIGNED_INT, 0);
    }
    
    // Первая фаланга
    // Вращение относительно костяшки пальца
    const matrix_float4x4 templeRotationMatrix2_1   = matrix4x4_rotation (-0.008, templeRotationAxis_x);
    const matrix_float4x4 templeRotationMatrix2_2   = matrix4x4_rotation (0.037, templeRotationAxis_y);
    const matrix_float4x4 templeRotationMatrix2_3   = matrix4x4_rotation (-_angleLittleFingerFloat, templeRotationAxis_z);
    const matrix_float4x4 templeRotationMatrix2_4   = matrix4x4_rotation (-0.037, templeRotationAxis_y);
    const matrix_float4x4 templeRotationMatrix2_5   = matrix4x4_rotation (0.008, templeRotationAxis_x);
    
    matrix_float4x4 currentRotation2 = matrix4x4_identity();
    if((_angleLittleFingerTransfer >= 0 && _angleLittleFingerTransfer <= 100)) {
        currentRotation2                 = matrix_multiply(templeRotationMatrix2_1, nullTranslationMatrix);
        currentRotation2                 = matrix_multiply(templeRotationMatrix2_2, currentRotation2);
        currentRotation2                 = matrix_multiply(templeRotationMatrix2_3, currentRotation2);
        currentRotation2                 = matrix_multiply(templeRotationMatrix2_4, currentRotation2);
        _accumulateRotationLittleFinger  = matrix_multiply(templeRotationMatrix2_5, currentRotation2);
    }
    // Вращение в составе всей сборки
    currentRotation2                 = matrix_multiply(_accumulateRotationGeneral, _accumulateRotationLittleFinger);
    // Отрисовка первой фаланги
    const matrix_float4x4 templeModelViewMatrix2   = matrix_multiply (cameraViewMatrix, currentRotation2);
    const matrix_float3x3 templeNormalMatrix2     = matrix3x3_upper_left(currentRotation2);

    _templeCameraMVPMatrix        = matrix_multiply(_projectionMatrix, templeModelViewMatrix2);
    _templeNormalMatrix           = templeNormalMatrix2;

    float packed3x3NormalMatrix2[9] =
    {
        _templeNormalMatrix.columns[0].x,
        _templeNormalMatrix.columns[0].y,
        _templeNormalMatrix.columns[0].z,
        _templeNormalMatrix.columns[1].x,
        _templeNormalMatrix.columns[1].y,
        _templeNormalMatrix.columns[1].z,
        _templeNormalMatrix.columns[2].x,
        _templeNormalMatrix.columns[2].y,
        _templeNormalMatrix.columns[2].z,
    };
    glUniformMatrix3fv(_templeNormalMatrixUniformLocation, 1, GL_FALSE, packed3x3NormalMatrix2);
    glUniformMatrix4fv(_templeMVPUniformLocation, 1, GL_FALSE, (const GLfloat*)&_templeCameraMVPMatrix);
    
    sketchableParts[0] = 13;
    for(GLuint i = 0; i < 1 ; i++)
    {
        glBindTexture(GL_TEXTURE_2D, _templeTextures[sketchableParts[i]]);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, _templeIndexBuffers[sketchableParts[i]]);
        glDrawElements(GL_TRIANGLES, _templeIndexBufferCounts[sketchableParts[i]], GL_UNSIGNED_INT, 0);
    }
}
-(void) drawBigFinger {

    const float x_traslate_1 = 57.835;
    const float y_traslate_1 = 31.673;
    const float z_traslate_1 = 29.318;
    const float r_x_traslate_1 = - fabsf(x_traslate_1)*cosf(-_angleBigFingerFloat) - fabsf(y_traslate_1)*sinf(-_angleBigFingerFloat);
    const float r_y_traslate_1 = fabsf(x_traslate_1)*sinf(-_angleBigFingerFloat) - fabsf(y_traslate_1)*cosf(-_angleBigFingerFloat);
    const float r_z_traslate_1 = -z_traslate_1;
    const vector_float3   cameraPosition = {0.0, -10.0, -230.0};
    const matrix_float4x4 cameraViewMatrix  = matrix4x4_translation(-cameraPosition);

    const vector_float3   templeRotationAxis_x      = {1, 0, 0};
    const vector_float3   templeRotationAxis_y      = {0, 1, 0};
    const vector_float3   templeRotationAxis_z      = {0, 0, 1};

    const matrix_float4x4 templeTranslationMatrix = matrix4x4_translation(x_traslate_1, y_traslate_1, z_traslate_1);
    const matrix_float4x4 rTranslationMatrix   = matrix4x4_translation(r_x_traslate_1, r_y_traslate_1, r_z_traslate_1);

    
    const matrix_float4x4 templeRotationMatrix_1   = matrix4x4_rotation (0, templeRotationAxis_x);  // 0.035    3°  //
    const matrix_float4x4 templeRotationMatrix_2   = matrix4x4_rotation (0.034, templeRotationAxis_y); //-0.052   -5°
    const matrix_float4x4 templeRotationMatrix_3   = matrix4x4_rotation (-_angleBigFingerFloat, templeRotationAxis_z);
    const matrix_float4x4 templeRotationMatrix_4   = matrix4x4_rotation (-0.034, templeRotationAxis_y);
    const matrix_float4x4 templeRotationMatrix_5   = matrix4x4_rotation (0, templeRotationAxis_x);
//    NSLog(@"_angleBigFingerFloat при отрисовке: %f", _angleBigFingerFloat);
//    NSLog(@"_angleBigFingerTransfer1 при отрисовке: %d", _angleBigFingerTransfer1);
    

    // Вторая фаланга
    // Вращение относительно середины пальца
    if (_selectedObject == 5) { _angleBigFingerFloat += _deltaY/10; }
    if((_angleBigFingerFloat < -1.029 || _angleBigFingerFloat > 0.5059)) { // 0.0174 - 1°   1.727 - 99°  -59° - -1.029   29° - 0.5059
        _angleBigFingerFloat -= _deltaY/10;
        _angleBigFingerTransfer1 = (int) (_angleBigFingerFloat/M_PI*180);
    }
    if((_angleBigFingerTransfer1 >= -60 && _angleBigFingerTransfer1 <= 30)) { //  -60° - -1.047  30° - 0.523
        matrix_float4x4 currentRotation_my = matrix_multiply(templeRotationMatrix_1, templeTranslationMatrix);
        currentRotation_my                 = matrix_multiply(templeRotationMatrix_2, currentRotation_my);
        currentRotation_my                 = matrix_multiply(templeRotationMatrix_3, currentRotation_my);
        currentRotation_my                 = matrix_multiply(templeRotationMatrix_4, currentRotation_my);
        _accumulateRotationBigFinger2      = matrix_multiply(templeRotationMatrix_5, currentRotation_my);
    }
    _angleBigFingerTransfer1 = (int) (_angleBigFingerFloat/M_PI*180);
    

    // Перемещение ко второй оси вращения
    matrix_float4x4 currentRotation_my2      = matrix_multiply(_accumulateRotationBigFinger, _accumulateRotationBigFinger2);
    // Перемещение в сборку
    currentRotation_my2                            = matrix_multiply(currentRotation_my2, rTranslationMatrix);
    // Вращение в составе всей сборки
    currentRotation_my2                            = matrix_multiply(_accumulateRotationGeneral, currentRotation_my2);
    // Отрисовка второй фаланги
    const matrix_float4x4 templeModelViewMatrix   = matrix_multiply (cameraViewMatrix, currentRotation_my2);
    const matrix_float3x3 templeNormalMatrix      = matrix3x3_upper_left(currentRotation_my2);

    _templeCameraMVPMatrix        = matrix_multiply(_projectionMatrix, templeModelViewMatrix);
    _templeNormalMatrix           = templeNormalMatrix;

    float packed3x3NormalMatrix[9] =
    {
        _templeNormalMatrix.columns[0].x,
        _templeNormalMatrix.columns[0].y,
        _templeNormalMatrix.columns[0].z,
        _templeNormalMatrix.columns[1].x,
        _templeNormalMatrix.columns[1].y,
        _templeNormalMatrix.columns[1].z,
        _templeNormalMatrix.columns[2].x,
        _templeNormalMatrix.columns[2].y,
        _templeNormalMatrix.columns[2].z,
    };
    glUniformMatrix3fv(_templeNormalMatrixUniformLocation, 1, GL_FALSE, packed3x3NormalMatrix);
    glUniformMatrix4fv(_templeMVPUniformLocation, 1, GL_FALSE, (const GLfloat*)&_templeCameraMVPMatrix);
    
    int sketchableParts[] = {7,1};
    for(GLuint i = 0; i < 2 ; i++)
    {
        glBindTexture(GL_TEXTURE_2D, _templeTextures[sketchableParts[i]]);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, _templeIndexBuffers[sketchableParts[i]]);
        glDrawElements(GL_TRIANGLES, _templeIndexBufferCounts[sketchableParts[i]], GL_UNSIGNED_INT, 0);
    }
    
    const float x_traslate_2 = 57.79;
    const float y_traslate_2 = 13.097;
    const float z_traslate_2 = 28.68;
    const float r_x_traslate_2 = - x_traslate_2;
    const float r_y_traslate_2 = - fabsf(z_traslate_2)*sinf(_angle_2_BigFingerFloat) - fabsf(y_traslate_2)*cosf(_angle_2_BigFingerFloat);
    const float r_z_traslate_2 = - fabsf(z_traslate_2)*cosf(_angle_2_BigFingerFloat) + fabsf(y_traslate_2)*sinf(_angle_2_BigFingerFloat);

    // Первая фаланга
    // Вращение относительно костяшки пальца
    const matrix_float4x4 templeTranslationMatrix_2 = matrix4x4_translation(x_traslate_2, y_traslate_2, z_traslate_2);
    const matrix_float4x4 rTranslationMatrix_2   = matrix4x4_translation(r_x_traslate_2, r_y_traslate_2, r_z_traslate_2);

    const matrix_float4x4 templeRotationMatrix2_2   = matrix4x4_rotation (_angle_2_BigFingerFloat, templeRotationAxis_x);

    if (_selectedObject == 5) {
        if (_handSide == 1) {
            _angle_2_BigFingerFloat += _deltaX/10;
        } else {
            _angle_2_BigFingerFloat -= _deltaX/10;
        }
    }
//    NSLog(@"_angle_2_BigFingerFloat = %d", ((int)(_angle_2_BigFingerFloat*100)));
    if((_angle_2_BigFingerFloat < 0 || _angle_2_BigFingerFloat > 1.58)) { // 0.0174 - 1°   1.553 - 89°  -59° - -1.029   29° - 0.5059
        if (_handSide == 1) {
            _angle_2_BigFingerFloat -= _deltaX/10;
        } else {
            _angle_2_BigFingerFloat += _deltaX/10;
        }
        _angleBigFingerTransfer2 = (int) (_angle_2_BigFingerFloat/M_PI*180);
    }
    matrix_float4x4 currentRotation3 = matrix4x4_identity();
    if((_angleBigFingerTransfer2 >= 0 && _angleBigFingerTransfer2 <= 90)) { //  -60° - -1.047  30° - 0.523
        currentRotation3   = matrix_multiply(templeRotationMatrix2_2, templeTranslationMatrix_2);
    }
    _angleBigFingerTransfer2 = (int) (_angle_2_BigFingerFloat/M_PI*180);
//    NSLog(@"Отправляемый угол  _angleBigFingerTransfer2: %d", _angleBigFingerTransfer2);
    
    // Перемещение в сборку
    _accumulateRotationBigFinger           = matrix_multiply(currentRotation3, rTranslationMatrix_2);
    // Вращение в составе всей сборки
    currentRotation3                       = matrix_multiply(_accumulateRotationGeneral, _accumulateRotationBigFinger);
    // Отрисовка первой фаланги
    const matrix_float4x4 templeModelViewMatrix2   = matrix_multiply (cameraViewMatrix, currentRotation3);
    const matrix_float3x3 templeNormalMatrix2      = matrix3x3_upper_left(currentRotation3);

    _templeCameraMVPMatrix        = matrix_multiply(_projectionMatrix, templeModelViewMatrix2);
    _templeNormalMatrix           = templeNormalMatrix2;

    float packed3x3NormalMatrix2[9] =
    {
        _templeNormalMatrix.columns[0].x,
        _templeNormalMatrix.columns[0].y,
        _templeNormalMatrix.columns[0].z,
        _templeNormalMatrix.columns[1].x,
        _templeNormalMatrix.columns[1].y,
        _templeNormalMatrix.columns[1].z,
        _templeNormalMatrix.columns[2].x,
        _templeNormalMatrix.columns[2].y,
        _templeNormalMatrix.columns[2].z,
    };
    glUniformMatrix3fv(_templeNormalMatrixUniformLocation, 1, GL_FALSE, packed3x3NormalMatrix2);
    glUniformMatrix4fv(_templeMVPUniformLocation, 1, GL_FALSE, (const GLfloat*)&_templeCameraMVPMatrix);
    
    sketchableParts[0] = 0;
    for(GLuint i = 0; i < 1 ; i++)
    {
        glBindTexture(GL_TEXTURE_2D, _templeTextures[sketchableParts[i]]);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, _templeIndexBuffers[sketchableParts[i]]);
        glDrawElements(GL_TRIANGLES, _templeIndexBufferCounts[sketchableParts[i]], GL_UNSIGNED_INT, 0);
    }
}
-(void) drawPalm {
    int sketchableParts[] = {14,2,9};
    for(GLuint i = 0; i < 3 ; i++)
    {
        glBindTexture(GL_TEXTURE_2D, _templeTextures[sketchableParts[i]]);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, _templeIndexBuffers[sketchableParts[i]]);
        glDrawElements(GL_TRIANGLES, _templeIndexBufferCounts[sketchableParts[i]], GL_UNSIGNED_INT, 0);
    }
}

-(int) selectObject {
    glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

    glUseProgram(_selectProgram);
    glUniformMatrix4fv(_templeMVPUniformLocation, 1, GL_FALSE, (const GLfloat*)&_templeCameraMVPMatrix);
    glBindVertexArray(_templeVAO);
    
    glUniform1f(_codeSelectUniformLocation, 250.0f);
    [self drawPalm];
    glUniform1f(_codeSelectUniformLocation,  0.40f);
    [self drawBigFinger];
    glUniform1f(_codeSelectUniformLocation,  0.04f);
    [self drawLittleFinger];
    glUniform1f(_codeSelectUniformLocation,  0.12f);
    [self drawRingFinger];
    glUniform1f(_codeSelectUniformLocation,  0.25f);
    [self drawMiddleFinger];
    glUniform1f(_codeSelectUniformLocation,  0.30f);
    [self drawForeFinger];
    
    
    GLint viewport[4];
    glGetIntegerv(GL_VIEWPORT, viewport);
    GLubyte *pixels = (GLubyte *)malloc(4);
    glReadPixels((GLint) _X*_xCoefficient, (GLint) (viewport[3]-_Y*_yCoefficient), 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
    
    return pixels[0];
}

matrix_float4x4 matrix_perspective_left_hand_gl(float fovyRadians, float aspect, float nearZ, float farZ) {
    float ys = 1 / tanf(fovyRadians * 0.5);
    float xs = ys / aspect;
    float zs = (farZ + nearZ) / (farZ - nearZ);
    float ws = -(2.f * farZ * nearZ) / (farZ - nearZ);

    return matrix_make_rows(xs,  0,  0,  0,
                             0, ys,  0,  0,
                             0,  0, zs, ws,
                             0,  0,  1,  0);
}

matrix_float4x4 matrix_perspective_right_hand_gl(float fovyRadians, float aspect, float nearZ, float farZ) {
    float ys = 1 / tanf(fovyRadians * 0.5);
    float xs = ys / aspect;
    float zs = (farZ + nearZ) / (farZ - nearZ);
    float ws = -(2.f * farZ * nearZ) / (farZ - nearZ);

    return matrix_make_rows(-xs,  0,  0,  0,
                             0, ys,  0,  0,
                             0,  0, zs, ws,
                             0,  0,  1,  0);
}
   
+ (GLuint) buildProgramWithVertexSourceURL:(NSURL*)vertexSourceURL
                    withFragmentSourceURL:(NSURL*)fragmentSourceURL
                               hasNormals:(BOOL)hasNormals
                          hasBaseColorMap:(BOOL)hasBaseColorMap
{
    
    NSLog(@"Линковка фрагментного и вершинного шейдеров к переменным");
    
    NSError *error;

    NSString *vertSourceString = [[NSString alloc] initWithContentsOfURL:vertexSourceURL
                                                                encoding:NSUTF8StringEncoding
                                                                   error:&error];

    NSAssert(vertSourceString, @"Could not load vertex shader source, error: %@.", error);

    NSString *fragSourceString = [[NSString alloc] initWithContentsOfURL:fragmentSourceURL
                                                                encoding:NSUTF8StringEncoding
                                                                   error:&error];

    NSAssert(fragSourceString, @"Could not load fragment shader source, error: %@.", error);

    // Prepend the #version definition to the vertex and fragment shaders.
    float  glLanguageVersion;

    sscanf((char *)glGetString(GL_SHADING_LANGUAGE_VERSION), "OpenGL ES GLSL ES %f", &glLanguageVersion);

    // `GL_SHADING_LANGUAGE_VERSION` returns the standard version form with decimals, but the
    //  GLSL version preprocessor directive simply uses integers (e.g. 1.10 should be 110 and 1.40
    //  should be 140). You multiply the floating point number by 100 to get a proper version number
    //  for the GLSL preprocessor directive.
    GLuint version = 100 * glLanguageVersion;

    NSString *versionString = [[NSString alloc] initWithFormat:@"#version %d", version];

    vertSourceString = [[NSString alloc] initWithFormat:@"%@\n%@", versionString, vertSourceString];
    fragSourceString = [[NSString alloc] initWithFormat:@"%@\n%@", versionString, fragSourceString];

    GLuint prgName;

    GLint logLength, status;

    // Create a program object.
    prgName = glCreateProgram();
    glBindAttribLocation(prgName, AAPLVertexAttributePosition, "inPosition");
    glBindAttribLocation(prgName, AAPLVertexAttributeTexcoord, "inTexcoord");

    if(hasNormals)
    {
        glBindAttribLocation(prgName, AAPLVertexAttributeNormal, "inNormal");
    }

    
    /*
     * Specify and compile a vertex shader.
     */
    GLchar *vertexSourceCString = (GLchar*)vertSourceString.UTF8String;
    GLuint vertexShader = glCreateShader(GL_VERTEX_SHADER);
    glShaderSource(vertexShader, 1, (const GLchar **)&(vertexSourceCString), NULL);
    glCompileShader(vertexShader);
    glGetShaderiv(vertexShader, GL_INFO_LOG_LENGTH, &logLength);

//    NSLog(@"Тут if который выполняется если logLenght > 0  logLenght=%d", logLength);
    if (logLength > 0)
    {
        GLchar *log = (GLchar*) malloc(logLength);
        glGetShaderInfoLog(vertexShader, logLength, &logLength, log);
        NSLog(@"Vertex shader compile log:\n%s.\n", log);
        free(log);
    }

    glGetShaderiv(vertexShader, GL_COMPILE_STATUS, &status);

    NSAssert(status, @"Failed to compile the vertex shader:\n%s.\n", vertexSourceCString);

    // Attach the vertex shader to the program.
    glAttachShader(prgName, vertexShader);

    // Delete the vertex shader because it's now attached to the program, which retains
    // a reference to it.
    glDeleteShader(vertexShader);

    /*
     * Specify and compile a fragment shader.
     */

    GLchar *fragSourceCString =  (GLchar*)fragSourceString.UTF8String;
    GLuint fragShader = glCreateShader(GL_FRAGMENT_SHADER);
    glShaderSource(fragShader, 1, (const GLchar **)&(fragSourceCString), NULL);
    glCompileShader(fragShader);
    glGetShaderiv(fragShader, GL_INFO_LOG_LENGTH, &logLength);
    if (logLength > 0)
    {
        GLchar *log = (GLchar*)malloc(logLength);
        glGetShaderInfoLog(fragShader, logLength, &logLength, log);
        NSLog(@"Fragment shader compile log:\n%s.\n", log);
        free(log);
    }

    glGetShaderiv(fragShader, GL_COMPILE_STATUS, &status);

    NSAssert(status, @"Failed to compile the fragment shader:\n%s.", fragSourceCString);

    // Attach the fragment shader to the program.
    glAttachShader(prgName, fragShader);

    // Delete the fragment shader because it's now attached to the program, which retains
    // a reference to it.
    glDeleteShader(fragShader);

    /*
     * Link the program.
     */

    glLinkProgram(prgName);
    glGetProgramiv(prgName, GL_INFO_LOG_LENGTH, &logLength);
    if (logLength > 0)
    {
        GLchar *log = (GLchar*)malloc(logLength);
        glGetProgramInfoLog(prgName, logLength, &logLength, log);
        NSLog(@"Program link log:\n%s.\n", log);
        free(log);
    }

    glGetProgramiv(prgName, GL_LINK_STATUS, &status);

    NSAssert(status, @"Failed to link program.");

    glGetProgramiv(prgName, GL_INFO_LOG_LENGTH, &logLength);
    if (logLength > 0)
    {
        GLchar *log = (GLchar*)malloc(logLength);
        glGetProgramInfoLog(prgName, logLength, &logLength, log);
        NSLog(@"Program validate log:\n%s.\n", log);
        free(log);
    }

    if(hasBaseColorMap)
    {
        GLint samplerLoc = glGetUniformLocation(prgName, "baseColorMap");
        NSAssert(samplerLoc >= 0, @"No uniform location found from `baseColorMap`.");
        glUseProgram(prgName);
        // Indicate that the diffuse texture will be bound to texture unit 0.
        glUniform1i(samplerLoc, AAPLTextureIndexBaseColor);
    } else {
        glUseProgram(prgName);
    }

    GetGLError();

//    NSLog(@"Скомпилированная программа: %u", prgName);
    return prgName;
}

- (void) beginTouchIvent { _selectedObject = [self selectObject]; }

- (void) touchIvent:(CGFloat) X  :(CGFloat) Y :(CGFloat) deltaX :(CGFloat) deltaY {
//    _deltaX = deltaX;
//    _deltaY = deltaY;
    _deltaX += deltaX;
    _deltaY += deltaY;
    _X = X;
    _Y = Y;
}

- (void) endTouchIvent {
    NSLog(@"Закончили возюкать пальцем по экрану");
    NSLog(@"Маленький палец: %d", _angleLittleFingerTransfer);
    if (_angleLittleFingerTransferOld != _angleLittleFingerTransfer) {
        NSLog(@"Обновили маленький палец: %d  старое: %d", _angleLittleFingerTransfer, _angleLittleFingerTransferOld);
        if (stateGesture == 0) {
            //изменение открытого состояния
            openStage1 = _angleLittleFingerTransfer;
//            if (_typeMultigribNewVM == 1) {
//                uint8_t data[]   = { openStage4,openStage3,openStage2,openStage1,openStage5,openStage6 };
////                [self sendDataToFest :data :sampleGattAtributes.MOVE_ALL_FINGERS_NEW_VM :sizeof(data)];
//            } else {
//                uint8_t data[]   = { openStage1,openStage2,openStage3,openStage4,openStage5,openStage6 };
////                [self sendDataToFest :data :sampleGattAtributes.MOVE_ALL_FINGERS_NEW :sizeof(data)];
//            }
        } else {
            //изменение закрытого состояния
            closeStage1 = _angleLittleFingerTransfer;
//            if (_typeMultigribNewVM == 1) {
//                uint8_t data[]   = { closeStage4,closeStage3,closeStage2,closeStage1,closeStage5,closeStage6 };
////                [self sendDataToFest :data :sampleGattAtributes.MOVE_ALL_FINGERS_NEW_VM :sizeof(data)];
//            } else {
//                uint8_t data[]   = { closeStage1,closeStage2,closeStage3,closeStage4,closeStage5,closeStage6 };
////                [self sendDataToFest :data :sampleGattAtributes.MOVE_ALL_FINGERS_NEW :sizeof(data)];
//            }
        }
//        if (_typeMultigribNewVM == 1) {
//            [self sendSaveComandFestX];
//        } else {
//            [self sendSaveComandFestH];
//        }
        _angleLittleFingerTransferOld = _angleLittleFingerTransfer;
    }
    NSLog(@"Безымянный палец: %d", _angleRingFingerTransfer);
    if (_angleRingFingerTransferOld != _angleRingFingerTransfer) {
        NSLog(@"Обновили безымянный палец: %d  старое: %d", _angleRingFingerTransfer, _angleRingFingerTransferOld);
        if (stateGesture == 0) {
            //изменение открытого состояния
            openStage2 = _angleRingFingerTransfer;
            
//            if (_typeMultigribNewVM == 1) {
//                uint8_t data[]   = { openStage4,openStage3,openStage2,openStage1,openStage5,openStage6 };
////                [self sendDataToFest :data :sampleGattAtributes.MOVE_ALL_FINGERS_NEW_VM :sizeof(data)];
//            } else {
//                uint8_t data[]   = { openStage1,openStage2,openStage3,openStage4,openStage5,openStage6 };
////                [self sendDataToFest :data :sampleGattAtributes.MOVE_ALL_FINGERS_NEW :sizeof(data)];
//            }
        } else {
            //изменение закрытого состояния
            closeStage2 = _angleRingFingerTransfer;
            
//            if (_typeMultigribNewVM == 1) {
//                uint8_t data[]   = { closeStage4,closeStage3,closeStage2,closeStage1,closeStage5,closeStage6 };
////                [self sendDataToFest :data :sampleGattAtributes.MOVE_ALL_FINGERS_NEW_VM :sizeof(data)];
//            } else {
//                uint8_t data[]   = { closeStage1,closeStage2,closeStage3,closeStage4,closeStage5,closeStage6 };
////                [self sendDataToFest :data :sampleGattAtributes.MOVE_ALL_FINGERS_NEW :sizeof(data)];
//            }
        }
//        if (_typeMultigribNewVM == 1) {
//            [self sendSaveComandFestX];
//        } else {
//            [self sendSaveComandFestH];
//        }
        _angleRingFingerTransferOld = _angleRingFingerTransfer;
    }
    NSLog(@"Средний палец: %d", _angleMiddleFingerTransfer);
    if (_angleMiddleFingerTransferOld != _angleMiddleFingerTransfer) {
        NSLog(@"Обновили средний палец: %d  старое: %d", _angleMiddleFingerTransfer, _angleMiddleFingerTransferOld);
        if (stateGesture == 0) {
            //изменение открытого состояния
            openStage3 = _angleMiddleFingerTransfer;
            
//            if (_typeMultigribNewVM == 1) {
//                uint8_t data[]   = { openStage4,openStage3,openStage2,openStage1,openStage5,openStage6 };
////                [self sendDataToFest :data :sampleGattAtributes.MOVE_ALL_FINGERS_NEW_VM :sizeof(data)];
//            } else {
//                uint8_t data[]   = { openStage1,openStage2,openStage3,openStage4,openStage5,openStage6 };
////                [self sendDataToFest :data :sampleGattAtributes.MOVE_ALL_FINGERS_NEW :sizeof(data)];
//            }
        } else {
            //изменение закрытого состояния
            closeStage3 = _angleMiddleFingerTransfer;
            
//            if (_typeMultigribNewVM == 1) {
//                uint8_t data[]   = { closeStage4,closeStage3,closeStage2,closeStage1,closeStage5,closeStage6 };
////                [self sendDataToFest :data :sampleGattAtributes.MOVE_ALL_FINGERS_NEW_VM :sizeof(data)];
//            } else {
//                uint8_t data[]   = { closeStage1,closeStage2,closeStage3,closeStage4,closeStage5,closeStage6 };
////                [self sendDataToFest :data :sampleGattAtributes.MOVE_ALL_FINGERS_NEW :sizeof(data)];
//            }
        }
//        if (_typeMultigribNewVM == 1) {
//            [self sendSaveComandFestX];
//        } else {
//            [self sendSaveComandFestH];
//        }
        _angleMiddleFingerTransferOld = _angleMiddleFingerTransfer;
    }
    NSLog(@"Указательный палец: %d", _angleForeFingerTransfer);
    if (_angleForeFingerTransferOld != _angleForeFingerTransfer) {
        NSLog(@"Обновили указательный палец: %d  старое: %d", _angleForeFingerTransfer, _angleForeFingerTransferOld);
        if (stateGesture == 0) {
            //изменение открытого состояния
            openStage4 = _angleForeFingerTransfer;
            
//            if (_typeMultigribNewVM == 1) {
//                uint8_t data[]   = { openStage4,openStage3,openStage2,openStage1,openStage5,openStage6 };
////                [self sendDataToFest :data :sampleGattAtributes.MOVE_ALL_FINGERS_NEW_VM :sizeof(data)];
//            } else {
//                uint8_t data[]   = { openStage1,openStage2,openStage3,openStage4,openStage5,openStage6 };
////                [self sendDataToFest :data :sampleGattAtributes.MOVE_ALL_FINGERS_NEW :sizeof(data)];
//            }
        } else {
            //изменение закрытого состояния
            closeStage4 = _angleForeFingerTransfer;
            
//            if (_typeMultigribNewVM == 1) {
//                uint8_t data[]   = { closeStage4,closeStage3,closeStage2,closeStage1,closeStage5,closeStage6 };
////                [self sendDataToFest :data :sampleGattAtributes.MOVE_ALL_FINGERS_NEW_VM :sizeof(data)];
//            } else {
//                uint8_t data[]   = { closeStage1,closeStage2,closeStage3,closeStage4,closeStage5,closeStage6 };
////                [self sendDataToFest :data :sampleGattAtributes.MOVE_ALL_FINGERS_NEW :sizeof(data)];
//            }
        }
//        if (_typeMultigribNewVM == 1) {
//            [self sendSaveComandFestX];
//        } else {
//            [self sendSaveComandFestH];
//        }
        _angleForeFingerTransferOld = _angleForeFingerTransfer;
    }
    NSLog(@"Большой палец: %d", (_angleBigFingerTransfer1+58));
    if (_angleBigFingerTransfer1Old != _angleBigFingerTransfer1) {
        NSLog(@"Обновили большой палец: %d  старое: %d", (100-(int)((_angleBigFingerTransfer1+58)*1.0f/86*100)), (100-(int)((_angleBigFingerTransfer1Old+58)*1.0f/86*100)));
        if (stateGesture == 0) {
            //изменение открытого состояния
            openStage5 = (100-(int)((_angleBigFingerTransfer1+58)*1.0f/86*100));
            
//            if (_typeMultigribNewVM == 1) {
//                uint8_t data[]   = { openStage4,openStage3,openStage2,openStage1,openStage5,openStage6 };
////                [self sendDataToFest :data :sampleGattAtributes.MOVE_ALL_FINGERS_NEW_VM :sizeof(data)];
//            } else {
//                uint8_t data[]   = { openStage1,openStage2,openStage3,openStage4,openStage5,openStage6 };
////                [self sendDataToFest :data :sampleGattAtributes.MOVE_ALL_FINGERS_NEW :sizeof(data)];
//            }
        } else {
            //изменение закрытого состояния
            closeStage5 = (100-(int)((_angleBigFingerTransfer1+58)*1.0f/86*100));
            
//            if (_typeMultigribNewVM == 1) {
//                uint8_t data[]   = { closeStage4,closeStage3,closeStage2,closeStage1,closeStage5,closeStage6 };
////                [self sendDataToFest :data :sampleGattAtributes.MOVE_ALL_FINGERS_NEW_VM :sizeof(data)];
//            } else {
//                uint8_t data[]   = { closeStage1,closeStage2,closeStage3,closeStage4,closeStage5,closeStage6 };
////                [self sendDataToFest :data :sampleGattAtributes.MOVE_ALL_FINGERS_NEW :sizeof(data)];
//            }
        }
//        if (_typeMultigribNewVM == 1) {
//            [self sendSaveComandFestX];
//        } else {
//            [self sendSaveComandFestH];
//        }
        _angleBigFingerTransfer1Old = _angleBigFingerTransfer1;
    }
    NSLog(@"Ротация палец: %d", _angleBigFingerTransfer2);
    if (_angleBigFingerTransfer2Old != _angleBigFingerTransfer2) {
        NSLog(@"Обновили ротацию пальца: %d  старое: %d", (int)(_angleBigFingerTransfer2*1.0f/90*100), (int)(_angleBigFingerTransfer2Old*1.0f/90*100));
        if (stateGesture == 0) {
            //изменение открытого состояния
            openStage6 = (int)(_angleBigFingerTransfer2*1.0f/90*100);
            
//            if (_typeMultigribNewVM == 1) {
//                uint8_t data[]   = { openStage4,openStage3,openStage2,openStage1,openStage5,openStage6 };
////                [self sendDataToFest :data :sampleGattAtributes.MOVE_ALL_FINGERS_NEW_VM :sizeof(data)];
//            } else {
//                uint8_t data[]   = { openStage1,openStage2,openStage3,openStage4,openStage5,openStage6 };
////                [self sendDataToFest :data :sampleGattAtributes.MOVE_ALL_FINGERS_NEW :sizeof(data)];
//            }
        } else {
            //изменение закрытого состояния
            closeStage6 = (int)(_angleBigFingerTransfer2*1.0f/90*100);
            
//            if (_typeMultigribNewVM == 1) {
//                uint8_t data[]   = { closeStage4,closeStage3,closeStage2,closeStage1,closeStage5,closeStage6 };
////                [self sendDataToFest :data :sampleGattAtributes.MOVE_ALL_FINGERS_NEW_VM :sizeof(data)];
//            } else {
//                uint8_t data[]   = { closeStage1,closeStage2,closeStage3,closeStage4,closeStage5,closeStage6 };
////                [self sendDataToFest :data :sampleGattAtributes.MOVE_ALL_FINGERS_NEW :sizeof(data)];
//            }
        }
//        if (_typeMultigribNewVM == 1) {
//            [self sendSaveComandFestX];
//        } else {
//            [self sendSaveComandFestH];
//        }
        _angleBigFingerTransfer2Old = _angleBigFingerTransfer2;
    }
}

- (void) sendGestureNumberFestX {
    uint8_t data2[]   = { (_gestureNumber-1)};
//    [self sendDataToFest :data2 :sampleGattAtributes.CHANGE_GESTURE_NEW_VM :sizeof(data2)];
    NSLog(@"");
}
- (void) sendSaveComandFestX {
    uint8_t data2[]   = { (_gestureNumber-1),openStage4,openStage3,openStage2,openStage1,openStage5,openStage6,
                            closeStage4,closeStage3,closeStage2,closeStage1,closeStage5,closeStage6};
//    [self sendDataToFest :data2 :sampleGattAtributes.CHANGE_GESTURE_NEW_VM :sizeof(data2)];
}
- (void) sendSaveComandFestH {
    uint8_t data2[]   = { (_gestureNumber-1),openStage1,openStage2,openStage3,openStage4,openStage5,openStage6,
                                        closeStage1,closeStage2,closeStage3,closeStage4,closeStage5,closeStage6 };
//    [self sendDataToFest :data2 :sampleGattAtributes.CHANGE_GESTURE_NEW :sizeof(data2)];
}


- (void) stopVC {
    NSLog(@"Переход назад 2");
    [self saveAllData];
    [self deallocAll];
}
- (void) savesAllData {
    [self saveAllData];
}
//Функция для очистки памяти, выделенной под отрисовку
- (void) deallocAll {
    glDeleteProgram(_templeProgram);

    glDeleteVertexArrays(1, &_templeVAO);

    glDeleteBuffers(1, &_templeVertexPositions);
    glDeleteBuffers(1, &_templeVertexGenerics);

    for(int i = 0; i < _numTempleSubmeshes; i++)
    {
        glDeleteTextures(1, &_templeTextures[i]);
        glDeleteBuffers(1, &_templeIndexBuffers[i]);
    }

    free(_templeIndexBufferCounts);
    free(_templeIndexBuffers);
    free(_templeTextures);
}

- (void) calculationOfCoefficients:(CGFloat) width  :(CGFloat) height {
    _xCoefficient = _viewSize.width/width;
    _yCoefficient = _viewSize.height/height;
}

- (void) changeState :(BOOL) state {
    NSLog(@"changeState tup %d", state);
    self->stateGesture = state;
    if (state == 1) {
        //код перехода в закрытое состояние
        _angleForeFingerFloat = (closeStage4*1.0f/100*97.9)/180*M_PI+0.0175;                    //  <0.0174 _angleForeFingerFloat   >1.727
        _angleMiddleFingerFloat = (closeStage3*1.0f/100*97.9)/180*M_PI+0.0175;                  //  <0.0174 _angleMiddleFingerFloat >1.727
        _angleRingFingerFloat = (closeStage2*1.0f/100*97.9)/180*M_PI+0.0175;                    //  <0.0174 _angleRingFingerFloat   >1.727
        _angleLittleFingerFloat = (closeStage1*1.0f/100*97.9)/180*M_PI+0.0175;                  //  <0.0174 _angleLittleFingerFloat >1.727  1,7096  98-диапазон в градусах
        _angleBigFingerFloat = ((100-closeStage5)*1.0f/100*87)/180*M_PI-1.028;                        //  <-1.029 _angleBigFingerFloat    >0.5059 1,5396  88-диапазон в градусах
        _angle_2_BigFingerFloat = (closeStage6*1.0f/100*90)/180*M_PI;                           //  <0      _angle_2_BigFingerFloat >1.58   1,58    90-диапазон в градусах
        [self checkingAnglesForValidValues];

        _angleForeFingerTransferOld     = (int) (_angleForeFingerFloat/M_PI*180);
        _angleMiddleFingerTransferOld   = (int) (_angleMiddleFingerFloat/M_PI*180);
        _angleRingFingerTransferOld     = (int) (_angleRingFingerFloat/M_PI*180);
        _angleLittleFingerTransferOld   = (int) (_angleLittleFingerFloat/M_PI*180);
        _angleBigFingerTransfer1Old     = (int) (_angleBigFingerFloat/M_PI*180);
        _angleBigFingerTransfer2Old     = (int) (_angle_2_BigFingerFloat/M_PI*180);
        
        
//        if (_typeMultigribNewVM == 1) {
//            uint8_t data[]   = { closeStage4,closeStage3,closeStage2,closeStage1,closeStage5,closeStage6 };
////            [self sendDataToFest :data :sampleGattAtributes.MOVE_ALL_FINGERS_NEW_VM :sizeof(data)];
//        } else {
//            uint8_t data[]   = { closeStage1,closeStage2,closeStage3,closeStage4,closeStage5,closeStage6 };
////            [self sendDataToFest :data :sampleGattAtributes.MOVE_ALL_FINGERS_NEW :sizeof(data)];
//        }
        
        
        [self saveStateData :@"1"];
    } else {
        //код перехода в открытое состояние
        _angleForeFingerFloat = (openStage4*1.0f/100*97.9)/180*M_PI+0.0175;                    //  <0.0174 _angleForeFingerFloat   >1.727
        _angleMiddleFingerFloat = (openStage3*1.0f/100*97.9)/180*M_PI+0.0175;                  //  <0.0174 _angleMiddleFingerFloat >1.727
        _angleRingFingerFloat = (openStage2*1.0f/100*97.9)/180*M_PI+0.0175;                    //  <0.0174 _angleRingFingerFloat   >1.727
        _angleLittleFingerFloat = (openStage1*1.0f/100*97.9)/180*M_PI+0.0175;                  //  <0.0174 _angleLittleFingerFloat >1.727  1,7096  98-диапазон в градусах
        _angleBigFingerFloat = ((100-openStage5)*1.0f/100*87)/180*M_PI-1.028;                        //  <-1.029 _angleBigFingerFloat    >0.5059 1,5396  88-диапазон в градусах
        _angle_2_BigFingerFloat = (openStage6*1.0f/100*90)/180*M_PI;                           //  <0      _angle_2_BigFingerFloat >1.58   1,58    90-диапазон в градусах
        [self checkingAnglesForValidValues];

        _angleForeFingerTransferOld     = (int) (_angleForeFingerFloat/M_PI*180);
        _angleMiddleFingerTransferOld   = (int) (_angleMiddleFingerFloat/M_PI*180);
        _angleRingFingerTransferOld     = (int) (_angleRingFingerFloat/M_PI*180);
        _angleLittleFingerTransferOld   = (int) (_angleLittleFingerFloat/M_PI*180);
        _angleBigFingerTransfer1Old     = (int) (_angleBigFingerFloat/M_PI*180);
        _angleBigFingerTransfer2Old     = (int) (_angle_2_BigFingerFloat/M_PI*180);
        
        
//        if (_typeMultigribNewVM == 1) {
//            uint8_t data[]   = { openStage4,openStage3,openStage2,openStage1,openStage5,openStage6 };
//            [self sendDataToFest :data :sampleGattAtributes.MOVE_ALL_FINGERS_NEW_VM :sizeof(data)];
//        } else {
//            uint8_t data[]   = { openStage1,openStage2,openStage3,openStage4,openStage5,openStage6 };
//            [self sendDataToFest :data :sampleGattAtributes.MOVE_ALL_FINGERS_NEW :sizeof(data)];
//        }
        
        
        [self saveStateData :@"0"];
    }
}

- (void) checkingAnglesForValidValues {
    if(_angleForeFingerFloat < 0.0174) {
        NSLog(@"пофиксили _angleForeFingerFloat было: %f  стало: %f", _angleForeFingerFloat, 0.02);
        _angleForeFingerFloat = 0.02;
        
    }
    if(_angleForeFingerFloat > 1.727) {
        NSLog(@"пофиксили _angleForeFingerFloat было: %f  стало: %f", _angleForeFingerFloat, 1.7);
        _angleForeFingerFloat = 1.7;
        
    }
    if(_angleMiddleFingerFloat < 0.0174) {
        NSLog(@"пофиксили _angleMiddleFingerFloat было: %f  стало: %f", _angleMiddleFingerFloat, 0.02);
        _angleMiddleFingerFloat = 0.02;
    }
    if(_angleMiddleFingerFloat > 1.727) {
        NSLog(@"пофиксили _angleMiddleFingerFloat было: %f  стало: %f", _angleMiddleFingerFloat, 1.7);
        _angleMiddleFingerFloat = 1.7;
    }
    if(_angleRingFingerFloat < 0.0174) {
        NSLog(@"пофиксили _angleRingFingerFloat было: %f  стало: %f", _angleRingFingerFloat, 0.02);
        _angleRingFingerFloat = 0.02;
    }
    if(_angleRingFingerFloat > 1.727) {
        NSLog(@"пофиксили _angleRingFingerFloat было: %f  стало: %f", _angleRingFingerFloat, 1.7);
        _angleRingFingerFloat = 1.7;
    }
    if(_angleLittleFingerFloat < 0.0174) {
        NSLog(@"пофиксили _angleLittleFingerFloat было: %f  стало: %f", _angleLittleFingerFloat, 0.02);
        _angleLittleFingerFloat = 0.02;
    }
    if(_angleLittleFingerFloat > 1.727) {
        NSLog(@"пофиксили _angleLittleFingerFloat было: %f  стало: %f", _angleLittleFingerFloat, 1.7);
        _angleLittleFingerFloat = 1.7;
    }
    if(_angleBigFingerFloat < -1.029) {
        NSLog(@"пофиксили _angleBigFingerFloat было: %f  стало: %f", _angleBigFingerFloat, -1.0);
        _angleBigFingerFloat = -1.0;
    }
    if(_angleBigFingerFloat > 0.5059) {
        NSLog(@"пофиксили _angleBigFingerFloat было: %f  стало: %f", _angleBigFingerFloat, 0.5);
        _angleBigFingerFloat = 0.5;
    }
    if(_angle_2_BigFingerFloat < 0.0174) {
        NSLog(@"пофиксили _angle_2_BigFingerFloat было: %f  стало: %f", _angle_2_BigFingerFloat, 0.02);
        _angle_2_BigFingerFloat = 0.02;
    }
    if(_angle_2_BigFingerFloat > 1.727) {
        NSLog(@"пофиксили _angle_2_BigFingerFloat было: %f  стало: %f", _angle_2_BigFingerFloat, 1.7);
        _angle_2_BigFingerFloat = 1.7;
    }
}

- (void) animateChangeState{
    if (stateGesture == 1) {
        //закрываем
        if (_directionForeFinger) {
            _angleForeFingerFloat += 0.001;
            if (_angleForeFingerTransfer >= closeStage4) {
                [self->timer invalidate];
                self->timer = nil;
                _angleForeFingerTransferOld     = (int) (_angleForeFingerFloat/M_PI*180);
            }
        } else {
            _angleForeFingerFloat -= 0.001;
            if (_angleForeFingerTransfer <= closeStage4) {
                [self->timer invalidate];
                self->timer = nil;
                _angleForeFingerTransferOld     = (int) (_angleForeFingerFloat/M_PI*180);
            }
        }
    } else {
        //открываем
        if (_directionForeFinger) {
            _angleForeFingerFloat += 0.001;
            if (_angleForeFingerTransfer >= openStage4) {
                [self->timer invalidate];
                self->timer = nil;
                _angleForeFingerTransferOld     = (int) (_angleForeFingerFloat/M_PI*180);
            }
        } else {
            _angleForeFingerFloat -= 0.001;
            if (_angleForeFingerTransfer <= openStage4) {
                [self->timer invalidate];
                self->timer = nil;
                _angleForeFingerTransferOld     = (int) (_angleForeFingerFloat/M_PI*180);
            }
        }        
    }
}

- (void) sendDataToFest :(uint8_t*) dataForWrite :(NSString*) characteristic  :(NSInteger) lenght {
    NSData *nsdataObj = [NSData dataWithBytes:dataForWrite length:lenght];
    [gestureService sendDataToFestWithDataForWrite:nsdataObj characteristic:characteristic typeFestX:true];
}

- (void) saveAllData {
//        _gestureTableBig[12*(_gestureNumber-2)+0] = openStage4;
//        _gestureTableBig[12*(_gestureNumber-2)+1] = openStage3;
//        _gestureTableBig[12*(_gestureNumber-2)+2] = openStage2;
//        _gestureTableBig[12*(_gestureNumber-2)+3] = openStage1;
//        _gestureTableBig[12*(_gestureNumber-2)+4] = openStage5;
//        _gestureTableBig[12*(_gestureNumber-2)+5] = openStage6;
//        
//        _gestureTableBig[12*(_gestureNumber-2)+6] = closeStage4;
//        _gestureTableBig[12*(_gestureNumber-2)+7] = closeStage3;
//        _gestureTableBig[12*(_gestureNumber-2)+8] = closeStage2;
//        _gestureTableBig[12*(_gestureNumber-2)+9] = closeStage1;
//        _gestureTableBig[12*(_gestureNumber-2)+10] = closeStage5;
//        _gestureTableBig[12*(_gestureNumber-2)+11] = closeStage6;
        
//        NSString *dataStrBig = @"";
//            for (int i = 0; i <= 159; i++) {
//                dataStrBig = [dataStrBig stringByAppendingString:([@(_gestureTableBig[i]) stringValue])];
//                dataStrBig = [dataStrBig stringByAppendingString:(@" ")];
//            }
//        [gestureVC saveDataStringWithKey:sampleGattAtributes.ADD_GESTURE_NEW_BIG value:dataStrBig];
}
- (void) saveStateData :(NSString*) dataForWrite {
    NSLog(@"changeState saveStateData: %@", dataForWrite);
}
- (void) updateGestureSettings:(SharedParameterRef *)parameterRef
                 parameterData:(NSString *)parameterData {
    if (parameterRef == nil) {
        return;
    }
    _gestureSettingsData = parameterRef.dataCode;
    _gestureSettingsParameterID = parameterRef.parameterID;
    _gestureSettingsParameterData = parameterData;
    [gestureService decodeGestureSettingsWithRaw:_gestureSettingsParameterData];
    SharedGesture *gestureSettings = [gestureService getLatestGestureSettings];
    NSLog(@"GestureSettings update from AAPLOpenGLRenderer: data = %@  openPosition1 = %d",
          _gestureSettingsParameterData,
          gestureSettings.openPosition1);
    (long)parameterRef.dataCode;
    (long)parameterRef.parameterID;
    [gestureService getDeviceName];
}
@end
