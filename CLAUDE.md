# Reglas de trabajo

- Hacer lo necesario para cumplir la tarea, sin agregar código, clases ni comentarios de más.
- Aplicar principios SOLID y patrones de diseño solo donde realmente aporten; no sobre-ingeniería.
- Seguir buenas prácticas de Spring Boot: capas claras, inyección por constructor, transacciones donde correspondan, validación en los DTO.
- Código simple, legible y sin duplicación; evitar consultas ineficientes (N+1, cargar de más).
- Arquitectura: monolito modular mx.uv.taan.{cuentas, administracion, configuracion, reportes, atencion, archivos, ia, notificaciones, comun}. Ningún módulo usa los repositorios de otro; se comunican por servicios.
- Cada cambio con sus pruebas unitarias; compilar y correr los tests antes de dar algo por terminado.
- Si algo no cuadra con el contrato TAAN_API.yaml, avisarme en lugar de decidir solo.
- Al proponer un commit, incluir también la descripción del Pull Request: una línea de resumen, viñetas cortas y las pruebas nuevas.
- Nunca poner secretos (JWT, contraseñas, claves de cifrado) en archivos del repo.

> La arquitectura y los incrementos vienen del documento maestro (`docs/Documento_Maestro_TAAN.docx`) y del contrato `docs/TAAN_API.yaml`.

## Convenciones del backend

- Los módulos acceden a los reportes de otro módulo solo por `ServicioExpedientes` (módulo reportes).
- Las transiciones de estado se hacen en la entidad `Reporte` y se validan con `EstadoReporte.validarTransicion` (409 `TRANSICION_INVALIDA`).
- Cada cambio bloquea el reporte con `bloquearPorFolio`, registra un evento público en el historial y, si hay justificación, una nota interna con `visible_ciudadano=false`.
- Alcance: personal de área ve solo su área; coordinación toda su organización; fuera de alcance responde 404.
- Errores con `ReglaNegocioException` y formato `ApiError` (código, mensaje, detalles).
- Valores de enums en minúsculas en la API.
- Documentos de referencia en `docs/`: `TAAN_API.yaml` (contrato), `Estados_TAAN.puml` (estados) y el documento maestro (`Documento_Maestro_TAAN.docx`).

## Convenciones del frontend

- taan-web (Angular) y taan-app (Ionic + Capacitor, solo ciudadanos) comparten `taan-comun/`
  (alias `@taan/comun`): cliente de la API, sesión, interceptores y errores. Nada de esto se duplica.
- Las dependencias del frontend se instalan en la raíz (workspace de npm); taan-app debe usar la
  misma versión de Angular que taan-web.
- El cliente de la API se genera desde `docs/TAAN_API.yaml` con `npm run api`; no se editan
  a mano ni se escriben rutas o tipos del backend en los componentes.
- Componentes standalone, signals y `OnPush`; carga diferida por sección
  (publico, ciudadano, institucional, admin). Formularios reactivos tipados.
- Errores: siempre por `leerApiError` y `mensajeDeError(codigo)`; `VALIDACION` se marca por campo;
  nunca se pierde lo capturado ni se muestran detalles técnicos. Mensajes como medida preventiva (RNF-022).
- Los guards son comodidad de interfaz; los permisos y el alcance los decide el backend (404 fuera de alcance).
- Mínima interacción: el ciudadano reporta en una sola pantalla (qué, dónde, foto opcional);
  no se agregan pasos, confirmaciones ni campos que el documento no pida.
- Accesibilidad (RNF-004): etiquetas en todos los campos, uso con teclado y estado/semáforo
  nunca solo por color.
- Rutas que llegan por correo y deben existir: `/mis-reportes/:folio`, `/verificacion?token=`,
  `/recuperacion?token=`.
- Claves públicas (Maps, Google client id, Turnstile site key) en `environment.local.ts`, ignorado por git.
- Cada pantalla o servicio con pruebas (`ng test`); compilar y probar antes de darlo por terminado.
