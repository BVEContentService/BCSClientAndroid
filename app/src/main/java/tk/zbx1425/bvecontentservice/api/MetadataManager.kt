package tk.zbx1425.bvecontentservice.api

import okhttp3.Request
import org.json.JSONObject

object MetadataManager {
    const val API_SUB_INDEX = "/sources.json"
    const val API_SUB_AUTHOR = "/index/authors.json"
    const val API_SUB_PACK = "/index/packs.json"

    val client = HttpClientFactory.getHttpClient()!!
    var initialized: Boolean = false
    var indexServers: ArrayList<String> = ArrayList()
    var sourceServers: ArrayList<SourceMetadata> = ArrayList()
    var authors: ArrayList<AuthorMetadata> = ArrayList()
    var packs: ArrayList<PackageMetadata> = ArrayList()
    var packMap: HashMap<String, PackageMetadata> = HashMap()

    fun fetchMetadata(indexServers: List<String>, progress: (String) -> Unit) {
        progress("BCS Protocol v1.2 Client v1.2\nBy zbx1425, 2020-2-6.")
        this.initialized = true
        this.indexServers = ArrayList(indexServers)
        sourceServers.clear()
        authors.clear()
        packs.clear()
        packMap.clear()
        fetchServers(progress)
        if (sourceServers.count() == 0) {
            progress(
                "ERROR: Cannot get any server!\n" +
                        "Check your network connection, or replace index server!"
            )
            return
        }
        fetchAuthors(progress)
        fetchPackages(progress)
        progress("Fetching Finished.")
    }

    fun fetchMetadataBySource(sourceServers: List<String>, progress: (String) -> Unit) {
        progress("BCS Protocol v1.2 Client v1.2\nRunning in SourceServer Mode.")
        this.initialized = true
        this.indexServers = arrayListOf("")
        this.sourceServers.clear()
        authors.clear()
        packs.clear()
        packMap.clear()
        for (sourceServer in sourceServers) {
            this.sourceServers.add(SourceMetadata((sourceServer)))
        }
        fetchAuthors(progress)
        fetchPackages(progress)
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
                    val indexServer = IndexMetadata(indexServerTotalJSON, indexServerURL.trim())
                    for (i in 0 until indexServerJSONArray.length()) {
                        try {
                            val indexServerJSON: JSONObject =
                                indexServerJSONArray[i] as? JSONObject ?: continue
                            if (indexServerJSON.getString("Type") == "Index") {
                                progress(
                                    "MMParser: Got Index Server " + indexServerJSON.getString("APIURL")
                                )
                                if (indexServerJSON.getString("APIURL") !in indexServers) {
                                    indexServers.add(indexServerJSON.getString("APIURL"))
                                }
                            } else {
                                progress(
                                    "MMParser: Got Source Server " + indexServerJSON.getString("APIURL")
                                )
                                sourceServers.add(SourceMetadata(indexServerJSON, indexServer))
                            }
                        } catch (ex: Exception) {
                            progress("ERROR! MMParser: SSq " + i.toString() + " : " + ex.message)
                        }
                    }
                    break
                } catch (ex: Exception) {
                    progress("ERROR! MMNetwork: " + ex.message)
                }
            }
        }
        sourceServers = sourceServers.distinctBy { it.APIURL } as ArrayList<SourceMetadata>
    }

    private fun fetchAuthors(progress: (String) -> Unit) {
        progress("MetaMan: Author Fetching started")
        for (sourceServer in sourceServers) {
            progress("MMNetwork: Fetching Authors from " + sourceServer.APIURL.trim())
            try {
                val authorJSONArray =
                    HttpHelper.fetchArray(sourceServer, API_SUB_AUTHOR) ?: continue
                for (i in 0 until authorJSONArray.length()) {
                    try {
                        val authorJSON: JSONObject = authorJSONArray[i] as? JSONObject ?: continue
                        progress("MMParser: Got Author " + authorJSON.getString("ID"))
                        authors.add(AuthorMetadata(authorJSON))
                    } catch (ex: Exception) {
                        progress("ERROR! MMParser: ASq " + i.toString() + " : " + ex.message)
                    }
                }
            } catch (ex: Exception) {
                progress("ERROR! MMNetwork: " + ex.message)
            }
        }
        authors = authors.distinctBy { it.ID } as ArrayList<AuthorMetadata>
    }

    private fun fetchPackages(progress: (String) -> Unit) {
        progress("MetaMan: Package Fetching started")
        for (sourceServer in sourceServers) {
            progress("MMNetwork: Fetching Packages from " + sourceServer.APIURL.trim())
            try {
                val packJSONArray = HttpHelper.fetchArray(sourceServer, API_SUB_PACK) ?: continue
                for (i in 0 until packJSONArray.length()) {
                    try {
                        val packJSON: JSONObject = packJSONArray[i] as? JSONObject ?: continue
                        progress("MMParser: Got Pack " + packJSON.getString("ID"))
                        val metadata = PackageMetadata(packJSON, this, sourceServer)
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
        }
        packs = packs.distinctBy { Pair(it.ID, it.Version) } as ArrayList<PackageMetadata>
    }

    fun getPack(id: String, version: Version): PackageMetadata {
        return packs.find { it.ID == id && it.Version == version }
            ?: throw IllegalArgumentException("Author cannot be found")
    }

    fun getAuthor(id: String): AuthorMetadata {
        return authors.find { it.ID == id }
            ?: throw IllegalArgumentException("Author cannot be found")
    }

    fun getPacksByAuthor(id: String): List<PackageMetadata> {
        return packs.filter { it.Author.ID == id }
    }
}