package com.applovin.apps.max.javademoapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.applovin.apps.max.javademoapp.ads.BannerAdActivity;
import com.applovin.apps.max.javademoapp.ads.InterstitialAdActivity;
import com.applovin.apps.max.javademoapp.ads.RewardedAdActivity;
import com.applovin.apps.max.javademoapp.data.home.AdType;
import com.applovin.apps.max.javademoapp.data.home.ListItem;
import com.applovin.apps.max.javademoapp.data.home.SectionHeader;
import com.applovin.apps.max.javademoapp.ui.HomeRecyclerViewAdapter;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * The home {@link android.app.Activity} of this app.
 * <p>
 * Created by santoshbagadi on 2019-09-10.
 */
public class HomeActivity
        extends AppCompatActivity
        implements HomeRecyclerViewAdapter.OnHomeListItemClickListener
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_home );
        Toolbar toolbar = findViewById( R.id.toolbar );
        setSupportActionBar( toolbar );

        final HomeRecyclerViewAdapter adapter = new HomeRecyclerViewAdapter( generateHomeListItems(), this, this );
        final LinearLayoutManager manager = new LinearLayoutManager( this );
        final DividerItemDecoration decoration = new DividerItemDecoration( this, manager.getOrientation() );

        final RecyclerView recyclerView = findViewById( R.id.home_recycler_view );
        recyclerView.setHasFixedSize( true );
        recyclerView.setLayoutManager( manager );
        recyclerView.addItemDecoration( decoration );
        recyclerView.setItemAnimator( new DefaultItemAnimator() );
        recyclerView.setAdapter( adapter );
    }

    private static List<ListItem> generateHomeListItems()
    {
        final List<ListItem> items = new ArrayList<>();

        items.add( new SectionHeader( "MAX Ads" ) );
        items.add( new AdType( "Interstital", InterstitialAdActivity.class ) );
        items.add( new AdType( "Rewarded", RewardedAdActivity.class ) );
        items.add( new AdType( "Banners / Leaders", BannerAdActivity.class ) );

        return items;
    }

    @Override
    public void onItemClicked(final View view, final ListItem item)
    {
        if ( item.getType() == ListItem.TYPE_AD_ITEM )
        {
            final AdType adType = (AdType) item;
            final Intent intent = new Intent( this, adType.getActivityToLaunch() );
            startActivity( intent );
        }
    }
}
