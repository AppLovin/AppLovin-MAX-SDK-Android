package com.applovin.enterprise.apps.demoapp.ads.max.mrecs;

import android.content.Intent;

import com.applovin.enterprise.apps.demoapp.ads.applovin.DemoMenuActivity;
import com.applovin.enterprise.apps.demoapp.data.main.DemoMenuItem;

public class MrecAdActivity
        extends DemoMenuActivity
{
    @Override
    protected DemoMenuItem[] getListViewContents()
    {
        DemoMenuItem[] result = {
                new DemoMenuItem( "Programmatic MRECs", new Intent( this, ProgrammaticMrecAdActivity.class ) ),
                new DemoMenuItem( "Layout Editor MRECs", new Intent( this, LayoutEditorMrecAdActivity.class ) ),
                new DemoMenuItem( "Recycler View MRECs", new Intent( this, RecyclerViewMrecAdActivity.class ) ),
        };
        return result;
    }
}
