package monitor;

import java.util.concurrent.Semaphore;

import rdp.RedDePetri;

//import logger.LoggerThread;

/**
 * Clase Monitor que implementa el patrón de diseño Singleton para la sincronización
 * de procesos concurrentes que operan sobre una Red de Petri.
 * <p>
 * Esta clase utiliza el patrón Singleton para garantizar una única instancia del monitor
 * en toda la aplicación, protegiendo así la Red de Petri de accesos concurrentes no controlados.
 * <p>
 * El monitor implementa mecanismos de exclusión mutua y encolamiento de procesos
 * cuando intentan disparar transiciones no sensibilizadas en la Red de Petri.
 */
public class Monitor implements MonitorInterface {

    //private static final LoggerThread logger = LoggerThread.getInstancia();

    /**
     * Número total de disparos a realizar en la simulación
     */
    private final static int DISPAROS_TOTALES = 186;

    /**
     * Número total de transiciones de la Red de Petri
     */
    private final static int TRANSICIONES_TOTALES = 12;

    /**
     * Instancia única del Monitor (patrón Singleton)
     * Tiene como finalidad mantener la consistencia y la visibilidad del objeto
     * monitor a través de todos los hilos.
     */
    private static volatile Monitor monitor; //

    /**
     * Semáforo para garantizar exclusión mutua en el acceso a la Red de Petri
     */
    private static Semaphore mutex;

    /**
     * Arreglo de semáforos para encolar procesos bloqueados por transición
     */
    private static Semaphore[] cola;

    /**
     * Instancia de la Red de Petri que utiliza el monitor
     */
    private static RedDePetri redDePetri;

    /**
     * Política de selección de transiciones a despertar
     */
    private static Politica politica;


    /**
     * Constructor privado que inicializa los componentes del monitor.
     * Es privado debido a la implementación del patrón Singleton.
     */
    private Monitor() {
        mutex = new Semaphore(1);
        cola = new Semaphore[TRANSICIONES_TOTALES];
        for (int i = 0; i < TRANSICIONES_TOTALES; i++) {
            cola[i] = new Semaphore(0);
        }
        redDePetri = new RedDePetri();
        politica = new Politica(redDePetri);
    }

    /**
     * Metodo que implementa el patrón Singleton para obtener una única instancia del Monitor.
     * Utiliza el patrón de diseño Double-Check Locking para garantizar la creación de una única instancia
     * en un entorno multihilo.
     *
     * @return La instancia única del Monitor
     */
    public static Monitor getInstanciaMonitor() {
        if (monitor == null) { // Primera comprobación (sincronización externa)
            synchronized (Monitor.class) { // Actúa Como un candado global para toda la clase. Si fuera this cada hilo crearia su propio lock.
                if (monitor == null) { // Segunda comprobación (sincronización interna)
                    monitor = new Monitor();
                }
            }
        }
        return monitor;
    }

    /**
     * Intenta disparar una transición en la Red de Petri.
     * Si la transición no está sensibilizada, el proceso se bloquea
     * hasta que otro proceso lo despierte.
     *
     * @param transicion Número de transición a disparar
     * @return true si el disparo se completó con éxito, false en caso contrario
     */
    public boolean fireTransition(int transicion) {
        boolean disparoEfectuado = false;
        adquirirMutex();

        do {
            // Verifica si ya se completaron todos los disparos requeridos
            if (disparosCompletados()) {
                liberarHilos();
                liberarMutex();
                break;
            }

            // Intenta disparar la transición
            disparoEfectuado = redDePetri.disparar(transicion);
            System.out.println("Resultado: " + disparoEfectuado + " Transicion: " + transicion);
            liberarMutex();

            // Si no se pudo disparar, el proceso se bloquea en su correspondiente cola
            if (!disparoEfectuado) {
                try {
                    System.out.println("Transicion " + transicion + " en cola");
                    cola[transicion].acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        } while (!disparoEfectuado && !disparosCompletados());

        return disparoEfectuado;
    }

    /**
     * Adquiere el semáforo de exclusión mutua para disparar la Red de Petri.
     * Si el semáforo no está disponible, el proceso se bloquea hasta que lo esté.
     */
    private void adquirirMutex() {
        System.out.println(Thread.currentThread().getName() + " intenta adquirir el mutex");
        try {
            mutex.acquire();
            System.out.println(Thread.currentThread().getName() + " adquirió el mutex");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Verifica si se completaron los disparos requeridos.
     *
     * @return true si se han completado todos los disparos, false en caso contrario
     */
    private boolean disparosCompletados() {
        return redDePetri.getDisparos()[11] >= DISPAROS_TOTALES;
    }

    /**
     * Obtiene un arreglo que indica qué transiciones tienen procesos esperando en sus colas.
     *
     * @return Un arreglo de enteros donde cada posición indica si hay procesos esperando (1) o no (0)
     */
    private int[] transicionesEnCola() {
        int[] transiciones = new int[TRANSICIONES_TOTALES];
        for (int i = 0; i < TRANSICIONES_TOTALES; i++) {
            transiciones[i] = cola[i].hasQueuedThreads() ? 1 : 0;
        }
        return transiciones;
    }

    /**
     * Obtiene un arreglo que indica qué transiciones están sensibilizadas y están en la cola.
     * Realiza una operación AND entre las transiciones sensibilizadas y las que están en la variable de condición (cola).
     *
     * @return Un arreglo con las transiciones que cumplen ambas condiciones
     */
    private int[] obtenerTransiciones() {
        // Obtiene las transiciones sensibilizadas y en cola
        long tiempo = System.currentTimeMillis();
        int[] sensibilizadas = redDePetri.getSensibilizadas();
        int[] enCola = transicionesEnCola();
        int[] transiciones = new int[TRANSICIONES_TOTALES];
        // And entre transiciones sensibilizadas y en cola
        for (int i = 0; i < TRANSICIONES_TOTALES; i++) {
            transiciones[i] = sensibilizadas[i] & enCola[i];
        }
        return transiciones;
    }

    /**
     * Intenta despertar un proceso que está en cola.
     * Utiliza una política para elegir qué proceso despertar. (Proceso asociado a transiciones)
     * Si no hay una transicion que cumpla con las condiciones de la politica devuelve la transicion T0
     * <p>
     * El metodo hasQueuedThreads se utiliza para asegurarse de que en tal caso T0 este en cola y no se rompa la cola
     *
     * @return true si se despertó algún proceso, false si no había procesos para despertar
     */
    private boolean despertarTransicion() {
        // Obtiene las transiciones sensibilizadas y en cola
        int[] t = obtenerTransiciones();

        // Llama a la política para elegir transición
        int tADisparar = politica.elegirPolitica(t);

        // Si la transición obtenida está en cola
        if (cola[tADisparar].hasQueuedThreads()) {
            // La libera
            cola[tADisparar].release();
            return true;
        }
        return false;
    }

    /**
     * Si hay alguna transición sensibilizada con procesos en cola, despierta uno de esos procesos.
     * Si no hay transiciones para despertar, libera el mutex para que otro proceso pueda acceder.
     */
    private void liberarMutex() {
        if (!despertarTransicion()) mutex.release();
        System.out.println(Thread.currentThread().getName() + " libero el mutex, permisos: " + mutex.availablePermits());

    }

    private void liberarHilos() {
        for (int i = 0; i < cola.length; i++) {
            if (cola[i].hasQueuedThreads()) {
                cola[i].release();
            }
        }
    }
}