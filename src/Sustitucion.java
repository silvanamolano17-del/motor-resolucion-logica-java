// ============================================================
// Sustitucion.java
//
// Mapa de ligaduras variable -> término que se construye
// durante la unificación (algoritmo de Robinson).
//
// Ejemplo de sustitución:  { x_1/Marco, y_2/Cesar }
// Significa: reemplaza x_1 por Marco, y_2 por Cesar.
//
// Puede aplicarse a términos, literales o cláusulas completas
// para reemplazar todas las variables ligadas por sus valores.
// ============================================================

import java.util.*; // Para Map, LinkedHashMap

public class Sustitucion {

    // Mapa de variable -> término al que se liga
    // LinkedHashMap para mantener el orden de inserción (útil al imprimir)
    public Map<String, Termino> ligaduras;

    // Constructor: crea una sustitución vacía (sin ligaduras)
    public Sustitucion() {
        this.ligaduras = new LinkedHashMap<>();
    }

    // Constructor de copia: crea una nueva sustitución con las mismas ligaduras
    // Usado por Unificador para no modificar la sustitución original al explorar
    public Sustitucion(Sustitucion otra) {
        this.ligaduras = new LinkedHashMap<>(otra.ligaduras); // copia todas las ligaduras
    }

    // Agrega o actualiza la ligadura: variable "var" => término t
    public void enlazar(String var, Termino t) { ligaduras.put(var, t); }

    // Devuelve true si la variable "var" tiene una ligadura en este mapa
    public boolean estaEnlazada(String var) { return ligaduras.containsKey(var); }

    // Devuelve el término al que está ligada la variable "var" (puede ser null si no está)
    public Termino buscar(String var) { return ligaduras.get(var); }

    // Devuelve true si la sustitución no tiene ninguna ligadura
    public boolean estaVacia() { return ligaduras.isEmpty(); }

    // ----------------------------------------------------------------
    // Aplica la sustitución a un TÉRMINO:
    // Si el término es una constante, se devuelve sin cambios.
    // Si es una variable ligada, se resuelve recursivamente hasta
    // llegar a un término sin variables (punto fijo).
    // Si es una función, se aplica la sustitución a cada argumento.
    // ----------------------------------------------------------------
    public Termino aplicar(Termino t) {
        if (t.esConstante()) return t; // las constantes no cambian
        if (t.esVariable()) {
            if (estaEnlazada(t.nombre)) return aplicar(buscar(t.nombre)); // sigue la cadena de ligaduras
            return t; // variable libre (sin ligadura), se deja igual
        }
        // Es una función: aplica la sustitución a cada argumento
        List<Termino> args = new ArrayList<>();
        for (Termino arg : t.argumentos) args.add(aplicar(arg)); // aplica recursivamente
        return new Termino(t.nombre, args); // devuelve función con argumentos sustituidos
    }

    // ----------------------------------------------------------------
    // Aplica la sustitución a un LITERAL:
    // Sustituye las variables en cada argumento del literal
    // ----------------------------------------------------------------
    public Literal aplicar(Literal lit) {
        List<Termino> args = new ArrayList<>();
        for (Termino arg : lit.argumentos) args.add(aplicar(arg)); // sustituye cada argumento
        return new Literal(lit.predicado, args, lit.estaNegado);   // devuelve literal con args sustituidos
    }

    // ----------------------------------------------------------------
    // Aplica la sustitución a una CLÁUSULA completa:
    // Sustituye las variables en cada literal de la cláusula
    // ----------------------------------------------------------------
    public Clausula aplicar(Clausula c) {
        List<Literal> lits = new ArrayList<>();
        for (Literal l : c.literales) lits.add(aplicar(l)); // sustituye en cada literal
        return new Clausula(lits, c.id);                     // devuelve cláusula con literales sustituidos
    }

    // ----------------------------------------------------------------
    // Representación textual: { x_1/Marco, y_2/Cesar }
    // ----------------------------------------------------------------
    public String toString() {
        if (ligaduras.isEmpty()) return "{}"; // sustitución vacía
        StringBuilder sb = new StringBuilder("{");
        boolean primero = true;
        for (Map.Entry<String, Termino> e : ligaduras.entrySet()) {
            if (!primero) sb.append(", ");                             // separador entre ligaduras
            sb.append(e.getKey()).append("/").append(e.getValue().toString()); // "variable/término"
            primero = false;
        }
        return sb.append("}").toString(); // ej: "{x_1/Marco, y_2/Cesar}"
    }
}
