package tk.zbx1425.bvecontentservice.ui.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_about.*
import tk.zbx1425.bvecontentservice.R
import tk.zbx1425.bvecontentservice.replaceView
import tk.zbx1425.bvecontentservice.ui.component.MetadataView

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        appMetadataPlaceholder.replaceView(MetadataView(this))
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}