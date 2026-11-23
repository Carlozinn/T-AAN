-- =====================================================================
-- T'AAN - Plataforma inteligente de reportes y gestión ciudadana
-- Esquema inicial de base de datos + datos de la prueba piloto
-- Motor: PostgreSQL 16+ con PostGIS 3
-- Corresponde al cuarto borrador del documento maestro y al modelo
-- TAAN_modelo_datos.dbml (dbdiagram).
--
-- Cómo ejecutarlo:
--   createdb taan
--   psql -d taan -f TAAN_esquema.sql
-- O con Docker (incluye PostGIS):
--   docker run --name taan-db -e POSTGRES_PASSWORD=taan -e POSTGRES_DB=taan \
--     -p 5432:5432 -d postgis/postgis:16-3.4
--   psql -h localhost -U postgres -d taan -f TAAN_esquema.sql
--
-- MARCADORES A REEMPLAZAR antes de usarlo en serio:
--   [HASH]     hashes de contraseña reales (los genera el backend con BCrypt)
--   El polígono de la cabecera es PROVISIONAL: sustituir por el polígono
--   de la localidad urbana de Ixtaczoquitlán del Marco Geoestadístico (INEGI).
-- =====================================================================

BEGIN;

CREATE EXTENSION IF NOT EXISTS postgis;

-- ---------------------------------------------------------------------
-- 1. Catálogos fijos (tipos enumerados)
-- ---------------------------------------------------------------------

CREATE TYPE metodo_acceso AS ENUM ('google', 'correo');

CREATE TYPE estado_cuenta AS ENUM ('activa', 'suspendida');

CREATE TYPE estado_senalamiento AS ENUM ('pendiente', 'aceptado', 'desestimado');

CREATE TYPE rol_usuario AS ENUM ('administrador', 'coordinacion', 'personal_area');

CREATE TYPE nivel_participacion AS ENUM ('integrada', 'conectada', 'externa');

CREATE TYPE estado_reporte AS ENUM (
  'pendiente_canalizacion',  -- transitorio; el ciudadano lo ve como "En revisión"
  'recibido',
  'validado',
  'asignado',
  'en_atencion',
  'resuelto',
  'reabierto',
  'rechazado',
  'duplicado',
  'canalizacion_externa'
);

CREATE TYPE motivo_canalizacion AS ENUM (
  'regla',              -- una regla aplicable de mayor prioridad
  'sin_regla',          -- organización receptora por defecto
  'empate_reglas',      -- organización receptora por defecto
  'sin_clasificacion',  -- la IA falló tras los reintentos
  'redireccion'         -- redirigido por personal institucional (CU-21)
);

CREATE TYPE motivo_rechazo AS ENUM (
  'no_corresponde',
  'informacion_insuficiente',
  'ya_atendido',
  'no_servicio_publico'
);

CREATE TYPE origen_reporte AS ENUM ('web', 'app');

CREATE TYPE tipo_evidencia AS ENUM ('ciudadana', 'atencion');

CREATE TYPE tipo_canal AS ENUM ('telefono', 'sitio_web', 'correo', 'otro');

CREATE TYPE resultado_acceso AS ENUM ('exitoso', 'fallido', 'limitado');

CREATE TYPE resultado_envio AS ENUM (
  'aceptado',
  'rechazado_limite',
  'rechazado_verificacion',
  'rechazado_honeypot',
  'rechazado_suspension'
);

CREATE TYPE formato_informe AS ENUM ('pdf', 'hoja_calculo');

-- ---------------------------------------------------------------------
-- 2. Configuración institucional
-- ---------------------------------------------------------------------

CREATE TABLE organizacion (
  id                    bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  nombre                varchar(200) NOT NULL UNIQUE,
  nivel_participacion   nivel_participacion NOT NULL,
  plazo_reapertura_dias int NOT NULL DEFAULT 15 CHECK (plazo_reapertura_dias > 0),
  activa                boolean NOT NULL DEFAULT true,
  creado_en             timestamptz NOT NULL DEFAULT now(),
  actualizado_en        timestamptz NOT NULL DEFAULT now()
);
COMMENT ON TABLE organizacion IS
  'Ayuntamientos y otras instituciones. Desactivar no elimina historial (RF-027).';

CREATE TABLE tiempo_objetivo (
  organizacion_id bigint NOT NULL REFERENCES organizacion (id),
  estado          estado_reporte NOT NULL,
  horas_amarillo  int NOT NULL CHECK (horas_amarillo > 0),
  horas_rojo      int NOT NULL,
  PRIMARY KEY (organizacion_id, estado),
  CHECK (horas_rojo > horas_amarillo)
);
COMMENT ON TABLE tiempo_objetivo IS
  'Semáforo de antigüedad por estado, configurado por la coordinación (RF-041).';

CREATE TABLE area (
  id               bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  organizacion_id  bigint NOT NULL REFERENCES organizacion (id),
  nombre           varchar(150) NOT NULL,
  telefono_publico varchar(20),
  correo_publico   varchar(150),
  funciones        varchar(1000),
  activa           boolean NOT NULL DEFAULT true,
  creado_en        timestamptz NOT NULL DEFAULT now(),
  UNIQUE (organizacion_id, nombre),
  UNIQUE (id, organizacion_id)  -- permite verificar por llave compuesta que un área pertenece a su organización
);
COMMENT ON TABLE area IS
  'Áreas internas de cada organización; las gestiona su coordinación (RF-033, CU-13).';
COMMENT ON COLUMN area.telefono_publico IS
  'Contacto oficial que ve el ciudadano cuando su reporte es rechazado (RF-036).';
COMMENT ON COLUMN area.funciones IS
  'Qué atiende el área, en texto libre; se envía a la IA para proponer la correspondencia (CU-22).';

CREATE TABLE usuario (
  id                  bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  correo              varchar(150) NOT NULL UNIQUE,
  password_hash       varchar(255) NOT NULL,
  nombre              varchar(150) NOT NULL,
  rol                 rol_usuario NOT NULL,
  organizacion_id     bigint REFERENCES organizacion (id),
  area_id             bigint,
  credencial_temporal boolean NOT NULL DEFAULT false,
  activo              boolean NOT NULL DEFAULT true,
  ultimo_acceso       timestamptz,
  creado_en           timestamptz NOT NULL DEFAULT now(),
  FOREIGN KEY (area_id, organizacion_id) REFERENCES area (id, organizacion_id),
  CHECK (
    (rol = 'administrador'  AND organizacion_id IS NULL     AND area_id IS NULL) OR
    (rol = 'coordinacion'   AND organizacion_id IS NOT NULL AND area_id IS NULL) OR
    (rol = 'personal_area'  AND organizacion_id IS NOT NULL AND area_id IS NOT NULL)
  )
);
COMMENT ON TABLE usuario IS
  'Usuarios institucionales (personal de área, coordinación, administradores). Las cuentas ciudadanas viven en la tabla ciudadano.';

CREATE TABLE categoria (
  id                 bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  nombre             varchar(150) NOT NULL,
  categoria_padre_id bigint REFERENCES categoria (id),
  activa             boolean NOT NULL DEFAULT true
);
-- Unicidad: el nombre no se repite dentro del mismo padre; NULLS NOT DISTINCT
-- hace únicas también las categorías raíz (PostgreSQL 15+).
CREATE UNIQUE INDEX ux_categoria_nombre_padre
  ON categoria (nombre, categoria_padre_id) NULLS NOT DISTINCT;
COMMENT ON TABLE categoria IS
  'Catálogo común para todos los municipios (RF-028). Una fila con padre es una subcategoría.';

CREATE TABLE zona_cobertura (
  id                         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  nombre                     varchar(150) NOT NULL,
  geometria                  geometry(MultiPolygon, 4326) NOT NULL,
  organizacion_receptora_id  bigint NOT NULL REFERENCES organizacion (id),
  activa                     boolean NOT NULL DEFAULT true,
  creado_en                  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_zona_geometria ON zona_cobertura USING gist (geometria);
COMMENT ON COLUMN zona_cobertura.organizacion_receptora_id IS
  'Recibe los reportes de la zona cuando ninguna regla aplica (RF-037).';

CREATE TABLE zona_organizacion (
  zona_id         bigint NOT NULL REFERENCES zona_cobertura (id),
  organizacion_id bigint NOT NULL REFERENCES organizacion (id),
  PRIMARY KEY (zona_id, organizacion_id)
);
COMMENT ON TABLE zona_organizacion IS
  'Organizaciones con cobertura en cada zona; define a quién puede redirigirse un reporte (CU-21).';

CREATE TABLE regla_canalizacion (
  id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  zona_id         bigint NOT NULL REFERENCES zona_cobertura (id),
  categoria_id    bigint NOT NULL REFERENCES categoria (id),
  organizacion_id bigint NOT NULL REFERENCES organizacion (id),
  area_id         bigint,
  prioridad       int NOT NULL DEFAULT 0,
  vigente_desde   date NOT NULL DEFAULT current_date,
  vigente_hasta   date,
  activa          boolean NOT NULL DEFAULT true,
  FOREIGN KEY (area_id, organizacion_id) REFERENCES area (id, organizacion_id),
  CHECK (vigente_hasta IS NULL OR vigente_hasta >= vigente_desde)
);
CREATE INDEX ix_regla_zona_categoria ON regla_canalizacion (zona_id, categoria_id, activa);
COMMENT ON TABLE regla_canalizacion IS
  'Territorio + categoría -> organización y, opcionalmente, área (RF-030). Un empate de prioridad equivale a no tener regla.';

CREATE TABLE canal_externo (
  id                   bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  organizacion_id      bigint NOT NULL REFERENCES organizacion (id),
  tipo                 tipo_canal NOT NULL,
  valor                varchar(255) NOT NULL,
  fuente_verificacion  varchar(255) NOT NULL,
  activo               boolean NOT NULL DEFAULT true
);
COMMENT ON TABLE canal_externo IS
  'Canales oficiales que ve el ciudadano cuando la canalización es externa (RF-036).';

-- ---------------------------------------------------------------------
-- 3. Plantilla municipal (RF-039, CU-22)
-- ---------------------------------------------------------------------

CREATE TABLE plantilla_area (
  id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  nombre      varchar(150) NOT NULL UNIQUE,
  descripcion text
);
COMMENT ON TABLE plantilla_area IS
  'Áreas estándar de un ayuntamiento según los servicios del artículo 115 constitucional.';

CREATE TABLE plantilla_correspondencia (
  plantilla_area_id bigint NOT NULL REFERENCES plantilla_area (id),
  categoria_id      bigint NOT NULL REFERENCES categoria (id),
  PRIMARY KEY (plantilla_area_id, categoria_id)
);
COMMENT ON TABLE plantilla_correspondencia IS
  'Qué categoría atiende cada área estándar; se copia a la organización al configurarla (CU-22).';

-- ---------------------------------------------------------------------
-- 4. Cuentas ciudadanas (RF-001, RF-012, RF-038, RF-043)
-- ---------------------------------------------------------------------

CREATE TABLE ciudadano (
  id                bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  nombre            varchar(150) NOT NULL,
  correo            varchar(150) NOT NULL UNIQUE,
  metodo_acceso     metodo_acceso NOT NULL,
  google_sub        varchar(255) UNIQUE,
  password_hash     varchar(255),
  correo_verificado boolean NOT NULL DEFAULT false,
  telefono_cifrado  text,
  avisos_correo     boolean NOT NULL DEFAULT true,
  estado            estado_cuenta NOT NULL DEFAULT 'activa',
  motivo_suspension text,
  suspendida_hasta  timestamptz,
  creado_en         timestamptz NOT NULL DEFAULT now(),
  actualizado_en    timestamptz NOT NULL DEFAULT now(),
  CHECK (
    (metodo_acceso = 'google' AND google_sub IS NOT NULL AND password_hash IS NULL) OR
    (metodo_acceso = 'correo' AND password_hash IS NOT NULL)
  ),
  CHECK (estado = 'activa' OR motivo_suspension IS NOT NULL)
);
COMMENT ON TABLE ciudadano IS
  'Cuenta ciudadana (Google o correo verificado). Eliminarla borra la fila; los reportes quedan desvinculados (ciudadano_id en NULL). El teléfono es obligatorio antes del primer reporte (lo valida el backend) y se guarda cifrado.';
COMMENT ON COLUMN ciudadano.suspendida_hasta IS
  'NULL con estado suspendida = suspensión indefinida (reincidencia, RF-043).';

-- ---------------------------------------------------------------------
-- 5. Reportes
-- ---------------------------------------------------------------------

CREATE TABLE reporte (
  id                           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  folio                        varchar(20) NOT NULL UNIQUE,
  ciudadano_id                 bigint REFERENCES ciudadano (id) ON DELETE SET NULL,
  descripcion                  text NOT NULL,
  ubicacion                    geometry(Point, 4326) NOT NULL,
  origen                       origen_reporte NOT NULL,
  estado                       estado_reporte NOT NULL DEFAULT 'pendiente_canalizacion',
  fecha_ultimo_estado          timestamptz NOT NULL DEFAULT now(),
  resultado_ia                 jsonb,
  intentos_ia                  int NOT NULL DEFAULT 0,
  categoria_sugerida_id        bigint REFERENCES categoria (id),
  categoria_final_id           bigint REFERENCES categoria (id),
  posible_contenido_no_valido  boolean NOT NULL DEFAULT false,
  organizacion_id              bigint REFERENCES organizacion (id),
  area_id                      bigint,
  motivo_canalizacion          motivo_canalizacion,
  motivo_rechazo               motivo_rechazo,
  texto_rechazo_publico        text,
  reporte_principal_id         bigint REFERENCES reporte (id),
  reabierto                    boolean NOT NULL DEFAULT false,
  fecha_resolucion             timestamptz,
  creado_en                    timestamptz NOT NULL DEFAULT now(),
  actualizado_en               timestamptz NOT NULL DEFAULT now(),
  FOREIGN KEY (area_id, organizacion_id) REFERENCES area (id, organizacion_id),
  CHECK (estado <> 'rechazado' OR motivo_rechazo IS NOT NULL),
  CHECK (estado <> 'duplicado' OR reporte_principal_id IS NOT NULL),
  CHECK (reporte_principal_id IS NULL OR reporte_principal_id <> id),
  CHECK (estado = 'pendiente_canalizacion' OR organizacion_id IS NOT NULL)
);
CREATE INDEX ix_reporte_ubicacion      ON reporte USING gist (ubicacion);
CREATE INDEX ix_reporte_ciudadano      ON reporte (ciudadano_id, creado_en);
CREATE INDEX ix_reporte_org_estado     ON reporte (organizacion_id, estado);
CREATE INDEX ix_reporte_area_estado    ON reporte (area_id, estado);
CREATE INDEX ix_reporte_principal      ON reporte (reporte_principal_id);
CREATE INDEX ix_reporte_creado         ON reporte (creado_en);
COMMENT ON TABLE reporte IS
  'Núcleo del sistema. El teléfono no se duplica aquí: se consulta por la cuenta. "Reportado por N personas" = COUNT de reportes cuyo reporte_principal_id apunta a este. La regla "solo una reapertura" la aplica el backend con la bandera reabierto.';

CREATE TABLE evidencia (
  id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  reporte_id   bigint NOT NULL REFERENCES reporte (id),
  tipo         tipo_evidencia NOT NULL,
  ruta_archivo varchar(500) NOT NULL,
  tipo_mime    varchar(50) NOT NULL,
  tamano_bytes int NOT NULL CHECK (tamano_bytes > 0),
  usuario_id   bigint REFERENCES usuario (id),
  creado_en    timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_evidencia_reporte ON evidencia (reporte_id);
COMMENT ON TABLE evidencia IS
  'Fotografías ciudadanas (sin metadatos GPS, RNF-017) y evidencia de atención. usuario_id nulo = la subió el ciudadano.';

CREATE TABLE historial_reporte (
  id                        bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  reporte_id                bigint NOT NULL REFERENCES reporte (id),
  estado_anterior           estado_reporte,
  estado_nuevo              estado_reporte,
  accion                    varchar(50) NOT NULL,
  organizacion_anterior_id  bigint REFERENCES organizacion (id),
  organizacion_nueva_id     bigint REFERENCES organizacion (id),
  area_anterior_id          bigint REFERENCES area (id),
  area_nueva_id             bigint REFERENCES area (id),
  observaciones             text,
  visible_ciudadano         boolean NOT NULL DEFAULT false,
  usuario_id                bigint REFERENCES usuario (id),
  creado_en                 timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_historial_reporte ON historial_reporte (reporte_id, creado_en);
COMMENT ON TABLE historial_reporte IS
  'Solo inserciones: ningún registro se modifica ni se borra (RF-017, RNF-018). usuario_id nulo = actuó el sistema o el ciudadano.';

CREATE TABLE suscripcion_aviso (
  id                bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  ciudadano_id      bigint NOT NULL REFERENCES ciudadano (id) ON DELETE CASCADE,
  token_dispositivo varchar(500) NOT NULL,
  creado_en         timestamptz NOT NULL DEFAULT now(),
  UNIQUE (ciudadano_id, token_dispositivo)
);
COMMENT ON TABLE suscripcion_aviso IS
  'Tokens de Firebase Cloud Messaging por cuenta (RF-034). Se elimina al cerrar sesión en el dispositivo o al eliminar la cuenta.';

CREATE TABLE senalamiento_cuenta (
  id            bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  reporte_id    bigint NOT NULL REFERENCES reporte (id),
  ciudadano_id  bigint REFERENCES ciudadano (id) ON DELETE SET NULL,
  senalado_por  bigint NOT NULL REFERENCES usuario (id),
  motivo        text NOT NULL,
  estado        estado_senalamiento NOT NULL DEFAULT 'pendiente',
  resuelto_por  bigint REFERENCES usuario (id),
  resolucion    text,
  creado_en     timestamptz NOT NULL DEFAULT now(),
  resuelto_en   timestamptz,
  CHECK (estado = 'pendiente' OR (resuelto_por IS NOT NULL AND resuelto_en IS NOT NULL))
);
CREATE INDEX ix_senalamiento_estado    ON senalamiento_cuenta (estado, creado_en);
CREATE INDEX ix_senalamiento_ciudadano ON senalamiento_cuenta (ciudadano_id);
COMMENT ON TABLE senalamiento_cuenta IS
  'Historial de moderación (RF-043, CU-26). La suspensión vigente se refleja en ciudadano; aquí queda la auditoría.';

-- ---------------------------------------------------------------------
-- 6. Seguridad y auditoría
-- ---------------------------------------------------------------------

CREATE TABLE intento_acceso (
  id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  origen_ip    inet NOT NULL,
  ciudadano_id bigint REFERENCES ciudadano (id) ON DELETE SET NULL,
  usuario_id   bigint REFERENCES usuario (id),
  resultado    resultado_acceso NOT NULL,
  creado_en    timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_acceso_ip        ON intento_acceso (origen_ip, creado_en);
CREATE INDEX ix_acceso_ciudadano ON intento_acceso (ciudadano_id, creado_en);
COMMENT ON TABLE intento_acceso IS
  'Límite de intentos de inicio de sesión por cuenta y por origen (RNF-009). No almacena contraseñas.';

CREATE TABLE registro_envio (
  id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  origen_ip      inet NOT NULL,
  ciudadano_id   bigint REFERENCES ciudadano (id) ON DELETE SET NULL,
  agente_usuario varchar(500),
  resultado      resultado_envio NOT NULL,
  reporte_id     bigint REFERENCES reporte (id),
  creado_en      timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_envio_ip        ON registro_envio (origen_ip, creado_en);
CREATE INDEX ix_envio_ciudadano ON registro_envio (ciudadano_id, creado_en);
COMMENT ON TABLE registro_envio IS
  'Registro técnico antiabuso (RNF-022): permite el límite por cuenta y por origen. No visible para el personal; se borra al vencer el plazo de conservación.';

CREATE TABLE informe_generado (
  id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  usuario_id      bigint NOT NULL REFERENCES usuario (id),
  organizacion_id bigint NOT NULL REFERENCES organizacion (id),
  area_id         bigint REFERENCES area (id),
  periodo_inicio  date NOT NULL,
  periodo_fin     date NOT NULL,
  filtros         jsonb,
  formato         formato_informe NOT NULL,
  creado_en       timestamptz NOT NULL DEFAULT now(),
  CHECK (periodo_fin >= periodo_inicio)
);
COMMENT ON TABLE informe_generado IS
  'Quién generó cada informe y cuándo (RF-026). Los informes no incluyen datos personales de ciudadanos.';

-- ---------------------------------------------------------------------
-- 7. Fecha de actualización automática
-- ---------------------------------------------------------------------

CREATE FUNCTION actualizar_fecha() RETURNS trigger AS $$
BEGIN
  NEW.actualizado_en := now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tg_organizacion_fecha BEFORE UPDATE ON organizacion
  FOR EACH ROW EXECUTE FUNCTION actualizar_fecha();
CREATE TRIGGER tg_ciudadano_fecha BEFORE UPDATE ON ciudadano
  FOR EACH ROW EXECUTE FUNCTION actualizar_fecha();
CREATE TRIGGER tg_reporte_fecha BEFORE UPDATE ON reporte
  FOR EACH ROW EXECUTE FUNCTION actualizar_fecha();

-- =====================================================================
-- 8. DATOS INICIALES
-- =====================================================================

-- 8.1 Catálogo de categorías del piloto (4 categorías, 9 subcategorías)

INSERT INTO categoria (nombre) VALUES
  ('Alumbrado público'),
  ('Agua potable y drenaje'),
  ('Limpia y residuos'),
  ('Vialidades');

INSERT INTO categoria (nombre, categoria_padre_id)
SELECT s.nombre, c.id
FROM (VALUES
  ('Luminaria apagada',    'Alumbrado público'),
  ('Poste dañado',         'Alumbrado público'),
  ('Fuga de agua',         'Agua potable y drenaje'),
  ('Falta de agua',        'Agua potable y drenaje'),
  ('Coladera tapada',      'Agua potable y drenaje'),
  ('Basura acumulada',     'Limpia y residuos'),
  ('Falta de recolección', 'Limpia y residuos'),
  ('Bache',                'Vialidades'),
  ('Banqueta dañada',      'Vialidades')
) AS s (nombre, padre)
JOIN categoria c ON c.nombre = s.padre AND c.categoria_padre_id IS NULL;

-- 8.2 Plantilla municipal (áreas estándar del artículo 115 y su correspondencia)

INSERT INTO plantilla_area (nombre, descripcion) VALUES
  ('Alumbrado Público',       'Servicio de alumbrado público (art. 115, fracción III, inciso b).'),
  ('Agua Potable y Drenaje',  'Agua potable, drenaje, alcantarillado, tratamiento y disposición de aguas residuales (art. 115, fracción III, inciso a).'),
  ('Limpia Pública',          'Limpia, recolección, traslado, tratamiento y disposición final de residuos (art. 115, fracción III, inciso c).'),
  ('Obras Públicas',          'Calles, banquetas y su equipamiento (art. 115, fracción III, inciso g).');

INSERT INTO plantilla_correspondencia (plantilla_area_id, categoria_id)
SELECT pa.id, c.id
FROM (VALUES
  ('Alumbrado público',       'Alumbrado Público'),
  ('Agua potable y drenaje',  'Agua Potable y Drenaje'),
  ('Limpia y residuos',       'Limpia Pública'),
  ('Vialidades',              'Obras Públicas')
) AS m (categoria, area)
JOIN categoria c       ON c.nombre = m.categoria AND c.categoria_padre_id IS NULL
JOIN plantilla_area pa ON pa.nombre = m.area;

-- 8.3 Prueba piloto: Ayuntamiento de Ixtaczoquitlán

INSERT INTO organizacion (nombre, nivel_participacion)
VALUES ('Ayuntamiento de Ixtaczoquitlán', 'integrada');

INSERT INTO area (organizacion_id, nombre)
SELECT o.id, pa.nombre
FROM organizacion o
CROSS JOIN plantilla_area pa
WHERE o.nombre = 'Ayuntamiento de Ixtaczoquitlán';

-- Zona de cobertura: cabecera municipal.
-- POLÍGONO PROVISIONAL (rectángulo aproximado alrededor de la cabecera).
-- Reemplazar por el polígono de la localidad urbana del Marco
-- Geoestadístico del INEGI (importar el GeoJSON con ST_GeomFromGeoJSON
-- o con ogr2ogr) antes de las pruebas.
INSERT INTO zona_cobertura (nombre, geometria, organizacion_receptora_id)
SELECT 'Cabecera municipal de Ixtaczoquitlán',
       ST_Multi(ST_GeomFromText(
         'POLYGON((-97.09 18.82, -97.03 18.82, -97.03 18.88, -97.09 18.88, -97.09 18.82))',
         4326)),
       o.id
FROM organizacion o
WHERE o.nombre = 'Ayuntamiento de Ixtaczoquitlán';

INSERT INTO zona_organizacion (zona_id, organizacion_id)
SELECT z.id, o.id
FROM zona_cobertura z, organizacion o
WHERE z.nombre = 'Cabecera municipal de Ixtaczoquitlán'
  AND o.nombre = 'Ayuntamiento de Ixtaczoquitlán';

-- 8.4 Reglas de canalización del piloto
-- Una regla por categoría Y por subcategoría (la IA puede proponer
-- cualquiera de las dos), todas hacia el área correspondiente.

INSERT INTO regla_canalizacion (zona_id, categoria_id, organizacion_id, area_id, prioridad)
SELECT z.id,
       c.id,
       o.id,
       a.id,
       10
FROM zona_cobertura z
JOIN organizacion o  ON o.nombre = 'Ayuntamiento de Ixtaczoquitlán'
JOIN categoria raiz  ON raiz.categoria_padre_id IS NULL
JOIN categoria c     ON c.id = raiz.id OR c.categoria_padre_id = raiz.id
JOIN plantilla_correspondencia pc ON pc.categoria_id = raiz.id
JOIN plantilla_area pa ON pa.id = pc.plantilla_area_id
JOIN area a          ON a.organizacion_id = o.id AND a.nombre = pa.nombre
WHERE z.nombre = 'Cabecera municipal de Ixtaczoquitlán';

-- 8.5 Semáforo de antigüedad del piloto (horas para amarillo / rojo)

INSERT INTO tiempo_objetivo (organizacion_id, estado, horas_amarillo, horas_rojo)
SELECT o.id, v.estado::estado_reporte, v.amarillo, v.rojo
FROM organizacion o,
     (VALUES
       ('recibido',    24,  72),
       ('validado',    24,  72),
       ('asignado',    48, 120),
       ('en_atencion', 120, 240),
       ('reabierto',   48, 120)
     ) AS v (estado, amarillo, rojo)
WHERE o.nombre = 'Ayuntamiento de Ixtaczoquitlán';

-- 8.6 Usuarios institucionales iniciales
-- [HASH]: generar con BCrypt desde el backend; nunca guardar contraseñas en claro.

INSERT INTO usuario (correo, password_hash, nombre, rol, credencial_temporal)
VALUES ('admin@taan.mx', '[HASH]', 'Administración T''AAN', 'administrador', true);

INSERT INTO usuario (correo, password_hash, nombre, rol, organizacion_id, credencial_temporal)
SELECT 'coordinacion@ixtaczoquitlan.gob.mx', '[HASH]', 'Coordinación Ixtaczoquitlán',
       'coordinacion', o.id, true
FROM organizacion o
WHERE o.nombre = 'Ayuntamiento de Ixtaczoquitlán';

COMMIT;

-- ---------------------------------------------------------------------
-- Verificación rápida (opcional, ejecutar a mano):
--   SELECT nombre FROM categoria WHERE categoria_padre_id IS NULL;   -- 4 categorías
--   SELECT count(*) FROM categoria WHERE categoria_padre_id IS NOT NULL; -- 9 subcategorías
--   SELECT count(*) FROM regla_canalizacion;                          -- 13 reglas
--   SELECT a.nombre FROM area a;                                      -- 4 áreas
--   -- Punto dentro de la cabecera (debe regresar la zona):
--   SELECT z.nombre FROM zona_cobertura z
--   WHERE ST_Contains(z.geometria, ST_SetSRID(ST_MakePoint(-97.06, 18.85), 4326));
-- ---------------------------------------------------------------------
