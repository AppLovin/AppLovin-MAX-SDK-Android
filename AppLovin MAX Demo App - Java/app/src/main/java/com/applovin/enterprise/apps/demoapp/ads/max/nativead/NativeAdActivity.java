package com.applovin.enterprise.apps.demoapp.ads.max.nativead;

import android.content.Intent;

import com.applovin.enterprise.apps.demoapp.data.main.DemoMenuItem;
import com.applovin.enterprise.apps.demoapp.ui.DemoMenuActivity;

public class NativeAdActivity
        extends DemoMenuActivity
{
    @Override
    protected DemoMenuItem[] getListViewContents()
    {
        DemoMenuItem[] result = {
                new DemoMenuItem( "Manual API", new Intent( this, ManualNativeAdActivity.class ) ),
                new DemoMenuItem( "Manual Late Binding API", new Intent( this, ManualNativeLateBindingAdActivity.class ) ),
                new DemoMenuItem( "Recycler View Ad Placer", new Intent( this, RecyclerViewNativeAdActivity.class ) )
        };
        return result;
    }
}
