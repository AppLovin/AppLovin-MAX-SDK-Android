# Changelog

## 8.2.1.0.0
* Certified with IronSource SDK 8.2.1.

## 8.1.0.0.1
* Add support for bidding.

## 8.1.0.0.0
* Certified with IronSource SDK 8.1.0.

## 8.0.0.0.0
* Certified with IronSource SDK 8.0.0.
* Remove deprecated callbacks `onRewardedAdVideoStarted()` and `onRewardedAdVideoCompleted()`.
* Requires minimum Android API level be 19 or higher.

## 7.9.0.0.0
* Certified with IronSource SDK 7.9.0.

## 7.8.1.0.0
* Certified with IronSource SDK 7.8.1.

## 7.8.0.0.0
* Certified with IronSource SDK 7.8.0.
* Fixed to allow multiple banners/MRECs with same instanceId in a session to load by destroying previous banners/MRECs using `destroyISDemandOnlyBanner()` API.

## 7.7.0.0.0
* Certified with IronSource SDK 7.7.0.
* Correctly map `ERROR_BN_INSTANCE_LOAD_AUCTION_FAILED` and `BN_INSTANCE_LOAD_NO_FILL` to MAX NO FILLs instead of unspecified errors.

## 7.6.0.0.0
* Certified with IronSource SDK 7.6.0.

## 7.5.2.0.0
* Certified with IronSource SDK 7.5.2.

## 7.5.1.0.1
* Set `is_deviceid_optout` parameter for COPPA end users.

## 7.5.1.0.0
* Certified with IronSource SDK 7.5.1.

## 7.5.0.0.1
* Remove activity lifecycle callbacks to prevent memory leak.

## 7.5.0.0.0
* Certified with IronSource SDK 7.5.0.

## 7.4.0.0.1
* Add support for bidding on banners/MRECs, interstitials, and rewarded ads.

## 7.4.0.0.0
* Certified with IronSource SDK 7.4.0.

## 7.3.1.1.0
* Certified with IronSource SDK 7.3.1.1.

## 7.3.1.0.0
* Certified with IronSource SDK 7.3.1.

## 7.3.0.1.0
* Certified with IronSource SDK 7.3.0.1.

## 7.3.0.1.0
* Certified with IronSource SDK 7.3.0.1.
* Remove the `getPrivacySetting()` function and call privacy methods directly.
* Now requires MAX SDK version 9.14.0 or higher.

## 7.2.7.0.0
* Certified with IronSource SDK 7.2.7.

## 7.2.6.0.1
* Remove `consentDialogState` guard.

## 7.2.6.0.0
* Certified with IronSource SDK 7.2.6.

## 7.2.5.0.1
* Add support for banners and MRECs.
* Add additional details for ad display failures.

## 7.2.5.0.0
* Certified with IronSource SDK 7.2.5.

## 7.2.4.1.0
* Certified with IronSource SDK 7.2.4.1.

## 7.2.4.0.0
* Certified with IronSource SDK 7.2.4.

## 7.2.3.1.0
* Certified with IronSource SDK 7.2.3.1.

## 7.2.3.0.0
* Certified with IronSource SDK 7.2.3.

## 7.2.2.1.0
* Certified with IronSource SDK 7.2.2.1.

## 7.2.2.0.0
* Certified with IronSource SDK 7.2.2.

## 7.2.1.1.1
* Update ad display failed error code.

## 7.2.1.1.0
* Certified with IronSource SDK 7.2.1.1.

## 7.2.1.0.0
* Certified with IronSource SDK 7.2.1.

## 7.2.0.0.0
* Certified with IronSource SDK 7.2.0.

## 7.1.14.0.2
* Support for null `Activity` on init.

## 7.1.14.0.1
* Fix rewarded ad listener not being correctly cleared.

## 7.1.14.0.0
* Certified with IronSource SDK 7.1.14.
* Remove `checkExistence()` call from adapter.

## 7.1.10.1.0
* Certified with ironSource SDK 7.1.10.1.

## 7.1.5.1.3
* Updated `initISDemandOnly()` API to use application context as a parameter instead of Activity context to prevent memory leak.

## 7.1.5.1.2
* Update ironSource's custom Maven repo for their SDK (updated to: https://android-sdk.is.com/).

## 7.1.5.1.1
* Use ironSource's custom Maven repo for their SDK (updated to: https://raw.githubusercontent.com/ironSource-mobile/android-sdk/master).

## 7.1.5.1.0
* Certified with ironSource SDK 7.1.5.1.

## 7.1.5.0.0
* Certified with ironSource SDK 7.1.5.

## 7.1.4.1.0
* Certified with ironSource SDK 7.1.4.1.

## 7.1.4.0.0
* Certified with ironSource SDK 7.1.4.

## 7.1.3.0.0
* Certified with ironSource SDK 7.1.3.

## 7.1.2.0.0
* Certified with ironSource SDK 7.1.2.
* Support flexible selective ad format initialization for ironSource SDK.

## 7.1.1.0.1
* Support new init and load API.
* Added COPPA support.

## 7.1.1.0.0
* Certified with ironSource SDK 7.1.1.
* Initial release to Maven Central and not JCenter.

## 7.1.0.2.0
* Certified with ironSource SDK 7.1.0.2.

## 7.1.0.1.0
* Certified with ironSource SDK 7.1.0.1.

## 7.1.0.0.0
* Certified with ironSource SDK 7.1.0.
* Add support to pass 3rd-party error code and description to SDK.

## 7.0.4.1.0
* Certified with ironSource SDK 7.0.4.1.

## 7.0.4.0.0
* Certified with ironSource SDK 7.0.4.

## 7.0.3.1.3
* Only call `setMetaData()` before initializing ironSource SDK.

## 7.0.3.1.2
* Only set user consent flag if GDPR applies.

## 7.0.3.1.1
* Removed repository from pom file.

## 7.0.3.1.0
* Certified with ironSource SDK 7.0.3.1.

## 7.0.3.0.0
* Certified with ironSource SDK 7.0.3.0.

## 7.0.1.1.1
* Update 10000000 version check to 9140000.
* Update initialization log.

## 7.0.1.1.0
* Certified with ironSource SDK 7.0.1.1.

## 7.0.1.0.0
* Certified with ironSource SDK 7.0.1.0.
