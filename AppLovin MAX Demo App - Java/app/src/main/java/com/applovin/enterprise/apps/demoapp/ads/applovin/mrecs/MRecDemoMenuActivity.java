package com.applovin.enterprise.apps.demoapp.ads.applovin.mrecs;

import android.content.Intent;

import com.applovin.enterprise.apps.demoapp.ads.applovin.DemoMenuActivity;
import com.applovin.enterprise.apps.demoapp.data.main.DemoMenuItem;

/**
 * Created by monica on 7/24/17.
 */

public class MRecDemoMenuActivity
        extends DemoMenuActivity
{
    @Override
    protected DemoMenuItem[] getListViewContents()
    {
        return new DemoMenuItem[] {
                new DemoMenuItem( "Programmatic", new Intent( this, MRecProgrammaticActivity.class ) ),
                new DemoMenuItem( "Layout Editor", new Intent( this, MRecLayoutEditorActivity.class ) ),
        };
    }
}
