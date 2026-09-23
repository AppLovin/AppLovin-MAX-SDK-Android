-keep class com.amazon.device.ads.** { *; }
-keep class com.iabtcf.** { *; }
-keep class com.amazon.aps.** { *; }

# Amazon APS 12.x optionally references Google Mobile Ads Next Gen; suppress R8 missing-class errors when GMA is absent.
-dontwarn com.google.android.libraries.ads.mobile.sdk.**
