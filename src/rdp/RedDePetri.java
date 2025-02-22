package rdp;

import static matrix.Matrix.createTransitionMatrix;
import static matrix.Matrix.multiplyMatrices;
import static matrix.Matrix.addMatrix;
import java.util.Arrays;

public class RedDePetri {

    private final static int TRANSICIONES_TOTALES = 12;
    private final static long INF = 1000000000;
    private final static int[] resultadosIP = {1, 5, 1, 1, 1, 5};
    private final int[] tTemporales = {1, 4, 5, 8, 9, 10};

    private final static int[] MARCADO_INICIAL = {
            5, 1, 0, 0, 5, 0, 1, 1, 0, 0, 1, 0, 0, 0, 0
    };

    private final static int[][] matrizDeIncidencia = {
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

    private final static int[][] invariantesDeTransicion = {
            {0, 1, 3, 4, 7, 8, 11},
            {0, 1, 3, 4, 6, 9, 10, 11},
            {0, 1, 2, 5, 7, 8, 11},
            {0, 1, 2, 5, 6, 9, 10, 11}
    };

    private final static int[][] invariantesDePlaza = {
            {1 ,2},
            {2, 3, 4},
            {5, 6},
            {7, 8},
            {10, 11, 12, 13},
            {0, 2, 3, 5, 8, 9, 11, 12, 13, 14}

    };

    private int[] marcado;
    private int[] disparos;
    private long[] alfa;
    private long[] beta;
    private long[] timeStamp;
    private long tiempo;
    private boolean[] tEsperando;
    private String tDisparadas;

}
