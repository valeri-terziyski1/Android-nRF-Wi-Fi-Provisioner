/*
 * Copyright (c) 2022, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list
 * of conditions and the following disclaimer in the documentation and/or other materials
 * provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be
 * used to endorse or promote products derived from this software without specific prior
 * written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package no.nordicsemi.android.wifi.provisioner.ble.domain

import android.util.Log
import no.nordicsemi.android.wifi.provisioner.ble.proto.AuthMode
import no.nordicsemi.android.wifi.provisioner.ble.proto.Band
import no.nordicsemi.android.wifi.provisioner.ble.proto.ConnectionFailureReason
import no.nordicsemi.android.wifi.provisioner.ble.proto.ConnectionInfo
import no.nordicsemi.android.wifi.provisioner.ble.proto.ConnectionState
import no.nordicsemi.android.wifi.provisioner.ble.proto.DeviceStatus
import no.nordicsemi.android.wifi.provisioner.ble.proto.ScanParams
import no.nordicsemi.android.wifi.provisioner.ble.proto.ScanRecord
import no.nordicsemi.android.wifi.provisioner.ble.proto.WifiInfo
import okio.ByteString

private const val TAG = "DOMAIN-MAPPER"

internal fun DeviceStatus.toDomain(): DeviceStatusDomain {
    Log.d(TAG, "status: $this")
    return DeviceStatusDomain(
        state?.toDomain(),
        provisioning_info?.toDomain(),
        connection_info?.toDomain(),
        scan_info?.toDomain()
    )
}

internal fun ScanParams.toDomain(): ScanParamsDomain {
    Log.d(TAG, "mapper: $this")
    return ScanParamsDomain(
        band!!.toDomain(),
        passive!!,
        period_ms!!,
        group_channels!!
    )
}

internal fun ConnectionInfo.toDomain(): ConnectionInfoDomain {
    return ConnectionInfoDomain(ip4_addr!!.toIp())
}

internal fun ConnectionState.toDomain(): WifiConnectionStateDomain {
    return when (this) {
        ConnectionState.DISCONNECTED -> WifiConnectionStateDomain.DISCONNECTED
        ConnectionState.AUTHENTICATION -> WifiConnectionStateDomain.AUTHENTICATION
        ConnectionState.ASSOCIATION -> WifiConnectionStateDomain.ASSOCIATION
        ConnectionState.OBTAINING_IP -> WifiConnectionStateDomain.OBTAINING_IP
        ConnectionState.CONNECTED -> WifiConnectionStateDomain.CONNECTED
        ConnectionState.CONNECTION_FAILED -> WifiConnectionStateDomain.CONNECTION_FAILED
    }
}

internal fun AuthMode.toDomain(): no.nordicsemi.kotlin.wifi.provisioner.domain.AuthModeDomain {
    return when (this) {
        AuthMode.OPEN -> no.nordicsemi.kotlin.wifi.provisioner.domain.AuthModeDomain.OPEN
        AuthMode.WEP -> no.nordicsemi.kotlin.wifi.provisioner.domain.AuthModeDomain.WEP
        AuthMode.WPA_PSK -> no.nordicsemi.kotlin.wifi.provisioner.domain.AuthModeDomain.WPA_PSK
        AuthMode.WPA2_PSK -> no.nordicsemi.kotlin.wifi.provisioner.domain.AuthModeDomain.WPA2_PSK
        AuthMode.WPA_WPA2_PSK -> no.nordicsemi.kotlin.wifi.provisioner.domain.AuthModeDomain.WPA_WPA2_PSK
        AuthMode.WPA2_ENTERPRISE -> no.nordicsemi.kotlin.wifi.provisioner.domain.AuthModeDomain.WPA2_ENTERPRISE
        AuthMode.WPA3_PSK -> no.nordicsemi.kotlin.wifi.provisioner.domain.AuthModeDomain.WPA3_PSK
    }
}

internal fun Band.toDomain(): BandDomain {
    return when (this) {
        Band.BAND_ANY -> BandDomain.BAND_ANY
        Band.BAND_2_4_GH -> BandDomain.BAND_2_4_GH
        Band.BAND_5_GH -> BandDomain.BAND_5_GH
    }
}

internal fun ConnectionFailureReason.toDomain(): WifiConnectionFailureReasonDomain {
    return when (this) {
        ConnectionFailureReason.AUTH_ERROR -> WifiConnectionFailureReasonDomain.AUTH_ERROR
        ConnectionFailureReason.NETWORK_NOT_FOUND -> WifiConnectionFailureReasonDomain.NETWORK_NOT_FOUND
        ConnectionFailureReason.TIMEOUT -> WifiConnectionFailureReasonDomain.TIMEOUT
        ConnectionFailureReason.FAIL_IP -> WifiConnectionFailureReasonDomain.FAIL_IP
        ConnectionFailureReason.FAIL_CONN -> WifiConnectionFailureReasonDomain.FAIL_CONN
    }
}

internal fun WifiInfo.toDomain(): no.nordicsemi.kotlin.wifi.provisioner.domain.WifiInfoDomain {
    return no.nordicsemi.kotlin.wifi.provisioner.domain.WifiInfoDomain(
        ssid.utf8(),
        bssid,
        band?.toDomain(),
        channel,
        auth?.toDomain()
    )
}

internal fun ScanRecord.toDomain(): no.nordicsemi.kotlin.wifi.provisioner.domain.ScanRecordDomain {
    Log.d(TAG, "mapper: $this")
    return no.nordicsemi.kotlin.wifi.provisioner.domain.ScanRecordDomain(rssi, wifi!!.toDomain())
}

internal fun ByteString.toIp(): String {
    return toByteArray().joinToString(".") {
        it.toUByte().toString()
    }
}
