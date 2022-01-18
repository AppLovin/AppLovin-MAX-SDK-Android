package com.applovin.enterprise.apps.demoapp.ads.max.nativead;

import android.content.Intent;

import com.applovin.enterprise.apps.demoapp.ads.applovin.DemoMenuActivity;
import com.applovin.enterprise.apps.demoapp.data.main.DemoMenuItem;

public class NativeAdActivity
        extends DemoMenuActivity
{
    @Override
    protected DemoMenuItem[] getListViewContents()
    {
        DemoMenuItem[] result = {
                new DemoMenuItem( "Template Native Ads", new Intent( this, TemplateNativeAdActivity.class ) ),
                new DemoMenuItem( "Manual Native Ads", new Intent( this, ManualNativeAdActivity.class ) ),
        };
        return result;
    }
}
