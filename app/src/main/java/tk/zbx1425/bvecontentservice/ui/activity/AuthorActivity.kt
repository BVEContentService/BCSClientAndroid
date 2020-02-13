package tk.zbx1425.bvecontentservice.ui.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_author.*
import tk.zbx1425.bvecontentservice.R
import tk.zbx1425.bvecontentservice.api.model.AuthorMetadata
import tk.zbx1425.bvecontentservice.ui.component.MetadataView

class AuthorActivity : AppCompatActivity() {
    lateinit var metadata: AuthorMetadata

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_author)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        metadata = intent.getSerializableExtra("metadata") as AuthorMetadata
        this.title = metadata.Name
        textPackName.text = metadata.Name
        authorMetadataPlaceholder.addView(MetadataView(this, metadata))
    }
}