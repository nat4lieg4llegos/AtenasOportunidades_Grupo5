package cl.duoc.atenas.api.repository;

import cl.duoc.atenas.api.model.Oportunidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OportunidadRepository extends JpaRepository<Oportunidad, Long> {
    List<Oportunidad> findAllByOrderByFechaEventoAscIdDesc();

    List<Oportunidad> findByEstadoOrderByFechaEventoAscIdDesc(String estado);
}
