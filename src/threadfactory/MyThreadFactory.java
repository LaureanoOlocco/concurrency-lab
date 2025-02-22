package threadfactory;

import java.util.concurrent.ThreadFactory;

public class MyThreadFactory implements ThreadFactory{
    private String nombre;
    private int contador;

    public MyThreadFactory(String nombre){
        this.nombre = nombre;
        contador = 0;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r, nombre + "_Thread_" + contador);
        contador++;
        return t;
    }

}
