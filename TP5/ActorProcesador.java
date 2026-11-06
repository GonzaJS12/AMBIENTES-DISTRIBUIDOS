package TP5;

import java.util.Locale;

// Nodo central: un solo hilo lee el buzón en orden FIFO y actualiza estado privado.
public class ActorProcesador extends Thread {
    private final Buzon buzon;
    private final int sensoresEsperados;

    private int cantidad;
    private double suma;
    private double promedio;
    private int sensoresTerminados;

    public ActorProcesador(Buzon buzon, int sensoresEsperados) {
        super("ActorProcesador");
        this.buzon = buzon;
        this.sensoresEsperados = sensoresEsperados;
    }

    public Buzon getBuzon() {
        return buzon;
    }

    @Override
    public void run() {
        System.out.println("ActorProcesador en marcha. Estado interno aislado.");
        System.out.println("Esperando lecturas de " + sensoresEsperados + " sensores...");
        System.out.println();

        while (true) {
            Mensaje mensaje = buzon.extraer();

            if (mensaje instanceof Lectura) {
                procesarLectura((Lectura) mensaje);
            } else if (mensaje instanceof MensajeFin) {
                MensajeFin fin = (MensajeFin) mensaje;
                sensoresTerminados++;
                System.out.println("Fin de rafaga de " + fin.getIdSensor()
                        + " (" + sensoresTerminados + "/" + sensoresEsperados + ")");
                if (sensoresTerminados >= sensoresEsperados) {
                    break;
                }
            }
        }

        System.out.println();
        System.out.println("--- Resultado (sin memoria compartida ni locks) ---");
        System.out.println("Mensajes procesados en secuencia: " + cantidad);
        System.out.println("Suma acumulada: " + String.format(Locale.US, "%.2f", suma));
        System.out.println("Promedio historico final: " + String.format(Locale.US, "%.2f", promedio));
    }

    // Designar (state change): un mensaje a la vez, el siguiente ve el estado ya actualizado.
    private void procesarLectura(Lectura lectura) {
        cantidad++;
        suma += lectura.getValor();
        promedio = suma / cantidad;
        System.out.println("Procesado #" + cantidad
                + " | " + lectura
                + " | promedio=" + String.format(Locale.US, "%.2f", promedio));
    }
}
