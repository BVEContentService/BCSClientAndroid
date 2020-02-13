package tk.zbx1425.bvecontentservice

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_about.*
import tk.zbx1425.bvecontentservice.ui.MetadataView

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        appMetadataPlaceholder.addView(MetadataView(this))
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}