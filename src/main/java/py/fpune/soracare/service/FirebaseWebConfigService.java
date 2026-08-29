package py.fpune.soracare.service;

public class FirebaseWebConfigService {

    private static final String API_KEY = "SORACARE_FIREBASE_API_KEY";
    private static final String AUTH_DOMAIN = "SORACARE_FIREBASE_AUTH_DOMAIN";
    private static final String PROJECT_ID = "SORACARE_FIREBASE_PROJECT_ID";
    private static final String STORAGE_BUCKET = "SORACARE_FIREBASE_STORAGE_BUCKET";
    private static final String MESSAGING_SENDER_ID = "SORACARE_FIREBASE_MESSAGING_SENDER_ID";
    private static final String APP_ID = "SORACARE_FIREBASE_APP_ID";
    private static final String VAPID_KEY = "SORACARE_FIREBASE_VAPID_KEY";

    public boolean estaConfigurado() {
        return !apiKey().isBlank()
                && !authDomain().isBlank()
                && !projectId().isBlank()
                && !messagingSenderId().isBlank()
                && !appId().isBlank()
                && !vapidKey().isBlank();
    }

    public String apiKey() {
        return leer(API_KEY);
    }

    public String authDomain() {
        return leer(AUTH_DOMAIN);
    }

    public String projectId() {
        return leer(PROJECT_ID);
    }

    public String storageBucket() {
        return leer(STORAGE_BUCKET);
    }

    public String messagingSenderId() {
        return leer(MESSAGING_SENDER_ID);
    }

    public String appId() {
        return leer(APP_ID);
    }

    public String vapidKey() {
        return leer(VAPID_KEY);
    }

    private static String leer(String nombre) {
        String valor = System.getenv(nombre);
        return valor == null ? "" : valor.strip();
    }
}
