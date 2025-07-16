# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep all classes in the main package
-keep class com.bluetoothchat.encrypted.** { *; }

# Keep Bluetooth related classes
-keep class android.bluetooth.** { *; }
-keep class javax.bluetooth.** { *; }

# Keep Bouncy Castle cryptography classes
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ApplicationComponentManager { *; }
-keep class **_HiltModules { *; }
-keep class **_HiltModules$* { *; }

# Keep ViewBinding classes
-keep class * extends androidx.viewbinding.ViewBinding { *; }

# Keep data binding classes
-keep class * extends androidx.databinding.ViewDataBinding { *; }

# Keep Kotlin coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Keep Timber logging
-keep class timber.log.** { *; }
-dontwarn timber.log.**

# Keep Retrofit and OkHttp (if used)
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**

# Keep Gson (if used)
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# Keep Material Design Components
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# Keep AndroidX classes
-keep class androidx.** { *; }
-dontwarn androidx.**

# Keep audio related classes
-keep class android.media.** { *; }
-keep class android.media.audiofx.** { *; }

# Keep security related classes
-keep class java.security.** { *; }
-keep class javax.crypto.** { *; }
-keep class android.security.** { *; }

# Keep serialization classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep Parcelable classes
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep annotation classes
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep constructors
-keepclassmembers class * {
    public <init>(...);
}

# Keep getter and setter methods
-keepclassmembers class * {
    public void set*(***);
    public *** get*();
}

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

-assumenosideeffects class timber.log.Timber {
    public static void v(...);
    public static void i(...);
    public static void w(...);
    public static void d(...);
    public static void e(...);
}

# Optimization settings
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose

# Keep line numbers for debugging
-keepattributes SourceFile,LineNumberTable

# Remove unused code
-dontwarn **
-ignorewarnings

# Keep R classes
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Keep BuildConfig
-keep class **.BuildConfig { *; }

# Specific rules for encrypted chat app
-keep class com.bluetoothchat.encrypted.crypto.** { *; }
-keep class com.bluetoothchat.encrypted.bluetooth.** { *; }
-keep class com.bluetoothchat.encrypted.audio.** { *; }
-keep class com.bluetoothchat.encrypted.data.model.** { *; }

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Keep Repository classes
-keep class * extends com.bluetoothchat.encrypted.data.repository.** { *; }

# Keep service classes
-keep class * extends android.app.Service { *; }

# Keep receiver classes
-keep class * extends android.content.BroadcastReceiver { *; }