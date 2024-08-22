# Changelog

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
* Requires minimum Android API level be 21 or higher.

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
