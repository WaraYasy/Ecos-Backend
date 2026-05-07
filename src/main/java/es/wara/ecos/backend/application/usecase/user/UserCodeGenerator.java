package es.wara.ecos.backend.application.usecase.user;

import java.security.SecureRandom;

/**
 * Genera códigos de usuario cortos y legibles para el crossover colaborativo.
 * Formato: {@code ECO-XXXX-XXXX} donde X ∈ alfabeto base32 sin caracteres ambiguos
 * (excluye 0/O, 1/I/L para evitar confusión visual).
 *
 * @author Wara Pacheco
 * @since 1.0.0
 */
public final class UserCodeGenerator {

    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int SEGMENT_LENGTH = 4;
    private static final SecureRandom RANDOM = new SecureRandom();

    private UserCodeGenerator() {}

    /**
     * Genera un código de usuario único en formato {@code ECO-XXXX-XXXX}.
     *
     * @return código generado aleatoriamente listo para mostrar al usuario
     */
    public static String generate() {
        return "ECO-" + segment() + "-" + segment();
    }

    private static String segment() {
        StringBuilder sb = new StringBuilder(SEGMENT_LENGTH);
        for (int i = 0; i < SEGMENT_LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

}