package com.applovin.enterprise.apps.demoapp.data.main

import android.content.Intent

/**
 * A [ListItem] representing an ad type on the main screen.
 * <p>
 * Created by Harry Arakkal on 09/17/2019.
 */
data class AdType(val adType: String, val intent: Intent?, val onTap: (() -> Unit)?, override val type: Int = ListItem.AD_ITEM) : ListItem
{
    constructor(adType: String, intent: Intent) : this(adType, intent, null)
    constructor(adType: String, f: () -> Unit) : this(adType, null, f)

    fun onTap()
    {
        onTap?.let { it() }
    }
}
