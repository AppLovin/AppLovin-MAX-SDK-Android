# Changelog

## 6.8.0.9
* Add support for true [native banner ads](https://developers.facebook.com/docs/audience-network/guides/ad-formats/native-banner/), which can be enabled on your MAX dashboard.

## 6.8.0.8
* Fix `android.content.Context android.content.Context.getApplicationContext()` NPE when initializing SDK with a non-Activity context on v11.1.0.

## 6.8.0.7
* Add support for new `MaxNativeAdView` constructor taking in non-Activity context.
  
## 6.8.0.6
* Fix headline and advertiser views in native ads.

## 6.8.0.5
* Fix native ad icon not rendering.

## 6.8.0.4
* Fix native ad icon not rendering.

## 6.8.0.3
* Use new `getMediaContentViewGroup` APIs on newer AppLovin SDK versions.
* Fix icon view never getting registered for interaction.

## 6.8.0.2
* Fix a crash when accessing media content view due to type mismatch between SDK versions.

## 6.8.0.1
* Add support for native custom ads and updated native template ad support.
* Remove `checkExistence()` call from adapter.

## 6.8.0.0
* Certified with Facebook SDK 6.8.0.

## 6.7.0.0
* Certified with Facebook SDK 6.7.0.

## 6.6.0.3
* Fix potential memory leak by cleaning up rewarded interstitial ad object.

## 6.6.0.2
* Fix compile issues regarding native ad APIs.

## 6.6.0.1
* Initial support for true native ads.

## 6.6.0.0
* Certified with Facebook SDK 6.6.0.

## 6.5.1.0
* Certified with Facebook SDK 6.5.1.

## 6.5.0.0
* Certified with Facebook SDK 6.5.0.

## 6.4.0.0
* Certified with Facebook SDK 6.4.0.
* Remove waterfall placements support.

## 6.3.0.0
* Certified with Facebook SDK 6.3.0.
* Initial release to Maven Central and not JCenter.

## 6.2.1.0
* Certified with SDK 6.2.1.
* Add support to pass 3rd-party error code and description to SDK.

## 6.2.0.1
* Add checks for null `MaxNativeAdView` components.
* Add ability to set custom template for native banners.

## 6.2.0.0
* Certified with SDK 6.2.0. Includes `onRenderProcessGone()` fix.

## 6.1.0.1
* Add try catch around Facebook's `registerViewForInteraction()` method which is causing crashes.

## 6.1.0.0
* Certified with SDK 6.1.0.

## 6.0.0.2
* Add support for vertical template native banners.

## 6.0.0.1
* Update 10000000 version check to 9140000.

## 6.0.0.0
* Certified with SDK 6.0.0.

## 5.11.0.6
* Update to fail native ad view ad if ad object is null or invalid.

## 5.11.0.5
* Update 100000 version check to 10000000.

## 5.11.0.4
* Update 91400 version check to 100000.

## 5.11.0.3
* Update custom layout for native leader ads.
* Fix rendering native banner and MREC ads.
* Fix impressions for native adview ads.

## 5.11.0.2
* Decrease minSdk back to API 16 from API 19.

## 5.11.0.1
* Added support for rewarded interstitial ads.

## 5.11.0.0
* Certified with SDK 5.11.0.

## 5.10.1.4
* Updated to not set mixed audience if age restricted user setting is null.

## 5.10.1.3
* Roll back privacy changes.

## 5.10.1.2
* Updated to not set mixed audience if age restricted user setting is null.

## 5.10.1.1
* Fix versioning.

## 5.10.1.0
* Certified with SDK 5.10.1.

## 5.10.0.0
* Certified with SDK 5.10.0.

## 5.9.1.1
* Add proguard rule to not obfuscate FAN SDK's BuildConfig.

## 5.9.1.0
* Certified with SDK 5.9.1.

## 5.9.0.0
* Certified with SDK 5.9.0.

## 5.8.0.2
* Add support for native ad views.

## 5.8.0.1
* Add proguard rules to not obfuscate AndroidX library dependencies.

## 5.8.0.0
* Certified with SDK 5.8.0.

## 5.7.1.0
* Certified with SDK 5.7.1.

## 5.7.0.0
* Certified with SDK 5.7.0.

## 5.6.1.0
* Certified with SDK 5.6.1.

## 5.6.0.0
* Certified with SDK 5.6.0.
* Remove deprecated API usages in favor of new APIs.

## 5.5.0.1
* Updated the minimum required AppLovin SDK version to 9.5.0.
* Unity only: Fixed an issue where beta versions of FAN SDK are being downloaded.

## 5.5.0.0
* Certified with SDK 5.5.0.

## 5.4.1.1
* Listen to Facebook's `onInterstitialActivityDestroyed()` callback for firing the [AD HIDDEN] callback.

## 5.4.1.0
* Certified with SDK 5.4.1.

## 5.4.0.1
* Better error code mapping; take into consideration FAN SDK creating new enums instead of using their pre-defined enums which breaks pointer equality.
* Add `com.android.support:appcompat-v7:28.+` to the Unity `Dependencies.xml` file.
* Add support for initialization status.

## 5.4.0.0
* Certified with SDK 5.4.0.
* Update logging.
* Better error code mapping.
* Remove custom initialization code for older versions of FAN (below 5.2.0).

## 5.3.1.1
* Add Unity support for automatic dependency resolution. Please ensure that you are on the latest [AppLovin MAX Unity Plugin](https://bintray.com/applovin/Unity/applovin-max-unity-plugin).

## 5.3.1.0
* Certified with SDK 5.3.1.
* Add support for extra reward options.

## 5.3.0.2
* Listen to Facebook's `onRewardedVideoActivityDestroyed()` callback for firing our [AD HIDDEN] and [REWARDED VIDEO COMPLETED] callbacks. Will not reward in this case, as it may be due to launching app from app icon and Facebook's Activity being destroyed by the OS.

## 5.3.0.1
* Check whether fullscreen ad has expired already before showing.

## 5.3.0.0
* Certified with Facebook Audience Network SDK 5.3.0 as it contains bidding improvements.

## 5.2.1.2
* Dynamically reference against Facebook SDK version number.

## 5.2.1.1
* In the Facebook adapters Unity Plugin, moved the dependencies bundled in 5.2.1.0 into `Assets/MaxSdk/Plugins/Android/Shared Dependencies`.

**Please delete the following from the `Assets/MaxSdk/Plugins/Android/Facebook` folder:**

    1. `exoplayer-core.aar`
    2. `exoplayer-dash.aar`
    3. `recyclerview-v7.aar`

## 5.2.1.0
* Certified with Facebook Audience Network SDK 5.2.1.
* Bundle in Unity Plugin the following Android dependencies:
    1. `exoplayer-core.aar`
    2. `exoplayer-dash.aar`
    3. `recyclerview-v7.aar`

## 5.2.0.1
* Set mediation provider as APPLOVIN_X.X.X:Y.Y.Y.Y where X.X.X is AppLovin's SDK version and Y.Y.Y.Y is the adapter version.

## 5.2.0.0
* Support for FAN SDK 5.2.0.
* Use new FAN SDK initialization APIs.

## 5.1.0.2
* Use unique package name in Android Manifest.

## 5.1.0.1
* Removed Redundant `activity` tags from AndroidManifest.

## 5.1.0.0
* Initial commit.
