package com.developer.pos

import android.app.Application
import com.developer.pos.device.payment.PaymentBootstrapManager
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@HiltAndroidApp
class PosApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        EntryPointAccessors.fromApplication(
            this,
            PosApplicationEntryPoint::class.java
        ).paymentBootstrapManager().warmUp()
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PosApplicationEntryPoint {
    fun paymentBootstrapManager(): PaymentBootstrapManager
}
