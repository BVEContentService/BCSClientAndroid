package tk.zbx1425.bvecontentservice.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_author.*
import tk.zbx1425.bvecontentservice.R
import tk.zbx1425.bvecontentservice.api.MetadataManager
import tk.zbx1425.bvecontentservice.api.model.AuthorMetadata
import tk.zbx1425.bvecontentservice.replaceView
import tk.zbx1425.bvecontentservice.ui.PackListAdapter
import tk.zbx1425.bvecontentservice.ui.component.DescriptionView
import tk.zbx1425.bvecontentservice.ui.component.MetadataView

class AuthorActivity : AppCompatActivity() {
    lateinit var metadata: AuthorMetadata
    lateinit var listAdapter: PackListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_author)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        val srcMetadata = MetadataManager.getAuthor(intent.getStringExtra("aid") ?: "")
        if (srcMetadata == null) {
            finish()
            return
        } else {
            metadata = srcMetadata
        }
        this.title = metadata.Name
        textPackName.text = metadata.Name
        authorMetadataPlaceholder.replaceView(MetadataView(this, metadata))
        descriptionPlaceholder.replaceView(DescriptionView(this, metadata))
        val dataList = MetadataManager.getPacksByAuthor(metadata.ID)
            .sortedBy { it.Timestamp }
        listAdapter = PackListAdapter(this, dataList) { metadata ->
            val intent = Intent(this, PackDetailActivity::class.java)
            intent.putExtra("metadata", metadata)
            startActivity(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = listAdapter
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}