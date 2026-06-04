// ============================================================
// Unificador.java
//
// Implementa el algoritmo de unificación de Robinson (1965).
// Dado dos literales con el mismo predicado, busca la Sustitución
// Más General (SMG) que los hace idénticos.
//
// Incluye occur-check para prevenir sustituciones circulares
// del tipo x -> f(x), que generarían estructuras infinitas.
// ============================================================

import java.util.*; // Para listas

public class Unificador {

    // ----------------------------------------------------------------
    // Clase interna que encapsula el resultado de un intento
    // de unificación: si tuvo éxito y cuál fue la SMG obtenida
    // ----------------------------------------------------------------
    public static class ResultadoUnificacion {
        public boolean     exito; // true si la unificación fue exitosa
        public Sustitucion mgu;   // la Sustitución Más General encontrada

        public ResultadoUnificacion(boolean exito, Sustitucion mgu) {
            this.exito = exito; // guarda si tuvo éxito
            this.mgu   = mgu;  // guarda la sustitución resultante
        }
    }

    // ----------------------------------------------------------------
    // Intenta unificar dos literales con el mismo predicado.
    // Devuelve un ResultadoUnificacion con exito=true y la SMG si
    // se pudo unificar, o exito=false si no fue posible.
    //
    // Llamado por MotorDeResolucion cuando la= y lb= tienen
    // predicados iguales pero signos opuestos.
    // ----------------------------------------------------------------
    public ResultadoUnificacion unificar(Literal p, Literal q) {
        // Si los predicados son distintos, no se puede unificar
        if (!p.predicado.equals(q.predicado))
            return new ResultadoUnificacion(false, new Sustitucion());

        // Si tienen distinta cantidad de argumentos, no se puede unificar
        if (p.argumentos.size() != q.argumentos.size())
            return new ResultadoUnificacion(false, new Sustitucion());

        // Sustitución acumuladora: se irá llenando durante la unificación
        Sustitucion theta = new Sustitucion();

        // Intenta unificar las listas de argumentos posición a posición
        if (!unificarListas(p.argumentos, q.argumentos, theta))
            return new ResultadoUnificacion(false, new Sustitucion()); // falló en algún argumento

        return new ResultadoUnificacion(true, theta); // éxito: devuelve la SMG
    }

    // ----------------------------------------------------------------
    // Intenta unificar dos términos individuales, extendiendo theta.
    // Retorna true si tuvo éxito (theta se modifica en el proceso).
    // ----------------------------------------------------------------
    private boolean unificarTerminos(Termino p, Termino q, Sustitucion theta) {
        // Primero aplica las ligaduras ya acumuladas en theta a ambos términos
        Termino tp = theta.aplicar(p); // p con sustitución aplicada
        Termino tq = theta.aplicar(q); // q con sustitución aplicada

        // Si ya son iguales tras aplicar theta, no hay nada que hacer
        if (tp.equals(tq)) return true;

        // Si tp es variable, intenta ligarla a tq
        if (tp.esVariable()) {
            if (ocurreEn(tp.nombre, tq)) return false; // occur-check: evita x -> f(x)
            theta.enlazar(tp.nombre, tq); // liga la variable tp al término tq
            return true;
        }

        // Si tq es variable, intenta ligarla a tp
        if (tq.esVariable()) {
            if (ocurreEn(tq.nombre, tp)) return false; // occur-check: evita y -> g(y)
            theta.enlazar(tq.nombre, tp); // liga la variable tq al término tp
            return true;
        }

        // Si ambos son funciones, deben tener el mismo nombre y aridad
        if (tp.esFuncion() && tq.esFuncion()) {
            if (!tp.nombre.equals(tq.nombre)) return false;                   // nombres distintos
            if (tp.argumentos.size() != tq.argumentos.size()) return false;   // aridades distintas
            return unificarListas(tp.argumentos, tq.argumentos, theta);       // unifica argumentos recursivamente
        }

        // Caso restante: dos constantes distintas o tipos incompatibles => falla
        return false;
    }

    // ----------------------------------------------------------------
    // Unifica dos listas de términos posición a posición.
    // Retorna false en cuanto encuentra un par que no unifica.
    // ----------------------------------------------------------------
    private boolean unificarListas(List<Termino> ps, List<Termino> qs, Sustitucion theta) {
        if (ps.size() != qs.size()) return false; // listas de distinto tamaño
        for (int i = 0; i < ps.size(); i++)
            if (!unificarTerminos(ps.get(i), qs.get(i), theta)) return false; // falla en posición i
        return true; // todos los pares unificaron correctamente
    }

    // ----------------------------------------------------------------
    // Occur-check: verifica si la variable "var" aparece en algún
    // lugar dentro del término t.
    // Previene ligaduras circulares como x -> f(x).
    // ----------------------------------------------------------------
    private boolean ocurreEn(String var, Termino t) {
        if (t.esVariable())  return t.nombre.equals(var); // t es la misma variable
        if (t.esConstante()) return false;                 // una constante nunca contiene variables
        for (Termino arg : t.argumentos)
            if (ocurreEn(var, arg)) return true; // busca recursivamente en los argumentos
        return false; // la variable no aparece en t
    }
}
