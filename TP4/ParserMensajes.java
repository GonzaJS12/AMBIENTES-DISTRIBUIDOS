package TP4;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

// Marshalling/unmarshalling local: JSON de texto (claves del PDF) y binario con DataOutputStream.
public class ParserMensajes {

    public static String aJson(Transaccion t) {
        return "{\"id\":" + t.getIdTransaccion()
                + ", \"origen\":\"" + t.getOrigen()
                + "\", \"monto\":" + String.format(Locale.US, "%.2f", t.getMonto())
                + ", \"timestamp\":" + t.getTimestamp() + "}";
    }

    public static Transaccion desdeJson(String json) {
        int id = Integer.parseInt(valor(json, "id"));
        String origen = valor(json, "origen");
        double monto = Double.parseDouble(valor(json, "monto"));
        long timestamp = Long.parseLong(valor(json, "timestamp"));
        return new Transaccion(id, origen, monto, timestamp);
    }

    public static void escribirBinario(Transaccion t, DataOutputStream out) throws Exception {
        out.writeInt(t.getIdTransaccion());
        out.writeUTF(t.getOrigen());
        out.writeDouble(t.getMonto());
        out.writeLong(t.getTimestamp());
    }

    public static Transaccion leerBinario(DataInputStream in) throws Exception {
        int id = in.readInt();
        String origen = in.readUTF();
        double monto = in.readDouble();
        long timestamp = in.readLong();
        return new Transaccion(id, origen, monto, timestamp);
    }

    public static byte[] aBinario(Transaccion t) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(buffer);
        escribirBinario(t, out);
        out.flush();
        return buffer.toByteArray();
    }

    public static Transaccion desdeBinario(byte[] datos) throws Exception {
        return leerBinario(new DataInputStream(new ByteArrayInputStream(datos)));
    }

    // Extrae el valor de "clave":... sin librerías JSON.
    private static String valor(String json, String clave) {
        String marca = "\"" + clave + "\":";
        int inicio = json.indexOf(marca);
        if (inicio < 0) {
            throw new IllegalArgumentException("Falta la clave " + clave);
        }
        inicio += marca.length();
        while (inicio < json.length() && json.charAt(inicio) == ' ') {
            inicio++;
        }
        if (inicio < json.length() && json.charAt(inicio) == '"') {
            int fin = json.indexOf('"', inicio + 1);
            return json.substring(inicio + 1, fin);
        }
        int fin = inicio;
        while (fin < json.length()) {
            char c = json.charAt(fin);
            if (c == ',' || c == '}') {
                break;
            }
            fin++;
        }
        return json.substring(inicio, fin).trim();
    }

    public static void main(String[] args) throws Exception {
        Transaccion original = new Transaccion(101, "NodoA", 1500.50, 1700000000L);

        String json = aJson(original);
        byte[] jsonUtf8 = json.getBytes(StandardCharsets.UTF_8);
        Transaccion desdeJson = desdeJson(json);

        byte[] binario = aBinario(original);
        Transaccion desdeBinario = desdeBinario(binario);

        System.out.println("JSON: " + json);
        System.out.println("Tamano JSON (UTF-8): " + jsonUtf8.length + " bytes");
        System.out.println("Tamano binario: " + binario.length + " bytes");
        System.out.println("Round-trip JSON: " + desdeJson);
        System.out.println("Round-trip binario: " + desdeBinario);
    }
}
