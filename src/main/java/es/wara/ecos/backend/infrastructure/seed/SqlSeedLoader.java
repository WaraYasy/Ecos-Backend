package es.wara.ecos.backend.infrastructure.seed;

import lombok.extern.slf4j.Slf4j;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;

/**
 * Carga el seed SQL precompilado (2 000 canciones) cuando la base de datos está vacía.
 *
 * <p>Usa el protocolo COPY de PostgreSQL vía {@link CopyManager}, que es órdenes de magnitud
 * más rápido que INSERT masivos. Corre con {@code @Order(1)}, antes que {@link DatabaseSeeder},
 * para que el seeder vía APIs externas salte el seed inicial si la BD ya tiene datos.</p>
 *
 * <p>Solo activo fuera del perfil {@code test}.</p>
 */
@Slf4j
@Component
@Order(1)
@Profile("!test")
public class SqlSeedLoader implements ApplicationRunner {

    private static final String SEED_RESOURCE = "/db/seed/seed.sql";

    private final DataSource dataSource;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    public SqlSeedLoader(DataSource dataSource,
                         org.springframework.jdbc.core.JdbcTemplate jdbc) {
        this.dataSource = dataSource;
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM songs", Long.class);
        if (count != null && count > 0) {
            log.info("[SqlSeed] BD ya tiene {} canciones — omitiendo seed SQL", count);
            return;
        }

        URL resource = getClass().getResource(SEED_RESOURCE);
        if (resource == null) {
            log.warn("[SqlSeed] No se encontró {} en el classpath — omitiendo", SEED_RESOURCE);
            return;
        }

        log.info("[SqlSeed] BD vacía — cargando seed SQL (puede tardar unos segundos)...");
        long start = System.currentTimeMillis();

        String content = new String(resource.openStream().readAllBytes(), StandardCharsets.UTF_8);
        executeCopyBlocks(content);

        long elapsed = (System.currentTimeMillis() - start) / 1000;
        Long loaded = jdbc.queryForObject("SELECT COUNT(*) FROM songs", Long.class);
        log.info("[SqlSeed] Seed completado en {}s — {} canciones cargadas", elapsed, loaded);
    }

    private void executeCopyBlocks(String content) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                CopyManager copyManager = conn.unwrap(PGConnection.class).getCopyAPI();

                String copySql = null;
                StringBuilder data = new StringBuilder();

                for (String line : content.split("\\r?\\n")) {
                    if (line.startsWith("COPY ")) {
                        // Strip trailing semicolon — CopyManager no lo espera
                        copySql = line.endsWith(";") ? line.substring(0, line.length() - 1) : line;
                        data = new StringBuilder();
                    } else if ("\\.".equals(line.trim())) {
                        if (copySql != null) {
                            copyManager.copyIn(copySql, new StringReader(data.toString()));
                            log.debug("[SqlSeed] → {}", copySql.split(" ")[2]);
                        }
                        copySql = null;
                    } else if (copySql != null) {
                        data.append(line).append("\n");
                    }
                    // Ignora BEGIN; COMMIT; y líneas vacías fuera de bloques COPY
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }
}