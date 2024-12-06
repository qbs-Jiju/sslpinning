package com.sslpinning

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.Promise
import android.util.Log
import com.facebook.react.bridge.ReadableMap

 public class SslpinningSpec internal constructor(context: ReactApplicationContext) :
   ReactContextBaseJavaModule(context) {

   private  val TAG = "SslpinningSpec"
   private var sslPinning: RNSslPinningImpl = RNSslPinningImpl(context)
   private val mContext = context

   @ReactMethod
   public fun multiply(a: Double, b: Double, promise: Promise) {
     promise.resolve(a * b)
   }

   @ReactMethod
   public fun showToast(message: String, promise: Promise) {
     if (message.contains("err"))
       promise.reject(Throwable(message))
     else promise.resolve(message)
     Toast.makeText(mContext, message, Toast.LENGTH_LONG).show()
   }

   @ReactMethod
   public fun fetch(url: String, options: ReadableMap, promise: Promise) {
     Log.d(TAG, "fetch: url: $url, options: $options ")
     sslPinning.fetch(url, options, promise)
   }

   @ReactMethod
   public fun getCookies(domain: String, promise: Promise) {
     sslPinning.getCookies(domain, promise)
   }

   @ReactMethod
   public fun removeCookieByName(cookieName: String, promise: Promise) {
     sslPinning.removeCookieByName(cookieName, promise)
   }

   companion object {
     const val NAME = "Sslpinning"
   }

   override fun getName(): String {
     return NAME
   }

 }
