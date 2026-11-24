-- Límite preventivo de registros, reenvíos de verificación y recuperaciones de contraseña por
-- origen (RNF-022). Solo guarda el origen y la hora: nunca el correo solicitado.
CREATE TABLE solicitud_cuenta (
  id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  origen_ip  inet NOT NULL,
  tipo       varchar(30) NOT NULL CHECK (tipo IN ('registro', 'reenvio_verificacion', 'recuperacion')),
  creado_en  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_solicitud_cuenta_origen ON solicitud_cuenta (origen_ip, tipo, creado_en);
COMMENT ON TABLE solicitud_cuenta IS
  'Límite de registros, reenvíos de verificación y recuperaciones por origen (RNF-022). No almacena correos.';
