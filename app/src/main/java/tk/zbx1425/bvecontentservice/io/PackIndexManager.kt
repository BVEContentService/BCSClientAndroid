package tk.zbx1425.bvecontentservice.io

import Identification
import java.io.File
import java.io.PrintWriter

object PackIndexManager {
    private val indexFile = File(PackLocalManager.appDir, "hmmsim.dat")
    private lateinit var _packList: ArrayList<String>

    private val packList: ArrayList<String>
        get() {
            if (!::_packList.isInitialized) {
                if (!indexFile.exists()) {
                    _packList = ArrayList()
                    indexFile.createNewFile()
                } else {
                    val lines = indexFile.readLines()
                    _packList = ArrayList(lines.mapNotNull {
                        try {
                            Identification.decrypt(it, Identification.deviceID)
                        } catch (ex: Exception) {
                            null
                        }
                    })
                }
            }
            return _packList
        }

    private fun writePacks() {
        val writer = PrintWriter(indexFile)
        packList.forEach { writer.println(Identification.encrypt(it, Identification.deviceID)) }
        writer.flush()
        writer.close()
    }

    fun onPackInstalled(id: String) {
        packList.add(id)
        indexFile.appendText(Identification.encrypt(id, Identification.deviceID) ?: "" + "\n")
    }

    fun onPackUninstalled(id: String) {
        packList.remove(id)
        writePacks()
    }

    fun isPackInstalled(id: String): Boolean {
        return packList.contains(id)
    }
}