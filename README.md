# motor-resolucion-logica-java

Este repositorio contiene la implementación completa de un **Motor de Inferencia Simbólica por Refutación** desarrollado en Java. El sistema es capaz de procesar conocimiento expresado en lógica formal, transformarlo a cláusulas estandarizadas y demostrar teoremas utilizando el **Principio de Resolución de Robinson** junto con un **Algoritmo de Unificación de Variables Simbólicas**.

El diseño del software se rige bajo el paradigma orientado a objetos (POO), estructurando de forma modular y desacoplada cada componente del proceso lógico.

---


Acorde con las especificaciones requeridas en la guía de evaluación, el desarrollo del motor se dividió en dos fases de implementación principales:

### 1. Motor de Inferencia Base (Resolución por Refutación)
- Implementación del núcleo algorítmico del principio de resolución para lógica proposicional/formal base sin variables.
- Estructuración del mecanismo de contradicción: el motor busca derivar de forma iterativa la **cláusula vacía ($\square$)** a partir de la negación del teorema objetivo adicionada a la base de conocimiento.

### 2. Extensión con Algoritmo de Unificación Completa
- Incorporación del algoritmo de unificación de variables de tipo simbólico para dar soporte a Lógica de Primer Orden.
- Capacidad de calcular el unificador más general (*mgu*) emparejando términos, constantes y funciones, aplicando las sustituciones correspondientes en la traza de resolución.
- Generación de una **traza detallada en consola**, permitiendo auditar y observar el paso a paso del proceso de inferencia y las sustituciones realizadas en cada ciclo.

---

## 📂 Estructura del Repositorio

El código fuente se encuentra alojado de manera limpia en la carpeta `src/`, omitiendo archivos binarios locales de compilación (`out/` o `.class`):

```text
motor-resolucion-logica/
├── src/
│   ├── Main.java               # Bucle principal, carga de casos de prueba y flujo
│   ├── MotorDeResolucion.java   # Algoritmo central de inferencia por refutación lógica
│   ├── ConversorFNC.java       # Algoritmo de transformación a Forma Normal Conjuntiva
│   ├── BaseDeConocimiento.java # Estructura que almacena los axiomas, premisas y teoremas
│   ├── Clausula.java           # Representación de una disyunción de literales
│   ├── Literal.java            # Átomo lógico predicativo (positivo o negado)
│   ├── Termino.java            # Representación abstracta de variables, constantes y funciones
│   ├── Unificador.java         # Mecanismo de unificación de primer orden (cálculo de mgu)
│   └── Sustitucion.java        # Estructura de mapeo para la sustitución de variables simbólicas
└── README.md                   # Documentación técnica del proyecto


El sistema requiere contar con el Java JDK 11 o superior instalado localmente.

Instrucciones de Compilación
Desde la raíz del proyecto, abre una terminal y ejecuta los siguientes comandos para compilar el código fuente hacia un directorio binario limpio:
# Crear directorio de salida
mkdir -p out

# Compilar todos los archivos fuente (.java)
javac -d out src/*.java
Instrucciones de Ejecución
Una vez completada la compilación, inicia el flujo interactivo y el motor de inferencia con el siguiente comando:
java -cp out Main

