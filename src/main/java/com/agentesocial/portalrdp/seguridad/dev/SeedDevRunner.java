package com.agentesocial.portalrdp.seguridad.dev;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

/**
 * Carga los datos semilla de desarrollo al arrancar (perfil "dev").
 * No usa Flyway a proposito: asi el historial de migraciones es identico en
 * todos los entornos y el script (idempotente) se puede re-ejecutar.
 */
@Component
@Profile("dev")
public class SeedDevRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDevRunner.class);
    private static final String SCRIPT = "db/dev-seed/datos_semilla.sql";

    private final DataSource dataSource;

    public SeedDevRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        EncodedResource recurso = new EncodedResource(new ClassPathResource(SCRIPT), StandardCharsets.UTF_8);
        try (Connection conexion = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(conexion, recurso);
        }
        log.info("Datos semilla de desarrollo cargados ({})", SCRIPT);
    }
}
