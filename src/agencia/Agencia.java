package agencia;

import java.util.Arrays;

public class Agencia {
    private static final int DISPAROS_TOTALES = 186;
    private int disparosFinales;
    private int[] disparoPorTransicion;

    public Agencia() {
        disparoPorTransicion = new int[12];
        Arrays.fill(disparoPorTransicion, 0);
        disparosFinales = disparoPorTransicion[11];
    }

    public synchronized void aumentarDisparos() throws InterruptedException{
        disparosFinales++;
        if (disparosFinales >= DISPAROS_TOTALES) {
            throw new InterruptedException("Se alcanzaron los disparos maximos");
        }
    }

    public synchronized boolean limiteAlcanzado() {
        disparosFinales = getDisparoPorTransicion()[11];
        return disparosFinales >= DISPAROS_TOTALES;
    }

    public synchronized void transicionDisparada(int t) {
        disparoPorTransicion[t]++;
    }

    public int[] getDisparoPorTransicion() {
        return disparoPorTransicion;
    }
}
