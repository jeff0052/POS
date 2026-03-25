package com.developer.pos.di

import android.content.Context
import androidx.room.Room
import com.developer.pos.data.local.PosDatabase
import com.developer.pos.data.local.dao.ProductDao
import com.developer.pos.data.remote.ProductApi
import com.developer.pos.data.remote.PosOrderApi
import com.developer.pos.data.repository.PosOrderRepository
import com.developer.pos.data.repository.ProductRepository
import com.developer.pos.data.repository.impl.PosOrderRepositoryImpl
import com.developer.pos.data.repository.impl.ProductRepositoryImpl
import com.developer.pos.device.payment.CashPaymentService
import com.developer.pos.device.payment.DcsPaymentService
import com.developer.pos.device.payment.PaymentService
import com.developer.pos.device.payment.UnifiedPaymentService
import com.developer.pos.device.payment.VibeCashPaymentService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PosDatabase {
        return Room.databaseBuilder(
            context,
            PosDatabase::class.java,
            "pos.db"
        ).build()
    }

    @Provides
    fun provideProductDao(database: PosDatabase): ProductDao = database.productDao()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8094/api/v2/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideProductApi(retrofit: Retrofit): ProductApi {
        return retrofit.create(ProductApi::class.java)
    }

    @Provides
    @Singleton
    fun providePosOrderApi(retrofit: Retrofit): PosOrderApi {
        return retrofit.create(PosOrderApi::class.java)
    }

    @Provides
    @Singleton
    fun provideProductRepository(productDao: ProductDao, productApi: ProductApi): ProductRepository {
        return ProductRepositoryImpl(productDao, productApi)
    }

    @Provides
    @Singleton
    fun providePosOrderRepository(posOrderApi: PosOrderApi): PosOrderRepository {
        return PosOrderRepositoryImpl(posOrderApi)
    }

    @Provides
    @Singleton
    fun providePaymentService(
        @ApplicationContext context: Context,
        posOrderApi: PosOrderApi
    ): PaymentService {
        val dcs = DcsPaymentService(context)
        val vibecash = VibeCashPaymentService(posOrderApi)
        val cash = CashPaymentService()
        return UnifiedPaymentService(dcs, vibecash, cash)
    }
}
