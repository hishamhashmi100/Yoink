# youtubedl-android maps yt-dlp's JSON output onto these classes with Jackson (reflection).
-keep class com.yausername.** { *; }
-keep class com.fasterxml.jackson.** { *; }
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# youtubedl-android unpacks Python with commons-compress, which creates its zip "extra field"
# classes through reflection. Without these rules R8 strips their constructors and the
# downloader fails to start in release builds (debug builds aren't shrunk, so they work).
-keep class org.apache.commons.compress.** { *; }
-keep class org.apache.commons.io.** { *; }

# The app is open source, so readable names cost nothing and keep crash reports useful.
-dontobfuscate

# Optional parts of Jackson and commons-compress that are never used on Android.
-dontwarn com.fasterxml.jackson.databind.ext.**
-dontwarn java.beans.**
-dontwarn org.w3c.dom.bootstrap.**
-dontwarn org.apache.commons.compress.**
-dontwarn org.tukaani.xz.**
-dontwarn org.brotli.dec.**
-dontwarn com.github.luben.zstd.**
