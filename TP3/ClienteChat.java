package TP3;
import java.io.*;
import java.net.*;
import java.util.Scanner;

// Paso 5: un hilo lee el teclado y otro las líneas que llegan (broadcast de los demás).
public class ClienteChat {
    public static void main(String[] args) {
        String host = "localhost";
        int puerto = 5000;

        try (Socket socket = new Socket(host, puerto);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("--- Chat TCP (broadcast) ---");
            System.out.println("Conectado a " + host + ":" + puerto);
            System.out.println("Escriba una línea y Enter. Escriba 'salir' para terminar.");

            Thread receptor = new Thread(() -> {
                try {
                    String llegada;
                    while ((llegada = in.readLine()) != null) {
                        System.out.println("\n" + llegada);
                        System.out.print("> ");
                    }
                } catch (IOException e) {
                    System.err.println("\nConexión cerrada por el servidor.");
                }
            });
            receptor.setDaemon(true);
            receptor.start();

            while (true) {
                System.out.print("> ");
                if (!scanner.hasNextLine()) {
                    break;
                }
                String mensaje = scanner.nextLine();
                if (mensaje.equalsIgnoreCase("salir")) {
                    break;
                }
                out.println(mensaje);
            }

        } catch (IOException e) {
            System.err.println("No se pudo conectar al servidor. Asegúrese que este en ejecución.");
            System.err.println("Detalle del error: " + e.getMessage());
        }
    }
}
