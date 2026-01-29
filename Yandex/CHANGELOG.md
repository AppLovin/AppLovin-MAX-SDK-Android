# Changelog

## 7.18.2.0
* Certified with Yandex SDK 7.18.2.

## 7.18.1.0
* Certified with Yandex SDK 7.18.1.

## 7.18.0.0
* Certified with Yandex SDK 7.18.0.

## 7.17.0.0
* Certified with Yandex SDK 7.17.0.

## 7.16.1.0
* Certified with Yandex SDK 7.16.1.

## 7.16.0.0
* Certified with Yandex SDK 7.16.0.

## 7.15.2.0
* Certified with Yandex SDK 7.15.2.

## 7.15.1.0
* Certified with Yandex SDK 7.15.1.

## 7.15.0.0
* Certified with Yandex SDK 7.15.0.

## 7.14.1.0
* Certified with Yandex SDK 7.14.1.

## 7.14.0.0
* Certified with Yandex SDK 7.14.0.
* Updated ad display failed error code.

## 7.13.0.0
* Certified with Yandex SDK 7.13.0.

## 7.12.3.0
* Certified with Yandex SDK 7.12.3.

## 7.12.2.1
* Add support for [adaptive banners](https://support.axon.ai/en/max/android/ad-formats/banner-and-mrec-ads#adaptive-banners) & inline adaptive ads in both [banners](https://support.axon.ai/en/max/android/ad-formats/banner-and-mrec-ads#inline-adaptive-banners) and [MRECs](https://support.axon.ai/en/max/android/ad-formats/banner-and-mrec-ads#inline-adaptive-mrecs). Requires AppLovin MAX SDK 13.2.0 or higher.
* Removed deprecated code paths based on the minimum supported AppLovin MAX SDK version 13.0.0.

## 7.12.2.0
* Certified with Yandex SDK 7.12.2.

## 7.12.1.1
* Remove null Activity check when loading interstitial and rewarded ads.

## 7.12.1.0
* Certified with Yandex SDK 7.12.1.

## 7.12.0.0
* Certified with Yandex SDK 7.12.0.

## 7.11.0.0
* Certified with Yandex SDK 7.11.0.

## 7.10.2.0
* Certified with Yandex SDK 7.10.2.

## 7.10.1.0
* Certified with Yandex SDK 7.10.1.

## 7.10.0.0
* Certified with Yandex SDK 7.10.0.

## 7.9.0.1
* Introduced `verification.properties` to facilitate adapter ownership verification with Google SDK console.

## 7.9.0.0
* Certified with Yandex SDK 7.9.0.

## 7.8.1.0
* Certified with Yandex SDK 7.8.1.

## 7.8.0.0
* Certified with Yandex SDK 7.8.0.

## 7.7.0.0
* Certified with Yandex SDK 7.7.0.
* Removed exception throwing if unable to map ad view ad format to that of the network's.

## 7.6.0.1
* Add support for loading rewarded ads and configuring bidder token requests without `Activity` context.

## 7.6.0.0
* Certified with Yandex SDK 7.6.0.

## 7.5.0.3
* Gracefully fail fullscreen ad display if `Activity` context is null.
* Add support for loading bidder token without an `Activity` context.
* Annotated all `Activity` parameters with `@Nullable` to better avoid potential NPEs.

## 7.5.0.2
* Add support for native ads in external plugins.

## 7.5.0.1
* Simplify native ad options view binding.
* Ensure non-empty parameters are passed in ad requests.

## 7.5.0.0
* Certified with Yandex SDK 7.5.0.

## 7.4.0.1
* Requires minimum AppLovin MAX SDK version be 13.0.0.
* Removed support for COPPA.
* Remove deprecated native API usages.

## 7.4.0.0
* Certified with Yandex SDK 7.4.0.

## 7.3.0.0
* Certified with Yandex SDK 7.3.0.

## 7.2.0.0
* Certified with Yandex SDK 7.2.0.

## 7.1.0.0
* Certified with Yandex SDK 7.1.0.

## 7.0.1.0
* Certified with Yandex SDK 7.0.1.
* Remove deprecated callbacks `onRewardedAdVideoStarted()` and `onRewardedAdVideoCompleted()`.
* Updated minimum Android API level to 21 to match Yandex SDK.

## 6.4.1.0
* Certified with Yandex SDK 6.4.1.

## 6.4.0.0
* Certified with Yandex SDK 6.4.0 and AppMetrica SDK 6.2.1.
* Add support for new bidding token generation method.
* Add support for age restricted users.

## 6.1.0.0
* Certified with Yandex SDK 6.1.0.

## 6.0.1.2
* Add support for native ad star ratings.

## 6.0.1.1
* Re-add support for bidding, which was omitted in adapter version 6.0.1.0.

## 6.0.1.0
* Certified with Yandex SDK 6.0.1.

## 5.10.0.0
* Certified with Yandex SDK 5.10.0.

## 5.9.0.1
* Use `Activity` context where available, to fix new tasks from spawning when displaying new ads.

## 5.9.0.0
* Certified with Yandex SDK 5.9.0.

## 5.8.0.0
* Certified with Yandex SDK 5.8.0.
* Remove the `getPrivacySetting()` function and call privacy methods directly.
* Now requires MAX SDK version 9.14.0 or higher.

## 5.7.0.0
* Certified with Yandex SDK 5.7.0.

## 5.6.0.0
* Certified with Yandex SDK 5.6.0.

## 5.4.1.1
* Remove `consentDialogState` guard.

## 5.4.1.0
* Certified with Yandex SDK 5.4.1.

## 5.4.0.0
* Certified with Yandex SDK 5.4.0.
* Add additional details for ad display failures.

## 5.3.2.0
* Certified with Yandex SDK 5.3.2.

## 5.3.1.0
* Certified with Yandex SDK 5.3.1.

## 5.3.0.1
* Add support for native ads.

## 5.3.0.0
* Certified with Yandex SDK 5.3.0.

## 5.2.1.0
* Certified with Yandex SDK 5.2.1.

## 5.2.0.1
* Update privacy settings before collecting signal.

## 5.2.0.0
* Certified with Yandex SDK 5.2.0.

## 5.1.1.0
* Certified with Yandex SDK 5.1.1.

## 5.1.0.0
* Certified with Yandex SDK 5.1.0.

## 5.0.0.1
* Update ad display failed error code.

## 5.0.0.0
* Certified with Yandex SDK 5.0.0.
* Update all ad formats to use a new click callback.

## 4.5.0.3
* Add impression callback for ad view ads.

## 4.5.0.2
* Support for null `Activity` on init.

## 4.5.0.1
* Add bidding support.
* Pass Application context instead of Activity context into fullscreen ad APIs.

## 4.5.0.0
* Certified with Yandex SDK 4.5.0.

## 4.4.1.0
* Certified with Yandex SDK 4.4.1.

## 4.4.0.0
* Certified with Yandex SDK 4.4.0.

## 4.3.0.0
* Certified with Yandex SDK 4.3.0.

## 4.1.0.0
* Certified with Yandex SDK 4.1.0.
* Update and remove deprecated API usages.

## 2.170.4
* Initial release to Maven Central and not JCenter.
* Add support to pass 3rd-party error code and description to SDK.

## 2.170.3
* Only set user consent flag if GDPR applies.

## 2.170.2
* Update 10000000 version check to 9140000.

## 2.170.1
* Update 91400 version check to 10000000.

## 2.170.0
* Certified with Yandex SDK 2.170.

## 2.160.1
* Add display callbacks for test mode interstitial and rewarded ads.

## 2.160.0
* Certified with Yandex SDK 2.160.

## 2.150.3
* Updated to not set privacy settings if null.

## 2.150.2
* Roll back privacy changes.

## 2.150.1
* Updated to not set privacy settings if null.

## 2.150.0
* Certified with Yandex SDK 2.150 and Yandex MobMetrica 3.13.1.
* Add custom parameters in ad requests as requested by Yandex.
* Add new `onAdapterImpressionTracked()` for tracking interstitial and rewarded ad impressions.
* Remove tracking impressions in original fullscreen `onAdShown()` methods.

## 2.112.0
* Certified with Yandex SDK 2.112.
* Add configuration of rewards from server parameters.

## 2.111.0
* Certified with Yandex SDK 2.111 and Yandex MobMetrica 3.8.0.

## 2.101.0
* Initial commit.
