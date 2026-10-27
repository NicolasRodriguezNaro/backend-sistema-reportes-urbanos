-- ============================================================================
-- V3 - Indices para consultas del dashboard
-- ============================================================================

CREATE INDEX idx_reportes_fecha_creacion ON reportes (fecha_creacion DESC);
CREATE INDEX idx_reportes_categoria_ia ON reportes (categoria_ia);
CREATE INDEX idx_reportes_prioridad_calculada ON reportes (prioridad_calculada);
