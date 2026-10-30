package com.agentesocial.portalrdp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// Carga el contexto con el perfil dev (login simulado); asi no requiere
// credenciales de Google. Necesita PostgreSQL disponible para Flyway/JPA.
@SpringBootTest
@ActiveProfiles("dev")
class PortalRdpApplicationTests {

	@Test
	void contextLoads() {
	}

}
