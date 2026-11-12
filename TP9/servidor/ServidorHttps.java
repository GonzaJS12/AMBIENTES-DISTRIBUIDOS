package TP9.servidor;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

// HTTPS + JWT. Login es publico. /productos exige Bearer valido.
// GET: cualquier rol autenticado. POST: solo ADMIN.
public class ServidorHttps {
    public static final int PUERTO = 8443;
    private static final String KEYSTORE = "TP9/cert/servidor.p12";
    private static final char[] CLAVE_KEYSTORE = "tp9tp9".toCharArray();
    private static final Catalogo catalogo = new Catalogo();

    public static void main(String[] args) throws Exception {
        HttpsServer server = HttpsServer.create(new InetSocketAddress(PUERTO), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(contextoTls()) {
            @Override
            public void configure(HttpsParameters params) {
                SSLContext contexto = getSSLContext();
                SSLParameters ssl = contexto.getDefaultSSLParameters();
                ssl.setNeedClientAuth(false);
                params.setSSLParameters(ssl);
            }
        });
        server.createContext("/", ServidorHttps::manejar);
        server.setExecutor(null);
        server.start();
        System.out.println("Servidor HTTPS en https://localhost:" + PUERTO + "/");
        System.out.println("POST /login  |  GET /productos (cualquier rol)  |  POST /productos (ADMIN)");
        System.out.println("No hay puerto HTTP en claro. No cerrar esta consola.");
    }

    private static SSLContext contextoTls() throws Exception {
        KeyStore almacen = KeyStore.getInstance("PKCS12");
        try (FileInputStream in = new FileInputStream(KEYSTORE)) {
            almacen.load(in, CLAVE_KEYSTORE);
        }
        KeyManagerFactory fabrica = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        fabrica.init(almacen, CLAVE_KEYSTORE);
        SSLContext contexto = SSLContext.getInstance("TLS");
        contexto.init(fabrica.getKeyManagers(), null, null);
        return contexto;
    }

    private static void manejar(HttpExchange intercambio) throws IOException {
        String ruta = intercambio.getRequestURI().getPath();
        String metodo = intercambio.getRequestMethod();
        System.out.println("Peticion " + metodo + " " + ruta + " (TLS)");
        try {
            if ("/login".equals(ruta) && "POST".equalsIgnoreCase(metodo)) {
                login(intercambio);
                return;
            }
            if ("/productos".equals(ruta) || "/productos/".equals(ruta)) {
                if ("GET".equalsIgnoreCase(metodo)) {
                    listar(intercambio);
                    return;
                }
                if ("POST".equalsIgnoreCase(metodo)) {
                    alta(intercambio);
                    return;
                }
                responder(intercambio, 405, "{\"error\":\"Metodo no permitido\"}");
                return;
            }
            if ("GET".equalsIgnoreCase(metodo) && ("/".equals(ruta) || "/estado".equals(ruta))) {
                responder(intercambio, 200, "{\"canal\":\"tls\",\"puerto\":" + PUERTO + "}");
                return;
            }
            responder(intercambio, 404, "{\"error\":\"Recurso no encontrado\"}");
        } catch (Exception e) {
            e.printStackTrace();
            responder(intercambio, 500, "{\"error\":\"Error interno del servidor\"}");
        }
    }

    private static void login(HttpExchange intercambio) throws IOException {
        String body = leerBody(intercambio);
        try {
            String usuario = Producto.valor(body, "usuario");
            String clave = Producto.valor(body, "clave");
            Usuarios.Usuario encontrado = Usuarios.autenticar(usuario, clave);
            if (encontrado == null) {
                System.out.println("  Login rechazado para " + usuario);
                responder(intercambio, 401, "{\"error\":\"Credenciales invalidas\"}");
                return;
            }
            String token = Jwt.emitir(encontrado.nombre, encontrado.rol);
            System.out.println("  Token emitido sub=" + encontrado.nombre + " rol=" + encontrado.rol);
            responder(intercambio, 200, "{\"token\":\"" + token + "\",\"rol\":\"" + encontrado.rol + "\"}");
        } catch (IllegalArgumentException e) {
            responder(intercambio, 400, "{\"error\":\"JSON invalido\"}");
        }
    }

    private static void listar(HttpExchange intercambio) throws IOException {
        Jwt.Claims claims = exigirToken(intercambio);
        if (claims == null) {
            return;
        }
        System.out.println("  GET autorizado sub=" + claims.sub + " rol=" + claims.rol);
        responder(intercambio, 200, Producto.listaAJson(catalogo.listar()));
    }

    private static void alta(HttpExchange intercambio) throws IOException {
        Jwt.Claims claims = exigirToken(intercambio);
        if (claims == null) {
            return;
        }
        if (!"ADMIN".equals(claims.rol)) {
            System.out.println("  POST prohibido rol=" + claims.rol);
            responder(intercambio, 403, "{\"error\":\"Rol sin permiso\",\"rol\":\"" + claims.rol + "\"}");
            return;
        }
        String body = leerBody(intercambio);
        try {
            Producto parcial = Producto.desdeJson(body);
            if (parcial.getNombre() == null || parcial.getNombre().trim().isEmpty()) {
                responder(intercambio, 400, "{\"error\":\"nombre obligatorio\"}");
                return;
            }
            if (parcial.getPrecio() < 0) {
                responder(intercambio, 400, "{\"error\":\"precio no puede ser negativo\"}");
                return;
            }
            Producto creado = catalogo.agregar(parcial.getNombre().trim(), parcial.getPrecio());
            System.out.println("  POST autorizado sub=" + claims.sub + " id=" + creado.getId());
            responder(intercambio, 201, creado.toJson());
        } catch (IllegalArgumentException e) {
            responder(intercambio, 400, "{\"error\":\"JSON invalido\"}");
        }
    }

    // null si ya respondio 401.
    private static Jwt.Claims exigirToken(HttpExchange intercambio) throws IOException {
        String header = intercambio.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            System.out.println("  401 sin Bearer");
            responder(intercambio, 401, "{\"error\":\"Token ausente o invalido\"}");
            return null;
        }
        String token = header.substring("Bearer ".length()).trim();
        try {
            return Jwt.verificar(token);
        } catch (Jwt.TokenInvalido e) {
            System.out.println("  401 " + e.getMessage());
            responder(intercambio, 401, "{\"error\":\"Token ausente o invalido\"}");
            return null;
        }
    }

    private static String leerBody(HttpExchange intercambio) throws IOException {
        try (InputStream in = intercambio.getRequestBody()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void responder(HttpExchange intercambio, int codigo, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        intercambio.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        intercambio.sendResponseHeaders(codigo, bytes.length);
        try (OutputStream out = intercambio.getResponseBody()) {
            out.write(bytes);
        }
        System.out.println("  -> HTTPS " + codigo);
    }
}
