// ============================================================
// MotorDeResolucion.java
//
// Núcleo del sistema de inferencia. Implementa la demostración
// por refutación: niega la consulta, la agrega a la base y
// aplica resolución hasta obtener la cláusula vacía (contradicción).
//
// Modo sin variables: compara literales directamente (Prueba A).
// Modo con variables: usa el algoritmo de Robinson para unificar (Prueba B).
//
// Las cláusulas pendientes se organizan en una pila (LIFO):
//   - La cláusula más reciente está en el tope y se intenta primero.
//   - Cuando dos cláusulas se resuelven, ambas se eliminan de la pila
//     y la nueva resolvente se inserta en el tope.
// ============================================================

import java.util.*; // Para List, ArrayList

public class MotorDeResolucion {

    public  BaseDeConocimiento base;            // repositorio de cláusulas (historial + pila)
    public  boolean            modoUnificacion; // true = Prueba B con variables; false = Prueba A proposicional
    private Unificador         unificador;      // objeto que ejecuta el algoritmo de Robinson
    private int                selloRenombre;  // contador para generar sufijos únicos al renombrar variables
    private List<Integer>      soporte;        // ids de las cláusulas en el conjunto de soporte (descendientes de la hipótesis negada)

    // ----------------------------------------------------------------
    // Constructor: inicializa todos los campos
    // usarUnificacion=true => Prueba B, false => Prueba A
    // ----------------------------------------------------------------
    public MotorDeResolucion(boolean usarUnificacion) {
        this.base            = new BaseDeConocimiento(); // base de conocimiento vacía
        this.modoUnificacion = usarUnificacion;          // guarda el modo de operación
        this.unificador      = new Unificador();         // crea el objeto unificador
        this.selloRenombre   = 0;                        // contador de sufijos en 0
        this.soporte         = new ArrayList<>();        // lista de soporte vacía
    }

    // ----------------------------------------------------------------
    // Agrega una cláusula directamente a la base.
    // Si estamos en modo unificación, primero renombra las variables
    // con un sufijo único para evitar conflictos entre cláusulas distintas.
    // ----------------------------------------------------------------
    public void agregarClausula(Clausula c) {
        if (modoUnificacion) c = frescasVariables(c); // renombra: x => x_1, x_2, etc.
        base.agregarClausula(c); // registra en historial y empuja al tope de la pila
    }

    // ----------------------------------------------------------------
    // Convierte una fórmula LPO a FNC usando el ConversorFNC,
    // imprime la fórmula original y cada cláusula resultante,
    // y registra cada cláusula en la base.
    // ----------------------------------------------------------------
    public void agregarFormula(Formula f, ConversorFNC conv) {
        System.out.println("  " + f.toString()); // imprime la fórmula original (ej: Ax[x] (Pompeyano(x) => Romano(x)))
        List<Clausula> resultado = conv.convertir(f); // aplica las 9 transformaciones FNC
        for (Clausula c : resultado) {
            System.out.println("    => " + c.toString()); // imprime cada cláusula generada
            agregarClausula(c); // registra la cláusula (con renombre de variables si aplica)
        }
    }

    // ----------------------------------------------------------------
    // MÉTODO PRINCIPAL: intenta refutar la hipótesis negada.
    // Devuelve true si se llega a la cláusula vacía (consulta demostrada).
    //
    // Flujo general:
    //   1. Agrega la hipótesis negada al tope de la pila.
    //   2. Toma la segunda cláusula desde el tope como cláusula "fija" (ci).
    //   3. Busca otra cláusula (cj) con la que ci pueda resolverse.
    //   4. Si las encuentra, genera el resolvente, elimina ci y cj de la pila,
    //      inserta el resolvente en el tope.
    //   5. Si el resolvente es vacío => contradicción => DEMOSTRADO.
    //   6. Si no hay pares resolvibles => NO DEMOSTRADO.
    // ----------------------------------------------------------------
    public boolean resolver(Clausula hipotesisNegada) {
        if (modoUnificacion) hipotesisNegada = frescasVariables(hipotesisNegada); // renombra variables
        base.agregarClausula(hipotesisNegada); // agrega la hipótesis negada (queda en el tope)

        // Registra el id de la hipótesis negada como punto de inicio del conjunto de soporte
        int idInicial = base.clausulas.get(base.clausulas.size() - 1).id;
        soporte.add(idInicial); // la hipótesis negada inicia el conjunto de soporte

        // Imprime el estado inicial de la base antes de comenzar la resolución
        System.out.println("\n--- Cláusulas en la base ---");
        base.imprimir(); // muestra todas las cláusulas con sus IDs
        System.out.println("\n--- Aplicando resolución por refutación ---");

        int paso = 1; // contador de pasos de resolución (para la impresión)

        // Bucle principal de resolución: continúa hasta demostrar o agotar opciones
        while (true) {
            // Imprime el estado actual de la pila antes de cada paso
            System.out.println("\n-- Cláusulas pendientes (pila) --");
            base.imprimirPila(); // muestra la pila de tope a fondo

            // Toma una instantánea de la pila como lista para iterar con índices
            // índice 0 = tope (más reciente), índice n-1 = fondo (más antigua)
            List<Clausula> lista = new ArrayList<>(base.pila);

            boolean huboAvance = false; // se pone en true si se genera un resolvente en este ciclo

            // Si hay menos de dos cláusulas no es posible resolver
            if (lista.size() < 2) {
                System.out.println("\nLa pila tiene menos de dos cláusulas, no hay pares posibles.");
                System.out.println(">>> NO DEMOSTRADO <<<");
                return false;
            }

            // ci = la SEGUNDA cláusula desde el tope (índice 1); es la cláusula "fija" del paso
            Clausula ci = lista.get(1);

            // Etiqueta "bucle" para poder salir de los bucles anidados cuando se genera un resolvente
            bucle:
            for (int j = 0; j < lista.size(); j++) {
                if (j == 1) continue; // no resolver ci consigo misma

                Clausula cj = lista.get(j); // cláusula candidata para resolver con ci

                // Intenta cada par de literales (uno de ci, uno de cj)
                for (int a = 0; a < ci.literales.size(); a++) {
                    for (int b = 0; b < cj.literales.size(); b++) {

                        Literal la = ci.literales.get(a); // literal de ci en posición a
                        Literal lb = cj.literales.get(b); // literal de cj en posición b

                        Sustitucion mgu        = new Sustitucion(); // sustitución más general (vacía inicialmente)
                        boolean     resolubles = false;             // indica si el par puede resolverse

                        if (!modoUnificacion) {
                            // MODO SIN VARIABLES (Prueba A): basta con que sean complementarios
                            resolubles = la.esComplementario(lb); // mismo átomo, signos opuestos
                        } else {
                            // MODO CON VARIABLES (Prueba B): deben tener predicados complementarios
                            // y además sus argumentos deben poder unificarse
                            if (la.estaNegado != lb.estaNegado          // signos opuestos
                                    && la.predicado.equals(lb.predicado)) { // mismo predicado
                                // Obtiene la versión positiva de cada literal para unificar argumentos
                                Literal posA = la.estaNegado ? la.inversion() : la; // versión positiva de la
                                Literal posB = lb.estaNegado ? lb.inversion() : lb; // versión positiva de lb
                                Unificador.ResultadoUnificacion intento =
                                        unificador.unificar(posA, posB); // intenta unificar
                                if (intento.exito) {
                                    resolubles = true;     // la unificación fue exitosa
                                    mgu = intento.mgu;     // guarda la SMG obtenida
                                }
                            }
                        }

                        if (!resolubles) continue; // este par no es resolvible, prueba el siguiente

                        // Aplica la SMG a ambas cláusulas antes de calcular el resolvente
                        Clausula ciAplicada = ci; // por defecto sin cambios (modo sin variables)
                        Clausula cjAplicada = cj;
                        if (modoUnificacion && !mgu.estaVacia()) {
                            ciAplicada = mgu.aplicar(ci); // reemplaza variables por constantes en ci
                            cjAplicada = mgu.aplicar(cj); // reemplaza variables por constantes en cj
                        }

                        // Genera el resolvente: une los literales de ci y cj eliminando los complementarios
                        Clausula nueva = ciAplicada.resolver(cjAplicada, a, b, base.siguienteId);

                        // Si el resolvente ya existe en el historial (y no es vacío), lo ignora
                        if (base.yaExiste(nueva) && !nueva.estaVacia()) continue;

                        // Imprime el paso de resolución
                        System.out.print("\n[Paso " + paso++ + "]  "
                                + "(" + ci.id + ") + (" + cj.id + ")"); // ids de las dos cláusulas usadas
                        if (modoUnificacion && !mgu.estaVacia())
                            System.out.print("   sustitución: " + mgu.toString()); // imprime la SMG aplicada

                        // Si el resolvente es la cláusula vacía => contradicción => DEMOSTRADO
                        if (nueva.estaVacia()) {
                            System.out.println("   =>  {}  <-- contradicción encontrada");
                            System.out.println("\n>>> DEMOSTRADO <<<");
                            System.out.println("La consulta es verdadera.");
                            return true; // termina con éxito
                        }

                        // Imprime el resolvente generado
                        System.out.println("   =>  (" + base.siguienteId + "): " + nueva.toString());

                        // Elimina las dos cláusulas usadas de la pila (no del historial)
                        base.eliminarDePilaPorId(ci.id);
                        base.eliminarDePilaPorId(cj.id);

                        // Registra el id del resolvente en el conjunto de soporte
                        soporte.add(base.siguienteId);

                        // Agrega el resolvente a la base (queda en el tope de la pila)
                        base.agregarClausula(nueva);

                        huboAvance = true; // hubo progreso en este ciclo
                        break bucle;       // sale de los tres bucles y vuelve al while para el siguiente paso
                    }
                }
            }

            // Si no hubo ningún par resolvible en toda la pila => no se puede demostrar
            if (!huboAvance) {
                System.out.println("\nNo quedan pares resolvibles en la pila.");
                System.out.println(">>> NO DEMOSTRADO <<<");
                return false;
            }
        }
    }

    // ----------------------------------------------------------------
    // AUXILIAR: Renombra todas las variables de una cláusula con un
    // sufijo único ("_1", "_2", ...) para evitar colisiones entre
    // cláusulas distintas que usen el mismo nombre de variable.
    // Ej: { Pompeyano(x) } => { Pompeyano(x_3) }
    // ----------------------------------------------------------------
    private Clausula frescasVariables(Clausula c) {
        selloRenombre++;                      // incrementa el contador de sufijos
        String sufijo = "_" + selloRenombre;  // ej: "_1", "_2", ...
        List<Literal> lits = new ArrayList<>();
        for (Literal lit : c.literales) {
            List<Termino> args = new ArrayList<>();
            for (Termino t : lit.argumentos)
                args.add(renombrarRec(t, sufijo)); // renombra cada argumento del literal
            lits.add(new Literal(lit.predicado, args, lit.estaNegado)); // reconstruye el literal renombrado
        }
        return new Clausula(lits, c.id); // devuelve la cláusula con variables renombradas
    }

    // Renombra recursivamente un término:
    // - Variables: se les añade el sufijo    (x => x_3)
    // - Constantes: no cambian
    // - Funciones: se renombran sus argumentos recursivamente
    private Termino renombrarRec(Termino t, String sufijo) {
        if (t.esVariable())  return new Termino(t.nombre + sufijo, true); // variable => variable renombrada
        if (t.esConstante()) return t;                                     // constante => sin cambio
        List<Termino> args = new ArrayList<>();
        for (Termino arg : t.argumentos)
            args.add(renombrarRec(arg, sufijo)); // renombra cada argumento de la función
        return new Termino(t.nombre, args); // devuelve función con argumentos renombrados
    }
}
