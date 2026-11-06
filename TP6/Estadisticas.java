package TP6;

import java.io.Serializable;
import java.util.Locale;

// Resultado de estadisticas(): viaja por RMI como Transaccion en TP4 (Serializable = se puede copiar a bytes).
public class Estadisticas implements Serializable {
    private static final long serialVersionUID = 1L;

    private final double promedio;
    private final double maximo;
    private final double minimo;
    private final double desvioEstandar;

    public Estadisticas(double promedio, double maximo, double minimo, double desvioEstandar) {
        this.promedio = promedio;
        this.maximo = maximo;
        this.minimo = minimo;
        this.desvioEstandar = desvioEstandar;
    }

    public double getPromedio() {
        return promedio;
    }

    public double getMaximo() {
        return maximo;
    }

    public double getMinimo() {
        return minimo;
    }

    public double getDesvioEstandar() {
        return desvioEstandar;
    }

    @Override
    public String toString() {
        return "Estadisticas { promedio=" + n(promedio)
                + ", maximo=" + n(maximo)
                + ", minimo=" + n(minimo)
                + ", desvio=" + n(desvioEstandar) + " }";
    }

    private static String n(double valor) {
        return String.format(Locale.US, "%.2f", valor);
    }
}
