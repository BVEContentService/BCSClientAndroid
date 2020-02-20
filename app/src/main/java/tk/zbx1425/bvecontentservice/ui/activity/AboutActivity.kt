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

package tk.zbx1425.bvecontentservice.ui.activity

import Identification
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_about.*
import tk.zbx1425.bvecontentservice.R
import tk.zbx1425.bvecontentservice.getPreference
import tk.zbx1425.bvecontentservice.replaceView
import tk.zbx1425.bvecontentservice.ui.component.MetadataView

class AboutActivity : AppCompatActivity() {

    private var clickTimes = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        appMetadataPlaceholder.replaceView(MetadataView(this))
        textUUID.text = Identification.deviceID
        if (getPreference("showLoadLog", false)){
            iconLayout.setOnClickListener {
                clickTimes++
                if (clickTimes > 20) textUUID.visibility = View.VISIBLE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}