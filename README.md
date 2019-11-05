# AppLovin MAX SDK

## Overview
MAX is AppLovin's in-app monetization solution.

MAX offers advertisers equal opportunity to bid simultaneously on each impression in a publisher’s inventory via a single unified auction to drive the highest possible yield. You can read more about it [here](https://www.applovin.com/max-header-bidding).

Please check out our [documentation](https://dash.applovin.com/documentation/mediation/android/getting-started) to get started on integrating and enabling mediated networks using our guides.

## Demo Apps
To get started with the demo apps, follow the instructions below:

1. Open your desired project in Android Studio: `DemoApp-Java` or `DemoApp-Kotlin`.
2. Verify that the dependency `implementation 'com.applovin:applovin-sdk:+'` is included in your `build.gradle (Module: app)`.
3. Update the `applovin.sdk.key` value in `AndroidManifest.xml` file with your AppLovin SDK key associated with your account.
4. Update the package with your own unique identifier associated with the application you will create (or already created, if it is an existing app) in the MAX dashboard.
5. Update the unique MAX ad unit id value within each ad's activity code. Each ad format will correspond to a unique MAX ad unit ID you created in the AppLovin dashboard for the package used before. 

## Support
We recommend using GitHub to file issues. For feature requests, improvements, questions or any other integration issues using MAX Mediation by AppLovin, please reach out to your account team and copy devsupport@applovin.com.
