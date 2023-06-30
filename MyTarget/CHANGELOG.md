# Changelog

## 5.17.0.0
* Certified with MyTarget SDK 5.17.0.

## 5.16.5.0
* Certified with MyTarget SDK 5.16.5.
* Remove the `getPrivacySetting()` function and call privacy methods directly.
* Now requires MAX SDK version 9.14.0 or higher.

## 5.16.4.0
* Certified with MyTarget SDK 5.16.4.

## 5.16.3.1
* Remove `consentDialogState` guard.

## 5.16.3.0
* Certified with MyTarget SDK 5.16.3.

## 5.16.2.0
* Certified with MyTarget SDK 5.16.2.

## 5.16.1.0
* Certified with MyTarget SDK 5.16.1.

## 5.16.0.1
* Support for native ads in external plugins (e.g. React Native).

## 5.16.0.0
* Certified with MyTarget SDK 5.16.0.
* Add additional details for ad display failures.

## 5.15.5.0
* Certified with MyTarget SDK 5.15.5.

## 5.15.4.0
* Certified with MyTarget SDK 5.15.4.

## 5.15.1.7
* Update privacy settings before collecting signal.

## 5.15.1.6
* Add support for returning the main image asset in `MaxNativeAd` for native ads.

## 5.15.1.5
* Add support for providing native media content aspect ratio in `MaxNativeAdView`.

## 5.15.1.4
* Update ad display failed error code.

## 5.15.1.3
* Remove check for manual native ad assets.

## 5.15.1.2
* Add support for `null` Activity context for signal collection, regular banners/MRECs, and native ads.

## 5.15.1.1
* Downgrade MyTarget SDK to 5.15.0, because 5.15.1 fails to build due to ExoPlayer version conflicts.

## 5.15.1.0
* Certified with MyTarget SDK 5.15.1.

## 5.15.0.3
* Add support for Binary CCPA.

## 5.15.0.2
* Support for null `Activity` on init.

## 5.15.0.1
* Fix a bug where certain native ad assets (icon, MediaView) are obstructed by others.

## 5.15.0.0
* Certified with MyTarget SDK 5.15.0.

## 5.14.4.3
* Add SDK initialization code: `MyTargetManager.initSdk()`.

## 5.14.4.2
* Fix icon view never getting registered for interaction.

## 5.14.4.1
* Added support for native bidding.

## 5.14.4.0
* Certified with MyTarget SDK 5.14.4.
* Remove `checkExistence()` call from adapter.

## 5.14.3.0
* Certified with MyTarget SDK 5.14.3.

## 5.14.2.0
* Certified with MyTarget SDK 5.14.2.

## 5.14.1.0
* Certified with MyTarget SDK 5.14.1.

## 5.13.4.0
* Certified with MyTarget SDK 5.13.4.

## 5.13.3.0
* Certified with MyTarget SDK 5.13.3.

## 5.13.1.0
* Certified with MyTarget SDK 5.13.1.
* Updated `setDebugMode` to reference `MyTargetManager`.

## 5.11.12.1
* Initial release to Maven Central and not JCenter.

## 5.11.12.0
* Certified with MyTarget SDK 5.11.12.

## 5.11.11.0
* Certified with MyTarget SDK 5.11.11.

## 5.11.10.0
* Certified with MyTarget SDK 5.11.10.
* Add support to pass 3rd-party error code and description to SDK.

## 5.11.9.0
* Certified with MyTarget SDK 5.11.9.

## 5.11.8.0
* Certified with MyTarget SDK 5.11.8.

## 5.11.7.0
* Certified with MyTarget SDK 5.11.7.

## 5.11.6.0
* Certified with MyTarget SDK 5.11.6.

## 5.11.5.1
* Only set user consent flag if GDPR applies.

## 5.11.5.0
* Certified with MyTarget SDK 5.11.5.

## 5.11.4.1
* Added support for header bidding.
* Updated to new `MyTargetView` and `RewardedAd` APIs.
* Added `setDebugMode` based on `isTesting` in initialize method.

## 5.11.4.0
* Certified with MyTarget SDK 5.11.4.

## 5.11.3.0
* Certified with MyTarget SDK 5.11.3.

## 5.11.2.0
* Certified with MyTarget SDK 5.11.2.

## 5.9.1.2
* Update 10000000 version check to 9140000.

## 5.9.1.1
* Update 91400 version check to 10000000.

## 5.9.1.0
* Certified with MyTarget SDK 5.9.1.

## 5.7.1.3
* Updated to not set privacy settings if null.

## 5.7.1.2
* Roll back privacy changes.

## 5.7.1.1
* Updated to not set privacy settings if null.

## 5.7.1.0
* Certified with myTarget SDK 5.7.1.

## 5.6.3.0
* Certified with myTarget SDK 5.6.3.

## 5.6.2.0
* Certified with myTarget SDK 5.6.2.
* Migrated to AndroidX.

## 5.5.3.0
* Certified with myTarget SDK 5.5.3.

## 5.4.7.0
* Certified with myTarget SDK 5.4.7.
* Remove unused server parameters.

## 5.4.5.0
* Initial commit.
