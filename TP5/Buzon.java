package TP5;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.LockSupport;

// Mailbox FIFO por actor. ConcurrentLinkedQueue usa CAS (no synchronized ni ReentrantLock).
public final class Buzon {
    private final ConcurrentLinkedQueue<Mensaje> cola = new ConcurrentLinkedQueue<>();

    public void enviar(Mensaje mensaje) {
        cola.offer(mensaje);
    }

    // Bloquea sin locks de monitor: si está vacío, espera un rato y vuelve a mirar.
    public Mensaje extraer() {
        Mensaje mensaje;
        while ((mensaje = cola.poll()) == null) {
            LockSupport.parkNanos(100_000L);
        }
        return mensaje;
    }
}
