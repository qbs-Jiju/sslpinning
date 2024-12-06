package com.sslpinning

import com.facebook.react.TurboReactPackage
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.NativeModule
import com.facebook.react.module.model.ReactModuleInfoProvider
import com.facebook.react.module.model.ReactModuleInfo
import java.util.HashMap

class SslpinningPackage : TurboReactPackage() {
  override fun getModule(name: String, reactContext: ReactApplicationContext): NativeModule? =
    if (name == RNSslPinningImpl.NAME) SslpinningModule(reactContext) else null

  override fun getReactModuleInfoProvider(): ReactModuleInfoProvider {
    return ReactModuleInfoProvider {
      val moduleInfos: MutableMap<String, ReactModuleInfo> = HashMap()
      val isTurboModule: Boolean = BuildConfig.IS_NEW_ARCHITECTURE_ENABLED
      moduleInfos[RNSslPinningImpl.NAME] = ReactModuleInfo(
        RNSslPinningImpl.NAME,
        RNSslPinningImpl.NAME,
        false,  // canOverrideExistingModule
        false,  // needsEagerInit
//        true,  // hasConstants --depreacted
        false,  // isCxxModule
        isTurboModule // isTurboModule
      )
      moduleInfos
    }
  }
}
