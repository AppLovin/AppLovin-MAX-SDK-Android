# Changelog

## 2.1.1.0
* Certified with Yahoo Mobile SDK 1.1.1.

## 2.0.0.11
* Update privacy settings before collecting signal.

## 2.0.0.10
* Explicitly set native activity's ActivityState to `RESUMED`.

## 2.0.0.9
* Add support for passing an Activity to Yahoo's SDK for native ad view impression tracking.
* Use local scope copy of native ad while preparing view.

## 2.0.0.8
* Fix native media content sizing issue.

## 2.0.0.7
* Add support for passing local parameter "is_location_collection_enabled" to set `YASAds.setLocationAccessMode(...)`.

## 2.0.0.6
* Add support for returning the main image asset in `MaxNativeAd` for native ads.

## 2.0.0.5
* Add support for IAB's TCFv2 GDPR consent string. Note that you must be on the AppLovin MAX SDK v11.4.3+ and use a TCFv2-compliant framework which stores the consent string in SharedPreferences via the `IABTCF_TCString` key to use this feature.

## 2.0.0.4
* Add support for preparing video, image and icon components.
* Do not explicitly destroy Yahoo native ads, as it will clear previously-registered `TextView`s and `Buttons`.

## 2.0.0.3
* Fix native ads not tracking impressions properly.

## 2.0.0.2
* Fixed Native Ads NPE: `Fatal Exception: java.lang.NullPointerException: Attempt to invoke virtual method 'void com.yahoo.ads.nativeplacement.b.w()' on a null object reference`.

## 2.0.0.1
* Add backwards compatibility support for Native Ads.

## 2.0.0.0
* Certified with Yahoo Mobile SDK 1.0.0.

## 1.14.0.13
* Add support for providing native media content aspect ratio in `MaxNativeAdView`.

## 1.14.0.12
* Add support for null `Activity` context for signal collection, native ads and regular banner/MRECs.

## 1.14.0.11
* Update ad display failed error code.

## 1.14.0.10
* Add support for tracking impressions for all ad formats.

## 1.14.0.9
* Add support for tracking banner ad impressions.

## 1.14.0.8
* Remove check for manual native ad assets.

## 1.14.0.7
* Remove support for `consent_string`.

## 1.14.0.6
* Add support for IAB's CCPA Privacy String.

## 1.14.0.5
* Fix native ad support.

## 1.14.0.4
* Support for null `Activity` on init.

## 1.14.0.3
* Update signal collection method.

## 1.14.0.2
* Add support for rewarded ads.
* Add support for native ads.
* Add support for passing creative id to SDK (supported in Android SDK 9.15.0+).

## 1.14.0.1
* Updated ProGuard rules for loading ads.

## 1.14.0.0
* Certified with VerizonAds SDK 1.14.0.

## 1.13.0.0
* Certified with VerizonAds SDK 1.13.0.

## 1.12.1.0
* Certified with VerizonAds SDK 1.12.1.

## 1.12.0.0
* Certified with VerizonAds SDK 1.12.0.

## 1.11.0.1
* Fix Android dependency for Unity adapter.

## 1.11.0.0
* Certified with VerizonAds SDK 1.11.0.
* Use Verizon's custom Maven repo for their SDK.
* Initial release to Maven Central and not JCenter.

## 1.9.0.0
* Certified with VerizonAds SDK 1.9.0.
* Add support to pass 3rd-party error code and description to SDK.

## 1.8.2.0
* Certified with VerizonAds SDK 1.8.2.

## 1.8.1.0
* Certified with VerizonAds SDK 1.8.1.

## 1.8.0.0
* Certified with VerizonAds SDK 1.8.0.

## 1.6.0.5
* Update 10000000 version check to 9140000.
* Update initialization log.

## 1.6.0.4
* Update 91400 version check to 10000000.

## 1.6.0.3
* Updated to not set privacy settings if null.

## 1.6.0.2
* Roll back privacy changes.

## 1.6.0.1
* Updated to not set privacy settings if null.

## 1.6.0.0
* Certified with VerizonAds SDK 1.6.0.
* Certified with bidding token version 1.1.

## 1.5.0.4
* Generate bid token in the adapter itself.

## 1.5.0.3
* Fixed bidding signal not being compressed correctly.

## 1.5.0.2
* Updated bidding signal compression encoding from `Base64.DEFAULT` to `Base64.NO_WRAP`.

## 1.5.0.1
* Compress the generated bidding signal.

## 1.5.0.0
* Certified with VerizonAds SDK 1.5.0.
* Added support for new header bidding APIs.

## 1.4.0.0
* Certified with VerizonAds SDK 1.4.0.
* Updated logs to differentiate ad view formats.

## 1.3.0.0
* Certified with VerizonAds SDK 1.3.0 (with privacy API changes).
* Remove support for passing in whether GDPR applies or not.

## 1.2.1.2
* Add proguard rule for Mediation Debugger support.

## 1.2.1.1
* Removed `WRITE_EXTERNAL_STORAGE` permission that is no longer needed by VerizonAds SDK from adapter's Android Manifest.

## 1.2.1.0
* Certified with VerizonAds SDK 1.2.1.

## 1.2.0.0
* Certified with VerizonAds SDK 1.2.0.

## 1.1.4.0
* Certified with VerizonAds SDK 1.1.4.
* Fixed an issue where banner ads were not being destroyed.

## 1.1.3.0
* Certified with VerizonAds SDK 1.1.3.
* Add support for initialization status.

## 1.1.1.0
* Initial commit.
