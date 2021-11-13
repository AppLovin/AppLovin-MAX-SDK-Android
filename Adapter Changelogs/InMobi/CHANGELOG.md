# Changelog

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
