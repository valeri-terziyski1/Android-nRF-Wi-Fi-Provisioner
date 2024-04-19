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

package no.nordicsemi.android.wifi.provisioner.ble.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import no.nordicsemi.android.wifi.provisioner.ble.view.BleProvisioningViewEvent
import no.nordicsemi.android.wifi.provisioner.ble.view.OnSelectWifiEvent
import no.nordicsemi.kotlin.wifi.provisioner.domain.ScanRecordDomain
import no.nordicsemi.android.wifi.provisioner.ble.wifi.view.WifiData
import no.nordicsemi.android.wifi.provisioner.feature.ble.R
import no.nordicsemi.android.wifi.provisioner.ui.ClickableDataItem
import no.nordicsemi.android.wifi.provisioner.ui.mapping.toImageVector

@Composable
internal fun WifiSection(
    record: WifiData,
    isEditable: Boolean = false,
    onEvent: (BleProvisioningViewEvent) -> Unit
) {
    Column {
        ClickableDataItem(
            imageVector = record.authMode.toImageVector(),
            title = stringResource(id = R.string.selected_wifi),
            isEditable = isEditable,
            description = record.selectedChannel?.let { getDescription(record = it) } ?: record.ssid,
            onClick =  {
                onEvent(OnSelectWifiEvent)
            },
            buttonText = stringResource(id = R.string.change_device)
        )
    }
}

@Composable
private fun getDescription(record: ScanRecordDomain): String {
    return StringBuilder()
        .append(record.wifiInfo?.ssid)
        .appendLine()
        .append(stringResource(id = R.string.channel, record.wifiInfo?.channel.toString()))
        .toString()
}
