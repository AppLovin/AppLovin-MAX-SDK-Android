# Changelog

## x.x.x.x
* Remove `checkExistence()` call from adapter.

## 5.16.4.0
* Certified with MoPub SDK 5.16.4.
* Initial release to Maven Central and not JCenter.

## 5.15.0.0
* Certified with MoPub SDK 5.15.0.
* Add support for initialization status.
* Add support for passing creative id to SDK (supported in iOS SDK 6.15.0+).
* Add support to pass 3rd-party error code and description to SDK.
* Only set user consent flag if GDPR applies.
* Use `useImpression(...)` for ad displayed callbacks.
* Refactor to use MediationAdapterRouter for rewarded events.
* Remove MOAT dependency/repo per MoPub's docs.
* Add `ACCESS_COARSE_LOCATION` permission removal to AndroidManifest.xml. 

## 5.5.0.5
* Dynamically reference against MoPub SDK version number.

## 5.5.0.4
* In the MoPub adapters Unity Plugin, moved the `support-annotations.jar` dependency into `Assets/MaxSdk/Plugins/Android/Shared Dependencies`.

**Please delete `support-annotations.jar` from the `Assets/MaxSdk/Plugins/Android/MoPub` folder:**

## 5.5.0.3
* Turn on verbose logging when in testing mode.

## 5.5.0.2
* Bundle `mopub-volley.jar` (v2.0.0) in the Unity Plugin.

## 5.5.0.1
* Fix MoPub rewarded videos not initializing.

## 5.5.0.0
* Certified with MoPub SDK 5.5.0.

## 5.3.0.2
* Use unique package name in Android Manifest.

## 5.3.0.1
* Add ProGuard rules.

## 5.3.0.0
* Initial commit.
