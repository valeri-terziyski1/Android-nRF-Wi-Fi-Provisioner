plugins {
    alias(libs.plugins.nordic.library)
    alias(libs.plugins.nordic.kotlin)
    alias(libs.plugins.nordic.hilt)
}

android {
    namespace = "no.nordicsemi.android.wifi.provisioner.nfc"
}

dependencies {
    implementation(libs.nordic.ble.ktx)
    implementation(libs.nordic.ble.common)
}