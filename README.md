# 🏆 Simulador de Agencia de Viajes con Redes de Petri

![Redes de Petri](https://img.shields.io/badge/Modelo-Redes%20de%20Petri-blue)
![Java](https://img.shields.io/badge/Lenguaje-Java-orange)
![Concurrency](https://img.shields.io/badge/Paradigma-Concurrencia-green)
![Status](https://img.shields.io/badge/Estado-Completado-success)

## 📋 Descripción

Implementación de un sistema concurrente que simula las operaciones de una agencia de viajes utilizando **Redes de Petri** como formalismo matemático. El proyecto modela los procesos de ingreso de clientes, gestión de reservas, procesamiento de pagos y confirmación/cancelación de transacciones, permitiendo la ejecución simultánea de operaciones en un entorno multihilo.

## 🎯 Características Principales

- **Modelado formal** con Redes de Petri para representar estados y transiciones del sistema
- **Monitor concurrente** para control de acceso a recursos compartidos
- **Semántica temporal** en transiciones críticas (T1, T4, T5, T8, T9 y T10)
- **Políticas configurables** para resolución de conflictos:
  - Política balanceada (distribución equitativa)
  - Política priorizada (75% agente superior, 80% confirmaciones)
- **Análisis de invariantes** para verificación de propiedades
- **Logging detallado** para monitoreo de ejecución y estadísticas

## 🔍 Análisis del Sistema

### Propiedades Verificadas

| Propiedad | Estado | Descripción |
|-----------|--------|-------------|
| Seguridad | ❌ No | La red no es segura (plazas P0 y P4 pueden acumular múltiples tokens) |
| Deadlock | ✅ No | No existen situaciones de bloqueo en ningún estado alcanzable |
| Vivacidad | ✅ Sí | Todas las transiciones conservan la posibilidad de ser disparadas |

### Invariantes de Transición

- **IT1** = {T0, T1, T3, T4, T7, T8, T11} - *Reserva con agente inferior, pago rechazado*
- **IT2** = {T0, T1, T3, T4, T6, T9, T10, T11} - *Reserva con agente inferior, pago aceptado*
- **IT3** = {T0, T1, T2, T5, T7, T8, T11} - *Reserva con agente superior, pago rechazado*
- **IT4** = {T0, T1, T2, T5, T6, T9, T10, T11} - *Reserva con agente superior, pago aceptado*

## 🏗️ Arquitectura

El proyecto está estructurado en 6 paquetes principales:

- **agencia**: Clases core `Agencia` y `Procesos`
- **logger**: Sistema de registro de eventos
- **matriz**: Operaciones matriciales para simulación
- **monitor**: Control de concurrencia y políticas
- **rdp**: Implementación de la Red de Petri
- **threadfactory**: Creación personalizada de hilos

## 📊 Resultados

### Comparativa de Políticas

#### Política Equitativa
```
Invariante 1: [0 1 3 4 7 8 11] completado: 47 veces
Invariante 2: [0 1 3 4 6 9 10 11] completado: 46 veces
Invariante 3: [0 1 2 5 7 8 11] completado: 46 veces
Invariante 4: [0 1 2 5 6 9 10 11] completado: 47 veces
```

#### Política Priorizada
```
Invariante 1: [0 1 3 4 7 8 11] completado: 19 veces
Invariante 2: [0 1 3 4 6 9 10 11] completado: 28 veces
Invariante 3: [0 1 2 5 7 8 11] completado: 18 veces
Invariante 4: [0 1 2 5 6 9 10 11] completado: 121 veces
```

### Análisis de Tiempos

| Configuración | Política | Peor Tiempo | Mejor Tiempo | Tiempo Promedio |
|---------------|----------|-------------|--------------|-----------------|
| Rápida        | Equitativa | 2.6s      | 0.5s         | 1.5s            |
|               | Balanceada  | 2.6s      | 0.4s         | 1.5s            |
| Media         | Equitativa | 7.6s      | 1.5s         | 4.5s            |
|               | Balanceada  | 8.2s      | 1.6s         | 4.9s            |
| Lenta         | Equitativa | 27.5s     | 5.6s         | 16.5s           |
|               | Balanceada  | 27.1s     | 5.5s         | 16.3s           |

## 🚀 Instalación y Ejecución

1. Clonar el repositorio
   ```bash
   git clone https://github.com/yourusername/agencia-viajes-petri.git
   cd agencia-viajes-petri
   ```

2. Compilar el proyecto
   ```bash
   javac -d bin src/**/*.java
   ```

3. Ejecutar la simulación
   ```bash
   java -cp bin Main
   ```

4. Revisar los logs generados
   ```bash
   cat log.txt
   ```

## 📝 Configuración

La configuración del sistema se realiza mediante los siguientes parámetros ajustables:

- **NUM_DISPAROS**: Define la cantidad total de invariantes a completar (186 por defecto)
- **POLITICA**: Selecciona entre política balanceada (0) o priorizada (1)
- **ALFA_Tx**: Define el tiempo mínimo para las transiciones temporizadas (en ms)
- **BETA_Tx**: Define el tiempo máximo para las transiciones temporizadas (en ms)

---
![RedDePetri](https://github.com/user-attachments/assets/df767a2d-425f-4105-9da7-1c7a75e9b411)
<p align="center">
  <em>Red de Petri modelando el sistema de agencia de viajes</em>
</p>
