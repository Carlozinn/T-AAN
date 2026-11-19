-- =====================================================================
-- T'AAN - Incremento 3: secuencia para el folio de los reportes (RF-011)
-- El folio se arma en el backend como TAAN-<año>-<número de 6 dígitos>;
-- la secuencia garantiza que el número no se repita ni con envíos simultáneos.
-- =====================================================================

CREATE SEQUENCE IF NOT EXISTS folio_reporte_seq START WITH 1 INCREMENT BY 1;
