package cl.duoc.gestionoportunidades.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

interface FeriadosApiService {

    @GET("api/v3/PublicHolidays/{anio}/{pais}")
    suspend fun feriados(@Path("anio") anio: Int, @Path("pais") pais: String): List<FeriadoDto>
}
