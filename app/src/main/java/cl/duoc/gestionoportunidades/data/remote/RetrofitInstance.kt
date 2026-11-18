package cl.duoc.gestionoportunidades.data.remote

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    const val BASE_URL = "http://10.0.2.2:8080/"
    const val URL_FERIADOS = "https://date.nager.at/"

    private val log = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

    private val cliente = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(log)
        .build()

    val api: OportunidadApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OportunidadApiService::class.java)
    }

    val feriadosApi: FeriadosApiService by lazy {
        Retrofit.Builder()
            .baseUrl(URL_FERIADOS)
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FeriadosApiService::class.java)
    }
}
