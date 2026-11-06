package TP7.servidor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

// API REST: GET lista / GET por id / POST alta. Codigos 200, 201, 400, 404, 405, 500.
public class ServidorHttp {
    public static final int PUERTO = 8080;
    private static final Catalogo catalogo = new Catalogo();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PUERTO), 0);
        server.createContext("/productos", ServidorHttp::manejarProductos);
        server.setExecutor(null);
        server.start();
        System.out.println("Servidor REST en http://localhost:" + PUERTO + "/productos");
        System.out.println("GET lista | GET /productos/{id} | POST JSON {nombre, precio}");
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
        } catch (Exception e) {
            e.printStackTrace();
            responder(intercambio, 500, "{\"error\":\"Error interno del servidor\"}");
        }
    }

    private static void manejarGet(HttpExchange intercambio, String ruta) throws IOException {
        if ("/productos".equals(ruta) || "/productos/".equals(ruta)) {
            responder(intercambio, 200, catalogo.toJsonLista());
            return;
        }
        Integer id = idDeRuta(ruta);
        if (id == null) {
            responder(intercambio, 404, "{\"error\":\"Recurso no encontrado\"}");
            return;
        }
        Producto producto = catalogo.buscar(id);
        if (producto == null) {
            responder(intercambio, 404, "{\"error\":\"Producto no encontrado\",\"id\":" + id + "}");
            return;
        }
        responder(intercambio, 200, producto.toJson());
    }

    private static void manejarPost(HttpExchange intercambio, String ruta) throws IOException {
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
            Producto parcial = Producto.desdeJson(body, 0);
            if (parcial.getNombre() == null || parcial.getNombre().trim().isEmpty()) {
                responder(intercambio, 400, "{\"error\":\"nombre obligatorio\"}");
                return;
            }
            if (parcial.getPrecio() < 0) {
                responder(intercambio, 400, "{\"error\":\"precio no puede ser negativo\"}");
                return;
            }
            Producto creado = catalogo.agregar(parcial.getNombre().trim(), parcial.getPrecio());
            System.out.println("  Alta id=" + creado.getId());
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
