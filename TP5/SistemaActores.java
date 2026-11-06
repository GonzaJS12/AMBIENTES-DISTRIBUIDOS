package TP5;

// Spawn: crea el procesador y varios sensores, cada uno con comunicación solo por mensajes.
public class SistemaActores {
    public static final int SENSORES = 5;
    public static final int MENSAJES_POR_SENSOR = 100;
    public static final int TOTAL = SENSORES * MENSAJES_POR_SENSOR;

    public static void main(String[] args) {
        System.out.println("Modelo de actores - TP5");
        System.out.println("Rafaga concurrente: " + SENSORES + " sensores x "
                + MENSAJES_POR_SENSOR + " = " + TOTAL + " mensajes.");
        System.out.println("Sin synchronized, sin ReentrantLock, sin estado compartido.");
        System.out.println();

        Buzon buzonProcesador = new Buzon();
        ActorProcesador procesador = new ActorProcesador(buzonProcesador, SENSORES);
        procesador.start();

        ActorSensor[] sensores = new ActorSensor[SENSORES];
        for (int i = 0; i < SENSORES; i++) {
            sensores[i] = new ActorSensor("Sensor-" + (i + 1), buzonProcesador, MENSAJES_POR_SENSOR);
            sensores[i].start();
        }

        try {
            for (ActorSensor sensor : sensores) {
                sensor.join();
            }
            procesador.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Interrumpido: " + e.getMessage());
        }
    }
}
