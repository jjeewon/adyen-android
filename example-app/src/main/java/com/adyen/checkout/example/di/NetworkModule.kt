/*
 * Copyright (c) 2019 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by arman on 9/10/2019.
 */

package com.adyen.checkout.example.di

import com.adyen.checkout.example.BuildConfig
import com.adyen.checkout.example.data.api.CheckoutApiService
import com.adyen.checkout.example.data.api.RecurringApiService
import com.adyen.checkout.example.data.api.adapter.JSONObjectAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val BASE_URL = if (CheckoutApiService.isRealUrlAvailable()) {
        BuildConfig.MERCHANT_SERVER_URL
    } else {
        "http://myserver.com/my/endpoint/"
    }

    private val BASE_URL_RECURRING = if (RecurringApiService.isRealUrlAvailable()) {
        BuildConfig.MERCHANT_RECURRING_SERVER_URL
    } else {
        "http://myserver.com/my/endpoint/"
    }

    @Singleton
    @Provides
    internal fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()

        if (BuildConfig.DEBUG) {
            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BODY
            builder.addNetworkInterceptor(interceptor)
        }

        builder.addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader(BuildConfig.API_KEY_HEADER_NAME, BuildConfig.CHECKOUT_API_KEY)
                .build()

            chain.proceed(request)
        }

        return builder.build()
    }

    @Singleton
    @Provides
    internal fun provideConverterFactory(): Converter.Factory = MoshiConverterFactory.create(
        Moshi.Builder()
            .add(JSONObjectAdapter())
            .add(KotlinJsonAdapterFactory())
            .build()
    )

    @Singleton
    @Provides
    @Named("RetrofitCheckout")
    internal fun provideRetrofit(
        okHttpClient: OkHttpClient,
        converterFactory: Converter.Factory,
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(converterFactory)
            .build()

    @Singleton
    @Provides
    @Named("RetrofitRecurring")
    internal fun provideRetrofitRecurring(
        okHttpClient: OkHttpClient,
        converterFactory: Converter.Factory,
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL_RECURRING)
            .client(okHttpClient)
            .addConverterFactory(converterFactory)
            .build()

    @Provides
    internal fun provideApiService(@Named("RetrofitCheckout") retrofit: Retrofit): CheckoutApiService =
        retrofit.create(CheckoutApiService::class.java)

    @Provides
    internal fun provideRecurringApiService(@Named("RetrofitRecurring") retrofit: Retrofit): RecurringApiService =
        retrofit.create(RecurringApiService::class.java)
}
