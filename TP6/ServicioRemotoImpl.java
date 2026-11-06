package TP6;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Logica de negocio. Extiende UnicastRemoteObject para que RMI pueda exportarla.
public class ServicioRemotoImpl extends UnicastRemoteObject implements ServicioRemoto {
    private static final long serialVersionUID = 1L;

    public ServicioRemotoImpl() throws RemoteException {
        super();
    }

    @Override
    public Estadisticas estadisticas(double[] valores) throws RemoteException {
        traza("estadisticas", valores == null ? "0 numeros" : valores.length + " numeros");
        if (valores == null || valores.length == 0) {
            throw new IllegalArgumentException("La lista de numeros no puede estar vacia");
        }

        double suma = 0;
        double maximo = valores[0];
        double minimo = valores[0];
        for (double valor : valores) {
            suma += valor;
            if (valor > maximo) {
                maximo = valor;
            }
            if (valor < minimo) {
                minimo = valor;
            }
        }
        double promedio = suma / valores.length;

        double sumaCuadrados = 0;
        for (double valor : valores) {
            double d = valor - promedio;
            sumaCuadrados += d * d;
        }
        double desvio = Math.sqrt(sumaCuadrados / valores.length);

        Estadisticas resultado = new Estadisticas(promedio, maximo, minimo, desvio);
        System.out.println("  -> " + resultado);
        return resultado;
    }

    @Override
    public String validarCuit(String cuit) throws RemoteException {
        traza("validarCuit", cuit);
        if (cuit == null || cuit.trim().isEmpty()) {
            return "Invalido: no se recibio ningun valor";
        }

        String digitos = soloDigitos(cuit);
        if (digitos.length() != 11) {
            return "Invalido: un CUIT tiene 11 digitos (recibidos: " + digitos.length() + ")";
        }

        int verificador = digitoVerificador(digitos);
        int ultimo = digitos.charAt(10) - '0';
        if (verificador != ultimo) {
            return "Invalido: digito verificador incorrecto (se esperaba " + verificador + ")";
        }
        return "Valido: CUIT " + formatearCuit(digitos);
    }

    @Override
    public String[] filtrar(String[] textos, String patron) throws RemoteException {
        traza("filtrar", "patron=\"" + patron + "\", n=" + (textos == null ? 0 : textos.length));
        if (textos == null || textos.length == 0) {
            return new String[0];
        }
        if (patron == null) {
            patron = "";
        }

        String buscado = patron.toLowerCase(Locale.ROOT);
        List<String> coincidencias = new ArrayList<String>();
        for (String texto : textos) {
            if (texto != null && texto.toLowerCase(Locale.ROOT).contains(buscado)) {
                coincidencias.add(texto);
            }
        }
        System.out.println("  -> coincidencias: " + coincidencias.size());
        return coincidencias.toArray(new String[0]);
    }

    private static void traza(String operacion, String detalle) {
        System.out.println("Operacion remota: " + operacion + " (" + detalle + ")");
    }

    private static String soloDigitos(String texto) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c >= '0' && c <= '9') {
                b.append(c);
            }
        }
        return b.toString();
    }

    // Multiplicadores AFIP: 5 4 3 2 7 6 5 4 3 2 sobre los primeros 10 digitos.
    private static int digitoVerificador(String onceDigitos) {
        int[] factores = { 5, 4, 3, 2, 7, 6, 5, 4, 3, 2 };
        int suma = 0;
        for (int i = 0; i < 10; i++) {
            suma += (onceDigitos.charAt(i) - '0') * factores[i];
        }
        int codigo = 11 - (suma % 11);
        if (codigo == 11) {
            return 0;
        }
        if (codigo == 10) {
            return 9;
        }
        return codigo;
    }

    private static String formatearCuit(String digitos) {
        return digitos.substring(0, 2) + "-" + digitos.substring(2, 10) + "-" + digitos.substring(10);
    }
}
