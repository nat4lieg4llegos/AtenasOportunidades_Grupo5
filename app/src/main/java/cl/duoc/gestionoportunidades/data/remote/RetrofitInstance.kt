package cl.duoc.gestionoportunidades.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Crea una sola instancia de Retrofit para toda la app. */
object RetrofitInstance {

    /**
     * Dirección del microservicio. 10.0.2.2 es "el computador" visto desde el emulador de Android
     * (localhost dentro del emulador sería el propio teléfono). En un teléfono físico se usa la IP del PC.
     */
    const val BASE_URL = "http://10.0.2.2:8080/"

    /** Muestra en Logcat cada petición y respuesta (filtrar por "okhttp"). */
    private val log = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

    /** Tiempos cortos: si el servidor está apagado, la app avisa rápido y sigue funcionando sin conexión. */
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
}
