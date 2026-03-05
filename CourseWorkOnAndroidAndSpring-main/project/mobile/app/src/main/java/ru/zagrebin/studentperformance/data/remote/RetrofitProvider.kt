package ru.ryabov.studentperformance.data.remote

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.ryabov.studentperformance.BuildConfig
import java.util.concurrent.TimeUnit

/**
 * Создание Retrofit и API. Base URL из BuildConfig.BASE_URL.
 * Все create/update/delete идут на сервер и сохраняются в БД на сервере.
 * Gson по умолчанию использует UTF-8 для JSON (русские ФИО передаются корректно).
 */
object RetrofitProvider {

    fun createApi(sessionManager: SessionManager, baseUrl: String = BuildConfig.BASE_URL): StudentPerformanceApi {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val gson = GsonBuilder().setLenient().create()
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionManager))
            .addInterceptor(logging)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        return retrofit.create(StudentPerformanceApi::class.java)
    }
}
