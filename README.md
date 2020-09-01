# AppLovin MAX SDK

## Overview
MAX is AppLovin's in-app monetization solution.

Move beyond the traditional monetization solution and integrate MAX. MAX is a single unbiased auction where advertisers get equal access to all ad inventory and bid simultaneously, which drives more competition and higher CPMs for you. You can read more about it [here](https://www.applovin.com/max-header-bidding).

To request an invite for MAX, apply [here](https://try.applovin.com/applovin-max-application).

Please check out our [documentation](https://dash.applovin.com/documentation/mediation/android/getting-started) to get started on integrating and enabling mediated networks using our guides.

## Demo Apps
To get started with the demo apps, follow the instructions below:

1. Open your desired project in Android Studio: `DemoApp-Java` or `DemoApp-Kotlin`.
2. Verify that the dependency `implementation 'com.applovin:applovin-sdk:+'` is included in your `build.gradle (Module: app)`.
3. Update the `applovin.sdk.key` value in `AndroidManifest.xml` file with your AppLovin SDK key associated with your account.
4. Update the package with your own unique identifier associated with the application you will create (or already created, if it is an existing app) in the MAX dashboard.
5. Update the unique MAX ad unit id value within each ad's activity code. Each ad format will correspond to a unique MAX ad unit ID you created in the AppLovin dashboard for the package used before. 

## Error Codes
| Code          | Description   |
| ------------- |:-------------:|
| -1            | Indicates an unspecified error with one of the mediated network SDKs. |
| 204           | Indicates that no ads are currently eligible for your device. |
| -102          | Indicates that the ad request timed out (usually due to poor connectivity). |
| -103          | Indicates that the device is not connected to the internet (e.g. airplane mode). |
| -2051         | Indicates that the device is not connected to a VPN or the VPN connection is not working properly (Users in China Only). |
| -5001         | Indicates that the ad failed to load due to various reasons (such as no networks being able to fill). |
| -5201         | Indicates an internal state error with the AppLovin MAX SDK. |
| -5601         | Indicates the provided `Activity` instance has been garbage collected while the AppLovin MAX SDK attempts to re-load an expired ad. |

## Support
We recommend using GitHub to file issues. For feature requests, improvements, questions or any other integration issues using MAX Mediation by AppLovin, contact us via our support page https://monetization-support.applovin.com/hc/en-us.

MRECs and native ads have been deprecated and will be removed in a future SDK release.
