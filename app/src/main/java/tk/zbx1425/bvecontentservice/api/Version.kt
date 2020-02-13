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

import java.io.Serializable

class Version(version: String?) : Comparable<Version?>, Serializable {
    private val version: String
    fun get(): String {
        return version
    }

    override fun compareTo(that: Version?): Int {
        if (that == null) return 1
        val thisParts = this.get().split(".").toTypedArray()
        val thatParts = that.get().split(".").toTypedArray()
        val length = Math.max(thisParts.size, thatParts.size)
        for (i in 0 until length) {
            val thisPart = if (i < thisParts.size) thisParts[i].toInt() else 0
            val thatPart = if (i < thatParts.size) thatParts[i].toInt() else 0
            if (thisPart < thatPart) return -1
            if (thisPart > thatPart) return 1
        }
        return 0
    }

    override fun equals(that: Any?): Boolean {
        if (this === that) return true
        if (that == null) return false
        return if (this.javaClass != that.javaClass) false else this.compareTo(that as Version) == 0
    }

    init {
        requireNotNull(version) { "Version can not be null" }
        require(version.matches(Regex("[0-9]+(\\.[0-9]+)*"))) {
            "Invalid version format " + version
        }
        this.version = version
    }
}