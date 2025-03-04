package com.dinapal.busdakho.data.remote.interceptor

import com.dinapal.busdakho.util.Constants
import com.dinapal.busdakho.util.Logger
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * Base interceptor class with common functionality for all interceptors
 */
abstract class BaseInterceptor : Interceptor {
    protected val tag = this::class.java.simpleName
    private val utf8 = StandardCharsets.UTF_8

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestBuilder = request.newBuilder()

        try {
            // Log request
            logRequest(request)

            // Modify request if needed
            val modifiedRequest = modifyRequest(requestBuilder).build()

            // Execute request
            val response = executeRequest(chain, modifiedRequest)

            // Log response
            logResponse(response)

            // Modify response if needed
            return modifyResponse(response)
        } catch (e: Exception) {
            Logger.e(tag, "Interceptor error", e)
            return handleError(e, chain)
        }
    }

    /**
     * Modify the request before sending
     */
    protected open fun modifyRequest(builder: okhttp3.Request.Builder): okhttp3.Request.Builder {
        return builder
    }

    /**
     * Execute the network request
     */
    protected open fun executeRequest(chain: Interceptor.Chain, request: okhttp3.Request): Response {
        return chain.proceed(request)
    }

    /**
     * Modify the response before returning
     */
    protected open fun modifyResponse(response: Response): Response {
        return response
    }

    /**
     * Handle any errors that occur during the request
     */
    protected open fun handleError(error: Exception, chain: Interceptor.Chain): Response {
        return when (error) {
            is IOException -> createErrorResponse(
                chain,
                503,
                "Network error occurred"
            )
            else -> createErrorResponse(
                chain,
                500,
                "Internal error occurred"
            )
        }
    }

    /**
     * Create an error response
     */
    protected fun createErrorResponse(
        chain: Interceptor.Chain,
        code: Int,
        message: String
    ): Response {
        val errorJson = """
            {
                "code": $code,
                "message": "$message"
            }
        """.trimIndent()

        return Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(message)
            .body(errorJson.toResponseBody("application/json".toMediaTypeOrNull()))
            .build()
    }

    /**
     * Log the request details
     */
    protected open fun logRequest(request: okhttp3.Request) {
        if (!Constants.DEBUG) return

        val requestBody = request.body
        val hasRequestBody = requestBody != null

        val requestMessage = StringBuilder().apply {
            append("-> ${request.method} ${request.url}")
            if (hasRequestBody) {
                append(" (${requestBody?.contentLength()} bytes)")
            }
        }.toString()

        Logger.d(tag, requestMessage)

        request.headers.forEach { (name, value) ->
            Logger.d(tag, "Header: $name: $value")
        }

        if (hasRequestBody) {
            requestBody?.contentType()?.let {
                Logger.d(tag, "Content-Type: $it")
            }

            if (isPlaintext(requestBody)) {
                val buffer = Buffer()
                requestBody?.writeTo(buffer)
                val charset = requestBody?.contentType()?.charset(utf8) ?: utf8
                Logger.d(tag, "Body: ${buffer.readString(charset)}")
            } else {
                Logger.d(tag, "Body: [binary ${requestBody?.contentLength()} bytes]")
            }
        }
    }

    /**
     * Log the response details
     */
    protected open fun logResponse(response: Response) {
        if (!Constants.DEBUG) return

        val responseBody = response.body
        val hasResponseBody = responseBody != null

        val responseMessage = StringBuilder().apply {
            append("<- ${response.code} ${response.message}")
            if (hasResponseBody) {
                append(" (${responseBody?.contentLength()} bytes)")
            }
        }.toString()

        Logger.d(tag, responseMessage)

        response.headers.forEach { (name, value) ->
            Logger.d(tag, "Header: $name: $value")
        }

        if (hasResponseBody && isPlaintext(responseBody)) {
            val source = responseBody?.source()
            source?.request(Long.MAX_VALUE)
            val buffer = source?.buffer
            val charset = responseBody?.contentType()?.charset(utf8) ?: utf8
            buffer?.clone()?.readString(charset)?.let {
                Logger.d(tag, "Body: $it")
            }
        }
    }

    /**
     * Check if the body is plain text
     */
    private fun isPlaintext(body: okhttp3.RequestBody?): Boolean {
        if (body == null) return false
        try {
            val buffer = Buffer()
            body.writeTo(buffer)
            val prefix = Buffer()
            val byteCount = if (buffer.size < 64) buffer.size else 64
            buffer.copyTo(prefix, 0, byteCount)
            for (i in 0..15) {
                if (prefix.exhausted()) {
                    break
                }
                val codePoint = prefix.readUtf8CodePoint()
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                    return false
                }
            }
            return true
        } catch (e: Exception) {
            return false
        }
    }

    private fun isPlaintext(body: okhttp3.ResponseBody?): Boolean {
        if (body == null) return false
        val contentType = body.contentType()
        return contentType?.let {
            it.type == "text" ||
                    it.subtype == "json" ||
                    it.subtype == "xml" ||
                    it.subtype == "html" ||
                    it.subtype == "x-www-form-urlencoded"
        } ?: false
    }
}
