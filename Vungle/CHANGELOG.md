# Changelog

## 6.10.3.1
* Support for null `Activity` on init.

## 6.10.3.0
* Certified with Vungle SDK 6.10.3.

## 6.10.2.1
* Fix centering of banner ads. 
* Remove `checkExistence()` call from adapter.

## 6.10.2.0
* Certified with Vungle SDK 6.10.2.
* Re-add code changes from 6.10.1.0.

## 6.10.1.1
* Downgrade certify version to 6.9.1 because Vungle's crash issues in 6.10.1.
* Revert code changes from 6.10.1.0.

## 6.10.1.0
* Certified with Vungle SDK 6.10.1.
* Update to pass ad markup into load and show methods.
* Update banner & MREC APIs.
* Add support for passing creative ID to SDK (supported in Android SDK 9.15.0+).

## 6.9.1.0
* Certified with Vungle SDK 6.9.1.
* Update `getAvailableBidTokens` method to the new one that takes an Android `Context`.
* Initial release to Maven Central and not JCenter.
* Add support to pass 3rd-party error code and description to SDK.

## 6.8.1.1
* Only set user consent flag if GDPR applies.

## 6.8.1.0
* Certified with Vungle SDK 6.8.1.
* Moved `MaxAdListener#onAdDisplayed()` callback to `onAdViewed()`.

## 6.8.0.0
* Certified with Vungle SDK 6.8.0.
* Update initialization log.

## 6.7.2.0
* Certified with Vungle SDK 6.7.2.
* Update MAX as a wrapper framework.

## 6.7.1.2
* Update 10000000 version check to 9140000.
* Add support for bidding.

## 6.7.1.1
* Update 91400 version check to 10000000.

## 6.7.1.0
* Certified with Vungle SDK 6.7.1.

## 6.7.0.4
* Updated to not set privacy settings if null.

## 6.7.0.3
* Roll back privacy changes.

## 6.7.0.2
* Updated to not set privacy settings if null.

## 6.7.0.1
* Add additional check for valid placement ID.
* Add additional check for uninitialized Vungle SDK.
* Add additional check for null MREC ad after rendering.
* Remove call to `renderAd()` for banners.

## 6.7.0.0
* Certified with Vungle SDK 6.7.0.
* Add support for CCPA.
* Click postbacks are now fired in realtime instead of at the end of the video.
* Now uses aar instead of jar.

## 6.5.3.1
* Show 728x90 leaders instead of 320x50 banners on tablets.

## 6.5.3.0
* Certified with Vungle SDK 6.5.3.
* Disable auto refresh for banner and MREC ads.

## 6.5.2.2
* Show 320x50 banners on tablets to fix resizing issues.

## 6.5.2.1
* Add support for banner ad views.

## 6.5.2.0
* Certified with Vungle SDK 6.5.2.

## 6.5.1.0
* Certified with Vungle SDK 6.5.1 (requires androidX).

## 6.4.11.3
* Add support for MREC ad views.

## 6.4.11.2
* Add support for mute configuration.

## 6.4.11.1
* Updated the minimum required AppLovin SDK version to 9.5.0.
* Removed support for muting/un-muting of ads.
* Removed optional WRITE_EXTERNAL_STORAGE permission.

## 6.4.11.0
* Certified with Vungle SDK 6.4.11.
* Add support for initialization status.

## 6.3.24.6
* Add Unity support for automatic dependency resolution. Please ensure that you are on the latest [AppLovin MAX Unity Plugin](https://bintray.com/applovin/Unity/applovin-max-unity-plugin).
* Add support for extra reward options.

## 6.3.24.5
* Update adapter logging.

## 6.3.24.4
* Dynamically reference against Vungle SDK version number.

## 6.3.24.3
* Bundle in Unity Plugin `/Assets/MaxSdk/Plugins/Android/Shared Dependencies` the following Android dependencies:
    1. `converter-gson.jar`
    2. `common.jar`
    3. `fetch.jar`
    4. `gson.jar`
    5. `okhttp.jar`
    6. `okio.jar`
    7. `retrofit.jar`

## 6.3.24.2
* Use unique package name in Android Manifest.

## 6.3.24.1
* Added Proguard rules required by Vungle SDK.

## 6.3.24.0
* Initial commit.
