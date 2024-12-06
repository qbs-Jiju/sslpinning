package com.sslpinning

import android.os.Build
import android.util.Base64
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.WritableNativeMap
import com.facebook.react.modules.network.ForwardingCookieHandler
import com.sslpinning.Utils.OkHttpUtils
import okhttp3.Call
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException


class RNSslPinningImpl(private var reactContext: ReactApplicationContext) {
  companion object{
    val NAME: String = "SslPinning"
  }

  private var cookieStore: HashMap<String, MutableList<Cookie>>
  private lateinit var cookieJar: CookieJar
  private var cookieHandler: ForwardingCookieHandler? = null

  init {
    cookieStore = HashMap()
    cookieHandler = ForwardingCookieHandler(reactContext)
    setupCookieJar()
  }

  private fun setupCookieJar() {
    cookieJar = object : CookieJar {
      @Synchronized
      override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        for (cookie in cookies) {
          setCookie(url, cookie)
        }
      }

      @Synchronized
      override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookies: List<Cookie>? = cookieStore[url.host]
        return cookies ?: ArrayList()
      }

      fun setCookie(url: HttpUrl, cookie: Cookie) {
        val host = url.host
        var cookieListForUrl = cookieStore[host]
        if (cookieListForUrl == null) {
          cookieListForUrl = ArrayList()
          cookieStore[host] = cookieListForUrl
        }
        try {
          putCookie(url, cookieListForUrl, cookie)
        } catch (e: Exception) {
          e.printStackTrace()
        }
      }

      @Throws(URISyntaxException::class, IOException::class)
      fun putCookie(url: HttpUrl, storedCookieList: MutableList<Cookie>, newCookie: Cookie) {
        var oldCookie: Cookie? = null
        val cookieMap: MutableMap<String, List<String>> = HashMap()
        for (storedCookie in storedCookieList) {
          val oldCookieKey = storedCookie.name + storedCookie.path
          val newCookieKey = newCookie.name + newCookie.path
          if (oldCookieKey == newCookieKey) {
            oldCookie = storedCookie
            break
          }
        }
        if (oldCookie != null) {
          storedCookieList.remove(oldCookie)
        }
        storedCookieList.add(newCookie)
        cookieMap["Set-cookie"] = listOf(newCookie.toString())
        cookieHandler!!.put(url.toUri(), cookieMap)
      }
    }
  }

  fun getCookies(domain: String, promise: Promise) {
    try {
      val map: WritableMap = WritableNativeMap()
      val cookies: List<Cookie>? = cookieStore[getDomainName(domain)]
      if (cookies != null) {
        for (cookie in cookies) {
          map.putString(cookie.name, cookie.value)
        }
      }
      promise.resolve(map)
    } catch (e: Exception) {
      promise.reject(e)
    }
  }

  fun removeCookieByName(cookieName: String, promise: Promise) {
    for (domain in cookieStore.keys) {
      val newCookiesList: MutableList<Cookie> = ArrayList()
      val cookies: List<Cookie>? = cookieStore[domain]
      if (cookies != null) {
        for (cookie in cookies) {
          if (cookie.name != cookieName) {
            newCookiesList.add(cookie)
          }
        }
        cookieStore[domain] = newCookiesList
      }
    }
    promise.resolve(null)
  }

  fun fetch(hostname: String, options: ReadableMap, promise: Promise) {
    lateinit var client:OkHttpClient
    val writableMap = Arguments.createMap()
    val domainName = try {
      getDomainName(hostname)
    } catch (e: URISyntaxException) {
      hostname
    }

    if (options.hasKey("disableAllSecurity") && options.getBoolean("disableAllSecurity")) {
      client = OkHttpUtils.buildDefaultOHttpClient(cookieJar, domainName, options)
    } else if (options.hasKey("sslPinning")) {
      if (options.getMap("sslPinning")!!.hasKey("certs")) {
        val certs = options.getMap("sslPinning")!!.getArray("certs")
        if (certs != null && certs.size() == 0) {
          throw RuntimeException("certs array is empty")
        }
        client = OkHttpUtils.buildOkHttpClient(cookieJar, domainName, certs!!, options)
      } else {
        promise.reject(Throwable("key certs was not found"))
        return
      }
    } else {
      promise.reject(Throwable("sslPinning key was not added"))
      return
    }

    try {
      val request: Request = OkHttpUtils.buildRequest(reactContext, options, hostname)
      client.newCall(request).enqueue(object : okhttp3.Callback {
        override fun onFailure(call: Call, e: IOException) {
          promise.reject(Throwable(e.message))
        }

        @Throws(IOException::class)
        override fun onResponse(call: Call, response: Response) {
          val bytes = response.body?.bytes()
          val stringResponse = String(bytes!!, charset("UTF-8"))
          val headers = buildResponseHeaders(response)
          writableMap.putInt("status", response.code)
          val responseType =
            if (options.hasKey("responseType")) options.getString("responseType") else ""
          if ("base64" == responseType) {
            val base64 = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) Base64.encodeToString(
              bytes,
              Base64.DEFAULT
            ) else java.util.Base64.getEncoder().encodeToString(bytes)
            writableMap.putString("data", base64)
          } else {
            writableMap.putString("bodyString", stringResponse)
          }
          writableMap.putMap("headers", headers)
          if (response.isSuccessful) {
            promise.resolve(writableMap)
          } else {
            promise.reject(Throwable(response.message),writableMap)
          }
        }
      })
    } catch (e: JSONException) {
      promise.reject(Throwable(e))
    }
  }

  private fun buildResponseHeaders(okHttpResponse: Response): WritableMap {
    val responseHeaders = okHttpResponse.headers
    val headerNames = responseHeaders.names()
    val headers = Arguments.createMap()
    for (header in headerNames) {
      headers.putString(header, responseHeaders[header])
    }
    return headers
  }

  @Throws(URISyntaxException::class)
  private fun getDomainName(url: String): String {
    val uri = URI(url)
    val domain = uri.host
    return if (domain.startsWith("www.")) domain.substring(4) else domain
  }
}
