package com.applovin.apps.max.javademoapp.data.home;

import android.app.Activity;
import android.content.Intent;

/**
 * A {@link ListItem} representing an ad type on the home screen
 * <p>
 * Created by santoshbagadi on 2019-09-10.
 */
public class AdType
        implements ListItem
{
    private final String                    adType;
    private final Intent intent;

    public AdType(final String adType, final Intent intent)
    {
        this.adType = adType;
        this.intent = intent;
    }

    /**
     * @return The ad type that will be shown.
     */
    public String getAdType()
    {
        return adType;
    }

    /**
     * @return the intent to launch.
     */
    public Intent getIntent()
    {
        return intent;
    }

    @Override
    public int getType()
    {
        return TYPE_AD_ITEM;
    }
}
