package cl.duoc.gestionoportunidades.util

import cl.duoc.gestionoportunidades.data.model.Fechas
import cl.duoc.gestionoportunidades.data.model.Oportunidad
import cl.duoc.gestionoportunidades.data.model.Usuario
import java.time.LocalDate

val USUARIO_DEMO = Usuario("ejecutivo", "Ejecutiva Demo", "Ejecutivo comercial", "Santiago Sur")

fun oportunidadDePrueba(
    id: Long = 1,
    nombre: String = "Feria de beneficios",
    estado: String = "Evento identificado",
    prioridad: String = "Alta",
    tipo: String = "Feria empresarial",
    region: String = "Metropolitana",
    comuna: String = "Puente Alto",
    fechaEvento: LocalDate = LocalDate.now().plusDays(10),
    proximoSeguimiento: LocalDate? = null,
    remoteId: Long? = null,
    sincronizado: Boolean = false,
    fotoRuta: String? = null,
    propuestaEnviada: Boolean = false
) = Oportunidad(
    id = id,
    nombre = nombre,
    organizacion = "Empresa Horizonte Ficticia",
    tipo = tipo,
    descripcion = "",
    fuente = "Sitio web",
    fechaDeteccion = Fechas.aIso(LocalDate.now().minusDays(5)),
    fechaEvento = Fechas.aIso(fechaEvento),
    horario = "09:00 - 17:00",
    comuna = comuna,
    region = region,
    publicoObjetivo = "Trabajadores",
    publicoEsperado = 250,
    prioridad = prioridad,
    potencial = "Alto",
    estado = estado,
    responsable = "Ejecutiva Demo",
    contactoNombre = "",
    contactoCargo = "",
    contactoArea = "",
    contactoCorreo = "",
    contactoTelefono = "",
    propuestaEnviada = propuestaEnviada,
    condiciones = "",
    costoReferencial = 150000,
    observaciones = "",
    fotoRuta = fotoRuta,
    proximoSeguimiento = proximoSeguimiento?.let { Fechas.aIso(it) },
    remoteId = remoteId,
    sincronizado = sincronizado
)
