package cl.duoc.gestionoportunidades.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Endpoints del microservicio atenas-api. Retrofit genera el código HTTP a partir de esta interfaz
 * y Gson convierte el JSON en OportunidadDto. Son "suspend": se llaman desde corrutinas, sin bloquear la pantalla.
 */
interface OportunidadApiService {

    @GET("api/oportunidades")
    suspend fun listar(): List<OportunidadDto>

    @GET("api/oportunidades/{id}")
    suspend fun obtener(@Path("id") id: Long): OportunidadDto

    @POST("api/oportunidades")
    suspend fun crear(@Body oportunidad: OportunidadDto): OportunidadDto

    @PUT("api/oportunidades/{id}")
    suspend fun actualizar(@Path("id") id: Long, @Body oportunidad: OportunidadDto): OportunidadDto

    /** El servidor responde 204 sin cuerpo; Response permite revisar el código (204 o 404). */
    @DELETE("api/oportunidades/{id}")
    suspend fun eliminar(@Path("id") id: Long): Response<Unit>
}
