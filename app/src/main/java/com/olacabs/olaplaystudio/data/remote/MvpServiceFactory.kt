package com.olacabs.olaplaystudio.data.remote

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.olacabs.olaplaystudio.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber

/**
 * Provide "make" methods to create instances of [MvpService]
 * and its related dependencies, such as OkHttpClient, Gson, etc.
 */
object MvpServiceFactory {

    fun makeStarterService(): MvpService {
        return makeMvpStarterService(makeGson())
    }

    private fun makeMvpStarterService(gson: Gson): MvpService {
        val retrofit = Retrofit.Builder()
                .baseUrl(BuildConfig.OLA_MEDIA_BASE_URL)
                .client(makeOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
        return retrofit.create(MvpService::class.java)
    }

    private fun makeOkHttpClient(): OkHttpClient {

        val httpClientBuilder = OkHttpClient.Builder()

        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor { message -> Timber.d(message) }
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            httpClientBuilder.addInterceptor(loggingInterceptor)
        }

        return httpClientBuilder.build()
    }

    private fun makeGson(): Gson {
        return GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create()
    }
}
