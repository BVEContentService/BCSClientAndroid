package tk.zbx1425.bvecontentservice.ui

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import tk.zbx1425.bvecontentservice.R
import tk.zbx1425.bvecontentservice.api.PackageMetadata
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class PackListAdapter(
    private val context: Context, val values: List<PackageMetadata>,
    val onListItemClickListener: (PackageMetadata) -> Unit
) : RecyclerView.Adapter<PackListAdapter.PackListViewHolder>(), Filterable {
    var valuesFiltered: List<PackageMetadata> = values

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
        return valuesFiltered.count()
    }

    override fun onBindViewHolder(holder: PackListViewHolder, position: Int) {
        val metadata = valuesFiltered[position]
        holder.textTitle.text = metadata.Name
        holder.textAuthor.text = metadata.Author.Name
        if (metadata.UpdateAvailable) {
            holder.textVersion.text = String.format(
                context.resources.getString(R.string.text_update_avail), metadata.Version.get()
            )
            holder.textVersion.setTextColor(Color.rgb(255, 140, 0))
        } else {
            holder.textVersion.text = metadata.Version.get()
            holder.textVersion.setTextColor(Color.BLACK)
        }
        holder.textTimestamp.text = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            .format(metadata.Timestamp)
        ImageLoader.setPackImageAsync(holder.imageView, metadata)
        holder.rowView.setOnClickListener {
            onListItemClickListener(values[position])
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                if (constraint == null || constraint == "") {
                    valuesFiltered = values
                } else {
                    val filteredList: ArrayList<PackageMetadata> = ArrayList()
                    for (metadata in values) {
                        if (metadata.searchAssistName.contains(
                                constraint.toString()
                                    .toLowerCase(Locale.US)
                            )
                        ) {
                            filteredList.add(metadata)
                        }
                    }
                    valuesFiltered = filteredList
                }
                val result = FilterResults()
                result.values = valuesFiltered
                return result
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                valuesFiltered = results?.values as List<PackageMetadata>
                notifyDataSetChanged()
            }
        }
    }
}