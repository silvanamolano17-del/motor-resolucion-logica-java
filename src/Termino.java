// ============================================================
// Termino.java
//
// Unidad básica que aparece como argumento dentro de un predicado.
// Un término puede ser de tres tipos:
//   - CONSTANTE : objeto concreto del dominio (Marco, Cesar)
//   - VARIABLE  : símbolo que puede sustituirse por otro término (x, y)
//   - FUNCION   : término compuesto con nombre y argumentos (Sk1(x))
//
// Las funciones de Skolem generadas por ConversorFNC son FUNCION.
// ============================================================

import java.util.*; // Para List y ArrayList

public class Termino {

    // Enum que clasifica el tipo de término
    public enum TipoTermino {
        CONSTANTE, // Objeto específico: Marco, Cesar
        VARIABLE,  // Símbolo sustituible: x, y, x_1
        FUNCION    // Término compuesto: Sk1(x), Padre(y)
    }

    public TipoTermino   tipo;        // Tipo de este término
    public String        nombre;      // Nombre del término (constante, variable o función)
    public List<Termino> argumentos;  // Argumentos si es FUNCION; lista vacía si no

    // ----------------------------------------------------------------
    // Constructor para CONSTANTE o VARIABLE
    // esVar=false => CONSTANTE,  esVar=true => VARIABLE
    // ----------------------------------------------------------------
    public Termino(String n, boolean esVar) {
        this.nombre     = n;                          // guarda el nombre
        this.tipo       = esVar ? TipoTermino.VARIABLE : TipoTermino.CONSTANTE; // asigna tipo
        this.argumentos = new ArrayList<>();           // sin argumentos (no es función)
    }

    // ----------------------------------------------------------------
    // Constructor para FUNCION (tiene argumentos)
    // Ejemplo: new Termino("Sk1", [x]) => Sk1(x)
    // ----------------------------------------------------------------
    public Termino(String n, List<Termino> args) {
        this.nombre     = n;                          // nombre de la función
        this.argumentos = new ArrayList<>(args);       // copia la lista de argumentos
        this.tipo       = TipoTermino.FUNCION;         // marca como función
    }

    // ----------------------------------------------------------------
    // Consultas de tipo (usadas en Unificador, Sustitucion, ConversorFNC)
    // ----------------------------------------------------------------

    // Devuelve true si este término es una constante
    public boolean esConstante() { return tipo == TipoTermino.CONSTANTE; }

    // Devuelve true si este término es una variable
    public boolean esVariable()  { return tipo == TipoTermino.VARIABLE;  }

    // Devuelve true si este término es una función compuesta
    public boolean esFuncion()   { return tipo == TipoTermino.FUNCION;   }

    // ----------------------------------------------------------------
    // Igualdad estructural: dos términos son iguales si tienen
    // el mismo tipo, mismo nombre y mismos argumentos (recursivo)
    // ----------------------------------------------------------------
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Termino)) return false;  // no es un Termino
        Termino otro = (Termino) obj;
        if (tipo != otro.tipo || !nombre.equals(otro.nombre)) return false; // tipo o nombre distintos
        if (argumentos.size() != otro.argumentos.size()) return false;      // distinta aridad
        for (int i = 0; i < argumentos.size(); i++)
            if (!argumentos.get(i).equals(otro.argumentos.get(i))) return false; // argumento distinto
        return true; // son iguales en todo
    }

    // hashCode coherente con equals (necesario para usar Termino en Sets/Maps)
    @Override
    public int hashCode() {
        return Objects.hash(tipo, nombre, argumentos);
    }

    // ----------------------------------------------------------------
    // Representación textual
    // ----------------------------------------------------------------
    public String toString() {
        if (esConstante() || esVariable()) return nombre; // solo el nombre: "Marco", "x"
        // Si es función: "nombre(arg1, arg2, ...)"
        StringBuilder sb = new StringBuilder(nombre).append("(");
        for (int i = 0; i < argumentos.size(); i++) {
            if (i > 0) sb.append(", ");          // separador entre argumentos
            sb.append(argumentos.get(i).toString()); // cada argumento recursivamente
        }
        return sb.append(")").toString();
    }
}
