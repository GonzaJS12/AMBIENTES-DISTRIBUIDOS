package TP9.cliente;

import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.Scanner;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

// Otro proceso. Confia solo en TP9/cert/servidor.cer. No desactiva la verificacion TLS.
public class ClienteHttps {
    private static final String BASE = "https://localhost:8443";
    private static final String CERT = "TP9/cert/servidor.cer";

    public static void main(String[] args) throws Exception {
        HttpClient http = HttpClient.newBuilder()
                .sslContext(confiarCertificadoLocal())
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Cliente HTTPS -> " + BASE);
            System.out.println("Usuarios: admin/admin123 (ADMIN), cliente/cliente123 (CLIENTE)");
            System.out.println();
            String token = null;
            String rol = null;
            boolean seguir = true;
            while (seguir) {
                mostrarMenu(token != null, rol);
                String opcion = scanner.nextLine().trim();
                try {
                    if ("1".equals(opcion)) {
                        String[] sesion = login(scanner, http);
                        token = sesion[0];
                        rol = sesion[1];
                    } else if ("2".equals(opcion)) {
                        listar(http, token);
                    } else if ("3".equals(opcion)) {
                        alta(scanner, http, token, rol);
                    } else if ("4".equals(opcion)) {
                        listar(http, null);
                    } else if ("5".equals(opcion)) {
                        alterar(http, token);
                    } else if ("0".equals(opcion)) {
                        seguir = false;
                        System.out.println("Cliente cerrado. El servidor sigue escuchando.");
                    } else {
                        System.out.println("Opcion no valida.");
                    }
                } catch (Exception e) {
                    System.err.println("Error de red o de lectura: " + e.getMessage());
                }
                System.out.println();
            }
        }
    }

    private static void mostrarMenu(boolean conToken, String rol) {
        System.out.println("--- Catalogo sobre TLS ---");
        if (rol != null) {
            System.out.println("Sesion: " + rol);
        }
        System.out.println("1) POST /login");
        System.out.println("2) GET /productos          " + (conToken ? "(con el token)" : "(todavia sin token)"));
        System.out.println("3) POST /productos         (solo ADMIN)");
        System.out.println("4) GET /productos sin token (401)");
        System.out.println("5) GET con el payload del JWT alterado (401)");
        System.out.println("0) Salir");
        System.out.print("Opcion: ");
    }

    private static String[] login(Scanner scanner, HttpClient http) throws Exception {
        System.out.print("Usuario: ");
        String usuario = scanner.nextLine().trim();
        System.out.print("Clave: ");
        String clave = scanner.nextLine().trim();
        String json = "{\"usuario\":\"" + escapar(usuario) + "\",\"clave\":\"" + escapar(clave) + "\"}";
        System.out.println("Payload enviado: " + json);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/login"))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> r = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        mostrar(r);
        if (r.statusCode() != 200) {
            return new String[] { null, null };
        }
        String token = valor(r.body(), "token");
        String rol = valor(r.body(), "rol");
        System.out.println("Token guardado (" + token.split("\\.").length + " partes). Rol: " + rol);
        return new String[] { token, rol };
    }

    private static void listar(HttpClient http, String token) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/productos"))
                .timeout(Duration.ofSeconds(8))
                .GET();
        if (token != null) {
            b.header("Authorization", "Bearer " + token);
            System.out.println("Authorization: Bearer <token>");
        } else {
            System.out.println("Sin encabezado Authorization.");
        }
        mostrar(http.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)));
    }

    private static void alta(Scanner scanner, HttpClient http, String token, String rol) throws Exception {
        if (token == null) {
            System.out.println("Primero hay que hacer login (opcion 1).");
            return;
        }
        if (!"ADMIN".equals(rol)) {
            System.out.println("El rol " + rol + " no puede dar de alta. Solo ADMIN.");
            System.out.println("No se piden nombre ni precio.");
            return;
        }
        System.out.print("Nombre: ");
        String nombre = scanner.nextLine().trim();
        System.out.print("Precio: ");
        String precio = scanner.nextLine().trim().replace(',', '.');
        String json = "{\"nombre\":\"" + escapar(nombre) + "\",\"precio\":" + String.format(Locale.US, "%s", precio) + "}";
        System.out.println("Payload enviado: " + json);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/productos"))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        mostrar(http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)));
    }

    private static void alterar(HttpClient http, String token) throws Exception {
        if (token == null) {
            System.out.println("Primero hay que hacer login (opcion 1).");
            return;
        }
        String[] partes = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8);
        String cambiado = payload.contains("\"rol\":\"CLIENTE\"")
                ? payload.replace("\"rol\":\"CLIENTE\"", "\"rol\":\"ADMIN\"")
                : payload.replace("\"rol\":\"ADMIN\"", "\"rol\":\"CLIENTE\"");
        String payloadNuevo = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(cambiado.getBytes(StandardCharsets.UTF_8));
        String falso = partes[0] + "." + payloadNuevo + "." + partes[2];
        System.out.println("Payload original: " + payload);
        System.out.println("Payload alterado: " + cambiado);
        System.out.println("La firma no se recalculo.");
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/productos"))
                .timeout(Duration.ofSeconds(8))
                .header("Authorization", "Bearer " + falso)
                .GET()
                .build();
        mostrar(http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)));
    }

    private static SSLContext confiarCertificadoLocal() throws Exception {
        CertificateFactory fabrica = CertificateFactory.getInstance("X.509");
        Certificate certificado;
        try (FileInputStream in = new FileInputStream(CERT)) {
            certificado = fabrica.generateCertificate(in);
        }
        KeyStore almacen = KeyStore.getInstance(KeyStore.getDefaultType());
        almacen.load(null, null);
        almacen.setCertificateEntry("tp9", certificado);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(almacen);
        SSLContext contexto = SSLContext.getInstance("TLS");
        contexto.init(null, tmf.getTrustManagers(), null);
        return contexto;
    }

    private static void mostrar(HttpResponse<String> r) {
        int codigo = r.statusCode();
        System.out.println("HTTPS " + codigo + " " + textoEstado(codigo));
        System.out.println("JSON recibido: " + r.body());
    }

    private static String textoEstado(int codigo) {
        if (codigo == 200) {
            return "OK";
        }
        if (codigo == 201) {
            return "Created";
        }
        if (codigo == 400) {
            return "Bad Request";
        }
        if (codigo == 401) {
            return "Unauthorized";
        }
        if (codigo == 403) {
            return "Forbidden";
        }
        return "";
    }

    private static String valor(String json, String clave) {
        String marca = "\"" + clave + "\":\"";
        int inicio = json.indexOf(marca);
        if (inicio < 0) {
            throw new IllegalArgumentException("Falta " + clave);
        }
        inicio += marca.length();
        int fin = json.indexOf('"', inicio);
        return json.substring(inicio, fin);
    }

    private static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
