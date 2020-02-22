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

package tk.zbx1425.bvecontentservice.api

import org.json.JSONObject
import tk.zbx1425.bvecontentservice.api.model.*
import java.util.*
import kotlin.collections.ArrayList

object MetadataManager {
    const val PROTOCOL_VER = "1.6"
    const val PROTOCOL_DATE = "2020-2-21"
    const val API_SUB_INDEX = "/sources.json"
    const val API_SUB_UPDATE = "/clients.json"
    const val API_SUB_AUTHOR = "/index/authors.json"
    const val API_SUB_PACK = "/index/packs.json"
    const val API_SUB_DEVSPEC = "/index/devspec.json"
    const val MAGIC_VERINFO = "__PROTOCOL_VERSION"

    var initialized: Boolean = false
    var updateMetadata: UpdateMetadata? = null
    var indexServers: ArrayList<String> = ArrayList()
    var sourceServers: ArrayList<SourceMetadata> = ArrayList()
    var ugcServers: ArrayList<IndexMetadata> = ArrayList()
    var authors: ArrayList<AuthorMetadata> = ArrayList()
    var packs: ArrayList<PackageMetadata> = ArrayList()
    var packMap: LinkedHashMap<String, PackageMetadata> = LinkedHashMap()

    var indexHomepage: String = ""

    fun cleanUp() {
        indexHomepage = ""
        updateMetadata = null
        indexServers.clear()
        sourceServers.clear()
        ugcServers.clear()
        authors.clear()
        packs.clear()
        packMap.clear()
    }

    fun fetchMetadata(indexServers: List<String>, progress: (String) -> Unit) {
        progress(String.format(ManagerConfig.strBegin, PROTOCOL_VER, PROTOCOL_DATE))
        cleanUp()
        this.initialized = true
        this.indexServers = ArrayList(indexServers)
        fetchServers(progress)
        if (sourceServers.count() == 0) {
            progress(ManagerConfig.strErrNoSrc)
            return
        }
        progress(ManagerConfig.strFinish)
    }

    fun fetchMetadataBySource(sourceServers: List<String>, progress: (String) -> Unit) {
        progress(String.format(ManagerConfig.strBegin, PROTOCOL_VER, PROTOCOL_DATE))
        this.initialized = true
        cleanUp()
        this.indexServers = arrayListOf("")
        for (sourceServer in sourceServers) {
            val newServer = if (sourceServer.trim().toLowerCase(Locale.US).endsWith(".json")) {
                try {
                    val sourceServerJSON = HttpHelper.fetchObject(sourceServer.trim()) ?: continue
                    SourceMetadata(sourceServerJSON, IndexMetadata())
                } catch (ex: java.lang.Exception) {
                    progress(String.format(ManagerConfig.strErrNetwork, ex.message))
                    null
                }
            } else {
                SourceMetadata((sourceServer.trim()))
            }
            if (newServer != null && this.sourceServers.find {
                    it.APIURL == newServer.APIURL && it.Username == newServer.Username
                } == null) {
                this.sourceServers.add(newServer)
                fetchAuthors(newServer, progress)
                fetchPackages(newServer, progress)
            }
        }
        progress(ManagerConfig.strFinish)
    }

    private fun fetchServers(progress: (String) -> Unit) {
        for (indexServerSetURL in indexServers) {
            for (indexServerURL in indexServerSetURL.split(",")) {
                progress(String.format(ManagerConfig.strBeginFetch, ManagerConfig.strSource, indexServerURL))
                try {
                    val indexServerTotalJSON = HttpHelper.fetchObject(indexServerURL.trim() + API_SUB_INDEX) ?: continue
                    val indexServerJSONArray = indexServerTotalJSON.getJSONArray("Servers")
                    val indexServer = IndexMetadata(indexServerTotalJSON, indexServerURL.trim())
                    var spiderServer: SourceMetadata? = null
                    if (indexServer.Homepage != "") indexHomepage = indexServer.Homepage
                    if (indexServer.Protocol != "" &&
                        Version(indexServer.Protocol) > Version(PROTOCOL_VER)
                    ) {
                        progress(String.format(ManagerConfig.strErrUpdate, indexServer.Protocol))
                        for (i in 0 until indexServerJSONArray.length()) {
                            try {
                                val indexServerJSON: JSONObject =
                                    indexServerJSONArray[i] as? JSONObject ?: continue
                                val APIURL = indexServerJSON.getString("APIURL")
                                if (indexServerJSON.getString("Type") == "Update") {
                                    progress(String.format(ManagerConfig.strGetContent, ManagerConfig.strUpdate, APIURL))
                                    val updateServer = SourceMetadata(indexServerJSON, indexServer)
                                    val content = JSONObject(HttpHelper.fetchApiString(updateServer, API_SUB_UPDATE) ?: "{}")
                                    if (!content.has(ManagerConfig.arch)) continue
                                    updateMetadata =
                                        UpdateMetadata(content.getJSONObject(ManagerConfig.arch), updateServer).chooseNewer(updateMetadata)
                                }
                            } catch (ex: Exception) {
                                progress(
                                    String.format(ManagerConfig.strErrParser, ManagerConfig.strSource, i, ex.message)
                                )
                            }
                        }
                        continue
                    }
                    for (i in 0 until indexServerJSONArray.length()) {
                        try {
                            val indexServerJSON: JSONObject =
                                indexServerJSONArray[i] as? JSONObject ?: continue
                            val APIURL = indexServerJSON.getString("APIURL")
                            when (indexServerJSON.getString("Type")) {
                                "Index" -> {
                                    progress(String.format(ManagerConfig.strGetContent, ManagerConfig.strIndex, APIURL))
                                    if (APIURL !in indexServers) {
                                        indexServers.add(APIURL)
                                    }
                                }
                                "Update" -> {
                                    progress(String.format(ManagerConfig.strGetContent, ManagerConfig.strUpdate, APIURL))
                                    val updateServer = SourceMetadata(indexServerJSON, indexServer)
                                    val content = JSONObject(HttpHelper.fetchApiString(updateServer, API_SUB_UPDATE) ?: "{}")
                                    if (content.has(ManagerConfig.arch)) {
                                        updateMetadata =
                                            UpdateMetadata(content.getJSONObject(ManagerConfig.arch), updateServer).chooseNewer(updateMetadata)
                                    }
                                }
                                "SourceSpider" -> {
                                    progress(String.format(ManagerConfig.strGetContent, ManagerConfig.strSpider, APIURL))
                                    spiderServer = SourceMetadata(indexServerJSON, indexServer)
                                }
                                "Source" -> {
                                    progress(String.format(ManagerConfig.strGetContent, ManagerConfig.strSource, APIURL))
                                    val newServer = SourceMetadata(indexServerJSON, indexServer)
                                    if (sourceServers.find { it.APIURL == newServer.APIURL && it.Username == newServer.Username } == null) {
                                        sourceServers.add(newServer)
                                        //A spider server is present, it holds all the data of the following servers
                                        if (spiderServer == null || !ManagerConfig.useSpider) {
                                            if (fetchAuthors(newServer, progress)) {
                                                fetchPackages(newServer, progress)
                                            }
                                        }
                                    }
                                }
                                "UGC" -> {
                                    progress(String.format(ManagerConfig.strGetContent, ManagerConfig.strUGC, APIURL))
                                    ugcServers.add(IndexMetadata(indexServerJSON, APIURL))
                                }
                            }
                        } catch (ex: Exception) {
                            progress(String.format(ManagerConfig.strErrParser, ManagerConfig.strSource, i, ex.message))
                        }
                    }
                    if (spiderServer != null && ManagerConfig.useSpider) {
                        if (fetchAuthors(spiderServer, progress)) {
                            fetchPackages(spiderServer, progress, true)
                        }
                    }
                    break
                } catch (ex: Exception) {
                    progress(String.format(ManagerConfig.strErrNetwork, ex.message))
                }
            }
        }
        sourceServers =
            sourceServers.distinctBy { Pair(it.APIURL, it.Username) } as ArrayList<SourceMetadata>
    }

    //Returns: If the parser is allowed to go on and parse pack metadata
    private fun fetchAuthors(sourceServer: SourceMetadata, progress: (String) -> Unit): Boolean {
        progress(String.format(ManagerConfig.strBeginFetch, ManagerConfig.strAuthor, sourceServer.APIURL))
        try {
            val authorJSONArray =
                HttpHelper.fetchApiArray(sourceServer, API_SUB_AUTHOR) ?: return false
            if ((authorJSONArray[0] as? JSONObject)?.optString("ID") == MAGIC_VERINFO) {
                val verInfo = authorJSONArray[0] as JSONObject
                if (Version(PROTOCOL_VER) < Version(verInfo.optString("Name_LO", "0.0"))) {
                    progress(String.format(ManagerConfig.strErrUpdate, verInfo.optString("Name_LO")))
                    return false
                }
            }
            for (i in 0 until authorJSONArray.length()) {
                try {
                    val authorJSON: JSONObject = authorJSONArray[i] as? JSONObject ?: continue
                    progress(String.format(ManagerConfig.strGetContent, ManagerConfig.strAuthor, authorJSON.getString("ID")))
                    authors.add(AuthorMetadata(authorJSON, sourceServer))
                } catch (ex: Exception) {
                    progress(String.format(ManagerConfig.strErrParser, ManagerConfig.strAuthor, i, ex.message))
                }
            }
        } catch (ex: Exception) {
            progress(String.format(ManagerConfig.strErrNetwork, ex.message))
        }
        authors = authors.distinctBy { it.ID } as ArrayList<AuthorMetadata>
        return true
    }

    private fun fetchPackages(
        sourceServer: SourceMetadata,
        progress: (String) -> Unit,
        bySpider: Boolean = false
    ) {
        progress(String.format(ManagerConfig.strBeginFetch, ManagerConfig.strPack, sourceServer.APIURL))
        try {
            val packJSONArray = HttpHelper.fetchApiArray(sourceServer, API_SUB_PACK) ?: return
            for (i in 0 until packJSONArray.length()) {
                try {
                    val packJSON: JSONObject = packJSONArray[i] as? JSONObject ?: continue
                    progress(String.format(ManagerConfig.strGetContent, ManagerConfig.strPack, packJSON.getString("ID")))
                    val metadata =
                        PackageMetadata(
                            packJSON,
                            sourceServer,
                            bySpider
                        )
                    if (metadata.File == "" && (metadata.Homepage == "" || !metadata.NoFile)) continue
                    packs.add(metadata)
                    if (!packMap.containsKey(metadata.ID) ||
                        packMap[metadata.ID]?.Version ?: Version("0.0") < metadata.Version
                    ) {
                        packMap[metadata.ID] = metadata
                    }
                } catch (ex: Exception) {
                    progress(String.format(ManagerConfig.strErrParser, ManagerConfig.strPack, i, ex.message))
                }
            }
        } catch (ex: Exception) {
            progress(String.format(ManagerConfig.strErrNetwork, ex.message))
        }
        packs = packs.distinctBy { Pair(it.ID, it.Version) } as ArrayList<PackageMetadata>
    }

    fun getAuthor(id: String): AuthorMetadata? {
        return authors.find { it.ID == id }
    }

    fun getPacksByAuthor(id: String): List<PackageMetadata> {
        return packs.filter { it.Author.ID == id }
    }

}