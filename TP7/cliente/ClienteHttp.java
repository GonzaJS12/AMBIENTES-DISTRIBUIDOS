package TP7.cliente;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Scanner;

// Consumidor REST: otro proceso. HttpClient + JSON; no usa Socket ni clases del servidor.
public class ClienteHttp {
    private static final String BASE = "http://localhost:8080/productos";

    public static void main(String[] args) {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Cliente REST -> " + BASE);
            System.out.println("El servidor tiene que estar en marcha.");
            System.out.println();

            boolean seguir = true;
            while (seguir) {
                mostrarMenu();
                String opcion = scanner.nextLine().trim();
                try {
                    if ("1".equals(opcion)) {
                        listar(http);
                    } else if ("2".equals(opcion)) {
                        buscar(scanner, http);
                    } else if ("3".equals(opcion)) {
                        alta(scanner, http);
                    } else if ("0".equals(opcion)) {
                        seguir = false;
                        System.out.println("Cliente cerrado. El servidor sigue escuchando.");
                    } else {
                        System.out.println("Opcion no valida.");
                    }
                } catch (ConnectException e) {
                    System.err.println("No se pudo conectar. Arranque primero TP7.servidor.ServidorHttp.");
                } catch (Exception e) {
                    System.err.println("Error de red o de lectura: " + e.getMessage());
                }
                System.out.println();
            }
        }
    }

    private static void mostrarMenu() {
        System.out.println("--- Catalogo remoto ---");
        System.out.println("1) GET lista /productos");
        System.out.println("2) GET producto por id");
        System.out.println("3) POST alta de producto");
        System.out.println("0) Salir");
        System.out.print("Opcion: ");
    }

    private static void listar(HttpClient http) throws Exception {
        HttpResponse<String> r = get(http, BASE);
        mostrar(r);
    }

    private static void buscar(Scanner scanner, HttpClient http) throws Exception {
        System.out.print("Id: ");
        String id = scanner.nextLine().trim();
        HttpResponse<String> r = get(http, BASE + "/" + id);
        mostrar(r);
    }

    private static void alta(Scanner scanner, HttpClient http) throws Exception {
        System.out.print("Nombre: ");
        String nombre = scanner.nextLine().trim();
        System.out.print("Precio: ");
        String precioTexto = scanner.nextLine().trim().replace(',', '.');
        String json = "{\"nombre\":\"" + escapar(nombre)
                + "\",\"precio\":" + String.format(Locale.US, "%s", precioTexto) + "}";
        System.out.println("Payload enviado: " + json);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> r = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        mostrar(r);
    }

    private static HttpResponse<String> get(HttpClient http, String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static void mostrar(HttpResponse<String> r) {
        int codigo = r.statusCode();
        System.out.println("HTTP " + codigo + " " + textoEstado(codigo));
        System.out.println("JSON recibido: " + r.body());
        if (codigo >= 400) {
            System.out.println("(el servidor rechazo la peticion; el cliente no se cayo)");
        }
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
        if (codigo == 404) {
            return "Not Found";
        }
        if (codigo == 500) {
            return "Internal Server Error";
        }
        return "";
    }

    private static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
