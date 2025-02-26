package monitor;

import java.util.concurrent.Semaphore;
import rdp.RedDePetri;

//import logger.LoggerThread;

public class Monitor implements MonitorInterface {


    //private static final LoggerThread logger = LoggerThread.getInstancia();
    private final static int DISPAROS_TOTALES = 186;
    private final static int TRANSICIONES_TOTALES = 12;
    private static Monitor monitor;
    private static Semaphore mutex;
    private static Semaphore[] cola;
    private static RedDePetri redDePetri;
   // private static Politicas politica;


    private Monitor() {
        mutex = new Semaphore(1);
        cola = new Semaphore[TRANSICIONES_TOTALES];
        for (int i = 0; i < TRANSICIONES_TOTALES; i++) {
            cola[i] = new Semaphore(0);
        }
        redDePetri = new RedDePetri();
        //politica = new Politicas(redDePetri);
    }

    public boolean fireTransition(int transition) {
        return false;
    }
}
