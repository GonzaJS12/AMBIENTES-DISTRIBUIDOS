package TP3;
import java.io.*;
import java.net.*;

// Paso 5: lee a este cliente, reenvía a los otros de la lista y, al desconectar, se saca solo a sí mismo.
public class ManejadorCliente implements Runnable {
    private final Socket socket;

    public ManejadorCliente(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        String quien = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        PrintWriter out = null;
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            ServidorChat.agregarCliente(out);
            System.out.println("[Hilo " + quien + "] En el chat.");

            String linea;
            while ((linea = in.readLine()) != null) {
                System.out.println("[Hilo " + quien + "] Dice: " + linea);
                ServidorChat.difundir(out, "[" + quien + "] " + linea);
            }
        } catch (IOException e) {
            System.err.println("[Hilo " + quien + "] Error: " + e.getMessage());
        } finally {
            if (out != null) {
                ServidorChat.quitarCliente(out);
            }
            try {
                socket.close();
            } catch (IOException ignored) {
            }
            System.out.println("[Hilo " + quien + "] Se fue. El servidor sigue aceptando.");
        }
    }
}
