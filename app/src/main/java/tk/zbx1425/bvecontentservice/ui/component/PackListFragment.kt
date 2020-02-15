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
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_main.view.*
import tk.zbx1425.bvecontentservice.R
import tk.zbx1425.bvecontentservice.api.model.PackageMetadata
import tk.zbx1425.bvecontentservice.io.PackListManager
import tk.zbx1425.bvecontentservice.ui.PackListAdapter
import tk.zbx1425.bvecontentservice.ui.activity.PackDetailActivity


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
        listAdapter = PackListAdapter(
            activity as Context,
            dataList
        ) { metadata ->
            val intent = Intent(activity as Context, PackDetailActivity::class.java)
            intent.putExtra("metadata", metadata)
            startActivity(intent)
        }
        val view = inflater.inflate(R.layout.fragment_main, container, false)
        val rv: RecyclerView = view.findViewById(R.id.recyclerView)
        rv.layoutManager = LinearLayoutManager(activity as Context)
        rv.adapter = listAdapter
        listAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                view.emptyTextView.visibility =
                    if (listAdapter.itemCount > 0) View.GONE else View.VISIBLE
            }
        })
        return view
    }

    override fun onResume() {
        listAdapter.notifyDataSetChanged()
        super.onResume()
    }

    val isAdapterInitialized: Boolean
        get() {
            return ::listAdapter.isInitialized
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