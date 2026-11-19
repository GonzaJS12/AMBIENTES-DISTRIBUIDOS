package TP10.cliente;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;

// Ventana para ver el catalogo y las metricas. La carga la sigue haciendo Locust.
public class MonitorVisual extends JFrame {
    private static final String BASE = "http://localhost:8090";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final JLabel estado = new JLabel(" ");
    private final JLabel hilos = dato("Hilos del pool");
    private final JLabel espera = dato("Espera por pedido");
    private final JLabel enCurso = dato("Pedidos en curso");
    private final JLabel pedidos = dato("Pedidos");
    private final JLabel ok = dato("OK");
    private final JLabel fallos = dato("Fallos");
    private final JLabel minimo = dato("Latencia minima");
    private final JLabel promedio = dato("Latencia promedio");
    private final JLabel maximo = dato("Latencia maxima");
    private final JTextArea json = new JTextArea(7, 40);
    private final JCheckBox seguir = new JCheckBox("Actualizar metricas cada 1 s", true);
    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[] { "Id", "Nombre", "Precio" }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private boolean consultandoMetricas;

    public MonitorVisual() {
        super("TP10 - Monitoreo de carga");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        JLabel titulo = new JLabel("Servicio de catalogo bajo medicion");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 16f));
        JLabel sub = new JLabel(BASE + "   |   Locust genera la carga; esta ventana solo observa");
        JPanel cabeza = new JPanel(new BorderLayout());
        cabeza.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        cabeza.add(titulo, BorderLayout.NORTH);
        cabeza.add(sub, BorderLayout.SOUTH);
        add(cabeza, BorderLayout.NORTH);

        JButton listar = new JButton("Consultar productos");
        JButton metricas = new JButton("Actualizar metricas");
        listar.addActionListener(e -> pedirProductos());
        metricas.addActionListener(e -> pedirMetricas());

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        acciones.setBorder(BorderFactory.createTitledBorder("Consultas"));
        acciones.add(listar);
        acciones.add(metricas);
        acciones.add(seguir);

        JPanel numeros = new JPanel(new GridLayout(3, 3, 12, 6));
        numeros.setBorder(BorderFactory.createTitledBorder("GET /metricas"));
        numeros.add(hilos);
        numeros.add(espera);
        numeros.add(enCurso);
        numeros.add(pedidos);
        numeros.add(ok);
        numeros.add(fallos);
        numeros.add(minimo);
        numeros.add(promedio);
        numeros.add(maximo);

        JTable tabla = new JTable(modelo);
        tabla.setFillsViewportHeight(true);
        JScrollPane scrollTabla = new JScrollPane(tabla);
        scrollTabla.setBorder(BorderFactory.createTitledBorder("GET /productos"));
        scrollTabla.setPreferredSize(new java.awt.Dimension(720, 120));

        json.setEditable(false);
        json.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollJson = new JScrollPane(json);
        scrollJson.setBorder(BorderFactory.createTitledBorder("Ultima respuesta"));

        JPanel centro = new JPanel(new BorderLayout(8, 8));
        centro.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        centro.add(acciones, BorderLayout.NORTH);
        JPanel medio = new JPanel(new BorderLayout(8, 8));
        medio.add(numeros, BorderLayout.NORTH);
        medio.add(scrollTabla, BorderLayout.CENTER);
        centro.add(medio, BorderLayout.CENTER);
        centro.add(scrollJson, BorderLayout.SOUTH);
        add(centro, BorderLayout.CENTER);

        estado.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));
        add(estado, BorderLayout.SOUTH);

        Timer timer = new Timer(1000, e -> {
            if (seguir.isSelected()) {
                pedirMetricas();
            }
        });
        timer.start();

        setSize(760, 560);
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MonitorVisual ventana = new MonitorVisual();
            ventana.setVisible(true);
            ventana.pedirProductos();
            ventana.pedirMetricas();
        });
    }

    private static JLabel dato(String nombre) {
        JLabel etiqueta = new JLabel(nombre + ": -");
        return etiqueta;
    }

    private void pedirProductos() {
        ejecutar(BASE + "/productos", false);
    }

    private void pedirMetricas() {
        if (consultandoMetricas) {
            return;
        }
        consultandoMetricas = true;
        ejecutar(BASE + "/metricas", true);
    }

    private void ejecutar(String url, boolean esMetrica) {
        estado.setText("Consultando " + url + " ...");
        new SwingWorker<Respuesta, Void>() {
            @Override
            protected Respuesta doInBackground() {
                try {
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .timeout(Duration.ofSeconds(5))
                            .GET()
                            .build();
                    HttpResponse<String> r = http.send(req,
                            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                    return Respuesta.de(r);
                } catch (Exception e) {
                    Respuesta fallo = new Respuesta();
                    fallo.error = e;
                    return fallo;
                }
            }

            @Override
            protected void done() {
                if (esMetrica) {
                    consultandoMetricas = false;
                }
                try {
                    mostrar(get(), esMetrica);
                } catch (Exception e) {
                    mostrarError(e);
                }
            }
        }.execute();
    }

    private void mostrar(Respuesta r, boolean esMetrica) {
        if (r.error != null) {
            mostrarError(r.error);
            return;
        }
        json.setText("HTTP " + r.codigo + "\n" + r.cuerpo);
        json.setCaretPosition(0);
        if (r.codigo >= 400) {
            estado.setText("El servidor respondio HTTP " + r.codigo + ".");
            return;
        }
        if (esMetrica) {
            poner(hilos, "Hilos del pool", valor(r.cuerpo, "hilos"));
            poner(espera, "Espera por pedido", valor(r.cuerpo, "trabajoMs") + " ms");
            poner(enCurso, "Pedidos en curso", valor(r.cuerpo, "enCurso"));
            poner(pedidos, "Pedidos", valor(r.cuerpo, "pedidos"));
            poner(ok, "OK", valor(r.cuerpo, "ok"));
            poner(fallos, "Fallos", valor(r.cuerpo, "fallos"));
            poner(minimo, "Latencia minima", valor(r.cuerpo, "latenciaMinMs") + " ms");
            poner(promedio, "Latencia promedio", valor(r.cuerpo, "latenciaPromedioMs") + " ms");
            poner(maximo, "Latencia maxima", valor(r.cuerpo, "latenciaMaxMs") + " ms");
            estado.setText("Metricas actualizadas.");
        } else {
            llenarTabla(r.cuerpo);
            estado.setText("Lista recibida.");
        }
    }

    private void mostrarError(Throwable error) {
        String mensaje;
        if (error instanceof ConnectException) {
            mensaje = "No se pudo conectar. Arranque primero TP10.servidor.ServidorCarga.";
        } else {
            mensaje = "Error de red o de lectura: " + error.getMessage();
        }
        estado.setText(mensaje);
        json.setText(mensaje);
    }

    private static void poner(JLabel etiqueta, String nombre, String valor) {
        etiqueta.setText(nombre + ": " + valor);
    }

    private void llenarTabla(String cuerpo) {
        String limpio = cuerpo == null ? "" : cuerpo.trim();
        if (!limpio.startsWith("[")) {
            return;
        }
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
            String objeto = limpio.substring(abre, cierra + 1);
            modelo.addRow(new Object[] {
                    valor(objeto, "id"),
                    valor(objeto, "nombre"),
                    valor(objeto, "precio")
            });
            desde = cierra + 1;
        }
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

    private static class Respuesta {
        int codigo;
        String cuerpo = "";
        Exception error;

        static Respuesta de(HttpResponse<String> r) {
            Respuesta respuesta = new Respuesta();
            respuesta.codigo = r.statusCode();
            respuesta.cuerpo = r.body();
            return respuesta;
        }
    }
}
