# Changelog

## 3.7.1.0
* Certified with Verve SDK 3.7.1.

## 3.7.0.0
* Certified with Verve SDK 3.7.0.

## 3.6.2.0
* Certified with Verve SDK 3.6.2.

## 3.6.1.0
* Certified with Verve SDK 3.6.1.

## 3.6.0.0
* Certified with Verve SDK 3.6.0.
* Removed deprecated code paths based on the minimum supported AppLovin MAX SDK version 13.0.0.
* Updated ad display failed error code.

## 3.3.0.0
* Certified with Verve SDK 3.3.0.

## 3.2.1.1
* Introduced `verification.properties` to facilitate adapter ownership verification with Google SDK console.

## 3.2.1.0
* Certified with Verve SDK 3.2.1.
* Removed redundant log output when initialization was already completed.

## 3.2.0.0
* Certified with Verve SDK 3.2.0.
* Annotated all `Activity` parameters with `@Nullable` to better avoid potential NPEs.

## 3.1.0.0
* Certified with Verve SDK 3.1.0.

## 3.0.4.1
* Requires minimum AppLovin MAX SDK version be 13.0.0.
* Removed support for COPPA.

## 3.0.4.0
* Certified with Verve SDK 3.0.4.

## 3.0.2.0
* Certified with Verve SDK 3.0.2.
* Replace parameter type `Activity` with `Context` in `HyBidInterstitialAd()` and `HyBidRewardedAd()`.
* Remove passing of `Activity` contexts into Verve APIs.

## 3.0.0.0
* Certified with Verve SDK 3.0.0.
* Remove deprecated callbacks `onRewardedAdVideoStarted()` and `onRewardedAdVideoCompleted()`.
* Remove updating mute state due to deprecation of `HyBid.setVideoAudioStatus( AudioState )`.

## 2.21.1.0
* Certified with Verve SDK 2.21.1.

## 2.20.0.0
* Certified with Verve SDK 2.20.0.

## 2.19.0.0
* Certified with Verve SDK 2.19.0.

## 2.18.1.1
* Add support for binary consent state as a fallback option if the TCFv2 GDPR consent string is not present in Shared Preferences.
* Remove unnecessary MAX SDK version check.

## 2.18.1.0
* Certified with Verve SDK 2.18.1.

## 2.18.0.0
* Certified with Verve SDK 2.18.0.

## 2.17.0.0
* Certified with Verve SDK 2.17.0.

## 2.16.2.1
* Remove `consentDialogState` guard.

## 2.16.2.0
* Certified with Verve SDK 2.16.2.

## 2.16.1.1
* Update adapter error code mapping.

## 2.16.1.0
* Certified with Verve SDK 2.16.1.
* Add additional details for ad display failures.

## 2.16.0.0
* Certified with Verve SDK 2.16.0.

## 2.15.1.0
* Certified with Verve SDK 2.15.1.

## 2.15.0.0
* Certified with Verve SDK 2.15.0.
* Update consent status before collecting signal.
* Add support for passing local parameter "is_location_collection_enabled" to set `HyBid.setLocationUpdatesEnabled()`.

## 2.14.0.0
* Certified with Verve SDK 2.14.0.

## 2.13.1.1
* Call `adViewAd.setTrackingMethod( ImpressionTrackingMethod.AD_VIEWABLE );` on `HyBidAdView` banners/MRECs.

## 2.13.1.0
* Certified with Verve SDK 2.13.1.

## 2.13.0.0
* Certified with Verve SDK 2.13.0.

## 2.12.1.1
* Update ad display failed error code.

## 2.12.1.0
* Certified with Verve SDK 2.12.1.

## 2.11.1.3
* Add check for SDK initialization before loading an ad.

## 2.11.1.2
* Add support for location updates.

## 2.11.1.1
* Update user consent to not override existing GDPR and CCPA privacy strings.

## 2.11.1.0
* Certified with Verve SDK 2.11.1.

## 2.10.0.1
* Support for null `Activity` on init.

## 2.10.0.0
* Certified with Verve SDK 2.10.0.

## 2.9.0.0
* Certified with Verve SDK 2.9.0.
* Remove `checkExistence()` call from adapter.

## 2.5.2.1
* Add custom repository to Unity's Dependencies.xml.

## 2.5.2.0
* Initial commit.
