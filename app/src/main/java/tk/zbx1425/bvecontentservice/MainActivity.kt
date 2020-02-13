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

import android.Manifest
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import tk.zbx1425.bvecontentservice.api.MetadataManager
import tk.zbx1425.bvecontentservice.log.Log
import tk.zbx1425.bvecontentservice.ui.SectionsPagerAdapter
import tk.zbx1425.bvecontentservice.ui.activity.AboutActivity
import tk.zbx1425.bvecontentservice.ui.activity.LoaderActivity
import tk.zbx1425.bvecontentservice.ui.activity.SettingActivity
import java.io.File


class MainActivity : AppCompatActivity() {

    private lateinit var searchView: SearchView
    private lateinit var sectionsPagerAdapter: SectionsPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!MetadataManager.initialized) {
            val intent = Intent(this, LoaderActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
        setContentView(R.layout.activity_main)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        setSupportActionBar(toolbar)
        sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
        tabs.setupWithViewPager(view_pager)
        view_pager.adapter = sectionsPagerAdapter
        fab.setOnClickListener {
            val directory = File("/storage/emulated/0/")
            Log.i("ZBX", directory.absolutePath)
            Log.i("ZBX", directory.listFiles()?.joinToString(",") ?: "")
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

    private fun checkPermission(perm: String) {
        val result = ContextCompat.checkSelfPermission(this, perm)
        if (result != PackageManager.PERMISSION_GRANTED) requestPermission(perm)
    }

    private fun requestPermission(perm: String) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, perm)) {
            Toast.makeText(
                this,
                "Write External Storage permission allows us to do store images. Please allow this permission in App Settings.",
                Toast.LENGTH_LONG
            ).show()
            Log.e("ZBX", "You fucking careless bastard!")
            finishAffinity()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(perm), 810)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        if (requestCode == 810) {
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Log.e("ZBX", "You fucking stubborn bastard!")
                finishAffinity()
            }
        }
    }
}