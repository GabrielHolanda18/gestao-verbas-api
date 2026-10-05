package br.com.gabriel.verbas.shared.erro;

import java.time.Instant;
import java.util.List;

public record ApiError(int status, String code, String message, List<String> errors, Instant timestamp) {

    public static ApiError of(int status, String code, String message, List<String> errors) {
        return new ApiError(status, code, message, errors == null ? List.of() : errors, Instant.now());
    }
}
