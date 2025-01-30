# Changelog

## 10.8.0.1
* Introduced `verification.properties` to facilitate adapter ownership verification with Google SDK console.
* Removed redundant log output when initialization was already completed.

## 10.8.0.0
* Certified with InMobi SDK 10.8.0.

## 10.7.8.1
* Add support for loading fullscreen ads without an `Activity` context.
* Annotated all `Activity` parameters with `@Nullable` to better avoid potential NPEs.

## 10.7.8.0
* Certified with InMobi SDK 10.7.8.

## 10.7.7.2
* Fix firing click callback twice on single click for native ads.

## 10.7.7.1
* Requires minimum AppLovin MAX SDK version be 13.0.0.
* Removed support for COPPA.
* Remove deprecated native API usages.

## 10.7.7.0
* Certified with InMobi SDK 10.7.7.

## 10.7.6.0
* Certified with InMobi SDK 10.7.6.

## 10.7.5.0
* Certified with InMobi SDK 10.7.5.

## 10.7.4.0
* Certified with InMobi SDK 10.7.4.

## 10.7.3.0
* Certified with InMobi SDK 10.7.3.
* Remove deprecated callbacks `onRewardedAdVideoStarted()` and `onRewardedAdVideoCompleted()`.

## 10.6.7.0
* Certified with InMobi SDK 10.6.7.

## 10.6.6.0
* Certified with InMobi SDK 10.6.6.

## 10.6.3.1
* Fix NPE caused by calling `LayoutParams.width` to get media view width for native ads.

## 10.6.3.0
* Certified with InMobi SDK 10.6.3.
* Improve error handling.

## 10.6.2.0
* Certified with InMobi SDK 10.6.2.

## 10.6.1.0
* Certified with InMobi SDK 10.6.1.

## 10.6.0.1
* Downgrade InMobi SDK to 10.5.9.

## 10.6.0.0
* Certified with InMobi SDK 10.6.0.

## 10.5.9.0
* Certified with InMobi SDK 10.5.9.

## 10.5.8.2
* Fix NPE caused by initializing with null account id.

## 10.5.8.1
* Fix `com.inmobi.ads.exceptions.SdkNotInitializedException` from calling `InMobiSdk.setIsAgeRestricted(...)` with `true` value before initializing SDK.
* Updated to use `InMobiPrivacyCompliance.setDoNotSell()` API to set CCPA values.

## 10.5.8.0
* Certified with InMobi SDK 10.5.8.

## 10.5.7.0
* Certified with InMobi's Kotlin SDK 10.5.7.

## 10.1.4.3
* Fix the `mediaView` scaling bug for native ads, caused by mismatch in aspect ratio.

## 10.1.4.2
* Revert temporary workaround for the issue where `onAdImpression()` is called before `onAdLoadSucceeded()`.

## 10.1.4.1
* Add a temporary workaround for the issue where `onAdImpression()` is called before `onAdLoadSucceeded().`

## 10.1.4.0
* Certified with InMobi SDK 10.1.4.
* Remove the `getPrivacySetting()` function and call privacy methods directly.
* Now requires MAX SDK version 9.14.0 or higher.

## 10.1.3.4
* Initialize InMobi SDK on UI thread to avoid crash: `IllegalStateException: Calling View methods on another thread than the UI thread`.

## 10.1.3.3
* Fix `ClassCastException` that occurs when the parent view of `mediaView` is not of type `FrameLayout` for native ads.

## 10.1.3.2
* Certified with InMobi SDK 10.1.3.

## 10.1.3.1
* Downgrade to InMobi SDK 10.1.2 to avoid crash: `IllegalStateException: Calling View methods on another thread than the UI thread`.

## 10.1.3.0
* Certified with InMobi SDK 10.1.3.

## 10.1.2.4
* Remove `consentDialogState` guard.

## 10.1.2.3
* Add support for star ratings in manual native ads.

## 10.1.2.2
* Add CCPA support.

## 10.1.2.1
* Support for native ads in external plugins (e.g. React Native).

## 10.1.2.0
* Certified with InMobi SDK 10.1.2.
* Add additional details for ad display failures.

## 10.1.1.0
* Certified with InMobi SDK 10.1.1.

## 10.0.9.3
* Fix NPE caused due to calling `InMobiNative.getAdTitle()` on a `null` native ad instance.

## 10.0.9.2
* Add support for native ad view ads.

## 10.0.9.1
* Add impression callbacks for banners, MRECs, interstitials and rewarded ads and update `onAdImpressed()` to `onAdImpression()` for native ads.

## 10.0.9.0
* Certified with InMobi SDK 10.0.9.

## 10.0.8.1
* Use local scope copy of native ad while preparing view.
* Update privacy settings before collecting signal.

## 10.0.8.0
* Certified with InMobi SDK 10.0.8.

## 10.0.7.0
* Certified with InMobi SDK 10.0.7.

## 10.0.6.0
* Certified with InMobi SDK 10.0.6.

## 10.0.5.6
* Update ad display failed error code.

## 10.0.5.5
* Remove check for manual native ad assets.

## 10.0.5.4
* Add support for null `Activity` context for regular banner/MRECs.

## 10.0.5.3
* Add support for native ads.

## 10.0.5.2
* Add support for both Family Apps and COPPA.

## 10.0.5.1
* Add support for COPPA.

## 10.0.5.0
* Certified with InMobi SDK 10.0.5.

## 10.0.3.1
* Support for null `Activity` on init.

## 10.0.3.0
* Certified with InMobi SDK 10.0.3.

## 10.0.2.0
* Certified with InMobi SDK 10.0.2.
* Remove `checkExistence()` call from adapter.

## 10.0.1.0
* Certified with InMobi SDK 10.0.1.
* Update signal collection APIs.

## 9.9.9.8
* Update consent dictionary and API used to set GDPR info.
* Update GDPR before signal collection.

## 9.9.9.7
* Fix potential memory leak by not saving unnecessary references to ad listeners.

## 9.9.9.6
* Certified with InMobi SDK 9.2.1.

## 9.9.9.5
* Certified with InMobi SDK 9.2.0.

## 9.9.9.4
* Certified with InMobi SDK 9.1.9.

## 9.9.9.3
* Downgrade certify version to 9.1.1 because of lower show rates on 9.1.6 and 9.1.7.

## 9.9.9.2
* Certified with InMobi SDK 9.1.7.

## 9.9.9.1
* Downgrade certify version to 9.1.6.

## 9.9.9.0
* Certified with InMobi SDK 9.9.9.

## 9.1.6.0
* Downgrade certify version to 9.1.6. InMobi accidentally released a test SDK 9.9.9.

## 9.9.9.0
* Certified with InMobi SDK 9.9.9.

## 9.1.1.4
* Unity: Bumped picasso dependency version to 2.71828.

## 9.1.1.3
* Add support for passing creative id to SDK (supported in Android SDK 9.15.0+).
* Add support to pass 3rd-party error code and description to SDK.

## 9.1.1.2
* Only set user consent flag if GDPR applies.
* When consent dialog state is .APPLIES, add key "gdpr" with value 1, else if state is .DOES_NOT_APPLY, add key "gdpr" with value 0.

## 9.1.1.1
* Fix silent crash when generating consent object due to us using `getInt()` as opposed to `getBoolean()` for `"gdpr_applies"`.

## 9.1.1.0
* Certified with InMobi SDK 9.1.1.

## 9.1.0.0
* Certified with InMobi SDK 9.1.0.
* Uses `<queries>` element which requires min Gradle version 3.5.4.

## 9.0.9.2
* Update 10000000 version check to 9140000.
* Update initialization log.

## 9.0.9.1
* Update signal collection to fail early if InMobi SDK is not initialized successfully.

## 9.0.9.0
* Certified with InMobi SDK 9.0.9.

## 9.0.8.2
* Fix delay caused by invalid placement identifier handling in signal collection.

## 9.0.8.1
* Update 91400 version check to 10000000.

## 9.0.8.0
* Certified with InMobi SDK 9.0.8.

## 9.0.7.4
* Updated to not set privacy settings if null.

## 9.0.7.3
* Roll back privacy settings.

## 9.0.7.2
* Updated to not set privacy settings if null.

## 9.0.7.1
* Changed signal collecting ad objects from method level to class level.

## 9.0.7.0
* Certified with InMobi SDK 9.0.7.

## 9.0.6.0
* Certified with InMobi SDK 9.0.6.

## 9.0.5.0
* Certified with InMobi SDK 9.0.5.
* Add support for bidding.

## 9.0.4.1
* Add proguard rules to not obfuscate AndroidX library dependencies.

## 9.0.4.0
* Certified with InMobi SDK 9.0.4.
* Track initialization status with new initialization complete callback.

## 9.0.2.0
* Certified with InMobi SDK 9.0.2.

## 9.0.1.4
* Add support for MREC ad views.

## 9.0.1.3
* Unity: add `com.android.support:customtabs` dependency.
* Unity: bump support library version to 28.0.0.

## 9.0.1.2
* Added banner display callback to `onAdLoadSucceeded()`.

## 9.0.1.1
* Moved interstitial and rewarded display callbacks from `onAdWillDisplay()` to `onAdDidDisplay()`.

## 9.0.1.0
* Certified with InMobi SDK 9.0.1.

## 9.0.0.0
* Certified with InMobi SDK 9.0.0.
* Removed unused server parameters.
* Updated the minimum required AppLovin SDK version to 9.5.0.
* Removed support for muting/un-muting of ads.

## 7.3.0.0
* Certified with InMobi SDK 7.3.0.

## 7.2.9.0
* Add support for initialization status.

## 7.2.8.1
* Add Unity support for automatic dependency resolution. Please ensure that you are on the latest [AppLovin MAX Unity Plugin](https://bintray.com/applovin/Unity/applovin-max-unity-plugin).
* Add support for extra reward options.

## 7.2.8.0
* Certified with InMobi SDK 7.2.8.

## 7.2.7.3
* Update adapter logging.

## 7.2.7.2
* In the InMobi adapters Unity Plugin, moved the `picasso.jar` dependency into `Assets/MaxSdk/Plugins/Android/Shared Dependencies`.

**Please delete `picasso.jar` from the `Assets/MaxSdk/Plugins/Android/InMobi` folder:**

## 7.2.7.1
* Use `onAdWillDisplay()` instead of `onAdDisplayed()` for impression tracking.

## 7.2.7.0
* Initial commit.
