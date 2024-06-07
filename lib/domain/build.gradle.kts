plugins {
    alias(libs.plugins.nordic.library)
    alias(libs.plugins.nordic.kotlin)
    alias(libs.plugins.wire)
}

android {
    namespace = "no.nordicsemi.kotlin.wifi.provisioner.domain"
}

wire {
    kotlin {}
}