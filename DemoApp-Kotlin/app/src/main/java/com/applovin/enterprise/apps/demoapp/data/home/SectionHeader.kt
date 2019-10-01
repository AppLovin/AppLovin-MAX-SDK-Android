package com.applovin.enterprise.apps.demoapp.data.home

/**
 * A [ListItem] representing a section header on the home screen
 * <p>
 * Created by Harry Arakkal on 9/17/2019.
 */
data class SectionHeader(val title: String) : ListItem
{
    override val type: Int = ListItem.SECTION_HEADER
}
