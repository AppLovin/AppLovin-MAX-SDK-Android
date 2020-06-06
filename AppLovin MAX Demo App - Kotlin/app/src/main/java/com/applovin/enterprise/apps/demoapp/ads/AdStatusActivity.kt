package com.applovin.enterprise.apps.demoapp.ads

import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView

abstract class AdStatusActivity : AppCompatActivity()
{

    protected var adStatusTextView: TextView? = null

    protected fun log(message: String)
    {
        runOnUiThread { adStatusTextView?.text = message }
        println(message)
    }
}