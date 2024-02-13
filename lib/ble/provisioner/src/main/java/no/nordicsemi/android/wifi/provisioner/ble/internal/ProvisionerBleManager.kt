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
package no.nordicsemi.android.wifi.provisioner.ble.internal

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.ktx.asValidResponseFlow
import no.nordicsemi.android.ble.ktx.suspend
import no.nordicsemi.android.ble.ktx.suspendForValidResponse
import no.nordicsemi.android.wifi.provisioner.ble.proto.Result
import no.nordicsemi.android.wifi.provisioner.ble.proto.DeviceStatus
import no.nordicsemi.android.wifi.provisioner.ble.proto.Info
import no.nordicsemi.android.wifi.provisioner.ble.proto.OpCode
import no.nordicsemi.android.wifi.provisioner.ble.proto.Request
import no.nordicsemi.android.wifi.provisioner.ble.proto.Response
import no.nordicsemi.android.wifi.provisioner.ble.proto.Status
import no.nordicsemi.android.wifi.provisioner.ble.proto.WifiConfig
import java.util.*

val PROVISIONING_SERVICE_UUID: UUID = UUID.fromString("14387800-130c-49e7-b877-2881c89cb258")
private val VERSION_CHARACTERISTIC_UUID = UUID.fromString("14387801-130c-49e7-b877-2881c89cb258")
private val CONTROL_POINT_CHARACTERISTIC_UUID =
    UUID.fromString("14387802-130c-49e7-b877-2881c89cb258")
private val DATA_OUT_CHARACTERISTIC_UUID = UUID.fromString("14387803-130c-49e7-b877-2881c89cb258")

private const val TIMEOUT_MILLIS = 60_000L

internal class ProvisionerBleManager(
    context: Context,
) : BleManager(context) {

    private var versionCharacteristic: BluetoothGattCharacteristic? = null
    private var controlPointCharacteristic: BluetoothGattCharacteristic? = null
    private var dataOutCharacteristic: BluetoothGattCharacteristic? = null

    private var useLongWrite = true

    val dataHolder = ConnectionObserverAdapter()

    init {
        connectionObserver = dataHolder
    }

    override fun log(priority: Int, message: String) {
        //logger.log(priority, message)
    }

    override fun getMinLogPriority(): Int {
        return Log.VERBOSE
    }

    @SuppressLint("MissingPermission")
    suspend fun start(device: BluetoothDevice): Flow<ConnectionStatus> {
        return try {
            connect(device)
                .useAutoConnect(false)
                .retry(3, 100)
                .suspend()
            createBondInsecure().suspend()
            dataHolder.status
        } catch (e: Exception) {
            e.printStackTrace()
            flow { emit(ConnectionStatus.DISCONNECTED) }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun release() {
        removeBond().suspend()
        disconnect().suspend()
    }

    @SuppressLint("WrongConstant")
    override fun initialize() {
        requestMtu(512).enqueue()
        enableIndications(controlPointCharacteristic).enqueue()
        enableNotifications(dataOutCharacteristic).enqueue()
    }

    override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
        val service: BluetoothGattService? = gatt.getService(PROVISIONING_SERVICE_UUID)
        if (service != null) {
            versionCharacteristic = service.getCharacteristic(VERSION_CHARACTERISTIC_UUID)
            controlPointCharacteristic =
                service.getCharacteristic(CONTROL_POINT_CHARACTERISTIC_UUID)
            dataOutCharacteristic = service.getCharacteristic(DATA_OUT_CHARACTERISTIC_UUID)
        }
        var writeRequest: Boolean

        controlPointCharacteristic?.let {
            val rxProperties: Int = it.properties
            writeRequest = rxProperties and BluetoothGattCharacteristic.PROPERTY_WRITE > 0

            // Set the WRITE REQUEST type when the characteristic supports it.
            // This will allow to send long write (also if the characteristic support it).
            // In case there is no WRITE REQUEST property, this manager will divide texts
            // longer then MTU-3 bytes into up to MTU-3 bytes chunks.
            if (!writeRequest) {
                useLongWrite = false
            }
        }
        return versionCharacteristic != null && controlPointCharacteristic != null && dataOutCharacteristic != null
    }

    override fun onServicesInvalidated() {
        versionCharacteristic = null
        controlPointCharacteristic = null
        dataOutCharacteristic = null
        useLongWrite = true
    }

    suspend fun getVersion(): Info = withTimeout(TIMEOUT_MILLIS) {
        val response =
            readCharacteristic(versionCharacteristic).suspendForValidResponse<ByteArrayReadResponse>().value

        Info.ADAPTER.decode(response)
    }

    suspend fun getStatus(): DeviceStatus = withTimeout(TIMEOUT_MILLIS) {
        val request = Request(op_code = OpCode.GET_STATUS)

        val response = waitForIndication(controlPointCharacteristic)
            .trigger(
                writeCharacteristic(
                    controlPointCharacteristic,
                    request.encode(),
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
            )
            .suspendForValidResponse<ByteArrayReadResponse>()

        verifyResponseSuccess(response.value)

        Response.ADAPTER.decode(response.value).device_status!!
    }

    fun startScan() = callbackFlow {
        val request = Request(op_code = OpCode.START_SCAN)

        val timeoutJob = launch {
            delay(TIMEOUT_MILLIS)
            throw NotificationTimeoutException()
        }

        setNotificationCallback(dataOutCharacteristic)
            .asValidResponseFlow<ByteArrayReadResponse>()
            .onEach {
                timeoutJob.cancel()

                val result = Result.ADAPTER.decode(it.value)
                trySend(result.scan_record!!)
            }
            .launchIn(this)

        val response = waitForIndication(controlPointCharacteristic)
            .trigger(
                writeCharacteristic(
                    controlPointCharacteristic,
                    request.encode(),
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
            )
            .suspendForValidResponse<ByteArrayReadResponse>()

        verifyResponseSuccess(response.value)

        awaitClose {
            removeNotificationCallback(dataOutCharacteristic)
        }
    }

    suspend fun stopScan() {
        val request = Request(op_code = OpCode.STOP_SCAN)
        val response = waitForIndication(controlPointCharacteristic)
            .trigger(
                writeCharacteristic(
                    controlPointCharacteristic,
                    request.encode(),
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
            )
            .suspendForValidResponse<ByteArrayReadResponse>()

        verifyResponseSuccess(response.value)
    }

    fun provision(config: WifiConfig) = callbackFlow {
        val request = Request(op_code = OpCode.SET_CONFIG, config = config)

        val timeoutJob = launch {
            delay(TIMEOUT_MILLIS)
            throw NotificationTimeoutException()
        }

        setNotificationCallback(dataOutCharacteristic)
            .asValidResponseFlow<ByteArrayReadResponse>()
            .onEach {
                timeoutJob.cancel()

                val result = Result.ADAPTER.decode(it.value)
                trySend(result.state!!)
            }.launchIn(this)

        val response = waitForIndication(controlPointCharacteristic)
            .trigger(
                writeCharacteristic(
                    controlPointCharacteristic,
                    request.encode(),
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
            )
            .suspendForValidResponse<ByteArrayReadResponse>()

        verifyResponseSuccess(response.value)

        awaitClose {
            removeNotificationCallback(dataOutCharacteristic)
        }
    }

    suspend fun forgetWifi() {
        val request = Request(op_code = OpCode.FORGET_CONFIG)
        val response = waitForIndication(controlPointCharacteristic)
            .trigger(
                writeCharacteristic(
                    controlPointCharacteristic,
                    request.encode(),
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
            )
            .suspendForValidResponse<ByteArrayReadResponse>()

        verifyResponseSuccess(response.value)
    }

    private fun verifyResponseSuccess(response: ByteArray) {
        val status = Response.ADAPTER.decode(response).status
        if (status != Status.SUCCESS) {
            throw createResponseError(status)
        }
    }

    private fun createResponseError(status: Status?): Exception {
        val errorCode = when (status) {
            Status.INVALID_ARGUMENT -> ResponseError.INVALID_ARGUMENT
            Status.INVALID_PROTO -> ResponseError.INVALID_PROTO
            Status.INTERNAL_ERROR -> ResponseError.INTERNAL_ERROR
            Status.SUCCESS,
            null -> throw IllegalArgumentException("Error code should be not null and not success.")
        }
        return ResponseErrorException(errorCode)
    }
}
