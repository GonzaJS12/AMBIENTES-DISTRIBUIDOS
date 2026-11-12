package TP9.servidor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

// Identidades del practico. La clave se compara contra SHA-256, no contra texto plano.
public class Usuarios {
    public static final class Usuario {
        public final String nombre;
        public final String rol;

        Usuario(String nombre, String rol) {
            this.nombre = nombre;
            this.rol = rol;
        }
    }

    private static final Map<String, String> HASHES = new HashMap<String, String>();
    private static final Map<String, String> ROLES = new HashMap<String, String>();

    static {
        // admin / admin123 , cliente / cliente123
        registrar("admin", "240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9", "ADMIN");
        registrar("cliente", "09a31a7001e261ab1e056182a71d3cf57f582ca9a29cff5eb83be0f0549730a9", "CLIENTE");
    }

    public static Usuario autenticar(String nombre, String clave) {
        if (nombre == null || clave == null) {
            return null;
        }
        String esperado = HASHES.get(nombre);
        if (esperado == null) {
            return null;
        }
        String recibido = sha256(clave);
        if (!MessageDigest.isEqual(esperado.getBytes(StandardCharsets.US_ASCII),
                recibido.getBytes(StandardCharsets.US_ASCII))) {
            return null;
        }
        return new Usuario(nombre, ROLES.get(nombre));
    }

    private static void registrar(String nombre, String hash, String rol) {
        HASHES.put(nombre, hash);
        ROLES.put(nombre, rol);
    }

    private static String sha256(String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : dig) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
