package TP3;
import java.io.*;
import java.net.*;
import java.util.concurrent.CopyOnWriteArrayList;

// Paso 5: lista compartida de salidas. El hilo principal solo acepta; cada manejador difunde a los demás.
public class ServidorChat {
    private static final CopyOnWriteArrayList<PrintWriter> clientes = new CopyOnWriteArrayList<>();

    public static void agregarCliente(PrintWriter out) {
        clientes.add(out);
        System.out.println("Clientes conectados: " + clientes.size());
    }

    public static void quitarCliente(PrintWriter out) {
        clientes.remove(out);
        System.out.println("Clientes conectados: " + clientes.size());
    }

    public static void difundir(PrintWriter origen, String mensaje) {
        for (PrintWriter out : clientes) {
            if (out != origen) {
                out.println(mensaje);
            }
        }
    }

    public static void main(String[] args) {
        int puerto = 5000;

        try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            System.out.println("Servidor de chat a la escucha en el puerto " + puerto + "...");
            System.out.println("Broadcast activo: un mensaje se reenvía a los demás clientes.");

            while (true) {
                Socket cliente = serverSocket.accept();
                System.out.println("Cliente conectado desde: " + cliente.getInetAddress()
                        + ":" + cliente.getPort());
                Thread hilo = new Thread(new ManejadorCliente(cliente));
                hilo.start();
            }
        } catch (IOException e) {
            System.err.println("Error en el servidor: " + e.getMessage());
        }
    }
}
