package com.assiten.accessibility.data.remote

import com.assiten.accessibility.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * ApiClient: Configura el cliente HTTP para comunicarse con el backend en cPanel
 * Prioridades:
 * - Timeout configurables para no bloquear el AccessibilityService
 * - Interceptor de logging en DEBUG
 * - Connection pooling para eficiencia
 */
object ApiClient {
    private const val REQUEST_TIMEOUT_SECONDS = 10L
    private const val CONNECT_TIMEOUT_SECONDS = 8L

    private val okHttpClient: OkHttpClient by lazy {
        val httpClientBuilder = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .connectionPool(okhttp3.ConnectionPool(5, 5, TimeUnit.MINUTES))

        // Añade logging interceptor solo en DEBUG
        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            httpClientBuilder.addInterceptor(loggingInterceptor)
        }

        httpClientBuilder.build()
    }

    val retrofitService: AuthorizationApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthorizationApiService::class.java)
    }
}
