#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

// Matches Swift's generated Objective-C runtime names for the OldMotoricaStart framework target.
#ifndef SWIFT_CLASS
#if __has_attribute(objc_runtime_name)
#define SWIFT_CLASS(_name) __attribute__((objc_runtime_name(_name)))
#else
#define SWIFT_CLASS(_name)
#endif
#endif

SWIFT_CLASS("_TtC16OldMotoricaStart20SampleGattAttributes")
@interface SampleGattAttributes : NSObject
@property (nonatomic, readonly, copy) NSString *ADD_GESTURE_NEW;
@property (nonatomic, readonly, copy) NSString *ADD_GESTURE_NEW_BIG;
@property (nonatomic, readonly, copy) NSString *CHANGE_GESTURE_NEW;
@property (nonatomic, readonly, copy) NSString *CHANGE_GESTURE_NEW_VM;
@property (nonatomic, readonly, copy) NSString *MOVE_ALL_FINGERS_NEW;
@property (nonatomic, readonly, copy) NSString *MOVE_ALL_FINGERS_NEW_VM;
@property (nonatomic, readonly, copy) NSString *SENS_ENABLED_NEW;
@property (nonatomic, readonly, copy) NSString *SENS_ENABLED_NEW_VM;
@property (nonatomic, readonly, copy) NSString *STATE_GESTURE;
@end

SWIFT_CLASS("_TtC16OldMotoricaStart29GestureSettingsViewController")
@interface GestureSettingsViewController : UIViewController
@property (nonatomic, copy) NSString *savingDeviceName;
- (NSInteger)getStatusConnection;
- (NSInteger)getGestureNum;
- (NSString *)getGestureNameWithNumberGesture:(NSInteger)numberGesture;
- (NSString *)getGestureTable;
- (NSString *)getGestureTableBig;
- (NSInteger)getUseFestX;
- (NSInteger)getHandSide;
- (NSInteger)getFingersDelaySwitch;
- (BOOL)getVersionDriverGreaterThan237;
- (void)setNameGestureWithNumberGesture:(NSInteger)numberGesture name:(NSString *)name;
- (void)sendDataToFestWithDataForWrite:(NSData *)dataForWrite
                        characteristic:(NSString *)characteristic
                             typeFestX:(BOOL)typeFestX;
- (void)saveDataStringWithKey:(NSString *)key value:(NSString *)value;
@end

NS_ASSUME_NONNULL_END
