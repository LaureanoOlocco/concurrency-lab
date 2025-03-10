package monitor;

import rdp.RedDePetri;

/**
 * Clase que implementa diferentes políticas de selección de transiciones
 * para una Red de Petri. Cada política define una estrategia distinta
 * para elegir qué transición disparar cuando hay múltiples sensibilizadas.
 */
public class Politica {
    /**
     * Enumeración que define los tipos de políticas disponibles
     */
    public enum TipoPolitica {
        POLITICA_BALANCEADA, POLITICA_PROCESAMIENTO_PRIORIZADA
    }

    // Política actualmente seleccionada
    private final static TipoPolitica POLITICA = TipoPolitica.POLITICA_PROCESAMIENTO_PRIORIZADA;

    // Umbrales para la política 2
    private static final double UMBRAL_AGENTE_SUPERIOR = 0.75;
    private static final double UMBRAL_RESERVAS_CONFIRMADAS = 0.80;

    // Índices de transiciones específicas
    private static final int TRANSICION_AGENTE_SUPERIOR = 2;
    private static final int TRANSICION_AGENTE_REGULAR = 3;
    private static final int TRANSICION_RESERVA_CONFIRMADA = 6;
    private static final int TRANSICION_RESERVA_CANCELADA = 7;

    private RedDePetri redDePetri;
    int[] disparos;

    /**
     * Constructor que inicializa la clase con una Red de Petri.
     *
     * @param redDePetri La Red de Petri sobre la que se aplicarán las políticas
     */
    public Politica(RedDePetri redDePetri) {
        this.redDePetri = redDePetri;
        disparos = redDePetri.getDisparos();
    }

    /**
     * Implementa la política balanceada que selecciona la transición sensibilizada
     * con el menor número de disparos previos, asegurando un balance en los disparos.
     *
     * @param transiciones Array resultante de la operación AND entre las transiciones sensibilizadas
     *                     y las transiciones que están en la variable de condición
     * @return Índice de la transición seleccionada por la política
     */
    private int politicaBalanceada(int[] transiciones) {
        int cantDisparos = redDePetri.getMaximosDisparos();
        int transicion = 0;

        for (int i = 0; i < transiciones.length; i++) {
            // Si la transicion está sensibilizada y cumple la variable de condición
            if (transiciones[i] == 1 && disparos[i] < cantDisparos) {
                cantDisparos = disparos[i];
                transicion = i;
            }
        }
        return transicion;
    }

    /**
     * Implementa la política de procesamiento priorizada que establece prioridades específicas
     * entre diferentes tipos de agentes y reservas.
     * Esta política establece prioridades basadas en proporciones:
     * - Prioriza agentes superiores vs. regulares según un umbral del 75%
     * - Prioriza confirmación vs. cancelación de reservas según un umbral del 80%
     *
     * @param transiciones Array resultante de la operación AND entre las transiciones sensibilizadas
     *                     y las transiciones que cumplen la variable de condición
     * @return Índice de la transición seleccionada
     */
    private int politicaProcesamientoPriorizada(int[] transiciones) {
        // Cálculo de totales para evitar división por cero
        int totalAtendidos = disparos[TRANSICION_AGENTE_SUPERIOR] + disparos[TRANSICION_AGENTE_REGULAR];
        int totalReservas = disparos[TRANSICION_RESERVA_CONFIRMADA] + disparos[TRANSICION_RESERVA_CANCELADA];

        if (totalAtendidos == 0) totalAtendidos = 1;
        if (totalReservas == 0) totalReservas = 1;

        // Cálculo de proporciones para toma de decisiones
        double agenteSuperior = disparos[TRANSICION_AGENTE_SUPERIOR] / (double) totalAtendidos;
        double reservasConfirmadas = disparos[TRANSICION_RESERVA_CONFIRMADA] / (double) totalReservas;

        // PRIORIDAD 1: Prioriza al agente de reservas superior según proporción actual
        if (transiciones[TRANSICION_AGENTE_SUPERIOR] == 1 || transiciones[TRANSICION_AGENTE_REGULAR] == 1) {
            if (agenteSuperior <= UMBRAL_AGENTE_SUPERIOR && transiciones[TRANSICION_AGENTE_SUPERIOR] == 1) {
                // Si la proporción de agentes superiores es baja, priorizamos agentes superiores
                return TRANSICION_AGENTE_SUPERIOR;
            } else if (agenteSuperior > UMBRAL_AGENTE_SUPERIOR && transiciones[TRANSICION_AGENTE_REGULAR] == 1) {
                // Si la proporción de agentes superiores es alta, priorizamos agentes regulares
                return TRANSICION_AGENTE_REGULAR;
            }
        }

        // PRIORIDAD 2: Prioriza la confirmación de reservas según proporción actual
        if (transiciones[TRANSICION_RESERVA_CONFIRMADA] == 1 || transiciones[TRANSICION_RESERVA_CANCELADA] == 1) {
            if (reservasConfirmadas <= UMBRAL_RESERVAS_CONFIRMADAS && transiciones[TRANSICION_RESERVA_CONFIRMADA] == 1) {
                // Si la proporción de reservas confirmadas es baja, priorizamos confirmaciones
                return TRANSICION_RESERVA_CONFIRMADA;
            } else if (reservasConfirmadas > UMBRAL_RESERVAS_CONFIRMADAS && transiciones[TRANSICION_RESERVA_CANCELADA] == 1) {
                // Si la proporción de reservas confirmadas es alta, priorizamos cancelaciones
                return TRANSICION_RESERVA_CANCELADA;
            }
        }

        // PRIORIDAD 3: Si no se cumplen las prioridades anteriores, buscamos otras transiciones
        // Listado de transiciones sin criterios de prioridad específicos
        int[] transicionesSinPrioridad = {0, 1, 4, 5, 8, 9, 10, 11};
        for (int i : transicionesSinPrioridad) {
            if (transiciones[i] == 1) {
                return i;
            }
        }

        // En caso de que ninguna esté sensibilizada o no se cumplan las prioridades retorna 0
        return 0;
    }

    /**
     * Selecciona qué política aplicar según la configuración actual.
     *
     * @param t Array que indica qué transiciones están sensibilizadas
     * @return Índice de la transición seleccionada según la política activa
     * @throws IllegalArgumentException sí se especifica una política no implementada
     */
    public int elegirPolitica(int[] t) {
        if (POLITICA == TipoPolitica.POLITICA_BALANCEADA) {
            return politicaBalanceada(t);
        } else if (POLITICA == TipoPolitica.POLITICA_PROCESAMIENTO_PRIORIZADA) {
            return politicaProcesamientoPriorizada(t);
        } else {
            throw new IllegalArgumentException("No existe esa politica");
        }
    }
}

