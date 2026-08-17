-dontwarn org.sqlite.**
-dontwarn org.slf4j.**
-dontwarn java.awt.**
-dontwarn sun.font.**
-dontwarn androidx.compose.**
-dontwarn kotlinx.coroutines.**

# Compose Desktop disables obfuscation in its generated release rules. Keep
# shrinking enabled, but retain SQLite's ServiceLoader and JNI callback classes.
-keep class org.sqlite.** { *; }

# ProGuard 7.6 produces invalid verifier types when optimizing current Kotlin,
# coroutines, and Compose bytecode.
-dontoptimize
