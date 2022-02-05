# Changelog

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
