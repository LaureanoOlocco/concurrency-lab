package rdp;

import java.util.Arrays;

import static matriz.Matriz.*;

/**
 * Clase que implementa una Red de Petri con transiciones temporales.
 * <p>
 * Una Red de Petri es un modelo matemático utilizado para la representación
 * de sistemas concurrentes, distribuidos o paralelos. Este implementación
 * extiende el modelo básico con restricciones temporales en las transiciones.
 */
public class RedDePetri {

    // ===================================================================================
    // Constantes de la Red de Petri
    // ===================================================================================

    /**
     * Número total de transiciones en la Red de Petri
     */
    private final static int TRANSICIONES_TOTALES = 12;

    /**
     * Valor que representa "infinito" para los límites temporales superiores (beta)
     */
    private final static long INF = 1000000000;

    /**
     * Resultados esperados para los invariantes de plaza.
     * Cada valor representa la suma de tokens que debe mantenerse constante
     * en cada conjunto de plazas definido como invariante.
     */
    private final static int[] RESULTADOS_IP = {
            1, 5, 1, 1, 1, 5
    };

    /**
     * Configuración inicial de marcado de la red.
     * Define cuántos tokens hay en cada plaza al inicio.
     * El índice corresponde a la plaza (P0, P1, ...) y el valor
     * es el número de tokens iniciales.
     */
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
     * - W⁺ es la matriz de incidencia de salida (arcos que van de transiciones a plazas)
     * - W⁻ es la matriz de incidencia de entrada (arcos que van de plazas a transiciones)
     * <p>
     * Las filas representan plazas (P0, P1, ...) y las columnas transiciones (T0, T1, ...)
     * Un valor negativo indica que esa plaza pierde tokens cuando se dispara la transición
     * Un valor positivo indica que esa plaza gana tokens cuando se dispara la transición
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

    /**
     * Conjuntos de transiciones que forman invariantes.
     * <p>
     * Un invariante de transición es un conjunto de transiciones que,
     * cuando se disparan todas, devuelven la red a su estado original
     * (forman un ciclo). Esto se usa para análisis de propiedades como
     * vivacidad y acotación.
     */
    private final static int[][] INVARIANTES_DE_TRANSICION = {
            {0, 1, 3, 4, 7, 8, 11},
            {0, 1, 3, 4, 6, 9, 10, 11},
            {0, 1, 2, 5, 7, 8, 11},
            {0, 1, 2, 5, 6, 9, 10, 11}
    };

    /**
     * Conjuntos de plazas que forman invariantes.
     * <p>
     * Un invariante de plaza es un conjunto de plazas cuya suma de tokens
     * se mantiene constante independientemente de las transiciones que se disparen.
     * Representan restricciones estructurales del sistema modelado.
     */
    private final static int[][] INVARIANTES_DE_PLAZA = {
            {1, 2},
            {2, 3, 4},
            {5, 6},
            {7, 8},
            {10, 11, 12, 13},
            {0, 2, 3, 5, 8, 9, 11, 12, 13, 14}
    };

    // ===================================================================================
    // Atributos de la instancia
    // ===================================================================================

    /**
     * Lista de transiciones que tienen restricciones temporales.
     * Estas transiciones requieren un tiempo mínimo de sensibilización
     * antes de poder dispararse.
     */
    private final int[] transicionesTemporales = {
            1, 4, 5, 8, 9, 10
    };

    /**
     * Estado actual de la red: tokens en cada plaza.
     * El índice corresponde al número de plaza y el valor es la cantidad de tokens.
     */
    private int[] marcado;

    /**
     * Contador de disparos realizados por cada transición.
     * Útil para estadísticas y condiciones de terminación.
     */
    private int[] disparos;

    /**
     * Tiempo mínimo (en ms) que una transición temporal debe estar sensibilizada
     * antes de poder dispararse. Define el límite inferior de la ventana temporal.
     */
    private long[] alfa;

    /**
     * Tiempo máximo (en ms) que una transición temporal puede estar sensibilizada
     * antes de que deba ser disparada o pierda su sensibilización.
     * Define el límite superior de la ventana temporal.
     */
    private long[] beta;

    /**
     * Marca de tiempo (timestamp) de cuando cada transición se volvió sensibilizada.
     * Se usa para calcular cuánto tiempo ha estado sensibilizada una transición.
     */
    private long[] timeStamp;

    /**
     * Tiempo de referencia del sistema al inicializar la red.
     * Se utiliza como base para cálculos temporales.
     */
    private long tiempo;

    /**
     * Indica qué transiciones están actualmente en espera de ser disparadas
     * debido a restricciones temporales.
     */
    private boolean[] tEsperando;

    /**
     * Registro de las transiciones que han sido disparadas, en orden.
     * Útil para depuración y análisis post-ejecución.
     */
    private String transicionesDisparadas;

    /**
     * Constructor de la Red de Petri.
     * Inicializa todos los componentes de la red.
     */
    public RedDePetri() {
        inicializarRed();
    }

    // ===================================================================================
    // Métodos de inicialización y configuración
    // ===================================================================================

    /**
     * Inicializa todos los componentes de la red.
     * Establece el marcado inicial y configura todas las estructuras
     * relacionadas con el tiempo y estados.
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
        setTiempos(1);
    }

    /**
     * Establece el marcado de la red.
     *
     * @param marcado Nuevo marcado (distribución de tokens en las plazas)
     */
    private void setMarcado(int[] marcado) {
        this.marcado = marcado;
    }

    /**
     * Configura los tiempos de las transiciones temporales según la configuración seleccionada.
     * Establece los valores alfa (tiempo mínimo) para cada transición.
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

    // ===================================================================================
    // Métodos de disparo y actualizaciones de estado
    // ===================================================================================

    /**
     * Intenta disparar (ejecutar) una transición en la red de Petri.
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
     * 1. La transición esté sensibilizada en tokens y en tiempo
     * 2. El disparo mantenga los invariantes de plaza (restricciones estructurales de la red)
     *
     * @param t             Índice de la transición a disparar (0-indexado)
     * @param puedeDisparar Indica si la transición cumple las condiciones temporales para ser disparada
     * @return true si el disparo fue exitoso, false en caso contrario
     */
    public boolean disparar(int t, boolean puedeDisparar) {
        // Crear el vector característico con un 1 en la posición t
        int[] matrizTransicion = crearMatrizTransicion(t, TRANSICIONES_TOTALES);

        // Calcular W·S (producto de la matriz de incidencia por el vector característico)
        int[] Ws = multiplicarMatriz(MATRIZ_DE_INCIDENCIA, matrizTransicion);

        // Calcular el nuevo marcado: M' = M + W·S
        int[] nuevoMarcado = sumarMatriz(marcado, Ws);

        // Verificar si la transición puede dispararse según condiciones temporales
        if (puedeDisparar) {
            // Verificar que se mantengan los invariantes de plaza
            if (invariantesPlaza(nuevoMarcado)) {
                // Actualizar el estado de la red
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
     * Actualiza el estado de la red después de un disparo.
     * Actualiza el marcado, cuenta de disparos, registro de transiciones
     * y las marcas de tiempo para las transiciones que cambiaron su estado.
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

    // ===================================================================================
    // Métodos para verificación de sensibilización
    // ===================================================================================

    /**
     * Obtiene las transiciones sensibilizadas por marcado.
     * Una transición está sensibilizada cuando todas sus plazas de entrada
     * tienen suficientes tokens.
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
     * Obtiene las transiciones sensibilizadas por marcado y tiempo.
     * Una transición está completamente sensibilizada cuando:
     * 1. Está sensibilizada por marcado
     * 2. Ha cumplido su tiempo mínimo de sensibilización (alfa)
     *
     * @param tiempo Tiempo actual en milisegundos
     * @return Array con 1 en posiciones de transiciones completamente sensibilizadas, 0 en el resto
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
     * Verifica si un marcado tiene todos sus valores no negativos.
     * Esto es requisito para que una transición esté sensibilizada.
     *
     * @param marcado Marcado a verificar
     * @return true si todos los valores son no negativos, false si hay algún valor negativo
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
     * Verifica si una transición específica está sensibilizada según el marcado actual.
     *
     * @param t Índice de la transición a verificar
     * @return true si la transición está sensibilizada, false en caso contrario
     */
    public boolean estaTransicionSensibilizada(int t) {
        return getSensibilizadas()[t] == 1;
    }

    /**
     * Verifica si una transición está sensibilizada en tiempo.
     * Es decir, si ha pasado suficiente tiempo desde que se sensibilizó
     * para cumplir con su restricción temporal mínima (alfa).
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
     * Verifica si una transición es temporal (tiene restricciones de tiempo).
     *
     * @param transicion Índice de la transición a verificar
     * @return true si la transición es temporal, false en caso contrario
     */
    public boolean esTransicionTemporal(int transicion) {
        // Busca en el array de transiciones temporales
        for (int t : transicionesTemporales) {
            if (transicion == t) {
                return true; // La transición tiene restricciones temporales
            }
        }
        // La transición no está en la lista de transiciones temporales
        return false;
    }

    // ===================================================================================
    // Métodos para invariantes y restricciones
    // ===================================================================================

    /**
     * Verifica que se respeten los invariantes de plaza en un marcado dado.
     * Los invariantes de plaza representan conjuntos de plazas cuya suma de tokens
     * debe mantenerse constante siempre
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
     * Verifica si todas las transiciones de un invariante han sido disparadas al menos una vez.
     * <p>
     * Este Metodo es utilizado como parte del análisis de invariantes de transición
     * para determinar si un ciclo completo puede haber ocurrido.
     *
     * @param invariante Array con los índices de las transiciones que forman el invariante
     * @param disparos   Array con el conteo de disparos de cada transición
     * @return true si todas las transiciones del invariante han sido disparadas al menos una vez,
     * false si alguna transición no ha sido disparada
     */
    private boolean verificarCombinacion(int[] invariante, int[] disparos) {
        for (int t : invariante) {
            if (disparos[t] == 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calcula cuántas veces se ha completado cada invariante de transición.
     * <p>
     * Un invariante de transición se considera completado cuando todas sus
     * transiciones han sido disparadas al menos una vez. Este metodo utiliza
     * un enfoque iterativo para calcular el número máximo de veces que cada
     * invariante puede haberse completado, basándose en los contadores de disparos.
     *
     * @return Array donde cada posición indica cuántas veces se ha completado
     * el invariante correspondiente
     */
    public int[] invariantesTransicion() {
        int[][] invariantes = getInvariantesDeTransicion();
        int[] disparosTransicion = Arrays.copyOf(disparos, disparos.length);
        int[] vecesCompletada = new int[invariantes.length];
        boolean cambios;

        do {
            cambios = false;
            for (int i = 0; i < invariantes.length; i++) {
                int[] invariante = invariantes[i];
                if (verificarCombinacion(invariante, disparosTransicion)) {
                    // Si todas las transiciones del invariante han sido disparadas,
                    // reducimos el contador de cada una y aumentamos el contador del invariante
                    for (int t : invariante) {
                        disparosTransicion[t]--;
                    }
                    vecesCompletada[i]++;
                    cambios = true;
                }
            }
        } while (cambios);

        return vecesCompletada;
    }

    // ===================================================================================
    // Métodos para estado de espera y tiempo
    // ===================================================================================

    /**
     * Verifica si una transición está en espera de ser disparada.
     * Una transición puede estar en espera por restricciones temporales.
     *
     * @param t Índice de la transición
     * @return true si la transición está en espera, false en caso contrario
     */
    public boolean transicionEnEspera(int t) {
        return tEsperando[t];
    }

    /**
     * Establece la marca de tiempo para una transición.
     * Se actualiza cuando una transición cambia su estado de sensibilización.
     *
     * @param t    Índice de la transición
     * @param time Tiempo a establecer (normalmente el tiempo actual del sistema)
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

    // ===================================================================================
    // Métodos de acceso (getters)
    // ===================================================================================

    /**
     * Obtiene el marcado actual de la red.
     *
     * @return Array con el número de tokens en cada plaza
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
     * Obtiene el número máximo de disparos realizados por cualquier transición.
     * Útil para estadísticas y condiciones de terminación.
     *
     * @return Número máximo de disparos entre todas las transiciones
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
     * Obtiene el tiempo mínimo (alfa) de la ventana temporal de una transición.
     *
     * @param transicion Índice de la transición
     * @return Tiempo mínimo en milisegundos para la transición especificada
     */
    public long obtenerTiempoMinimo(int transicion) {
        return alfa[transicion];
    }

    /**
     * Obtiene la marca de tiempo (timestamp) de cuando una transición se volvió
     * sensibilizada por última vez.
     *
     * @param t Índice de la transición
     * @return Marca de tiempo en milisegundos
     */
    public long getTimeStamp(int t) {
        return timeStamp[t];
    }

    /**
     * Obtiene los invariantes de transición de la red.
     *
     * @return Matriz con los conjuntos de transiciones que forman invariantes
     */
    private int[][] getInvariantesDeTransicion() {
        return INVARIANTES_DE_TRANSICION;
    }

    /**
     * Obtiene el registro de transiciones disparadas.
     *
     * @return Cadena con las transiciones disparadas en orden
     */
    public String getTransicionesDisparadas() {
        return transicionesDisparadas;
    }
}