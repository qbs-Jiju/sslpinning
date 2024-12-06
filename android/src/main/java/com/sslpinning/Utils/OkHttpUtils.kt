package com.sslpinning.Utils

import android.content.Context
import android.net.Uri
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.ReadableType
import com.sslpinning.BuildConfig
import okhttp3.CertificatePinner
import okhttp3.CookieJar
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONException
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 *
 */
object OkHttpUtils {
    private const val HEADERS_KEY = "headers"
    private const val BODY_KEY = "body"
    private const val METHOD_KEY = "method"
    private const val FILE = "file"
    private val clientsByDomain: HashMap<String, OkHttpClient?> = HashMap<String, OkHttpClient?>()
    private lateinit var defaultClient: OkHttpClient

    //    private static OkHttpClient client = null;
    private lateinit var sslContext: SSLContext
    private var content_type: String = "application/json; charset=utf-8"
    var mediaType: MediaType? = content_type.toMediaTypeOrNull()

    fun buildOkHttpClient(
        cookieJar: CookieJar,
        domainName: String,
        certs: ReadableArray,
        options: ReadableMap
    ): OkHttpClient {
         var client: OkHttpClient
        var certificatePinner: CertificatePinner? = null
        if (!clientsByDomain.containsKey(domainName)) {
            // add logging interceptor
            val logging: HttpLoggingInterceptor = HttpLoggingInterceptor()
            logging.setLevel(HttpLoggingInterceptor.Level.BODY)

            val clientBuilder: OkHttpClient.Builder = OkHttpClient.Builder()
            clientBuilder.cookieJar(cookieJar)

            if (options.hasKey("pkPinning") && options.getBoolean("pkPinning")) {
                // public key pinning
                certificatePinner = initPublicKeyPinning(certs, domainName)
                clientBuilder.certificatePinner(certificatePinner)
            } else {
                // ssl pinning
                val manager = initSSLPinning(certs)
              manager?.let {
                clientBuilder
                  .sslSocketFactory(sslContext!!.socketFactory, it)
              }
            }


            if (BuildConfig.DEBUG) {
                clientBuilder.addInterceptor(logging)
            }

            client = clientBuilder
                .build()


            clientsByDomain[domainName] = client
        } else {
            client = clientsByDomain[domainName]!!
        }



        if (options.hasKey("timeoutInterval")) {
            val timeout: Int = options.getInt("timeoutInterval")
            // Copy to customize OkHttp for this request.
            client = client.newBuilder()
                .readTimeout(timeout.toLong(), TimeUnit.MILLISECONDS)
                .writeTimeout(timeout.toLong(), TimeUnit.MILLISECONDS)
                .connectTimeout(timeout.toLong(), TimeUnit.MILLISECONDS)
                .build()
        }


        return client
    }

    fun buildDefaultOHttpClient(
        cookieJar: CookieJar,
        domainName: String?,
        options: ReadableMap
    ): OkHttpClient {
            val logging: HttpLoggingInterceptor = HttpLoggingInterceptor()
            logging.setLevel(HttpLoggingInterceptor.Level.BODY)

            val clientBuilder: OkHttpClient.Builder = OkHttpClient.Builder()
            clientBuilder.cookieJar(cookieJar)

            if (BuildConfig.DEBUG) {
                clientBuilder.addInterceptor(logging)
            }

            defaultClient = clientBuilder.build()

        if (options.hasKey("timeoutInterval")) {
            val timeout: Int = options.getInt("timeoutInterval")

            defaultClient = defaultClient.newBuilder()
                .readTimeout(timeout.toLong(), TimeUnit.MILLISECONDS)
                .writeTimeout(timeout.toLong(), TimeUnit.MILLISECONDS)
                .connectTimeout(timeout.toLong(), TimeUnit.MILLISECONDS)
                .build()
        }

        return defaultClient
    }

    private fun initPublicKeyPinning(pins: ReadableArray, domain: String): CertificatePinner {
        val certificatePinnerBuilder: CertificatePinner.Builder = CertificatePinner.Builder()
        //add all keys to the certficates pinner
        for (i in 0 until pins.size()) {
            certificatePinnerBuilder.add(domain, pins.getString(i))
        }

        val certificatePinner: CertificatePinner = certificatePinnerBuilder.build()

        return certificatePinner
    }

    private fun initSSLPinning(certs: ReadableArray): X509TrustManager? {
        var trustManager: X509TrustManager? = null
        try {
            sslContext = SSLContext.getInstance("TLS")
            val cf = CertificateFactory.getInstance("X.509")
            val keyStoreType = KeyStore.getDefaultType()
            val keyStore = KeyStore.getInstance(keyStoreType)
            keyStore.load(null, null)

            for (i in 0 until certs.size()) {
                val filename: String = certs.getString(i)
                val caInput: InputStream = BufferedInputStream(
                    OkHttpUtils::class.java.classLoader.getResourceAsStream(
                        "assets/$filename.cer"
                    )
                )
                var ca: Certificate?
                try {
                    ca = cf.generateCertificate(caInput)
                } finally {
                    caInput.close()
                }

                keyStore.setCertificateEntry(filename, ca)
            }

            val tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm()
            val tmf = TrustManagerFactory.getInstance(tmfAlgorithm)
            tmf.init(keyStore)

            val trustManagers = tmf.trustManagers
            check(!(trustManagers.size != 1 || trustManagers[0] !is X509TrustManager)) { "Unexpected default trust managers:" + trustManagers.contentToString() }
            trustManager = trustManagers[0] as X509TrustManager

            sslContext.init(null, arrayOf<TrustManager>(trustManager), null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return trustManager
    }

    private fun isFilePart(part: ReadableArray): Boolean {
        if (part.getType(1) != ReadableType.Map) {
            return false
        }
        val value: ReadableMap = part.getMap(1)
        return value.hasKey("type") && (value.hasKey("uri") || value.hasKey("path"))
    }

    private fun addFormDataPart(
        context: Context,
        multipartBodyBuilder: MultipartBody.Builder,
        fileData: ReadableMap,
        key: String
    ) {
        var _uri = Uri.parse("")
        if (fileData.hasKey("uri")) {
            _uri = Uri.parse(fileData.getString("uri"))
        } else if (fileData.hasKey("path")) {
            _uri = Uri.parse(fileData.getString("path"))
        }
        val type: String? = fileData.getString("type")
        var fileName: String? = null
        if (fileData.hasKey("fileName")) {
            fileName = fileData.getString("fileName")
        } else if (fileData.hasKey("name")) {
            fileName = fileData.getString("name")
        }

        try {
            val file = getTempFile(context, _uri)
            multipartBodyBuilder.addFormDataPart(
                key,
                fileName,
              file.asRequestBody(type?.toMediaTypeOrNull())
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun buildFormDataRequestBody(context: Context, formData: ReadableMap): RequestBody {
        val multipartBodyBuilder: MultipartBody.Builder = MultipartBody.Builder().setType(MultipartBody.FORM)
        multipartBodyBuilder.setType(("multipart/form-data".toMediaTypeOrNull()!!))
        if (formData.hasKey("_parts")) {
          val parts: ReadableArray? = formData.getArray("_parts")
          parts?.let {
            for (i in 0 until it.size()) {
              val part: ReadableArray = it.getArray(i)
              var key = ""
              if (part.getType(0) == ReadableType.String) {
                key = part.getString(0)
              } else if (part.getType(0) == ReadableType.Number) {
                key = part.getInt(0).toString()
              }

              if (isFilePart(part)) {
                val fileData: ReadableMap = part.getMap(1)
                addFormDataPart(context, multipartBodyBuilder, fileData, key)
              } else {
                val value: String = part.getString(1)
                multipartBodyBuilder.addFormDataPart(key, value)
              }
            }
          }
        }
        return multipartBodyBuilder.build()
    }

    @Throws(JSONException::class)
    fun buildRequest(context: Context, options: ReadableMap, hostname: String): Request {
        val requestBuilder: Request.Builder = Request.Builder()
        var body: RequestBody? = null

        var method:String? = "GET"

        if (options.hasKey(HEADERS_KEY)) {
            setRequestHeaders(options, requestBuilder)
        }

        if (options.hasKey(METHOD_KEY)) {
            method = options.getString(METHOD_KEY)
        }

        if (options.hasKey(BODY_KEY)) {
            val bodyType: ReadableType = options.getType(BODY_KEY)
            when (bodyType) {
                ReadableType.String -> body = options.getString(
                  BODY_KEY
                )!!
                  .toRequestBody(mediaType)

                ReadableType.Map -> {
                    val bodyMap: ReadableMap? = options.getMap(BODY_KEY)
                  bodyMap?.apply {
                    if (hasKey("formData")) {
                        val formData: ReadableMap? = getMap("formData")
                        body = buildFormDataRequestBody(context, formData!!)
                    } else if (hasKey("_parts")) {
                        body = buildFormDataRequestBody(context, this)
                    }
                  }
                }
              else ->{
                println("Readable type is not map or string")
              }
            }
        }
        return requestBuilder
            .url(hostname)
            .method(method!!, body)
            .build()
    }

    @Throws(IOException::class)
    fun getTempFile(context: Context, uri: Uri): File {
        val file = File.createTempFile("media", null)
        val inputStream = context.contentResolver.openInputStream(uri)
        val outputStream: OutputStream = BufferedOutputStream(FileOutputStream(file))
        val buffer = ByteArray(1024)
        var len: Int
        while ((inputStream!!.read(buffer).also { len = it }) != -1) outputStream.write(
            buffer,
            0,
            len
        )
        inputStream.close()
        outputStream.close()
        return file
    }

    private fun setRequestHeaders(options: ReadableMap, requestBuilder: Request.Builder) {
        val map: ReadableMap? = options.getMap((HEADERS_KEY))
        //add headers to request
        Utilities.addHeadersFromMap(map!!, requestBuilder)
        if (map.hasKey("content-type")) {
            content_type = map.getString("content-type")!!
            mediaType = content_type.toMediaTypeOrNull()
        }
    }
}
