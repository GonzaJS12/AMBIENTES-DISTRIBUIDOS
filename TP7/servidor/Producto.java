package TP7.servidor;

import java.util.Locale;

// Recurso del catalogo. toJson/desdeJson = marshalling JSON a mano (como TP4, ahora sobre HTTP).
public class Producto {
    private final int id;
    private final String nombre;
    private final double precio;

    public Producto(int id, String nombre, double precio) {
        this.id = id;
        this.nombre = nombre;
        this.precio = precio;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public double getPrecio() {
        return precio;
    }

    public String toJson() {
        return "{\"id\":" + id
                + ",\"nombre\":\"" + escapar(nombre)
                + "\",\"precio\":" + String.format(Locale.US, "%.2f", precio) + "}";
    }

    public static Producto desdeJson(String json, int idAsignado) {
        String nombre = valor(json, "nombre");
        double precio = Double.parseDouble(valor(json, "precio"));
        return new Producto(idAsignado, nombre, precio);
    }

    private static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    static String valor(String json, String clave) {
        String marca = "\"" + clave + "\":";
        int inicio = json.indexOf(marca);
        if (inicio < 0) {
            throw new IllegalArgumentException("Falta la clave \"" + clave + "\"");
        }
        inicio += marca.length();
        while (inicio < json.length() && json.charAt(inicio) == ' ') {
            inicio++;
        }
        if (inicio < json.length() && json.charAt(inicio) == '"') {
            int fin = json.indexOf('"', inicio + 1);
            if (fin < 0) {
                throw new IllegalArgumentException("JSON invalido en \"" + clave + "\"");
            }
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
}
