# Changelog

## 2.0.3.1
* Introduced `verification.properties` to facilitate adapter ownership verification with Google SDK console.

## 2.0.3.0
* Certified with Maio SDK 2.0.3.

## 2.0.2.0
* Certified with Maio SDK 2.0.2.

## 2.0.1.2
* Updated ProGuard rules.

## 2.0.1.1
* Improve error code mapping by using Maio's major error codes.

## 2.0.1.0
* Certified with Maio SDK 2.0.1.
* Annotated all `Activity` parameters with `@Nullable` to better avoid potential NPEs.

## 2.0.0.0
* Certified with Maio SDK 2.0.0.
 
## 1.1.16.3
* Now requires MAX SDK version 9.8.2 or higher.
* Add additional details for ad display failures.
* Remove deprecated callbacks `onRewardedAdVideoStarted()` and `onRewardedAdVideoCompleted()`.

## 1.1.16.2
* Update ad display failed error code.

## 1.1.16.1
* Support for null `Activity` on init.
* Remove `checkExistence()` call from adapter.

## 1.1.16.0
* Certified with Maio SDK 1.1.16.

## 1.1.15.0
* Certified with Maio SDK 1.1.15.
* Initial release to Maven Central and not JCenter.
* Add support to pass 3rd-party error code and description to SDK.

## 1.1.14.0
* Certified with Maio SDK 1.1.14.

## 1.1.13.0
* Certified with Maio SDK 1.1.13.
* Update initialization log.

## 1.1.12.0
* Certified with Maio SDK 1.1.12.
* Fix ad display failed callback not firing in rare cases.

## 1.1.11.0
* Certified with SDK 1.1.11.

## 1.1.10.3
* If an ad is not ready when requested, consider it a no fill instead of waiting.

## 1.1.10.2
* Use SDK aar instead of jar.
* Fix incorrect `Unspecified` and `No Fill` errors due to keeping ad load state between ads.

## 1.1.10.1
* Add configuration of rewards from server parameters.
* Add ProGuard rule for Mediation Debugger support.

## 1.1.10.0
* Certified with SDK 1.1.10.

## 1.1.9.0
* Initial commit.
