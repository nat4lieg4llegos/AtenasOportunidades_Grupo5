package cl.duoc.atenas.api.repository;

import cl.duoc.atenas.api.model.Oportunidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Acceso a la tabla "oportunidades". JpaRepository ya trae save, findById, findAll y deleteById;
 * Spring genera el SQL de los métodos a partir de su nombre.
 */
public interface OportunidadRepository extends JpaRepository<Oportunidad, Long> {

    List<Oportunidad> findAllByOrderByFechaEventoAscIdDesc();

    List<Oportunidad> findByEstadoOrderByFechaEventoAscIdDesc(String estado);
}
