# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/basil/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-keep public interface com.bytedance.sdk.openadsdk.downloadnew.** {*;}
-keep class com.pgl.sys.ces.* {*;}

-keep class com.bytedance.sdk.openadsdk.** { *; }
-keep class com.bytedance.frameworks.** { *; }

-keep class ms.bd.c.Pgl.**{*;}
-keep class com.bytedance.mobsec.metasec.ml.**{*;}

-keep class com.ss.android.**{*;}

-keep class com.bytedance.embedapplog.** {*;}
-keep class com.bytedance.embed_dr.** {*;}

-keep class com.bykv.vk.** {*;}