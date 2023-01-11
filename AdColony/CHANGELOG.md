# Changelog

## 4.8.0.3
* Add additional details for ad display failures.
* Remove `consentDialogState` guard.

## 4.8.0.2
* Update privacy settings before collecting signal.

## 4.8.0.1
* Add impression callback, `onShow()`, for banners and MRECs.

## 4.8.0.0
* Certified with AdColony SDK 4.8.0.

## 4.7.1.1
* Update ad display failed error code.

## 4.7.1.0
* Certified with AdColony SDK 4.7.1.

## 4.7.0.0
* Certified with AdColony SDK 4.7.0.

## 4.6.5.2
* Update CCPA arguments passed to `setPrivacyConsentString()` to correct order.

## 4.6.5.1
* Remove `checkExistence()` call from adapter.
* Support for null `Activity` on init.

## 4.6.5.0
* Certified with AdColony SDK 4.6.5.

## 4.6.4.0
* Certified with AdColony SDK 4.6.4.

## 4.6.3.3
* Fix reward logic to properly handle rewarding the user.

## 4.6.3.2
* Fix memory leak by initializing using application.

## 4.6.3.1
* Fix memory leaks by cleaning up listeners properly.

## 4.6.3.0
* Certified with AdColony SDK 4.6.3.

## 4.6.2.0
* Certified with AdColony SDK 4.6.2.

## 4.6.1.0
* Certified with AdColony SDK 4.6.1.

## 4.6.0.0
* Certified with AdColony SDK 4.6.0.

## 4.5.0.0
* Certified with AdColony SDK 4.5.0.

## 4.4.1.6
* Update to use AdColony SDK hosted on MavenCentral and not Bintray.

## 4.4.1.5
* Update SDK version check for setting CCPA from 61100 to 91100.

## 4.4.1.4
* Update deprecated method `collectSignal()` to new one.

## 4.4.1.3
* Remove setting COPPA string to empty.

## 4.4.1.2
* Initial release to Maven Central and not JCenter.

## 4.4.1.0
* Certified with AdColony SDK 4.4.1.

## 4.4.0.0
* Certified with AdColony SDK 4.4.0.

## 4.3.1.0
* Certified with AdColony SDK 4.3.1.

## 4.3.0.1
* Only set user consent flag if GDPR applies.
* When consent dialog state is .APPLIES, set privacy framework required for .GDPR to be true, else if state is .DOES_NOT_APPLY, set privacy framework required for .GDPR to be false.

## 4.3.0.0
* Certified with AdColony SDK 4.3.0.

## 4.2.4.1
* Fix NPE that occurs when `hasUserConsent` is `null`.

## 4.2.4.0
* Certified with AdColony SDK 4.2.4.

## 4.2.3.1
* Update 10000000 version check to 9140000.
* Update initialization log.

## 4.2.3.0
* Certified with AdColony SDK 4.2.3.

## 4.2.2.3
* Update 100000 version check to 10000000.

## 4.2.2.2
* Update 91400 version check to 100000.

## 4.2.2.1
* Updated deprecated GDPR API usages.
* Added CCPA support.
* Added COPPA support.
* Added bidding support.
* Added banner/MREC support.

## 4.2.2.0
* Certified with AdColony SDK 4.2.2.

## 4.2.1.0
* Certified with AdColony SDK 4.2.1.

## 4.1.4.3
* Updated to not set privacy settings if null.

## 4.1.4.2
* Roll back privacy changes.

## 4.1.4.1
* Updated to not set privacy settings if null.

## 4.1.4.0
* Certified with AdColony SDK 4.1.4.

## 4.1.3.1
* Downgrade AdColony SDK to 4.1.2, because 4.1.3 causes building an app bundle to fail.

## 4.1.3.0
* Certified with AdColony SDK 4.1.3.

## 4.1.2.1
* Actualy certify with AdColony SDK 4.1.2.

## 4.1.2.0
* Certified with AdColony SDK 4.1.2.

## 4.1.1.0
* Certified with AdColony SDK 4.1.1.
* Updated the minimum required AppLovin SDK version to 9.5.0.

## 4.1.0.0
* Certified with AdColony SDK 4.1.0.

## 3.3.11.1
* Add proguard rule to not obfuscate public AdColony SDK classes.

## 3.3.11.0
* Certified with AdColony SDK 3.3.11.
* Add support for initialization status.
* Fix potential crash for when the AdColony expires.

## 3.3.10.1
* Add Unity support for automatic dependency resolution. Please ensure that you are on the latest [AppLovin MAX Unity Plugin](https://bintray.com/applovin/Unity/applovin-max-unity-plugin).
* Add support for extra reward options.

## 3.3.10.0
* Certified with AdColony SDK 3.3.10.

## 3.3.8.1
* Update adapter logging.

## 3.3.8.0
* Initial commit.
