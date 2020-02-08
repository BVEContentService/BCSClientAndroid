package tk.zbx1425.bvecontentservice.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import tk.zbx1425.bvecontentservice.PackDetailActivity
import tk.zbx1425.bvecontentservice.R
import tk.zbx1425.bvecontentservice.api.PackageMetadata
import tk.zbx1425.bvecontentservice.storage.PackListManager


class PackListFragment : Fragment() {

    private lateinit var dataList: ArrayList<PackageMetadata>
    lateinit var listAdapter: PackListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val index = arguments?.getInt(ARG_SECTION_NUMBER) ?: 1
        dataList = if (index == 1) {
            PackListManager.onlineList
        } else {
            PackListManager.localList
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i("BCSUi", "onCreateView called")
        listAdapter = PackListAdapter(activity as Context, dataList) { metadata ->
            val intent = Intent(activity as Context, PackDetailActivity::class.java)
            intent.putExtra("metadata", metadata)
            startActivity(intent)
        }
        val view = inflater.inflate(R.layout.fragment_main, container, false)
        val rv: RecyclerView = view.findViewById(R.id.recyclerView)
        rv.layoutManager = LinearLayoutManager(activity as Context)
        rv.adapter = listAdapter
        return view
    }

    override fun onResume() {
        listAdapter.notifyDataSetChanged()
        super.onResume()
    }

    companion object {
        private const val ARG_SECTION_NUMBER = "section_number"

        @JvmStatic
        fun newInstance(sectionNumber: Int): PackListFragment {
            return PackListFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_NUMBER, sectionNumber)
                }
            }
        }
    }
}