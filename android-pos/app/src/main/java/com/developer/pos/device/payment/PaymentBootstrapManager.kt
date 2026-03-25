package com.developer.pos.device.payment

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Singleton
class PaymentBootstrapManager @Inject constructor(
    private val sunmiPayHardwareBootstrapService: SunmiPayHardwareBootstrapService,
    private val dcsPaymentService: DcsPaymentService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var warmedUp = false

    fun warmUp() {
        if (warmedUp) return
        warmedUp = true
        scope.launch {
            runCatching {
                sunmiPayHardwareBootstrapService.connect()
                dcsPaymentService.connect(PaymentMethods.CARD_TERMINAL)
            }.onFailure {
                warmedUp = false
            }
        }
    }
}
