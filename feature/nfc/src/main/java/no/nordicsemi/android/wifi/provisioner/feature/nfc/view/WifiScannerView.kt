package no.nordicsemi.android.wifi.provisioner.feature.nfc.view

import android.net.wifi.ScanResult
import android.net.wifi.WifiSsid
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.GroupRemove
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import no.nordicsemi.android.common.theme.view.NordicAppBar
import no.nordicsemi.android.wifi.provisioner.feature.nfc.R
import no.nordicsemi.android.wifi.provisioner.feature.nfc.mapping.Frequency
import no.nordicsemi.android.wifi.provisioner.feature.nfc.mapping.toDisplayString
import no.nordicsemi.android.wifi.provisioner.feature.nfc.permission.RequireLocationForWifi
import no.nordicsemi.android.wifi.provisioner.feature.nfc.permission.RequireWifi
import no.nordicsemi.android.wifi.provisioner.feature.nfc.uicomponent.PasswordDialog
import no.nordicsemi.android.wifi.provisioner.feature.nfc.uicomponent.RssiIconView
import no.nordicsemi.android.wifi.provisioner.feature.nfc.uicomponent.VerticalBlueBar
import no.nordicsemi.android.wifi.provisioner.feature.nfc.viewmodel.OnNavigateUpClickEvent
import no.nordicsemi.android.wifi.provisioner.feature.nfc.viewmodel.OnNetworkSelectEvent
import no.nordicsemi.android.wifi.provisioner.feature.nfc.viewmodel.OnPasswordCancelEvent
import no.nordicsemi.android.wifi.provisioner.feature.nfc.viewmodel.OnPasswordSetEvent
import no.nordicsemi.android.wifi.provisioner.feature.nfc.viewmodel.OnSortOptionSelected
import no.nordicsemi.android.wifi.provisioner.feature.nfc.viewmodel.WifiScannerViewEvent
import no.nordicsemi.android.wifi.provisioner.feature.nfc.viewmodel.WifiScannerViewModel
import no.nordicsemi.android.wifi.provisioner.nfc.domain.AuthenticationMode
import no.nordicsemi.android.wifi.provisioner.nfc.domain.EncryptionMode
import no.nordicsemi.android.wifi.provisioner.nfc.domain.Error
import no.nordicsemi.android.wifi.provisioner.nfc.domain.Loading
import no.nordicsemi.android.wifi.provisioner.nfc.domain.Success
import no.nordicsemi.android.wifi.provisioner.nfc.domain.WifiAuthTypeBelowTiramisu
import no.nordicsemi.android.wifi.provisioner.nfc.domain.WifiAuthTypeTiramisuOrAbove
import no.nordicsemi.android.wifi.provisioner.nfc.domain.WifiData
import no.nordicsemi.android.wifi.provisioner.ui.view.WifiSortView

/**
 * A composable function to display the list of available networks.
 */
@RequiresApi(Build.VERSION_CODES.M)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WifiScannerScreen() {
    val wifiScannerViewModel = hiltViewModel<WifiScannerViewModel>()
    val onEvent: (WifiScannerViewEvent) -> Unit = { wifiScannerViewModel.onEvent(it) }
    val viewState by wifiScannerViewModel.viewState.collectAsStateWithLifecycle()
    var isGroupedBySsid by rememberSaveable { mutableStateOf(false) }
    val groupIcon = if (isGroupedBySsid) Icons.Outlined.GroupRemove else Icons.Outlined.Group
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle the back press.
    BackHandler {
        onEvent(OnNavigateUpClickEvent)
    }
    // Show the scanning screen.
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            NordicAppBar(
                text = stringResource(id = R.string.wifi_scanner_appbar),
                showBackButton = true,
                onNavigationButtonClick = { onEvent(OnNavigateUpClickEvent) },
                actions = {
                    // Show the group icon to group by SSID.
                    Icon(
                        imageVector = groupIcon,
                        contentDescription = null,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                isGroupedBySsid = !isGroupedBySsid
                            }
                            .padding(8.dp)

                    )
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.padding(innerPadding)
        ) {
            RequireWifi {
                RequireLocationForWifi {
                    LaunchedEffect(key1 = it) {
                        wifiScannerViewModel.scanAvailableWifiNetworks()
                    }

                    when (val scanningState = viewState.networks) {
                        is Error -> {
                            // Show the error message.
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = stringResource(id = R.string.error_while_scanning))
                                    Text(
                                        text = scanningState.t.message ?: "Unknown error occurred."
                                    )
                                }
                            }
                        }

                        is Loading -> {
                            // Show the loading indicator.
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }

                        is Success -> {
                            // Show the list of available networks.
                            Column {
                                WifiSortView(viewState.sortOption) {
                                    onEvent(OnSortOptionSelected(it))
                                }
                                PullToRefreshBox(
                                    isRefreshing = viewState.isRefreshing,
                                    onRefresh = { // Refreshing
                                        wifiScannerViewModel.scanAvailableWifiNetworks()
                                    },
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .verticalScroll(rememberScrollState())
                                            .padding(bottom = 32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        if (isGroupedBySsid) {
                                            // Group by SSID
                                            GroupBySsid(viewState.sortedItems, onEvent)
                                        } else {
                                            // Show the list of available networks grouped by SSIDs.
                                            WifiList(viewState.sortedItems, onEvent)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    when (val selectedNetwork = viewState.selectedNetwork) {
                        null -> {
                            // Do nothing
                        }

                        else -> {
                            // Show the password dialog
                            PasswordDialog(
                                scanResult = selectedNetwork,
                                onCancelClick = {
                                    // Dismiss the dialog
                                    // Set the selected network to null
                                    onEvent(OnPasswordCancelEvent)
                                }) {
                                onEvent(OnPasswordSetEvent(it))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * A composable function to display the list of available networks.
 *
 * @param networks The list of available networks.
 * @param onEvent The event callback.
 */
@Composable
internal fun WifiList(
    networks: List<ScanResult>,
    onEvent: (WifiScannerViewEvent) -> Unit
) {
    networks.forEach { network ->
        NetworkItem(
            network = network,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
            onEvent = onEvent
        )
        HorizontalDivider()
    }
}

@Composable
private fun NetworkItem(
    network: ScanResult,
    modifier: Modifier = Modifier,
    onEvent: (WifiScannerViewEvent) -> Unit,
) {
    val securityType = AuthenticationMode.get(network)
    val isOpen = securityType.contains(WifiAuthTypeBelowTiramisu.OPEN) or
            securityType.contains(WifiAuthTypeTiramisuOrAbove.OPEN)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                if (isOpen) {
                    // Password dialog is not required for open networks.
                    val wifiData = WifiData(
                        ssid = network.SSID,
                        macAddress = network.BSSID,
                        password = "", // Empty password for open networks
                        authType = WifiAuthTypeBelowTiramisu.OPEN,
                        encryptionMode = EncryptionMode.NONE
                    )
                    onEvent(OnPasswordSetEvent(wifiData))
                } else {
                    // Show the password dialog
                    onEvent(OnNetworkSelectEvent(network))
                }
            },
    ) {
        RssiIconView(network.level)
        Column(
            modifier = Modifier
                .padding(8.dp)
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Display the SSID of the access point
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Text(
                    text = getSSid(network.wifiSsid),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                Text(
                    text = network.SSID,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            // Display the address of the access point.
            Text(
                text = network.BSSID.uppercase(),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.alpha(0.7f)
            )
            // Display the security type of the access point.
            Text(
                text = securityType.joinToString(", ") { it.toDisplayString() },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.alpha(0.7f)
            )
            Text(
                text = stringResource(
                    id = R.string.channel_number,
                    Frequency.toChannelNumber(network.frequency),
                    Frequency.get(network.frequency)
                ),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.alpha(0.7f),
            )
        }
        if (!isOpen) {
            // Show the lock icon for protected networks.
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
            )
        }
    }
}

/**
 * Returns the SSID of the network from given [WifiSsid].
 */
fun getSSid(wifiSsid: WifiSsid?): String {
    return wifiSsid.toString().replace("\"", "")
}

@Composable
private fun GroupBySsid(
    networks: List<ScanResult>,
    onEvent: (WifiScannerViewEvent) -> Unit,
) {
    networks.groupBy { it.SSID }.forEach { (ssid, network) ->
        var isExpanded by rememberSaveable { mutableStateOf(false) }
        val expandIcon =
            if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore

        // Skip hidden networks.
        if (ssid == null || ssid.isEmpty()) {
            return@forEach
        }
        // Show the network.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(start = 16.dp, 8.dp)
        ) {
            Text(text = ssid)
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = expandIcon,
                contentDescription = null,
                modifier = Modifier.padding(8.dp)
            )
        }
        // Show networks under the same SSID.
        AnimatedVisibility(visible = isExpanded) {
            Column {
                HorizontalDivider()
                VerticalBlueBar {
                    network.forEach { scanResult ->
                        NetworkItem(
                            network = scanResult,
                            modifier = Modifier.padding(8.dp),
                            onEvent = onEvent
                        )
                        if (scanResult != network.last()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 16.dp),
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }
        }
        HorizontalDivider()
    }
}