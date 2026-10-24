package com.gestion.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"gestion.semantica.ruta-tdb=target/jena-test-base",
		"gestion.almacenamiento.ruta-local=target/uploads-test-base" })
class GestionBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
