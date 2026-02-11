# Changelog

## 8.4.3.0
* Certified with Fyber SDK 8.4.3.

## 8.4.2.0
* Certified with Fyber SDK 8.4.2.

## 8.4.1.0
* Certified with Fyber SDK 8.4.1.

## 8.4.0.0
* Certified with Fyber SDK 8.4.0.

## 8.3.8.0
* Certified with Fyber SDK 8.3.8.
* Updated ad display failed error code.

## 8.3.7.0
* Certified with Fyber SDK 8.3.7.

## 8.3.6.1
* Removed deprecated code paths based on the minimum supported AppLovin MAX SDK version 13.2.0.
* Updated minimum Android API level to 19 to match AppLovin SDK.
* Requires minimum AppLovin MAX SDK version be 13.2.0.

## 8.3.6.0
* Certified with Fyber SDK 8.3.6.

## 8.3.5.1
* Introduced `verification.properties` to facilitate adapter ownership verification with Google SDK console.

## 8.3.5.0
* Certified with Fyber SDK 8.3.5.

## 8.3.4.0
* Certified with Fyber SDK 8.3.4.
* Removed redundant log output when initialization was already completed.

## 8.3.3.0
* Certified with Fyber SDK 8.3.3.

## 8.3.2.0
* Certified with Fyber SDK 8.3.2.

## 8.3.1.2
* Fix NPE when binding `adViewGroup` after it has been destroyed.
* Annotated all `Activity` parameters with `@Nullable` to better avoid potential NPEs.

## 8.3.1.1
* Requires minimum AppLovin MAX SDK version be 13.0.0.
* Removed support for COPPA.

## 8.3.1.0
* Certified with Fyber SDK 8.3.1.

## 8.3.0.0
* Certified with Fyber SDK 8.3.0.
* Remove String validation for signal.

## 8.2.7.1
* Fix `Unsupported class file major version 65` build error when using JDK < 21.

## 8.2.7.0
* Certified with Fyber SDK 8.2.7.
* Remove deprecated callbacks `onRewardedAdVideoStarted()` and `onRewardedAdVideoCompleted()`.

## 8.2.6.0
* Certified with Fyber SDK 8.2.6.

## 8.2.5.0
* Certified with Fyber SDK 8.2.5.

## 8.2.4.0
* Certified with Fyber SDK 8.2.4.

## 8.2.3.3
* Fix to call `InneractiveAdManager.setMediationName()` and `InneractiveAdManager.setMediationVersion()` before initialization.

## 8.2.3.2
* Call `InneractiveAdManager.setMediationName()` and `InneractiveAdManager.setMediationVersion()` statically at the time of signal collection.

## 8.2.3.1
* Call `InneractiveAdManager.currentAudienceAppliesToCoppa()` after SDK initialization.
* Remove the `getPrivacySetting()` function and call privacy methods directly.
* Now requires MAX SDK version 9.14.0 or higher.

## 8.2.3.0
* Certified with Fyber SDK 8.2.3.

## 8.2.2.0
* Certified with Fyber SDK 8.2.2.

## 8.2.1.2
* Implement `InneractiveAdManager.currentAudienceIsAChild()` API to set if end user is COPPA.

## 8.2.1.1
* Remove `consentDialogState` guard.

## 8.2.1.0
* Certified with Fyber SDK 8.2.1.
* Add additional details for ad display failures.

## 8.2.0.0
* Certified with Fyber SDK 8.2.0.

## 8.1.5.1
* Update privacy settings before collecting signal.

## 8.1.5.0
* Certified with Fyber SDK 8.1.5.

## 8.1.4.1
* Downgrade Fyber SDK to 8.1.3 due to duplicate symbol conflicts with other SDKs (e.g. `a.a`).

## 8.1.4.0
* Certified with Fyber SDK 8.1.4.

## 8.1.3.3
* Add support for IAB's TCFv2 GDPR consent string. Note that you must be on the AppLovin MAX SDK v11.4.3+ and use a TCFv2-compliant framework which stores the consent string in SharedPreferences via the `IABTCF_TCString` key to use this feature. Fyber will still be filtered out of the waterfall in GDPR regions if the string is not available or one of the criteria is not met.

## 8.1.3.2
* Update ad display failed error code.

## 8.1.3.1
* Add support for `null` Activity context for regular banners/MRECs.

## 8.1.3.0
* Certified with Fyber SDK 8.1.3.

## 8.1.2.2
* Add support for IAB's CCPA Privacy String.

## 8.1.2.1
* Support for null `Activity` on init.

## 8.1.2.0
* Certified with Fyber SDK 8.1.2.

## 8.1.1.0
* Certified with Fyber SDK 8.1.1.
* Remove `checkExistence()` call from adapter.

## 8.1.0.0
* Certified with Fyber SDK 8.1.0.

## 8.0.0.0
* Certified with Fyber SDK 8.0.0.
* Add support for bidding.
* Update mute APIs to use `InneractiveAdManager.setMuteVideo(...)`.

## 7.8.4.2
* Send MAX SDK version instead of adapter version.

## 7.8.4.1
* Fix potential memory leak by cleaning up ad view ad object.

## 7.8.4.0
* Certified with Fyber SDK 7.8.4.
* Update to clear GDPR consent if it does not apply.

## 7.8.3.0
* Certified with Fyber SDK 7.8.3.

## 7.8.2.0
* Set mediation type to `Max` and mediation version to adapter version.
* Update to use Fyber SDK hosted on MavenCentral and not Bintray.

## 7.8.1.3
* Check `isReady` before attempting to display ad view.

## 7.8.1.2
* Initial release to Maven Central and not JCenter.
* Implement adapter initialization using callback-based method.
* Update error codes mapping.

## 7.8.1.0
* Certified with Fyber SDK 7.8.1.

## 7.8.0.0
* Certified with Fyber SDK 7.8.0.

## 7.7.4.1
* Add support for passing creative id to SDK (supported in Android SDK 9.15.0+).
* Add support to pass 3rd-party error code and description to SDK.

## 7.7.4.0
* Certified with Fyber SDK 7.7.4.

## 7.7.3.1
* Only set user consent flag if GDPR applies.

## 7.7.3.0
* Certified with Fyber SDK 7.7.3.
* Removed repository from pom file.

## 7.7.2.0
* Certified with Fyber SDK 7.7.2.

## 7.7.1.0
* Certified with Fyber SDK 7.7.1.

## 7.7.0.1
* Update 10000000 version check to 9140000.
* Update initialization log.

## 7.7.0.0
* Certified with Fyber SDK 7.7.0.
* Pass user id to Fyber SDK.

## 7.6.1.3
* Update 100000 version check to 10000000.

## 7.6.1.2
* Update 91400 version check to 100000.

## 7.6.1.1
* Decrease minSdk back to API 16 from API 19.

## 7.6.1.0
* Certified with Fyber SDK 7.6.1.
* Include Fyber error string insted of just error code.

## 7.6.0.6
* Updated to not set privacy settings if null.

## 7.6.0.5
* Roll back privacy changes.

## 7.6.0.4
* Updated to not set privacy settings if null.

## 7.6.0.3
* Use new `onAdRewarded` callback to grant rewarded ad reward.

## 7.6.0.2
* Update Unity dependencies to point to Fyber repo.

## 7.6.0.1
* Fix adapter build.gradle.kts to point to Fyber repo.

## 7.6.0.0
* Certified with Fyber SDK 7.6.0.

## 7.5.4.0
* Certified with Inneractive SDK 7.5.4.
* Fix rewarded ad callback order. (Ad displayed then rewarded video started)

## 7.5.3.0
* Certified with Inneractive SDK 7.5.3.

## 7.5.2.0
* Certified with Inneractive SDK 7.5.2.
* Updated log to differentiate ad view formats.

## 7.5.0.0
* Certified with Inneractive SDK 7.5.0.
* Add support for muting videos.

## 7.4.1.0
* Certified with Inneractive SDK 7.4.1 where GSON dependency is actually removed.

## 7.4.0.0
* Certified with Inneractive SDK 7.4.0.
* Updated the minimum required AppLovin SDK version to 9.5.0.
* Removed GSON dependency.

## 7.3.3.0
* Certified with Inneractive SDK 7.3.3.

## 7.3.1.1
* Add support for initialization status.

## 7.3.1.0
* Certified with Inneractive SDK 7.3.1.

## 7.2.1.3
* Add Unity support for automatic dependency resolution. Please ensure that you are on the latest [AppLovin MAX Unity Plugin](https://bintray.com/applovin/Unity/applovin-max-unity-plugin).
* Add support for extra reward options.

## 7.2.1.2
* Update adapter logging.

## 7.2.1.1
* Bundle against gson.jar in Unity Package.

## 7.2.1.0
* Certified with Inneractive SDK 7.2.1.
* Support for display errors.

## 7.1.7.1
* Use unique package name in Android Manifest.

## 7.1.7.0
* Initial commit.
