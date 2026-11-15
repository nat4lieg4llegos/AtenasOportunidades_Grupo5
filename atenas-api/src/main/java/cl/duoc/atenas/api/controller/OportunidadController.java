package cl.duoc.atenas.api.controller;

import cl.duoc.atenas.api.model.Oportunidad;
import cl.duoc.atenas.api.service.OportunidadService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints REST que consume la app con Retrofit.
 *   GET    /api/oportunidades          -> lista (opcional ?estado=...)
 *   GET    /api/oportunidades/{id}     -> una oportunidad
 *   POST   /api/oportunidades          -> crea (201)
 *   PUT    /api/oportunidades/{id}     -> actualiza
 *   DELETE /api/oportunidades/{id}     -> elimina (204)
 */
@RestController
@RequestMapping("/api/oportunidades")
public class OportunidadController {

    private final OportunidadService servicio;

    public OportunidadController(OportunidadService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<Oportunidad> listar(@RequestParam(name = "estado", required = false) String estado) {
        return servicio.listar(estado);
    }

    @GetMapping("/{id}")
    public Oportunidad obtener(@PathVariable("id") Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    public ResponseEntity<Oportunidad> crear(@Valid @RequestBody Oportunidad oportunidad) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(oportunidad));
    }

    @PutMapping("/{id}")
    public Oportunidad actualizar(@PathVariable("id") Long id, @Valid @RequestBody Oportunidad oportunidad) {
        return servicio.actualizar(id, oportunidad);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable("id") Long id) {
        servicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
