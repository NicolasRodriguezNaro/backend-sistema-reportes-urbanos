package com.gestion.backend.dto;

import java.util.List;
import java.util.Map;

public record FeatureCollection(String type, List<Feature> features) {

	public static FeatureCollection de(List<Feature> features) {
		return new FeatureCollection("FeatureCollection", features);
	}

	public record Feature(String type, Geometry geometry, Map<String, Object> properties) {

		public static Feature punto(double longitud, double latitud, Map<String, Object> propiedades) {
			return new Feature("Feature", new Geometry("Point", new double[] { longitud, latitud }), propiedades);
		}
	}

	public record Geometry(String type, double[] coordinates) {
	}
}
