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

import android.app.SearchManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_webview.*
import tk.zbx1425.bvecontentservice.api.MetadataManager
import tk.zbx1425.bvecontentservice.io.PackListManager
import tk.zbx1425.bvecontentservice.ui.SectionsPagerAdapter
import tk.zbx1425.bvecontentservice.ui.activity.AboutActivity
import tk.zbx1425.bvecontentservice.ui.activity.LoaderActivity
import tk.zbx1425.bvecontentservice.ui.activity.SettingActivity
import tk.zbx1425.bvecontentservice.ui.component.InfoFragment
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {

    private lateinit var searchView: SearchView
    private lateinit var sectionsPagerAdapter: SectionsPagerAdapter
    private var mExitTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!MetadataManager.initialized) {
            val intent = Intent(this, LoaderActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
        tabs.setupWithViewPager(view_pager)
        view_pager.adapter = sectionsPagerAdapter
        PackListManager.pagerAdapter = sectionsPagerAdapter
        fab.setOnClickListener {
            val launchIntent =
                packageManager.getLaunchIntentForPackage("com.Jeminie.Hmmsim2")
                    ?: packageManager.getLaunchIntentForPackage("com.Jeminie.Hmmsim")
            if (launchIntent == null) {
                Snackbar.make(
                    root_view,
                    R.string.info_hmmsim_fail,
                    Snackbar.LENGTH_SHORT
                ).show()
            } else {
                val dlgAlert = AlertDialog.Builder(this)
                dlgAlert.setNegativeButton(android.R.string.no, null)
                dlgAlert.setCancelable(true)
                dlgAlert.setTitle(R.string.app_name)
                dlgAlert.setMessage(R.string.alert_hmmsim)
                dlgAlert.setPositiveButton(android.R.string.yes) { _: DialogInterface, i: Int ->
                    if (i == DialogInterface.BUTTON_POSITIVE) {
                        startActivity(launchIntent)
                        exitProcess(0)
                    }
                }
                dlgAlert.create().show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchView = menu!!.findItem(R.id.action_search).actionView as SearchView
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView.maxWidth = Int.MAX_VALUE
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                sectionsPagerAdapter.setAllFilter(query ?: return false)
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                sectionsPagerAdapter.setAllFilter(newText ?: return false)
                return false
            }
        })
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.refresh -> {
                val intent = Intent(this, LoaderActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }
            R.id.setting -> {
                val intent = Intent(this, SettingActivity::class.java)
                startActivity(intent)
            }
            R.id.about -> {
                val intent = Intent(this, AboutActivity::class.java)
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
            val activeView = sectionsPagerAdapter.getItem(view_pager.currentItem)
            if (activeView is InfoFragment && activeView.webView.canGoBack()) {
                activeView.webView.goBack()
                return true
            }
            if ((System.currentTimeMillis() - mExitTime) > 2000) {
                Snackbar.make(
                    root_view,
                    R.string.info_exit_again,
                    Snackbar.LENGTH_SHORT
                ).show()
                mExitTime = System.currentTimeMillis()
            } else {
                finishAffinity()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}