# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /usr/local/Cellar/android-sdk/24.3.3/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# React Native core classes
-keep class com.facebook.react.** { *; }
-keep class com.facebook.soloader.** { *; }
-keep class com.facebook.jni.** { *; }

# React Native dev support (needed even in release builds)
-keep class com.facebook.react.devsupport.** { *; }
-keep class com.facebook.react.modules.debug.** { *; }

# Hermes engine
-keep class com.facebook.hermes.** { *; }

# React Native bridge and modules
-keep class com.facebook.react.bridge.** { *; }
-keep class com.facebook.react.modules.** { *; }
-keep class com.facebook.react.views.** { *; }

# Keep native modules
-keep class com.facebook.react.ReactPackage { *; }
-keep class com.facebook.react.shell.MainPackageConfig { *; }
-keep class com.facebook.react.shell.MainReactPackage { *; }

# AsyncStorage
-keep class com.reactnativecommunity.asyncstorage.** { *; }

# Native method names and classes
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep React Native annotations
-keep @com.facebook.react.bridge.ReactMethod class * { *; }
-keep @com.facebook.react.bridge.ReactModule class * { *; }

# Prevent stripping of React Native classes
-keep class * extends com.facebook.react.ReactActivity
-keep class * extends com.facebook.react.ReactApplication

# SoLoader specific rules
-keep class com.facebook.soloader.DirectorySoSource { *; }
-keep class com.facebook.soloader.SoLoader { *; }
-keep class com.facebook.soloader.ApkSoSource { *; }

# Parking Widget specific classes
-keep class com.parkingwidgetapp.** { *; }
