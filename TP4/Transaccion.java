package TP4;

// Estructura en memoria: cuatro campos tipados. La red, más adelante, solo verá bytes.
public class Transaccion {
    private int idTransaccion;
    private String origen;
    private double monto;
    private long timestamp;

    public Transaccion(int idTransaccion, String origen, double monto, long timestamp) {
        this.idTransaccion = idTransaccion;
        this.origen = origen;
        this.monto = monto;
        this.timestamp = timestamp;
    }

    public int getIdTransaccion() {
        return idTransaccion;
    }

    public String getOrigen() {
        return origen;
    }

    public double getMonto() {
        return monto;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "Transaccion { id=" + idTransaccion
                + ", origen=" + origen
                + ", monto=" + String.format(java.util.Locale.US, "%.2f", monto)
                + ", marca de tiempo=" + timestamp + " }";
    }

    public static void main(String[] args) {
        Transaccion muestra = new Transaccion(101, "NodoA", 1500.50, 1700000000L);
        System.out.println(muestra);
    }
}
