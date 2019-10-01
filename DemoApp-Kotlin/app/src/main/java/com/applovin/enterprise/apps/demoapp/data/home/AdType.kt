package com.applovin.enterprise.apps.demoapp.data.home

import android.content.Intent

/**
 * A [ListItem] representing an ad type on the home screen.
 * <p>
 * Created by Harry Arakkal on 09/17/2019.
 */
data class AdType(val adType: String, val intent: Intent) : ListItem
{
    override val type: Int = ListItem.AD_ITEM
}
