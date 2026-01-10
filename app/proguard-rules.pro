# Project-specific ProGuard rules for time_manager.
# Trimmed to avoid breaking changes and unrelated packages.

# If your project uses WebView with JS, uncomment and set the JS interface class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment to keep line numbers for crash reports.
#-keepattributes SourceFile,LineNumberTable
#-renamesourcefileattribute SourceFile

# ============================================================
# 1. BASIC ANDROID KEEPS (SAFE)
# ============================================================
# Keep native methods (JNI)
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelables
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializable classes
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ============================================================
# 2. JETPACK COMPOSE (MINIMAL)
# ============================================================
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }

# ============================================================
# 3. ROOM DATABASE
# ============================================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# ============================================================
# 4. KOTLIN SERIALIZATION (Nav3 + app models)
# ============================================================
-keepattributes *Annotation*
-keep class kotlinx.serialization.** { *; }
-keep,includedescriptorclasses class org.examples.time_manager.**$$serializer { *; }
-keepclassmembers class org.examples.time_manager.** {
    *** Companion;
}
-keepclasseswithmembers class org.examples.time_manager.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ============================================================
# 5. LOTTIE
# ============================================================
-keep class com.airbnb.lottie.** { *; }

# ============================================================
# 6. VIEWMODELS
# ============================================================
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# ============================================================
# 7. SAFE WARNINGS TO IGNORE
# ============================================================
-dontwarn javax.annotation.**
-dontwarn javax.inject.**

# ============================================================
# 8. APACHE POI (poi + poi-ooxml)
# ============================================================
-keep class org.apache.poi.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class org.openxmlformats.** { *; }
-keep class org.apache.commons.compress.** { *; }

# Ignore warnings for classes not available on Android or optional dependencies
-dontwarn java.awt.**
-dontwarn javax.activation.**
-dontwarn javax.security.**
-dontwarn javax.xml.bind.**
-dontwarn javax.xml.stream.**
-dontwarn javax.xml.crypto.**
-dontwarn javax.imageio.**
-dontwarn javax.swing.**

# Ignore warnings for third-party libraries used by POI but not included
-dontwarn aQute.bnd.**
-dontwarn com.github.javaparser.**
-dontwarn com.github.luben.zstd.**
-dontwarn com.graphbuilder.**
-dontwarn com.microsoft.schemas.**
-dontwarn com.sun.javadoc.**
-dontwarn com.sun.org.apache.xml.internal.**
-dontwarn de.rototor.pdfbox.**
-dontwarn edu.umd.cs.findbugs.**
-dontwarn net.sf.saxon.**
-dontwarn org.apache.batik.**
-dontwarn org.apache.jcp.xml.dsig.internal.**
-dontwarn org.apache.maven.**
-dontwarn org.apache.pdfbox.**
-dontwarn org.apache.tools.ant.**
-dontwarn org.apache.xml.resolver.**
-dontwarn org.apache.xml.security.**
-dontwarn org.bouncycastle.**
-dontwarn org.brotli.dec.**
-dontwarn org.codehaus.stax2.**
-dontwarn org.ietf.jgss.**
-dontwarn org.objectweb.asm.**
-dontwarn org.osgi.framework.**
-dontwarn org.tukaani.xz.**
-dontwarn org.w3c.dom.**

# Ignore warnings for openxmlformats schemas (often incomplete in lite builds)
-dontwarn org.openxmlformats.schemas.**
