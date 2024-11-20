package com.applovin.mediation.adapters;

import android.app.Activity;

import com.applovin.impl.sdk.utils.BundleUtils;
import com.applovin.mediation.adapter.MaxSignalProvider;
import com.applovin.mediation.adapter.listeners.MaxSignalCollectionListener;
import com.applovin.mediation.adapter.parameters.MaxAdapterInitializationParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterSignalCollectionParameters;
import com.applovin.mediation.adapters.linkedin.BuildConfig;
import com.applovin.sdk.AppLovinSdk;
import com.linkedin.audiencenetwork.LinkedInAudienceNetwork;

import java.util.concurrent.atomic.AtomicBoolean;

public class LinkedInDSPAdapter
        extends MediationAdapterBase
        implements MaxSignalProvider
{
    private static final AtomicBoolean        initialized = new AtomicBoolean();
    private static       InitializationStatus status;

    public LinkedInDSPAdapter(final AppLovinSdk sdk) { super( sdk ); }

    //region Adapter Methods

    @Override
    public String getSdkVersion()
    {
        return LinkedInAudienceNetwork.INSTANCE.getVersion();
    }

    @Override
    public String getAdapterVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, final Activity activity, final OnCompletionListener onCompletionListener)
    {
        if ( initialized.compareAndSet( false, true ) )
        {
            status = InitializationStatus.INITIALIZING;

            d( "Initializing SDK..." );

            final String sdkKey = BundleUtils.getString( "sdk_key", parameters.getServerParameters() );
            LinkedInAudienceNetwork.INSTANCE.initialize( getApplicationContext(), getAdapterVersion(), sdkKey, success -> {

                if ( success )
                {
                    d( "SDK initialized" );
                    status = InitializationStatus.INITIALIZED_SUCCESS;
                }
                else
                {
                    e( "SDK failed to initialize" );
                    status = InitializationStatus.INITIALIZED_FAILURE;
                }

                onCompletionListener.onCompletion( status, null );

                return null;
            } );
        }
        else
        {
            onCompletionListener.onCompletion( status, null );
        }
    }

    @Override
    public void onDestroy() { }

    //endregion

    //region MaxSignalProvider Methods

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, final Activity activity, final MaxSignalCollectionListener callback)
    {
        log( "Collecting signal..." );

        callback.onSignalCollected( LinkedInAudienceNetwork.INSTANCE.getBidderToken() );
    }

    //endregion
}
