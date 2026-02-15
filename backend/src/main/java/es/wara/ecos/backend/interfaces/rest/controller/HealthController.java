package es.wara.ecos.backend.interfaces.rest.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST para verificar el estado de salud de la aplicación.
 * <p>
 * Proporciona un endpoint simple para monitorear si el servicio está
 * operativo y responde correctamente. Útil para:
 * <ul>
 *   <li>Health checks en contenedores Docker/Kubernetes</li>
 *   <li>Monitoreo de disponibilidad del servicio</li>
 *   <li>Verificación rápida del estado de la aplicación</li>
 * </ul>
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@RestController
@RequestMapping("/")
public class HealthController {

    /**
     * Endpoint de verificación de salud del servicio.
     * <p>
     * Retorna información básica sobre el estado y versión del servicio.
     * Siempre responde con HTTP 200 si la aplicación está operativa.
     * </p>
     *
     * @return Map con información del servicio:
     *         <ul>
     *           <li><b>status</b>: Estado del servicio ("UP")</li>
     *           <li><b>service</b>: Nombre del servicio ("Ecos Backend")</li>
     *           <li><b>version</b>: Versión actual de la aplicación</li>
     *         </ul>
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Ecos Backend");
        response.put("version", "1.0.0");
        return response;
    }
}