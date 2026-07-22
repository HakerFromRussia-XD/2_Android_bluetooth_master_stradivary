#import <Foundation/Foundation.h>
#import <OpenGLES/EAGL.h>

NS_ASSUME_NONNULL_BEGIN

typedef NS_ENUM(NSInteger, V3ModelResourceCacheState) {
    V3ModelResourceCacheStateIdle,
    V3ModelResourceCacheStateLoading,
    V3ModelResourceCacheStateReady,
    V3ModelResourceCacheStateFailed,
};

FOUNDATION_EXPORT NSNotificationName const V3ModelResourceCacheDidBecomeReadyNotification;
FOUNDATION_EXPORT NSNotificationName const V3ModelResourceCacheDidFailNotification;
FOUNDATION_EXPORT NSNotificationName const V3ModelFirstFramePresentedNotification;

@interface V3ModelResourceCache : NSObject

@property (atomic, readonly) V3ModelResourceCacheState state;
@property (atomic, readonly, getter=isReady) BOOL ready;
@property (atomic, readonly, nullable) NSError *lastError;
@property (atomic, readonly, nullable) EAGLSharegroup *sharegroup;
@property (atomic, readonly, copy) NSDictionary<NSString *, NSNumber *> *latestMetrics;

+ (instancetype)sharedCache NS_SWIFT_NAME(shared());

- (void)preloadWithCompletion:(void (^ _Nullable)(BOOL ready, NSError * _Nullable error))completion
    NS_SWIFT_NAME(preload(completion:));
- (nullable EAGLContext *)newSharedContext;
- (void)mark3DOpenRequested;
- (void)recordFirstPresentedFrame;
- (void)recordFrameDurationMilliseconds:(double)milliseconds;

// Kept public so corrupt/truncated fixtures can be tested without creating GL resources.
+ (BOOL)validatePartsBundleData:(NSData *)data error:(NSError **)error;
+ (BOOL)validateModelPartData:(NSData *)data error:(NSError **)error;
+ (BOOL)validateDeformationData:(NSData *)data
            expectedVertexCount:(NSInteger)expectedVertexCount
              requiresCenterline:(BOOL)requiresCenterline
                           error:(NSError **)error;

#if DEBUG
- (NSDictionary<NSString *, NSArray<NSString *> *> *)resolvedGroupPartIDsForTesting;
- (NSDictionary<NSString *, NSDictionary<NSString *, NSNumber *> *> *)deformationDiagnosticsForTesting;
- (void)resetForTesting;
#endif

@end

NS_ASSUME_NONNULL_END
