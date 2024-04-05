package com.applovin.mediation.adapters;

import com.applovin.sdk.AppLovinSdk;

/**
 * // NOTE: We need another class name to access `BigoAdsMediationAdapter` from the Mediation Debugger because there is currently a naming conflict.
 */
public class MDBigoAdsMediationAdapter
        extends BigoAdsMediationAdapter
{
    public MDBigoAdsMediationAdapter(final AppLovinSdk sdk)
    {
        super( sdk );
    }
}
