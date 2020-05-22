package com.applovin.enterprise.apps.demoapp.ads.applovin.mrecs;

import android.content.Intent;

import com.applovin.enterprise.apps.demoapp.ads.applovin.DemoMenuActivity;
import com.applovin.enterprise.apps.demoapp.ads.applovin.DemoMenuItem;

/**
 * Created by monica on 7/24/17.
 */

public class MRecDemoMenuActivity
        extends DemoMenuActivity
{
    @Override
    protected DemoMenuItem[] getListViewContents()
    {
        DemoMenuItem[] result = {
                new DemoMenuItem( "Programmatic", "Programmatically creating an instance of it", new Intent( this, MRecProgrammaticActivity.class ) ),
                new DemoMenuItem( "Layout Editor", "Create an MRec from the layout editor", new Intent( this, MRecLayoutEditorActivity.class ) ),
        };
        return result;
    }
}
