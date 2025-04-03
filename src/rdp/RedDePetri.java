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

    private final static int[] RESULTADOS_IP = {
            1, 5, 1, 1, 1, 5
    };

    // Configuración inicial de la red
    private final static int[] MARCADO_INICIAL = {
            5, 1, 0, 0, 5, 0, 1, 1, 0, 0, 1, 0, 0, 0, 0
    };

    /**
     * Matriz de Incidencia (W) de la red de Petri.
     * <p>
     * Esta matriz representa la estructura de la red y se utiliza para calcular
     * los cambios en el marcado cuando se disparan las transiciones.
     * <p>
     * La matriz de incidencia se construye como W = W⁺ - W⁻ donde:
     * W⁺ es la matriz de incidencia de salida (arcos que van de transiciones a plazas)
     * W⁻ es la matriz de incidencia de entrada (arcos que van de plazas a transiciones)
     */

    private final static int[][] MATRIZ_DE_INCIDENCIA = {
            // T0 T1 T2 T3 T4 T5 T6 T7 T8 T9 T10 T11
            {-1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},  // P0
            {-1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},  // P1
            {1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},  // P2
            {0, 1, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0}, // P3
            {-1, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0},  // P4
            {0, 0, 1, 0, 0, -1, 0, 0, 0, 0, 0, 0},  // P5
            {0, 0, -1, 0, 0, 1, 0, 0, 0, 0, 0, 0},  // P6
            {0, 0, 0, -1, 1, 0, 0, 0, 0, 0, 0, 0},  // P7
            {0, 0, 0, 1, -1, 0, 0, 0, 0, 0, 0, 0},  // P8
            {0, 0, 0, 0, 1, 1, -1, -1, 0, 0, 0, 0}, // P9
            {0, 0, 0, 0, 0, 0, -1, -1, 1, 0, 1, 0}, // P10
            {0, 0, 0, 0, 0, 0, 1, 0, 0, -1, 0, 0},  // P11
            {0, 0, 0, 0, 0, 0, 0, 1, -1, 0, 0, 0},  // P12
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 1, -1, 0},  // P13
            {0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, -1}   // P14
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
    private final int[] transicionesTemporales = {
            1, 4, 5, 8, 9, 10
    };


    private int[] marcado;
    private int[] disparos;
    private long[] alfa;            // Tiempo mínimo de sensibilización
    private long[] beta;            // Tiempo máximo de sensibilización
    private long[] timeStamp;       // Marca de tiempo de sensibilización
    private long tiempo;            // Tiempo actual del sistema
    private boolean[] tEsperando;   // Transiciones en espera de disparo
    private String transicionesDisparadas;     // Registro de transiciones disparadas

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

        transicionesDisparadas = "";

        // Configuración de tiempos de transiciones
        setTiempos(3);
    }

    /**
     * Intenta disparar (ejecutar) una transición en una red de Petri.
     * <p>
     * Este metodo implementa la ecuación fundamental de las redes de Petri:
     * M' = M + W·s
     * Donde:
     * - M' es el nuevo marcado (nuevoMarcado)
     * - M es el marcado actual (marcado)
     * - W es la matriz de incidencia (MATRIZ_DE_INCIDENCIA)
     * - S es el vector característico de la secuencia de disparo (matrizTransicion)
     * que contiene un 1 en la posición de la transición a disparar y 0 en las demás
     * <p>
     * El metodo verifica que:
     * 1. La transición esté sensibilizada (tenga suficientes tokens en sus plazas de entrada)
     * 2. El disparo mantenga los invariantes de plaza (restricciones estructurales de la red)
     *
     * @param t Índice de la transición a disparar (0-indexado)
     * @return true si el disparo fue exitoso (transición sensibilizada y se mantienen los invariantes),
     * false en caso contrario (transición no sensibilizada o se violan los invariantes)
     * @note Asume que t está dentro del rango válido [0, TRANSICIONES_TOTALES-1]
     */
    public boolean disparar(int t, boolean puedeDisparar) {

        // Crear el vector característico con un 1 en la posición t
        int[] matrizTransicion = crearMatrizTransicion(t, TRANSICIONES_TOTALES);

        // Calcular W·S (producto de la matriz de incidencia por el vector característico)
        int[] Ws = multiplicarMatriz(MATRIZ_DE_INCIDENCIA, matrizTransicion);

        // Calcular el nuevo marcado: M' = M + W·S
        int[] nuevoMarcado = sumarMatriz(marcado, Ws);

        // Verificar que el nuevo marcado tenga tokens
        if (puedeDisparar) {

            // Verificar que se mantengan los invariantes de plaza
            if (invariantesPlaza(nuevoMarcado)) {
                // Llama al metodo actualizar red.
                actualizarRed(t, nuevoMarcado);
                return true;

            } else {
                System.out.println("Error con los invariantes de plaza");
                for (int i = 0; i < marcado.length; i++) {
                    System.out.print(nuevoMarcado[i] + " ");
                }
                System.out.println();
                return false;
            }

        } else {
            System.out.println("La transición " + t + " no está sensibilizada");
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
     * Configura los tiempos de las transiciones temporales según la configuración seleccionada.
     *
     * @param configuracionTemporal Configuración temporal a utilizar:
     *                              1 - Configuración rápida
     *                              2 - Configuración media
     *                              3 - Configuración lenta
     *                              otro - Sin tiempos (todos en cero)
     */
    private void setTiempos(int configuracionTemporal) {
        alfa = switch (configuracionTemporal) {
            case 1 -> new long[]{0, 3, 0, 0, 5, 5, 0, 0, 7, 3, 3, 0};        // Configuración rápida
            case 2 -> new long[]{0, 10, 0, 0, 15, 15, 0, 0, 20, 10, 12, 0};  // Configuración media
            case 3 -> new long[]{0, 30, 0, 0, 50, 50, 0, 0, 70, 30, 35, 0};  // Configuración lenta
            default -> new long[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};       // Sin restricciones temporales
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
            sensibilizadas[i] = tieneTokens(marcadoPosible) ? 1 : 0;
        }
        return sensibilizadas;
    }

    /**
     * Obtiene las transiciones sensibilizadas por tiempo.
     *
     * @param tiempo Tiempo actual
     * @return Array con 1 en las posiciones de transiciones sensibilizadas por tiempo, 0 en el resto
     */
    public int[] getSensibilizadasTiempo(long tiempo) {
        int[] sensibilizadas = getSensibilizadas();
        int[] sensibilizadasTiempo = new int[TRANSICIONES_TOTALES];
        for (int i = 0; i < TRANSICIONES_TOTALES; i++) {
            sensibilizadasTiempo[i] = (sensibilizadas[i] == 1 && estaSensibilizadaEnTiempo(i, tiempo)) ? 1 : 0;
        }
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
    private boolean tieneTokens(int[] marcado) {
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
        boolean[] invariantesCumplidos = new boolean[RESULTADOS_IP.length];

        // Calcula la suma para cada invariante
        for (int i = 0; i < INVARIANTES_DE_PLAZA.length; i++) {
            for (int inv : INVARIANTES_DE_PLAZA[i]) {
                sumaInvariantes[i] += marcado[inv];
            }
            invariantesCumplidos[i] = sumaInvariantes[i] == RESULTADOS_IP[i];
        }

        // Verifica que todos los invariantes se cumplan
        boolean respetaInvariantes = true;
        for (boolean resultado : invariantesCumplidos) {
            respetaInvariantes = resultado && respetaInvariantes;
        }

        return respetaInvariantes;
    }

    /**
     * Actualiza el estado de la red después de un disparo.
     *
     * @param t            Transición disparada
     * @param nuevoMarcado Nuevo marcado después del disparo
     */
    private void actualizarRed(int t, int[] nuevoMarcado) {
        // Guarda el estado de sensibilización actual
        int[] sensibilizadasActual = getSensibilizadas();

        // Actualiza el marcado
        marcado = nuevoMarcado;

        // Aumenta la cuenta de disparos de esa transición
        disparos[t]++;

        // Registra la transición disparada
        transicionesDisparadas += "T" + t + " ";

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
     * @param t    Índice de la transición
     * @param time Tiempo a establecer
     */
    public void setTimeStamp(int t, long time) {
        timeStamp[t] = time;
    }

    /**
     * Establece el estado de espera de una transición.
     * <p>
     * Este metodo actualiza el array de estados de espera (tEsperando) que indica
     * si una transición está actualmente en proceso de espera debido a restricciones
     * temporales. Este estado puede ser consultado por otros componentes del sistema
     * para determinar si una transición está temporalmente bloqueada.
     *
     * @param transicion El índice de la transición a modificar (0-indexado)
     * @param estado     true si la transición debe marcarse como en espera,
     *                   false si la transición ya no está en espera
     */
    public void setEstadoDeEspera(int transicion, boolean estado) {
        tEsperando[transicion] = estado;
    }

    /**
     * Verifica si una transición es temporal.
     *
     * @param transicion Índice de la transición a verificar
     * @return true si la transición es temporal, false en caso contrario
     */
    public boolean esTransicionTemporal(int transicion) {
        int[] transicionesTemporales = this.transicionesTemporales;

        // Busca en el array de transiciones temporales
        for (int t : transicionesTemporales) {
            if (transicion == t) {
                return true; // La transición tiene restricciones temporales
            }
        }

        // La transición no está en la lista de transiciones temporales
        return false;
    }

    /**
     * Verifica si una transición está sensibilizada en tiempo.
     *
     * @param transicion   Índice de la transición a verificar
     * @param tiempoActual Tiempo actual en milisegundos
     * @return true si ha pasado el tiempo mínimo (alfa) desde su sensibilización,
     * false si aún no alcanza el tiempo mínimo
     */
    public boolean estaSensibilizadaEnTiempo(int transicion, long tiempoActual) {
        // Verifica si el tiempo transcurrido desde la marca de tiempo es mayor o igual que alfa
        return (tiempoActual - timeStamp[transicion]) >= alfa[transicion];
    }

    /**
     * Verifica si una transición específica está sensibilizada según el marcado actual.
     *
     * @param t Índice de la transición a verificar
     * @return true si la transición está sensibilizada, false en caso contrario
     */
    public boolean estaTransicionSensibilizada(int t) {
        return getSensibilizadas()[t] == 1;
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
     * Obtiene el tiempo mínimo (alfa) de la ventana temporal de una transición.
     *
     * @param transicion Índice de la transición
     * @return Tiempo mínimo en milisegundos para la transición especificada
     */
    public long obtenerTiempoMinimo(int transicion) {
        return alfa[transicion];
    }

    public long getTimeStamp(int t) {
        return timeStamp[t];
    }

    private int[][] getInvariantesDeTransicion() {
        return INVARIANTES_DE_TRANSICION;
    }

    /**
     * Obtiene el registro de transiciones disparadas.
     *
     * @return Cadena con las transiciones disparadas
     */
    public String getTransicionesDisparadas() {
        return transicionesDisparadas;
    }
}