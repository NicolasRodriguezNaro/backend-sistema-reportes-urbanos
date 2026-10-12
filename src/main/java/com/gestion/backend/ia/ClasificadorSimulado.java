package com.gestion.backend.ia;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.gestion.backend.modelo.Severidad;
import com.gestion.backend.modelo.TipoIncidencia;

@Service
@Profile("dev")
public class ClasificadorSimulado implements ClasificadorIncidencias {

	@Override
	public ResultadoClasificacion clasificar(String urlImagen) {
		int hash = urlImagen.hashCode();

		TipoIncidencia categoria = TipoIncidencia.values()[Math.floorMod(hash, TipoIncidencia.values().length)];
		Severidad severidad = Severidad.values()[Math.floorMod(hash / 7, Severidad.values().length)];
		double confianza = Math.round((0.55 + Math.floorMod(hash, 45) / 100.0) * 100.0) / 100.0;

		String borrador = "Borrador generado por vision artificial (simulado): posible "
				+ categoria.getDescripcion().toLowerCase() + ".";

		return new ResultadoClasificacion(categoria, severidad, confianza, borrador);
	}
}
