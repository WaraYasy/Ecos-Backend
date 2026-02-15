package es.wara.ecos.backend.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String username;            // Asignado (registro)
    private String email;               // Asignado (registro)
    private String passwordHash;        // Asignado (registro, hasheado)
    private String spotifyUserId;       // Obtenido de Spotify OAuth

    // Perfil musical - CALCULADO por sistema
    private Double avgEnergy;           // Calculado: promedio de canciones escuchadas
    private Double avgValence;          // Calculado: promedio de canciones escuchadas
    private Double avgTempo;            // Calculado: promedio de BPM preferidos
    private Double ecleticismScore;     // Calculado: varianza en gustos musicales

    private LocalDateTime createdAt;    // Auto: timestamp registro
    private LocalDateTime updatedAt;    // Auto: timestamp última modificación

}
