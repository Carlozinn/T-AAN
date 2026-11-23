-- Funciones de cada área, capturadas por la coordinación (CU-22 paso 3). Las usa la propuesta
-- de correspondencia categoría → área generada por IA (RF-039).
ALTER TABLE area ADD COLUMN funciones varchar(1000);
COMMENT ON COLUMN area.funciones IS
  'Qué atiende el área, en texto libre; se envía a la IA para proponer la correspondencia (CU-22).';
