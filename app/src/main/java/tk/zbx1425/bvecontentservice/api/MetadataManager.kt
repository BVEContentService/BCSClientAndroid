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

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import tk.zbx1425.bvecontentservice.api.model.AuthorMetadata
import tk.zbx1425.bvecontentservice.api.model.IndexMetadata
import tk.zbx1425.bvecontentservice.api.model.PackageMetadata
import tk.zbx1425.bvecontentservice.api.model.SourceMetadata
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
        progress(String.format(ManagerConfig.strBegin, PROTOCOL_VER, PROTOCOL_DATE))
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
            progress(ManagerConfig.strErrNoSrc)
            return
        }
        progress(ManagerConfig.strFinish)
    }

    fun fetchMetadataBySource(sourceServers: List<String>, progress: (String) -> Unit) {
        progress(String.format(ManagerConfig.strBegin, PROTOCOL_VER, PROTOCOL_DATE))
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
                    progress(String.format(ManagerConfig.strErrNetwork, ex.message))
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
        progress(ManagerConfig.strFinish)
    }

    private fun fetchServers(progress: (String) -> Unit) {
        for (indexServerSetURL in indexServers) {
            for (indexServerURL in indexServerSetURL.split(",")) {
                progress(
                    String.format(
                        ManagerConfig.strBeginFetch,
                        ManagerConfig.strSource,
                        indexServerURL
                    )
                )
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
                        indexHomepage = indexServer.Homepage
                    }
                    if (indexServer.Protocol != "" &&
                        Version(indexServer.Protocol) > Version(PROTOCOL_VER)
                    ) {
                        progress(String.format(ManagerConfig.strErrUpdate, indexServer.Protocol))
                        continue
                    }
                    for (i in 0 until indexServerJSONArray.length()) {
                        try {
                            val indexServerJSON: JSONObject =
                                indexServerJSONArray[i] as? JSONObject ?: continue
                            when (indexServerJSON.getString("Type")) {
                                "Index" -> {
                                    progress(
                                        String.format(
                                            ManagerConfig.strGetContent, ManagerConfig.strIndex,
                                            indexServerJSON.getString("APIURL")
                                        )
                                    )
                                    if (indexServerJSON.getString("APIURL") !in indexServers) {
                                        indexServers.add(indexServerJSON.getString("APIURL"))
                                    }
                                }
                                "SourceSpider" -> {
                                    progress(
                                        String.format(
                                            ManagerConfig.strGetContent, ManagerConfig.strSpider,
                                            indexServerJSON.getString("APIURL")
                                        )
                                    )
                                    spiderServer = SourceMetadata(indexServerJSON, indexServer)
                                }
                                "Source" -> {
                                    progress(
                                        String.format(
                                            ManagerConfig.strGetContent, ManagerConfig.strSource,
                                            indexServerJSON.getString("APIURL")
                                        )
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
                                        if (spiderServer == null || !ManagerConfig.useSpider) {
                                            fetchAuthors(newServer, progress)
                                            fetchPackages(newServer, progress)
                                        }
                                    }
                                }
                                "UGC" -> {
                                    progress(
                                        String.format(
                                            ManagerConfig.strGetContent, ManagerConfig.strUGC,
                                            indexServerJSON.getString("APIURL")
                                        )
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
                            progress(
                                String.format(
                                    ManagerConfig.strErrParser,
                                    ManagerConfig.strSource,
                                    i,
                                    ex.message
                                )
                            )
                        }
                    }
                    if (spiderServer != null && ManagerConfig.useSpider) {
                        fetchAuthors(spiderServer, progress)
                        fetchPackages(spiderServer, progress, true)
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

    private fun fetchAuthors(sourceServer: SourceMetadata, progress: (String) -> Unit) {
        progress(
            String.format(
                ManagerConfig.strBeginFetch,
                ManagerConfig.strAuthor,
                sourceServer.APIURL
            )
        )
        try {
            val authorJSONArray =
                HttpHelper.fetchArray(sourceServer, API_SUB_AUTHOR) ?: return
            for (i in 0 until authorJSONArray.length()) {
                try {
                    val authorJSON: JSONObject = authorJSONArray[i] as? JSONObject ?: continue
                    progress(
                        String.format(
                            ManagerConfig.strGetContent,
                            ManagerConfig.strAuthor,
                            authorJSON.getString("ID")
                        )
                    )
                    authors.add(
                        AuthorMetadata(
                            authorJSON, sourceServer
                        )
                    )
                } catch (ex: Exception) {
                    progress(
                        String.format(
                            ManagerConfig.strErrParser,
                            ManagerConfig.strAuthor,
                            i,
                            ex.message
                        )
                    )
                }
            }
        } catch (ex: Exception) {
            progress(String.format(ManagerConfig.strErrNetwork, ex.message))
        }
        authors = authors.distinctBy { it.ID } as ArrayList<AuthorMetadata>
    }

    private fun fetchPackages(
        sourceServer: SourceMetadata,
        progress: (String) -> Unit,
        bySpider: Boolean = false
    ) {
        progress(
            String.format(
                ManagerConfig.strBeginFetch,
                ManagerConfig.strPack,
                sourceServer.APIURL
            )
        )
        try {
            val packJSONArray = HttpHelper.fetchArray(sourceServer, API_SUB_PACK) ?: return
            for (i in 0 until packJSONArray.length()) {
                try {
                    val packJSON: JSONObject = packJSONArray[i] as? JSONObject ?: continue
                    progress(
                        String.format(
                            ManagerConfig.strGetContent,
                            ManagerConfig.strPack,
                            packJSON.getString("ID")
                        )
                    )
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
                    progress(
                        String.format(
                            ManagerConfig.strErrParser,
                            ManagerConfig.strPack,
                            i,
                            ex.message
                        )
                    )
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