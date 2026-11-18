package cl.duoc.atenas.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "oportunidades")
public class Oportunidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, max = 80, message = "El nombre debe tener entre 3 y 80 caracteres")
    private String nombre;

    @NotBlank(message = "La organización es obligatoria")
    @Size(min = 3, max = 120, message = "La organización debe tener entre 3 y 120 caracteres")
    private String organizacion;

    @NotBlank(message = "El tipo es obligatorio")
    private String tipo;

    @Size(max = 300, message = "La descripción admite máximo 300 caracteres")
    @Column(length = 300)
    private String descripcion;

    private String fuente;

    @NotNull(message = "La fecha de detección es obligatoria")
    private LocalDate fechaDeteccion;

    @NotNull(message = "La fecha del evento es obligatoria")
    private LocalDate fechaEvento;

    private String horario;

    @NotBlank(message = "La comuna es obligatoria")
    private String comuna;

    @NotBlank(message = "La región es obligatoria")
    private String region;

    private String publicoObjetivo;

    @NotNull(message = "El público esperado es obligatorio")
    @Positive(message = "El público esperado debe ser mayor que 0")
    private Integer publicoEsperado;

    @NotBlank(message = "La prioridad es obligatoria")
    private String prioridad;

    @NotBlank(message = "El potencial es obligatorio")
    private String potencial;

    @NotBlank(message = "El estado es obligatorio")
    private String estado;

    @NotBlank(message = "El responsable es obligatorio")
    private String responsable;

    private String contactoNombre;

    private String contactoCargo;

    private String contactoArea;

    @Email(message = "El correo del contacto no es válido")
    private String contactoCorreo;

    private String contactoTelefono;

    private boolean propuestaEnviada;

    private String condiciones;

    @PositiveOrZero(message = "El costo no puede ser negativo")
    private Long costoReferencial;

    @Size(max = 500, message = "Las observaciones admiten máximo 500 caracteres")
    @Column(length = 500)
    private String observaciones;

    private LocalDate proximoSeguimiento;

    private LocalDateTime actualizadoEn;

    @PrePersist
    @PreUpdate
    void marcarActualizacion() {
        actualizadoEn = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getOrganizacion() { return organizacion; }
    public void setOrganizacion(String organizacion) { this.organizacion = organizacion; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getFuente() { return fuente; }
    public void setFuente(String fuente) { this.fuente = fuente; }

    public LocalDate getFechaDeteccion() { return fechaDeteccion; }
    public void setFechaDeteccion(LocalDate fechaDeteccion) { this.fechaDeteccion = fechaDeteccion; }

    public LocalDate getFechaEvento() { return fechaEvento; }
    public void setFechaEvento(LocalDate fechaEvento) { this.fechaEvento = fechaEvento; }

    public String getHorario() { return horario; }
    public void setHorario(String horario) { this.horario = horario; }

    public String getComuna() { return comuna; }
    public void setComuna(String comuna) { this.comuna = comuna; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getPublicoObjetivo() { return publicoObjetivo; }
    public void setPublicoObjetivo(String publicoObjetivo) { this.publicoObjetivo = publicoObjetivo; }

    public Integer getPublicoEsperado() { return publicoEsperado; }
    public void setPublicoEsperado(Integer publicoEsperado) { this.publicoEsperado = publicoEsperado; }

    public String getPrioridad() { return prioridad; }
    public void setPrioridad(String prioridad) { this.prioridad = prioridad; }

    public String getPotencial() { return potencial; }
    public void setPotencial(String potencial) { this.potencial = potencial; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getResponsable() { return responsable; }
    public void setResponsable(String responsable) { this.responsable = responsable; }

    public String getContactoNombre() { return contactoNombre; }
    public void setContactoNombre(String contactoNombre) { this.contactoNombre = contactoNombre; }

    public String getContactoCargo() { return contactoCargo; }
    public void setContactoCargo(String contactoCargo) { this.contactoCargo = contactoCargo; }

    public String getContactoArea() { return contactoArea; }
    public void setContactoArea(String contactoArea) { this.contactoArea = contactoArea; }

    public String getContactoCorreo() { return contactoCorreo; }
    public void setContactoCorreo(String contactoCorreo) { this.contactoCorreo = contactoCorreo; }

    public String getContactoTelefono() { return contactoTelefono; }
    public void setContactoTelefono(String contactoTelefono) { this.contactoTelefono = contactoTelefono; }

    public boolean isPropuestaEnviada() { return propuestaEnviada; }
    public void setPropuestaEnviada(boolean propuestaEnviada) { this.propuestaEnviada = propuestaEnviada; }

    public String getCondiciones() { return condiciones; }
    public void setCondiciones(String condiciones) { this.condiciones = condiciones; }

    public Long getCostoReferencial() { return costoReferencial; }
    public void setCostoReferencial(Long costoReferencial) { this.costoReferencial = costoReferencial; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public LocalDate getProximoSeguimiento() { return proximoSeguimiento; }
    public void setProximoSeguimiento(LocalDate proximoSeguimiento) { this.proximoSeguimiento = proximoSeguimiento; }

    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(LocalDateTime actualizadoEn) { this.actualizadoEn = actualizadoEn; }
}
