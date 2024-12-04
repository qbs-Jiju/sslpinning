
#ifdef RCT_NEW_ARCH_ENABLED
#import "RNSslpinningSpec.h"

@interface Sslpinning : NSObject <NativeSslpinningSpec>
#else
#import <React/RCTBridgeModule.h>

@interface Sslpinning : NSObject <RCTBridgeModule>
#endif

@end
