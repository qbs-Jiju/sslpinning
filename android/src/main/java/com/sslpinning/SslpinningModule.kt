package com.sslpinning

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap

class SslpinningModule internal constructor(context: ReactApplicationContext) :
  SslpinningSpec(context) {

  private var sslPinning:RNSslPinningImpl = RNSslPinningImpl(context)
  private val mContext = context

  override fun getName(): String {
    return RNSslPinningImpl.NAME
  }

  @ReactMethod
  override fun fetch(url: String, options: ReadableMap,promise: Promise) {
      sslPinning.fetch(url,options,promise)
  }

  @ReactMethod
  override fun getCookies(domain: String, promise: Promise) {
    sslPinning.getCookies(domain,promise)
  }

  @ReactMethod
  override fun removeCookieByName(cookieName: String, promise: Promise) {
    sslPinning.removeCookieByName(cookieName,promise)
  }

}
