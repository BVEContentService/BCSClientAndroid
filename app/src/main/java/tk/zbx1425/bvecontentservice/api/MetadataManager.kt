package tk.zbx1425.bvecontentservice.api

import androidx.preference.PreferenceManager
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import tk.zbx1425.bvecontentservice.ApplicationContext
import tk.zbx1425.bvecontentservice.R
import tk.zbx1425.bvecontentservice.api.model.AuthorMetadata
import tk.zbx1425.bvecontentservice.api.model.IndexMetadata
import tk.zbx1425.bvecontentservice.api.model.PackageMetadata
import tk.zbx1425.bvecontentservice.api.model.SourceMetadata
import tk.zbx1425.bvecontentservice.log.Log
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

object MetadataManager {
    const val PROTOCOL_VER = "1.4"
    const val PROTOCOL_DATE = "2020-2-10"
    const val API_SUB_INDEX = "/sources.json"
    const val API_SUB_AUTHOR = "/index/authors.json"
    const val API_SUB_PACK = "/index/packs.json"

    val client = OkHttpClient()
    var initialized: Boolean = false
    var indexServers: ArrayList<String> = ArrayList()
    var sourceServers: ArrayList<SourceMetadata> = ArrayList()
    var ugcServers: ArrayList<IndexMetadata> = ArrayList()
    var authors: ArrayList<AuthorMetadata> = ArrayList()
    var packs: ArrayList<PackageMetadata> = ArrayList()
    var packMap: HashMap<String, PackageMetadata> = HashMap()

    var indexHomepage: String = ""

    fun fetchMetadata(indexServers: List<String>, progress: (String) -> Unit) {
        progress(String.format("BCS Protocol v%s\nBy zbx1425, %s.", PROTOCOL_VER, PROTOCOL_DATE))
        this.initialized = true
        this.indexServers = ArrayList(indexServers)
        indexHomepage = ""
        sourceServers.clear()
        ugcServers.clear()
        authors.clear()
        packs.clear()
        packMap.clear()
        fetchServers(progress)
        if (sourceServers.count() == 0) {
            progress(
                ApplicationContext.context.resources.getString(R.string.fetch_err_nosrc)
            )
            return
        }
        progress("Fetching Finished.")
    }

    fun fetchMetadataBySource(sourceServers: List<String>, progress: (String) -> Unit) {
        progress(String.format("BCS Protocol v%s\nBy zbx1425, %s.", PROTOCOL_VER, PROTOCOL_DATE))
        this.initialized = true
        this.indexServers = arrayListOf("")
        this.sourceServers.clear()
        indexHomepage = ""
        ugcServers.clear()
        authors.clear()
        packs.clear()
        packMap.clear()
        for (sourceServer in sourceServers) {
            val newServer = if (sourceServer.trim().toLowerCase(Locale.US).endsWith(".json")) {
                try {
                    val request =
                        Request.Builder().url(sourceServer.trim()).build()
                    val response = client.newCall(request).execute()
                    val result = response.body()?.string() ?: continue
                    val sourceServerJSON = JSONObject(result)
                    SourceMetadata(
                        sourceServerJSON,
                        IndexMetadata()
                    )
                } catch (ex: java.lang.Exception) {
                    progress("ERROR! MMNetwork: " + ex.message)
                    null
                }
            } else {
                SourceMetadata((sourceServer.trim()))
            }
            if (newServer != null && this.sourceServers.find {
                    it.APIURL == newServer.APIURL &&
                            it.Username == newServer.Username
                } == null) {
                this.sourceServers.add(newServer)
                fetchAuthors(newServer, progress)
                fetchPackages(newServer, progress)
            }
        }
        progress("Fetching Finished.")
    }

    private fun fetchServers(progress: (String) -> Unit) {
        progress("MetaMan: Server Fetching started")
        for (indexServerSetURL in indexServers) {
            for (indexServerURL in indexServerSetURL.split(",")) {
                progress("MMNetwork: Fetching Servers from " + indexServerURL.trim() + API_SUB_INDEX)
                try {
                    val request =
                        Request.Builder().url(indexServerURL.trim() + API_SUB_INDEX).build()
                    val response = client.newCall(request).execute()
                    val result = response.body()?.string() ?: continue
                    val indexServerTotalJSON = JSONObject(result)
                    val indexServerJSONArray = indexServerTotalJSON.getJSONArray("Servers")
                    val indexServer =
                        IndexMetadata(
                            indexServerTotalJSON,
                            indexServerURL.trim()
                        )
                    var spiderServer: SourceMetadata? = null
                    if (indexServer.Homepage != "") {
                        Log.i("BCSDebug", indexServer.APIURL)
                        indexHomepage = indexServer.Homepage
                    }
                    if (indexServer.Protocol != "" &&
                        Version(indexServer.Protocol) > Version(PROTOCOL_VER)
                    ) {
                        progress(
                            String.format(
                                ApplicationContext.context.resources
                                    .getString(R.string.fetch_err_update), indexServer.Protocol
                            )
                        )
                        continue
                    }
                    for (i in 0 until indexServerJSONArray.length()) {
                        try {
                            val indexServerJSON: JSONObject =
                                indexServerJSONArray[i] as? JSONObject ?: continue
                            when (indexServerJSON.getString("Type")) {
                                "Index" -> {
                                    progress(
                                        "MMParser: Got Index Server " + indexServerJSON.getString("APIURL")
                                    )
                                    if (indexServerJSON.getString("APIURL") !in indexServers) {
                                        indexServers.add(indexServerJSON.getString("APIURL"))
                                    }
                                }
                                "SourceSpider" -> {
                                    progress(
                                        "MMParser: Got Source Spider " + indexServerJSON.getString("APIURL")
                                    )
                                    spiderServer =
                                        SourceMetadata(
                                            indexServerJSON,
                                            indexServer
                                        )
                                }
                                "Source" -> {
                                    progress(
                                        "MMParser: Got Source Server " + indexServerJSON.getString("APIURL")
                                    )
                                    val newServer =
                                        SourceMetadata(
                                            indexServerJSON,
                                            indexServer
                                        )
                                    if (sourceServers.find {
                                            it.APIURL == newServer.APIURL &&
                                                    it.Username == newServer.Username
                                        } == null) {
                                        sourceServers.add(newServer)
                                        //A spider server is present, it holds all the data of the following servers
                                        if (spiderServer == null || !PreferenceManager.getDefaultSharedPreferences(
                                                ApplicationContext.context
                                            ).getBoolean("useSourceSpider", true)
                                        ) {
                                            fetchAuthors(newServer, progress)
                                            fetchPackages(newServer, progress)
                                        }
                                    }
                                }
                                "UGC" -> {
                                    progress(
                                        "MMParser: Got UGC Server " + indexServerJSON.getString("APIURL")
                                    )
                                    ugcServers.add(
                                        IndexMetadata(
                                            indexServerJSON,
                                            indexServerJSON.getString("APIURL")
                                        )
                                    )
                                }
                            }
                        } catch (ex: Exception) {
                            progress("ERROR! MMParser: SSq " + i.toString() + " : " + ex.message)
                        }
                    }
                    if (spiderServer != null && PreferenceManager.getDefaultSharedPreferences(
                            ApplicationContext.context
                        ).getBoolean("useSourceSpider", true)
                    ) {
                        fetchAuthors(spiderServer, progress)
                        fetchPackages(spiderServer, progress, true)
                    }
                    break
                } catch (ex: Exception) {
                    progress("ERROR! MMNetwork: " + ex.message)
                }
            }
        }
        sourceServers =
            sourceServers.distinctBy { Pair(it.APIURL, it.Username) } as ArrayList<SourceMetadata>
    }

    private fun fetchAuthors(sourceServer: SourceMetadata, progress: (String) -> Unit) {
        progress("MMNetwork: Fetching Authors from " + sourceServer.APIURL.trim())
        try {
            val authorJSONArray =
                HttpHelper.fetchArray(sourceServer, API_SUB_AUTHOR) ?: return
            for (i in 0 until authorJSONArray.length()) {
                try {
                    val authorJSON: JSONObject = authorJSONArray[i] as? JSONObject ?: continue
                    progress("MMParser: Got Author " + authorJSON.getString("ID"))
                    authors.add(
                        AuthorMetadata(
                            authorJSON, sourceServer
                        )
                    )
                } catch (ex: Exception) {
                    progress("ERROR! MMParser: ASq " + i.toString() + " : " + ex.message)
                }
            }
        } catch (ex: Exception) {
            progress("ERROR! MMNetwork: " + ex.message)
        }
        authors = authors.distinctBy { it.ID } as ArrayList<AuthorMetadata>
    }

    private fun fetchPackages(
        sourceServer: SourceMetadata,
        progress: (String) -> Unit,
        bySpider: Boolean = false
    ) {
        progress("MMNetwork: Fetching Packages from " + sourceServer.APIURL.trim())
        try {
            val packJSONArray = HttpHelper.fetchArray(sourceServer, API_SUB_PACK) ?: return
            for (i in 0 until packJSONArray.length()) {
                try {
                    val packJSON: JSONObject = packJSONArray[i] as? JSONObject ?: continue
                    progress("MMParser: Got Pack " + packJSON.getString("ID"))
                    val metadata =
                        PackageMetadata(
                            packJSON,
                            this,
                            sourceServer,
                            bySpider
                        )
                    if (metadata.File == "") continue
                    packs.add(metadata)
                    if (!packMap.containsKey(metadata.ID) ||
                        packMap[metadata.ID]?.Version ?: Version("0.0") < metadata.Version
                    ) {
                        packMap[metadata.ID] = metadata
                    }
                } catch (ex: Exception) {
                    progress("ERROR! MMParser: PSq " + i.toString() + " : " + ex.message)
                }
            }
        } catch (ex: Exception) {
            progress("ERROR! MMNetwork: " + ex.message)
        }
        packs = packs.distinctBy { Pair(it.ID, it.Version) } as ArrayList<PackageMetadata>
    }

    fun getPack(id: String, version: Version): PackageMetadata {
        return packs.find { it.ID == id && it.Version == version }
            ?: throw IllegalArgumentException("Author cannot be found")
    }

    fun getAuthor(id: String): AuthorMetadata? {
        return authors.find { it.ID == id }
    }

    fun getPacksByAuthor(id: String): List<PackageMetadata> {
        return packs.filter { it.Author.ID == id }
    }

    fun getActiveUGCServer(): IndexMetadata? {
        val ugcSrc = PreferenceManager.getDefaultSharedPreferences(ApplicationContext.context)
            .getString("listUGCSource", "********")
        val customUgc = PreferenceManager.getDefaultSharedPreferences(ApplicationContext.context)
            .getString("customUGCSource", "")
        return when (ugcSrc) {
            "" -> null
            "########" -> IndexMetadata(
                customUgc ?: return null
            )
            "********" -> if (ugcServers.count() > 0) ugcServers[0]
            else IndexMetadata(
                customUgc ?: return null
            )
            else -> ugcServers.find { it.APIURL == ugcSrc }
        }
    }
}