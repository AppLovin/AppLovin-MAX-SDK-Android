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
    public interface OnTap
    {
        public void onTap();
    }

    private final String adType;
    private final Intent intent;
    private final OnTap  onTap;

    public AdType(final String adType, final Intent intent)
    {
        this.adType = adType;
        this.intent = intent;
        this.onTap = null;
    }

    public AdType(final String adType, final Runnable runnable)
    {
        this.adType = adType;
        this.intent = null;
        this.onTap = onTap;
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

    public void onTap()
    {
        this.onTap.onTap();
    }
}
