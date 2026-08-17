import java.io.*;
import java.net.*;

public class Servidor {
    public static void main(String[] args) {
        int puerto = 5000;

        try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            System.out.println("Servidor a la escucha en el puerto " + puerto + "...");
            while (true) {
                try (Socket cliente = serverSocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
                    PrintWriter out = new PrintWriter(cliente.getOutputStream(), true)) {
                    
                    System.out.println("Cliente conectado desde: " + cliente.getInetAddress());
                    String peticion = in.readLine();
                    
                    if (peticion != null) {
                        System.out.println("Petición recibida: " + peticion);
                        String[] partes = peticion.split(";");
                        if (partes.length == 3) {
                            int num1 = Integer.parseInt(partes[0].trim());
                            String operador = partes[1].trim();
                            int num2 = Integer.parseInt(partes[2].trim());
                            String resultado = calcular(num1, operador, num2);
                            out.println(resultado);
                        } else {
                            out.println("ERROR: Formato incorrecto. Use 'numero;operador;numero'");
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error procesando la petición del cliente: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Error al iniciar el servidor: " + e.getMessage());
        }
    }

    private static String calcular(int num1, String op, int num2) {
        switch (op) {
            case "+":
                return String.valueOf(num1 + num2);
            case "-":
                return String.valueOf(num1 - num2);
            case "*":
                return String.valueOf(num1 * num2);
            case "/":
                if (num2 == 0) {
                    return "ERROR: Division por cero";
                }
                return String.valueOf((double) num1 / num2); 
            default:
                return "ERROR: Operador matemático no válido";
        }
    }
}