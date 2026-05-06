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
 * Carga el seed SQL precompilado al arrancar la aplicación con la base de datos vacía.
 *
 * <h3>Cuándo actúa</h3>
 * <p>Solo ejecuta la carga si {@code COUNT(*) = 0} en la tabla {@code songs}. En cualquier
 * otro arranque el bean se activa pero sale inmediatamente sin tocar la BD, por lo que es
 * completamente idempotente.</p>
 *
 * <h3>Mecanismo de carga</h3>
 * <p>Lee {@code /db/seed/seed.sql} del classpath y ejecuta cada bloque
 * {@code COPY … FROM stdin} usando {@link CopyManager} (protocolo binario de PostgreSQL),
 * que es órdenes de magnitud más rápido que INSERTs equivalentes. Toda la carga se envuelve
 * en una única transacción; si falla cualquier bloque se hace rollback completo.</p>
 *
 * <h3>Orden de arranque</h3>
 * <p>{@code @Order(1)} garantiza que este runner se ejecuta antes que {@link DatabaseSeeder}
 * (sin {@code @Order}, orden por defecto). Si {@code SqlSeedLoader} carga datos, el
 * {@code DatabaseSeeder} detecta {@code count ≥ minSongs} y salta el seed vía APIs externas.</p>
 *
 * <h3>Perfil</h3>
 * <p>Excluido del perfil {@code test} con {@code @Profile("!test")} para no interferir
 * con los tests funcionales que gestionan su propio estado de BD.</p>
 *
 * @author Wara Pacheco
 * @since 1.0.0
 * @see DatabaseSeeder
 * @see CopyManager
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

    /**
     * Punto de entrada del runner. Comprueba si la BD está vacía y, en ese caso,
     * lee el fichero de seed del classpath y delega la ejecución en {@link #executeCopyBlocks}.
     *
     * @param args argumentos de arranque de la aplicación (no se usan)
     * @throws Exception si la lectura del recurso o la ejecución del seed falla
     */
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

        String content;
        try (var is = resource.openStream()) {
            content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        executeCopyBlocks(content);

        long elapsed = (System.currentTimeMillis() - start) / 1000;
        Long loaded = jdbc.queryForObject("SELECT COUNT(*) FROM songs", Long.class);
        log.info("[SqlSeed] Seed completado en {}s — {} canciones cargadas", elapsed, loaded);
    }

    /**
     * Parsea el contenido del fichero SQL y ejecuta cada bloque {@code COPY … FROM stdin}
     * usando {@link CopyManager}.
     *
     * <p>El parser es línea a línea: detecta la cabecera {@code COPY}, acumula las filas
     * de datos hasta encontrar el terminador {@code \.} y llama a
     * {@link CopyManager#copyIn(String, java.io.Reader)} con los datos acumulados.
     * Las líneas {@code BEGIN;}, {@code COMMIT;} y las vacías se ignoran — la transacción
     * la gestiona este método directamente.</p>
     *
     * @param content texto completo del fichero seed.sql
     * @throws Exception si algún bloque COPY falla (se hace rollback de toda la transacción)
     */
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