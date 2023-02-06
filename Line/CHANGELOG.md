# Changelog

## 2022.12.26.0
* Certified with Line SDK 2.5.20221226.

## 2022.2.16.4
* Remove `consentDialogState` guard.

## 2022.2.16.3
* Support for native ads in external plugins (e.g. React Native).

## 2022.2.16.2
* Remove checks to enable initializing and showing interstitial and rewarded ads using different `activity` instances.
* Update error code mapping for `NO_AD`.

## 2022.2.16.1
* Add comment stating that FiveAd's SDK requires that same `activity` instance used to initialize an interstitial or rewarded ad needs to be the SAME one used to show it.

## 2022.2.16.0
* Certified with FiveAd SDK 2.4.20220216.

## 2021.10.29.2
* Update ad display failed error code.

## 2021.10.29.1
* Remove check for manual native ad assets.

## 2021.10.29.0
* Certified with FiveAd SDK 2.4.20211029.
* Update mute setting API to use `enableSoundByDefault()` instead of `enableSound()`. NOTE: The mute state can only be set at SDK initialization, hence, the mute state at time of ad display may not reflect the current mute state.
* Update impression callback to `onFiveAdImpression()` to replace `onFiveAdImpressionImage()`.
* Add check to ensure that the same `activity` instance used to initialize interstitial and rewarded ads is passed in their respective `show()` methods.
* Improve error handling.
* Remove `FiveAdConfig.formats` since it is deprecated.

## 2021.5.11.11
* Support for null `Activity` on init.

## 2021.5.11.10
* Add support for new `MaxNativeAdView` constructor taking in non-Activity context.

## 2021.5.11.9
* Use new `getMediaContentViewGroup` APIs on newer AppLovin SDK versions.
* Fix icon view never getting registered for interaction.

## 2021.5.11.8
* Fix a crash when accessing media content view due to type mismatch between SDK versions.

## 2021.5.11.7
* Add support for native custom ads and updated native template ad support.
* Remove `checkExistence()` call from adapter.

## 2021.5.11.6
* Pass `Activity` into interstitial and rewarded video `FiveAdInterstitial#show()` and `FiveAdVideoReward#show()` calls.

## 2021.5.11.5
* Fix true native ad NPE from enabling sound on null object.

## 2021.5.11.4
* Fix potential memory leak by removing unnecessary references to ad listeners.

## 2021.5.11.3
* Initial support for true native ads.

## 2021.5.11.2
* Always mute banner and MREC ads.

## 2021.5.11.1
* Fix the dependency in the pom.xml file.

## 2021.5.11.0
* Certified with FiveAd SDK 2.3.20210511.
* Update to use FiveAd SDK hosted on MavenCentral.
* Update to use `FiveAd.getSdkSemanticVersion()` to retrieve SDK Version and not `FiveAd.getSdkVersion()`.

## 2020.11.25.2
* Initial release to Maven Central and not JCenter.

## 2020.11.25.1
* Updated proguard rules.

## 2020.11.25.0
* Initial commit.
