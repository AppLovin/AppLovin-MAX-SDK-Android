package com.applovin.enterprise.apps.demoapp.ads.applovin;

import android.content.Intent;

public class DemoMenuItem
{
    private final String title;
    private final String subtitle;
    private final Intent intent;

    public DemoMenuItem(final String title, final String subtitle, final Intent intent)
    {
        this.title = title;
        this.subtitle = subtitle;
        this.intent = intent;
    }

    public String getTitle()
    {
        return title;
    }

    public String getSubtitle()
    {
        return subtitle;
    }

    public Intent getIntent()
    {
        return intent;
    }
}