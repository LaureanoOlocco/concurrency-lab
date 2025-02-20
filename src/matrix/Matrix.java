package matrix;

import java.util.Arrays;

public final class Matrix {

    private Matrix() {  // Constructor privado para evitar instanciación
        throw new AssertionError("Utility class");
    }

    public static int[] createTransitionMatrix(int t, int transitions) {
        // Initialize matrix with zeros
        int[] transitionMatrix = new int[transitions];
        Arrays.fill(transitionMatrix, 0);
        // Place 1 in the received transition
        transitionMatrix[t] = 1;
        return transitionMatrix;
    }

    public static int[] multiplyMatrices(int[][] incidenceMatrix, int[] transitionMatrix) {
        // Throws exception if matrices don't match
        if (incidenceMatrix[0].length != transitionMatrix.length) {
            throw new IllegalArgumentException("Matrix dimensions are not compatible.");
        }
        // Matrix to store the result
        int[] result = new int[incidenceMatrix.length];
        // Multiply matrices
        for (int i = 0; i < incidenceMatrix.length; i++) {
            for (int j = 0; j < transitionMatrix.length; j++) {
                result[i] += incidenceMatrix[i][j] * transitionMatrix[j];
            }
        }
        return result;
    }

    public static int[] addMatrix(int[] markingMatrix, int[] mMatrix) throws IllegalArgumentException {
        // Throws exception if dimensions are different
        if (markingMatrix.length != mMatrix.length) {
            throw new IllegalArgumentException("Matrices don't have the same dimension: " + markingMatrix.length + " " + mMatrix.length);
        }

        // Matrix to store the result
        int[] result = new int[markingMatrix.length];
        // Add matrices
        for (int i = 0; i < markingMatrix.length; i++) {
            result[i] = markingMatrix[i] + mMatrix[i];
        }

        return result;
    }

}
