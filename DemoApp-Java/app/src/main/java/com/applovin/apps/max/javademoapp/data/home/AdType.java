package com.applovin.apps.max.javademoapp.data.home;

import android.app.Activity;

/**
 * A {@link ListItem} representing an ad type on the home screen
 * <p>
 * Created by santoshbagadi on 2019-09-10.
 */
public class AdType
        implements ListItem
{
    private final String                    adType;
    private final Class<? extends Activity> activityToLaunch;

    public AdType(final String adType, final Class<? extends Activity> activityToLaunch)
    {
        this.adType = adType;
        this.activityToLaunch = activityToLaunch;
    }

    /**
     * @return The ad type that will be shown.
     */
    public String getAdType()
    {
        return adType;
    }

    /**
     * @return An {@link Activity} class to which to navigate to when this item is clicked.
     */
    public Class<? extends Activity> getActivityToLaunch()
    {
        return activityToLaunch;
    }

    @Override
    public int getType()
    {
        return TYPE_AD_ITEM;
    }
}
