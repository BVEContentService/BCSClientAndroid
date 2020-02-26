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

import tk.zbx1425.bvecontentservice.io.log.Log
import java.io.IOException
import java.io.InputStream
import kotlin.math.max


class ThrottledInputStream(private val rawStream: InputStream, kBytesPersecond: Int) : InputStream() {
    private var totalBytesRead: Long = 0
    private var startTimeMillis: Long = 0
    private val ratePerMillis: Long

    @Throws(IOException::class)
    override fun read(): Int {
        if (startTimeMillis == 0L) {
            startTimeMillis = System.currentTimeMillis()
        }
        if (totalBytesRead % CHECK_BLOCK_SIZE == 0L) {
            val now = System.currentTimeMillis()
            val interval = now - startTimeMillis
            if (interval * ratePerMillis < totalBytesRead + 1) {
                try {
                    val sleepTime = (totalBytesRead + 1) / ratePerMillis - interval
                    Log.i("BCSTIS", "Slept " + sleepTime)
                    Thread.sleep(max(1, sleepTime))
                } catch (e: InterruptedException) {
                }
            }
        }
        totalBytesRead += 1
        return rawStream.read()
    }

    companion object {
        private const val BYTES_PER_KILOBYTE = 1024L
        private const val MILLIS_PER_SECOND = 1000L
        private const val CHECK_BLOCK_SIZE = 4096L
    }

    init {
        ratePerMillis = kBytesPersecond * BYTES_PER_KILOBYTE / MILLIS_PER_SECOND
    }
}