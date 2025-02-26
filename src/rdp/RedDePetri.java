package rdp;

import java.util.Arrays;
import static matriz.Matriz.*;

/**
 * Clase que implementa una Red de Petri con transiciones temporales.
 */
public class RedDePetri {

    // Constantes
    private final static int TRANSICIONES_TOTALES = 12;
    private final static long INF = 1000000000;
    private final static int[] RESULTADOS_IP = {1, 5, 1, 1, 1, 5};

    // Configuración inicial de la red
    private final static int[] MARCADO_INICIAL = {
            5, 1, 0, 0, 5, 0, 1, 1, 0, 0, 1, 0, 0, 0, 0
    };

    private final static int[][] MATRIZ_DE_INCIDENCIA = {
            {-1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
            {-1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 1, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0},
            {-1, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 1, 0, 0, -1, 0, 0, 0, 0, 0, 0},
            {0, 0, -1, 0, 0, 1, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, -1, 1, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 1, -1, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 1, 1, -1, -1, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, -1, -1, 1, 0, 1, 0},
            {0, 0, 0, 0, 0, 0, 1, 0, 0, -1, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 1, -1, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 1, -1, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, -1}
    };

    private final static int[][] INVARIANTES_DE_TRANSICION = {
            {0, 1, 3, 4, 7, 8, 11},
            {0, 1, 3, 4, 6, 9, 10, 11},
            {0, 1, 2, 5, 7, 8, 11},
            {0, 1, 2, 5, 6, 9, 10, 11}
    };

    private final static int[][] INVARIANTES_DE_PLAZA = {
            {1, 2},
            {2, 3, 4},
            {5, 6},
            {7, 8},
            {10, 11, 12, 13},
            {0, 2, 3, 5, 8, 9, 11, 12, 13, 14}
    };

    // Atributos de la instancia
    private final int[] tTemporales = {1, 4, 5, 8, 9, 10};
    private int[] marcado;
    private int[] disparos;
    private long[] alfa;            // Tiempo mínimo de sensibilización
    private long[] beta;            // Tiempo máximo de sensibilización
    private long[] timeStamp;       // Marca de tiempo de sensibilización
    private long tiempo;            // Tiempo actual del sistema
    private boolean[] tEsperando;   // Transiciones en espera de disparo
    private String tDisparadas;     // Registro de transiciones disparadas

    /**
     * Constructor de la Red de Petri.
     * Inicializa todos los componentes de la red.
     */
    public RedDePetri() {
        inicializarRed();
    }

    /**
     * Inicializa todos los componentes de la red.
     */
    private void inicializarRed() {
        setMarcado(MARCADO_INICIAL);
        tiempo = System.currentTimeMillis();

        // Inicialización de contadores de disparos
        disparos = new int[TRANSICIONES_TOTALES];
        Arrays.fill(disparos, 0);

        // Inicialización de ventanas temporales
        alfa = new long[TRANSICIONES_TOTALES];
        Arrays.fill(alfa, 0);

        beta = new long[TRANSICIONES_TOTALES];
        Arrays.fill(beta, INF);

        // Inicialización de marcas de tiempo
        timeStamp = new long[TRANSICIONES_TOTALES];
        Arrays.fill(timeStamp, tiempo);

        // Inicialización de estados de espera
        tEsperando = new boolean[TRANSICIONES_TOTALES];
        Arrays.fill(tEsperando, false);

        tDisparadas = "";

        // Configuración de tiempos de transiciones
        setTiempos(1);
    }

    /**
     * Intenta disparar una transición.
     *
     * @param t Índice de la transición a disparar
     * @param sensibilizada Indica si la transición está sensibilizada
     * @return true si el disparo fue exitoso, false en caso contrario
     */
    public boolean disparar(int t, boolean sensibilizada) {
        // Calcula el marcado posible después del disparo
        int[] matrizTransicion = crearMatrizTransicion(t, TRANSICIONES_TOTALES);
        int[] marcadoPosible = multiplicarMatriz(MATRIZ_DE_INCIDENCIA, matrizTransicion);
        marcadoPosible = sumarMatriz(marcado, marcadoPosible);

        // Verifica si está sensibilizada
        if (sensibilizada) {
            // Verifica si cumple invariantes de plaza
            if (invariantesPlaza(marcadoPosible)) {
                // Actualiza el estado de la red
                actualizarRed(t, marcadoPosible);
                return true;
            } else {
                System.out.println("Error con los invariantes de plaza");
                return false;
            }
        } else {
            // Maneja el caso cuando la transición no está sensibilizada
            if (transicionEnEspera(t)) {
                System.out.println("Ya hay una transición esperando disparar");
            } else {
                System.out.println("La transición " + t + " no está sensibilizada");
            }
            return false;
        }
    }

    /**
     * Establece el marcado de la red.
     *
     * @param marcado Nuevo marcado
     */
    private void setMarcado(int[] marcado) {
        this.marcado = marcado;
    }

    /**
     * Configura los tiempos de las transiciones temporales.
     *
     * @param i Perfil de tiempos a utilizar
     */
    private void setTiempos(int i) {
        alfa = switch (i) {
            case 1 -> new long[]{0, 3, 0, 0, 5, 5, 0, 0, 7, 3, 3, 0};
            case 2 -> new long[]{0, 10, 0, 0, 15, 15, 0, 0, 20, 10, 12, 0};
            case 3 -> new long[]{0, 30, 0, 0, 50, 50, 0, 0, 70, 30, 35, 0};
            default -> new long[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        };
    }

    /**
     * Obtiene las transiciones sensibilizadas.
     *
     * @return Array con 1 en las posiciones de transiciones sensibilizadas, 0 en el resto
     */
    public int[] getSensibilizadas() {
        int[] sensibilizadas = new int[TRANSICIONES_TOTALES];

        for (int i = 0; i < TRANSICIONES_TOTALES; i++) {
            int[] matrizTransicion = crearMatrizTransicion(i, TRANSICIONES_TOTALES);
            int[] marcadoPosible = multiplicarMatriz(MATRIZ_DE_INCIDENCIA, matrizTransicion);
            marcadoPosible = sumarMatriz(marcado, marcadoPosible);
            sensibilizadas[i] = estaSensibilizada(marcadoPosible) ? 1 : 0;
        }
        return sensibilizadas;
    }

    /**
     * Obtiene las transiciones sensibilizadas por tiempo.
     *
     * @param time Tiempo actual
     * @return Array con 1 en las posiciones de transiciones sensibilizadas por tiempo, 0 en el resto
     */
    public int[] getSensibilizadasTiempo(long time) {
        int[] sensibilizadas = getSensibilizadas();
        int[] sensibilizadasTiempo = new int[TRANSICIONES_TOTALES];
        // Método incompleto - se debe implementar la lógica de tiempo
        // for (int i = 0; i < TRANSICIONES_TOTALES; i++) {
        //     sensibilizadasTiempo[i] = (sensibilizadas[i] == 1 && isSensibilizadaTiempo(i, time)) ? 1 : 0;
        // }
        return sensibilizadasTiempo;
    }

    /**
     * Obtiene el número máximo de disparos realizados por cualquier transición.
     *
     * @return Número máximo de disparos
     */
    public int getMaximosDisparos() {
        int maximo = disparos[0];
        // Busca el máximo valor de disparos
        for (int i = 1; i < disparos.length; i++) {
            if (disparos[i] > maximo) {
                maximo = disparos[i];
            }
        }
        return maximo;
    }

    /**
     * Verifica si un marcado sensibiliza alguna transición.
     *
     * @param marcado Marcado a verificar
     * @return true si el marcado sensibiliza alguna transición, false en caso contrario
     */
    private boolean estaSensibilizada(int[] marcado) {
        // Verifica si la transición está sensibilizada (todos los lugares tienen suficientes tokens)
        for (int tokens : marcado) {
            if (tokens < 0) {
                return false; // Si hay una plaza que no tiene tokens, la transición no está sensibilizada
            }
        }
        return true; // Todas las plazas tienen suficientes tokens
    }

    /**
     * Verifica que se respeten los invariantes de plaza.
     *
     * @param marcado Marcado a verificar
     * @return true si se respetan todos los invariantes, false en caso contrario
     */
    private boolean invariantesPlaza(int[] marcado) {
        // Array para almacenar suma resultados
        int[] sumaInvariantes = new int[RESULTADOS_IP.length];
        Arrays.fill(sumaInvariantes, 0);

        // boolean que almacena los invariantes
        boolean[] invariantes = new boolean[RESULTADOS_IP.length];

        // Calcula la suma para cada invariante
        for (int i = 0; i < INVARIANTES_DE_PLAZA.length; i++) {
            for (int inv : INVARIANTES_DE_PLAZA[i]) {
                sumaInvariantes[i] += marcado[inv];
            }
            invariantes[i] = sumaInvariantes[i] == RESULTADOS_IP[i];
        }

        // Verifica que todos los invariantes se cumplan
        boolean respetaInvariantes = true;
        for (boolean resultado : invariantes) {
            respetaInvariantes = resultado && respetaInvariantes;
        }

        return respetaInvariantes;
    }

    /**
     * Actualiza el estado de la red después de un disparo.
     *
     * @param t Transición disparada
     * @param marcadoPosible Nuevo marcado después del disparo
     */
    private void actualizarRed(int t, int[] marcadoPosible) {
        // Guarda el estado de sensibilización actual
        int[] sensibilizadasActual = getSensibilizadas();

        // Actualiza el marcado
        marcado = marcadoPosible;

        // Aumenta la cuenta de disparos de esa transición
        disparos[t]++;

        // Registra la transición disparada
        tDisparadas += "T" + t + " ";

        // Obtiene las nuevas transiciones sensibilizadas
        int[] nuevasSensibilizadas = getSensibilizadas();

        // Actualiza las marcas de tiempo para transiciones que cambiaron su estado
        for (int i = 0; i < TRANSICIONES_TOTALES; i++) {
            // Verifica si cambió el sensibilizado de alguna transición
            if (sensibilizadasActual[i] != nuevasSensibilizadas[i]) {
                // Actualiza la marca de tiempo
                long tiempo = System.currentTimeMillis();
                setTimeStamp(i, tiempo);
            }
        }
    }

    /**
     * Verifica si una transición está en espera de ser disparada.
     *
     * @param t Índice de la transición
     * @return true si la transición está en espera, false en caso contrario
     */
    public boolean transicionEnEspera(int t) {
        return tEsperando[t];
    }

    /**
     * Establece la marca de tiempo para una transición.
     *
     * @param t Índice de la transición
     * @param time Tiempo a establecer
     */
    public void setTimeStamp(int t, long time) {
        timeStamp[t] = time;
    }

    /**
     * Obtiene el marcado actual de la red.
     *
     * @return Marcado actual
     */
    public int[] getMarcado() {
        return marcado;
    }

    /**
     * Obtiene el contador de disparos para todas las transiciones.
     *
     * @return Array con el número de disparos de cada transición
     */
    public int[] getDisparos() {
        return disparos;
    }

    /**
     * Obtiene el registro de transiciones disparadas.
     *
     * @return Cadena con las transiciones disparadas
     */
    public String getTransicionesDisparadas() {
        return tDisparadas;
    }
}