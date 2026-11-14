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

```bash
npm install -g @angular/cli
ng new taan-web --routing --style=scss --ssr=false
cd taan-web
ng generate environments
```

1. **Copia encima la carpeta `src` de `taan-web/`** de este paquete (reemplaza `app.routes.ts` y
   los archivos de `environments`).
2. En `src/app/app.config.ts` agrega el cliente HTTP con el interceptor de sesión dentro de `providers`:

   ```ts
   import { provideHttpClient, withInterceptors } from '@angular/common/http';
   import { authInterceptor } from './core/auth/auth.interceptor';

   // dentro de providers: [ ... ]
   provideHttpClient(withInterceptors([authInterceptor])),
   ```

3. `ng serve` y abre http://localhost:4200

## 4. App móvil (Ionic + Capacitor)

```bash
npm install -g @ionic/cli
ionic start taan-app blank --type=angular --capacitor
cd taan-app
npm install @capacitor/geolocation @capacitor/camera @capacitor/push-notifications
```

1. **Copia encima `src` y `capacitor.config.ts` de `taan-app/`** de este paquete.
2. Registra el mismo interceptor que en la web: en `src/main.ts`, dentro de los `providers` de
   `bootstrapApplication`, agrega `provideHttpClient(withInterceptors([authInterceptor]))`
   con los mismos `import` del paso 3.
3. Para probar en Android:

   ```bash
   ionic build
   npx cap add android
   npx cap open android
   ```

Las notificaciones push requieren además un proyecto de Firebase y su archivo
`google-services.json`; eso corresponde al incremento 7.

## 5. Opcional: generar el código que llama a la API

Con el contrato se puede generar automáticamente el cliente para Angular e Ionic
(requiere Java instalado):

```bash
npx @openapitools/openapi-generator-cli generate -i ../docs/TAAN_API.yaml -g typescript-angular -o src/app/api
```

## Pendientes conocidos (marcados en el código)

- **Polígono de la cabecera:** la migración trae un rectángulo provisional; se reemplaza por el
  polígono del INEGI con un `UPDATE` (ver el comentario en la sección 8.3 de la migración).
- **Adaptadores de IA y notificaciones:** su estructura está lista; la llamada real a Gemini, Groq
  y Firebase se implementa en los incrementos 5 y 7.
- **Emisión de tokens:** la verificación de JWT ya está configurada; la emisión (al iniciar sesión)
  se implementa en el módulo `cuentas` usando la misma clave.
