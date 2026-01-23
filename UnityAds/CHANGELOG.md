# Changelog

## 4.16.6.0
* Certified with UnityAds SDK 4.16.6.

## 4.16.5.0
* Certified with UnityAds SDK 4.16.5.

## 4.16.4.0
* Certified with UnityAds SDK 4.16.4.

## 4.16.3.0
* Certified with UnityAds SDK 4.16.3.

## 4.16.2.0
* Certified with UnityAds SDK 4.16.2.

## 4.16.1.0
* Certified with UnityAds SDK 4.16.1.

## 4.16.0.0
* Certified with UnityAds SDK 4.16.0.

## 4.15.1.0
* Certified with UnityAds SDK 4.15.1.

## 4.15.0.1
* Updated to use Unity Ads's new `getToken` API that supports Ad Format for signal collection.

## 4.15.0.0
* Certified with UnityAds SDK 4.15.0.

## 4.14.2.0
* Certified with UnityAds SDK 4.14.2.

## 4.14.1.0
* Certified with UnityAds SDK 4.14.1.
* Removed deprecated code paths based on the minimum supported AppLovin MAX SDK version 13.0.0.

## 4.14.0.0
* Certified with UnityAds SDK 4.14.0.

## 4.13.2.0
* Certified with UnityAds SDK 4.13.2.

## 4.13.1.1
* Introduced `verification.properties` to facilitate adapter ownership verification with Google SDK console.

## 4.13.1.0
* Certified with UnityAds SDK 4.13.1.

## 4.13.0.0
* Certified with UnityAds SDK 4.13.0.

## 4.12.5.0
* Certified with UnityAds SDK 4.12.5.
* Removed redundant log output when initialization was already completed.

## 4.12.4.0
* Certified with UnityAds SDK 4.12.4.
* Annotated all `Activity` parameters with `@Nullable` to better avoid potential NPEs.

## 4.12.3.1
* Fix caught NPE caused by retrieving the application context with a null `Activity`.

## 4.12.3.0
* Certified with UnityAds SDK 4.12.3.

## 4.12.2.1
* Requires minimum AppLovin MAX SDK version be 13.0.0.
* Removed support for COPPA.

## 4.12.2.0
* Certified with UnityAds SDK 4.12.2, which includes a fix for the initialization issue caused by missing ProGuard rules introduced in version 4.11.0.

## 4.12.1.1
* Downgrade UnityAds SDK to 4.10.0 due to an initialization issue introduced in version 4.11.0 caused by missing ProGuard rules.

## 4.12.1.0
* Certified with UnityAds SDK 4.12.1.

## 4.12.0.0
* Certified with UnityAds SDK 4.12.0.

## 4.11.3.0
* Certified with UnityAds SDK 4.11.3.
* Remove deprecated callbacks `onRewardedAdVideoStarted()` and `onRewardedAdVideoCompleted()`.

## 4.10.0.0
* Certified with UnityAds SDK 4.10.0.

## 4.9.3.0
* Certified with UnityAds SDK 4.9.3.

## 4.9.2.1
* Add support for bidding on banners/MRECs.

## 4.9.2.0
* Certified with UnityAds SDK 4.9.2.

## 4.9.1.0
* Certified with UnityAds SDK 4.9.1.

## 4.9.0.1
* Downgrade UnityAds SDK to 4.8.0 due to signal collection issues with 4.9.0.

## 4.9.0.0
* Certified with UnityAds SDK 4.9.0.

## 4.8.0.0
* Certified with UnityAds SDK 4.8.0.
* Add the `onBannerShown()` callback.

## 4.7.1.0
* Certified with UnityAds SDK 4.7.1.
* Remove the `getPrivacySetting()` function and call privacy methods directly.
* Now requires MAX SDK version 9.14.0 or higher.

## 4.6.1.0
* Certified with UnityAds SDK 4.6.1.

## 4.6.0.0
* Certified with UnityAds SDK 4.6.0.

## 4.5.0.2
* Include a random ID for every interstitial and rewarded ad request to improve fill and tracking.

## 4.5.0.1
* Remove `consentDialogState` guard.

## 4.5.0.0
* Certified with UnityAds SDK 4.5.0.

## 4.4.1.0
* Certified with UnityAds SDK 4.4.1.

## 4.4.0.0
* Certified with UnityAds SDK 4.4.0.

## 4.3.0.0
* Certified with UnityAds SDK 4.3.0.

## 4.2.1.1
* Update privacy settings before collecting signal.

## 4.2.1.0
* Certified with UnityAds SDK 4.2.1.

## 4.2.0.0
* Certified with UnityAds SDK 4.2.0.

## 4.1.0.3
* Update ad display failed error code.

## 4.1.0.2
* Update error message from "No Activity" to "Missing Activity" for internal codebase consistency.

## 4.1.0.1
* Gracefully fail banner/MRECs ad load if `Activity` context is null.

## 4.1.0.0
* Certified with UnityAds SDK 4.1.0.
* Remove checks for UnityAds SDK being initialized before loading ads.

## 4.0.1.2
* Add support for COPPA.

## 4.0.1.1
* Fix privacy consent by using `commit()` after each value is set to the metadata.

## 4.0.1.0
* Certified with UnityAds SDK 4.0.1.

## 4.0.0.2
* Support for null `Activity` on init.

## 4.0.0.1
* Verify UnityAds SDK is initialized before loading ads.

## 4.0.0.0
* Certified with UnityAds SDK 4.0.0.
* Update to use consolidated `initialize()` API.

## 3.7.5.1
* Remove setting of bidding meta data.

## 3.7.5.0
* Certified with UnityAds SDK 3.7.5.

## 3.7.2.0
* Certified with UnityAds SDK 3.7.2.

## 3.7.1.1
* Fix signal collection by setting bidding meta data before initialization based on server parameters.

## 3.7.1.0
* Updated to use new APIs introduced in UnityAds SDK 3.7.0.
* Removed deprecated APIs and router.

## 3.6.0.1
* Initial release to Maven Central and not JCenter.
* Update bidding APIs to include a random ID.

## 3.6.0.0
* Add support for UnityAds interstitial and rewarded bidding.
* Add support to pass 3rd-party error code and description to SDK.

## 3.5.1.1
* Add support for Unity's new load callbacks since old callbacks aren't called for some load errors.

## 3.5.1.0
* Certified with UnityAds SDK 3.5.1.
* Switched to Unity's new load API for interstitials and rewarded videos.
* Implemented Unity's new initialization listener.

## 3.5.0.3
* Fix ProGuard issue by moving privacy settings code to adapter from router and using `getWrappingSdk().getConfiguration()` instead of `mSdk.getConfiguration()`.

## 3.5.0.2
* Only set user consent flag if GDPR applies.

## 3.5.0.1
* Downgrade UnityAds SDK to 3.4.8.

## 3.5.0.0
* Certified with UnityAds SDK 3.5.0.

## 3.4.8.3
* Fix edge case where ad hidden callback was not fired if the ad experienced an error.

## 3.4.8.2
* Update 10000000 version check to 9140000.
* Update initialization log.

## 3.4.8.1
* Update 91400 version check to 10000000.

## 3.4.8.0
* Certified with UnityAds SDK 3.4.8.

## 3.4.6.3
* Updated to not set privacy settings if null.

## 3.4.6.2
* Roll back privacy changes.

## 3.4.6.1
* Updated to not set privacy settings if null.

## 3.4.6.0
* Certified with UnityAds SDK 3.4.6.

## 3.4.2.1
* Fix code inconsistency with other adapters.
* Add placement ID to fullscreen ad load and display log messages.

## 3.4.2.0
* Certified with UnityAds SDK 3.4.2.

## 3.4.0.1
* Add support for CCPA.

## 3.4.0.0
* Certified with UnityAds SDK 3.4.0.

## 3.3.0.1
* Add support for UnityAds banners.

## 3.3.0.0
* Certified with UnityAds SDK 3.3.0.
* Updated the minimum required AppLovin SDK version to 9.5.0.

## 3.2.0.0
* Certified with UnityAds SDK 3.2.0.
* Add support for per-placement loading. Requires whitelisted game ID and 'enable_per_placement_load' server parameter set to true on initialize.

## 3.1.0.1
* Add support for initialization status.

## 3.1.0.0
* Certified with UnityAds SDK 3.1.0.
* Add Unity support for automatic dependency resolution. Please ensure that you are on the latest [AppLovin MAX Unity Plugin](https://bintray.com/applovin/Unity/applovin-max-unity-plugin).
* Add support for extra reward options.

## 3.0.3.0
* Certified with UnityAds SDK 3.0.3.

## 3.0.1.1
* Minor adapter improvements.

## 3.0.1.0
* Certified with UnityAds SDK 3.0.1.

## 3.0.0.3
* Use unique package name in Android Manifest.

## 3.0.0.2
* Removed Redundant `activity` tags from AndroidManifest.

## 3.0.0.1
* Added Proguard rules required by Unity Ads SDK.

## 3.0.0.0
* Initial commit.
