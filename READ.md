Trabajo Práctico N° 1 - Calculadora Distribuida Simple

Materia: Desarrollo de Aplicaciones para Ambientes Distribuidos
Tema: Arquitectura Cliente-Servidor y Comunicación mediante Sockets TCP
Alumno: Suarez Gonzalo


2-1) El archivo Cliente busca conectarse al Servidor pero al no estar inicializado emite error de conexion
""No se pudo conectar al servidor. Asegúrese que este en ejecución.""
""Detalle del error: Connection refused: connect""
2-2) En la linea 11 del archivo Servidor.java se genera el evento de red.
2-3) En primer lugar se debiria cambiar el HOST donde apuntaria, este debe tener el ip de la notebook donde se inicializaria el servidor. 
Que ambas notebook esten conectadas a la misma red.
configurar debidamente el firewall.

===========================================================================

Este proyecto es un modelo Cliente-Servidor basico utilizando la API `java.net`. 
Consiste en una calculadora matemática remota donde el cliente envía dos números y un operador, 
y el servidor realiza el cálculo (incluyendo el manejo de errores como la división por cero) devolviendo el resultado.

Para probar el sistema, es fundamental ejecutar primero el Servidor y luego el Cliente.

1. Compilación
Abrir una terminal en la carpeta donde se encuentran los archivos `.java` y compílalos:
2. Ejecutar el Servidor
En la terminal, iniciar el servidor para que quede a la escucha en el puerto 5500:
3. Ejecutar el Cliente
Abre una nueva terminal (manteniendo el servidor corriendo en la anterior) y ejecuta el cliente.

Capturas de Pantalla de la Ejecución

Ejecución normal (Suma/Resta/Multiplicación):
![Suma](./imagenes/calculadora-suma.jpge)
![Resta](./imagenes/calculadora-resta.jpge)
![Multiplicacion](./imagenes/calculadora-mult.jpge)
![Division](./imagenes/calculadora-division.jpge)

Manejo de error (División por cero):
![Division por cero](./imagenes/calculadora-diviporcero.jpge)

Inicio de servidor:
![Inicio](./imagenes/inicio-servidor.jpge)

Error de servidor:
![Error de conexion](./imagenes/error-servidor.jpge)

Inicio de calculadora:
![Inicio](./imagenes/inicio-calculadora.jpge)
