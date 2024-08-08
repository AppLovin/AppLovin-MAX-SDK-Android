package com.applovin.mediation.adapters;

import com.applovin.sdk.AppLovinSdk;

/**
 * NOTE: YSO initially named their adapter ALYsoNetworkMediationAdapter but our convention is YsoNetworkMediationAdapter. We will support both naming conventions.
 */
public class ALYsoNetworkMediationAdapter
        extends YsoNetworkMediationAdapter
{
    public ALYsoNetworkMediationAdapter(final AppLovinSdk sdk)
    {
        super( sdk );
    }
}
