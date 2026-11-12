package TP10.servidor;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

// Servicio de catalogo para medir carga. El cuello de botella es el pool de 8 hilos:
// cada GET simula una lectura sincronica de 40 ms y retiene un hilo todo ese tiempo.
public class ServidorCarga {
    public static final int PUERTO = 8090;
    static final int HILOS = 8;
    static final int TRABAJO_MS = 40;

    private static final String LISTA = "["
            + "{\"id\":1,\"nombre\":\"Camara\",\"precio\":10000.00},"
            + "{\"id\":2,\"nombre\":\"Mouse\",\"precio\":5000.00},"
            + "{\"id\":3,\"nombre\":\"Teclado\",\"precio\":8000.00}"
            + "]";

    private static final LongAdder pedidos = new LongAdder();
    private static final LongAdder ok = new LongAdder();
    private static final LongAdder fallos = new LongAdder();
    private static final LongAdder sumaMs = new LongAdder();
    private static final AtomicLong minimo = new AtomicLong(Long.MAX_VALUE);
    private static final AtomicLong maximo = new AtomicLong(0);
    private static final AtomicLong enCurso = new AtomicLong(0);

    public static void main(String[] args) throws IOException {
        ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(HILOS);
        HttpServer server = HttpServer.create(new InetSocketAddress(PUERTO), 0);
        server.setExecutor(pool);
        server.createContext("/productos", ServidorCarga::productos);
        server.createContext("/metricas", ServidorCarga::metricas);
        server.start();
        System.out.println("Servidor de carga en http://localhost:" + PUERTO + "/productos");
        System.out.println("Pool de " + HILOS + " hilos. Cada GET retiene un hilo " + TRABAJO_MS + " ms.");
        System.out.println("Metricas: http://localhost:" + PUERTO + "/metricas");
        System.out.println("No cerrar esta consola durante la prueba.");
    }

    private static void productos(HttpExchange intercambio) throws IOException {
        long inicio = System.nanoTime();
        enCurso.incrementAndGet();
        pedidos.increment();
        int codigo = 500;
        try {
            if (!"GET".equalsIgnoreCase(intercambio.getRequestMethod())) {
                codigo = 405;
                responder(intercambio, 405, "{\"error\":\"Metodo no permitido\"}");
                return;
            }
            TimeUnit.MILLISECONDS.sleep(TRABAJO_MS);
            codigo = 200;
            responder(intercambio, 200, LISTA);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            codigo = 500;
            responder(intercambio, 500, "{\"error\":\"interrumpido\"}");
        } finally {
            enCurso.decrementAndGet();
            long ms = (System.nanoTime() - inicio) / 1_000_000L;
            sumaMs.add(ms);
            if (codigo >= 200 && codigo < 400) {
                ok.increment();
            } else {
                fallos.increment();
            }
            actualizarExtremo(minimo, maximo, ms);
        }
    }

    private static void metricas(HttpExchange intercambio) throws IOException {
        long total = pedidos.sum();
        long promedio = total == 0 ? 0 : sumaMs.sum() / total;
        long min = minimo.get() == Long.MAX_VALUE ? 0 : minimo.get();
        String json = "{\"hilos\":" + HILOS
                + ",\"trabajoMs\":" + TRABAJO_MS
                + ",\"enCurso\":" + enCurso.get()
                + ",\"pedidos\":" + total
                + ",\"ok\":" + ok.sum()
                + ",\"fallos\":" + fallos.sum()
                + ",\"latenciaPromedioMs\":" + promedio
                + ",\"latenciaMinMs\":" + min
                + ",\"latenciaMaxMs\":" + maximo.get()
                + "}";
        responder(intercambio, 200, json);
    }

    private static void actualizarExtremo(AtomicLong min, AtomicLong max, long valor) {
        long previo;
        do {
            previo = min.get();
            if (valor >= previo) {
                break;
            }
        } while (!min.compareAndSet(previo, valor));
        do {
            previo = max.get();
            if (valor <= previo) {
                break;
            }
        } while (!max.compareAndSet(previo, valor));
    }

    private static void responder(HttpExchange intercambio, int codigo, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        intercambio.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        intercambio.sendResponseHeaders(codigo, bytes.length);
        try (OutputStream out = intercambio.getResponseBody()) {
            out.write(bytes);
        }
    }
}
