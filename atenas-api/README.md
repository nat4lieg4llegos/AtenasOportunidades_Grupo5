# atenas-api — Microservicio REST de oportunidades

Backend de la app **Atenas Oportunidades** (DSY1105, equipo 5). Expone las oportunidades de activación de Agencia Atenas como API REST para que la app las consuma con Retrofit.

## Ejecutar
1. Abrir esta carpeta (`atenas-api`) en **IntelliJ IDEA** con **File → Open** y esperar que Maven descargue las dependencias.
2. Ejecutar `src/main/java/cl/duoc/atenas/api/AtenasApiApplication.java` (botón ▶ verde junto a `main`).
3. Cuando la consola diga `Started AtenasApiApplication`, abrir en el navegador: http://localhost:8080/api/oportunidades

Requisitos: JDK 17 o superior. No hay que instalar base de datos: se usa **H2** guardada en `./data/atenasdb`.

## Endpoints
| Método | Ruta | Respuesta |
|---|---|---|
| GET | `/api/oportunidades` (opcional `?estado=Propuesta enviada`) | 200 + lista |
| GET | `/api/oportunidades/{id}` | 200, o 404 si no existe |
| POST | `/api/oportunidades` | 201 + oportunidad creada, o 400 con errores por campo |
| PUT | `/api/oportunidades/{id}` | 200, 400 o 404 |
| DELETE | `/api/oportunidades/{id}` | 204, o 404 |

Ejemplo de error 400:
```json
{ "estado": 400, "mensaje": "Hay campos con errores", "errores": { "nombre": "El nombre es obligatorio" } }
```

## Estructura
```
controller/  OportunidadController   recibe la petición HTTP y responde
service/     OportunidadService      reglas de negocio (ej.: el evento no puede ser anterior a su detección)
repository/  OportunidadRepository   acceso a datos con Spring Data JPA
model/       Oportunidad             entidad JPA + validaciones (@NotBlank, @Positive, @Email...)
exception/   ManejadorErrores        convierte errores en JSON (400 / 404)
```

## Ver la base de datos
http://localhost:8080/h2-console → JDBC URL `jdbc:h2:file:./data/atenasdb`, usuario `sa`, contraseña vacía → tabla `OPORTUNIDADES`.

## Pruebas
`src/test/java/.../OportunidadControllerTest.java`: 6 pruebas con MockMvc sobre H2 en memoria. Clic derecho → **Run**.
