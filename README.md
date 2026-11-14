# T'AAN — Plataforma inteligente de reportes y gestión ciudadana

Proyecto desarrollado en la Universidad Veracruzana (Facultad de Negocios y Tecnologías,
Ixtaczoquitlán). Piloto: cabecera municipal de Ixtaczoquitlán.

## Estructura del repositorio

```
taan/
├── docs/                 Documento maestro, contrato OpenAPI, esquema SQL, modelo DBML y diagrama de estados
├── docker-compose.yml    Base de datos local (PostgreSQL + PostGIS en Docker)
├── .env.ejemplo          Plantilla de configuración local (se copia como .env, que no se sube)
├── taan-backend/         Backend (Java 21 + Spring Boot)
├── taan-web/             Portal web (Angular)
└── taan-app/             App móvil (Ionic + Angular + Capacitor)
```

> **Estado actual:** `taan-backend/` es un proyecto completo y ejecutable (incremento 1 en curso:
> autenticación y roles). `taan-web/` y `taan-app/` contienen por ahora solo el código propio de
> T'AAN; el proyecto completo se genera con Angular CLI / Ionic CLI siguiendo `LEEME.md` cuando
> empiecen esos incrementos.

## Requisitos

- **Git**, **JDK 21**, **Docker Desktop** e **IntelliJ IDEA**.
- **Node.js LTS**: solo cuando se trabaje la web o la app.
- **Android Studio**: solo para compilar la app en Android.

## Puesta en marcha del backend

1. **Clonar el repositorio**

   ```bash
   git clone https://github.com/USUARIO/taan.git
   cd taan
   ```

2. **Levantar la base de datos**

   ```bash
   docker compose up -d
   ```

   Queda una base `taan` en `localhost:5432` (usuario `postgres`, contraseña `taan`).

   > **¿El puerto 5432 ya está ocupado en tu máquina?** (por ejemplo, porque tienes otro
   > PostgreSQL instalado en Windows): copia `.env.ejemplo` como `.env`, pon `DB_PORT=5433`
   > y vuelve a ejecutar `docker compose up -d`. En ese caso define también `DB_PORT=5433`
   > en la Run Configuration de IntelliJ (paso 3). Si no tienes conflicto, no necesitas
   > hacer nada de esto.

3. **Configurar las variables de entorno en IntelliJ**

   Abre la carpeta `taan-backend` en IntelliJ. En **Run → Edit Configurations… →
   Environment variables** de `TaanBackendApplication` define:

   | Variable               | Obligatoria | Qué es                                                                 |
   |------------------------|-------------|------------------------------------------------------------------------|
   | `TAAN_JWT_SECRETO`     | Sí          | Frase secreta de al menos 32 caracteres. Cada quien inventa la suya.   |
   | `TAAN_ADMIN_PASSWORD`  | Primera vez | Contraseña inicial del administrador; solo se usa al crear la cuenta.  |
   | `DB_PORT`              | No          | Solo si tu base no está en el 5432 (ver paso 2).                       |

   Las demás variables (`GEMINI_API_KEY`, `GROQ_API_KEY`, `GOOGLE_CLIENT_ID`,
   `TURNSTILE_SECRETO`, correo SMTP…) son opcionales en desarrollo: si están vacías, esa
   función queda deshabilitada. Ver `taan-backend/src/main/resources/application.yml`.

4. **Probar y arrancar**

   ```bash
   cd taan-backend
   ./mvnw test
   ./mvnw spring-boot:run
   ```

   (o ejecuta `TaanBackendApplication` desde IntelliJ). Al arrancar, Flyway crea las tablas y
   carga los datos del piloto, y se crea la cuenta del administrador si no existe.

5. **Verificar**: la documentación interactiva queda en
   <http://localhost:8080/api/v1/swagger-ui.html>

## Secretos y claves

- **Ningún secreto se escribe en archivos del repositorio.** Todos se leen de variables de
  entorno (Run Configuration de IntelliJ) o del `.env` local, que está en `.gitignore`.
- `.env.ejemplo` sí se sube: es la plantilla sin valores reales.
- La contraseña `taan` del `docker-compose.yml` es solo de la base **local** de desarrollo;
  el servidor real usa credenciales propias que no están en el repositorio.
- Si una clave real llega a subirse por error, se considera comprometida: hay que **rotarla**
  (generar una nueva) además de borrarla del historial.

## Flujo de trabajo con Git

- **`main` siempre arranca.** Nadie hace push directo a `main`: todo entra por Pull Request
  revisado por el otro integrante.
- **Una rama por tarea**, nombrada con el incremento y una descripción corta:
  `i1/emision-tokens`, `i1/login-google`, `i2/alta-reportes`…

```bash
git switch main
git pull                          # siempre partir de main actualizado
git switch -c i1/emision-tokens   # crear la rama de la tarea
# ... trabajar, con commits pequeños ...
git push -u origin i1/emision-tokens
```

Después, en GitHub: **Compare & pull request** → el compañero revisa y aprueba → **Merge** →
borrar la rama. De vuelta en local: `git switch main && git pull`.

- **Commits**: mensajes cortos en imperativo que digan qué cambia y por qué si no es obvio.
  Ejemplo: `Emite tokens de acceso y refresco al iniciar sesión con correo`.
- **Antes de cada commit**: revisar `git status` para confirmar que no se cuele ningún
  archivo local (`.env`, `.idea/`, `archivos/`…).
