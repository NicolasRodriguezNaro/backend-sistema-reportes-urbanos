package com.gestion.backend.semantica;

import java.util.Optional;
import java.util.Set;

public record InferenciasGrafo(Set<String> prioridades, Optional<Integer> entidadId) {
}
