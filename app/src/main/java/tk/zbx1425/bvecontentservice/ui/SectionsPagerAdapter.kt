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

package tk.zbx1425.bvecontentservice.ui

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import tk.zbx1425.bvecontentservice.R
import tk.zbx1425.bvecontentservice.api.MetadataManager
import tk.zbx1425.bvecontentservice.ui.component.InfoFragment
import tk.zbx1425.bvecontentservice.ui.component.PackListFragment

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
class SectionsPagerAdapter(
    private val context: Context, fm: FragmentManager,
    private val fragments: Array<Fragment> = arrayOf(
        InfoFragment(),
        PackListFragment.newInstance(1),
        PackListFragment.newInstance(2)
    )
) : FragmentPagerAdapter(fm) {

    val TAB_TITLES = arrayOf(
        R.string.tab_text_info,
        R.string.tab_text_download,
        R.string.tab_text_manage
    )

    override fun getItem(position: Int): Fragment {
        return if (MetadataManager.indexHomepage != "") {
            fragments[position]
        } else {
            fragments[kotlin.math.min(position + 1, fragments.size - 1)]
        }
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return if (MetadataManager.indexHomepage != "") {
            context.resources.getString(TAB_TITLES[position])
        } else {
            context.resources.getString(TAB_TITLES[position + 1])
        }
    }

    override fun getCount(): Int {
        return if (MetadataManager.indexHomepage != "") {
            fragments.size
        } else {
            fragments.size - 1
        }
    }

    fun setAllFilter(query: String) {
        for (fragment in fragments) {
            if (fragment !is PackListFragment) continue
            if (!fragment.isAdapterInitialized) continue
            fragment.listAdapter.filter.filter(query)
        }
    }
}