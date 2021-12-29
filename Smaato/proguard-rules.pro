# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/basil/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

#-keep public class com.smaato.soma.internal.connector.OrmmaBridge {
#public *;
#}
#-keepattributes *Annotation*

-keep public class com.smaato.sdk.** { *; }
-keep public interface com.smaato.sdk.** { *; }

# For Mediation Debugger support
-keepnames class com.smaato.sdk.core.*
