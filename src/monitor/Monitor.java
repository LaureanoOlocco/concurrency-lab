package monitor;

import java.util.concurrent.Semaphore;

import rdp.RedDePetri;

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

    // Constantes
    private static final int DISPAROS_TOTALES = 186;
    private static final int TRANSICIONES_TOTALES = 12;

    // Componente del patrón Singleton
    private static volatile Monitor monitor;

    // Componentes de sincronización
    private static Semaphore mutex;
    private static Semaphore[] cola;

    // Componentes del modelo
    private static RedDePetri redDePetri;
    private static Politica politica;

    // Logging (comentado)
    // private static final LoggerThread logger = LoggerThread.getInstancia();

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
            synchronized (Monitor.class) { // Actúa como un candado global para toda la clase
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
        adquirirMutex();
        boolean disparoEfectuado = false;
        boolean puedeDisparar = false;

        do {
            // Verifica si ya se completaron todos los disparos requeridos
            if (disparosCompletados()) {
                liberarHilos();
                liberarMutex();
                break;
            }

            puedeDisparar = puedeDispararse(transicion);
            // Intenta disparar la transición
            disparoEfectuado = redDePetri.disparar(transicion, puedeDisparar);
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

    // ===================================================================================
    // Métodos para gestión de transiciones temporales
    // ===================================================================================

    /**
     * Verifica si una transición está antes de su ventana temporal.
     * <p>
     * Este metodo determina si una transición temporal ha alcanzado su tiempo mínimo
     * de sensibilización (alfa). Una transición está "antes de la ventana" cuando
     * el tiempo actual es menor que el tiempo mínimo requerido para dispararla.
     *
     * @param transicion   Índice de la transición a verificar
     * @param tiempoActual Tiempo actual en milisegundos (System.currentTimeMillis)
     * @return true si la transición está antes de su ventana temporal (debe esperar),
     * false si está dentro de su ventana o no es una transición temporal
     */
    private boolean estaAntesDeVentanaTemporal(int transicion, long tiempoActual) {
        // Solo aplica a transiciones con restricciones temporales
        if (redDePetri.esTransicionTemporal(transicion)) {
            // Verifica si la transición está sensibilizada en tiempo
            // Si está en la ventana devuelve false, si no devuelve true
            return !redDePetri.estaSensibilizadaEnTiempo(transicion, tiempoActual);
        }
        return false; // No es una transición temporal
    }

    /**
     * Realiza la espera necesaria cuando una transición debe retrasar su disparo.
     * <p>
     * Cuando una transición está sensibilizada por marcado, pero aún no cumple con
     * el tiempo mínimo de su ventana temporal (alfa), este metodo suspende el hilo
     * actual hasta que se alcance dicho tiempo mínimo.
     *
     * @param transicion   Índice de la transición que debe esperar
     * @param tiempoEspera Tiempo en milisegundos que debe esperar para alcanzar
     *                     el límite inferior de la ventana temporal
     */
    private void esperarTiempoMinimo(int transicion, long tiempoEspera) {
        // Marca la transición como en estado de espera
        redDePetri.setEstadoDeEspera(transicion, true);

        // Libera el mutex para que otros hilos puedan ejecutarse
        liberarMutex();

        try {
            // Suspende el hilo por el tiempo necesario
            Thread.sleep(tiempoEspera);

            // Marca la transición como ya no en espera
            redDePetri.setEstadoDeEspera(transicion, false);

            // Vuelve a adquirir el mutex para continuar
            adquirirMutex();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Verifica si una transición está sensibilizada, considerando tanto el marcado
     * como las restricciones temporales.
     * <p>
     * Una transición está completamente sensibilizada cuando:
     * 1. Tiene suficientes tokens en sus plazas de entrada (sensibilizada por marcado)
     * 2. Ha cumplido con el tiempo mínimo de espera si es una transición temporal
     * <p>
     * Si la transición está sensibilizada por marcado pero aún no ha alcanzado
     * su tiempo mínimo, este metodo hará que el hilo espere lo necesario.
     *
     * @param transicion Índice de la transición a verificar
     * @return true si la transición está sensibilizada (o lo estará después de esperar),
     * false si no está sensibilizada por marcado
     */
    private boolean estaTransicionSensibilizada(int transicion) {
        // Verifica primero si está sensibilizada por marcado
        if (redDePetri.estaTransicionSensibilizada(transicion)) {
            // Obtiene el tiempo actual
            long tiempoActual = System.currentTimeMillis();

            // Verifica si está antes de su ventana temporal
            if (estaAntesDeVentanaTemporal(transicion, tiempoActual)) {
                // Calcula cuánto tiempo debe esperar para alcanzar el límite inferior
                long tiempoEspera = redDePetri.getTimeStamp(transicion) +
                        redDePetri.obtenerTiempoMinimo(transicion) -
                        tiempoActual;

                // Espera el tiempo necesario
                esperarTiempoMinimo(transicion, tiempoEspera);
            }

            // La transición está completamente sensibilizada (o lo estará después de esperar)
            return true;
        }

        // No está sensibilizada por marcado
        return false;
    }

    /**
     * Verifica si una transición cumple todas las condiciones para ser disparada.
     *
     * @param transicion Índice de la transición a verificar
     * @return true si la transición no está en espera y está sensibilizada,
     * false si está en espera o no está sensibilizada
     */
    private boolean puedeDispararse(int transicion) {
        // Verifica que la transición no esté en espera y que esté sensibilizada
        return !redDePetri.transicionEnEspera(transicion) &&
                estaTransicionSensibilizada(transicion);
    }

    // ===================================================================================
    // Métodos para gestión de sincronización
    // ===================================================================================

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
     * Si hay alguna transición sensibilizada con procesos en cola, despierta uno de esos procesos.
     * Si no hay transiciones para despertar, libera el mutex para que otro proceso pueda acceder.
     */
    private void liberarMutex() {
        if (!despertarTransicion()) {
            mutex.release();
        }
        System.out.println(Thread.currentThread().getName() +
                " libero el mutex, permisos: " +
                mutex.availablePermits());
    }

    /**
     * Libera todos los hilos en espera.
     * Se utiliza cuando se han completado todos los disparos requeridos.
     */
    private void liberarHilos() {
        for (int i = 0; i < cola.length; i++) {
            if (cola[i].hasQueuedThreads()) {
                cola[i].release();
            }
        }
    }

    // ===================================================================================
    // Métodos para selección y desbloqueo de transiciones
    // ===================================================================================

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
     * Realiza una operación AND entre las transiciones sensibilizadas y las que están en la cola.
     *
     * @return Un arreglo con las transiciones que cumplen ambas condiciones
     */
    private int[] obtenerTransiciones() {
        // Obtiene las transiciones sensibilizadas y en cola
        long tiempo = System.currentTimeMillis();
        int[] sensibilizadas = redDePetri.getSensibilizadasTiempo(tiempo);
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
     * Utiliza una política para elegir qué proceso despertar.
     * Si no hay una transición que cumpla con las condiciones de la política devuelve la transición T0.
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
     * Verifica si se completaron los disparos requeridos.
     *
     * @return true si se han completado todos los disparos, false en caso contrario
     */
    private boolean disparosCompletados() {
        return redDePetri.getDisparos()[11] >= DISPAROS_TOTALES;
    }
}