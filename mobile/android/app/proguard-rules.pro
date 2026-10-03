# kotlinx.serialization: conservar serializadores generados de los modelos de la API.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class com.soyvinko.app.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.soyvinko.app.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.soyvinko.app.data.**$$serializer { *; }
