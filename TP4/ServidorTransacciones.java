package TP4;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

// Paso 6: un cliente, 1000 JSON (una linea cada una) y despues 1000 binarios (campos DataInputStream).
public class ServidorTransacciones {
    public static final int PUERTO = 5000;
    public static final int CANTIDAD = 1000;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            System.out.println("Servidor a la escucha en el puerto " + PUERTO + "...");
            System.out.println("Esperando " + CANTIDAD + " JSON (linea) y luego " + CANTIDAD + " binarios.");

            try (Socket cliente = serverSocket.accept()) {
                System.out.println("Cliente conectado desde "
                        + cliente.getInetAddress() + ":" + cliente.getPort());
                InputStream bruto = cliente.getInputStream();

                long t0 = System.nanoTime();
                long bytesJson = 0;
                Transaccion ultimaJson = null;
                for (int i = 0; i < CANTIDAD; i++) {
                    String linea = leerLinea(bruto);
                    if (linea == null) {
                        System.err.println("JSON incompleto en el mensaje " + (i + 1));
                        return;
                    }
                    bytesJson += linea.getBytes(StandardCharsets.UTF_8).length;
                    ultimaJson = ParserMensajes.desdeJson(linea);
                }
                long msJson = (System.nanoTime() - t0) / 1_000_000L;

                System.out.println();
                System.out.println("--- JSON (" + CANTIDAD + " transacciones) ---");
                System.out.println("Tamano total de la carga util: " + bytesJson + " bytes");
                System.out.println("Tiempo de recepcion + unmarshalling: " + msJson + " ms");
                System.out.println("Ultima reconstruida: " + ultimaJson);

                DataInputStream inBin = new DataInputStream(bruto);
                t0 = System.nanoTime();
                long bytesBin = 0;
                Transaccion ultimaBin = null;
                for (int i = 0; i < CANTIDAD; i++) {
                    ultimaBin = ParserMensajes.leerBinario(inBin);
                    // int + longitud writeUTF (2) + caracteres + double + long
                    bytesBin += 4 + 2 + ultimaBin.getOrigen().length() + 8 + 8;
                }
                long msBin = (System.nanoTime() - t0) / 1_000_000L;

                System.out.println();
                System.out.println("--- Binario (" + CANTIDAD + " transacciones) ---");
                System.out.println("Tamano total de la carga util: " + bytesBin + " bytes");
                System.out.println("Tiempo de recepcion + unmarshalling: " + msBin + " ms");
                System.out.println("Ultima reconstruida: " + ultimaBin);
            }
        } catch (Exception e) {
            System.err.println("Error en el servidor: " + e.getMessage());
        }
    }

    // Lee hasta '\n' byte a byte: no usa buffer extra que se comeria el binario.
    private static String leerLinea(InputStream in) throws Exception {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\n') {
                break;
            }
            if (b != '\r') {
                buf.write(b);
            }
        }
        if (b == -1 && buf.size() == 0) {
            return null;
        }
        return buf.toString(StandardCharsets.UTF_8.name());
    }
}
