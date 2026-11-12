package TP9.servidor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

// JWT compacto (RFC 7519): header.payload.signature, firmado con HMAC-SHA256.
// El payload no va cifrado: se puede leer. La firma impide cambiar sub o rol.
public class Jwt {
    private static final String SECRETO = "tp9-secreto-hmac-daad";
    private static final long VIGENCIA_SEGUNDOS = 3600;

    public static final class Claims {
        public final String sub;
        public final String rol;
        public final long exp;

        Claims(String sub, String rol, long exp) {
            this.sub = sub;
            this.rol = rol;
            this.exp = exp;
        }
    }

    public static String emitir(String sub, String rol) {
        long exp = (System.currentTimeMillis() / 1000L) + VIGENCIA_SEGUNDOS;
        return emitirHasta(sub, rol, exp);
    }

    static String emitirHasta(String sub, String rol, long exp) {
        String header = b64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = b64("{\"sub\":\"" + escapar(sub) + "\",\"rol\":\"" + escapar(rol) + "\",\"exp\":" + exp + "}");
        String firma = firmar(header + "." + payload);
        return header + "." + payload + "." + firma;
    }

    public static Claims verificar(String token) throws TokenInvalido {
        if (token == null) {
            throw new TokenInvalido("ausente");
        }
        String[] partes = token.split("\\.");
        if (partes.length != 3) {
            throw new TokenInvalido("mal formado");
        }
        String esperada = firmar(partes[0] + "." + partes[1]);
        if (!MessageDigest.isEqual(esperada.getBytes(StandardCharsets.US_ASCII),
                partes[2].getBytes(StandardCharsets.US_ASCII))) {
            throw new TokenInvalido("firma invalida");
        }
        String json = new String(b64d(partes[1]), StandardCharsets.UTF_8);
        String sub = texto(json, "sub");
        String rol = texto(json, "rol");
        long exp = Long.parseLong(texto(json, "exp"));
        long ahora = System.currentTimeMillis() / 1000L;
        if (exp <= ahora) {
            throw new TokenInvalido("vencido");
        }
        return new Claims(sub, rol, exp);
    }

    private static String firmar(String contenido) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRETO.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return b64(mac.doFinal(contenido.getBytes(StandardCharsets.US_ASCII)));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo firmar el JWT", e);
        }
    }

    private static String b64(String texto) {
        return b64(texto.getBytes(StandardCharsets.UTF_8));
    }

    private static String b64(byte[] datos) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(datos);
    }

    private static byte[] b64d(String texto) {
        return Base64.getUrlDecoder().decode(texto);
    }

    private static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String texto(String json, String clave) throws TokenInvalido {
        String marca = "\"" + clave + "\":";
        int inicio = json.indexOf(marca);
        if (inicio < 0) {
            throw new TokenInvalido("payload sin " + clave);
        }
        inicio += marca.length();
        while (inicio < json.length() && json.charAt(inicio) == ' ') {
            inicio++;
        }
        if (inicio < json.length() && json.charAt(inicio) == '"') {
            int fin = json.indexOf('"', inicio + 1);
            if (fin < 0) {
                throw new TokenInvalido("payload invalido");
            }
            return json.substring(inicio + 1, fin);
        }
        int fin = inicio;
        while (fin < json.length()) {
            char c = json.charAt(fin);
            if (c == ',' || c == '}') {
                break;
            }
            fin++;
        }
        return json.substring(inicio, fin).trim();
    }

    public static class TokenInvalido extends Exception {
        public TokenInvalido(String motivo) {
            super(motivo);
        }
    }
}
