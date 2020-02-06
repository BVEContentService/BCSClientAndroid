package tk.zbx1425.bvecontentservice.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import tk.zbx1425.bvecontentservice.R
import tk.zbx1425.bvecontentservice.api.DownloadImageTask
import tk.zbx1425.bvecontentservice.api.PackageMetadata
import java.text.SimpleDateFormat
import java.util.*

class PackListAdapter(
    private val context: Context, val values: List<PackageMetadata>,
    val onListItemClickListener: (PackageMetadata) -> Unit
) : RecyclerView.Adapter<PackListAdapter.PackListViewHolder>() {
    class PackListViewHolder(val rowView: View) : RecyclerView.ViewHolder(rowView) {
        val textTitle = rowView.findViewById<View>(R.id.textTitle) as TextView
        val textAuthor = rowView.findViewById(R.id.textAuthor) as TextView
        val textVersion = rowView.findViewById(R.id.textVersion) as TextView
        val textTimestamp = rowView.findViewById(R.id.textTimestamp) as TextView
        val imageView = rowView.findViewById<View>(R.id.imageThumbnail) as ImageView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackListViewHolder {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        return PackListViewHolder(inflater.inflate(R.layout.listitem_route, parent, false))
    }

    override fun getItemCount(): Int {
        return values.count()
    }

    override fun onBindViewHolder(holder: PackListViewHolder, position: Int) {
        val metadata = values[position]
        holder.textTitle.text = metadata.Name_LO
        holder.textAuthor.text = metadata.Author.Name_LO
        holder.textVersion.text = metadata.Version.get()
        holder.textTimestamp.text = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            .format(metadata.Timestamp)
        DownloadImageTask(holder.imageView).execute(metadata.Thumbnail)
        holder.rowView.setOnClickListener {
            onListItemClickListener(values[position])
        }
    }
}