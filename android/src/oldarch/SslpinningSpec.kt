package com.sslpinning

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.Promise
import android.util.Log
import com.facebook.react.bridge.ReadableMap

 abstract class SslpinningSpec internal constructor(context: ReactApplicationContext) :
   ReactContextBaseJavaModule(context) {

   abstract fun fetch(url: String, options: ReadableMap, promise: Promise)
   abstract fun getCookies(domain: String, promise: Promise)
   abstract fun removeCookieByName(cookieName: String, promise: Promise)
   
 }
