package monitor;

import java.util.concurrent.Semaphore;

import rdp.RedDePetri;
import logger.Log;

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

    private static final Log loggerThread = Log.getInstancia();

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


    /**
     * Constructor privado que inicializa los componentes del monitor.
     * Es privado debido a la implementación del patrón Singleton.
     */
    private Monitor() {
        mutex = new Semaphore(1);

        // La cola de semáforos se inicializa con 0 permisos para que los hilos
        // que intenten adquirirlos queden bloqueados hasta que sean liberados
        // explícitamente por otro hilo
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
     * <p>
     * Este ciclo do-while controla los intentos de disparo de la transición.
     * Se ejecutará mientras se cumplan estas dos condiciones:
     * 1. No se haya podido efectuar el disparo de la transición (! DisparoEfectuado)
     * 2. No se hayan completado todos los disparos requeridos (! DisparosCompletados())
     *
     * @param transicion Número de transición a disparar
     * @return true si el disparo se completó con éxito, false en caso contrario
     */
    public boolean fireTransition(int transicion) {

        //Adquiere el mutex dispara y libera el mutex, el resto lo hace sin mutex.
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
     * Si la transición está sensibilizada por marcado, pero aún no ha alcanzado
     * su tiempo mínimo, este metodo hará que el hilo espere lo necesario.
     *
     * @param transicion Índice de la transición a verificar
     * @return true si la transición está sensibilizada (o lo estará después de esperar),
     * false si no está sensibilizada por marcado
     */
    private boolean verificarMarcadoYTiempo(int transicion) {
        // Verifica primero si está sensibilizada por marcado
        if (redDePetri.estaTransicionSensibilizada(transicion)) {
            // Obtiene el tiempo actual
            long tiempoActual = System.currentTimeMillis();

            // Verifica si está antes de su ventana temporal
            if (estaAntesDeVentanaTemporal(transicion, tiempoActual)) {
                // Calcula cuánto tiempo debe esperar para alcanzar el límite inferior
                long tiempoEspera = redDePetri.getTimeStamp(transicion) + redDePetri.obtenerTiempoMinimo(transicion) - tiempoActual;

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
        return !redDePetri.transicionEnEspera(transicion) && verificarMarcadoYTiempo(transicion);
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

        if (disparosCompletados()) {
            if (!liberarHilos()) {
                loggerThread.setTdisparadas(redDePetri.getTransicionesDisparadas());
                loggerThread.setDisparoPorT(redDePetri.getDisparos());
                loggerThread.setInvariantes(redDePetri.invariantesTransicion());
            }
        }

        if (!despertarTransicion()) {
            mutex.release();
        }
        System.out.println(Thread.currentThread().getName() +
                " libero el mutex, permisos: " +
                mutex.availablePermits());
    }

    /**
     * Intenta liberar un hilo que esté esperando en alguno de los semáforos de cola.
     * Recorre todos los semáforos y libera el primer hilo que encuentra en espera.
     * <p>
     * Este metodo se utiliza cuando se han completado los disparos requeridos para
     * permitir que los hilos salgan de su estado de espera de manera controlada.
     *
     * @return true si no había hilos en espera (todos los semáforos vacíos),
     * false si se encontró y liberó un hilo (al menos un semáforo tenía hilos)
     */
    private boolean liberarHilos() {
        for (int i = 0; i < cola.length; i++) {
            if (cola[i].hasQueuedThreads()) {
                cola[i].release();
                return false;
            }
        }

        return true;
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
     * Obtiene un arreglo que indica qué transiciones están tanto sensibilizadas como en espera en la cola.
     * Realiza una operación lógica AND entre el conjunto de transiciones sensibilizadas en el momento actual
     * y aquellas que tienen procesos en espera (en cola).
     *
     * @return Un arreglo binario donde cada posición indica si la transición correspondiente está
     * sensibilizada y en cola (1 si cumple ambas condiciones, 0 en caso contrario).
     */
    private int[] obtenerTransiciones() {
        long tiempoActual = System.currentTimeMillis();

        // Transiciones que están sensibilizadas en el momento actual
        int[] transicionesSensibilizadas = redDePetri.getSensibilizadasTiempo(tiempoActual);

        // Transiciones que tienen procesos esperando en la cola
        int[] transicionesEnEspera = transicionesEnCola();

        int[] transicionesDisponibles = new int[TRANSICIONES_TOTALES];

        // Combina ambas condiciones mediante una operación AND
        for (int i = 0; i < TRANSICIONES_TOTALES; i++) {
            transicionesDisponibles[i] = transicionesSensibilizadas[i] & transicionesEnEspera[i];
        }

        return transicionesDisponibles;
    }

    /**
     * Intenta despertar un proceso que está en espera en la cola, utilizando una política de selección.
     * Primero obtiene las transiciones que están activas (sensibilizadas) y con procesos en espera.
     * Luego, aplica una política para decidir cuál transición disparar. Si la transición seleccionada
     * tiene procesos en espera, se libera para que uno de ellos continúe su ejecución.
     *
     * @return true si se despertó un proceso, false si no había procesos para despertar o
     * ninguna transición cumplía con los criterios.
     */
    private boolean despertarTransicion() {
        // Obtiene las transiciones que están sensibilizadas y en cola
        int[] transicionesDisponibles = obtenerTransiciones();

        // Aplica la política para elegir qué transición disparar
        int transicionElegida = politica.elegirPolitica(transicionesDisponibles);

        // Verifica si hay procesos esperando en la cola de esa transición
        if (cola[transicionElegida].hasQueuedThreads()) {
            cola[transicionElegida].release(); // Despierta al proceso
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