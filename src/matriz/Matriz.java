package matriz;

import rdp.RedDePetri;

import java.util.Arrays;

/**
 * Clase de utilidad para operaciones con matrices.
 */
public final class Matriz extends RedDePetri {

    private Matriz() {  // Constructor privado para evitar instanciación
        throw new AssertionError("Clase de utilidad");
    }

    /**
     * Crea una matriz de transición con un 1 en la posición especificada.
     *
     * @param t            Posición de la transición
     * @param transiciones Tamaño de la matriz
     * @return Matriz de transición
     * @throws IllegalArgumentException si t es negativo o mayor que transiciones
     */
    public static int[] crearMatrizTransicion(int t, int transiciones) {
        if (t < 0 || t >= transiciones) {
            throw new IllegalArgumentException("Posición de transición fuera de rango: " + t);
        }

        // Inicializar matriz con ceros
        int[] matrizTransicion = new int[transiciones];
        Arrays.fill(matrizTransicion, 0);  // Mantener para mayor claridad

        // Colocar 1 en la transición recibida
        matrizTransicion[t] = 1;
        return matrizTransicion;
    }

    /**
     * Multiplica una matriz de incidencia por una matriz de transición.
     *
     * @param matrizIncidencia Matriz de incidencia
     * @param matrizTransicion Matriz de transición
     * @return Resultado de la multiplicación
     * @throws IllegalArgumentException si las dimensiones no son compatibles
     * @throws NullPointerException     si alguna matriz es nula
     */
    public static int[] multiplicarMatriz(int[][] matrizIncidencia, int[] matrizTransicion) {
        // Validar entradas nulas
        if (matrizIncidencia == null || matrizTransicion == null) {
            throw new NullPointerException("Las matrices no pueden ser nulas");
        }

        // Validar si hay filas en la matriz
        if (matrizIncidencia.length == 0) {
            return new int[0];
        }

        // Lanza excepción si las matrices no coinciden
        if (matrizIncidencia[0].length != matrizTransicion.length) {
            throw new IllegalArgumentException("Las dimensiones de las matrices no son compatibles: " +
                    matrizIncidencia[0].length + " != " + matrizTransicion.length);
        }

        // Matriz para almacenar el resultado
        int[] resultado = new int[matrizIncidencia.length];

        // Multiplicar matrices con optimización para valores cero
        for (int i = 0; i < matrizIncidencia.length; i++) {
            for (int j = 0; j < matrizTransicion.length; j++) {
                // Solo multiplicar cuando el valor de la transición no es cero
                if (matrizTransicion[j] != 0) {
                    resultado[i] += matrizIncidencia[i][j] * matrizTransicion[j];
                }
            }
        }
        return resultado;
    }

    /**
     * Suma dos matrices unidimensionales.
     *
     * @param matrizMarcado Primera matriz
     * @param matrizM       Segunda matriz
     * @return Suma de las matrices
     * @throws IllegalArgumentException si las dimensiones son diferentes
     * @throws NullPointerException     si alguna matriz es nula
     */
    public static int[] sumarMatriz(int[] matrizMarcado, int[] matrizM) {
        // Validar entradas nulas
        if (matrizMarcado == null || matrizM == null) {
            throw new NullPointerException("Las matrices no pueden ser nulas");
        }

        // Lanza excepción si las dimensiones son diferentes
        if (matrizMarcado.length != matrizM.length) {
            throw new IllegalArgumentException("Las matrices no tienen la misma dimensión: " +
                    matrizMarcado.length + " != " + matrizM.length);
        }

        // Matriz para almacenar el resultado
        int[] resultado = new int[matrizMarcado.length];

        // Sumar matrices
        for (int i = 0; i < matrizMarcado.length; i++) {
            resultado[i] = matrizMarcado[i] + matrizM[i];
        }

        return resultado;
    }
}
