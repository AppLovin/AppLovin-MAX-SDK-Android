# Changelog

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
