package com.applovin.enterprise.apps.demoapp.ads

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_list.*
import com.applovin.enterprise.apps.demoapp.kotlin.R

data class DemoMenuItem(val title: String, val subtitle: String, val intent: Intent? = null)
abstract class DemoMenuActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        setupListViewFooter()
        setupListViewContents(getListViewContents())
    }

    protected open fun setupListViewFooter()
    {
    }

    protected abstract fun getListViewContents(): Array<DemoMenuItem>

    private fun setupListViewContents(items: Array<DemoMenuItem>)
    {
        val listAdapter = object : ArrayAdapter<DemoMenuItem>(this, android.R.layout.simple_list_item_2, items)
        {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View
            {
                val inflater = this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                val row = convertView ?: inflater.inflate(android.R.layout.simple_list_item_2, parent, false)

                val item = items[position]

                val title: TextView = row.findViewById(android.R.id.text1)
                title.text = item.title
                val subtitle: TextView = row.findViewById(android.R.id.text2)
                subtitle.text = item.subtitle

                return row
            }
        }
        list_view.adapter = listAdapter

        list_view.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            if (items[position].intent != null)
                startActivity(items[position].intent)
        }
    }
}