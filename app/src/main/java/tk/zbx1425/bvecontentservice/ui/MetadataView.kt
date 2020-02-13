package tk.zbx1425.bvecontentservice.ui

import android.content.Context
import android.view.View
import android.widget.TableLayout
import kotlinx.android.synthetic.main.view_metadata.view.*
import tk.zbx1425.bvecontentservice.BuildConfig
import tk.zbx1425.bvecontentservice.R
import tk.zbx1425.bvecontentservice.api.IndexMetadata
import tk.zbx1425.bvecontentservice.api.PackageMetadata
import tk.zbx1425.bvecontentservice.api.SourceMetadata
import java.text.SimpleDateFormat
import java.util.*

class MetadataView : TableLayout {

    constructor(context: Context) : super(context) {
        View.inflate(context, R.layout.view_metadata, this)
        textVersion.text = BuildConfig.VERSION_NAME + " " + BuildConfig.BUILD_TYPE
        textDate.text = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            .format(BuildConfig.BUILD_TIME)
    }

    constructor(context: Context, metadata: PackageMetadata) : this(context) {
        rowAPIURL.visibility = View.GONE
        rowMaintainer.visibility = View.GONE
        textName.visibility = View.GONE
        textAuthor.text = metadata.Author.Name
        textVersion.text = metadata.Version.get()
        textDate.text = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            .format(metadata.Timestamp)
        textContact.text = metadata.Author.ID
        if (metadata.Homepage == "" && metadata.Author.Homepage == "") {
            rowHomepage.visibility = View.GONE
        } else if (metadata.Homepage == "") {
            textHomepage.text = metadata.Author.Homepage
            textHomepage2.visibility = View.GONE
        } else if (metadata.Author.Homepage == "") {
            textHomepage.text = metadata.Homepage
            textHomepage2.visibility = View.GONE
        } else {
            textHomepage.text = metadata.Homepage
            textHomepage2.text = metadata.Author.Homepage
        }
    }

    constructor(context: Context, metadata: SourceMetadata) : this(context) {
        rowVersion.visibility = View.GONE
        rowAuthor.visibility = View.GONE
        rowDate.visibility = View.GONE
        rowAPIURL.visibility = View.VISIBLE
        rowMaintainer.visibility = View.VISIBLE
        textAPIURL.text = metadata.APIURL
        textName.text = metadata.Name
        textMaintainer.text = metadata.Author
        textContact.text = metadata.Contact
        textHomepage2.visibility = View.GONE
        if (metadata.Homepage == "") {
            rowHomepage.visibility = View.GONE
        } else {
            textHomepage.text = metadata.Homepage
        }
    }

    constructor(context: Context, metadata: IndexMetadata) : this(context) {
        rowVersion.visibility = View.GONE
        rowAuthor.visibility = View.GONE
        rowDate.visibility = View.GONE
        rowAPIURL.visibility = View.VISIBLE
        rowMaintainer.visibility = View.VISIBLE
        textAPIURL.text = metadata.APIURL
        textName.text = metadata.Name
        textMaintainer.text = metadata.Author
        textContact.text = metadata.Contact
        textHomepage2.visibility = View.GONE
        if (metadata.Homepage == "") {
            rowHomepage.visibility = View.GONE
        } else {
            textHomepage.text = metadata.Homepage
        }
    }

}