package com.applovin.enterprise.apps.demoapp.ads.applovin.mrecs;

import android.content.Intent;

import com.applovin.enterprise.apps.demoapp.ads.applovin.DemoMenuActivity;
import com.applovin.enterprise.apps.demoapp.data.main.DemoMenuItem;

public class MrecDemoMenuActivity
    extends DemoMenuActivity
{
    @Override
    protected DemoMenuItem[] getListViewContents()
    {
        return new DemoMenuItem[] {
                new DemoMenuItem( "Programmatic", new Intent( this, MrecProgrammaticActivity.class ) ),
                new DemoMenuItem( "Layout Editor", new Intent( this, MrecLayoutEditorActivity.class ) ),
                new DemoMenuItem( "Zone Integration", new Intent( this, MrecZoneActivity.class ) ),
        };
    }
}
