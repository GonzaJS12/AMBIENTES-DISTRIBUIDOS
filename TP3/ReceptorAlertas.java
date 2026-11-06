package TP3;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;

// Paso 6: espera datagramas UDP. Si pasan 5 s sin nada, avisa y sigue escuchando (no termina).
public class ReceptorAlertas {
    public static void main(String[] args) {
        int puerto = 6000;
        byte[] buffer = new byte[1024];

        try (DatagramSocket socket = new DatagramSocket(puerto)) {
            socket.setSoTimeout(5000);
            System.out.println("Receptor de alertas UDP en el puerto " + puerto + "...");
            System.out.println("Timeout de espera: 5 segundos. Ctrl+C para salir.");

            while (true) {
                try {
                    DatagramPacket paquete = new DatagramPacket(buffer, buffer.length);
                    socket.receive(paquete);
                    String texto = new String(paquete.getData(), 0, paquete.getLength());
                    System.out.println("Alerta recibida de " + paquete.getAddress()
                            + ":" + paquete.getPort() + " -> " + texto);
                    System.out.flush();
                } catch (SocketTimeoutException e) {
                    System.out.println("ADVERTENCIA: no llegó ningún datagrama en 5 segundos. Sigo escuchando...");
                    System.out.flush();
                }
            }
        } catch (IOException e) {
            System.err.println("Error en el receptor UDP: " + e.getMessage());
        }
    }
}
