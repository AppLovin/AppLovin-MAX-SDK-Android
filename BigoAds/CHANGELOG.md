# Changelog

## 5.7.0.0
* Certified with BigoAds SDK 5.7.0.

## 5.6.2.0
* Certified with BigoAds SDK 5.6.2.

## 5.6.1.0
* Certified with BigoAds SDK 5.6.1.

## 5.5.2.0
* Certified with BigoAds SDK 5.5.2.

## 5.5.1.2
* Added null checks to prevent `setTag()` calls on null views in BigoAds native.

## 5.5.1.1
* Fixed native ad view clickability issues where only media view and advertiser view were clickable.

## 5.5.1.0
* Certified with BigoAds SDK 5.5.1.

## 5.5.0.0
* Certified with BigoAds SDK 5.5.0.

## 5.4.0.0
* Certified with BigoAds SDK 5.4.0.
* Updated ad display failed error code.

## 5.3.0.2
* Removed requirement of the title asset for native banners and MRECs.

## 5.3.0.1
* Add support for respecting mute state for native ads.

## 5.3.0.0
* Certified with BigoAds SDK 5.3.0.

## 5.2.1.0
* Certified with BigoAds SDK 5.2.1.

## 5.2.0.0
* Certified with BigoAds SDK 5.2.0.

## 5.1.0.1
* Introduced `verification.properties` to facilitate adapter ownership verification with Google SDK console.

## 5.1.0.0
* Certified with BigoAds SDK 5.1.0.
* Removed redundant log output when initialization was already completed.

## 5.0.2.0
* Certified with BigoAds SDK 5.0.2.
* Add support for providing native media content aspect ratio in `MaxNativeAdView`.

## 4.9.1.2
* Explicitly fail ad load with code `-5202` if unable to map ad view ad format to that of the network's.
* Revert minimum AppLovin MAX SDK version to 12.4.1.

## 4.9.1.1
* Annotated all `Activity` parameters with `@Nullable` to better avoid potential NPEs.
* Add support for native ads in external plugins. Requires minimum React Native Plugin 8.0.4 or Flutter Plugin 4.0.2.

## 4.9.1.0
* Certified with BigoAds SDK 4.9.1.
* Remove deprecated native API usages.

## 4.7.0.0
* Certified with BigoAds SDK 4.7.0.

## 4.6.1.2
* Fix `Null extracted folder for artifact` build error when using AGP < 8.0.

## 4.6.1.1
* Fix a dependency issue with previous adapter version.

## 4.6.1.0
* Certified with BigoAds SDK 4.6.1.

## 4.5.1.0
* Initial commit.
* Minimum AppLovin MAX SDK version 12.4.1.
