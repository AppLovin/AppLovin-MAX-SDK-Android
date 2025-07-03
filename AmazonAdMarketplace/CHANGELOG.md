# Changelog

## 11.0.1.1
* Update ProGuard rules to preserve `com.amazon.aps.**` classes to fix error from Amazon SDK: `java.lang.ClassNotFoundException: com.amazon.aps.ads.model.ApsAdNetwork`.

## 11.0.1.0
* Certified with AmazonAdMarketplace SDK 11.0.1.
* Updated ad display failed error code.

## 11.0.0.0
* Certified with AmazonAdMarketplace SDK 11.0.0.

## 10.1.1.0
* Certified with AmazonAdMarketplace SDK 10.1.1.
* Removed deprecated code paths based on the minimum supported AppLovin MAX SDK version 11.7.0+.

## 10.1.0.0
* Certified with AmazonAdMarketplace SDK 10.1.0.

## 10.0.0.1
* Introduced `verification.properties` to facilitate adapter ownership verification with Google SDK console.

## 10.0.0.0
* Certified with AmazonAdMarketplace SDK 10.0.0.

## 9.10.4.0
* Certified with AmazonAdMarketplace SDK 9.10.4.

## 9.10.3.2
* Fixed ad display failures caused by loading fullscreen ads without an `Activity` context. AmazonAdMarketplace SDK needs an `Activity` context.

## 9.10.3.1
* Add support for loading fullscreen ads without an `Activity` context.
* Annotated all `Activity` parameters with `@Nullable` to better avoid potential NPEs.

## 9.10.3.0
* Certified with AmazonAdMarketplace SDK 9.10.3.

## 9.10.2.0
* Certified with AmazonAdMarketplace SDK 9.10.2.

## 9.10.1.1
* Remove the listener display failed callback in `onAdError()` because `onAdError()` is sometimes fired when ad display is successful.

## 9.10.1.0
* Certified with AmazonAdMarketplace SDK 9.10.1.

## 9.10.0.0
* Certified with AmazonAdMarketplace SDK 9.10.0.

## 9.9.5.0
* Certified with AmazonAdMarketplace SDK 9.9.5.

## 9.9.4.0
* Certified with AmazonAdMarketplace SDK 9.9.4.

## 9.9.3.2
* Add `com.iabtcf:iabtcf-decoder:2.0.10` dependency to complete fix in v9.9.3.1. This is recommended by Amazon's docs [here](https://ams.amazon.com/webpublisher/uam/docs/aps-mobile/android).

## 9.9.3.1
* Remove deprecated callbacks `onRewardedAdVideoStarted()` and `onRewardedAdVideoCompleted()`.
* Update ProGuard rules to preserve `com.iabtcf.**` classes and APIs to fix error from Amazon SDK: `Non-fatal Exception: java.lang.IllegalArgumentException: Missing the dependency libraries - Ex; com.iabtcf:iabtcf-decoder:2.0.10. For further details, please refer to our Android SDK documentation.`.

## 9.9.3.0
* Certified with AmazonAdMarketplace SDK 9.9.3.

## 9.9.2.1
* Update to new APIs added in Amazon SDK version 9.8.7.
* Note: The minimum Amazon SDK version required for this adapter is 9.8.7.
* Remove the memory leak fix added for APS banners in adapter v9.8.8.1, as Amazon has addressed the issue in Amazon SDK 9.8.10.

## 9.9.2.0
* Certified with AmazonAdMarketplace SDK 9.9.2.

## 9.9.1.0
* Certified with AmazonAdMarketplace SDK 9.9.1.

## 9.9.0.0
* Certified with AmazonAdMarketplace SDK 9.9.0.

## 9.8.10.0
* Certified with AmazonAdMarketplace SDK 9.8.10.

## 9.8.9.0
* Certified with AmazonAdMarketplace SDK 9.8.9.

## 9.8.8.1
* Fix memory leak in APS banners.

## 9.8.8.0
* Certified with AmazonAdMarketplace SDK 9.8.8.

## 9.8.7.0
* Certified with AmazonAdMarketplace SDK 9.8.7.

## 9.8.6.0
* Certified with AmazonAdMarketplace SDK 9.8.6.

## 9.8.5.0
* Certified with AmazonAdMarketplace SDK 9.8.5.

## 9.8.4.0
* Certified with AmazonAdMarketplace SDK 9.8.4.

## 9.8.3.0
* Certified with AmazonAdMarketplace SDK 9.8.3.

## 9.8.2.0
* Certified with AmazonAdMarketplace SDK 9.8.2.

## 9.8.1.0
* Certified with AmazonAdMarketplace SDK 9.8.1.

## 9.8.0.1
* Fix APS banner ads not loading on tablets in edge-case situations.

## 9.8.0.0
* Certified with AmazonAdMarketplace SDK 9.8.0.

## 9.7.1.0
* Certified with AmazonAdMarketplace SDK 9.7.1.

## 9.7.0.0
* Certified with AmazonAdMarketplace SDK 9.7.0.

## 9.6.2.3
* Add support for passing Amazon hashed bidder ID (`amznp`) in `onAdLoaded` callback via `getAdValue( "amazon_hashed_bidder_id" )`. NOTE: The value is not available for static interstitials. AppLovin MAX SDK v11.7.0+ is required.

## 9.6.2.2
* Fix using incorrect mediation hints when same price point is used for different ad formats.

## 9.6.2.1
* Add support for passing creative id to AppLovin SDK.

## 9.6.2.0
* Certified with APS SDK 9.6.2.
* Add additional details for ad display failures.

## 9.5.7.0
* Certified with APS SDK 9.5.7.

## 9.5.6.0
* Certified with APS SDK 9.5.6.

## 9.5.4.1
* Add rewarded video support.

## 9.5.4.0
* Certified with APS SDK 9.5.4.

## 9.5.2.0
* Certified with APS SDK 9.5.2.

## 9.5.1.0
* Certified with APS SDK 9.5.1.

## 9.4.3.3
* Add support for `null` Activity context for regular banners.

## 9.4.3.2
* Update ad display failed error code.

## 9.4.3.1
* Fix memory leak for APS ad request and response objects.

## 9.4.3.0
* Certified with APS SDK 9.4.3.
* Remove temporary memory leak fix for previous SDK version.

## 9.4.2.2
* Fix memory leak for all ad formats.

## 9.4.2.1
* Fix memory leak in APS banners.
* Publishers can enable fix via maxAdView.setLocalExtraParameter( "enable_aps_banner_memory_leak_fix", "true" ); (requires MAX SDK 11.0.0+)

## 9.4.2.0
* Certified with APS SDK 9.4.2 with interstitial video support.

## 9.3.0.3
* Fix case where new ad loader might not be used when passed in.

## 9.3.0.2
* Add support for updating cached ad loaders with new ones passed in via local parameters.

## 9.3.0.1
* Fix unable to load Amazon bid on refresh.

## 9.3.0.0
* Certified with APS SDK 9.3.0.

## 9.2.2.0
* Initial commit.
* Minimum AppLovin MAX SDK version 11.0.0.
