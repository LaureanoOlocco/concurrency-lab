/**
 * Clase principal que inicia la aplicación de la agencia.
 * Configura y ejecuta los diferentes procesos de la agencia utilizando hilos independientes.
 * Cada proceso se encarga de gestionar determinados segmentos de la red de petri.
 *
 * @author Laureano Olocco
 * @author Ezequiel Landaeta
 * @version 1.0
 */

import agencia.Agencia;
import agencia.Proceso;
import threadfactory.MyThreadFactory;
import logger.Log;

public class Main {

    private static final Log logger = Log.getInstancia();

    /**
     * Metodo principal que inicia la aplicación.
     * Crea los procesos, los ejecuta en hilos paralelos y mide el tiempo de ejecución.
     *
     * @param args Argumentos de línea de comandos (no utilizados)
     */
    public static void main(String[] args) {
        // Inicializar la fábrica de hilos con un nombre identificativo
        MyThreadFactory threadFactory = new MyThreadFactory("Agencia");

        // Crear una instancia de la agencia que maneja los procesos
        Agencia agencia = new Agencia();

        // Definir los segmentos que corresponden a cada transición
        // Cada array representa los segmentos asignados a un proceso específico
        int[][] segmentos = {
                {0, 1},         // Segmentos para el proceso "Entrar"
                {2, 5},         // Segmentos para el proceso "Gestionar Reserva 1"
                {3, 4},         // Segmentos para el proceso "Gestionar Reserva 2"
                {6, 9, 10},     // Segmentos para el proceso "Confirmar Pago"
                {7, 8},         // Segmentos para el proceso "Cancelar Pago"
                {11}            // Segmentos para el proceso "Salir"
        };

        // Registrar el tiempo de inicio para medir el rendimiento
        long tiempoInicial = System.currentTimeMillis();

        // Crear instancias de cada proceso con sus segmentos correspondientes
        Proceso[] procesos = crearProcesos(agencia, segmentos);

        // Crear y ejecutar un hilo para cada proceso
        ejecutarProcesos(threadFactory, procesos);

        // Calcular y mostrar el tiempo total de ejecución
        long tiempoFinal = System.currentTimeMillis();
        long tiempoTotal = tiempoFinal - tiempoInicial;
        System.out.println("\nEl programa demoró: " + tiempoTotal + " ms");
    }

    /**
     * Crea un array de procesos asociados a la agencia especificada.
     * Cada proceso se configura con sus segmentos correspondientes y un identificador.
     *
     * @param agencia   La instancia de Agencia a la que se vincularán los procesos
     * @param segmentos Matriz de segmentos donde cada fila contiene los segmentos de un proceso
     * @return Un array con todos los procesos creados
     */
    private static Proceso[] crearProcesos(Agencia agencia, int[][] segmentos) {
        Proceso entrar = new Proceso(agencia, segmentos[0], "S0");
        Proceso gestionarReserva1 = new Proceso(agencia, segmentos[1], "S1");
        Proceso gestionarReserva2 = new Proceso(agencia, segmentos[2], "S2");
        Proceso confirmarPago = new Proceso(agencia, segmentos[3], "S3");
        Proceso cancelarPago = new Proceso(agencia, segmentos[4], "S4");
        Proceso salir = new Proceso(agencia, segmentos[5], "S5");

        // Devolver directamente el array de procesos
        return new Proceso[]{entrar, gestionarReserva1, gestionarReserva2, confirmarPago, cancelarPago, salir};
    }

    /**
     * Ejecuta cada proceso en un hilo independiente y espera a que todos terminen.
     * Además, ejecuta el proceso loggerThread que tomara registro de los acontencimientos de la red de petri
     *
     * @param threadFactory La fábrica de hilos que se utilizará para crear los hilos
     * @param procesos      Array de procesos que se ejecutarán concurrentemente
     */
    private static void ejecutarProcesos(MyThreadFactory threadFactory, Proceso[] procesos) {
        Thread[] hilos = new Thread[procesos.length];

        // Crear y ejecutar un hilo para cada proceso
        for (int i = 0; i < procesos.length; i++) {
            hilos[i] = threadFactory.newThread(procesos[i]);
            hilos[i].start();
        }

        // Crea un hilo para el logger usando threadFactory
        Thread loggerThread = threadFactory.newThread(logger);
        loggerThread.start();

        // Esperar a que todos los hilos de proceso terminen
        for (int i = 0; i < procesos.length; i++) {
            try {
                hilos[i].join();
            } catch (InterruptedException e) {
                System.err.println("Error al esperar la finalización del proceso " + i);
                e.printStackTrace();
            }
        }

        // Una vez que todos los procesos han terminado, notifica al logger
        // para que genere el informe final con las estadísticas recopiladas
        logger.setTerminado(true);
        // Esperar a que el logger termine (con timeout para evitar bloqueos)
        try {
            loggerThread.join(2000);  // Esperar hasta 2 segundos
        } catch (InterruptedException e) {
            System.err.println("Error al esperar que el logger termine");
            e.printStackTrace();
        }
    }
}