package TP5;

// Aviso asincrónico: ese sensor ya mandó toda su ráfaga. No comparte estado, solo un objeto.
public final class MensajeFin implements Mensaje {
    private final String idSensor;

    public MensajeFin(String idSensor) {
        this.idSensor = idSensor;
    }

    public String getIdSensor() {
        return idSensor;
    }
}
