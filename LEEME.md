# T'AAN — Estructura base de los proyectos

Este paquete contiene la base de los tres proyectos de T'AAN y la documentación técnica que los guía.
Los proyectos **no se entregan completos a propósito**: las herramientas oficiales (Spring Initializr,
Angular CLI e Ionic CLI) generan la parte estándar con las versiones vigentes, y encima se copia lo
que es propio de T'AAN. Así no hay versiones desactualizadas ni archivos de configuración inventados.

```
taan/
├── docs/                 Contrato de la API, script SQL, modelo DBML y diagrama de estados
├── docker-compose.yml    Base de datos local (PostgreSQL + PostGIS)
├── taan-backend/         Lo propio del backend (Java 21 + Spring Boot)
├── taan-web/             Lo propio de la web (Angular)
└── taan-app/             Lo propio de la app móvil (Ionic + Angular + Capacitor)
```

## 0. Requisitos

- **Java 21** (JDK), **Node.js** en versión LTS y **Docker Desktop**.
- **Android Studio**, solo para compilar la app.

## 1. Base de datos
      
```bash
docker compose up -d
```

Queda una base `taan` en `localhost:5432` (usuario `postgres`, contraseña `taan`). No hay que
ejecutar el script SQL a mano: el backend lo aplica solo al arrancar (paso 2).

> Para el servidor de Oracle Cloud (procesador ARM) verifica que la imagen de PostGIS que uses
> tenga versión para ARM, o instala PostgreSQL y PostGIS directamente en la máquina.

## 2. Backend (Spring Boot)

1. Genera el proyecto con este enlace de Spring Initializr (ya trae las dependencias marcadas)
   y pulsa **Generate**:

   https://start.spring.io/#!type=maven-project&language=java&groupId=mx.uv&artifactId=taan-backend&name=taan-backend&packageName=mx.uv.taan&javaVersion=21&dependencies=web,validation,data-jpa,security,oauth2-resource-server,postgresql,flyway,mail,actuator

2. Descomprímelo y **copia encima la carpeta `src` de `taan-backend/`** de este paquete
   (combinar carpetas; reemplaza `application.properties` por nuestro `application.yml`:
   borra el `.properties` generado).
3. Abre `pom.xml` y pega dentro de `<dependencies>` lo que indica `pom-dependencias-adicionales.xml`
   (PostGIS para JPA y la documentación interactiva). Lee ahí qué versión de springdoc usar.
4. Define las variables de entorno obligatorias. En **PowerShell**:

   ```powershell
   $env:TAAN_JWT_SECRETO = "una-frase-secreta-de-al-menos-32-caracteres"
   $env:TAAN_ADMIN_PASSWORD = "ContraseñaInicialDelAdmin"
   ```

   En **macOS o Linux**: `export TAAN_JWT_SECRETO="..."` y `export TAAN_ADMIN_PASSWORD="..."`.
   La contraseña del administrador solo se usa la primera vez: el backend crea la cuenta con la
   contraseña cifrada con BCrypt y obliga a cambiarla en el primer acceso. **Nunca escribas
   contraseñas ni claves en archivos del repositorio.**

5. Ejecuta las pruebas y arranca:

   ```bash
   ./mvnw test
   ./mvnw spring-boot:run
   ```

   Al arrancar, Flyway crea las 20 tablas y carga los datos del piloto de Ixtaczoquitlán.
   La documentación interactiva queda en http://localhost:8080/api/v1/swagger-ui.html

**Qué trae:** la organización por módulos (cada carpeta tiene un `package-info.java` que explica
qué hace y qué casos de uso cubre), la máquina de estados del Anexo G con sus pruebas, el manejo de
errores con los códigos del contrato, la seguridad con JWT y roles, el adaptador de IA con Gemini
como principal y Groq como respaldo (con sus pruebas), y la creación segura de la cuenta del
administrador.

## 3. Web (Angular)

La web ya es un proyecto completo de Angular 22. Los proyectos de frontend forman un **workspace de
npm** en la raíz (`package.json`): las dependencias se instalan en `taan/node_modules`, donde también
las encuentra el código compartido de `taan-comun/` (alias `@taan/comun` en `tsconfig.json`).

```bash
npm install            # en la raíz del repositorio; crea src/environments/environment.local.ts
cd taan-web
npm start              # http://localhost:4200 (el backend debe correr en http://localhost:8080)
npm test               # pruebas unitarias (Vitest), incluidas las de taan-comun
```

`environment.local.ts` guarda las claves públicas de navegador (por ahora la site key de Turnstile;
la plantilla trae la clave de prueba de Cloudflare). No se sube al repositorio.

## 4. App móvil (Ionic + Capacitor)

```bash
npm install -g @ionic/cli
ionic start taan-app blank --type=angular --capacitor
cd taan-app
npm install @capacitor/geolocation @capacitor/camera @capacitor/push-notifications
```

1. **Copia encima `src` y `capacitor.config.ts` de `taan-app/`** de este paquete.
2. Agrega `taan-app` a `workspaces` en el `package.json` de la raíz y el alias
   `"@taan/comun/*": ["../taan-comun/src/*"]` en su `tsconfig.json`. Usa la misma versión de
   Angular que la web: si no, el código compartido cargaría dos copias de Angular.
3. En `src/main.ts`, dentro de los `providers` de `bootstrapApplication`, registra el cliente y la
   sesión compartidos: `provideHttpClient(withInterceptors([authInterceptor]))` y
   `provideApi(environment.apiUrl)` (de `@taan/comun/sesion/auth.interceptor` y `@taan/comun/api`).
4. Para probar en Android:

   ```bash
   ionic build
   npx cap add android
   npx cap open android
   ```

Las notificaciones push requieren además un proyecto de Firebase y su archivo
`google-services.json`; eso corresponde al incremento 7.

## 5. Cliente de la API

El cliente de Angular se genera desde el contrato en `taan-comun/src/api` y se sube al repositorio,
así que compilar la web o la app no requiere Java. Cuando cambie `docs/TAAN_API.yaml`, regenéralo
(requiere Java):

```bash
cd taan-web
npm run api
```

## Pendientes conocidos (marcados en el código)

- **Polígono de la cabecera:** la migración trae un rectángulo provisional; se reemplaza por el
  polígono del INEGI con un `UPDATE` (ver el comentario en la sección 8.3 de la migración).
- **Adaptadores de IA y notificaciones:** su estructura está lista; la llamada real a Gemini, Groq
  y Firebase se implementa en los incrementos 5 y 7.
- **Emisión de tokens:** la verificación de JWT ya está configurada; la emisión (al iniciar sesión)
  se implementa en el módulo `cuentas` usando la misma clave.
