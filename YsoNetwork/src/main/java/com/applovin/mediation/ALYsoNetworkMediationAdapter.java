package com.applovin.mediation;

import com.applovin.sdk.AppLovinSdk;
import com.applovin.mediation.adapters.YsoNetworkMediationAdapter;

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
