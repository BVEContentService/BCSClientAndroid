//  This file is part of BVE Content Service Client (BCSC).
//
//  BCSC is free software: you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  BCSC is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with BCSC.  If not, see <https://www.gnu.org/licenses/>.

package tk.zbx1425.bvecontentservice.io.throttling

import Identification
import com.tonyodev.fetch2core.Downloader
import com.tonyodev.fetch2core.Downloader.Response
import com.tonyodev.fetch2core.InterruptMonitor
import com.tonyodev.fetch2core.copyStreamToString
import com.tonyodev.fetch2core.getContentLengthFromHeader
import com.tonyodev.fetch2okhttp.OkHttpDownloader
import okhttp3.Headers
import okhttp3.OkHttpClient
import tk.zbx1425.bvecontentservice.api.HttpHelper
import java.io.BufferedInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import com.tonyodev.fetch2core.Downloader.ServerRequest as ServerRequest1

class ThrottledOkHttpDownloader(okHttpClient: OkHttpClient) : OkHttpDownloader(okHttpClient) {
    override fun execute(request: ServerRequest1, interruptMonitor: InterruptMonitor): Response? {
        var okHttpRequest = onPreClientExecute(client, request).newBuilder()
            .header("Referer", request.extras.getString("Referer", HttpHelper.REFERER))
            .header("User-Agent", HttpHelper.FAKEUA)
            .header("X-BCS-UUID", Identification.deviceID)
            .header("X-BCS-CHECKSUM", Identification.getDateChecksum())
            .build()
        var okHttpResponse = client.newCall(okHttpRequest).execute()
        var responseHeaders = getResponseHeaders(okHttpResponse.headers())
        var code = okHttpResponse.code()
        if ((code == HttpURLConnection.HTTP_MOVED_TEMP
                    || code == HttpURLConnection.HTTP_MOVED_PERM
                    || code == HttpURLConnection.HTTP_SEE_OTHER) && responseHeaders.containsKey("location")
        ) {
            okHttpRequest = onPreClientExecute(client, getRedirectedServerRequest(request, responseHeaders["location"]?.firstOrNull() ?: ""))
                .newBuilder()
                .header("Referer", request.extras.getString("Referer", HttpHelper.REFERER))
                .header("User-Agent", HttpHelper.FAKEUA)
                .header("X-BCS-UUID", Identification.deviceID)
                .header("X-BCS-CHECKSUM", Identification.getDateChecksum())
                .build()
            okHttpResponse = client.newCall(okHttpRequest).execute()
            responseHeaders = getResponseHeaders(okHttpResponse.headers())
            code = okHttpResponse.code()
        }
        val success = okHttpResponse.isSuccessful
        var contentLength = getContentLengthFromHeader(responseHeaders, -1)
        val byteStream: InputStream? = okHttpResponse.body()?.byteStream()
        val errorResponseString: String? = if (!success) {
            copyStreamToString(byteStream, false)
        } else {
            null
        }

        val hash = getContentHash(responseHeaders)

        if (contentLength < 1) {
            contentLength = responseHeaders["content-length"]?.firstOrNull()?.toLong() ?: -1L
        }

        val acceptsRanges = code == HttpURLConnection.HTTP_PARTIAL ||
                responseHeaders["accept-ranges"]?.firstOrNull() == "bytes"

        onServerResponse(
            request, Response(
                code = code,
                isSuccessful = success,
                contentLength = contentLength,
                byteStream = null,
                request = request,
                hash = hash,
                responseHeaders = responseHeaders,
                acceptsRanges = acceptsRanges,
                errorResponse = errorResponseString
            )
        )
        val evilStream = if (byteStream != null) {
            val kbps = request.extras.getString("Throttle", "").toIntOrNull()
            if (kbps != null && kbps > 0) {
                ThrottledInputStream(BufferedInputStream(byteStream), kbps)
            } else {
                byteStream
            }
        } else {
            null
        }

        val response = Response(
            code = code,
            isSuccessful = success,
            contentLength = contentLength,
            byteStream = evilStream,
            request = request,
            hash = hash,
            responseHeaders = responseHeaders,
            acceptsRanges = acceptsRanges,
            errorResponse = errorResponseString
        )

        connections[response] = okHttpResponse
        return response
    }

    private fun getResponseHeaders(okResponseHeaders: Headers): MutableMap<String, List<String>> {
        val headers = mutableMapOf<String, List<String>>()
        for (i in 0 until okResponseHeaders.size()) {
            val key = okResponseHeaders.name(i)
            @Suppress("SENSELESS_COMPARISON")
            if (key != null) {
                val values = okResponseHeaders.values(key)
                headers[key.toLowerCase()] = values
            }
        }
        return headers
    }

    private fun getRedirectedServerRequest(oldRequest: Downloader.ServerRequest, redirectUrl: String): Downloader.ServerRequest {
        return Downloader.ServerRequest(
            id = oldRequest.id,
            url = oldRequest.url,
            headers = oldRequest.headers,
            file = oldRequest.file,
            fileUri = oldRequest.fileUri,
            tag = oldRequest.tag,
            identifier = oldRequest.identifier,
            requestMethod = oldRequest.requestMethod,
            extras = oldRequest.extras,
            redirected = true,
            redirectUrl = redirectUrl,
            segment = oldRequest.segment
        )
    }
}