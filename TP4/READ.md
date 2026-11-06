Trabajo Práctico N° 4 - Representación de Datos y Paso de Mensajes

Materia: Desarrollo de Aplicaciones para Ambientes Distribuidos
Tema: Empaquetado, serialización/deserialización y transmisión por Socket TCP
Alumno: Suarez Gonzalo


Objetivo

Comprender e implementar el empaquetado de una estructura `Transaccion` (id, origen, monto, timestamp) en dos formatos — JSON de texto y binario con `DataOutputStream` — y transmitirla por Socket TCP. Se mide el overhead: tamaño de la carga útil y tiempo de serialización + envío en una ráfaga de 1000 mensajes por formato.


Marshalling y unmarshalling

La red solo transporta bytes. Marshalling (serialización) es aplanar el objeto en memoria a esa secuencia: en el cliente, `ParserMensajes.aJson` arma una línea con las claves del enunciado (`id`, `origen`, `monto`, `timestamp`) y `aBinario` empaca `writeInt`, `writeUTF`, `writeDouble` y `writeLong`. Unmarshalling es el camino inverso en el servidor: `desdeJson` o `leerBinario` reconstruyen la `Transaccion`. El JSON viaja una línea por mensaje (`\n` como marco). El binario se lee en secuencia con `DataInputStream`, sin `BufferedReader`, para no mezclar los dos bloques en el mismo flujo TCP.


Tabla comparativa (JSON vs binario)

Valores de una corrida de 1000 transacciones (ids 1 a 1000, origen NodoA). El tiempo del cliente es serialización + envío. En otra máquina o con carga distinta los milisegundos pueden variar; el tamaño JSON cambia un poco si cambian los dígitos del `id`.

| Formato | Tamaño total (bytes) | Tiempo serialización + envío (cliente) |
| --- | --- | --- |
| JSON | 68893 | 73 ms |
| Binario | 27000 | 19 ms |

En el servidor, recepción + unmarshalling fue 88 ms (JSON) y 14 ms (binario) en esa misma prueba.


===========================================================================

Hay que ejecutar siempre primero el servidor y después el cliente, desde la carpeta `Ambiente-Distribuido`.

1. Compilación

```
javac TP4\Transaccion.java TP4\ParserMensajes.java TP4\ServidorTransacciones.java TP4\ClienteTransacciones.java
```

2. Servidor (terminal 1)

```
java TP4.ServidorTransacciones
```

Escucha el puerto 5000 y espera 1000 líneas JSON y luego 1000 mensajes binarios.

3. Cliente (terminal 2)

```
java TP4.ClienteTransacciones
```

Manda la ráfaga y muestra el tamaño de la carga útil y el tiempo de cada formato. El servidor imprime las mismas métricas y la última `Transaccion` reconstruida (id=1000).

Capturas de Pantalla de la Ejecución

Ráfaga de 1000 JSON y 1000 binarios (métricas en consola):
![Servidor: bytes y tiempos JSON vs binario](./imagenes/rafaga-servidor.jpeg.png)
![Cliente: serialización + envío JSON vs binario](./imagenes/rafaga-cliente.jpeg.png)
