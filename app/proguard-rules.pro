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


-dontwarn aQute.bnd.annotation.baseline.BaselineIgnore
-dontwarn aQute.bnd.annotation.spi.ServiceConsumer
-dontwarn aQute.bnd.annotation.spi.ServiceProvider
-dontwarn edu.umd.cs.findbugs.annotations.Nullable
-dontwarn edu.umd.cs.findbugs.annotations.SuppressFBWarnings
-dontwarn java.awt.Color
-dontwarn java.awt.color.ColorSpace
-dontwarn java.awt.geom.AffineTransform
-dontwarn java.awt.geom.Dimension2D
-dontwarn java.awt.geom.Path2D
-dontwarn java.awt.geom.PathIterator
-dontwarn java.awt.geom.Point2D
-dontwarn java.awt.geom.Rectangle2D$Double
-dontwarn java.awt.geom.Rectangle2D
-dontwarn java.awt.image.BufferedImage
-dontwarn java.awt.image.ColorModel
-dontwarn java.awt.image.ComponentColorModel
-dontwarn java.awt.image.DirectColorModel
-dontwarn java.awt.image.IndexColorModel
-dontwarn java.awt.image.PackedColorModel
-dontwarn javax.xml.stream.Location
-dontwarn javax.xml.stream.XMLStreamException
-dontwarn javax.xml.stream.XMLStreamReader
-dontwarn net.sf.saxon.Configuration
-dontwarn net.sf.saxon.dom.DOMNodeWrapper
-dontwarn net.sf.saxon.om.Item
-dontwarn net.sf.saxon.om.NamespaceUri
-dontwarn net.sf.saxon.om.NodeInfo
-dontwarn net.sf.saxon.om.Sequence
-dontwarn net.sf.saxon.om.SequenceTool
-dontwarn net.sf.saxon.sxpath.IndependentContext
-dontwarn net.sf.saxon.sxpath.XPathDynamicContext
-dontwarn net.sf.saxon.sxpath.XPathEvaluator
-dontwarn net.sf.saxon.sxpath.XPathExpression
-dontwarn net.sf.saxon.sxpath.XPathStaticContext
-dontwarn net.sf.saxon.sxpath.XPathVariable
-dontwarn net.sf.saxon.tree.wrapper.VirtualNode
-dontwarn net.sf.saxon.value.DateTimeValue
-dontwarn net.sf.saxon.value.GDateValue
-dontwarn org.apache.batik.anim.dom.SAXSVGDocumentFactory
-dontwarn org.apache.batik.bridge.BridgeContext
-dontwarn org.apache.batik.bridge.DocumentLoader
-dontwarn org.apache.batik.bridge.GVTBuilder
-dontwarn org.apache.batik.bridge.UserAgent
-dontwarn org.apache.batik.bridge.UserAgentAdapter
-dontwarn org.apache.batik.util.XMLResourceDescriptor
-dontwarn org.osgi.framework.Bundle
-dontwarn org.osgi.framework.BundleContext
-dontwarn org.osgi.framework.FrameworkUtil
-dontwarn org.osgi.framework.ServiceReference
-dontwarn org.osgi.framework.wiring.BundleRevision