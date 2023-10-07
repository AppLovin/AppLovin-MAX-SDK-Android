package com.applovin.enterprise.apps.demoapp.ui

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import com.applovin.enterprise.apps.demoapp.R
import com.applovin.enterprise.apps.demoapp.data.main.DemoMenuItem


abstract class DemoMenuActivity : AppCompatActivity()
{
    protected lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        listView = findViewById(R.id.list_view)

        setupListViewFooter()
        setupListViewContents(getListViewContents())
    }

    protected open fun setupListViewFooter()
    {
    }

    protected abstract fun getListViewContents(): Array<DemoMenuItem>

    private fun setupListViewContents(items: Array<DemoMenuItem>)
    {
        val listAdapter = object : ArrayAdapter<DemoMenuItem>(this, android.R.layout.simple_list_item_1, items)
        {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View
            {
                val inflater = this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                val row = convertView ?: inflater.inflate(android.R.layout.simple_list_item_1, parent, false)

                val item = items[position]

                val title: TextView = row.findViewById(android.R.id.text1)
                title.text = item.title

                return row
            }
        }
        listView.adapter = listAdapter

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            if (items[position].intent != null)
                startActivity(items[position].intent)
        }
    }
}