/*:-----------------------------------------------------------------------------
 *:                       INSTITUTO TECNOLOGICO DE LA LAGUNA
 *:                     INGENIERIA EN SISTEMAS COMPUTACIONALES
 *:                         LENGUAJES Y AUTOMATAS II
 *:
 *:        SEMESTRE: ______________            HORA: ______________ HRS
 *:
 *:
 *:    # Clase con la funcionalidad del Generador de Codigo Objeto
 *:
 *:
 *: Archivo       : GenCodigoObj.java
 *: Autor         : Fernando Gil
 *: Fecha         : 03/SEP/2014
 *: Compilador    : Java JDK 7
 *: Descripción   : Esta clase traduce los cuadruplos del C3D a codigo
 *:                 ensamblador para la practica preliminar de Generacion
 *:                 de Codigo Objeto de PaytonTK. En esta version solo se
 *:                 traducen sentencias de asignacion y cada cuadruplo se
 *:                 convierte directamente a ASM para MASM/Irvine32, sin
 *:                 retener resultados parciales en registros ni optimizar.
 *:
 *: Ult.Modif.    :
 *:  Fecha      Modificó            Modificacion
 *:=============================================================================
 *: 24/May/2023 F.Gil              -Generar la plantilla de programa Ensamblador
 *: 26/MAY/2026                    -Se simplifico la etapa al alcance preliminar
 *:                                 de la practica: solo asignaciones con
 *:                                 traduccion directa cuadruplo -> ASM.
 *: 27/MAY/2026                    -Se adapto la plantilla de salida a
 *:                                 MASM de 32 bits con Irvine32.
 *:-----------------------------------------------------------------------------
 */

package compilador;

import general.Linea_TS;
import java.util.ArrayList;
import java.util.LinkedHashSet;

public class GenCodigoObj {

    private static final String AX = "EAX";
    private static final String BX = "EBX";
    private static final String FIRMA_PREFIJO = "func(";
    private static final String AMBITO_FUNCION = "fun";
    private static final String AMBITO_PARAM_PREFIJO = "param:";

    private Compilador cmp;
    private ArrayList<Cuadruplo> cuadruplos = new ArrayList<Cuadruplo>();
    private final LinkedHashSet<String> nombresVariables = new LinkedHashSet<String>();
    private final LinkedHashSet<String> nombresTemporales = new LinkedHashSet<String>();

    //--------------------------------------------------------------------------
    // Constructor de la clase, recibe la referencia de la clase principal del
    // compilador.
    //
    public GenCodigoObj(Compilador c) {
        cmp = c;
    }
    // Fin del Constructor
    //--------------------------------------------------------------------------

    public void generar() {
        if (cmp.cua.getTamano() == 0) {
            cmp.be.restablecer();
            cmp.cua.inicializar();
            cmp.gci.generar();
        }

        cuadruplos = cmp.cua.getCuadruplos();
        prepararMetadata();

        genEncabezadoASM();
        genDeclaraVarsASM();
        genSegmentoCodigo();
        algoritmoGCO();
        genPieASM();
    }

    //--------------------------------------------------------------------------
    // Genera las primeras lineas del programa Ensamblador hasta antes de la
    // declaracion de variables.

    private void genEncabezadoASM() {
        mostrar("TITLE CodigoObjeto ( codigoObjeto.asm )");
        mostrar("; Descripcion del programa: Automatas II");
        mostrar("; Fecha de creacion: Ene-Jun/2023");
        mostrar("; Revisiones:");
        mostrar("; Fecha de ult. modificacion: 27/MAY/2026");
        mostrar("");
        mostrar(".386");
        mostrar(".model flat, stdcall");
        mostrar(".stack 4096");
        mostrar("INCLUDE Irvine32.inc");
        mostrar("");
        mostrar(".data");
        mostrar("  ; (aqui se insertan las variables)");
    }

    //--------------------------------------------------------------------------
    // Genera la seccion .data con variables del programa, temporales del C3D
    // y mensajes auxiliares para imprimir resultados.

    private void genDeclaraVarsASM() {
        for (String variable : nombresVariables) {
            mostrar("  " + variable + " DWORD 0");
        }

        for (String temporal : nombresTemporales) {
            mostrar("  " + temporal + " DWORD 0");
        }

        if (!nombresVariables.isEmpty()) {
            // Etiquetas de apoyo para mostrar "x = ", "y = ", etc. con Irvine32.
            mostrar("");
            for (String variable : nombresVariables) {
                mostrar("  msg_" + variable + " BYTE \"" + variable + " = \",0");
            }
        }

        mostrar("");
    }

    //--------------------------------------------------------------------------
    // Abre el procedimiento principal donde se insertan las instrucciones
    // traducidas desde los cuadruplos.

    private void genSegmentoCodigo() {
        mostrar(".code");
        mostrar("main PROC");
        mostrar("  ; (aqui se insertan las instrucciones ejecutables)");
    }

    //--------------------------------------------------------------------------
    // Cierra el programa y agrega la impresion final de variables.

    private void genPieASM() {
        emitirImpresionVariables();
        mostrar("  exit");
        mostrar("main ENDP");
        mostrar("");
        mostrar("END main");
    }

    //--------------------------------------------------------------------------
    // Recorre los cuadruplos y traduce solo el subconjunto autorizado para
    // esta entrega preliminar: asignaciones simples y binarias.

    private void algoritmoGCO() {
        for (int i = 0; i < cuadruplos.size(); i++) {
            Cuadruplo cuadruplo = cuadruplos.get(i);

            if (":=".equals(cuadruplo.op)) {
                traducirAsignacionSimple(cuadruplo);
            } else if (esOperacionBinaria(cuadruplo.op)) {
                traducirAsignacionBinaria(cuadruplo);
            } else {
                errorCodObj("Esta practica preliminar solo traduce sentencias de asignacion");
                return;
            }
        }
    }

    //--------------------------------------------------------------------------
    // Prepara los nombres que se declararan en ASM: variables reales del
    // programa y temporales generados por el codigo intermedio.

    private void prepararMetadata() {
        nombresVariables.clear();
        nombresTemporales.clear();

        for (int i = 1; i < cmp.ts.getTamaño(); i++) {
            Linea_TS elemento = cmp.ts.obt_elemento(i);
            String complex = elemento.getComplex();
            String tipo = elemento.getTipo();
            String ambito = elemento.getAmbito();

            if (!"id".equals(complex)) {
                continue;
            }

            if (AMBITO_FUNCION.equals(ambito)
                    || (ambito != null && ambito.startsWith(AMBITO_PARAM_PREFIJO))
                    || esFirmaFuncion(tipo)) {
                continue;
            }

            nombresVariables.add(elemento.getLexema());
        }

        for (Cuadruplo cuadruplo : cuadruplos) {
            registrarTemporal(cuadruplo.arg1);
            registrarTemporal(cuadruplo.arg2);
            registrarTemporal(cuadruplo.resultado);
        }
    }

    private void registrarTemporal(String operando) {
        String nombre = nombreDesdeOperando(operando);
        if (esTemporal(nombre)) {
            nombresTemporales.add(nombre);
        }
    }

    //--------------------------------------------------------------------------
    // Traduccion directa de cuadruplos de asignacion.
    // Cada cuadruplo se baja a ASM sin reutilizar resultados en registros.

    private void traducirAsignacionSimple(Cuadruplo cuadruplo) {
        String destino = nombreDesdeOperando(cuadruplo.resultado);
        String fuente = nombreDesdeOperando(cuadruplo.arg1);

        if (!operandoEnteroValido(destino) || !operandoEnteroValido(fuente)) {
            errorCodObj("La traduccion preliminar a ASM solo soporta asignaciones enteras");
            return;
        }

        mostrar("  mov " + AX.toLowerCase() + ", " + fuente);
        mostrar("  mov " + destino + ", " + AX.toLowerCase());
    }

    private void traducirAsignacionBinaria(Cuadruplo cuadruplo) {
        String destino = nombreDesdeOperando(cuadruplo.resultado);
        String izquierdo = nombreDesdeOperando(cuadruplo.arg1);
        String derecho = nombreDesdeOperando(cuadruplo.arg2);

        if (!operandoEnteroValido(destino)
                || !operandoEnteroValido(izquierdo)
                || !operandoEnteroValido(derecho)) {
            errorCodObj("La traduccion preliminar a ASM solo soporta operaciones enteras");
            return;
        }

        mostrar("  mov " + AX.toLowerCase() + ", " + izquierdo);
        mostrar("  mov " + BX.toLowerCase() + ", " + derecho);

        if ("+".equals(cuadruplo.op)) {
            mostrar("  add " + AX.toLowerCase() + ", " + BX.toLowerCase());
        } else if ("-".equals(cuadruplo.op)) {
            mostrar("  sub " + AX.toLowerCase() + ", " + BX.toLowerCase());
        } else if ("*".equals(cuadruplo.op)) {
            mostrar("  imul " + AX.toLowerCase() + ", " + BX.toLowerCase());
        } else {
            errorCodObj("Operacion no soportada en ASM: " + cuadruplo.op);
            return;
        }

        mostrar("  mov " + destino + ", " + AX.toLowerCase());
    }

    //--------------------------------------------------------------------------
    // Impresion final de resultados con Irvine32.
    // Esta salida se agrega como apoyo de demostracion al ejecutar el ASM.

    private void emitirImpresionVariables() {
        if (nombresVariables.isEmpty()) {
            return;
        }

        mostrar("");
        mostrar("  ; Imprimir valores finales de las variables");

        for (String variable : nombresVariables) {
            mostrar("  mov edx, OFFSET msg_" + variable);
            mostrar("  call WriteString");
            mostrar("  mov eax, " + variable);
            mostrar("  call WriteDec");
            mostrar("  call Crlf");
        }
    }

    //--------------------------------------------------------------------------
    // Salida hacia la pestaña de codigo objeto y reporte de errores.

    private void mostrar(String linea) {
        if (cmp.iuListener != null) {
            cmp.iuListener.mostrarCodObj(linea);
        }
    }

    private void errorCodObj(String mensaje) {
        cmp.me.error(Compilador.ERR_CODOBJ, mensaje);
    }

    //--------------------------------------------------------------------------
    // Convierte operandos internos del compilador a nombres usables en ASM
    // y valida el subconjunto entero soportado por esta entrega.

    private String nombreDesdeOperando(String operando) {
        if (operando == null || "".equals(operando)) {
            return "";
        }

        if (esTemporal(operando) || esInmediato(operando)) {
            return operando;
        }

        if (!esReferenciaTS(operando)) {
            return operando;
        }

        int entrada = entradaDeReferencia(operando);
        if (entrada <= 0 || entrada >= cmp.ts.getTamaño()) {
            return operando;
        }

        return cmp.ts.obt_elemento(entrada).getLexema();
    }

    private boolean operandoEnteroValido(String operando) {
        if (operando == null || "".equals(operando)) {
            return false;
        }

        if (esInmediato(operando)) {
            return !operando.contains(".");
        }

        if (esTemporal(operando)) {
            return true;
        }

        return nombresVariables.contains(operando);
    }

    private boolean esReferenciaTS(String operando) {
        return operando != null
                && operando.startsWith("[")
                && operando.endsWith("]");
    }

    private int entradaDeReferencia(String operando) {
        try {
            return Integer.parseInt(operando.substring(1, operando.length() - 1));
        } catch (Exception ex) {
            return -1;
        }
    }

    private boolean esTemporal(String nombre) {
        return nombre != null && nombre.matches("t\\d+");
    }

    private boolean esInmediato(String operando) {
        return operando != null && operando.matches("-?\\d+");
    }

    private boolean esOperacionBinaria(String op) {
        return "+".equals(op) || "-".equals(op) || "*".equals(op);
    }

    private boolean esFirmaFuncion(String tipo) {
        return tipo != null && tipo.startsWith(FIRMA_PREFIJO);
    }
}
