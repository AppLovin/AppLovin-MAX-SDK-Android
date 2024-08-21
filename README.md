# AppLovin MAX SDK

Welcome to the AppLovin MAX SDK, your gateway to unlocking the full potential of in-app monetization.

MAX features a unified auction, premium demand, and various ad formats allowing you to maximize revenue from in-app advertising. With support for 25+ ad networks and custom integrations, MAX makes it easy to drive higher CPMs and optimize your monetization strategy. 
Learn [more about MAX](https://www.applovin.com/max/) on the AppLovin website.

This `AppLovin-MAX-SDK-Android` repository contains:
1. Example source code for using MAX
2. Open source mediation adapters

## Examples
### Demo App
The [Java Demo App](https://github.com/AppLovin/AppLovin-MAX-SDK-Android/tree/master/AppLovin%20MAX%20Demo%20App%20-%20Java) and [Kotlin Demo App](https://github.com/AppLovin/AppLovin-MAX-SDK-Android/tree/master/AppLovin%20MAX%20Demo%20App%20-%20Kotlin) are sample projects demonstrating how to mediate ads using AppLovin MAX. To get started with the demo apps, follow the instructions below:

1. Open your desired project in Android Studio: `AppLovin MAX Demo App - Java` or `AppLovin MAX Demo App - Kotlin`.
2. Verify that the dependency `implementation 'com.applovin:applovin-sdk:+'` is included in your `build.gradle (Module: app)`.
3. Update the package with your own unique identifier associated with the application you will create (or already created, if it is an existing app) in the MAX dashboard in your `build.gradle (Module: app)`.
4. Update the unique MAX ad unit id value within each ad's activity code. Each ad format will correspond to a unique MAX ad unit ID you created in the AppLovin dashboard for the package used before. 

<img src="https://github.com/user-attachments/assets/102213b3-463a-4a16-9688-58e40d6c26e6" height="500" />

### Demo Ad Formats
The Java/Kotlin demo apps have examples of implementing the following ad formats.
|   |   |   |
|---|---|---|
| App Open     | [Java](https://github.com/AppLovin/AppLovin-MAX-SDK-Android/blob/master/AppLovin%20MAX%20Demo%20App%20-%20Java/app/src/main/java/com/applovin/enterprise/apps/demoapp/ads/max/AppOpenAdActivity.java) | [Kotlin](https://github.com/AppLovin/AppLovin-MAX-SDK-Android/blob/master/AppLovin%20MAX%20Demo%20App%20-%20Kotlin/app/src/main/java/com/applovin/enterprise/apps/demoapp/ads/max/AppOpenAdActivity.kt) |
| Banner       | [Java](https://github.com/AppLovin/AppLovin-MAX-SDK-Android/tree/master/AppLovin%20MAX%20Demo%20App%20-%20Java/app/src/main/java/com/applovin/enterprise/apps/demoapp/ads/max/banner) | [Kotlin](https://github.com/AppLovin/AppLovin-MAX-SDK-Android/tree/master/AppLovin%20MAX%20Demo%20App%20-%20Kotlin/app/src/main/java/com/applovin/enterprise/apps/demoapp/ads/max/banner) |
| Interstitial | [Java](https://github.com/AppLovin/AppLovin-MAX-SDK-Android/blob/master/AppLovin%20MAX%20Demo%20App%20-%20Java/app/src/main/java/com/applovin/enterprise/apps/demoapp/ads/max/InterstitialAdActivity.java) | [Kotlin](https://github.com/AppLovin/AppLovin-MAX-SDK-Android/blob/master/AppLovin%20MAX%20Demo%20App%20-%20Kotlin/app/src/main/java/com/applovin/enterprise/apps/demoapp/ads/max/InterstitialAdActivity.kt) |
| MREC         | [Java](https://github.com/AppLovin/AppLovin-MAX-SDK-Android/tree/master/AppLovin%20MAX%20Demo%20App%20-%20Java/app/src/main/java/com/applovin/enterprise/apps/demoapp/ads/max/mrecs) | [Kotlin](https://github.com/AppLovin/AppLovin-MAX-SDK-Android/tree/master/AppLovin%20MAX%20Demo%20App%20-%20Kotlin/app/src/main/java/com/applovin/enterprise/apps/demoapp/ads/max/mrecs) |
| Native       | [Java](https://github.com/AppLovin/AppLovin-MAX-SDK-Android/tree/master/AppLovin%20MAX%20Demo%20App%20-%20Java/app/src/main/java/com/applovin/enterprise/apps/demoapp/ads/max/nativead) | [Kotlin](https://github.com/AppLovin/AppLovin-MAX-SDK-Android/tree/master/AppLovin%20MAX%20Demo%20App%20-%20Kotlin/app/src/main/java/com/applovin/enterprise/apps/demoapp/ads/max/nativead) |
| Rewarded     | [Java](https://github.com/AppLovin/AppLovin-MAX-SDK-Android/blob/master/AppLovin%20MAX%20Demo%20App%20-%20Java/app/src/main/java/com/applovin/enterprise/apps/demoapp/ads/max/RewardedAdActivity.java) | [Kotlin](https://github.com/AppLovin/AppLovin-MAX-SDK-Android/blob/master/AppLovin%20MAX%20Demo%20App%20-%20Kotlin/app/src/main/java/com/applovin/enterprise/apps/demoapp/ads/max/RewardedAdActivity.kt) |

## Mediation Adapters
There are 25+ open source adapters mediating with the AppLovin SDK. A [list of adapters](https://www.applovin.com/partners/) are available on our AppLovin partners page, filtered by MAX monetization partner.

## Getting Started
Ready to get started? Refer to our [documentation](https://developers.applovin.com/en/android/overview/integration/) for step-by-step guides on integrating MAX and enabling mediated networks in your app.

## GitHub Issue Tracker
To file bugs, make feature requests, or suggest improvements for MAX, please use [GitHub's issue tracker](https://github.com/AppLovin/AppLovin-MAX-SDK-Android/issues).

For questions or further support, please contact us via our [AppLovin support page](https://monetization-support.applovin.com/hc/en-us).
