# Changelog

## 9.11.1.0
* Certified with Chartboost SDK 9.11.1.

## 9.11.0.0
* Certified with Chartboost SDK 9.11.0.

## 9.10.2.0
* Certified with Chartboost SDK 9.10.2.

## 9.10.1.0
* Certified with Chartboost SDK 9.10.1.

## 9.10.0.0
* Certified with Chartboost SDK 9.10.0.

## 9.9.3.0
* Certified with Chartboost SDK 9.9.3.

## 9.9.2.1
* Removed `isCached()` checks to prevent valid ads from being blocked.

## 9.9.2.0
* Certified with Chartboost SDK 9.9.2.

## 9.9.1.0
* Certified with Chartboost SDK 9.9.1.
* Implemented `onAdExpired` in listeners to satisfy the updated Chartboost SDK callback interface.
* Removed deprecated code paths based on the minimum supported AppLovin MAX SDK version 13.0.0.
* Updated minimum Android API level to 19 to match Chartboost SDK.
* Updated ad display failed error code.

## 9.8.3.0
* Certified with Chartboost SDK 9.8.3.

## 9.8.2.1
* Introduced `verification.properties` to facilitate adapter ownership verification with Google SDK console.

## 9.8.2.0
* Certified with Chartboost SDK 9.8.2.

## 9.8.1.0
* Certified with Chartboost SDK 9.8.1.
* Removed redundant log output when initialization was already completed.

## 9.8.0.1
* Downgrade Chartboost SDK to version 9.7.0 due to removal of 9.8.0.

## 9.8.0.0
* Certified with Chartboost SDK 9.8.0.

## 9.7.0.4
* Add support for loading ads without an `Activity` context.
* Annotated all `Activity` parameters with `@Nullable` to better avoid potential NPEs.

## 9.7.0.3
* Requires minimum AppLovin MAX SDK version be 13.0.0.
* Removed support for COPPA.

## 9.7.0.2
* Add bidding support for banners, leaders, MRECs, interstitials, and rewarded ads.

## 9.7.0.1
* Remove deprecated callbacks `onRewardedAdVideoStarted()` and `onRewardedAdVideoCompleted()`.

## 9.7.0.0
* Certified with Chartboost SDK 9.7.0.

## 9.6.1.0
* Certified with Chartboost SDK 9.6.1.

## 9.6.0.0
* Certified with Chartboost SDK 9.6.0.

## 9.5.0.0
* Certified with Chartboost SDK 9.5.0.

## 9.4.1.0
* Certified with Chartboost SDK 9.4.1.

## 9.4.0.0
* Certified with Chartboost SDK 9.4.0.

## 9.3.1.0
* Certified with Chartboost SDK 9.3.1.

## 9.3.0.0
* Certified with Chartboost SDK 9.3.0.
* Use Chartboost's custom Maven repo for their SDK (updated to: https://cboost.jfrog.io/artifactory/chartboost-ads/).
* Remove the `getPrivacySetting()` function and call privacy methods directly.
* Now requires MAX SDK version 9.14.0 or higher.

## 9.2.1.0
* Certified with Chartboost SDK 9.2.1.

## 9.2.0.0
* Certified with Chartboost SDK 9.2.0.

## 9.1.1.2
* Remove `consentDialogState` guard.

## 9.1.1.1
* Update error code mapping for SDK error reports.
* Add additional details for ad display failures.

## 9.1.1.0
* Certified with Chartboost SDK 9.1.1.

## 9.1.0.0
* Certified with Chartboost SDK 9.1.0.

## 9.0.0.1
* Fix NPE caused by calling `getLocation()` on a null adView.

## 9.0.0.0
* Certified with Chartboost SDK 9.0.0.
* Add collection of creative id for fullscreen ads.

## 8.4.3.1
* Fix incomplete ad display failure error message.

## 8.4.3.0
* Certified with Chartboost SDK 8.4.3.

## 8.4.2.1
* Update ad display failed error code.

## 8.4.2.0
* Certified with Chartboost SDK 8.4.2.

## 8.4.1.3
* Re-certified with Chartboost SDK 8.4.1.

## 8.4.1.2
* Downgrade Chartboost SDK to 8.3.1 due to Android build issues.

## 8.4.1.1
* Add support for COPPA.

## 8.4.1.0
* Certified with Chartboost SDK 8.4.1.

## 8.4.0.0
* Certified with Chartboost SDK 8.4.0.

## 8.3.1.1
* Support for null `Activity` on init.

## 8.3.1.0
* Certified with Chartboost SDK 8.3.1.

## 8.3.0.0
* Certified with Chartboost SDK 8.3.0.

## 8.2.1.1
* Add support for banners and MRECs.
* Remove `checkExistence()` call from adapter.

## 8.2.1.0
* Certified with Chartboost SDK 8.2.1.

## 8.2.0.2
* Update to use Chartboost SDK hosted on MavenCentral and not Bintray.

## 8.2.0.1
* Initial release to Maven Central and not JCenter.
* Add support to pass 3rd-party error code and description to SDK.

## 8.2.0.0
* Certified with Chartboost SDK 8.2.0.

## 8.1.0.9
* Fix ProGuard issue by moving privacy settings code to adapter from router and using `getWrappingSdk().getConfiguration()` instead of `mSdk.getConfiguration()`.

## 8.1.0.8
* Only set user consent flag if GDPR applies.
* Removed repository from pom file.

## 8.1.0.7
* Update 10000000 version check to 9140000.
* Update initialization log.

## 8.1.0.6
* Update 100000 version check to 10000000.

## 8.1.0.5
* Update 91400 version check to 100000.

## 8.1.0.4
* Updated to not set privacy settings if null.

## 8.1.0.3
* Roll back privacy changes.

## 8.1.0.2
* Add support for CCPA.
* Updated deprecated GDPR settings.
* Updated to not set privacy settings if null.

## 8.1.0.1
* Fix versioning.

## 8.1.0.0
* Certified with Chartboost SDK 8.1.0.

## 8.0.3.0
* Certified with Chartboost SDK 8.0.3.
* Fix ad display failed callback not firing in rare cases.

## 8.0.2.0
* Certified with Chartboost SDK 8.0.2.
* Fix Chartboost SDK's null delegate race condition introduced in Chartboost SDK 8.0.1.
* Fix Chartboost SDK's interstitial & rewarded ad timeout on older Android devices with API less than 21.

## 8.0.1.3
* Revert Chartboost SDK 8.0.1 to 7.5.0, Chartboost callbacks not working.

## 8.0.1.2
* Fix Chartboost SDK's `artifactId`.
* Revert previous unneeded fix in `Dependencies.xml`.

## 8.0.1.1
* Fix Unity `Dependencies.xml` Android package name.

## 8.0.1.0
* Certified with Chartboost SDK 8.0.1.
* Removed activity dependency.
* Removed and updated deprecated API calls.
* Use Chartboost's custom Maven repo for their SDK.

## 7.5.0.2
* Unity: add `com.google.android.gms:play-services-base` dependency.

## 7.5.0.1
* Fix Chartboost SDK display failures for apps with multiple Activities/screens.

## 7.5.0.0
* Certified with Chartboost SDK 7.5.0.

## 7.3.1.6
* Updated the minimum required AppLovin SDK version to 9.5.0.
* Removed optional WRITE_EXTERNAL_STORAGE & ACCESS_WIFI_STATE permissions.

## 7.3.1.5
* Add support for initialization status.

## 7.3.1.4
* Add Unity support for automatic dependency resolution. Please ensure that you are on the latest [AppLovin MAX Unity Plugin](https://bintray.com/applovin/Unity/applovin-max-unity-plugin).

## 7.3.1.3
* Add support for `[AD DISPLAY FAILED]` callbacks from Chartboost's `didFailToLoadInterstitial()` and `didFailToLoadRewardedVideo()` methods.
* Add support for extra reward options.

## 7.3.1.2
* Update adapter logging.

## 7.3.1.1
* Use unique package name in Android Manifest.

## 7.3.1.0
* Initial commit.
