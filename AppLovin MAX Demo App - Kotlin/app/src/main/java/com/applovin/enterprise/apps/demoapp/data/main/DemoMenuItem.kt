package com.applovin.enterprise.apps.demoapp.data.main

import android.content.Intent

/**
 * A [ListItem] representing an ad type on the main screen.
 * <p>
 * Created by Harry Arakkal on 09/17/2019.
 */
data class DemoMenuItem(val title: String, val intent: Intent?, val runnable:Runnable?, override val type: Int = ListItem.AD_ITEM) : ListItem
{
    constructor(title: String, intent: Intent) : this(title, intent, null)
    constructor(title: String, runnable:Runnable) : this(title, null, runnable)
}
