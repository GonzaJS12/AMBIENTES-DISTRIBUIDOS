package TP4;

import java.io.DataOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

// Paso 6: rafaga JSON y luego binaria. Mide bytes de carga util y tiempo de serializacion + envio.
public class ClienteTransacciones {
    public static final int PUERTO = 5000;
    public static final int CANTIDAD = 1000;

    public static void main(String[] args) {
        String host = "localhost";

        try (Socket socket = new Socket(host, PUERTO)) {
            System.out.println("Conectado a " + host + ":" + PUERTO);
            OutputStream bruto = socket.getOutputStream();

            long t0 = System.nanoTime();
            long bytesJson = 0;
            for (int i = 1; i <= CANTIDAD; i++) {
                Transaccion t = simulada(i);
                byte[] utf8 = ParserMensajes.aJson(t).getBytes(StandardCharsets.UTF_8);
                bytesJson += utf8.length;
                bruto.write(utf8);
                bruto.write('\n');
            }
            bruto.flush();
            long msJson = (System.nanoTime() - t0) / 1_000_000L;

            System.out.println();
            System.out.println("--- JSON (" + CANTIDAD + " transacciones) ---");
            System.out.println("Tamano total de la carga util: " + bytesJson + " bytes");
            System.out.println("Tiempo de serializacion + envio: " + msJson + " ms");

            DataOutputStream outBin = new DataOutputStream(bruto);
            t0 = System.nanoTime();
            long bytesBin = 0;
            for (int i = 1; i <= CANTIDAD; i++) {
                Transaccion t = simulada(i);
                byte[] bin = ParserMensajes.aBinario(t);
                bytesBin += bin.length;
                outBin.write(bin);
            }
            outBin.flush();
            long msBin = (System.nanoTime() - t0) / 1_000_000L;

            System.out.println();
            System.out.println("--- Binario (" + CANTIDAD + " transacciones) ---");
            System.out.println("Tamano total de la carga util: " + bytesBin + " bytes");
            System.out.println("Tiempo de serializacion + envio: " + msBin + " ms");
        } catch (Exception e) {
            System.err.println("Error en el cliente: " + e.getMessage());
        }
    }

    private static Transaccion simulada(int id) {
        return new Transaccion(id, "NodoA", 1500.50, 1700000000L);
    }
}
