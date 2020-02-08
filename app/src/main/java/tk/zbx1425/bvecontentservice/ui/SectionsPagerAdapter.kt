package tk.zbx1425.bvecontentservice.ui

import android.content.Context
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import tk.zbx1425.bvecontentservice.R

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
class SectionsPagerAdapter(
    private val context: Context, fm: FragmentManager,
    private val fragments: Array<PackListFragment> = Array(2) { i ->
        PackListFragment.newInstance(i + 1)
    }
) : FragmentPagerAdapter(fm) {

    val TAB_TITLES = arrayOf(
        R.string.tab_text_download,
        R.string.tab_text_manage
    )

    override fun getItem(position: Int): Fragment {
        Log.i("BCSUi", "getItem called")
        return fragments[position]
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return context.resources.getString(TAB_TITLES[position])
    }

    override fun getCount(): Int {
        return fragments.size
    }

    fun setAllFilter(query: String) {
        for (fragment in fragments) {
            fragment.listAdapter.filter.filter(query)
        }
    }
}