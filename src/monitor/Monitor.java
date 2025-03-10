package monitor;

import java.util.concurrent.Semaphore;

import rdp.RedDePetri;

//import logger.LoggerThread;

public class Monitor implements MonitorInterface {
// EL MONITOR DEBE RECIBIR PROCESOS

    //private static final LoggerThread logger = LoggerThread.getInstancia();
    private final static int DISPAROS_TOTALES = 186;
    private final static int TRANSICIONES_TOTALES = 12;
    private static volatile Monitor monitor; // Mantener la consistencia y la visibilidad del objeto Singleton a través de todos los hilos.

    private static Semaphore mutex;
    private static Semaphore[] cola;
    private static RedDePetri redDePetri;
    private static Politica politica;


    private Monitor() {
        mutex = new Semaphore(1);
        cola = new Semaphore[TRANSICIONES_TOTALES];
        for (int i = 0; i < TRANSICIONES_TOTALES; i++) {
            cola[i] = new Semaphore(0);
        }
        redDePetri = new RedDePetri();
        //politica = new Politicas(redDePetri);
    }

    public static Monitor getInstanciaMonitor() {
        if (monitor == null) { // Primera comprobación (sincronización externa)
            synchronized (Monitor.class) { // Actúa Como un candado global para toda la clase. si fuera this cada hilo crearia su propio lock.
                if (monitor == null) { // Segunda comprobación (sincronización interna)
                    monitor = new Monitor();
                }
            }
        }
        return monitor;
    }

    public boolean fireTransition(int transicion) {

        boolean disparoEfectuado = false;
        adquirirMutex();


        do {

            if (disparosCompletados()) {

                mutex.release();
                break;
            }

            disparoEfectuado = redDePetri.disparar(transicion, redDePetri.estaTransicionSensibilizada(transicion));
            mutex.release();

            if (!disparoEfectuado) {

                try {
                    cola[transicion].acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        } while (!disparosCompletados());


        return false;
    }


    private void adquirirMutex() {
        try {
            mutex.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean disparosCompletados() {
        return redDePetri.getDisparos()[11] >= DISPAROS_TOTALES;
    }

    private int[] transicionesEnCola() {
        int[] transiciones = new int[TRANSICIONES_TOTALES];
        for (int i = 0; i < TRANSICIONES_TOTALES; i++) {
            transiciones[i] = cola[i].hasQueuedThreads() ? 1 : 0;
        }
        ;
        return transiciones;
    }

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

}
