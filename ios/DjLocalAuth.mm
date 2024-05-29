#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(DjLocalAuth, NSObject)


RCT_EXTERN_METHOD(authenticate:(NSDictionary *)options
                 withResolver:(RCTPromiseResolveBlock)resolve
                  withRejecter:(RCTPromiseRejectBlock)reject);

+ (BOOL)requiresMainQueueSetup
{
  return NO;
}

@end
