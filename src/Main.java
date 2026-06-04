// ============================================================
// Proyecto 2 - Inferencia por Resolución
// Pontificia Universidad Javeriana - Inteligencia Artificial 2026
//
// Punto de entrada del programa.
// Ejecuta dos pruebas de demostración por refutación:
//   Prueba A: sin variables (lógica proposicional)
//   Prueba B: con variables y unificación (lógica de primer orden)
//
// Compilar:  javac src/*.java -d out/
// Ejecutar:  java -cp out/ Main
// ============================================================

import java.io.PrintStream; // Para configurar la salida estándar con codificación UTF-8
import java.util.*;         // Para Arrays, ArrayList y otras colecciones

public class Main {

    public static void main(String[] args) throws Exception {

        // Configura la consola para imprimir correctamente tildes y caracteres especiales
        System.setOut(new PrintStream(System.out, true, "UTF-8"));

        // Imprime el encabezado del programa
        System.out.println("=================================================");
        System.out.println("   Inferencia por Resolución - Proyecto 2");
        System.out.println("   Pontificia Universidad Javeriana");
        System.out.println("=================================================");

        // =============================================================
        // PRUEBA A: Resolución sin variables (lógica proposicional)
        //
        // Conocimiento en lenguaje natural:
        //   - Todo hombre es mortal.  (H => M)
        //   - Sócrates es hombre.     (H)
        //
        // Pregunta: ¿Sócrates es mortal? (M)
        //
        // Representación en FNC (Forma Normal Conjuntiva):
        //   [1] { ~H, M }   -- de H => M  (si es hombre entonces es mortal)
        //   [2] { H }       -- hecho: es hombre
        //   [3] { ~M }      -- negación de la pregunta: suponemos que NO es mortal
        // =============================================================
        System.out.println("\n--- PRUEBA A: sin variables ---");
        System.out.println("Conocimiento: todo hombre es mortal. Sócrates es hombre.");
        System.out.println("Pregunta: ¿Sócrates es mortal?");
        System.out.println();

        // Crea el motor de resolución en modo SIN unificación (false = proposicional)
        MotorDeResolucion motorA = new MotorDeResolucion(false);

        // Crea el literal ~H (negado=true): representa "NO es hombre"
        Literal noHombre = new Literal("H", new ArrayList<>(), true);

        // Crea el literal M (negado=false): representa "es mortal"
        Literal mortal = new Literal("M", new ArrayList<>(), false);

        // Agrega al motor la cláusula [1]: { ~H, M }
        motorA.agregarClausula(new Clausula(Arrays.asList(noHombre, mortal), 0));

        // Crea el literal H (negado=false): representa "es hombre"
        Literal hombre = new Literal("H", new ArrayList<>(), false);

        // Agrega al motor la cláusula [2]: { H }
        motorA.agregarClausula(new Clausula(Arrays.asList(hombre), 0));

        // Crea el literal ~M (negado=true): negación de la pregunta "NO es mortal"
        Literal noMortal = new Literal("M", new ArrayList<>(), true);

        // Crea la cláusula hipótesis negada [3]: { ~M }
        Clausula hipotesisA = new Clausula(Arrays.asList(noMortal), 0);

        // Ejecuta la resolución por refutación; devuelve true si llega a contradicción
        boolean exitoA = motorA.resolver(hipotesisA);

        // Si no se encontró contradicción, informa que no pudo demostrar la consulta
        if (!exitoA)
            System.out.println("No fue posible demostrar la consulta.");

        // =============================================================
        // PRUEBA B: Resolución con unificación de variables (LPO)
        //
        // Conocimiento en lenguaje natural:
        //   1. Marco es hombre.
        //   2. Marco es pompeyano.
        //   3. Todo pompeyano es romano.
        //   4. César es gobernante.
        //   5. Todo romano es leal a César o lo odia.
        //   6. Quien intenta asesinar a un gobernante no le es leal.
        //   7. Marco intentó asesinar a César.
        //
        // Pregunta: ¿Marco odia a César?
        // =============================================================
        System.out.println("\n--- PRUEBA B: con unificación de variables ---");
        System.out.println("Pregunta: ¿Marco odia a César?");
        System.out.println();

        // Crea el motor en modo CON unificación (true = lógica de primer orden)
        MotorDeResolucion motorB = new MotorDeResolucion(true);

        // Crea el conversor a FNC para transformar las fórmulas LPO
        ConversorFNC conv = new ConversorFNC();

        // Define las constantes del dominio (esVar=false significa que son constantes)
        Termino Marco = new Termino("Marco", false); // constante Marco
        Termino Cesar = new Termino("Cesar", false); // constante César

        // Define las variables de cuantificación (esVar=true significa que son variables)
        Termino vx = new Termino("x", true); // variable x
        Termino vy = new Termino("y", true); // variable y

        System.out.println("Convirtiendo base de conocimiento a FNC:");

        // Regla 1: Hombre(Marco) — hecho simple, Marco es hombre
        motorB.agregarFormula(
            Formula.atomo("Hombre", Arrays.asList(Marco)), conv);

        // Regla 2: Pompeyano(Marco) — hecho simple, Marco es pompeyano
        motorB.agregarFormula(
            Formula.atomo("Pompeyano", Arrays.asList(Marco)), conv);

        // Regla 3: ∀x: Pompeyano(x) => Romano(x)
        // Todo pompeyano es romano
        motorB.agregarFormula(
            Formula.paraTodo("x",
                Formula.implica(
                    Formula.atomo("Pompeyano", Arrays.asList(vx)),  // si es pompeyano
                    Formula.atomo("Romano",    Arrays.asList(vx)))),// entonces es romano
            conv);

        // Regla 4: Gobernante(Cesar) — hecho simple, César es gobernante
        motorB.agregarFormula(
            Formula.atomo("Gobernante", Arrays.asList(Cesar)), conv);

        // Regla 5: ∀x: Romano(x) => (Leal(x,César) v Odia(x,César))
        // Todo romano es leal a César o lo odia
        motorB.agregarFormula(
            Formula.paraTodo("x",
                Formula.implica(
                    Formula.atomo("Romano", Arrays.asList(vx)),           // si es romano
                    Formula.oOp(                                           // entonces:
                        Formula.atomo("Leal", Arrays.asList(vx, Cesar)),  //   es leal a César
                        Formula.atomo("Odia", Arrays.asList(vx, Cesar))))),// o lo odia
            conv);

        // Regla 6: ∀x ∀y: (Hombre(x) ∧ Gobernante(y) ∧ IntentaAsesinar(x,y)) => ~Leal(x,y)
        // Quien intenta asesinar a un gobernante no le es leal
        motorB.agregarFormula(
            Formula.paraTodo("x",
                Formula.paraTodo("y",
                    Formula.implica(
                        Formula.yOp(                                              // si se cumplen los tres:
                            Formula.yOp(
                                Formula.atomo("Hombre",         Arrays.asList(vx)),   // x es hombre
                                Formula.atomo("Gobernante",     Arrays.asList(vy))),  // y es gobernante
                            Formula.atomo("IntentaAsesinar",    Arrays.asList(vx, vy))),// x intentó asesinar a y
                        Formula.no(
                            Formula.atomo("Leal", Arrays.asList(vx, vy)))))),    // entonces x NO es leal a y
            conv);

        // Regla 7: IntentaAsesinar(Marco, Cesar) — hecho simple
        motorB.agregarFormula(
            Formula.atomo("IntentaAsesinar", Arrays.asList(Marco, Cesar)), conv);

        // Niega la pregunta: ~Odia(Marco, Cesar)
        // Si llegamos a contradicción con esta negación, entonces Marco SÍ odia a César
        System.out.println("\nHipótesis negada: ~Odia(Marco, César)");

        // Convierte la negación de la pregunta a FNC y toma la primera (y única) cláusula
        List<Clausula> negadas = conv.convertir(
            Formula.no(Formula.atomo("Odia", Arrays.asList(Marco, Cesar))));

        // Ejecuta la resolución; devuelve true si se demuestra la consulta
        boolean exitoB = motorB.resolver(negadas.get(0));

        // Si no se encontró contradicción, informa que no pudo demostrar la consulta
        if (!exitoB)
            System.out.println("No fue posible demostrar la consulta.");

        // Imprime el pie del programa
        System.out.println("\n=================================================");
        System.out.println("   Fin de la ejecución");
        System.out.println("=================================================");
    }
}
