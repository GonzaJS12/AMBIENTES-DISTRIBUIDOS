package TP7.servidor;

import java.util.ArrayList;
import java.util.List;

// Estado del servidor: lista en memoria. Cada request HTTP trae todo lo que necesita (stateless a nivel sesion).
public class Catalogo {
    private final List<Producto> productos = new ArrayList<Producto>();
    private int siguienteId = 1;

    public Catalogo() {
        agregar("Teclado", 15000.00);
        agregar("Mouse", 8000.50);
        agregar("Monitor", 120000.00);
    }

    public synchronized List<Producto> listar() {
        return new ArrayList<Producto>(productos);
    }

    public synchronized Producto buscar(int id) {
        for (Producto producto : productos) {
            if (producto.getId() == id) {
                return producto;
            }
        }
        return null;
    }

    public synchronized Producto agregar(String nombre, double precio) {
        Producto creado = new Producto(siguienteId++, nombre, precio);
        productos.add(creado);
        return creado;
    }

    public String toJsonLista() {
        StringBuilder json = new StringBuilder("[");
        List<Producto> copia = listar();
        for (int i = 0; i < copia.size(); i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append(copia.get(i).toJson());
        }
        json.append("]");
        return json.toString();
    }
}
