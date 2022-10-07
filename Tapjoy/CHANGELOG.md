# Changelog

## 12.11.0.0
* Certified with Tapjoy SDK 12.11.0.

## 12.10.0.2
* Set `Tapjoy.optOutAdvertisingID(...)` to `true` if COPPA, to comply with Google Families Program rules.

## 12.10.0.1
* Update ad display failed error code.

## 12.10.0.0
* Certified with Tapjoy SDK 12.10.0.

## 12.9.1.0
* Certified with Tapjoy SDK 12.9.1.

## 12.9.0.3
* Add support for IAB's CCPA Privacy String.

## 12.9.0.2
* Support for null `Activity` on init.

## 12.9.0.1
* Re-add setting of activity before ad loads, which was removed in 12.8.1.1.

## 12.9.0.0
* Certified with Tapjoy SDK 12.9.0.
* Remove `checkExistence()` call from adapter.

## 12.8.1.1
* Fix potential memory leaks by cleaning up placements and using application context for init.

## 12.8.1.0
* Certified with Tapjoy SDK 12.8.1.

## 12.8.0.0
* Certified with Tapjoy SDK 12.8.0.
* Use Tapjoy's custom Maven repo for their SDK.

## 12.7.1.2
* Initial release to Maven Central and not JCenter.
* Add support to pass 3rd-party error code and description to SDK.
* Use `onVideoStart()` for rewarded `MaxAdListener#onAdDisplayed()` callbacks.

## 12.7.1.1
* Remove support for setting user consent based on `"gdpr_applies"`.

## 12.7.1.0
* Certified with Tapjoy SDK 12.7.1.

## 12.7.0.1
* Downgrade Tapjoy SDK to 12.6.1.

## 12.7.0.0
* Certified with Tapjoy SDK 12.7.0.

## 12.6.1.5
* Update 10000000 version check to 9140000.
* Update initialization log.

## 12.6.1.4
* Update 91400 version check to 10000000.

## 12.6.1.3
* Updated to not set privacy settings if null.

## 12.6.1.2
* Roll back privacy settings.

## 12.6.1.1
* Updated to not set privacy settings if null.

## 12.6.1.0
* Certified with Tapjoy SDK 12.6.1.

## 12.6.0.1
* Update privacy APIs for GDPR.

## 12.6.0.0
* Certified with Tapjoy SDK 12.6.0.

## 12.4.2.0
* Certified with Tapjoy SDK 12.4.2.

## 12.4.1.0
* Certified with Tapjoy SDK 12.4.1.

## 12.4.0.0
* Certified with Tapjoy SDK 12.4.0.
* Use `onVideoStart()` for CIMPs.
* Implement `MaxAdListener#onAdDisplayFailed()` for inters.

## 12.3.4.0
* Certified with Tapjoy SDK 12.3.4.
* Updated the minimum required AppLovin SDK version to 9.5.0.

## 12.3.3.0
* Certified with Tapjoy SDK 12.3.3.

## 12.3.1.0
* Add support for initialization status.

## 12.3.0.1
* Add support for setting whether the user is below the age of consent or not.

## 12.3.0.0
* Certified with Tapjoy SDK 12.3.0.
* Add Unity support for automatic dependency resolution. Please ensure that you are on the latest [AppLovin MAX Unity Plugin](https://bintray.com/applovin/Unity/applovin-max-unity-plugin).
* Add support for extra reward options.
* Support for tracking clicks.

## 12.2.1.1
* Minor adapter improvements.

## 12.2.1.0
* Certified with Tapjoy SDK 12.2.1.

## 12.2.0.0
* Certified with Tapjoy SDK 12.2.0.

## 12.1.0.2
* Use unique package name in Android Manifest.

## 12.1.0.1
* Added Proguard rules required by Tapjoy SDK.

## 12.1.0.0
* Initial commit.
