package cl.duoc.gestionoportunidades.data.remote

/**
 * Forma del JSON que envía y recibe el microservicio (/api/oportunidades).
 * Todos los campos son opcionales (?) porque Gson los deja en null si el servidor no los manda;
 * así la app nunca se cae por un dato faltante. Las fechas viajan como texto AAAA-MM-DD.
 */
data class OportunidadDto(
    val id: Long? = null,
    val nombre: String? = null,
    val organizacion: String? = null,
    val tipo: String? = null,
    val descripcion: String? = null,
    val fuente: String? = null,
    val fechaDeteccion: String? = null,
    val fechaEvento: String? = null,
    val horario: String? = null,
    val comuna: String? = null,
    val region: String? = null,
    val publicoObjetivo: String? = null,
    val publicoEsperado: Int? = null,
    val prioridad: String? = null,
    val potencial: String? = null,
    val estado: String? = null,
    val responsable: String? = null,
    val contactoNombre: String? = null,
    val contactoCargo: String? = null,
    val contactoArea: String? = null,
    val contactoCorreo: String? = null,
    val contactoTelefono: String? = null,
    val propuestaEnviada: Boolean? = null,
    val condiciones: String? = null,
    val costoReferencial: Long? = null,
    val observaciones: String? = null,
    val proximoSeguimiento: String? = null
)
