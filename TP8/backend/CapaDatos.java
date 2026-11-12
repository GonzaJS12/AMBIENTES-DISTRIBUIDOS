package TP8.backend;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Separacion de cargas: INSERT al primario (5433), SELECT a la replica (5434).
// Cada pedido abre su conexion. Si el nodo no responde, el backend no se cuelga.
public class CapaDatos {
    public static final String URL_PRIMARIO = "jdbc:postgresql://localhost:5433/catalogo?connectTimeout=3";
    public static final String URL_REPLICA = "jdbc:postgresql://localhost:5434/catalogo?connectTimeout=3";
    private static final String USUARIO = "app";
    private static final String CLAVE = "app";

    public Producto insertar(String nombre, double precio) throws SQLException {
        String sql = "INSERT INTO producto (nombre, precio) VALUES (?, ?) RETURNING id, nombre, precio";
        System.out.println("  Escritura -> primario " + URL_PRIMARIO);
        try (Connection conexion = abrir(URL_PRIMARIO);
                PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setDouble(2, precio);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return leer(rs);
            }
        }
    }

    public List<Producto> listar() throws SQLException {
        String sql = "SELECT id, nombre, precio FROM producto ORDER BY id";
        System.out.println("  Lectura -> replica " + URL_REPLICA);
        List<Producto> productos = new ArrayList<Producto>();
        try (Connection conexion = abrir(URL_REPLICA);
                PreparedStatement ps = conexion.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                productos.add(leer(rs));
            }
        }
        return productos;
    }

    public Producto buscar(int id) throws SQLException {
        String sql = "SELECT id, nombre, precio FROM producto WHERE id = ?";
        System.out.println("  Lectura -> replica " + URL_REPLICA);
        try (Connection conexion = abrir(URL_REPLICA);
                PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return leer(rs);
            }
        }
    }

    public static boolean esNodoCaido(SQLException error) {
        String estado = error.getSQLState();
        if (estado != null && estado.startsWith("08")) {
            return true;
        }
        Throwable causa = error;
        while (causa != null) {
            if (causa instanceof java.net.ConnectException || causa instanceof java.net.SocketTimeoutException) {
                return true;
            }
            String mensaje = causa.getMessage();
            if (mensaje != null) {
                String bajo = mensaje.toLowerCase();
                if (bajo.contains("no suitable driver")) {
                    return false;
                }
                if (bajo.contains("connection refused")
                        || bajo.contains("connection attempt failed")
                        || bajo.contains("the connection attempt failed")) {
                    return true;
                }
            }
            causa = causa.getCause();
        }
        return false;
    }

    private static Connection abrir(String url) throws SQLException {
        return DriverManager.getConnection(url, USUARIO, CLAVE);
    }

    private static Producto leer(ResultSet rs) throws SQLException {
        return new Producto(rs.getInt("id"), rs.getString("nombre"), rs.getDouble("precio"));
    }
}
