# youtubedl-android maps yt-dlp's JSON output onto these classes with Jackson (reflection).
-keep class com.yausername.** { *; }
-keep class com.fasterxml.jackson.** { *; }
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Optional parts of Jackson and commons-compress that are never used on Android.
-dontwarn com.fasterxml.jackson.databind.ext.**
-dontwarn java.beans.**
-dontwarn org.w3c.dom.bootstrap.**
-dontwarn org.apache.commons.compress.**
-dontwarn org.tukaani.xz.**
-dontwarn org.brotli.dec.**
-dontwarn com.github.luben.zstd.**
