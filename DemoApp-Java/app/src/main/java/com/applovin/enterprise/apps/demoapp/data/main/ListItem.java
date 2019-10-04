package com.applovin.enterprise.apps.demoapp.data.main;

/**
 * All the list items on main screen should confirm to this interface.
 * <p>
 * Created by santoshbagadi on 2019-09-10.
 */
public interface ListItem
{
    int TYPE_SECTION_HEADER = 0;
    int TYPE_AD_ITEM        = 1;

    /**
     * @return The type of list item.
     */
    int getType();
}
