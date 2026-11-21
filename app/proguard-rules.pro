# kotlinx.serialization ships its own consumer rules; keep our models' generated serializers explicitly too.
-keep,includedescriptorclasses class app.ondea.data.**$$serializer { *; }
-keepclassmembers class app.ondea.data.** {
    *** Companion;
}
-keepclasseswithmembers class app.ondea.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
