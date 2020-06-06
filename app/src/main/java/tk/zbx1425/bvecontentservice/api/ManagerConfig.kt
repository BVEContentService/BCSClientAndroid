package tk.zbx1425.bvecontentservice.api

import tk.zbx1425.bvecontentservice.ApplicationContext
import tk.zbx1425.bvecontentservice.R
import tk.zbx1425.bvecontentservice.getPreference

object ManagerConfig {
    const val arch = "Official-Android"
    const val simulator = "H2"

    val mergedResponseCache: Boolean
        get() {
            return getPreference("useSourceSpider", true)
        }
    val reverseProxy: Boolean
        get() {
            return getPreference("reverseProxy", false)
        }

    //Important strings that should get localized
    val strIndex = "Index"
    val strUpdate = "Update"
    val strResponseCache = "Response Cache"
    val strUGC = "UGC"
    val strSource = "Source"
    val strAuthor = "Author"
    val strPack = "Pack"
    val strBegin = "BCS Protocol v%s\nBy zbx1425, %s."
    val strBeginFetch = "MMNetwork: Fetching %s from %s"
    val strGetContent = "MMParser: Got %s %s"
    val strFinish = "Fetching Finished."
    val strErrNetwork = "ERROR: MMNetwork: %s"
    val strErrParser = "ERROR: MMParser: %s %d : %s"
    val strErrNoSrc = ApplicationContext.context.resources.getString(R.string.fetch_err_nosrc)
    val strErrUpdate = ApplicationContext.context.resources.getString(R.string.fetch_err_update)
}