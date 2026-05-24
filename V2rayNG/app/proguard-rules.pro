# ============================================================
# Alfredo VPN — ProGuard / R8 Rules
# ============================================================

# ---- DTO / Gson (все модели с JSON-сериализацией) ----
-keepclassmembers class com.v2ray.ang.dto.** { *; }
-keep class com.v2ray.ang.dto.** { *; }
-keep class com.v2ray.ang.**Dto { *; }

# ---- JNI / Native ----
-keepclasseswithmembers class com.v2ray.ang.service.TProxyService {
    private native <methods>;
    public native <methods>;
    static native <methods>;
}

# ---- MMKV (reflection-based) ----
-keep class com.tencent.mmkv.** { *; }
-keepclassmembers class com.tencent.mmkv.** { *; }

# ---- AppConfig / BuildConfig ----
-keep class com.v2ray.ang.AppConfig { *; }
-keep class com.v2ray.ang.BuildConfig { *; }

# ---- Services, Activities, Receivers (manifest) ----
-keep class com.v2ray.ang.service.** { *; }
-keep class com.v2ray.ang.ui.** { *; }
-keep class com.v2ray.ang.receiver.** { *; }

# ---- Core v2ray ----
-keep class com.v2ray.ang.core.** { *; }
-keep class com.v2ray.ang.handler.** { *; }

# ---- OkHttp3 ----
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ---- Gson ----
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ---- Kotlin ----
-keep class kotlin.Metadata { *; }
-keep class kotlinx.coroutines.** { *; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ---- WorkManager ----
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keepclassmembers class * extends androidx.work.Worker { *; }
-keep class androidx.work.** { *; }

# ---- Material ----
-dontwarn com.google.android.material.**
-keep class com.google.android.material.** { *; }

# ---- AndroidX ----
-keep class androidx.** { *; }
-keep interface androidx.** { *; }

# ---- Quickie QR Scanner ----
-keep class com.t8rin.quickie.** { *; }

# ---- ZXing ----
-keep class com.google.zxing.** { *; }

# ---- License plugin ----
-keep class com.jaredsburrows.** { *; }

# ---- Keep enum classes (R8 может ломать valueOf/values) ----
-keepclassmembers enum * {
    *;
}

# ---- Keep Application class ----
-keep class * extends android.app.Application { *; }

# ---- Keep Parcelable ----
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# ---- Keep Serializable ----
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ---- Keep R (resources) ----
-keep class **.R$* { *; }

# ---- Debug info (опционально, для readable stacktraces) ----
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
