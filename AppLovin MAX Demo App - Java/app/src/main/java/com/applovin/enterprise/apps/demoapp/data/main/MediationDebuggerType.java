package com.applovin.enterprise.apps.demoapp.data.main;

public class MediationDebuggerType
        implements ListItem
{
    private String title;

    public MediationDebuggerType(final String title)
    {
        this.title = title;
    }

    public String getTitle()
    {
        return title;
    }

    @Override
    public int getType()
    {
        return TYPE_MEDIATION_DEBUGGER;
    }
}
