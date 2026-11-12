package TP9.servidor;

import java.util.ArrayList;
import java.util.List;

// Catalogo en memoria de este practico. No usa la base del TP8.
public class Catalogo {
    private final List<Producto> productos = new ArrayList<Producto>();
    private int siguienteId = 1;

    public Catalogo() {
        agregar("Camara", 10000);
        agregar("Mouse", 5000);
    }

    public synchronized List<Producto> listar() {
        return new ArrayList<Producto>(productos);
    }

    public synchronized Producto agregar(String nombre, double precio) {
        Producto creado = new Producto(siguienteId++, nombre, precio);
        productos.add(creado);
        return creado;
    }
}
