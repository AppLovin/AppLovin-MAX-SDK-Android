package com.applovin.enterprise.apps.demoapp.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.applovin.enterprise.apps.demoapp.R
import com.applovin.enterprise.apps.demoapp.data.main.DemoMenuItem
import com.applovin.enterprise.apps.demoapp.data.main.Footer
import com.applovin.enterprise.apps.demoapp.data.main.ListItem
import com.applovin.enterprise.apps.demoapp.data.main.SectionHeader

/**
 * [RecyclerView.Adapter] used to show list of items on the main screen.
 * <p>
 * Created by Harry Arakkal on 9/17/2019
 */
class MainRecyclerViewAdapter(private val listItems: List<ListItem>,
                              private val listener: OnMainListItemClickListener,
                              context: Context)
    : RecyclerView.Adapter<MainRecyclerViewAdapter.ViewHolder>()
{
    interface OnMainListItemClickListener
    {
        fun onItemClicked(item: ListItem)
    }

    private val layoutInflater: LayoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        val viewId = when (viewType)
        {
            ListItem.SECTION_HEADER -> R.layout.section_header_item
            ListItem.AD_ITEM -> R.layout.ad_type_item
            ListItem.FOOTER -> R.layout.footer_item
            else -> View.NO_ID
        }

        return ViewHolder(layoutInflater.inflate(viewId, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int)
    {
        holder.title.text = when (val item = listItems[position])
        {
            is SectionHeader -> item.title
            is DemoMenuItem -> item.title
            is Footer -> item.getAppDetails()
            else -> ""
        }
    }

    override fun getItemCount(): Int
    {
        return listItems.count()
    }

    override fun getItemViewType(position: Int): Int
    {
        return listItems[position].type
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
    {
        val title: TextView = view.findViewById(R.id.title)

        init
        {
            title.setOnClickListener {
                listener.onItemClicked(listItems[adapterPosition])
            }
        }
    }
}
