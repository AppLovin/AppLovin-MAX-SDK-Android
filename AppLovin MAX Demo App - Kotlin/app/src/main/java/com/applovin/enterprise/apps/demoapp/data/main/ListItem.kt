package com.applovin.enterprise.apps.demoapp.data.main

/**
 * Interface for all list items on the main screen.
 * <p>
 * Created by Harry Arakkal on 9/17/2019
 */
interface ListItem
{
    companion object
    {
        const val SECTION_HEADER = 0
        const val AD_ITEM = 1
        const val FOOTER = 2;
    }

    val type: Int
}
