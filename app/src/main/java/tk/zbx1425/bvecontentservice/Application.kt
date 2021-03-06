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

package tk.zbx1425.bvecontentservice

import android.app.Application
import tk.zbx1425.bvecontentservice.io.bindHandlerToThread
import tk.zbx1425.bvecontentservice.io.network.ImageLoader
import tk.zbx1425.bvecontentservice.io.network.PackDownloadManager


class Application : Application() {
    override fun onCreate() {
        super.onCreate()
        ApplicationContext.initialize(this)
        bindHandlerToThread(Thread.currentThread())
        ImageLoader.initCache()
        PackDownloadManager.register(this)
        //PackLocalManager.deleteUnqualifiedFile()
    }

}