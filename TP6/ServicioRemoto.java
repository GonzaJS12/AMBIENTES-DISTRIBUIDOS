package TP6;

import java.rmi.Remote;
import java.rmi.RemoteException;

// Contrato RMI: el cliente llama estos metodos como si fueran locales.
// extends Remote + RemoteException en cada uno = "esto puede ir por la red".
public interface ServicioRemoto extends Remote {

    Estadisticas estadisticas(double[] valores) throws RemoteException;

    String validarCuit(String cuit) throws RemoteException;

    String[] filtrar(String[] textos, String patron) throws RemoteException;
}
