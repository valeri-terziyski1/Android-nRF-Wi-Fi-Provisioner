package no.nordicsemi.android.wifi.provisioner.softap.view

import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiFind
import androidx.compose.material.icons.filled.WifiPassword
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import no.nordicsemi.android.common.logger.view.LoggerAppBarIcon
import no.nordicsemi.android.common.theme.view.NordicAppBar
import no.nordicsemi.android.common.theme.view.ProgressItem
import no.nordicsemi.android.common.theme.view.ProgressItemStatus
import no.nordicsemi.android.common.theme.view.WizardStepAction
import no.nordicsemi.android.common.theme.view.WizardStepComponent
import no.nordicsemi.android.common.theme.view.WizardStepState
import no.nordicsemi.android.common.theme.view.getWiFiRes
import no.nordicsemi.android.wifi.provisioner.feature.softap.R
import no.nordicsemi.android.wifi.provisioner.softap.FailedToBindToNetwork
import no.nordicsemi.android.wifi.provisioner.softap.OnConnectionLost
import no.nordicsemi.android.wifi.provisioner.softap.Open
import no.nordicsemi.android.wifi.provisioner.softap.PassphraseConfiguration
import no.nordicsemi.android.wifi.provisioner.softap.UnableToConnectToNetwork
import no.nordicsemi.android.wifi.provisioner.softap.WifiNotEnabledException
import no.nordicsemi.android.wifi.provisioner.softap.viewmodel.SoftApScreenState
import no.nordicsemi.android.wifi.provisioner.ui.PasswordDialog
import no.nordicsemi.android.wifi.provisioner.ui.SelectChannelDialog
import no.nordicsemi.android.wifi.provisioner.ui.mapping.toDisplayString
import no.nordicsemi.android.wifi.provisioner.ui.mapping.toImageVector
import no.nordicsemi.kotlin.wifi.provisioner.domain.AuthModeDomain
import no.nordicsemi.kotlin.wifi.provisioner.domain.ScanRecordDomain
import no.nordicsemi.kotlin.wifi.provisioner.feature.common.ScanRecordsForSsid
import no.nordicsemi.kotlin.wifi.provisioner.feature.common.WifiData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoftApScreen(
    context: Context,
    state: SoftApScreenState,
    onLoggerAppBarIconPressed: () -> Unit,
    start: (String, PassphraseConfiguration) -> Unit,
    onSelectWifiPressed: () -> Unit,
    onPasswordEntered: (String) -> Unit,
    onProvisionPressed: () -> Unit,
    verify: () -> Unit,
    navigateUp: (() -> Unit),
    resetError: () -> Unit
) {
    var ssidName by rememberSaveable { mutableStateOf("nrf-wifiprov") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    if (state.error != null) {
        showSnackBar(
            context = context,
            scope = scope,
            snackbarHostState = snackbarHostState,
            throwable = state.error
        ) {
            resetError()
        }
    }
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            NordicAppBar(
                text = stringResource(id = R.string.provision_over_wifi),
                actions = {
                    LoggerAppBarIcon(onClick = onLoggerAppBarIconPressed)
                },
                showBackButton = true,
                onNavigationButtonClick = navigateUp
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            OutlinedCard(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp, horizontal = 16.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ConfigureSoftAp(
                        configureState = state.configureState,
                        connectionState = state.connectionState,
                        ssidName = ssidName,
                        onSsidChange = { ssidName = it }
                    )
                    ConnectToSoftAp(
                        connectionState = state.connectionState,
                        serviceDiscoveryState = state.discoveringServicesState,
                        isConnectionRequested = state.isConnectionRequested,
                        ssidName = ssidName,
                        start = {
                            start(ssidName, Open)
                        }
                    )
                    SelectWifi(
                        provisioningState = state.provisionState,
                        selectWifiState = state.selectWifiState,
                        wifiData = state.selectedWifi,
                        onSelectWifiPressed = onSelectWifiPressed,
                        onPasswordEntered = onPasswordEntered,
                    )
                    SetPassphrase(
                        provisioningState = state.provisionState,
                        providePasswordState = state.providePasswordState,
                        wifiData = state.selectedWifi,
                        password = state.password,
                        onPasswordEntered = onPasswordEntered
                    )
                    Provisioning(
                        passwordState = state.providePasswordState,
                        provisioningState = state.provisionState,
                        isProvisioningRequested = state.isProvisioningRequested,
                        onProvisionPressed = onProvisionPressed
                    )
                    Verify(
                        provisioningState = state.provisionState,
                        verificationState = state.verifyState,
                        isVerificationRequested = state.isVerificationRequested,
                        verify = verify
                    )
                }
            }
        }
    }
}

private fun showSnackBar(
    context: Context,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    throwable: Throwable,
    onDismissed: () -> Unit
) {
    scope.launch {
        val message = when (throwable) {
            is WifiNotEnabledException -> context.getString(R.string.please_enable_wi_fi)
            is FailedToBindToNetwork -> context.getString(R.string.failed_to_bind_to_network)
            is UnableToConnectToNetwork -> context.getString(R.string.unable_to_connect_to_network)
            is OnConnectionLost -> context.getString(R.string.connection_lost)
            is TimeoutCancellationException -> context.getString(R.string.verification_timed_out_please_retry)
            else -> "${throwable::class.simpleName}: ${throwable.message}"
        }
        val result = snackbarHostState.showSnackbar(message = message)
        if (result == SnackbarResult.Dismissed) onDismissed()
    }
}


@Composable
private fun ConfigureSoftAp(
    configureState: WizardStepState,
    connectionState: WizardStepState,
    ssidName: String,
    onSsidChange: (String) -> Unit,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    WizardStepComponent(
        icon = Icons.Default.Settings,
        title = stringResource(R.string.configure),
        state = configureState,
        decor = if (connectionState == WizardStepState.COMPLETED
            && configureState == WizardStepState.COMPLETED
        ) {
            null
        } else if (configureState != WizardStepState.INACTIVE) WizardStepAction.Action(
            text = stringResource(id = R.string.edit_ssid),
            onClick = {
                showDialog = true
            }
        ) else null,
        showVerticalDivider = true
    ) {
        Text(style = MaterialTheme.typography.bodyMedium, text = "SSID: $ssidName")
    }
    if (showDialog) {
        EditSsidDialog(
            ssidName = ssidName,
            onSsidChange = {
                onSsidChange(it)
                showDialog = false
            },
            dismiss = {
                showDialog = false
            }
        )
    }
}

@Composable
private fun ConnectToSoftAp(
    connectionState: WizardStepState,
    isConnectionRequested: Boolean,
    serviceDiscoveryState: WizardStepState,
    ssidName: String,
    start: () -> Unit,
) {
    WizardStepComponent(
        icon = Icons.Default.Wifi,
        title = stringResource(id = R.string.connect),
        state = connectionState,
        decor = if (connectionState == WizardStepState.COMPLETED &&
            serviceDiscoveryState == WizardStepState.COMPLETED
        ) {
            null
        } else WizardStepAction.Action(
            text = stringResource(id = R.string.start),
            onClick = start,
        ),
        showVerticalDivider = false,
    ) {
        ProgressItem(
            text = when {
                isConnectionRequested && connectionState == WizardStepState.CURRENT ->
                    stringResource(id = R.string.connecting)

                connectionState == WizardStepState.COMPLETED ->
                    stringResource(id = R.string.connected)

                else -> stringResource(id = R.string.connect)
            },
            status = when {
                isConnectionRequested && connectionState == WizardStepState.CURRENT -> ProgressItemStatus.WORKING
                connectionState == WizardStepState.COMPLETED -> ProgressItemStatus.SUCCESS
                else -> ProgressItemStatus.DISABLED
            },
            iconRightPadding = 24.dp,
        )
        ProgressItem(
            text = when (serviceDiscoveryState) {
                WizardStepState.INACTIVE -> stringResource(id = R.string.discover_services)
                WizardStepState.CURRENT -> stringResource(id = R.string.discovering_services)
                else -> stringResource(id = R.string.discovered_services)
            },
            status = when (serviceDiscoveryState) {
                WizardStepState.CURRENT -> ProgressItemStatus.WORKING
                WizardStepState.COMPLETED -> ProgressItemStatus.SUCCESS
                else -> ProgressItemStatus.DISABLED
            },
            iconRightPadding = 24.dp,
        )
    }
}

@Composable
private fun SelectWifi(
    provisioningState: WizardStepState,
    selectWifiState: WizardStepState,
    wifiData: WifiData?,
    onSelectWifiPressed: () -> Unit,
    onPasswordEntered: (String) -> Unit,
) {
    WizardStepComponent(
        icon = Icons.Default.WifiFind,
        title = stringResource(id = R.string.wifi_network),
        state = selectWifiState,
        decor = if (selectWifiState == WizardStepState.INACTIVE) null
        else if ((provisioningState == WizardStepState.CURRENT
                    && selectWifiState == WizardStepState.COMPLETED)
            || (provisioningState == WizardStepState.COMPLETED
                    && selectWifiState == WizardStepState.COMPLETED)
        ) {
            null
        } else WizardStepAction.Action(
            text = stringResource(id = R.string.select),
            onClick = onSelectWifiPressed
        ),
        showVerticalDivider = true
    ) {
        if (wifiData != null && selectWifiState != WizardStepState.INACTIVE) {
            Text(style = MaterialTheme.typography.bodyMedium, text = "SSID: ${wifiData.ssid}")
            Text(style = MaterialTheme.typography.bodyMedium,
                text = "Band: ${
                    wifiData.let {
                        it.selectedChannel?.wifiInfo?.band?.toDisplayString()
                            ?: it.channelFallback.wifiInfo?.band?.toDisplayString()
                    }
                }"
            )
        } else {
            Text(
                style = MaterialTheme.typography.bodyMedium,
                text = stringResource(R.string.select_wifi)
            )
        }
    }
}

@Composable
private fun SetPassphrase(
    providePasswordState: WizardStepState,
    provisioningState: WizardStepState,
    wifiData: WifiData?,
    password: String? = null,
    onPasswordEntered: (String) -> Unit,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    WizardStepComponent(
        icon = wifiData?.authMode?.toImageVector() ?: Icons.Default.WifiPassword,
        title = stringResource(id = R.string.security),
        state = providePasswordState,
        decor = if (providePasswordState == WizardStepState.INACTIVE) {
            null
        } else if ((provisioningState == WizardStepState.CURRENT
                    && providePasswordState == WizardStepState.COMPLETED)
            || (provisioningState == WizardStepState.COMPLETED
                    && providePasswordState == WizardStepState.COMPLETED)
        ) {
            null
        } else if (providePasswordState == WizardStepState.CURRENT) WizardStepAction.Action(
            text = stringResource(id = R.string.set),
            onClick = { showDialog = true }
        ) else WizardStepAction.Action(
            text = stringResource(id = R.string.set),
            onClick = { showDialog = true }
        ),
        showVerticalDivider = true
    ) {

        if (wifiData != null) {
            Text(
                style = MaterialTheme.typography.bodyMedium,
                text = "Security: ${wifiData.authMode.toDisplayString()}"
            )

            if (wifiData.authMode == AuthModeDomain.OPEN) {
                Text(
                    style = MaterialTheme.typography.bodyMedium,
                    text = stringResource(R.string.empty_passphrase_rationale)
                )
            } else
                if (password != null) {
                    Text(
                        style = MaterialTheme.typography.bodyMedium,
                        text = stringResource(
                            R.string.set_passphrase_value,
                            password.toPassphrase()
                        )
                    )
                }
        } else {
            Text(
                style = MaterialTheme.typography.bodyMedium,
                text = stringResource(R.string.se_wifi_passphrase)
            )
        }
        if (showDialog) {
            PasswordDialog(
                onConfirmPressed = {
                    onPasswordEntered(it)
                    showDialog = false
                },
                onDismiss = { showDialog = false }
            )
        }
    }
}

@Composable
private fun Provisioning(
    passwordState: WizardStepState,
    provisioningState: WizardStepState,
    isProvisioningRequested: Boolean,
    onProvisionPressed: () -> Unit
) {
    WizardStepComponent(
        icon = Icons.Outlined.NetworkCheck,
        title = stringResource(id = R.string.provision),
        state = provisioningState,
        decor = if (isProvisioningRequested && provisioningState == WizardStepState.CURRENT) {
            WizardStepAction.ProgressIndicator
        } else if (passwordState == WizardStepState.COMPLETED &&
            provisioningState == WizardStepState.CURRENT
        ) {
            WizardStepAction.Action(
                text = stringResource(id = R.string.provision),
                onClick = onProvisionPressed
            )
        } else if (passwordState == WizardStepState.COMPLETED &&
            provisioningState == WizardStepState.COMPLETED
        ) {
            null
        } else null,
        showVerticalDivider = false
    ) {
        ProgressItem(
            text = when {
                isProvisioningRequested && provisioningState == WizardStepState.CURRENT -> stringResource(
                    R.string.provisioning_device_to_your_network
                )

                provisioningState == WizardStepState.COMPLETED -> stringResource(R.string.provisioning_completed)
                provisioningState == WizardStepState.INACTIVE -> stringResource(R.string.provisioning_rationale)
                else -> stringResource(R.string.provisioning_rationale)
            },
            status = when {
                isProvisioningRequested && provisioningState == WizardStepState.CURRENT -> ProgressItemStatus.WORKING
                provisioningState == WizardStepState.COMPLETED -> ProgressItemStatus.SUCCESS
                else -> ProgressItemStatus.DISABLED
            },
            iconRightPadding = 24.dp,
        )
    }
}

@Composable
private fun Verify(
    provisioningState: WizardStepState,
    verificationState: WizardStepState,
    isVerificationRequested: Boolean,
    verify: () -> Unit
) {
    WizardStepComponent(
        icon = Icons.Default.Verified,
        title = stringResource(id = R.string.verify),
        state = verificationState,
        decor = if (provisioningState == WizardStepState.COMPLETED && !isVerificationRequested && verificationState == WizardStepState.CURRENT) {
            WizardStepAction.Action(
                text = stringResource(id = R.string.verify),
                onClick = {
                    verify()
                }
            )
        } else if (verificationState == WizardStepState.CURRENT) {
            WizardStepAction.ProgressIndicator
        } else null,
        showVerticalDivider = false
    ) {
        ProgressItem(
            text = when {
                isVerificationRequested && verificationState == WizardStepState.CURRENT ->
                    stringResource(R.string.locating_provisioned_device)

                verificationState == WizardStepState.CURRENT -> stringResource(R.string.optional_verification_rationale)
                verificationState == WizardStepState.COMPLETED -> stringResource(R.string.verification_completed)
                else -> stringResource(R.string.optional_verification_rationale)
            },
            status = when {
                isVerificationRequested && verificationState == WizardStepState.CURRENT -> ProgressItemStatus.WORKING
                verificationState == WizardStepState.COMPLETED -> ProgressItemStatus.SUCCESS
                else -> ProgressItemStatus.DISABLED
            },
            iconRightPadding = 24.dp,
        )
    }
}


@Composable
private fun WifiItem(
    record: ScanRecordsForSsid,
    onWifiSelected: (ScanRecordsForSsid) -> Unit = {}
) {
    val wifiData = record.wifiData
    val selectedScanRecord = remember { mutableStateOf<ScanRecordDomain?>(null) }
    val scanRecord = selectedScanRecord.value
    val wifi = scanRecord?.wifiInfo

    val showSelectChannelDialog = rememberSaveable { mutableStateOf(false) }

    if (showSelectChannelDialog.value) {
        SelectChannelDialog(
            records = record,
            onDismiss = { showSelectChannelDialog.value = false }
        ) {
            selectedScanRecord.value = it
            showSelectChannelDialog.value = false
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable {
                onWifiSelected(record)
            }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = wifiData.ssid, style = MaterialTheme.typography.labelLarge)

            if (wifi != null) {
                if (wifi.macAddress.isNotEmpty()) {
                    Text(
                        text = stringResource(id = R.string.bssid, wifi.macAddress),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (wifi.band != null) {
                    Text(
                        text = stringResource(
                            id = R.string.band_and_channel,
                            wifi.band!!.toDisplayString(),
                            wifi.channel.toString()
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        text = stringResource(id = R.string.channel, wifi.channel.toString()),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                Text(
                    text = stringResource(id = R.string.channel, stringResource(id = R.string.any)),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        val displayRssi = scanRecord?.rssi ?: record.biggestRssi

        Row(modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { showSelectChannelDialog.value = true }
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface,
                RoundedCornerShape(10.dp)
            )
            .padding(9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(getWiFiRes(displayRssi), contentDescription = "")

            Icon(Icons.Default.ArrowDropDown, contentDescription = "")
        }
    }
}

private fun String.toPassphrase(): String {
    var password = ""
    repeat(length) {
        password += '*'
    }
    return password
}