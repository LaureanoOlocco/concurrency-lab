package logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Clase de registro (Log) que captura y almacena información sobre la ejecución
 * de una Red de Petri.
 * <p>
 * Esta clase implementa:
 * 1. El patrón Singleton para garantizar una única instancia del logger
 * 2. La interfaz Runnable para ejecutarse en un hilo separado
 * 3. Funcionalidad para registrar transiciones disparadas, conteo de disparos
 * y seguimiento de invariantes completados
 * <p>
 * Al finalizar la ejecución, escribe toda la información recopilada en un archivo de texto.
 */
public class Log implements Runnable {

    // ===================================================================================
    // Atributos
    // ===================================================================================

    /**
     * Instancia única del Logger (patrón Singleton)
     */
    private static Log logger;

    /**
     * Writer para escribir en el archivo de log
     */
    private BufferedWriter writer;

    /**
     * Cadena que registra la secuencia de transiciones disparadas
     * en la red de Petri (por ejemplo: "T0 T1 T3 T4...")
     */
    private String tDisparadas = "";

    /**
     * Array que cuenta cuántas veces se ha disparado cada transición
     * El índice corresponde al número de transición (T0, T1, T2...)
     */
    private int[] disparos = new int[12];

    /**
     * Array que cuenta cuántas veces se ha completado cada invariante de transición
     * La Red de Petri tiene 4 invariantes definidos que representan ciclos completos
     */
    private int[] invariantes = new int[4];

    /**
     * Bandera que indica si el proceso ha terminado y se debe generar el log
     */
    private boolean terminado = false;

    // ===================================================================================
    // Constructor y metodo Singleton
    // ===================================================================================

    /**
     * Constructor privado que inicializa el archivo de log.
     * Es privado debido a la implementación del patrón Singleton.
     *
     * @param fileName Nombre del archivo donde se guardará el log
     */
    private Log(String fileName) {
        try {
            this.writer = new BufferedWriter(new FileWriter(fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Metodo que implementa el patrón Singleton para obtener la única instancia del Logger.
     * Si no existe, crea una nueva instancia con el archivo "log.txt".
     *
     * @return La instancia única del Logger
     */
    public synchronized static Log getInstancia() {
        if (logger == null) {
            logger = new Log("log.txt");
        }
        return logger;
    }

    // ===================================================================================
    // Implementación de Runnable
    // ===================================================================================

    /**
     * Metodo principal que se ejecuta cuando este objeto se inicia en un hilo.
     * Espera hasta que se marque como terminado y luego escribe toda la información
     * recopilada en el archivo de log.
     */
    @Override
    public void run() {
        // Espera activa hasta que se indique que el proceso ha terminado
        while (!terminado) {
            try {
                Thread.sleep(5); // Pausa breve para no consumir demasiados recursos
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Una vez terminado, escribe toda la información recopilada en el archivo
        try {
            // Elimina "null" del inicio si existe
            if (tDisparadas.length() > 4 && tDisparadas.substring(0, 4).equals("null")) {
                tDisparadas = tDisparadas.substring(4);
            }

            // Escribe la secuencia completa de transiciones disparadas
            writer.write(tDisparadas);
            writer.newLine();
            writer.newLine();

            // Escribe el conteo de cada transición
            writer.write("-------------------------- Transiciones disparadas --------------------------");
            writer.newLine();
            for (int i = 0; i < disparos.length; i++) {
                writer.write("Transicion " + i + " disparada: " + disparos[i] + " veces.");
                writer.newLine();
            }
            writer.newLine();

            // Escribe estadísticas sobre los invariantes completados
            writer.write("-------------------------- Invariantes completados --------------------------");
            writer.newLine();
            writer.write("Invariante 1: [0 1 3 4 7 8 11] completado: " + invariantes[0] + " veces");
            writer.newLine();
            writer.write("Invariante 2: [0 1 3 4 6 9 10 11] completado: " + invariantes[1] + " veces");
            writer.newLine();
            writer.write("Invariante 3: [0 1 2 5 7 8 11] completado: " + invariantes[2] + " veces");
            writer.newLine();
            writer.write("Invariante 4: [0 1 2 5 6 9 10 11] completado: " + invariantes[3] + " veces");
            writer.newLine();

            // Escribe el total de invariantes completados
            writer.write("Total de invariantes completados: " +
                    (invariantes[0] + invariantes[1] + invariantes[2] + invariantes[3]));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Asegura que el archivo se cierre correctamente
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // ===================================================================================
    // Métodos para actualizar el estado del logger
    // ===================================================================================

    /**
     * Establece la cadena que representa la secuencia de transiciones disparadas.
     *
     * @param t Cadena con formato "T0 T1 T2..." que indica las transiciones disparadas en orden
     */
    public void setTdisparadas(String t) {
        tDisparadas = t;
    }

    /**
     * Establece el contador de disparos para cada transición.
     *
     * @param d Array donde cada posición contiene el número de veces que la transición ha sido disparada
     */
    public void setDisparoPorT(int[] d) {
        disparos = d;
    }

    /**
     * Establece el contador de invariantes completados.
     *
     * @param i Array donde cada posición indica cuántas veces se ha completado cada invariante
     */
    public void setInvariantes(int[] i) {
        invariantes = i;
    }

    /**
     * Marca el proceso como terminado, lo que provocará que el hilo escriba el log y finalice.
     *
     * @param terminado true para indicar que se debe escribir el log y finalizar
     */
    public void setTerminado(boolean terminado) {
        this.terminado = terminado;
    }
}