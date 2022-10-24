# Changelog

## 21.3.0.2
* Fix silent exception thrown during signal collection for app open ads.

## 21.3.0.1
* Remove redundant client side check for setting user consent.

## 21.3.0.0
* Certified with Google SDK 21.3.0.

## 21.2.0.2
* App open updates.

## 21.2.0.1
* Unity: Add assembly definition files.

## 21.2.0.0
* Certified with Google SDK 21.2.0.

## 21.1.0.2
* Add support for app open ads.

## 21.1.0.1
* Fix impression tracking for fullscreen ads.

## 21.1.0.0
* Certified with Google SDK 21.1.0.

## 21.0.0.3
* Add support for DV360 Bidding by updating the requester type (`requester_type_3`) and request agent (`applovin_dv360`).
* Use local scope copy of native ad while preparing view.

## 21.0.0.2
* Add support for returning the main image asset in `MaxNativeAd` for native ads.

## 21.0.0.1
* Fix interstitial ad display failed callback not being mapped correctly.

## 21.0.0.0
* Certified with Google SDK 21.0.0.
* MinSdkVersion required is now 19.

## 20.6.0.11
* Add support for providing native media content aspect ratio in `MaxNativeAdView`.

## 20.6.0.10
* Update ad display failed error code.

## 20.6.0.9
* Add ability to set [content mapping URLs](https://support.google.com/admob/answer/11050896) via local extra parameters by calling `setLocalExtraParameter("google_content_url", String)` or set multiple URLs by calling `setLocalExtraParameter("google_neighbouring_content_url_strings", List<String>)`.

## 20.6.0.8
* Remove check for manual native ad assets.

## 20.6.0.7
* Add support for null `Activity` context for native ads, native banner/MRECs, and regular banner/MRECs.

## 20.6.0.6
* Map `AdRequest.ERROR_CODE_INVALID_AD_STRING` error to MAX invalid configuration error.

## 20.6.0.5
* Add ability to set [maximum ad content rating](https://support.google.com/admob/answer/10477886) via local extra parameters by calling `setLocalExtraParameter("google_max_ad_content_rating", String)`.

## 20.6.0.4
* Fix potential memory leak with using Activity context for creating native ad views.

## 20.6.0.3
* Fix version check minimum for local extra parameter API usage.

## 20.6.0.2
* Add adaptive banner size info for bidding ad requests.

## 20.6.0.1
* Fix AdChoices `getLocalExtraParameters()` crash. Publishers can set a custom placement on AppLovin SDKs 11.0.0+ and the placement is defaulted to the top right corner otherwise.

## 20.6.0.0
* Certified with Google SDK 20.6.0.

## 20.5.0.9
* Add support for sending ad size information for adview ads. This value can be retrieved in the `onAdLoaded()` callback using `getSize()` from `MaxAd.java` available in MAX SDK v11.2.0.

## 20.5.0.8
* Support for null `Activity` on init.

## 20.5.0.7
* Add support for new `MaxNativeAdView` constructor taking in non-Activity context.

## 20.5.0.6
* Add support for custom [AdChoices placements](https://developers.google.com/android/reference/com/google/android/gms/ads/formats/NativeAdOptions.Builder#setAdChoicesPlacement(int)), which publishers can set by calling `setLocalExtraParameter("admob_ad_choices_placement", int)` on the `MaxNativeAdLoader` instance.

## 20.5.0.5
* Check for Google Mobile Ads SDK initialization status before returning the version string.

## 20.5.0.4
* Return the true version of the Google Mobile Ads SDK integrated.

## 20.5.0.3
* Add support for using initialize method without callback.

## 20.5.0.2
* Add support for native custom ads and updated native template ad support.
* Remove `checkExistence()` call from adapter.

## 20.5.0.1
* Fix signal collection to include query info type.
* Update request agent to "applovin".

## 20.5.0.0
* Certified with Google SDK 20.5.0.

## 20.4.0.2
* Add query info type for bidding ad requests.

## 20.4.0.1
* Fix crash caused by missing icon asset in native ad view ads.

## 20.4.0.0
* Certified with Google SDK 20.4.0.
* Add `onAdClicked()` callback for fullscreen ads.

## 20.3.0.1
* Initial support for true native ads.

## 20.3.0.0
* Certified with Google SDK 20.3.0.

## 20.2.0.4
* Add `placement_req_id` to network extras for all ad requests.
* Fix memory leak issue with collecting signal using activity context instead of application context.

## 20.2.0.3
* Add support for rewarded interstitial ads.

## 20.2.0.2
* Add banner & MREC impression tracking callback to match iOS.

## 20.2.0.1
* Update bidding APIs.

## 20.2.0.0
* Certified with Google SDK 20.2.0.

## 20.1.0.0
* Certified with Google SDK 20.1.0.

## 20.0.0.1
* Remove expired bids after set amount of time.

## 20.0.0.0
* Certified with Google SDK 20.0.0.
* Remove `setIsDesignedForFamilies()` method. As a result `is_designed_for_families` is not passed to Google anymore. 

## 19.8.0.0
* Certified with Google SDK 19.8.0.

## 19.7.0.3
* Add support for bidding.

## 19.7.0.2
* Initial release to Maven Central and not JCenter.
* Update fullscreen ad APIs and ad view method names.
* Remove click tracking for interstitial ads.

## 19.7.0.1
* Add support for creative id for banner native.
* Fix NPE when getting creative id.
* Fix `onAdViewAdLoaded()` called 2x for regular banners ads.

## 19.7.0.0
* Certified with Google SDK 19.7.0.

## 19.6.0.2
* Add support for passing creative id to SDK (supported in Android SDK 9.15.0+).
* Add support to pass 3rd-party error code and description to SDK.

## 19.6.0.1
* Remove deprecated `onAdLeftApplication()` callback. As a result, clicks are no longer tracked for interstitials.

## 19.6.0.0
* Certified with Google SDK 19.6.0.

## 19.5.0.4
* Add adaptive banner support.

## 19.5.0.3
* Only set user consent flag if GDPR applies.

## 19.5.0.2
* Update native banners to use custom templates passed down from backend instead of hardcoding templates.

## 19.5.0.1
* Add checks for required native ad assets (headline, body, images, icon and CTA).
* Add cleanup `destroy()` methods for native ad objects.

## 19.5.0.0
* Certified with Google SDK 19.5.0.

## 19.4.0.4
* Add support for vertical template native banners.

## 19.4.0.3
* Fix AdMob SDK Key not being updated in Android manifest on Unity 2020+ (Integration Manager).

## 19.4.0.2
* Add support for disabling mediation.

## 19.4.0.1
* Update activity reference in native ad listener to be weak reference.

## 19.4.0.0
* Certified with Google SDK 19.4.0.

## 19.3.0.3
* Update 10000000 version check to 9140000.

## 19.3.0.2
* Update 100000 version check to 10000000.

## 19.3.0.1
* Updated deprecated ad load failed callbacks.
* Update 91400 version check to 100000.

## 19.3.0.0
* Certified with Google SDK 19.3.0.

## 19.2.0.5
* Updated to not set privacy settings if null.

## 19.2.0.4
* Roll back privacy changes.

## 19.2.0.3
* Updated to not set privacy settings if null.

## 19.2.0.2
* Add support for native ad views.

## 19.2.0.1
* Updated support for entering App ID via Integration Manager in Unity IDE.
* Fixed compile issues on Unity 2017 or older.

## 19.2.0.0
* Certified with Google SDK 19.2.0.
* Updated to use new initialization API.

## 19.1.0.2
* Fix missing `onRewardedAdVideoCompleted()` callback.
* Add logging to callbacks.

## 19.1.0.1
* Use new rewarded APIs.

## 19.1.0.0
* Certified with Google SDK 19.1.0.

## 19.0.1.1
* Added ability to enter App ID via Integration Manager in Unity IDE.

## 19.0.1.0
* Certified with AdMob SDK Version 19.0.1.

## 18.3.0.1
* Fix SDK versioning in the Mediation Debugger.

## 18.3.0.0
* Certified with AdMob SDK version 18.3.0 (requires AndroidX).

## 17.2.1.7
* Add support for CCPA.

## 17.2.1.6
* Updated the minimum required AppLovin SDK version to 9.5.0.
* Add support for mute configuration.

## 17.2.1.5
* Add support for initialization status.

## 17.2.1.4
* Add back missing import in Unity's Post

## 17.2.1.3
* Fix leader ads using `AdSize.LARGE_BANNER` instead of `AdSize.LEADERBOARD`.

## 17.2.1.2
* Enforce versioning on Unity.

## 17.2.1.1
* Fix incorrect versioning.

## 17.2.1.0
* Certified with AdMob SDK version 17.2.1.
* Please make sure to have your Google Application ID declared in the Android Manifest file. For more information, please refer to our [documentation](https://dash.applovin.com/documentation/mediation/android/mediation-adapters)
* Add Unity support for automatic dependency resolution. Please ensure that you are on the latest [AppLovin MAX Unity Plugin](https://bintray.com/applovin/Unity/applovin-max-unity-plugin).
* Add support for extra reward options.

## 15.0.1.3
* Minor adapter improvements.

## 15.0.1.2
* Use unique package name in Android Manifest.

## 15.0.1.1
* Fixed a rare crash caused due to AndroidManifest merge.

## 15.0.1.0
* Initial commit.
