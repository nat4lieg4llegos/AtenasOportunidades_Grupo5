package cl.duoc.gestionoportunidades.data.local

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import cl.duoc.gestionoportunidades.data.model.Oportunidad

/** Acceso a la tabla de oportunidades (consultas SQL parametrizadas para evitar inyección). */
class OportunidadDao(private val helper: AtenasDbHelper) {

    fun listar(): List<Oportunidad> =
        helper.readableDatabase.query(
            Tablas.OPORTUNIDADES, null, null, null, null, null, "fecha_evento ASC, id DESC"
        ).use { c -> buildList { while (c.moveToNext()) add(c.aOportunidad()) } }

    fun obtener(id: Long): Oportunidad? =
        helper.readableDatabase.query(
            Tablas.OPORTUNIDADES, null, "id = ?", arrayOf(id.toString()), null, null, null
        ).use { c -> if (c.moveToFirst()) c.aOportunidad() else null }

    fun insertar(o: Oportunidad): Long = helper.writableDatabase.insertOrThrow(Tablas.OPORTUNIDADES, null, o.aValores())

    fun actualizar(o: Oportunidad): Int =
        helper.writableDatabase.update(Tablas.OPORTUNIDADES, o.aValores(), "id = ?", arrayOf(o.id.toString()))

    fun eliminar(id: Long): Int = helper.writableDatabase.delete(Tablas.OPORTUNIDADES, "id = ?", arrayOf(id.toString()))

    // ---- Sincronización con el microservicio (EP3) ----

    fun obtenerPorRemoteId(remoteId: Long): Oportunidad? =
        helper.readableDatabase.query(
            Tablas.OPORTUNIDADES, null, "remote_id = ?", arrayOf(remoteId.toString()), null, null, null
        ).use { c -> if (c.moveToFirst()) c.aOportunidad() else null }

    /** Oportunidades con cambios locales que todavía no llegan al servidor. */
    fun listarPendientes(): List<Oportunidad> =
        helper.readableDatabase.query(
            Tablas.OPORTUNIDADES, null, "sincronizado = 0", null, null, null, "id ASC"
        ).use { c -> buildList { while (c.moveToNext()) add(c.aOportunidad()) } }

    /** Guarda el id que asignó el servidor e indica si la fila quedó igual que en el servidor. */
    fun marcarSincronizada(id: Long, remoteId: Long, sincronizado: Boolean): Int =
        helper.writableDatabase.update(
            Tablas.OPORTUNIDADES,
            ContentValues().apply { put("remote_id", remoteId); put("sincronizado", if (sincronizado) 1 else 0) },
            "id = ?", arrayOf(id.toString())
        )

    /** Vuelve a dejar la fila como "nunca subida" (por ejemplo, si el servidor ya no la tiene). */
    fun desvincular(id: Long): Int =
        helper.writableDatabase.update(
            Tablas.OPORTUNIDADES,
            ContentValues().apply { putNull("remote_id"); put("sincronizado", 0) },
            "id = ?", arrayOf(id.toString())
        )

    fun agregarEliminacionPendiente(remoteId: Long) {
        helper.writableDatabase.insertWithOnConflict(
            Tablas.ELIMINACIONES_PENDIENTES, null,
            ContentValues().apply { put("remote_id", remoteId) }, SQLiteDatabase.CONFLICT_IGNORE
        )
    }

    fun listarEliminacionesPendientes(): List<Long> =
        helper.readableDatabase.query(
            Tablas.ELIMINACIONES_PENDIENTES, arrayOf("remote_id"), null, null, null, null, null
        ).use { c -> buildList { while (c.moveToNext()) add(c.getLong(0)) } }

    fun quitarEliminacionPendiente(remoteId: Long) {
        helper.writableDatabase.delete(Tablas.ELIMINACIONES_PENDIENTES, "remote_id = ?", arrayOf(remoteId.toString()))
    }

    private fun Oportunidad.aValores() = ContentValues().apply {
        put("nombre", nombre); put("organizacion", organizacion); put("tipo", tipo)
        put("descripcion", descripcion); put("fuente", fuente)
        put("fecha_deteccion", fechaDeteccion); put("fecha_evento", fechaEvento); put("horario", horario)
        put("comuna", comuna); put("region", region)
        put("publico_objetivo", publicoObjetivo); put("publico_esperado", publicoEsperado)
        put("prioridad", prioridad); put("potencial", potencial); put("estado", estado); put("responsable", responsable)
        put("contacto_nombre", contactoNombre); put("contacto_cargo", contactoCargo); put("contacto_area", contactoArea)
        put("contacto_correo", contactoCorreo); put("contacto_telefono", contactoTelefono)
        put("propuesta_enviada", if (propuestaEnviada) 1 else 0)
        put("condiciones", condiciones); put("costo_referencial", costoReferencial)
        put("observaciones", observaciones); put("foto_ruta", fotoRuta)
        put("proximo_seguimiento", proximoSeguimiento); put("actualizado", actualizado)
        put("remote_id", remoteId); put("sincronizado", if (sincronizado) 1 else 0)
    }

    private fun Cursor.aOportunidad() = Oportunidad(
        id = getLong(getColumnIndexOrThrow("id")),
        nombre = texto("nombre"),
        organizacion = texto("organizacion"),
        tipo = texto("tipo"),
        descripcion = texto("descripcion"),
        fuente = texto("fuente"),
        fechaDeteccion = texto("fecha_deteccion"),
        fechaEvento = texto("fecha_evento"),
        horario = texto("horario"),
        comuna = texto("comuna"),
        region = texto("region"),
        publicoObjetivo = texto("publico_objetivo"),
        publicoEsperado = getInt(getColumnIndexOrThrow("publico_esperado")),
        prioridad = texto("prioridad"),
        potencial = texto("potencial"),
        estado = texto("estado"),
        responsable = texto("responsable"),
        contactoNombre = texto("contacto_nombre"),
        contactoCargo = texto("contacto_cargo"),
        contactoArea = texto("contacto_area"),
        contactoCorreo = texto("contacto_correo"),
        contactoTelefono = texto("contacto_telefono"),
        propuestaEnviada = getInt(getColumnIndexOrThrow("propuesta_enviada")) == 1,
        condiciones = texto("condiciones"),
        costoReferencial = getLong(getColumnIndexOrThrow("costo_referencial")),
        observaciones = texto("observaciones"),
        fotoRuta = textoONulo("foto_ruta"),
        proximoSeguimiento = textoONulo("proximo_seguimiento"),
        actualizado = texto("actualizado"),
        remoteId = getColumnIndexOrThrow("remote_id").let { i -> if (isNull(i)) null else getLong(i) },
        sincronizado = getInt(getColumnIndexOrThrow("sincronizado")) == 1
    )
}

internal fun Cursor.texto(columna: String): String = getString(getColumnIndexOrThrow(columna)).orEmpty()

internal fun Cursor.textoONulo(columna: String): String? {
    val i = getColumnIndexOrThrow(columna)
    return if (isNull(i)) null else getString(i)
}
