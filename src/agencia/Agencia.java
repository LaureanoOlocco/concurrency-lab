package agencia;

import java.util.Arrays;

/**
 * Clase que representa una Agencia, la cual controla y registra disparos
 * de transiciones en un sistema.
 * <p>
 * Esta clase mantiene un contador para cada transición y verifica
 * si se ha alcanzado el límite total de disparos permitidos.
 */
public class Agencia {
    /**
     * Número máximo de disparos totales permitidos
     */
    private static final int DISPAROS_TOTALES = 186;

    /**
     * Contador de disparos en la posición final
     */
    private int disparosFinales;

    /**
     * Array que almacena el número de disparos por cada transición
     */
    private final int[] disparoPorTransicion;

    /**
     * Constructor de la clase Agencia.
     * Inicializa el array de disparos por transición con ceros
     * y establece el contador de disparos finales.
     */
    public Agencia() {
        // Inicializar array para 12 transiciones (0-11)
        disparoPorTransicion = new int[12];
        // Llenar el array con ceros
        Arrays.fill(disparoPorTransicion, 0);
        // Inicializar contador de disparos finales
        disparosFinales = disparoPorTransicion[11];
    }

    /**
     * Verifica si se ha alcanzado el límite de disparos totales.
     *
     * @return true si se ha alcanzado o superado el límite, false en caso contrario
     */
    public synchronized boolean limiteAlcanzado() {
        // Actualizar el contador de disparos finales
        disparosFinales = getDisparoPorTransicion()[11];
        // Verificar si se ha alcanzado el límite
        return disparosFinales >= DISPAROS_TOTALES;
    }

    /**
     * Incrementa el contador de disparos para una transición específica.
     *
     * @param t Índice de la transición disparada (0-11)
     */
    public synchronized void transicionDisparada(int t) {
        disparoPorTransicion[t]++;
    }

    /**
     * Obtiene el array con el número de disparos por transición.
     *
     * @return Array con el número de disparos por cada transición
     */
    public int[] getDisparoPorTransicion() {
        return disparoPorTransicion;
    }
}