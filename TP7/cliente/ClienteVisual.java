package TP7.cliente;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

// Misma API que ClienteHttp, con ventana en lugar del menu de consola.
public class ClienteVisual extends JFrame {
    private static final String BASE = "http://localhost:8080/productos";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final JTextField campoId = new JTextField(6);
    private final JTextField campoNombre = new JTextField(14);
    private final JTextField campoPrecio = new JTextField(8);
    private final JLabel estado = new JLabel(" ");
    private final JTextArea json = new JTextArea(8, 40);
    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[] { "Id", "Nombre", "Precio" }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final List<JButton> botones = new ArrayList<JButton>();

    public ClienteVisual() {
        super("TP7 - Catalogo REST");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        JLabel titulo = new JLabel("Catalogo de productos");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 16f));
        JLabel sub = new JLabel(BASE + "   |   primero hay que tener el servidor en marcha");
        JPanel cabeza = new JPanel(new BorderLayout());
        cabeza.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        cabeza.add(titulo, BorderLayout.NORTH);
        cabeza.add(sub, BorderLayout.SOUTH);
        add(cabeza, BorderLayout.NORTH);

        JButton listar = boton("Listar");
        JButton buscar = boton("Buscar");
        JButton alta = boton("Dar de alta");
        listar.addActionListener(e -> pedirLista());
        buscar.addActionListener(e -> pedirUno());
        alta.addActionListener(e -> pedirAlta());

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        acciones.setBorder(BorderFactory.createTitledBorder("Peticiones"));
        acciones.add(listar);
        acciones.add(new JLabel("Id:"));
        acciones.add(campoId);
        acciones.add(buscar);
        acciones.add(new JLabel("Nombre:"));
        acciones.add(campoNombre);
        acciones.add(new JLabel("Precio:"));
        acciones.add(campoPrecio);
        acciones.add(alta);

        JTable tabla = new JTable(modelo);
        tabla.setFillsViewportHeight(true);
        JScrollPane scrollTabla = new JScrollPane(tabla);
        scrollTabla.setBorder(BorderFactory.createTitledBorder("Productos"));

        json.setEditable(false);
        json.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollJson = new JScrollPane(json);
        scrollJson.setBorder(BorderFactory.createTitledBorder("Respuesta HTTP"));

        JPanel centro = new JPanel(new BorderLayout(8, 8));
        centro.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        centro.add(acciones, BorderLayout.NORTH);
        centro.add(scrollTabla, BorderLayout.CENTER);
        centro.add(scrollJson, BorderLayout.SOUTH);
        add(centro, BorderLayout.CENTER);

        estado.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        add(estado, BorderLayout.SOUTH);

        setSize(760, 520);
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClienteVisual ventana = new ClienteVisual();
            ventana.setVisible(true);
            ventana.pedirLista();
        });
    }

    private JButton boton(String texto) {
        JButton boton = new JButton(texto);
        botones.add(boton);
        return boton;
    }

    private void pedirLista() {
        ejecutar(() -> enviarGet(BASE));
    }

    private void pedirUno() {
        String id = campoId.getText().trim();
        if (id.isEmpty()) {
            mostrarLocal("Falta el id.");
            return;
        }
        ejecutar(() -> enviarGet(BASE + "/" + id));
    }

    private void pedirAlta() {
        String nombre = campoNombre.getText().trim();
        String precioTexto = campoPrecio.getText().trim().replace(',', '.');
        if (nombre.isEmpty() || precioTexto.isEmpty()) {
            mostrarLocal("Nombre y precio son obligatorios.");
            return;
        }
        String cuerpo = "{\"nombre\":\"" + escapar(nombre)
                + "\",\"precio\":" + String.format(Locale.US, "%s", precioTexto) + "}";
        ejecutar(() -> enviarPost(cuerpo));
    }

    private void ejecutar(Llamada llamada) {
        habilitar(false);
        estado.setText("Consultando el servidor...");
        new SwingWorker<Respuesta, Void>() {
            @Override
            protected Respuesta doInBackground() {
                try {
                    return llamada.ejecutar();
                } catch (Exception e) {
                    Respuesta fallo = new Respuesta();
                    fallo.error = e;
                    return fallo;
                }
            }

            @Override
            protected void done() {
                try {
                    mostrar(get());
                } catch (Exception e) {
                    mostrarLocal("Error: " + e.getMessage());
                }
                habilitar(true);
            }
        }.execute();
    }

    private Respuesta enviarGet(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        HttpResponse<String> r = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return Respuesta.de(r);
    }

    private Respuesta enviarPost(String cuerpo) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> r = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        Respuesta respuesta = Respuesta.de(r);
        respuesta.enviado = cuerpo;
        return respuesta;
    }

    private void mostrar(Respuesta r) {
        if (r.error != null) {
            if (r.error instanceof ConnectException) {
                mostrarLocal("No se pudo conectar. Arranque primero TP7.servidor.ServidorHttp.");
            } else {
                mostrarLocal("Error de red o de lectura: " + r.error.getMessage());
            }
            return;
        }
        StringBuilder texto = new StringBuilder();
        texto.append("HTTP ").append(r.codigo).append(" ").append(textoEstado(r.codigo)).append("\n");
        if (r.enviado != null) {
            texto.append("JSON enviado: ").append(r.enviado).append("\n");
        }
        texto.append("JSON recibido:\n").append(r.cuerpo);
        json.setText(texto.toString());
        json.setCaretPosition(0);
        if (r.codigo >= 400) {
            estado.setText("El servidor rechazo la peticion. El cliente sigue abierto.");
            return;
        }
        estado.setText("Listo.");
        if (r.codigo == 201) {
            agregarFila(r.cuerpo);
            campoNombre.setText("");
            campoPrecio.setText("");
        } else {
            llenarTabla(r.cuerpo);
        }
    }

    private void mostrarLocal(String mensaje) {
        estado.setText(mensaje);
        json.setText(mensaje);
    }

    private void llenarTabla(String cuerpo) {
        String limpio = cuerpo == null ? "" : cuerpo.trim();
        if (limpio.startsWith("[")) {
            modelo.setRowCount(0);
            int desde = 0;
            while (true) {
                int abre = limpio.indexOf('{', desde);
                if (abre < 0) {
                    break;
                }
                int cierra = limpio.indexOf('}', abre);
                if (cierra < 0) {
                    break;
                }
                agregarFila(limpio.substring(abre, cierra + 1));
                desde = cierra + 1;
            }
        } else if (limpio.startsWith("{") && limpio.contains("\"nombre\"")) {
            modelo.setRowCount(0);
            agregarFila(limpio);
        }
    }

    private void agregarFila(String objeto) {
        modelo.addRow(new Object[] {
                valor(objeto, "id"),
                valor(objeto, "nombre"),
                valor(objeto, "precio")
        });
    }

    private void habilitar(boolean activo) {
        for (JButton boton : botones) {
            boton.setEnabled(activo);
        }
    }

    private static String textoEstado(int codigo) {
        if (codigo == 200) {
            return "OK";
        }
        if (codigo == 201) {
            return "Created";
        }
        if (codigo == 400) {
            return "Bad Request";
        }
        if (codigo == 404) {
            return "Not Found";
        }
        if (codigo == 500) {
            return "Internal Server Error";
        }
        return "";
    }

    private static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String valor(String json, String clave) {
        String marca = "\"" + clave + "\":";
        int inicio = json.indexOf(marca);
        if (inicio < 0) {
            return "";
        }
        inicio += marca.length();
        while (inicio < json.length() && json.charAt(inicio) == ' ') {
            inicio++;
        }
        if (inicio < json.length() && json.charAt(inicio) == '"') {
            int fin = json.indexOf('"', inicio + 1);
            if (fin < 0) {
                return "";
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

    private interface Llamada {
        Respuesta ejecutar() throws Exception;
    }

    private static class Respuesta {
        int codigo;
        String cuerpo = "";
        String enviado;
        Exception error;

        static Respuesta de(HttpResponse<String> r) {
            Respuesta respuesta = new Respuesta();
            respuesta.codigo = r.statusCode();
            respuesta.cuerpo = r.body();
            return respuesta;
        }
    }
}
