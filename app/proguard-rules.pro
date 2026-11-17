# kotlinx.serialization: keep generated serializers for the user's vault.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class io.github.akrishna87.policyvault.**$$serializer { *; }
-keepclassmembers class io.github.akrishna87.policyvault.** { *** Companion; }
-keepclasseswithmembers class io.github.akrishna87.policyvault.** { kotlinx.serialization.KSerializer serializer(...); }
