package py.fpune.soracare.dto;

public record AutorizacionGoogleCalendarDTO(
        String accessToken,
        String refreshToken,
        String idToken,
        String alcance,
        long expiraEnSegundos
) {
}
