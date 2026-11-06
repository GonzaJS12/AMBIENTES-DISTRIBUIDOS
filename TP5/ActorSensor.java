package TP5;

// Dispositivo: genera lecturas inmutables y las manda al buzón del procesador (Send asíncrono).
public class ActorSensor extends Thread {
    private final String idSensor;
    private final Buzon destino;
    private final int cantidadMensajes;

    public ActorSensor(String idSensor, Buzon destino, int cantidadMensajes) {
        super(idSensor);
        this.idSensor = idSensor;
        this.destino = destino;
        this.cantidadMensajes = cantidadMensajes;
    }

    @Override
    public void run() {
        System.out.println(idSensor + " spawn: va a enviar " + cantidadMensajes + " lecturas.");
        for (int i = 0; i < cantidadMensajes; i++) {
            double temperatura = 18.0 + (Math.random() * 12.0);
            Lectura lectura = new Lectura(idSensor, "temperatura", temperatura, System.currentTimeMillis());
            destino.enviar(lectura);
        }
        destino.enviar(new MensajeFin(idSensor));
        System.out.println(idSensor + " termino de encolar su rafaga.");
    }
}
