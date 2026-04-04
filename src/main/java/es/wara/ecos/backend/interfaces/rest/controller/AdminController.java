package es.wara.ecos.backend.interfaces.rest.controller;

import es.wara.ecos.backend.infrastructure.seed.CsvImportRunner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Endpoints de administración protegidos por {@code X-Admin-Key}.
 *
 * <p>La autenticación la gestiona {@link es.wara.ecos.backend.infrastructure.security.AdminApiKeyFilter}
 * antes de que la petición llegue aquí. No requiere JWT.</p>
 *
 * <h3>Endpoints disponibles:</h3>
 * <ul>
 *   <li>{@code POST /v1/admin/songs/import} — importa canciones desde un CSV subido</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/v1/admin")
public class AdminController {

    private final CsvImportRunner csvImportRunner;

    public AdminController(CsvImportRunner csvImportRunner) {
        this.csvImportRunner = csvImportRunner;
    }

    /**
     * Importa canciones desde un CSV subido como multipart.
     *
     * <p>El CSV debe tener el mismo formato que {@code spotify_songs_clean.csv}
     * (o el enriquecido {@code spotify_songs_enriched.csv}).</p>
     *
     * <p>La importación corre en el hilo de la petición — para CSVs grandes
     * la conexión permanece abierta hasta que termina. Usa {@code limit} para
     * importaciones parciales de prueba.</p>
     *
     * @param file  archivo CSV (multipart/form-data, campo {@code file})
     * @param limit máximo de filas a importar; {@code 0} = sin límite (default)
     * @return resumen {@code { imported, skipped, errors }}
     */
    @PostMapping(value = "/songs/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> importCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "limit", defaultValue = "0") int limit) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "El archivo CSV está vacío"));
        }

        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.csv";
        log.info("[Admin] Importación CSV iniciada: '{}' ({} bytes), limit={}",
                filename, file.getSize(), limit);

        try {
            InputStreamReader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
            int[] result = csvImportRunner.importFromReader(reader, limit, filename);

            return ResponseEntity.ok(Map.of(
                "imported", result[0],
                "skipped",  result[1],
                "errors",   result[2],
                "source",   filename
            ));
        } catch (Exception e) {
            log.error("[Admin] Error al importar CSV '{}': {}", filename, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Error al procesar el CSV: " + e.getMessage()));
        }
    }
}
