# Changelog

## 4.5.0.9.0
* Certified with ByteDance SDK 4.5.0.9.

## 4.5.0.7.0
* Certified with ByteDance SDK 4.5.0.7.

## 4.5.0.6.0
* Certified with ByteDance SDK 4.5.0.6.

## 4.5.0.5.0
* Certified with ByteDance SDK 4.5.0.5.

## 4.5.0.4.1
* Add support for returning the main image asset in `MaxNativeAd` for native ads.

## 4.5.0.4.0
* Certified with ByteDance SDK 4.5.0.4.

## 4.5.0.3.0
* Certified with ByteDance SDK 4.5.0.3.

## 4.3.0.9.0
* Certified with ByteDance SDK 4.3.0.9.

## 4.3.0.8.1
* Update ad display failed error code.

## 4.3.0.8.0
* Certified with ByteDance SDK 4.3.0.8.

## 4.3.0.7.5
* Update to check SDK initialization status before collecting signal.

## 4.3.0.7.4
* Remove check for manual native ad assets.

## 4.3.0.7.3
* Add support for null `Activity` context for native banner/MRECs.

## 4.3.0.7.2
* Remove setting app name.

## 4.3.0.7.1
* Downgrade ByteDance SDK to 4.3.0.6.

## 4.3.0.7.0
* Certified with ByteDance SDK 4.3.0.7.

## 4.3.0.6.0
* Certified with ByteDance SDK 4.3.0.6.

## 4.3.0.4.0
* Certified with ByteDance SDK 4.3.0.4.

## 4.2.5.3.0
* Certified with ByteDance SDK 4.2.5.3.

## 4.2.5.2.6
* Update to ingest `event_id` during ad load.

## 4.2.5.2.5
* Update adapters to ingest `event_id`.

## 4.2.5.2.4
* Fix `TTNativeAd.getAdLogoView()` NPE for banner native ads introduced in v4.2.5.2.2.

## 4.2.5.2.3
* Fix memory leaks related to adapter passing in `Activity` contexts into ByteDance SDK which uses strong references.
* Minimum native ads SDK version will be 11.1.0+.

## 4.2.5.2.2
* Add privacy icon (ad logo view) for native ads.

## 4.2.5.2.1
* Add support for Binary CCPA.

## 4.2.5.2.0
* Certified with ByteDance SDK 4.2.5.2.

## 4.2.5.1.0
* Certified with ByteDance SDK 4.2.5.1.

## 4.1.1.9.3
* Support for null `Activity` on init.

## 4.1.1.9.2
* Add support for new `MaxNativeAdView` constructor taking in non-Activity context.

## 4.1.1.9.1
* Add non-bidding support for native ads.

## 4.1.1.9.0
* Certified with ByteDance SDK 4.1.1.9.

## 4.1.1.8.0
* Certified with ByteDance SDK 4.1.1.8.

## 4.1.1.7.0
* Certified with ByteDance SDK 4.1.1.7.

## 4.1.1.6.0
* Certified with ByteDance SDK 4.1.1.6.

## 4.1.1.5.2
* Fix icon view never getting registered for interaction.

## 4.1.1.5.1
* Fix banner sizing.

## 4.1.1.5.0
* Certified with ByteDance SDK 4.1.1.5.

## 4.1.1.4.0
* Certified with ByteDance SDK 4.1.1.4.
* Add support for native custom ads and updated native template ad support.

## 4.1.1.2.0
* Certified with ByteDance SDK 4.1.1.2.

## 4.1.1.1.2
* Re-certified with ByteDance SDK 4.1.1.1.

## 4.1.1.1.1
* Downgrade ByteDance SDK version back to 4.0.1.4 due to `java.lang.ClassNotFoundException: Didn't find class "com.bytedance.sdk.openadsdk.multipro.TTMultiProvider"`.

## 4.1.1.1.0
* Certified with ByteDance SDK 4.1.1.1.

## 4.0.1.4.0
* Certified with ByteDance SDK 4.0.1.4.

## 4.0.1.3.0
* Certified with ByteDance SDK 4.0.1.3.

## 4.0.1.1.0
* Certified with ByteDance SDK 4.0.1.1.

## 3.9.0.5.1
* Add support for banner and MREC ads.

## 3.9.0.5.0
* Certified with ByteDance SDK 3.9.0.5.

## 3.9.0.3.2
* Fix compile issues regarding native ad APIs.

## 3.9.0.3.1
* Initial support for true native ads.

## 3.9.0.3.0
* Certified with ByteDance SDK 3.9.0.3.

## 3.8.1.2.0
* Certified with ByteDance SDK 3.8.1.2.

## 3.8.1.1.1
* Add error log when user fails to receive reward. 

## 3.8.1.1.0
* Certified with ByteDance SDK 3.8.1.1.
* Updated `onRewardVerify` method to the new one with additional parameters.

## 3.6.0.4.1
* Updated `init` method to the new one with callbacks.

## 3.6.0.4.0
* Certified with ByteDance SDK 3.6.0.4.

## 3.5.1.1.0
* Certified with ByteDance SDK 3.5.1.1.

## 3.5.1.0.0
* Certified with ByteDance SDK 3.5.1.0.

## 3.5.0.5.0
* Certified with ByteDance SDK 3.5.0.5.
* Initial release to Maven Central and not JCenter.
* Updated to use ByteDance's custom Maven repo.
* Set mediation provider for ByteDance when the adapter is initialized.

## 3.4.1.1.2
* Updated proguard rules.

## 3.4.1.1.1
* Add support for native ad views.
* Add support to pass 3rd-party error code and description to SDK.

## 3.4.1.1.0
* Certified with ByteDance SDK 3.4.1.1.

## 3.4.0.0.1
* Update Pangle to conditionally reward.

## 3.4.0.0.0
* Certified with ByteDance SDK 3.4.0.0.

## 3.1.7.5.0
* Certified with ByteDance SDK 3.1.7.5.

## 3.1.7.4.1
* Remove support for setting user consent based on `"gdpr_applies"`.

## 3.1.7.4.0
* Certified with ByteDance SDK 3.1.7.4.

## 3.1.5.5.0
* Certified with ByteDance SDK 3.1.5.5.

## 3.1.5.4.1
* Fix GDPR consent to check hasUserConsent.
* Update to check for `gdpr_applies` server parameter.

## 3.1.5.4.0
* Certified with ByteDance SDK 3.1.5.4.

## 3.1.5.3.0
* Certified with SDK 3.1.5.3.

## 3.1.0.1.6
* Update 10000000 version check to 9140000.
* Update initialization log.

## 3.1.0.1.5
* Update 100000 version check to 10000000.

## 3.1.0.1.4
* Update 91400 version check to 100000.

## 3.1.0.1.3
* Add support for bidding on interstitials and rewarded ads.
* Add support for passing up 3rd-party SDK error code.

## 3.1.0.1.2
* Updated to not set privacy settings if null.

## 3.1.0.1.1
* Roll back privacy changes.

## 3.1.0.1.0
* Certified with SDK 3.1.0.1.
* Updated to not set privacy settings if null.

## 2.1.5.0.3
* Add the required content provider element to the adapter's `AndroidManifest.xml` file.

## 2.1.5.0.2
* Add missing `onRewardedAdVideoStarted()` to ByteDance's rewarded ad `onAdShow()` callback.

## 2.1.5.0.1
* Fix rewarded videos not loading b/c of erroneous usage of `setNativeAdType()`. Also removed its usage for regular interstitial ads.

## 2.1.5.0.0
* Initial commit.
