# Changelog

## 6.4.2.0
* Certified with HyprMX SDK 6.4.2.

## 6.4.1.0
* Certified with HyprMX SDK 6.4.1.

## 6.4.0.0
* Certified with HyprMX SDK 6.4.0.
* Update to use new initialization and ad callback APIs.
* Remove deprecated callbacks `onRewardedAdVideoStarted()` and `onRewardedAdVideoCompleted()`.

## 6.2.3.0
* Certified with HyprMX SDK 6.2.3.

## 6.2.0.2
* Add ProGuard rule to not obfuscate public HyprMX SDK classes.

## 6.2.0.1
* Remove the `ageRestrictedUser` check from user consent.

## 6.2.0.0
* Certified with HyprMX SDK 6.2.0.
* Add additional details for ad display failures.

## 6.0.3.1
* Set consent based on a mix of 'hasUserConsent', 'ageRestrictedUser' and 'doNotSell' values.

## 6.0.3.0
* Certified with HyprMX SDK 6.0.3.

## 6.0.2.2
* Add support to set `hasUserConsent` and `ageRestrictedUser` on initialization.

## 6.0.2.1
* Update ad display failed error code.

## 6.0.2.0
* Certified with HyprMX SDK 6.0.2.

## 6.0.1.7
* Set GDPR consent status regardless of users' region.

## 6.0.1.6
* Fix `null` context when creating `HyprMXBannerView`.

## 6.0.1.5
* Remove setting GDPR consent status to `CONSENT_STATUS_UNKNOWN`.

## 6.0.1.4
* Add support for GDPR.

## 6.0.1.3
* Remove `https://hyprmx.jfrog.io/artifactory/hyprmx` from repositories in Unity.
* Fix for getting HyperMX SDK version.

## 6.0.1.2
* Support for null `Activity` on init.

## 6.0.1.1
* Add support for banners, leaders, MRECs.
* Better mapping of fullscreen ad display errors.

## 6.0.1.0
* Certified with HyprMX SDK 6.0.1.

## 5.1.2.6
* Fix potential memory leak by not saving unnecessary references to ad listeners.

## 5.1.2.5
* Initial release to Maven Central and not JCenter.
* Add support to pass 3rd-party error code and description to SDK.
* Updated to point to new repo `https://hyprmx.jfrog.io/artifactory/hyprmx`.

## 5.1.2.4
* Updated to point to new repo `https://hyprmx.bintray.com/hyprmx`.

## 5.1.2.3
* Added error code for when SDK is not initialized in `onAdNotAvailable` callback.

## 5.1.2.2
* Fix userID being null.

## 5.1.2.1
* Fix Unity adapter missing maven repo.

## 5.1.2.0
* Initial commit.
