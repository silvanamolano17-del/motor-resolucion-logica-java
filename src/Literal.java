// ============================================================
// Literal.java
//
// Elemento atómico de una cláusula.
// Es un predicado aplicado a una lista de términos,
// con posible negación.
//
// Ejemplos:
//   Hombre(Marco)       -- positivo  (estaNegado = false)
//   ~Leal(x_1, Cesar)   -- negado    (estaNegado = true)
// ============================================================

import java.util.*; // Para List, ArrayList, Objects

public class Literal {

    public String        predicado;   // Nombre del predicado (ej: "Hombre", "Leal")
    public List<Termino> argumentos;  // Lista de términos a los que se aplica el predicado
    public boolean       estaNegado;  // true si el literal está negado (~), false si es positivo

    // ----------------------------------------------------------------
    // Constructor
    // ----------------------------------------------------------------
    public Literal(String pred, List<Termino> args, boolean negado) {
        this.predicado  = pred;                  // guarda el nombre del predicado
        this.argumentos = new ArrayList<>(args); // copia la lista de argumentos
        this.estaNegado = negado;                // guarda si está negado
    }

    // ----------------------------------------------------------------
    // Devuelve una COPIA de este literal con el signo opuesto
    // Ej: ~Leal(x,y).inversion() => Leal(x,y)
    // Usado por MotorDeResolucion para preparar la unificación
    // ----------------------------------------------------------------
    public Literal inversion() {
        return new Literal(predicado, argumentos, !estaNegado); // mismo predicado y args, signo contrario
    }

    // ----------------------------------------------------------------
    // Verifica si dos literales tienen el mismo predicado y los mismos
    // argumentos, SIN importar si están negados o no
    // Ej: Leal(Marco,Cesar) y ~Leal(Marco,Cesar) => mismoAtomo = true
    // ----------------------------------------------------------------
    public boolean mismoAtomo(Literal otro) {
        if (!predicado.equals(otro.predicado)) return false;         // predicados distintos
        if (argumentos.size() != otro.argumentos.size()) return false; // distinta aridad
        for (int i = 0; i < argumentos.size(); i++)
            if (!argumentos.get(i).equals(otro.argumentos.get(i))) return false; // argumento distinto
        return true; // mismo predicado y mismos argumentos
    }

    // ----------------------------------------------------------------
    // Devuelve true si dos literales son COMPLEMENTARIOS:
    // mismo átomo (predicado + argumentos) pero signos OPUESTOS
    // Ej: H y ~H => complementarios => se pueden cancelar en resolución
    // Usado en Prueba A (sin variables) para detectar pares resolvibles
    // ----------------------------------------------------------------
    public boolean esComplementario(Literal otro) {
        return mismoAtomo(otro) && (estaNegado != otro.estaNegado); // mismo átomo, signo distinto
    }

    // ----------------------------------------------------------------
    // Igualdad completa: mismo predicado, mismos argumentos Y mismo signo
    // Usado para detectar literales duplicados dentro de una cláusula
    // ----------------------------------------------------------------
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Literal)) return false; // no es un Literal
        Literal otro = (Literal) obj;
        return mismoAtomo(otro) && (estaNegado == otro.estaNegado); // igual en todo
    }

    // hashCode coherente con equals
    @Override
    public int hashCode() {
        return Objects.hash(predicado, argumentos, estaNegado);
    }

    // ----------------------------------------------------------------
    // Representación textual
    // ----------------------------------------------------------------
    public String toString() {
        StringBuilder sb = new StringBuilder(estaNegado ? "~" : ""); // prefijo ~ si está negado
        sb.append(predicado); // nombre del predicado
        if (!argumentos.isEmpty()) {
            sb.append("(");
            for (int i = 0; i < argumentos.size(); i++) {
                if (i > 0) sb.append(", ");              // separador entre argumentos
                sb.append(argumentos.get(i).toString()); // cada argumento como texto
            }
            sb.append(")");
        }
        return sb.toString(); // ej: "~Leal(x_1, Cesar)"
    }
}
