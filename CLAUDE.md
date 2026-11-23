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
