package cl.duoc.atenas.api.service;

import cl.duoc.atenas.api.exception.RecursoNoEncontradoException;
import cl.duoc.atenas.api.exception.ReglaNegocioException;
import cl.duoc.atenas.api.model.Oportunidad;
import cl.duoc.atenas.api.repository.OportunidadRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/** Reglas de negocio del servidor. El controlador solo recibe y responde; aquí se decide. */
@Service
public class OportunidadService {

    private final OportunidadRepository repositorio;

    public OportunidadService(OportunidadRepository repositorio) {
        this.repositorio = repositorio;
    }

    public List<Oportunidad> listar(String estado) {
        if (estado == null || estado.isBlank()) {
            return repositorio.findAllByOrderByFechaEventoAscIdDesc();
        }
        return repositorio.findByEstadoOrderByFechaEventoAscIdDesc(estado);
    }

    public Oportunidad obtener(Long id) {
        return repositorio.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
    }

    public Oportunidad crear(Oportunidad oportunidad) {
        oportunidad.setId(null); // el id lo asigna la base de datos
        validarReglas(oportunidad);
        return repositorio.save(oportunidad);
    }

    public Oportunidad actualizar(Long id, Oportunidad datos) {
        if (!repositorio.existsById(id)) {
            throw new RecursoNoEncontradoException(id);
        }
        datos.setId(id);
        validarReglas(datos);
        return repositorio.save(datos);
    }

    public void eliminar(Long id) {
        if (!repositorio.existsById(id)) {
            throw new RecursoNoEncontradoException(id);
        }
        repositorio.deleteById(id);
    }

    /** Misma regla que valida la app: un evento no puede ocurrir antes de haber sido detectado. */
    private void validarReglas(Oportunidad o) {
        if (o.getFechaEvento().isBefore(o.getFechaDeteccion())) {
            throw new ReglaNegocioException("fechaEvento", "El evento no puede ser anterior a su detección");
        }
        if (o.getCostoReferencial() == null) {
            o.setCostoReferencial(0L);
        }
    }
}
