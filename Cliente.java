import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Cliente {
    public static void main(String[] args) {
        String host = "localhost";
        int puerto = 5500;

        try (Scanner scanner = new Scanner(System.in);
            Socket socket = new Socket(host, puerto);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("--- Calculadora ---");
            System.out.print("Ingrese el primer número entero: ");
            int num1 = scanner.nextInt();
            System.out.print("Ingrese la operación (+, -, *, /): ");
            String operador = scanner.next();
            System.out.print("Ingrese el segundo número entero: ");
            int num2 = scanner.nextInt();
            String mensaje = num1 + ";" + operador + ";" + num2;

            out.println(mensaje);
            System.out.println("\nEnviando al servidor: " + mensaje);

            String respuesta = in.readLine();
            System.out.println("Resultado recibido: " + respuesta);

        } catch (IOException e) {
            System.err.println("No se pudo conectar al servidor. Asegúrese que este en ejecución.");
            System.err.println("Detalle del error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error de entrada de datos: " + e.getMessage());
        }
    }
}