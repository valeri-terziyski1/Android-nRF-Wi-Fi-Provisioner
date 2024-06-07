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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.wifi.provisioner.ble.view.BleViewEntity
import no.nordicsemi.android.wifi.provisioner.ble.view.OnUnprovisionEvent
import no.nordicsemi.android.wifi.provisioner.feature.ble.R
import no.nordicsemi.kotlin.wifi.provisioner.feature.common.event.OnFinishedEvent
import no.nordicsemi.kotlin.wifi.provisioner.feature.common.event.OnProvisionClickEvent
import no.nordicsemi.kotlin.wifi.provisioner.feature.common.event.OnProvisionNextDeviceEvent
import no.nordicsemi.kotlin.wifi.provisioner.feature.common.event.OnSelectWifiEvent
import no.nordicsemi.kotlin.wifi.provisioner.feature.common.event.OnShowPasswordDialog
import no.nordicsemi.kotlin.wifi.provisioner.feature.common.event.ProvisioningViewEvent

@Composable
fun ActionButtonSection(viewEntity: BleViewEntity, onEvent: (ProvisioningViewEvent) -> Unit) {
    if (viewEntity.isRunning()) {
        return
    }
    if (viewEntity.device == null) {
        ActionButton(stringResource(id = R.string.start)) {
            onEvent(OnProvisionNextDeviceEvent)
        }
    } else if (viewEntity.hasFinishedWithSuccess()) {
        ActionButton(stringResource(id = R.string.next_device)) {
            onEvent(OnProvisionNextDeviceEvent)
        }
    } else if (viewEntity.hasFinished()) {
        ActionButton(stringResource(id = R.string.finish)) {
            onEvent(OnFinishedEvent)
        }
    } else if (!viewEntity.isStatusSuccess()) {
        //I think it's here to prevent entering somewhere else.
    } else if (viewEntity.isUnprovisioning()) {
        ActionButton(stringResource(id = R.string.unprovision)) {
            onEvent(OnUnprovisionEvent)
        }
    } else if (viewEntity.isStatusSuccess() && viewEntity.network == null) {
        ActionButton(stringResource(id = R.string.start_provisioning)) {
            onEvent(OnSelectWifiEvent)
        }
    } else if (viewEntity.network!!.isPasswordRequired() && viewEntity.password == null) {
        ActionButton(stringResource(id = R.string.password_select)) {
            onEvent(OnShowPasswordDialog)
        }
    } else {
        ActionButton(stringResource(id = R.string.provision)) {
            onEvent(OnProvisionClickEvent)
        }
    }
}

@Composable
private fun ActionButton(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = 16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        ElevatedButton(
            onClick = onClick,
            modifier = Modifier
                .padding(all = 16.dp)
                //.align(Alignment.Center)
                .widthIn(min = 100.dp)
        ) {
            Text(text = text)
        }
    }
}