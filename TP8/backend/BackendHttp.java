package TP8.backend;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.TimeZone;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

// Capa de negocio stateless. HTTP/JSON como el TP7; el estado vive en PostgreSQL.
public class BackendHttp {
    public static final int PUERTO = 8080;
    private static final CapaDatos datos = new CapaDatos();

    public static void main(String[] args) throws IOException {
        // Windows manda America/Buenos_Aires. La imagen de Docker solo conoce America/Argentina/Buenos_Aires.
        TimeZone.setDefault(TimeZone.getTimeZone("America/Argentina/Buenos_Aires"));
        HttpServer server = HttpServer.create(new InetSocketAddress(PUERTO), 0);
        server.createContext("/productos", BackendHttp::manejarProductos);
        server.setExecutor(null);
        server.start();
        System.out.println("Backend en http://localhost:" + PUERTO + "/productos");
        System.out.println("POST (escritura) -> primario localhost:5433");
        System.out.println("GET  (lectura)   -> replica  localhost:5434");
        System.out.println("No cerrar esta consola.");
    }

    private static void manejarProductos(HttpExchange intercambio) throws IOException {
        String metodo = intercambio.getRequestMethod();
        String ruta = intercambio.getRequestURI().getPath();
        System.out.println("Peticion " + metodo + " " + ruta);
        try {
            if ("GET".equalsIgnoreCase(metodo)) {
                manejarGet(intercambio, ruta);
            } else if ("POST".equalsIgnoreCase(metodo)) {
                manejarPost(intercambio, ruta);
            } else {
                responder(intercambio, 405, "{\"error\":\"Metodo no permitido\"}");
            }
        } catch (SQLException e) {
            if (CapaDatos.esNodoCaido(e)) {
                boolean escritura = "POST".equalsIgnoreCase(metodo);
                String nodo = escritura ? "primario" : "replica";
                System.out.println("  Nodo " + nodo + " no disponible: " + e.getMessage());
                responder(intercambio, 503, "{\"error\":\"Nodo " + nodo + " no disponible\"}");
            } else {
                e.printStackTrace();
                responder(intercambio, 500, "{\"error\":\"Error de base de datos\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            responder(intercambio, 500, "{\"error\":\"Error interno del servidor\"}");
        }
    }

    private static void manejarGet(HttpExchange intercambio, String ruta) throws IOException, SQLException {
        if ("/productos".equals(ruta) || "/productos/".equals(ruta)) {
            responder(intercambio, 200, Producto.listaAJson(datos.listar()));
            return;
        }
        Integer id = idDeRuta(ruta);
        if (id == null) {
            responder(intercambio, 404, "{\"error\":\"Recurso no encontrado\"}");
            return;
        }
        Producto producto = datos.buscar(id);
        if (producto == null) {
            responder(intercambio, 404, "{\"error\":\"Producto no encontrado\",\"id\":" + id + "}");
            return;
        }
        responder(intercambio, 200, producto.toJson());
    }

    private static void manejarPost(HttpExchange intercambio, String ruta) throws IOException, SQLException {
        if (!"/productos".equals(ruta) && !"/productos/".equals(ruta)) {
            responder(intercambio, 404, "{\"error\":\"Recurso no encontrado\"}");
            return;
        }
        String body = leerBody(intercambio);
        if (body == null || body.trim().isEmpty()) {
            responder(intercambio, 400, "{\"error\":\"Body JSON vacio\"}");
            return;
        }
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
            Producto creado = datos.insertar(parcial.getNombre().trim(), parcial.getPrecio());
            System.out.println("  Alta id=" + creado.getId() + " (el primario replica despues de confirmar)");
            responder(intercambio, 201, creado.toJson());
        } catch (IllegalArgumentException e) {
            responder(intercambio, 400, "{\"error\":\"JSON invalido\",\"detalle\":\""
                    + escapar(e.getMessage()) + "\"}");
        }
    }

    private static Integer idDeRuta(String ruta) {
        String prefijo = "/productos/";
        if (!ruta.startsWith(prefijo)) {
            return null;
        }
        String resto = ruta.substring(prefijo.length());
        if (resto.isEmpty() || resto.contains("/")) {
            return null;
        }
        try {
            return Integer.parseInt(resto);
        } catch (NumberFormatException e) {
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
        System.out.println("  -> HTTP " + codigo);
    }

    private static String escapar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
