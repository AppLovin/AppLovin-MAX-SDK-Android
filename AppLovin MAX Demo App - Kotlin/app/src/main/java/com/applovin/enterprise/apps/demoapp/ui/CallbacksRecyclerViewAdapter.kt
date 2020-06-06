package com.applovin.enterprise.apps.demoapp.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

import com.applovin.enterprise.apps.demoapp.R

/**
 * [RecyclerView.Adapter] for the callback RecyclerView in ad activities.
 * <p>
 * Created by Harry Arakkal on 2019-10-18.
 */
class CallbacksRecyclerViewAdapter(private val callbacks: List<String>, context: Context)
    : RecyclerView.Adapter<CallbacksRecyclerViewAdapter.ViewHolder>()
{
    private val layoutInflater: LayoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        return ViewHolder(layoutInflater.inflate(R.layout.ad_callback_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int)
    {
        holder.callbackName.text = callbacks[position]
    }

    override fun getItemCount(): Int
    {
        return callbacks.count()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
    {
        val callbackName: TextView = view.findViewById(R.id.callbackName)
    }
}
