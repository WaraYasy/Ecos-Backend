package es.wara.ecos.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representa un usuario registrado en ECOS con sus preferencias de idioma
 * y datos de acceso.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private UUID id;
    private String username;
    private String email;
    private String language;
    private String passwordHash;
    /** Proveedor de autenticación: {@code "local"} o {@code "spotify"}. */
    private String authProvider;
    /** ID del usuario en el proveedor OAuth externo (null si authProvider es "local"). */
    private String providerId;
    private LocalDateTime createdAt;
    /** Código único de 5 caracteres alfanuméricos visible al usuario (ej. ABCD1), usado para crossover. */
    private String userCode;

}
