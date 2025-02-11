/*
 * Copyright (c) 2023, Nordic Semiconductor
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

package ui.blinky

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import client.Client
import client.ClientCharacteristic
import client.ClientServices
import com.benasher44.uuid.uuidFrom
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import scanner.IoTDevice
import scanner.Scanner
import ui.scanner.advertisementUuids
import utils.toEntaggedUuid

private val BLINKY_SERVICE_UUID = uuidFrom("00001523-1212-efde-1523-785feabcd123")
private val BLINKY_BUTTON_CHARACTERISTIC_UUID = uuidFrom("00001524-1212-efde-1523-785feabcd123")
private val BLINKY_LED_CHARACTERISTIC_UUID = uuidFrom("00001525-1212-efde-1523-785feabcd123")

private val BOOTLOADER_SECURE_DFU_SERVICE = uuidFrom("0000fe59-0000-1000-8000-00805f9b34fb")
private val DFU_CHARACTERISTIC = uuidFrom("8ec90003-f315-4f60-9fb8-838830daea50")


class BlinkyViewModel(
    private val device: IoTDevice
) : ScreenModel, KoinComponent {

    private val client: Client by inject()

    private val _state = MutableStateFlow(BlinkyViewState())
    val state = _state.asStateFlow()

    private lateinit var ledCharacteristic: ClientCharacteristic

    init {
        screenModelScope.launch {
            client.connect(device, screenModelScope)

            val services = client.discoverServices()

            goToBootloader(services)

//            val service = services.findService(BLINKY_SERVICE_UUID)!!
//
//            ledCharacteristic = service.findCharacteristic(BLINKY_LED_CHARACTERISTIC_UUID)!!
//            val buttonCharacteristic = service.findCharacteristic(BLINKY_BUTTON_CHARACTERISTIC_UUID)!!
//
//            buttonCharacteristic.getNotifications()
//                .onEach {
//                    _state.value = _state.value.copy(
//                        isButtonPressed = BlinkyButtonParser.isButtonPressed(
//                            it
//                        )
//                    )
//                }
//                .launchIn(screenModelScope)
        }
    }

    private suspend fun goToBootloader(services: ClientServices) {
        Napier.w { "Go to bootloader" }
        val bootloaderService = services.findService(BOOTLOADER_SECURE_DFU_SERVICE)
        val dfu = bootloaderService?.findCharacteristic(DFU_CHARACTERISTIC)
        Napier.w { " bootlaoder service $bootloaderService , dfu $dfu" }
        dfu?.getNotifications()
            ?.onEach {
                Napier.w("onNotify -- $it")
            }?.launchIn(screenModelScope)

        dfu?.write(value = ByteArray(1).apply { this[0] = (1 and 0xFF).toByte() })

    }

    fun turnLed() {
        screenModelScope.launch {
            try {
                if (state.value.isLedOn) {
                    _state.value = _state.value.copy(isLedOn = false)
                    ledCharacteristic.write(byteArrayOf(0x00))
                } else {
                    _state.value = _state.value.copy(isLedOn = true)
                    ledCharacteristic.write(byteArrayOf(0x01))
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

    override fun onDispose() {
        screenModelScope.launch {
            client.disconnect()
        }
        super.onDispose()
    }
}
