package com.applovin.enterprise.apps.demoapp.data.home;

/**
 * A {@link ListItem} representing a section header on the home screen
 * <p>
 * Created by santoshbagadi on 2019-09-10.
 */
public class SectionHeader
        implements ListItem
{
    private final String title;

    public SectionHeader(final String title)
    {
        this.title = title;
    }

    /**
     * @return The time of the section header.
     */
    public String getTitle()
    {
        return title;
    }

    @Override
    public int getType()
    {
        return TYPE_SECTION_HEADER;
    }
}
