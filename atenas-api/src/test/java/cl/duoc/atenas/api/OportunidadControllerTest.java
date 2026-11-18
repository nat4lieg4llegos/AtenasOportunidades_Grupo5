package cl.duoc.atenas.api;

import cl.duoc.atenas.api.repository.OportunidadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:pruebas;DB_CLOSE_DELAY=-1")
class OportunidadControllerTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private OportunidadRepository repositorio;

    @BeforeEach
    void limpiar() {
        repositorio.deleteAll();
    }

    private String json(String nombre, String deteccion, String evento) {
        return """
            {
              "nombre": "%s",
              "organizacion": "Empresa Horizonte Ficticia",
              "tipo": "Feria empresarial",
              "fechaDeteccion": "%s",
              "fechaEvento": "%s",
              "comuna": "Puente Alto",
              "region": "Metropolitana",
              "publicoEsperado": 300,
              "prioridad": "Alta",
              "potencial": "Alto",
              "estado": "Evento identificado",
              "responsable": "Ejecutiva Demo",
              "contactoCorreo": "contacto.demo@ejemplo.cl",
              "costoReferencial": 150000
            }
            """.formatted(nombre, deteccion, evento);
    }

    private long crear(String nombre) throws Exception {
        MvcResult r = mvc.perform(post("/api/oportunidades").contentType(MediaType.APPLICATION_JSON)
                        .content(json(nombre, "2026-11-01", "2026-12-01")))
                .andExpect(status().isCreated())
                .andReturn();
        String cuerpo = r.getResponse().getContentAsString();
        return Long.parseLong(cuerpo.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }

    @Test
    void crearDevuelve201ConId() throws Exception {
        mvc.perform(post("/api/oportunidades").contentType(MediaType.APPLICATION_JSON)
                        .content(json("Feria de beneficios", "2026-11-01", "2026-12-01")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre").value("Feria de beneficios"))
                .andExpect(jsonPath("$.fechaEvento").value("2026-12-01"));
    }

    @Test
    void listarDevuelveLasOportunidadesCreadas() throws Exception {
        crear("Feria uno");
        crear("Feria dos");
        mvc.perform(get("/api/oportunidades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void nombreVacioDevuelve400ConErrorPorCampo() throws Exception {
        mvc.perform(post("/api/oportunidades").contentType(MediaType.APPLICATION_JSON)
                        .content(json("", "2026-11-01", "2026-12-01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists());
    }

    @Test
    void eventoAnteriorADeteccionDevuelve400() throws Exception {
        mvc.perform(post("/api/oportunidades").contentType(MediaType.APPLICATION_JSON)
                        .content(json("Feria al revés", "2026-12-01", "2026-11-01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.fechaEvento").value("El evento no puede ser anterior a su detección"));
    }

    @Test
    void actualizarCambiaLosDatos() throws Exception {
        long id = crear("Feria original");
        mvc.perform(put("/api/oportunidades/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(json("Feria actualizada", "2026-11-01", "2026-12-01")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.nombre").value("Feria actualizada"));
    }

    @Test
    void eliminarYLuegoBuscarDevuelve404() throws Exception {
        long id = crear("Feria a eliminar");
        mvc.perform(delete("/api/oportunidades/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/oportunidades/" + id)).andExpect(status().isNotFound());
    }
}
