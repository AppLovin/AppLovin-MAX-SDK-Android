package com.applovin.enterprise.apps.demoapp.data.main;

import android.content.Intent;

/**
 * A {@link ListItem} representing an ad type on the main screen
 * <p>
 * Created by santoshbagadi on 2019-09-10.
 */
public class AdType
        implements ListItem
{
    private final String   adType;
    private final Intent   intent;
    private final Runnable runnable;

    public AdType(final String adType, final Intent intent)
    {
        this.adType = adType;
        this.intent = intent;
        runnable = null;
    }

    public AdType(final String adType, final Runnable runnable)
    {
        this.adType = adType;
        this.intent = null;
        this.runnable = runnable;
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

    public Runnable getRunnable()
    {
        return runnable;
    }
}
