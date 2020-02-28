import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.*

/**
 * Provides a synchronization point between the webview cookie store and okhttp3.OkHttpClient cookie store
 */
class WebviewCookieJar : CookieJar {
    private val webviewCookieManager = CookieManager.getInstance()
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val urlString = url.toString()
        for (cookie in cookies) {
            webviewCookieManager.setCookie(urlString, cookie.toString())
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val urlString = url.toString()
        val cookiesString = webviewCookieManager.getCookie(urlString)
        if (cookiesString != null && !cookiesString.isEmpty()) { //We can split on the ';' char as the cookie manager only returns cookies
//that match the url and haven't expired, so the cookie attributes aren't included
            val cookieHeaders = cookiesString.split(";").toTypedArray()
            val cookies: MutableList<Cookie> = ArrayList(cookieHeaders.size)
            for (header in cookieHeaders) {
                cookies.add(Cookie.parse(url, header)!!)
            }
            return cookies
        }
        return emptyList()
    }
}