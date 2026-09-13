# kotlinx.serialization keeps its generated serializers on the companion.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.claudianapolitano.leyla.** {
    *** Companion;
}
-keepclasseswithmembers class com.claudianapolitano.leyla.** {
    kotlinx.serialization.KSerializer serializer(...);
}
