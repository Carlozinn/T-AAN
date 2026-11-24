-- Índices para las estadísticas e informes (CU-10, RF-026): cada consulta filtra por organización
-- y periodo, y busca la primera asignación y las redirecciones en el historial.
CREATE INDEX ix_reporte_org_creado ON reporte (organizacion_id, creado_en);
CREATE INDEX ix_historial_asignacion ON historial_reporte (reporte_id, creado_en) WHERE estado_nuevo = 'asignado';
CREATE INDEX ix_historial_redireccion ON historial_reporte (organizacion_anterior_id) WHERE accion = 'redireccion';
