# Changelog

## 6.1.0.1
* Updated to pass adapter version to the `OguryMediation` object.

## 6.1.0.0
* Certified with OguryPresage SDK 6.1.0.
* Updated minimum Android API level to 21 to match OguryPresage SDK.
* Updated ad display failed error code.

## 6.0.1.1
* Introduced `verification.properties` to facilitate adapter ownership verification with Google SDK console.

## 6.0.1.0
* Certified with OguryPresage SDK 6.0.1.

## 6.0.0.1
* Fix missing MAX SDK name and version for AdView ad requests.

## 6.0.0.0
* Certified with OguryPresage SDK 6.0.0.
* Annotated all `Activity` parameters with `@Nullable` to better avoid potential NPEs.

## 5.8.0.0
* Certified with OguryPresage SDK 5.8.0.
* Add support for passing in MAX SDK name and version.

## 5.7.0.0
* Certified with OguryPresage SDK 5.7.0.

## 5.6.2.2
* Remove privacy method calls as `OguryChoiceManagerExternal.setConsent()` is deprecated and [OguryPresage SDK can collect TCF string directly from disk](https://ogury-ltd.gitbook.io/android/ogury-choice-manager/third-party-consent-manager#case-a-your-cmp-is-compatible-with-the-iab-gdpr-consent-framework).
* Remove deprecated callbacks `onRewardedAdVideoStarted()` and `onRewardedAdVideoCompleted()`.

## 5.6.2.1
* Move `updateUserConsent()` call after `Ogury.start(...)`.

## 5.6.2.0
* Certified with OguryPresage SDK 5.6.2.

## 5.6.1.0
* Certified with OguryPresage SDK 5.6.1.

## 5.6.0.0
* Certified with OguryPresage SDK 5.6.0.
* Remove the `getPrivacySetting()` function and call privacy methods directly.
* Now requires MAX SDK version 9.14.0 or higher.

## 5.5.0.2
* Add bidding support for rewarded, banner, and MREC ads.

## 5.5.0.1
* Add support for IAB's TCFv2 GDPR consent string. Note that you must be on the AppLovin MAX SDK v11.4.3+ and use a TCFv2-compliant framework which stores the consent string in SharedPreferences via the `IABTCF_TCString` key to use this feature.

## 5.5.0.0
* Certified with OguryPresage SDK 5.5.0.

## 5.4.0.1
* Remove `consentDialogState` guard.

## 5.4.0.0
* Certified with OguryPresage SDK 5.4.0.
* Add additional details for ad display failures.

## 5.3.0.1
* Update privacy settings before collecting signal.

## 5.3.0.0
* Certified with OguryPresage SDK 5.3.0.

## 5.2.0.1
* Update ad display failed error code.

## 5.2.0.0
* Support for header bidding.

## 5.0.10.1
* Support for null `Activity` on init.

## 5.0.10.0
* Certified with OguryPresage SDK 5.0.10.
* Remove `checkExistence()` call from adapter.

## 5.0.9.2
* Remove banner support.

## 5.0.9.1
* Use `huc` for GDPR consent status.

## 5.0.9.0
* Certified with OguryPresage SDK 5.0.9.
* Add support for new impression callback.

## 5.0.8.0
* Certified with OguryPresage SDK 5.0.8.

## 5.0.7.0
* Certified with OguryPresage SDK 5.0.7.
* Initial release to Maven Central and not JCenter.

## 5.0.6.0
* Certified with OguryPresage SDK 5.0.6.

## 5.0.5.1
* Add support for banner and MREC ad views.
* Add support to pass 3rd-party error code and description to SDK.

## 5.0.5.0
* Certified with OguryPresage SDK 5.0.5.

## 5.0.2.1
* Migrated to new APIs.

## 5.0.2.0
* Certified with OguryPresage SDK 5.0.2.

## 4.7.7.1
* Update initialization log.
* Fix adapter extending `AppLovinMediationAdapter` instead of `MediationAdapterBase`.
* Removed repository from pom file.

## 4.7.7.0
* Certified with OguryPresage SDK 4.7.7.

## 4.7.2.0
* Certified with Ogury-Presage SDK 4.7.2.
* Move from `ConsentManagerExternal` to `OguryChoiceManagerExternal`.

## 4.3.10.0
* Certified with Ogury-Presage SDK 4.3.10.
* Remove hardcoded SDK version; use Ogury SDK version function.

## 4.3.6.2
* Hardcode the SDK version; SDK version retrieval is not publicly available.

## 4.3.6.1
* Return the correct SDK version.
* Using `Ogury-Presage` as the package naming convention.

## 4.3.6.0
* Initial commit.
* Note: this version is not released.
