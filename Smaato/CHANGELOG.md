# Changelog

## 21.8.5.0
* Certified with Smaato SDK 21.8.5.

## 21.8.4.0
* Certified with Smaato SDK 21.8.4.

## 21.8.3.2
* Use local scope copy of native ad while preparing view.
* Update privacy settings before collecting signal.

## 21.8.3.1
* Add support for returning the main image asset in `MaxNativeAd` for native ads.

## 21.8.3.0
* Certified with Smaato SDK 21.8.3.

## 21.8.2.1
* Update ad display failed error code.

## 21.8.2.0
* Certified with Smaato SDK 21.8.2.

## 21.8.1.2
* Remove check for manual native ad assets.

## 21.8.1.1
* Add support for `null` Activity context for signal collection, regular banners/MRECs, and native ads.

## 21.8.1.0
* Certified with Smaato SDK 21.8.1.

## 21.8.0.1
* Add debug log for "is_location_collection_enabled" value.

## 21.8.0.0
* Certified with Smaato SDK 21.8.0.

## 21.7.3.2
* Downgrade Smaato SDK to 21.6.7, because 21.7.3 fails to build for Unity IDE.

## 21.7.3.1
* Add support for passing local parameter "is_location_collection_enabled" to set `SmaatoSdk.setGPSEnabled(...)` for bidding.

## 21.7.3.0
* Certified with Smaato SDK 21.7.3.

## 21.6.7.3
* Add support for passing local parameter "is_location_collection_enabled" to set `SmaatoSdk.setGPSEnabled(...)`.

## 21.6.7.2
* Remove code for removing legacy privacy settings `"IABConsent_SubjectToGDPR"` and `"IABConsent_ConsentString"`.
* Support for null `Activity` on init.

## 21.6.7.1
* Add support for native custom ads and native template ads.

## 21.6.7.0
* Certified with Smaato SDK 21.6.7.

## 21.6.6.1
* Add signal collection.

## 21.6.6.0
* Certified with Smaato SDK 21.6.6.

## 21.6.4.0
* Certified with Smaato SDK 21.6.4.

## 21.6.3.0
* Certified with Smaato SDK 21.6.3.

## 21.6.2.0
* Certified with Smaato SDK 21.6.2.

## 21.6.1.0
* Certified with Smaato SDK 21.6.1.

## 21.6.0.0
* Certified with Smaato SDK 21.6.0.

## 21.5.10.0
* Certified with Smaato SDK 21.5.10.

## 21.5.9.0
* Certified with Smaato SDK 21.5.9.

## 21.5.8.1
* Fixed a bug where every second ad was failing for interstitial or rewarded.

## 21.5.8.0
* Certified with Smaato SDK 21.5.8.

## 21.5.7.2
* Initial release to Maven Central and not JCenter.
* Update ad load to fail if bid response is valid but ad request creation is not.
* Update ad load to use old API if bid response is not valid.

## 21.5.7.1
* Add support for passing creative id to SDK (supported in Android SDK 9.15.0+).

## 21.5.7.0
* Certified with Smaato SDK 21.5.7.
* Add support to pass 3rd-party error code and description to SDK.

## 21.5.6.0
* Certified with Smaato SDK 21.5.6.

## 21.5.5.0
* Certified with Smaato SDK 21.5.5.

## 21.5.4.0
* Certified with Smaato SDK 21.5.4.

## 21.5.3.0
* Certified with Smaato SDK 21.5.3.

## 21.5.2.5
* Update 10000000 version check to 9140000.
* Update initialization log.

## 21.5.2.4
* Remove unsupported GDPR consent setting code and remove previously set values.

## 21.5.2.3
* Update 91400 version check to 10000000.

## 21.5.2.2
* Add support for bidding (signal collection not required).

## 21.5.2.1
* Updated to not set age restriction setting if null.

## 21.5.2.0
* Certified with Smaato SDK 21.5.2.

## 21.5.1.3
* Roll back privacy settings.

## 21.5.1.2
* Updated to not set age restriction setting if null.

## 21.5.1.1
* Add ProGuard rules.
* Remove Activity declarations from Android Manifest file.

## 21.5.1.0
* Certified with Smaato SDK 21.5.1.
* Fix code inconsistency with other adapters.

## 21.3.8.0
* Certified with Smaato SDK 21.3.8.

## 21.3.7.0
* Certified with Smaato SDK 21.3.7.

## 21.3.6.0
* Certified with 21.3.6.
* Updated log to differentiate ad view formats.

## 21.3.1.0
* Certified with SDK 21.3.1. This SDK fixes ProGuard issues.

## 21.2.1.2
* Fix Smaato crashes at the recommendation of Smaato's SDK team by calling all Smaato SDK APIs _after_ `Smaato.init(...)`.

## 21.2.1.1
* Add proguard rule for Mediation Debugger support.

## 21.2.1.0
* Certified with SDK 21.2.1.

## 21.1.2.0
* Certified with SDK 21.1.2.
* Remove unused server parameters.
* Updated the minimum required AppLovin SDK version to 9.5.0.

## 9.1.6.0
* Certified with Smaato SDK 9.1.6.

## 9.1.5.6
* Add support for initialization status.

## 9.1.5.5
* Add Unity support for automatic dependency resolution. Please ensure that you are on the latest [AppLovin MAX Unity Plugin](https://bintray.com/applovin/Unity/applovin-max-unity-plugin).
* Add support for extra reward options.

## 9.1.5.4
* Fix `NoMethodFoundException`'s.

## 9.1.5.3
* GDPR fixes.

## 9.1.5.2
* GDPR fixes.

## 9.1.5.1
* Dynamically reference against Smaato SDK version number.

## 9.1.5.0
* Initial commit.
