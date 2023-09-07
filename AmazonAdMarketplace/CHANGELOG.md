# Changelog

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
