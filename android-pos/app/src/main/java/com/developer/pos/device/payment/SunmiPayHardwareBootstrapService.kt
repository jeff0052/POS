package com.developer.pos.device.payment

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@Singleton
class SunmiPayHardwareBootstrapService @Inject constructor(
    @ApplicationContext
    private val appContext: Context
) {
    @Volatile
    private var connected = false

    @Volatile
    private var statusMessage = "Sunmi PAY_HARDWARE service not checked"

    private var serviceConnection: ServiceConnection? = null

    suspend fun connect(): PaymentConnectionStatus = withContext(Dispatchers.IO) {
        if (connected) {
            return@withContext PaymentConnectionStatus(
                available = true,
                connected = true,
                providerName = "SUNMI_PAY_HARDWARE",
                message = statusMessage
            )
        }

        val result = withTimeoutOrNull(5_000L) {
            suspendCancellableCoroutine<PaymentConnectionStatus> { continuation ->
                val intent = Intent(SERVICE_ACTION).apply {
                    `package` = SERVICE_PACKAGE
                    component = ComponentName(SERVICE_PACKAGE, SERVICE_CLASS)
                }

                val connection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                        connected = true
                        statusMessage = "SUNMI PAY_HARDWARE service connected"
                        if (continuation.context[kotlinx.coroutines.Job]?.isActive != false) {
                            continuation.resume(
                                PaymentConnectionStatus(
                                    available = true,
                                    connected = true,
                                    providerName = "SUNMI_PAY_HARDWARE",
                                    message = statusMessage
                                )
                            )
                        }
                    }

                    override fun onServiceDisconnected(name: ComponentName?) {
                        connected = false
                        statusMessage = "SUNMI PAY_HARDWARE service disconnected"
                    }

                    override fun onBindingDied(name: ComponentName?) {
                        connected = false
                        statusMessage = "SUNMI PAY_HARDWARE binding died"
                    }

                    override fun onNullBinding(name: ComponentName?) {
                        connected = false
                        statusMessage = "SUNMI PAY_HARDWARE returned null binding"
                        if (continuation.context[kotlinx.coroutines.Job]?.isActive != false) {
                            continuation.resume(
                                PaymentConnectionStatus(
                                    available = false,
                                    connected = false,
                                    providerName = "SUNMI_PAY_HARDWARE",
                                    message = statusMessage
                                )
                            )
                        }
                    }
                }

                serviceConnection = connection
                val bound = runCatching {
                    appContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)
                }.getOrDefault(false)

                if (!bound && continuation.context[kotlinx.coroutines.Job]?.isActive != false) {
                    connected = false
                    statusMessage = "Unable to bind SUNMI PAY_HARDWARE service"
                    continuation.resume(
                        PaymentConnectionStatus(
                            available = false,
                            connected = false,
                            providerName = "SUNMI_PAY_HARDWARE",
                            message = statusMessage
                        )
                    )
                }

                continuation.invokeOnCancellation {
                    runCatching {
                        serviceConnection?.let { appContext.unbindService(it) }
                    }
                }
            }
        }

        result ?: PaymentConnectionStatus(
            available = false,
            connected = false,
            providerName = "SUNMI_PAY_HARDWARE",
            message = "SUNMI PAY_HARDWARE connection timed out"
        )
    }

    fun currentStatus(): PaymentConnectionStatus {
        return PaymentConnectionStatus(
            available = connected,
            connected = connected,
            providerName = "SUNMI_PAY_HARDWARE",
            message = statusMessage
        )
    }

    private companion object {
        const val SERVICE_ACTION = "sunmi.intent.action.PAY_HARDWARE"
        const val SERVICE_PACKAGE = "com.sunmi.pay.hardware_v3"
        const val SERVICE_CLASS = "com.sunmi.pay.hardware.PayHardwareService"
    }
}
