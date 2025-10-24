# Changelog

## 3.5.0.0
* Certified with BidMachine SDK 3.5.0.

## 3.4.0.0
* Certified with BidMachine SDK 3.4.0.

## 3.3.0.2
* Implement new signal collection API.

## 3.3.0.1
* Updated ad display failed error code.

## 3.3.0.0
* Certified with BidMachine SDK 3.3.0.

## 3.2.1.0
* Certified with BidMachine SDK 3.2.1.

## 3.2.0.1
* Fixed `getSdkVersion()` to return the installed SDK version.
* Removed deprecated code paths based on the minimum supported AppLovin MAX SDK version 13.0.0.

## 3.2.0.0
* Certified with BidMachine SDK 3.2.0.

## 3.1.1.1
* Introduced `verification.properties` to facilitate adapter ownership verification with Google SDK console.

## 3.1.1.0
* Certified with BidMachine SDK 3.1.1.
* Removed redundant log output when initialization was already completed.

## 3.1.0.0
* Certified with BidMachine SDK 3.1.0.
* Annotated all `Activity` parameters with `@Nullable` to better avoid potential NPEs.

## 3.0.1.2
* Add AppLovin MAX SDK version 13.0.0+ as a maven dependency.

## 3.0.1.1
* Requires minimum AppLovin MAX SDK version be 13.0.0.
* Removed support for COPPA.
* Remove deprecated native API usages.
* Certified with BidMachine SDK 3.0.1.

## 3.0.0.0
* Certified with BidMachine SDK 3.0.0.
* Updated minimum Android API level to 21 to match BidMachine SDK.

## 2.7.0.0
* Certified with BidMachine SDK 2.7.0.

## 2.6.0.1
* Fix a dependency issue with previous adapter version.

## 2.6.0.0
* Certified with BidMachine SDK 2.6.0.
* Remove deprecated callbacks `onRewardedAdVideoStarted()` and `onRewardedAdVideoCompleted()`.

## 2.5.2.0
* Certified with BidMachine SDK 2.5.2.

## 2.5.1.0
* Certified with BidMachine SDK 2.5.1.

## 2.5.0.0
* Certified with BidMachine SDK 2.5.0.

## 2.4.2.0
* Certified with BidMachine SDK 2.4.2.

## 2.4.1.0
* Certified with BidMachine SDK 2.4.1.

## 2.4.0.0
* Certified with BidMachine SDK 2.4.0.

## 2.3.3.0
* Certified with BidMachine SDK 2.3.3.

## 2.3.2.0
* Certified with BidMachine SDK 2.3.2.

## 2.3.1.0
* Certified with BidMachine SDK 2.3.1.
* Add `onAdShowFailed()` callback for banners, MRECs, and native ads.

## 2.2.0.1
* Updated `BidMachine.getBidToken()` usage.

## 2.2.0.0
* Certified with BidMachine SDK 2.2.0.

## 2.1.13.0
* Certified with BidMachine SDK 2.1.13.

## 2.1.12.0
* Certified with BidMachine SDK 2.1.12.

## 2.1.11.0
* Certified with BidMachine SDK 2.1.11.

## 2.1.10.0
* Certified with BidMachine SDK 2.1.10.

## 2.1.9.0
* Certified with BidMachine SDK 2.1.9.

## 2.1.8.0
* Certified with BidMachine SDK 2.1.8.

## 2.1.7.0
* Certified with BidMachine SDK 2.1.7.

## 2.1.6.0
* Certified with BidMachine SDK 2.1.6.

## 2.1.5.1
* Map `BMError.BAD_CONTENT` to `MaxAdapterError.INTERNAL_ERROR` instead of `MaxAdapterError.NO_FILL`.
* Remove `consentDialogState` guard.

## 2.1.5.0
* Certified with BidMachine SDK 2.1.5.
* Remove deprecated `onAdShown()` callbacks.

## 1.9.10.5
* Support for native ads in external plugins (e.g. React Native).

## 1.9.10.4
* Update error code mapping for SDK error reports.
* Add additional details for ad display failures.

## 1.9.10.3
* Add CCPA support.

## 1.9.10.2
* Update Unity dependencies.

## 1.9.10.1
* Update Unity dependencies.

## 1.9.10.0
* Certified with BidMachine SDK 1.9.10.

## 1.9.4.3
* Add BidMachine repository to Unity plugin.

## 1.9.4.2
* Add support for passing creative id to SDK.

## 1.9.4.1
* Add support for returning the main image asset in `MaxNativeAd` for native ads.

## 1.9.4.0
* Certified with BidMachine SDK 1.9.4.

## 1.9.3.2
* Use server parameters instead of custom parameters.

## 1.9.3.1
* Update ad display failed error code.

## 1.9.3.0
* Initial commit.
* Minimum AppLovin MAX SDK version 11.4.0.
