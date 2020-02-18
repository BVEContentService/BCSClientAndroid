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

package tk.zbx1425.bvecontentservice.ui.component

import android.content.Context
import android.text.Html
import android.text.Spanned
import android.view.View
import android.widget.TableLayout
import kotlinx.android.synthetic.main.view_metadata.view.*
import tk.zbx1425.bvecontentservice.BuildConfig
import tk.zbx1425.bvecontentservice.R
import tk.zbx1425.bvecontentservice.api.model.AuthorMetadata
import tk.zbx1425.bvecontentservice.api.model.IndexMetadata
import tk.zbx1425.bvecontentservice.api.model.PackageMetadata
import tk.zbx1425.bvecontentservice.api.model.SourceMetadata
import java.text.SimpleDateFormat
import java.util.*

class MetadataView : TableLayout {

    constructor(context: Context) : super(context) {
        View.inflate(context, R.layout.view_metadata, this)
        rowID.visibility = View.GONE
        textVersion.text = BuildConfig.VERSION_NAME + " " + BuildConfig.BUILD_TYPE
        textDate.text = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            .format(BuildConfig.BUILD_TIME)
    }

    constructor(
        context: Context, metadata: PackageMetadata,
        clickListener: OnClickListener? = null
    ) : this(context) {
        rowAPIURL.visibility = View.GONE
        rowMaintainer.visibility = View.GONE
        textName.visibility = View.GONE
        textID.text = metadata.ID
        textVersion.text = metadata.Version.get()
        textDate.text = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            .format(metadata.Timestamp)
        if (metadata.Origin == "") {
            textAuthor.text = linkedText(metadata.Author.Name)
            textAuthor.setOnClickListener(clickListener)
        } else {
            textAuthor.text = metadata.Origin
            rowUploader.visibility = View.VISIBLE
            textUploader.text = linkedText(metadata.Author.Name)
            textUploader.setOnClickListener(clickListener)
        }
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

    constructor(context: Context, metadata: AuthorMetadata) : this(context) {
        rowID.visibility = View.GONE
        rowAPIURL.visibility = View.GONE
        rowMaintainer.visibility = View.GONE
        textName.visibility = View.GONE
        rowAuthor.visibility = View.GONE
        rowVersion.visibility = View.GONE
        rowDate.visibility = View.GONE
        textContact.text = metadata.ID
        textHomepage2.visibility = View.GONE
        if (metadata.Homepage == "") {
            rowHomepage.visibility = View.GONE
        } else {
            textHomepage.text = metadata.Homepage
        }
    }

    constructor(context: Context, metadata: SourceMetadata) : this(context) {
        rowID.visibility = View.GONE
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
        rowID.visibility = View.GONE
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

    private fun linkedText(text: String): Spanned {
        return if (android.os.Build.VERSION.SDK_INT > 24) {
            Html.fromHtml(
                String.format(
                    "<a href='#'>%s</a>",
                    Html.escapeHtml(text)
                ), Html.FROM_HTML_MODE_COMPACT
            )
        } else {
            Html.fromHtml(
                String.format(
                    "<a href='#'>%s</a>",
                    Html.escapeHtml(text)
                )
            )
        }
    }

}