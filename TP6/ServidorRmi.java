package TP6;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

// Publica el servicio: registro de nombres (1099) + rebind. La logica esta en ServicioRemotoImpl.
public class ServidorRmi {
    public static final int PUERTO = 1099;
    public static final String NOMBRE = "ServicioProcesamiento";

    public static void main(String[] args) {
        try {
            LocateRegistry.createRegistry(PUERTO);
            System.out.println("RMI Registry listo en el puerto " + PUERTO);

            ServicioRemotoImpl servicio = new ServicioRemotoImpl();
            Registry registro = LocateRegistry.getRegistry(PUERTO);
            registro.rebind(NOMBRE, servicio);

            System.out.println("Servicio publicado como \"" + NOMBRE + "\"");
            System.out.println("Esperando invocaciones remotas... (no cerrar esta consola)");
        } catch (Exception e) {
            System.err.println("Error al publicar el servicio: " + e.getMessage());
        }
    }
}
