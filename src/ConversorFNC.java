// ============================================================
// ConversorFNC.java
//
// Transforma una fórmula arbitraria de lógica de primer orden
// a su representación en Forma Normal Conjuntiva (FNC),
// lista para ser usada por el motor de resolución.
//
// El proceso aplica 9 transformaciones en cascada:
//  1. Eliminar equivalencias     F <=> G  =>  (F=>G) ^ (G=>F)
//  2. Eliminar implicaciones     F => G   =>  ~F v G
//  3. Interiorizar negaciones    De Morgan, doble negación, cuantificadores
//  4. Estandarizar variables     nombre único por cuantificador
//  5. Forma prenexa              cuantificadores al frente
//  6. Skolemizar                 eliminar cuantificadores existenciales
//  7. Eliminar universales       ∀x F => F  (quedan implícitos)
//  8. Distribuir disyunciones    (F^G)vH => (FvH)^(GvH)
//  9. Extraer cláusulas          separar cada conjunción como cláusula
// ============================================================

import java.util.*; // Para List, ArrayList, Map, LinkedHashMap

public class ConversorFNC {

    private int contSkolem; // contador para generar nombres únicos de funciones de Skolem (Sk1, Sk2, ...)
    private int contVar;    // contador para estandarizar variables con nombres únicos (x1, x2, ...)

    // Constructor: inicializa los contadores en 0
    public ConversorFNC() { contSkolem = 0; contVar = 0; }

    // ----------------------------------------------------------------
    // PUNTO DE ENTRADA: aplica las 9 transformaciones en orden
    // y devuelve la lista de cláusulas FNC resultantes
    // ----------------------------------------------------------------
    public List<Clausula> convertir(Formula f) {
        Formula e1 = eliminarEquivalencias(f);   // paso 1: quita <=>
        Formula e2 = eliminarImplicaciones(e1);  // paso 2: quita =>
        Formula e3 = interiorizarNegaciones(e2); // paso 3: empuja ~ hacia los átomos
        Formula e4 = estandarizarVariables(e3);  // paso 4: renombra variables para evitar conflictos
        Formula e5 = formaPrenexa(e4);           // paso 5: sube cuantificadores al frente
        List<String> univs = new ArrayList<>();  // lista de variables universales (para Skolem)
        Formula e6 = skolemizar(e5, univs);      // paso 6: elimina ∃ con constantes/funciones de Skolem
        Formula e7 = quitarUniversales(e6);      // paso 7: elimina ∀ (quedan implícitos)
        Formula e8 = distribuir(e7);             // paso 8: distribuye v sobre ^
        return extraerClausulas(e8);             // paso 9: separa conjunciones en cláusulas individuales
    }

    // ----------------------------------------------------------------
    // PASO 1: Eliminar equivalencias
    // F <=> G  se convierte en  (F => G) ^ (G => F)
    // ----------------------------------------------------------------
    private Formula eliminarEquivalencias(Formula f) {
        switch (f.tipo) {
            case EQUIVALENCIA:
                // Reemplaza <=> por dos implicaciones en conjunción
                return Formula.yOp(
                    Formula.implica(eliminarEquivalencias(f.izq), eliminarEquivalencias(f.der)), // F => G
                    Formula.implica(eliminarEquivalencias(f.der), eliminarEquivalencias(f.izq)));// G => F
            case ATOMO:       return f; // los átomos no cambian
            case NEGACION:    return Formula.no(eliminarEquivalencias(f.izq)); // baja al interior
            case CONJUNCION:  return Formula.yOp(eliminarEquivalencias(f.izq), eliminarEquivalencias(f.der));
            case DISYUNCION:  return Formula.oOp(eliminarEquivalencias(f.izq), eliminarEquivalencias(f.der));
            case IMPLICACION: return Formula.implica(eliminarEquivalencias(f.izq), eliminarEquivalencias(f.der));
            case PARATODO:    return Formula.paraTodo(f.variable, eliminarEquivalencias(f.izq));
            case EXISTE:      return Formula.existe(f.variable, eliminarEquivalencias(f.izq));
            default:          return f;
        }
    }

    // ----------------------------------------------------------------
    // PASO 2: Eliminar implicaciones
    // F => G  se convierte en  ~F v G
    // ----------------------------------------------------------------
    private Formula eliminarImplicaciones(Formula f) {
        switch (f.tipo) {
            case IMPLICACION:
                // Reemplaza => por disyunción con el antecedente negado
                return Formula.oOp(Formula.no(eliminarImplicaciones(f.izq)), eliminarImplicaciones(f.der));
            case ATOMO:      return f; // los átomos no cambian
            case NEGACION:   return Formula.no(eliminarImplicaciones(f.izq));
            case CONJUNCION: return Formula.yOp(eliminarImplicaciones(f.izq), eliminarImplicaciones(f.der));
            case DISYUNCION: return Formula.oOp(eliminarImplicaciones(f.izq), eliminarImplicaciones(f.der));
            case PARATODO:   return Formula.paraTodo(f.variable, eliminarImplicaciones(f.izq));
            case EXISTE:     return Formula.existe(f.variable, eliminarImplicaciones(f.izq));
            default:         return f;
        }
    }

    // ----------------------------------------------------------------
    // PASO 3: Interiorizar negaciones (Leyes de De Morgan + doble negación)
    // Las negaciones se "empujan" hasta que estén justo frente a un átomo.
    //
    // Reglas aplicadas:
    //   ~~F          => F                 (doble negación)
    //   ~(F ^ G)     => ~F v ~G           (De Morgan)
    //   ~(F v G)     => ~F ^ ~G           (De Morgan)
    //   ~(∀x F)      => ∃x ~F             (negación de universal)
    //   ~(∃x F)      => ∀x ~F             (negación de existencial)
    // ----------------------------------------------------------------
    private Formula interiorizarNegaciones(Formula f) {
        if (f.tipo != Formula.Tipo.NEGACION) {
            // No es negación: simplemente baja recursivamente
            switch (f.tipo) {
                case ATOMO:      return f;
                case CONJUNCION: return Formula.yOp(interiorizarNegaciones(f.izq), interiorizarNegaciones(f.der));
                case DISYUNCION: return Formula.oOp(interiorizarNegaciones(f.izq), interiorizarNegaciones(f.der));
                case PARATODO:   return Formula.paraTodo(f.variable, interiorizarNegaciones(f.izq));
                case EXISTE:     return Formula.existe(f.variable, interiorizarNegaciones(f.izq));
                default:         return f;
            }
        }
        // Es negación: analiza qué hay dentro
        Formula interior = f.izq; // la fórmula que está siendo negada
        switch (interior.tipo) {
            case NEGACION: // ~~F => F (doble negación)
                return interiorizarNegaciones(interior.izq);
            case CONJUNCION: // ~(F ^ G) => (~F v ~G) (De Morgan)
                return Formula.oOp(
                    interiorizarNegaciones(Formula.no(interior.izq)),  // ~F
                    interiorizarNegaciones(Formula.no(interior.der))); // ~G
            case DISYUNCION: // ~(F v G) => (~F ^ ~G) (De Morgan)
                return Formula.yOp(
                    interiorizarNegaciones(Formula.no(interior.izq)),  // ~F
                    interiorizarNegaciones(Formula.no(interior.der))); // ~G
            case PARATODO: // ~(∀x F) => ∃x ~F
                return Formula.existe(interior.variable,
                    interiorizarNegaciones(Formula.no(interior.izq)));
            case EXISTE: // ~(∃x F) => ∀x ~F
                return Formula.paraTodo(interior.variable,
                    interiorizarNegaciones(Formula.no(interior.izq)));
            default: return f; // ~Átomo: ya está en la forma correcta, no cambia
        }
    }

    // ----------------------------------------------------------------
    // PASO 4: Estandarizar variables
    // Renombra cada variable cuantificada con un nombre único
    // para evitar conflictos entre distintos cuantificadores.
    // Ej: ∀x (P(x) ^ ∀x Q(x))  =>  ∀x1 (P(x1) ^ ∀x2 Q(x2))
    // ----------------------------------------------------------------
    private Formula estandarizarVariables(Formula f) {
        return estandarizarRec(f, new LinkedHashMap<>()); // empieza con mapa de renombres vacío
    }

    // Recorre el árbol propagando el mapa de renombres actual
    private Formula estandarizarRec(Formula f, Map<String, String> mapa) {
        switch (f.tipo) {
            case ATOMO: {
                // Renombra cada argumento según el mapa actual
                List<Termino> args = new ArrayList<>();
                for (Termino t : f.argumentos) args.add(aplicarMapa(t, mapa)); // aplica renombramiento
                return Formula.atomo(f.predicado, args);
            }
            case NEGACION:   return Formula.no(estandarizarRec(f.izq, mapa));
            case CONJUNCION: return Formula.yOp(estandarizarRec(f.izq, mapa), estandarizarRec(f.der, mapa));
            case DISYUNCION: return Formula.oOp(estandarizarRec(f.izq, mapa), estandarizarRec(f.der, mapa));
            case PARATODO:
            case EXISTE: {
                contVar++; // incrementa el contador para generar un nombre único
                String nuevo = f.variable + contVar; // ej: "x" => "x1", "x2", etc.
                Map<String, String> m2 = new LinkedHashMap<>(mapa); // copia el mapa para no alterar el padre
                m2.put(f.variable, nuevo); // registra el renombrado en el mapa local
                Formula cuerpo = estandarizarRec(f.izq, m2); // aplica en el cuerpo con el nuevo mapa
                return f.tipo == Formula.Tipo.PARATODO
                    ? Formula.paraTodo(nuevo, cuerpo) // ∀nuevo cuerpo
                    : Formula.existe(nuevo, cuerpo);  // ∃nuevo cuerpo
            }
            default: return f;
        }
    }

    // Aplica el mapa de renombres a un término
    private Termino aplicarMapa(Termino t, Map<String, String> mapa) {
        if (t.esVariable()) {
            if (mapa.containsKey(t.nombre)) return new Termino(mapa.get(t.nombre), true); // renombra la variable
            return t; // variable no está en el mapa, se deja igual
        }
        if (t.esConstante()) return t; // las constantes no se renombran
        // Es función: aplica el mapa a cada argumento
        List<Termino> args = new ArrayList<>();
        for (Termino arg : t.argumentos) args.add(aplicarMapa(arg, mapa));
        return new Termino(t.nombre, args);
    }

    // ----------------------------------------------------------------
    // PASO 5: Forma prenexa
    // Sube todos los cuantificadores al frente de la fórmula.
    // Ej: (∀x P(x)) ^ (∀y Q(y))  =>  ∀x ∀y (P(x) ^ Q(y))
    // ----------------------------------------------------------------
    private Formula formaPrenexa(Formula f) {
        if (f.tipo == Formula.Tipo.ATOMO)    return f; // los átomos no tienen cuantificadores
        if (f.tipo == Formula.Tipo.NEGACION) return Formula.no(formaPrenexa(f.izq)); // baja
        if (f.tipo == Formula.Tipo.PARATODO) return Formula.paraTodo(f.variable, formaPrenexa(f.izq)); // sube el ∀
        if (f.tipo == Formula.Tipo.EXISTE)   return Formula.existe(f.variable, formaPrenexa(f.izq));   // sube el ∃
        if (f.tipo == Formula.Tipo.CONJUNCION || f.tipo == Formula.Tipo.DISYUNCION) {
            Formula li = formaPrenexa(f.izq); // forma prenexa del lado izquierdo
            Formula ld = formaPrenexa(f.der); // forma prenexa del lado derecho
            // Construye el núcleo (sin cuantificadores) y luego saca los cuantificadores hacia afuera
            Formula nucleo = (f.tipo == Formula.Tipo.CONJUNCION) ? Formula.yOp(li, ld) : Formula.oOp(li, ld);
            return sacarCuantificadores(nucleo); // extrae cuantificadores que quedaron anidados
        }
        return f;
    }

    // Extrae cuantificadores de los hijos de una conjunción o disyunción hacia afuera
    private Formula sacarCuantificadores(Formula f) {
        if (f.tipo != Formula.Tipo.CONJUNCION && f.tipo != Formula.Tipo.DISYUNCION) return f;
        boolean esConj = f.tipo == Formula.Tipo.CONJUNCION; // true = ^, false = v
        Formula li = f.izq, ld = f.der;
        // Si el hijo izquierdo empieza con ∀, lo saca hacia afuera
        if (li.tipo == Formula.Tipo.PARATODO) {
            Formula n = esConj ? Formula.yOp(li.izq, ld) : Formula.oOp(li.izq, ld);
            return Formula.paraTodo(li.variable, sacarCuantificadores(n));
        }
        // Si el hijo izquierdo empieza con ∃, lo saca hacia afuera
        if (li.tipo == Formula.Tipo.EXISTE) {
            Formula n = esConj ? Formula.yOp(li.izq, ld) : Formula.oOp(li.izq, ld);
            return Formula.existe(li.variable, sacarCuantificadores(n));
        }
        // Si el hijo derecho empieza con ∀, lo saca hacia afuera
        if (ld.tipo == Formula.Tipo.PARATODO) {
            Formula n = esConj ? Formula.yOp(li, ld.izq) : Formula.oOp(li, ld.izq);
            return Formula.paraTodo(ld.variable, sacarCuantificadores(n));
        }
        // Si el hijo derecho empieza con ∃, lo saca hacia afuera
        if (ld.tipo == Formula.Tipo.EXISTE) {
            Formula n = esConj ? Formula.yOp(li, ld.izq) : Formula.oOp(li, ld.izq);
            return Formula.existe(ld.variable, sacarCuantificadores(n));
        }
        return f; // no había cuantificadores que sacar
    }

    // ----------------------------------------------------------------
    // PASO 6: Skolemización
    // Elimina los cuantificadores existenciales (∃) reemplazando la
    // variable existencial por una constante o función de Skolem.
    //
    // Si no hay variables universales en el contexto => constante Sk1, Sk2, ...
    // Si hay variables universales [x, y, ...] => función Sk1(x, y, ...)
    //
    // Parámetro univs: lista de variables universales en el contexto actual
    // ----------------------------------------------------------------
    private Formula skolemizar(Formula f, List<String> univs) {
        switch (f.tipo) {
            case PARATODO: {
                // Agrega la variable universal al contexto y continúa hacia adentro
                List<String> u2 = new ArrayList<>(univs); // copia el contexto actual
                u2.add(f.variable); // agrega la nueva variable universal
                return Formula.paraTodo(f.variable, skolemizar(f.izq, u2)); // skolemiza el cuerpo
            }
            case EXISTE: {
                contSkolem++; // incrementa el contador para el nombre de Skolem
                String sk = "Sk" + contSkolem; // nombre de la constante/función de Skolem (Sk1, Sk2, ...)
                Termino rep; // término de reemplazo
                if (univs.isEmpty()) {
                    rep = new Termino(sk, false); // no hay variables universales => constante Skolem
                } else {
                    // Hay variables universales => función de Skolem con esas variables como argumentos
                    List<Termino> args = new ArrayList<>();
                    for (String v : univs) args.add(new Termino(v, true)); // cada variable universal como argumento
                    rep = new Termino(sk, args); // función Sk1(x, y, ...)
                }
                // Reemplaza la variable existencial por el término Skolem en el cuerpo
                return skolemizar(reemplazarVar(f.variable, rep, f.izq), univs);
            }
            case ATOMO:      return f;
            case NEGACION:   return Formula.no(skolemizar(f.izq, univs));
            case CONJUNCION: return Formula.yOp(skolemizar(f.izq, univs), skolemizar(f.der, univs));
            case DISYUNCION: return Formula.oOp(skolemizar(f.izq, univs), skolemizar(f.der, univs));
            default:         return f;
        }
    }

    // Reemplaza todas las ocurrencias de la variable "var" por el término "rep" en la fórmula f
    private Formula reemplazarVar(String var, Termino rep, Formula f) {
        switch (f.tipo) {
            case ATOMO: {
                List<Termino> args = new ArrayList<>();
                for (Termino t : f.argumentos) args.add(reemplazarEnTermino(var, rep, t)); // reemplaza en cada arg
                return Formula.atomo(f.predicado, args);
            }
            case NEGACION:   return Formula.no(reemplazarVar(var, rep, f.izq));
            case CONJUNCION: return Formula.yOp(reemplazarVar(var, rep, f.izq), reemplazarVar(var, rep, f.der));
            case DISYUNCION: return Formula.oOp(reemplazarVar(var, rep, f.izq), reemplazarVar(var, rep, f.der));
            case PARATODO:
                if (f.variable.equals(var)) return f; // la variable está sombreada por este cuantificador, no reemplaza
                return Formula.paraTodo(f.variable, reemplazarVar(var, rep, f.izq));
            case EXISTE:
                if (f.variable.equals(var)) return f; // idem para existencial
                return Formula.existe(f.variable, reemplazarVar(var, rep, f.izq));
            default: return f;
        }
    }

    // Reemplaza la variable "var" por el término "rep" dentro de un término t
    private Termino reemplazarEnTermino(String var, Termino rep, Termino t) {
        if (t.esVariable() && t.nombre.equals(var)) return rep; // es la variable a reemplazar
        if (t.esConstante() || t.esVariable()) return t;         // constante u otra variable: no cambia
        // Es función: reemplaza en cada argumento
        List<Termino> args = new ArrayList<>();
        for (Termino arg : t.argumentos) args.add(reemplazarEnTermino(var, rep, arg));
        return new Termino(t.nombre, args);
    }

    // ----------------------------------------------------------------
    // PASO 7: Quitar cuantificadores universales
    // Los ∀ quedan implícitos en la FNC, así que simplemente se eliminan.
    // ∀x F  =>  F
    // ----------------------------------------------------------------
    private Formula quitarUniversales(Formula f) {
        switch (f.tipo) {
            case PARATODO:   return quitarUniversales(f.izq); // descarta el ∀ y procesa el cuerpo
            case ATOMO:      return f;
            case NEGACION:   return Formula.no(quitarUniversales(f.izq));
            case CONJUNCION: return Formula.yOp(quitarUniversales(f.izq), quitarUniversales(f.der));
            case DISYUNCION: return Formula.oOp(quitarUniversales(f.izq), quitarUniversales(f.der));
            default:         return f;
        }
    }

    // ----------------------------------------------------------------
    // PASO 8: Distribuir disyunciones sobre conjunciones
    // Asegura que todas las disyunciones queden "dentro" de las conjunciones.
    // Regla: (F ^ G) v H  =>  (F v H) ^ (G v H)
    // Se aplica recursivamente hasta que no haya más distribuciones posibles.
    // ----------------------------------------------------------------
    private Formula distribuir(Formula f) {
        if (f.tipo == Formula.Tipo.CONJUNCION)
            return Formula.yOp(distribuir(f.izq), distribuir(f.der)); // distribuye en ambos lados de la conjunción
        if (f.tipo == Formula.Tipo.DISYUNCION) {
            Formula di = distribuir(f.izq); // distribuye recursivamente el lado izquierdo
            Formula dd = distribuir(f.der); // distribuye recursivamente el lado derecho
            if (di.tipo == Formula.Tipo.CONJUNCION)
                // (A ^ B) v D  =>  (A v D) ^ (B v D)
                return Formula.yOp(distribuir(Formula.oOp(di.izq, dd)),
                                   distribuir(Formula.oOp(di.der, dd)));
            if (dd.tipo == Formula.Tipo.CONJUNCION)
                // I v (A ^ B)  =>  (I v A) ^ (I v B)
                return Formula.yOp(distribuir(Formula.oOp(di, dd.izq)),
                                   distribuir(Formula.oOp(di, dd.der)));
            return Formula.oOp(di, dd); // no hay conjunción que distribuir
        }
        return f; // átomo o negación: no cambia
    }

    // ----------------------------------------------------------------
    // PASO 9: Extraer cláusulas individuales del árbol en FNC
    // Una FNC es una conjunción de disyunciones.
    // Cada disyunción se convierte en una Clausula.
    // ----------------------------------------------------------------
    private List<Clausula> extraerClausulas(Formula f) {
        List<Clausula> lista = new ArrayList<>();
        descomponerConj(f, lista); // separa la conjunción en sus partes
        return lista;
    }

    // Recorre el árbol separando cada rama de una conjunción como una cláusula
    private void descomponerConj(Formula f, List<Clausula> lista) {
        if (f.tipo == Formula.Tipo.CONJUNCION) {
            descomponerConj(f.izq, lista); // procesa el lado izquierdo de la conjunción
            descomponerConj(f.der, lista); // procesa el lado derecho de la conjunción
        } else {
            // No es conjunción: esta sub-fórmula es una cláusula (disyunción o átomo)
            List<Literal> lits = new ArrayList<>();
            recogerLiterales(f, lits);         // recolecta todos los literales de la disyunción
            lista.add(new Clausula(lits, 0));  // crea la cláusula (id=0, se asignará después)
        }
    }

    // Recorre una disyunción y recolecta todos sus literales en la lista
    private void recogerLiterales(Formula f, List<Literal> lits) {
        if (f.tipo == Formula.Tipo.DISYUNCION) {
            recogerLiterales(f.izq, lits); // recolecta del lado izquierdo
            recogerLiterales(f.der, lits); // recolecta del lado derecho
        } else if (f.tipo == Formula.Tipo.ATOMO) {
            // Átomo positivo: literal sin negación
            lits.add(new Literal(f.predicado, f.argumentos, false));
        } else if (f.tipo == Formula.Tipo.NEGACION && f.izq.tipo == Formula.Tipo.ATOMO) {
            // Átomo negado: literal con negación
            lits.add(new Literal(f.izq.predicado, f.izq.argumentos, true));
        }
    }
}
