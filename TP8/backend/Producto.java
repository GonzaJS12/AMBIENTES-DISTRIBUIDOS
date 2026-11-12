package TP8.backend;

import java.util.Locale;

// Misma forma que el catalogo del TP7. El id lo asigna PostgreSQL (SERIAL), no el backend.
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

    public static Producto desdeJson(String json) {
        String nombre = valor(json, "nombre");
        double precio = Double.parseDouble(valor(json, "precio"));
        return new Producto(0, nombre, precio);
    }

    public static String listaAJson(java.util.List<Producto> productos) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < productos.size(); i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append(productos.get(i).toJson());
        }
        json.append("]");
        return json.toString();
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
