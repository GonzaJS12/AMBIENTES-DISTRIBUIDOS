package TP5;

import java.util.Locale;

// Mensaje inmutable: una lectura de sensor. Nadie puede cambiarla después de crearla.
public final class Lectura implements Mensaje {
    private final String idSensor;
    private final String tipo;
    private final double valor;
    private final long timestamp;

    public Lectura(String idSensor, String tipo, double valor, long timestamp) {
        this.idSensor = idSensor;
        this.tipo = tipo;
        this.valor = valor;
        this.timestamp = timestamp;
    }

    public String getIdSensor() {
        return idSensor;
    }

    public String getTipo() {
        return tipo;
    }

    public double getValor() {
        return valor;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "Lectura { sensor=" + idSensor
                + ", tipo=" + tipo
                + ", valor=" + String.format(Locale.US, "%.2f", valor)
                + ", marca de tiempo=" + timestamp + " }";
    }
}
