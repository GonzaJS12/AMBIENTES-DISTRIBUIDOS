package TP6;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

// Menu por consola. lookup + llamadas a la interfaz: no hay Socket ni serializacion a mano.
public class ClienteRmi {
    public static void main(String[] args) {
        String host = "localhost";

        try (Scanner scanner = new Scanner(System.in)) {
            Registry registro = LocateRegistry.getRegistry(host, ServidorRmi.PUERTO);
            ServicioRemoto servicio = (ServicioRemoto) registro.lookup(ServidorRmi.NOMBRE);
            System.out.println("Referencia remota obtenida: \"" + ServidorRmi.NOMBRE + "\"");
            System.out.println("Las llamadas parecen locales; el stub habla con el servidor.");
            System.out.println();

            boolean seguir = true;
            while (seguir) {
                mostrarMenu();
                String opcion = scanner.nextLine().trim();
                try {
                    if ("1".equals(opcion)) {
                        ejecutarEstadisticas(scanner, servicio);
                    } else if ("2".equals(opcion)) {
                        ejecutarValidacion(scanner, servicio);
                    } else if ("3".equals(opcion)) {
                        ejecutarFiltro(scanner, servicio);
                    } else if ("0".equals(opcion)) {
                        seguir = false;
                        System.out.println("Cliente cerrado. El servidor sigue publicado.");
                    } else {
                        System.out.println("Opcion no valida.");
                    }
                } catch (RemoteException e) {
                    System.err.println("Fallo remoto (RemoteException): " + e.getMessage());
                    System.err.println("El servidor se cayo o no respondio durante la invocacion.");
                } catch (IllegalArgumentException e) {
                    System.err.println("Datos rechazados por el servidor: " + e.getMessage());
                }
                System.out.println();
            }
        } catch (NotBoundException e) {
            System.err.println("No hay un servicio llamado \"" + ServidorRmi.NOMBRE + "\" en el registro.");
            System.err.println("Arranque primero el servidor.");
        } catch (RemoteException e) {
            System.err.println("No se pudo contactar el RMI Registry en "
                    + host + ":" + ServidorRmi.PUERTO);
            System.err.println("Asegurese de que ServidorRmi este en ejecucion.");
            System.err.println("Detalle: " + e.getMessage());
        }
    }

    private static void mostrarMenu() {
        System.out.println("--- Procesamiento remoto ---");
        System.out.println("1) Estadisticas (promedio, max, min, desvio)");
        System.out.println("2) Validar CUIT");
        System.out.println("3) Filtrar textos por patron");
        System.out.println("0) Salir");
        System.out.print("Opcion: ");
    }

    private static void ejecutarEstadisticas(Scanner scanner, ServicioRemoto servicio) throws RemoteException {
        System.out.print("Numeros separados por espacio o coma: ");
        double[] valores = parsearNumeros(scanner.nextLine());
        Estadisticas resultado = servicio.estadisticas(valores);
        System.out.println("Resultado remoto: " + resultado);
    }

    private static void ejecutarValidacion(Scanner scanner, ServicioRemoto servicio) throws RemoteException {
        System.out.print("CUIT (con o sin guiones): ");
        String cuit = scanner.nextLine();
        System.out.println("Resultado remoto: " + servicio.validarCuit(cuit));
    }

    private static void ejecutarFiltro(Scanner scanner, ServicioRemoto servicio) throws RemoteException {
        System.out.print("Cuantas cadenas va a cargar?: ");
        int n = Integer.parseInt(scanner.nextLine().trim());
        String[] textos = new String[n];
        for (int i = 0; i < n; i++) {
            System.out.print("  texto " + (i + 1) + ": ");
            textos[i] = scanner.nextLine();
        }
        System.out.print("Patron de busqueda: ");
        String patron = scanner.nextLine();
        String[] filtradas = servicio.filtrar(textos, patron);
        System.out.println("Coincidencias remotas: " + filtradas.length);
        for (String linea : filtradas) {
            System.out.println("  - " + linea);
        }
    }

    private static double[] parsearNumeros(String linea) {
        String[] partes = linea.trim().split("[,;\\s]+");
        List<Double> lista = new ArrayList<Double>();
        for (String parte : partes) {
            if (!parte.isEmpty()) {
                lista.add(Double.parseDouble(parte.replace(',', '.')));
            }
        }
        double[] valores = new double[lista.size()];
        for (int i = 0; i < lista.size(); i++) {
            valores[i] = lista.get(i);
        }
        return valores;
    }
}
