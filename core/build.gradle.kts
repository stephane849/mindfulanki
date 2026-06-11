plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.coroutines.core)
    implementation(libs.serialization.json)
    // zstd-jni works on both the JVM and Android, so the modern .apkg
    // (collection.anki21b) decompression lives here in shared code.
    implementation(libs.zstd.jni)

    testImplementation(libs.junit.jupiter)
    // sqlite-jdbc is JVM-only; it backs the test SqlQuerier so the readers can
    // be exercised end-to-end without an Android device. The app module supplies
    // an android.database-backed SqlQuerier instead.
    testImplementation(libs.sqlite.jdbc)
}

tasks.test {
    useJUnitPlatform()
}
