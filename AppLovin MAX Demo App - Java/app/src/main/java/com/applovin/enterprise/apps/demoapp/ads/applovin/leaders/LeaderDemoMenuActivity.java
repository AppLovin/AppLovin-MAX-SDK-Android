package com.applovin.enterprise.apps.demoapp.ads.applovin.leaders;

import android.content.Intent;

import com.applovin.enterprise.apps.demoapp.ads.applovin.DemoMenuActivity;
import com.applovin.enterprise.apps.demoapp.ads.applovin.DemoMenuItem;

public class LeaderDemoMenuActivity
        extends DemoMenuActivity
{
    @Override
    protected DemoMenuItem[] getListViewContents()
    {
        DemoMenuItem[] result = {
                new DemoMenuItem( "Programmatic", "Programmatically create an instance of it", new Intent( this, LeaderProgrammaticActivity.class ) ),
                new DemoMenuItem( "Layout Editor", "Create a Leader from the layout editor", new Intent( this, LeaderLayoutEditorActivity.class ) ),
        };
        return result;
    }
}
