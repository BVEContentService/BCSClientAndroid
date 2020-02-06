package tk.zbx1425.bvecontentservice.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import tk.zbx1425.bvecontentservice.R
import tk.zbx1425.bvecontentservice.api.DownloadImageTask
import tk.zbx1425.bvecontentservice.api.PackageMetadata
import java.text.SimpleDateFormat
import java.util.*


class PackMetadataAdapter(context: Context, val values: List<PackageMetadata>) :
    ArrayAdapter<PackageMetadata>(context, -1, values) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        if (convertView == null) {
            val inflater = context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            /*val rowView : View = inflater.inflate(android.R.layout.simple_list_item_1, parent, false)
            val text1 = rowView.findViewById(android.R.id.text1) as TextView
            text1.text = values[position].Name_LO*/
            val metadata = values[position]
            val rowView: View = inflater.inflate(R.layout.listitem_route, parent, false)
            val textTitle = rowView.findViewById<View>(R.id.textTitle) as TextView
            val textAuthor = rowView.findViewById(R.id.textAuthor) as TextView
            val textVersion = rowView.findViewById(R.id.textVersion) as TextView
            val textTimestamp = rowView.findViewById(R.id.textTimestamp) as TextView
            val imageView: ImageView = rowView.findViewById<View>(R.id.imageThumbnail) as ImageView
            textTitle.text = metadata.Name_LO
            textAuthor.text = metadata.Author.Name_LO
            textVersion.text = metadata.Version.get()
            textTimestamp.text = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .format(values[position].Timestamp)
            DownloadImageTask(imageView).execute(metadata.Thumbnail)
            return rowView
        }
        return convertView
    }

}