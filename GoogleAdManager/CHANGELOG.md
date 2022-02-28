# Changelog

## 20.6.0.1
* Fix AdChoices `getLocalExtraParameters()` crash. Publishers can set a custom placement on AppLovin SDKs 11.0.0+ and the placement is defaulted to the top right corner otherwise.

## 20.6.0.0
* Certified with GoogleAdManager SDK 20.6.0.

## 20.5.0.7
* Initialize Google `MobileAds` SDK.

## 20.5.0.6
* Add support for sending ad size information for adview ads. This value can be retrieved in the `onAdLoaded()` callback using `getSize()` from `MaxAd.java` available in MAX SDK v11.2.0.

## 20.5.0.5
* Add support for new `MaxNativeAdView` constructor taking in non-Activity context.

## 20.5.0.4
* Add support for custom [AdChoices placements](https://developers.google.com/android/reference/com/google/android/gms/ads/formats/NativeAdOptions.Builder#setAdChoicesPlacement(int)), which publishers can set by calling `setLocalExtraParameter("gam_ad_choices_placement", int)` on the `MaxNativeAdLoader` instance.

## 20.5.0.3
* Fall back to the former approach for getting the SDK version string.

## 20.5.0.2
* Return the true version of the Google Mobile Ads SDK integrated.

## 20.5.0.1
* Add support for native custom ads and updated native template ad support.
* Remove bidding and signal collection logic from adapter.
* Remove `checkExistence()` call from adapter.

## 20.5.0.0
* Certified with GoogleAdManager SDK 20.5.0.

## 20.4.0.0
* Certified with GoogleAdManager SDK 20.4.0.
* Add `onAdClicked()` callback for fullscreen ads.

## 20.3.0.1
* Initial support for true native ads.

## 20.3.0.0
* Certified with GoogleAdManager SDK 20.3.0.

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
* Certified with GoogleAdManager SDK 20.2.0.

## 20.1.0.0
* Certified with GoogleAdManager SDK 20.1.0.
* Add check for `MobileAds` class in initialization to match the Mediation Debugger class existence check.

## 20.0.0.1
* Remove expired bids after set amount of time.

## 20.0.0.0
* Certified with GoogleAdManager SDK 20.0.0.
* Remove `setIsDesignedForFamilies()` method. As a result `is_designed_for_families` is not passed to Google anymore.
* Update deprecated APIs by setting test mode using `RequestConfiguration.Builder.setTestDeviceIds()` and age restricted user using `RequestConfiguration.Builder.setTagForChildDirectedTreatment()`. 

## 19.8.0.0
* Certified with GoogleAdManager SDK 19.8.0.

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
* Certified with GoogleAdManager SDK 19.7.0.

## 19.6.0.2
* Add support for passing creative id to SDK (supported in Android SDK 9.15.0+).
* Add support to pass 3rd-party error code and description to SDK.

## 19.6.0.1
* Remove deprecated `onAdLeftApplication()` callback. As a result, clicks are no longer tracked for interstitials.

## 19.6.0.0
* Certified with GoogleAdManager SDK 19.6.0.

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
* Certified with GoogleAdManager SDK 19.5.0.

## 19.4.0.3
* Add support for vertical template native banners.

## 19.4.0.2
* Fix Google Ad Manager manifest element not being applied in Unity 2020+ (Integration Manager).

## 19.4.0.1
* Update activity reference in native ad listener to be weak reference.

## 19.4.0.0
* Certified with AdMob SDK Version 19.4.0.

## 19.3.0.3
* Update 10000000 version check to 9140000.

## 19.3.0.2
* Update 100000 version check to 10000000.

## 19.3.0.1
* Updated deprecated ad load failed callbacks.
* Update 91400 version check to 100000.

## 19.3.0.0
* Certified with AdMob SDK Version 19.3.0.

## 19.2.0.4
* Updated to not set privacy settings if null.

## 19.2.0.3
* Roll back privacy changes.

## 19.2.0.2
* Updated to not set privacy settings if null.

## 19.2.0.1
* Add support for native ad views.

## 19.2.0.0
* Certified with AdMob SDK version 19.2.0.

## 19.1.0.0
* Certified with AdMob SDK version 19.1.0.

## 19.0.1.0
* Certified with AdMob SDK version 19.0.1.

## 18.3.0.1
* Fix SDK versioning in the Mediation Debugger.

## 18.3.0.0
* Certified with AdMob SDK version 18.3.0 (requires AndroidX).

## 17.2.1.1
* Add support for CCPA.

## 17.2.1.0
* Initial commit.
