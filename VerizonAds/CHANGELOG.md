# Changelog

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
