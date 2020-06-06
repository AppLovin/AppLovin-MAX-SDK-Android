package com.applovin.enterprise.apps.demoapp.ads.applovin;

import android.content.Intent;

public class DemoMenuItem
{
    private final String title;
    private final Intent intent;

    public DemoMenuItem(final String title, final Intent intent)
    {
        this.title = title;
        this.intent = intent;
    }

    public String getTitle()
    {
        return title;
    }

    public Intent getIntent()
    {
        return intent;
    }
}