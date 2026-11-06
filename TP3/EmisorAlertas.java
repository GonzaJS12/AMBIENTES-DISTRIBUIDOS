package TP3;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

// Paso 6: envía una alerta UDP cada 2 s a localhost:6000. Cortar con Ctrl+C.
public class EmisorAlertas {
    public static void main(String[] args) {
        String host = "localhost";
        int puerto = 6000;
        int intervaloMs = 2000;

        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress destino = InetAddress.getByName(host);
            System.out.println("Emisor de alertas UDP hacia " + host + ":" + puerto);
            System.out.println("Envío cada " + (intervaloMs / 1000) + " segundos. Ctrl+C para detener.");

            int numero = 1;
            while (true) {
                String mensaje = "Alerta de telemetría n° " + numero;
                byte[] datos = mensaje.getBytes();
                DatagramPacket paquete = new DatagramPacket(datos, datos.length, destino, puerto);
                socket.send(paquete);
                System.out.println("Enviado: " + mensaje);
                numero++;
                Thread.sleep(intervaloMs);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Emisor interrumpido.");
        } catch (IOException e) {
            System.err.println("Error en el emisor UDP: " + e.getMessage());
        }
    }
}
