// ============================================================
// Formula.java
//
// Árbol sintáctico de una fórmula de lógica de primer orden.
// Cada objeto Formula es un nodo del árbol que almacena:
//   - su tipo (qué operador o átomo representa)
//   - sus hijos izquierdo (izq) y derecho (der) según corresponda
//
// Se construye SOLO a través de los métodos estáticos (atomo, no, yOp, etc.)
// El constructor privado evita que se creen fórmulas incompletas.
// ============================================================

import java.util.*; // Para List y ArrayList

public class Formula {

    // Enum que define todos los tipos posibles de nodo en el árbol
    public enum Tipo {
        ATOMO,        // Predicado aplicado a términos: Hombre(Marco)
        NEGACION,     // Negación:                     ~F
        CONJUNCION,   // Conjunción (Y lógico):         F ^ G
        DISYUNCION,   // Disyunción (O lógico):         F v G
        IMPLICACION,  // Implicación:                   F => G
        EQUIVALENCIA, // Equivalencia (reservado para ConversorFNC): F <=> G
        PARATODO,     // Cuantificador universal:        ∀x F
        EXISTE        // Cuantificador existencial:      ∃x F
    }

    public Tipo          tipo;        // Tipo de este nodo
    public String        predicado;   // Nombre del predicado (solo si tipo == ATOMO)
    public List<Termino> argumentos;  // Argumentos del predicado (solo si tipo == ATOMO)
    public String        variable;    // Nombre de la variable cuantificada (solo en PARATODO / EXISTE)
    public Formula       izq;         // Hijo izquierdo (subformula principal o única)
    public Formula       der;         // Hijo derecho (solo en CONJUNCION, DISYUNCION, IMPLICACION, EQUIVALENCIA)

    // Constructor privado: inicializa la lista de argumentos vacía
    // Solo los métodos estáticos pueden crear instancias de Formula
    private Formula() { this.argumentos = new ArrayList<>(); }

    // ----------------------------------------------------------------
    // Métodos estáticos de construcción
    // ----------------------------------------------------------------

    // Crea un átomo: predicado aplicado a una lista de términos
    // Ejemplo: Formula.atomo("Hombre", [Marco]) => Hombre(Marco)
    public static Formula atomo(String pred, List<Termino> args) {
        Formula f = new Formula();           // crea nodo vacío
        f.tipo = Tipo.ATOMO;                 // marca como átomo
        f.predicado = pred;                  // guarda el nombre del predicado
        f.argumentos = new ArrayList<>(args);// copia la lista de argumentos
        return f;
    }

    // Crea una negación: ~F
    // Ejemplo: Formula.no(Formula.atomo("Mortal", [Socrates])) => ~Mortal(Sócrates)
    public static Formula no(Formula f) {
        Formula r = new Formula();   // crea nodo vacío
        r.tipo = Tipo.NEGACION;      // marca como negación
        r.izq = f;                   // la fórmula negada va como hijo izquierdo
        return r;
    }

    // Crea una conjunción (AND): F ^ G
    // Ejemplo: Formula.yOp(A, B) => A ^ B
    public static Formula yOp(Formula f, Formula g) {
        Formula r = new Formula();   // crea nodo vacío
        r.tipo = Tipo.CONJUNCION;    // marca como conjunción
        r.izq = f;                   // primer operando (izquierda)
        r.der = g;                   // segundo operando (derecha)
        return r;
    }

    // Crea una disyunción (OR): F v G
    // Ejemplo: Formula.oOp(A, B) => A v B
    public static Formula oOp(Formula f, Formula g) {
        Formula r = new Formula();   // crea nodo vacío
        r.tipo = Tipo.DISYUNCION;    // marca como disyunción
        r.izq = f;                   // primer operando (izquierda)
        r.der = g;                   // segundo operando (derecha)
        return r;
    }

    // Crea una implicación: F => G
    // Ejemplo: Formula.implica(Pompeyano(x), Romano(x)) => Pompeyano(x) => Romano(x)
    public static Formula implica(Formula f, Formula g) {
        Formula r = new Formula();   // crea nodo vacío
        r.tipo = Tipo.IMPLICACION;   // marca como implicación
        r.izq = f;                   // antecedente (izquierda)
        r.der = g;                   // consecuente (derecha)
        return r;
    }

    // Crea un cuantificador universal: ∀var F
    // Ejemplo: Formula.paraTodo("x", cuerpo) => ∀x cuerpo
    public static Formula paraTodo(String var, Formula f) {
        Formula r = new Formula();   // crea nodo vacío
        r.tipo = Tipo.PARATODO;      // marca como cuantificador universal
        r.variable = var;            // nombre de la variable cuantificada
        r.izq = f;                   // cuerpo de la fórmula cuantificada
        return r;
    }

    // Crea un cuantificador existencial: ∃var F
    // Usado internamente por ConversorFNC durante la skolemización
    public static Formula existe(String var, Formula f) {
        Formula r = new Formula();   // crea nodo vacío
        r.tipo = Tipo.EXISTE;        // marca como cuantificador existencial
        r.variable = var;            // nombre de la variable cuantificada
        r.izq = f;                   // cuerpo de la fórmula cuantificada
        return r;
    }

    // ----------------------------------------------------------------
    // Representación textual del árbol (para impresión en pantalla)
    // ----------------------------------------------------------------
    public String toString() {
        switch (tipo) {
            case ATOMO: {
                // Construye "Predicado(arg1, arg2, ...)" o solo "Predicado" si no hay argumentos
                StringBuilder s = new StringBuilder(predicado);
                if (!argumentos.isEmpty()) {
                    s.append("(");
                    for (int i = 0; i < argumentos.size(); i++) {
                        if (i > 0) s.append(", ");      // separador entre argumentos
                        s.append(argumentos.get(i).toString()); // convierte cada término a texto
                    }
                    s.append(")");
                }
                return s.toString();
            }
            case NEGACION:     return "~(" + izq + ")";              // ~(F)
            case CONJUNCION:   return "(" + izq + " ^ " + der + ")"; // (F ^ G)
            case DISYUNCION:   return "(" + izq + " v " + der + ")"; // (F v G)
            case IMPLICACION:  return "(" + izq + " => " + der + ")";// (F => G)
            case EQUIVALENCIA: return "(" + izq + " <=> " + der + ")";// (F <=> G)
            case PARATODO:     return "Ax[" + variable + "] " + izq; // Ax[x] F
            case EXISTE:       return "Ex[" + variable + "] " + izq; // Ex[x] F
            default:           return "?"; // caso inesperado
        }
    }
}
