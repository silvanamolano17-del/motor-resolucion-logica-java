// ============================================================
// Clausula.java
//
// Disyunción de literales que representa una unidad de conocimiento
// en Forma Normal Conjuntiva (FNC).
//
// Ejemplos:
//   { ~H, M }       -- si es hombre entonces es mortal
//   { H }           -- es hombre (hecho)
//   { }             -- cláusula VACÍA = contradicción (objetivo del motor)
//
// Cada cláusula tiene un ID numérico único asignado por BaseDeConocimiento.
// ============================================================

import java.util.*; // Para List, ArrayList, HashSet

public class Clausula {

    public List<Literal> literales; // Lista de literales que componen la disyunción
    public int           id;        // Identificador único asignado al registrar en la base

    // ----------------------------------------------------------------
    // Constructor vacío (sin literales, sin id)
    // ----------------------------------------------------------------
    public Clausula() {
        this.literales = new ArrayList<>(); // lista vacía
        this.id = 0;                        // id sin asignar
    }

    // ----------------------------------------------------------------
    // Constructor con lista de literales e id inicial
    // El id real se asigna luego por BaseDeConocimiento.agregarClausula()
    // ----------------------------------------------------------------
    public Clausula(List<Literal> lits, int idClausula) {
        this.literales = new ArrayList<>(lits); // copia la lista de literales
        this.id = idClausula;                   // guarda el id
    }

    // Devuelve true si la cláusula no tiene literales (contradicción)
    public boolean estaVacia() { return literales.isEmpty(); }

    // ----------------------------------------------------------------
    // Verifica si un literal está contenido en esta cláusula
    // Usa equals() de Literal (mismo predicado, argumentos y signo)
    // ----------------------------------------------------------------
    public boolean contiene(Literal lit) {
        for (Literal l : literales)
            if (l.equals(lit)) return true; // encontró el literal
        return false; // no lo encontró
    }

    // ----------------------------------------------------------------
    // Genera el RESOLVENTE entre esta cláusula y otra:
    // Toma todos los literales de ambas cláusulas EXCEPTO los dos
    // complementarios en las posiciones indicadas, sin duplicados.
    //
    // Parámetros:
    //   otra    -- la otra cláusula participante
    //   posEsta -- índice del literal a eliminar en ESTA cláusula
    //   posOtra -- índice del literal a eliminar en la OTRA cláusula
    //   nuevoId -- id provisional para la nueva cláusula
    // ----------------------------------------------------------------
    public Clausula resolver(Clausula otra, int posEsta, int posOtra, int nuevoId) {
        List<Literal> union = new ArrayList<>(); // acumulará los literales del resolvente

        // Agrega todos los literales de ESTA cláusula, excepto el que se cancela
        for (int i = 0; i < literales.size(); i++) {
            if (i == posEsta) continue; // salta el literal complementario de esta cláusula
            boolean repetido = false;
            for (Literal l : union) if (l.equals(literales.get(i))) { repetido = true; break; } // evita duplicados
            if (!repetido) union.add(literales.get(i)); // agrega solo si no está ya
        }

        // Agrega todos los literales de la OTRA cláusula, excepto el que se cancela
        for (int j = 0; j < otra.literales.size(); j++) {
            if (j == posOtra) continue; // salta el literal complementario de la otra cláusula
            boolean repetido = false;
            for (Literal l : union) if (l.equals(otra.literales.get(j))) { repetido = true; break; } // evita duplicados
            if (!repetido) union.add(otra.literales.get(j)); // agrega solo si no está ya
        }

        return new Clausula(union, nuevoId); // devuelve la nueva cláusula resolvente
    }

    // ----------------------------------------------------------------
    // Igualdad: dos cláusulas son iguales si tienen los mismos literales
    // (sin importar el orden ni el id)
    // Usado por BaseDeConocimiento.yaExiste() para evitar duplicados
    // ----------------------------------------------------------------
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Clausula)) return false; // no es una Clausula
        Clausula otra = (Clausula) obj;
        if (literales.size() != otra.literales.size()) return false; // distinto número de literales
        for (Literal l : literales)
            if (!otra.contiene(l)) return false; // algún literal de esta no está en la otra
        return true; // todas las literales coinciden
    }

    // hashCode basado en el conjunto de literales (sin orden)
    @Override
    public int hashCode() {
        return new HashSet<>(literales).hashCode();
    }

    // ----------------------------------------------------------------
    // Representación textual
    // ----------------------------------------------------------------
    public String toString() {
        if (estaVacia()) return "{ }"; // cláusula vacía = contradicción
        StringBuilder sb = new StringBuilder("{ ");
        for (int i = 0; i < literales.size(); i++) {
            if (i > 0) sb.append(", ");               // separador entre literales
            sb.append(literales.get(i).toString());    // cada literal como texto
        }
        return sb.append(" }").toString(); // ej: "{ ~H, M }"
    }
}
