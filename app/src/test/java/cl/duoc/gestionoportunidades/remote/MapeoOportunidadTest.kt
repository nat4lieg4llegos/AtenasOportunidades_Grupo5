package cl.duoc.gestionoportunidades.remote

import cl.duoc.gestionoportunidades.data.model.Oportunidad
import cl.duoc.gestionoportunidades.data.remote.OportunidadDto
import cl.duoc.gestionoportunidades.data.remote.aDto
import cl.duoc.gestionoportunidades.data.remote.aOportunidad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas de la conversión entre el modelo de la app y el JSON del microservicio. */
class MapeoOportunidadTest {

    private fun oportunidad(id: Long = 7, remoteId: Long? = null) = Oportunidad(
        id = id, nombre = "Feria de beneficios", organizacion = "Empresa Horizonte Ficticia",
        tipo = "Feria empresarial", descripcion = "", fuente = "Sitio web",
        fechaDeteccion = "2026-11-01", fechaEvento = "2026-12-01", horario = "09:00 - 17:00",
        comuna = "Puente Alto", region = "Metropolitana", publicoObjetivo = "", publicoEsperado = 300,
        prioridad = "Alta", potencial = "Alto", estado = "Evento identificado", responsable = "Ejecutiva Demo",
        contactoNombre = "", contactoCargo = "", contactoArea = "", contactoCorreo = "", contactoTelefono = "",
        propuestaEnviada = false, condiciones = "", costoReferencial = 150000, observaciones = "",
        fotoRuta = "/data/fotos/evento.jpg", remoteId = remoteId
    )

    @Test
    fun alEnviarSeUsaElIdDelServidorYNoElLocal() {
        assertNull(oportunidad(id = 7, remoteId = null).aDto().id)
        assertEquals(15L, oportunidad(id = 7, remoteId = 15).aDto().id)
    }

    @Test
    fun alEnviarSeMandanLosDatosDelFormulario() {
        val dto = oportunidad().aDto()
        assertEquals("Feria de beneficios", dto.nombre)
        assertEquals("2026-12-01", dto.fechaEvento)
        assertEquals(300, dto.publicoEsperado)
        assertEquals(150000L, dto.costoReferencial)
    }

    @Test
    fun alRecibirSeGuardaElIdDelServidorYQuedaSincronizada() {
        val o = OportunidadDto(id = 15, nombre = "Feria", fechaEvento = "2026-12-01", publicoEsperado = 50)
            .aOportunidad(idLocal = 3, fotoRuta = "/data/fotos/evento.jpg")
        assertEquals(3L, o.id)
        assertEquals(15L, o.remoteId)
        assertTrue(o.sincronizado)
        assertEquals("/data/fotos/evento.jpg", o.fotoRuta) // la foto local se conserva
    }

    @Test
    fun siElServidorNoMandaUnCampoSeUsaUnValorPorDefecto() {
        val o = OportunidadDto(id = 1).aOportunidad()
        assertEquals("", o.nombre)
        assertEquals(0, o.publicoEsperado)
        assertEquals(0L, o.costoReferencial)
        assertFalse(o.propuestaEnviada)
    }
}
