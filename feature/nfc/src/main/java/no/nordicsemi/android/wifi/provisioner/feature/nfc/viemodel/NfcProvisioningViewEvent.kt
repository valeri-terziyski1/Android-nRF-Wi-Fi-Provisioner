package no.nordicsemi.android.wifi.provisioner.feature.nfc.viemodel

/**
 * A sealed class to represent the events that can be triggered from the UI.
 */
internal sealed interface NfcProvisioningViewEvent

/**
 * Event triggered when the wifi ssid scan button is clicked.
 */
internal data object OnScanClickEvent : NfcProvisioningViewEvent

/**
 * Event triggered when the back button is clicked.
 */
internal data object OnBackClickEvent: NfcProvisioningViewEvent
