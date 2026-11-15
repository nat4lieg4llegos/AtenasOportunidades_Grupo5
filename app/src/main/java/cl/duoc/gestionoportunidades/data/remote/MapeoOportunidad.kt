package cl.duoc.gestionoportunidades.data.remote

import cl.duoc.gestionoportunidades.data.model.Oportunidad

/**
 * Conversión entre el modelo de la app y el JSON del servidor.
 * - El id local (SQLite) y el id del servidor son distintos: el del servidor se guarda en remoteId.
 * - La foto NO viaja al servidor: es un archivo privado del teléfono.
 */
fun Oportunidad.aDto(): OportunidadDto = OportunidadDto(
    id = remoteId,
    nombre = nombre, organizacion = organizacion, tipo = tipo, descripcion = descripcion, fuente = fuente,
    fechaDeteccion = fechaDeteccion, fechaEvento = fechaEvento, horario = horario,
    comuna = comuna, region = region, publicoObjetivo = publicoObjetivo, publicoEsperado = publicoEsperado,
    prioridad = prioridad, potencial = potencial, estado = estado, responsable = responsable,
    contactoNombre = contactoNombre, contactoCargo = contactoCargo, contactoArea = contactoArea,
    contactoCorreo = contactoCorreo, contactoTelefono = contactoTelefono, propuestaEnviada = propuestaEnviada,
    condiciones = condiciones, costoReferencial = costoReferencial, observaciones = observaciones,
    proximoSeguimiento = proximoSeguimiento
)

/**
 * Crea la versión local de una oportunidad que llegó del servidor.
 * @param idLocal id de la fila en SQLite (0 = fila nueva).
 * @param fotoRuta se conserva la foto local, porque el servidor no la conoce.
 */
fun OportunidadDto.aOportunidad(idLocal: Long = 0, fotoRuta: String? = null, actualizado: String = ""): Oportunidad = Oportunidad(
    id = idLocal,
    nombre = nombre.orEmpty(), organizacion = organizacion.orEmpty(), tipo = tipo.orEmpty(),
    descripcion = descripcion.orEmpty(), fuente = fuente.orEmpty(),
    fechaDeteccion = fechaDeteccion.orEmpty(), fechaEvento = fechaEvento.orEmpty(), horario = horario.orEmpty(),
    comuna = comuna.orEmpty(), region = region.orEmpty(), publicoObjetivo = publicoObjetivo.orEmpty(),
    publicoEsperado = publicoEsperado ?: 0,
    prioridad = prioridad.orEmpty(), potencial = potencial.orEmpty(), estado = estado.orEmpty(),
    responsable = responsable.orEmpty(),
    contactoNombre = contactoNombre.orEmpty(), contactoCargo = contactoCargo.orEmpty(),
    contactoArea = contactoArea.orEmpty(), contactoCorreo = contactoCorreo.orEmpty(),
    contactoTelefono = contactoTelefono.orEmpty(), propuestaEnviada = propuestaEnviada ?: false,
    condiciones = condiciones.orEmpty(), costoReferencial = costoReferencial ?: 0L,
    observaciones = observaciones.orEmpty(), fotoRuta = fotoRuta, proximoSeguimiento = proximoSeguimiento,
    actualizado = actualizado, remoteId = id, sincronizado = true
)
