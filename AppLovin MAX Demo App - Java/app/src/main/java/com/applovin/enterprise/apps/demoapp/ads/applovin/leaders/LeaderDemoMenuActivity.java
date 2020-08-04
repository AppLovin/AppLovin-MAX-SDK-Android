package com.applovin.enterprise.apps.demoapp.ads.applovin.leaders;

import android.content.Intent;

import com.applovin.enterprise.apps.demoapp.ads.applovin.DemoMenuActivity;
import com.applovin.enterprise.apps.demoapp.data.main.DemoMenuItem;

public class LeaderDemoMenuActivity
        extends DemoMenuActivity
{
    @Override
    protected DemoMenuItem[] getListViewContents()
    {
        return new DemoMenuItem[] {
                new DemoMenuItem( "Programmatic", new Intent( this, LeaderProgrammaticActivity.class ) ),
                new DemoMenuItem( "Layout Editor", new Intent( this, LeaderLayoutEditorActivity.class ) ),
        };
    }
}
