package agencia;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.Arrays;

import monitor.Monitor;

/**
 * Clase Proceso que implementa la interfaz Runnable para crear hilos
 * que interactúan con el Monitor y la Red de Petri.
 * <p>
 * Esta clase representa un proceso concurrente que realiza disparos de transiciones
 * en la Red de Petri a través del Monitor. Cada instancia de Proceso
 * puede estar asociada a una Agencia específica y tiene asignadas
 * determinadas transiciones para disparar.
 */
public class Proceso implements Runnable {

    /**
     * Número total de disparos a realizar en toda la simulación
     */
    private static final int DISPAROS_TOTALES = 186;

    /**
     * Instancia única del Monitor que sincroniza los procesos
     */
    private static final Monitor monitor = Monitor.getInstanciaMonitor();

    /**
     * Agencia a la que pertenece este proceso
     */
    private Agencia agencia;

    /**
     * Contador de reservas totales realizadas por este proceso
     */
    // private int reservasTotales;

    /**
     * Arreglo que almacena las transiciones que este proceso puede disparar
     */
    private int[] transiciones;

    private final String nombreProceso;

    private final AtomicInteger reservasTotales = new AtomicInteger();


    /**
     * Constructor para crear un nuevo proceso.
     *
     * @param agencia       La agencia a la que pertenece este proceso
     * @param transiciones  Arreglo con los identificadores de las transiciones que este proceso puede disparar
     * @param nombreProceso Nombre identificador del proceso para facilitar el seguimiento
     */
    public Proceso(Agencia agencia, int[] transiciones, String nombreProceso) {
        this.agencia = agencia;
        this.transiciones = Arrays.copyOf(transiciones, transiciones.length); // Defensiva
        this.nombreProceso = nombreProceso;
        //this.reservasTotales = 0;
    }

    /**
     * Metodo principal que se ejecuta cuando el hilo es iniciado.
     * Este metodo debe ser implementado para contener la lógica
     * de ejecución del proceso, como el disparo de transiciones
     * en la Red de Petri a través del Monitor.
     */
    @Override
    public void run() {
        // La implementación específica debe ser añadida aquí
        if (transiciones == null || transiciones.length == 0) return;

        try {
            if (transiciones[0] == 0) {
                entradaAgencia();
            } else {
                realizarAccion();
            }
        } catch (InterruptedException e) {
            System.err.println("El hilo fue interrumpido: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    private void entradaAgencia() throws InterruptedException {
        while (reservasTotales.get() < DISPAROS_TOTALES) {
            for (int transicion : transiciones) {
                if (monitor.fireTransition(transicion)) {
                    agencia.transicionDisparada(transicion);
                    if (transicion == 0) {
                        reservasTotales.incrementAndGet();
                    }
                }
            }
            System.out.println("\n ----------------Reservas totales: " + reservasTotales.get() + "---------------------------------\n");
        }
        if (reservasTotales.get() == DISPAROS_TOTALES) {
            throw new InterruptedException("Se alcanzaron los disparos máximos.");
        }
    }

    private void realizarAccion() throws InterruptedException {
        while (!agencia.limiteAlcanzado()) {
            for (int transicion : transiciones) {
                if (monitor.fireTransition(transicion)) {
                    agencia.transicionDisparada(transicion);
                }
            }
        }
        if (reservasTotales.get() == DISPAROS_TOTALES) {
            throw new InterruptedException("Se alcanzaron los disparos máximos.");
        }
    }

}