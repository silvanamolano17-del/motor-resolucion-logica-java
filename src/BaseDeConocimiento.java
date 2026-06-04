// ============================================================
// BaseDeConocimiento.java
//
// Repositorio de cláusulas en Forma Normal Conjuntiva.
// Cumple dos funciones:
//
//   1. Historial completo (clausulas): guarda TODAS las cláusulas
//      que han sido registradas, para poder detectar duplicados.
//
//   2. Pila de pendientes (pila LIFO): contiene las cláusulas que
//      todavía no han sido "consumidas" por el motor de resolución.
//      La cláusula más reciente queda en el TOPE (posición 0).
//      Cuando dos cláusulas se usan en una resolución, se eliminan
//      de la pila y la nueva resolvente se inserta en el tope.
// ============================================================

import java.util.*; // Para List, ArrayList, Deque, ArrayDeque, Iterator

public class BaseDeConocimiento {

    public List<Clausula>  clausulas;   // historial completo (todas las cláusulas registradas)
    public Deque<Clausula> pila;        // pila LIFO de cláusulas pendientes (tope = más reciente)
    public int             siguienteId; // contador para asignar IDs únicos secuenciales

    // Constructor: inicializa la base vacía con contador de IDs en 1
    public BaseDeConocimiento() {
        this.clausulas   = new ArrayList<>();  // historial vacío
        this.pila        = new ArrayDeque<>(); // pila vacía
        this.siguienteId = 1;                  // los IDs empiezan en 1
    }

    // ----------------------------------------------------------------
    // Registra una cláusula nueva:
    //   1. Le asigna el próximo ID disponible
    //   2. La agrega al historial completo
    //   3. La empuja al TOPE de la pila de pendientes
    // ----------------------------------------------------------------
    public void agregarClausula(Clausula c) {
        c.id = siguienteId++;    // asigna el ID actual y luego incrementa el contador
        clausulas.add(c);        // agrega al historial completo
        pila.addFirst(c);        // addFirst = push al tope de la pila (LIFO)
    }

    // ----------------------------------------------------------------
    // Elimina de la pila (NO del historial) la cláusula con el id dado.
    // Se usa después de que una cláusula participó en una resolución.
    // ----------------------------------------------------------------
    public void eliminarDePilaPorId(int id) {
        Iterator<Clausula> it = pila.iterator(); // itera sobre la pila
        while (it.hasNext()) {
            if (it.next().id == id) { // encontró la cláusula con ese id
                it.remove();          // la elimina de la pila
                return;               // termina (solo hay una cláusula con ese id)
            }
        }
    }

    // ----------------------------------------------------------------
    // Verifica si una cláusula equivalente ya existe en el HISTORIAL.
    // Evita agregar resolventes duplicados que no aportan información nueva.
    // Usa equals() de Clausula (compara conjuntos de literales).
    // ----------------------------------------------------------------
    public boolean yaExiste(Clausula c) {
        for (Clausula cl : clausulas)
            if (cl.equals(c)) return true; // encontró una cláusula igual
        return false; // no hay duplicado
    }

    // ----------------------------------------------------------------
    // Imprime el historial completo de cláusulas con su ID
    // Formato: "  (1) { ~H, M }"
    // ----------------------------------------------------------------
    public void imprimir() {
        for (Clausula c : clausulas)
            System.out.println("  (" + c.id + ") " + c.toString()); // id y representación textual
    }

    // ----------------------------------------------------------------
    // Imprime el estado actual de la pila de pendientes,
    // del tope (más reciente) al fondo (más antigua).
    // El tope lleva la etiqueta "[TOP]".
    // ----------------------------------------------------------------
    public void imprimirPila() {
        if (pila.isEmpty()) {
            System.out.println("  (pila vacía)"); // caso especial: pila vacía
            return;
        }
        int pos = 0;
        for (Clausula c : pila) { // itera de addFirst a addLast = tope a fondo
            String etiqueta = (pos == 0) ? "[TOP]" : "     "; // etiqueta solo para el tope
            System.out.println("  " + etiqueta + " (" + c.id + ") " + c.toString());
            pos++; // avanza a la siguiente posición
        }
    }
}
