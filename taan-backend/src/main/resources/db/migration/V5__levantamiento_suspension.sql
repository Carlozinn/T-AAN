-- Suspensiones levantadas por el administrador (RF-043): quién, cuándo, por qué y qué suspensión
-- se interrumpió. No borra antecedentes: la reincidencia se sigue contando con los señalamientos aceptados.
CREATE TABLE levantamiento_suspension (
  id                          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  ciudadano_id                bigint REFERENCES ciudadano (id) ON DELETE SET NULL,
  levantado_por               bigint NOT NULL REFERENCES usuario (id),
  motivo                      text NOT NULL,
  motivo_suspension_anterior  text NOT NULL,
  suspendida_hasta_anterior   timestamptz,
  creado_en                   timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_levantamiento_ciudadano ON levantamiento_suspension (ciudadano_id);
COMMENT ON TABLE levantamiento_suspension IS
  'Auditoría de suspensiones levantadas por el administrador (RF-043). No borra antecedentes: la reincidencia se sigue contando con los señalamientos aceptados.';
COMMENT ON COLUMN levantamiento_suspension.suspendida_hasta_anterior IS
  'Vigencia de la suspensión interrumpida; NULL = era indefinida.';
