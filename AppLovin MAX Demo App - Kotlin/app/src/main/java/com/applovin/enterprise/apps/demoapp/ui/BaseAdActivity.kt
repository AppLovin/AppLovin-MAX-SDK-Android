package com.applovin.enterprise.apps.demoapp.ui

import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_interstitial_ad.*

/**
 * Base for activities showing ads.
 * <p>
 * Created by Harry Arakkal on 2019-10-18.
 */
abstract class BaseAdActivity : AppCompatActivity()
{
    private lateinit var callbacksAdapter: CallbacksRecyclerViewAdapter
    private val callbacks: MutableList<String> = mutableListOf()

    /**
     * Setup callbacks RecyclerView adapter and appearance.
     */
    protected fun setupCallbacksRecyclerView()
    {
        callbacksAdapter = CallbacksRecyclerViewAdapter(callbacks, this)
        val manager = LinearLayoutManager(this)
        val decoration = DividerItemDecoration(this, manager.orientation)

        callbacksRecyclerView.apply {
            setHasFixedSize(true)
            adapter = callbacksAdapter
            layoutManager = manager
            addItemDecoration(decoration)
        }
    }

    /**
     * Log ad callbacks in the RecyclerView.
     * Uses the name of the function that calls this one in the log.
     */
    protected fun logCallback()
    {
        val callbackName = Throwable().stackTrace[1].methodName
        callbacks.add(callbackName)

        callbacksAdapter.notifyItemInserted(callbacks.lastIndex)
    }

    protected fun logAnonymousCallback()
    {
        val callbackName = Throwable().stackTrace[2].methodName
        callbacks.add(callbackName)

        callbacksAdapter.notifyItemInserted(callbacks.lastIndex)
    }
}
