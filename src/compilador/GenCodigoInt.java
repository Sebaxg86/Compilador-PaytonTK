/*:-----------------------------------------------------------------------------
 *:                       INSTITUTO TECNOLOGICO DE LA LAGUNA
 *:                     INGENIERIA EN SISTEMAS COMPUTACIONALES
 *:                         LENGUAJES Y AUTOMATAS II
 *:
 *:        SEMESTRE: ______________            HORA: ______________ HRS
 *:
 *:
 *:    # Clase con la funcionalidad del Generador de Codigo Intermedio
 *:
 *:
 *: Archivo       : GenCodigoInt.java
 *: Autor         : Fernando Gil
 *: Fecha         : 03/SEP/2014
 *: Compilador    : Java JDK 7
 *: Descripción   : Esta clase implementa la generacion de Codigo Intermedio
 *:                 mediante un traductor predictivo recursivo dirigido por la
 *:                 sintaxis, siguiendo el metodo clasico del Libro del Dragon.
 *:
 *: Ult.Modif.    :
 *:  Fecha      Modificó            Modificacion
 *:=============================================================================
 *: 08/MAY/2026 Sebas               -Se implemento el generador de Codigo
 *:                                 Intermedio de PaytonTK con acciones
 *:                                 semanticas embebidas y emision de
 *:                                 C3D.
 *:-----------------------------------------------------------------------------
 */

package compilador;

import general.Linea_BE;
import java.util.ArrayList;

public class GenCodigoInt {

    private static final String NIL = "nil";

    private Compilador cmp;
    private String preAnalisis;
    private int consecutivoTemp = 1;
    private int consecutivoEtiq = 1;
    private final ArrayList<String> codigoIntermedio = new ArrayList<String>();

    //--------------------------------------------------------------------------
    // Constructor de la clase, recibe la referencia de la clase principal del
    // compilador.
    //
    public GenCodigoInt(Compilador c) {
        cmp = c;
    }
    // Fin del Constructor
    //--------------------------------------------------------------------------

    // Punto de entrada de la etapa: recorre el buffer de entrada, emite C3D
    // y al final construye la tabla de cuadruplos desde ese C3D.
    public void generar() {
        consecutivoTemp = 1;
        consecutivoEtiq = 1;
        codigoIntermedio.clear();
        preAnalisis = cmp.be.preAnalisis.complex;

        Atributos programa = new Atributos();
        PROGRAMA(programa);

        if (!preAnalisis.equals("$")) {
            errorEmparejar("$", cmp.be.preAnalisis.lexema, cmp.be.preAnalisis.numLinea);
        }

        cmp.cua.generarDesdeCodigoIntermedio(codigoIntermedio);
    }

    //--------------------------------------------------------------------------

    // Consume el token esperado por la produccion actual del parser predictivo.
    private void emparejar(String t) {
        if (cmp.be.preAnalisis.complex.equals(t)) {
            cmp.be.siguiente();
            preAnalisis = cmp.be.preAnalisis.complex;
        } else {
            errorEmparejar(t, cmp.be.preAnalisis.lexema, cmp.be.preAnalisis.numLinea);
        }
    }

    //--------------------------------------------------------------------------

    // Reporta errores de token esperado durante la pasada de GCI.
    private void errorEmparejar(String tokenEsperado, String lexemaEncontrado, int numLinea) {
        String msjError = "";

        if (tokenEsperado.equals("id")) {
            msjError += "Se esperaba un identificador";
        } else if (tokenEsperado.equals("num")) {
            msjError += "Se esperaba una constante entera";
        } else if (tokenEsperado.equals("num.num")) {
            msjError += "Se esperaba una constante real";
        } else if (tokenEsperado.equals("literal")) {
            msjError += "Se esperaba una literal";
        } else if (tokenEsperado.equals("oprel")) {
            msjError += "Se esperaba un operador relacional";
        } else if (tokenEsperado.equals("opasig")) {
            msjError += "Se esperaba operador de asignacion";
        } else if (tokenEsperado.equals("$")) {
            msjError += "Se esperaba fin de archivo";
        } else {
            msjError += "Se esperaba " + tokenEsperado;
        }

        msjError += " se encontró "
                + (lexemaEncontrado.equals("$") ? "fin de archivo" : lexemaEncontrado)
                + ". Linea " + numLinea;

        cmp.me.error(Compilador.ERR_CODINT, msjError);

        // Avanzar token para evitar ciclo infinito.
        cmp.be.siguiente();
        preAnalisis = cmp.be.preAnalisis.complex;
    }

    //--------------------------------------------------------------------------

    // Reporta errores generales de la gramatica durante la generacion de C3D.
    private void error(String descripError) {
        cmp.me.error(Compilador.ERR_CODINT, descripError);

        // Avanzar token para evitar ciclo infinito.
        cmp.be.siguiente();
        preAnalisis = cmp.be.preAnalisis.complex;
    }

    //--------------------------------------------------------------------------
    // Rutinas auxiliares de generacion de codigo intermedio
    //--------------------------------------------------------------------------

    // Consume el cierre de bloque "::" de PaytonTK.
    private void emparejarFinBloque() {
        // El lexer actual entrega "::" como dos tokens ":" consecutivos.
        emparejar(":");
        emparejar(":");
    }

    // Conjunto PRIMERO usado para decidir si aun hay instrucciones por procesar.
    private boolean esInicioPrograma() {
        return "def".equals(preAnalisis) || esInicioProposicion();
    }

    // Conjunto PRIMERO de los tipos declarables en PaytonTK.
    private boolean esInicioTipoDato() {
        return "int".equals(preAnalisis) || "float".equals(preAnalisis)
                || "string".equals(preAnalisis);
    }

    // Conjunto PRIMERO de una proposicion del lenguaje.
    private boolean esInicioProposicion() {
        return esInicioTipoDato() || "id".equals(preAnalisis) || "if".equals(preAnalisis)
                || "while".equals(preAnalisis) || "print".equals(preAnalisis);
    }

    // Conjunto PRIMERO de una expresion aritmetica o literal.
    private boolean esInicioExpresion() {
        return "id".equals(preAnalisis) || "num".equals(preAnalisis)
                || "num.num".equals(preAnalisis) || "(".equals(preAnalisis)
                || "literal".equals(preAnalisis);
    }

    // Genera nombres temporales para almacenar resultados intermedios de C3D.
    private String tempnuevo() {
        return "t" + consecutivoTemp++;
    }

    // Genera etiquetas para saltos y puntos de control del C3D.
    private String etiqnueva() {
        return "etiq" + consecutivoEtiq++;
    }

    // Convierte la referencia de una funcion en una etiqueta interna estable.
    private String etiqfun(String entrada) {
        if (entrada == null || "".equals(entrada)) {
            return "fun0";
        }

        if (entrada.startsWith("[") && entrada.endsWith("]")) {
            return "fun" + entrada.substring(1, entrada.length() - 1);
        }

        return "fun_" + entrada.replace(' ', '_');
    }

    // Obtiene el lugar de un token: referencia [n] a TS o lexema directo.
    private String referencia(Linea_BE token) {
        if (token == null) {
            return NIL;
        }

        if (token.getEntrada() > 0) {
            return "[" + token.getEntrada() + "]";
        }

        return token.getLexema();
    }

    // Reconoce el valor especial usado para representar ausencia de resultado.
    private boolean esNil(String lugar) {
        return lugar == null || "".equals(lugar) || NIL.equals(lugar);
    }

    // Emite una proposicion de tres direcciones textual; los cuadruplos se
    // construyen despues a partir de la lista codigoIntermedio.
    private void emite(String op, String arg1, String arg2, String resultado) {
        String sentencia = formatearC3D(op, arg1, arg2, resultado);
        codigoIntermedio.add(sentencia);

        if (cmp.iuListener != null) {
            cmp.iuListener.mostrarCodInt(sentencia);
        }
    }

    // Traduce los campos logicos de una instruccion a la forma textual de C3D.
    private String formatearC3D(String op, String arg1, String arg2, String resultado) {
        if ("label".equals(op)) {
            return resultado + ":";
        } else if ("goto".equals(op)) {
            return "goto " + resultado;
        } else if (op.startsWith("if")) {
            return "if " + arg1 + " " + op.substring(2) + " "
                    + arg2 + " goto " + resultado;
        } else if (":=".equals(op)) {
            return resultado + " := " + arg1;
        } else if ("param".equals(op)) {
            return "param " + arg1;
        } else if ("call".equals(op)) {
            if ("".equals(resultado)) {
                return "call " + arg1 + ", " + arg2;
            }
            return resultado + " := call " + arg1 + ", " + arg2;
        } else if ("return".equals(op)) {
            return "".equals(arg1) ? "return" : "return " + arg1;
        } else if ("print".equals(op)) {
            return "print " + arg1;
        } else if ("".equals(arg2)) {
            return resultado + " := " + op + " " + arg1;
        }

        return resultado + " := " + arg1 + " " + op + " " + arg2;
    }

    //--------------------------------------------------------------------------
    //  *  *   *   *    PEGAR AQUI EL CODIGO DE LOS PROCEDURES  *  *  *  *
    //--------------------------------------------------------------------------

    // ---------------- Procedure 1 ----------------
    // Reconoce el programa completo como una secuencia de instrucciones.
    private void PROGRAMA(Atributos _PROGRAMA) {
        Atributos _INSTRUCCION = new Atributos();
        Atributos _PROGRAMA1 = new Atributos();

        if (esInicioPrograma()) {
            // PROGRAMA -> INSTRUCCION PROGRAMA
            INSTRUCCION(_INSTRUCCION);
            PROGRAMA(_PROGRAMA1);

            // Accion semantica 1.
            // La concatenacion del codigo se produce directamente al emitir.
            // Fin accion semantica 1.
        } else {
            // PROGRAMA -> empty

            // Accion semantica 2.
            // Produccion vacia: no emite codigo.
            // Fin accion semantica 2.
        }
    }

    // ---------------- Procedure 2 ----------------
    // Despacha cada instruccion hacia declaracion de funcion o proposicion.
    private void INSTRUCCION(Atributos _INSTRUCCION) {
        Atributos _FUNCION = new Atributos();
        Atributos _PROPOSICION = new Atributos();

        if (preAnalisis.equals("def")) {
            // INSTRUCCION -> FUNCION
            FUNCION(_FUNCION);

            // Accion semantica 3.
            // No emite codigo adicional.
            // Fin accion semantica 3.
        } else if (esInicioProposicion()) {
            // INSTRUCCION -> PROPOSICION
            PROPOSICION(_PROPOSICION);

            // Accion semantica 4.
            // No emite codigo adicional.
            // Fin accion semantica 4.
        } else {
            error("Se esperaba una instruccion o funcion");
        }
    }

    // ---------------- Procedure 3 ----------------
    // Traduce una definicion de funcion y genera etiquetas de entrada/salida.
    private void FUNCION(Atributos _FUNCION) {
        Atributos _ARGUMENTOS = new Atributos();
        Atributos _TIPO_RETORNO = new Atributos();
        Atributos _PROPOSICIONES_OPTATIVAS = new Atributos();
        Atributos _RESULTADO = new Atributos();
        Linea_BE id = new Linea_BE();

        if (preAnalisis.equals("def")) {
            // FUNCION -> def id ( ARGUMENTOS ) : TIPO_RETORNO PROPOSICIONES_OPTATIVAS return RESULTADO ::
            emparejar("def");
            id = cmp.be.preAnalisis;
            emparejar("id");

            // Accion semantica 5.
            _FUNCION.h = referencia(id);
            _FUNCION.siguiente = etiqnueva();
            emite("goto", "", "", _FUNCION.siguiente);
            emite("label", "", "", etiqfun(_FUNCION.h));
            // Fin accion semantica 5.

            emparejar("(");
            ARGUMENTOS(_ARGUMENTOS);
            emparejar(")");
            emparejar(":");
            TIPO_RETORNO(_TIPO_RETORNO);
            PROPOSICIONES_OPTATIVAS(_PROPOSICIONES_OPTATIVAS);
            emparejar("return");
            RESULTADO(_RESULTADO);

            // Accion semantica 6.
            if (esNil(_RESULTADO.lugar)) {
                emite("return", "", "", "");
            } else {
                emite("return", _RESULTADO.lugar, "", "");
            }
            // Fin accion semantica 6.

            emparejarFinBloque();

            // Accion semantica 7.
            emite("label", "", "", _FUNCION.siguiente);
            // Fin accion semantica 7.
        } else {
            error("Se esperaba la definicion de una funcion");
        }
    }

    // ---------------- Procedure 4 ----------------
    // Reconoce la lista inicial de parametros formales de una funcion.
    private void ARGUMENTOS(Atributos _ARGUMENTOS) {
        Atributos _TIPO_DATO = new Atributos();
        Atributos _ARGUMENTOS_2 = new Atributos();

        if (esInicioTipoDato()) {
            // ARGUMENTOS -> TIPO_DATO id ARGUMENTOS'
            TIPO_DATO(_TIPO_DATO);
            emparejar("id");
            ARGUMENTOS_2(_ARGUMENTOS_2);

            // Accion semantica 8.
            // Los parametros formales no emiten cuádruplos en GCI.
            // Fin accion semantica 8.
        } else {
            // ARGUMENTOS -> empty

            // Accion semantica 9.
            // Produccion vacia: no emite codigo.
            // Fin accion semantica 9.
        }
    }

    // ---------------- Procedure 5 ----------------
    // Reconoce parametros formales adicionales separados por coma.
    private void ARGUMENTOS_2(Atributos _ARGUMENTOS_2) {
        Atributos _TIPO_DATO = new Atributos();
        Atributos _ARGUMENTOS_21 = new Atributos();

        if (preAnalisis.equals(",")) {
            // ARGUMENTOS' -> , TIPO_DATO id ARGUMENTOS'
            emparejar(",");
            TIPO_DATO(_TIPO_DATO);
            emparejar("id");
            ARGUMENTOS_2(_ARGUMENTOS_21);

            // Accion semantica 10.
            // Los parametros formales no emiten cuádruplos en GCI.
            // Fin accion semantica 10.
        } else {
            // ARGUMENTOS' -> empty

            // Accion semantica 11.
            // Produccion vacia: no emite codigo.
            // Fin accion semantica 11.
        }
    }

    // ---------------- Procedure 6 ----------------
    // Reconoce declaraciones de variables; no emite C3D ejecutable.
    private void DECLARACION_VARS(Atributos _DECLARACION_VARS) {
        Atributos _TIPO_DATO = new Atributos();
        Atributos _DECLARACION_VARS_2 = new Atributos();

        if (esInicioTipoDato()) {
            // DECLARACION_VARS -> TIPO_DATO id DECLARACION_VARS'
            TIPO_DATO(_TIPO_DATO);
            emparejar("id");
            DECLARACION_VARS_2(_DECLARACION_VARS_2);

            // Accion semantica 12.
            // Las declaraciones no emiten cuádruplos en esta etapa.
            // Fin accion semantica 12.
        } else {
            error("Declaracion de variables invalida");
        }
    }

    // ---------------- Procedure 7 ----------------
    // Reconoce identificadores adicionales en una declaracion de variables.
    private void DECLARACION_VARS_2(Atributos _DECLARACION_VARS_2) {
        Atributos _DECLARACION_VARS_21 = new Atributos();

        if (preAnalisis.equals(",")) {
            // DECLARACION_VARS' -> , id DECLARACION_VARS'
            emparejar(",");
            emparejar("id");
            DECLARACION_VARS_2(_DECLARACION_VARS_21);

            // Accion semantica 13.
            // Las declaraciones no emiten cuádruplos en esta etapa.
            // Fin accion semantica 13.
        } else {
            // DECLARACION_VARS' -> empty

            // Accion semantica 14.
            // Produccion vacia: no emite codigo.
            // Fin accion semantica 14.
        }
    }

    // ---------------- Procedure 8 ----------------
    // Reconoce tipos de dato basicos del lenguaje.
    private void TIPO_DATO(Atributos _TIPO_DATO) {
        if (preAnalisis.equals("int")) {
            emparejar("int");

            // Accion semantica 17.
            // No emite codigo.
            // Fin accion semantica 17.
        } else if (preAnalisis.equals("float")) {
            emparejar("float");

            // Accion semantica 18.
            // No emite codigo.
            // Fin accion semantica 18.
        } else if (preAnalisis.equals("string")) {
            emparejar("string");

            // Accion semantica 19.
            // No emite codigo.
            // Fin accion semantica 19.
        } else {
            error("Tipo de dato invalido");
        }
    }

    // ---------------- Procedure 9 ----------------
    // Reconoce el tipo de retorno de una funcion.
    private void TIPO_RETORNO(Atributos _TIPO_RETORNO) {
        Atributos _TIPO_DATO = new Atributos();

        if (preAnalisis.equals("void")) {
            emparejar("void");

            // Accion semantica 15.
            // No emite codigo.
            // Fin accion semantica 15.
        } else if (esInicioTipoDato()) {
            // TIPO_RETORNO -> TIPO_DATO
            TIPO_DATO(_TIPO_DATO);

            // Accion semantica 16.
            // No emite codigo.
            // Fin accion semantica 16.
        } else {
            error("Se esperaba un tipo de retorno");
        }
    }

    // ---------------- Procedure 10 ---------------
    // Traduce el resultado de return y propaga su lugar.
    private void RESULTADO(Atributos _RESULTADO) {
        Atributos _EXPRESION = new Atributos();

        if (esInicioExpresion()) {
            // RESULTADO -> EXPRESION
            EXPRESION(_EXPRESION);

            // Accion semantica 20.
            _RESULTADO.lugar = _EXPRESION.lugar;
            // Fin accion semantica 20.
        } else if (preAnalisis.equals("void")) {
            // RESULTADO -> void
            emparejar("void");

            // Accion semantica 21.
            _RESULTADO.lugar = NIL;
            // Fin accion semantica 21.
        } else {
            error("Se esperaba un resultado");
        }
    }

    // ---------------- Procedure 11 ---------------
    // Reconoce cero o mas proposiciones dentro de un bloque.
    private void PROPOSICIONES_OPTATIVAS(Atributos _PROPOSICIONES_OPTATIVAS) {
        Atributos _PROPOSICION = new Atributos();
        Atributos _PROPOSICIONES_OPTATIVAS1 = new Atributos();

        if (esInicioProposicion()) {
            // PROPOSICIONES_OPTATIVAS -> PROPOSICION PROPOSICIONES_OPTATIVAS
            PROPOSICION(_PROPOSICION);
            PROPOSICIONES_OPTATIVAS(_PROPOSICIONES_OPTATIVAS1);

            // Accion semantica 22.
            // La concatenacion del codigo se produce directamente al emitir.
            // Fin accion semantica 22.
        } else {
            // PROPOSICIONES_OPTATIVAS -> empty

            // Accion semantica 23.
            // Produccion vacia: no emite codigo.
            // Fin accion semantica 23.
        }
    }

    // ---------------- Procedure 12 ---------------
    // Traduce las proposiciones ejecutables: asignacion, if, while, print, etc.
    private void PROPOSICION(Atributos _PROPOSICION) {
        Atributos _DECLARACION_VARS = new Atributos();
        Atributos _PROPOSICION_2 = new Atributos();
        Atributos _CONDICION = new Atributos();
        Atributos _PROPOSICIONES_OPTATIVAS1 = new Atributos();
        Atributos _PROPOSICIONES_OPTATIVAS2 = new Atributos();
        Atributos _EXPRESION = new Atributos();
        Linea_BE id = new Linea_BE();

        if (esInicioTipoDato()) {
            // PROPOSICION -> DECLARACION_VARS
            DECLARACION_VARS(_DECLARACION_VARS);

            // Accion semantica 24.
            // No emite codigo adicional.
            // Fin accion semantica 24.
        } else if (preAnalisis.equals("id")) {
            // PROPOSICION -> id PROPOSICION'
            id = cmp.be.preAnalisis;
            emparejar("id");

            // Accion semantica 25.
            _PROPOSICION_2.h = referencia(id);
            // Fin accion semantica 25.

            PROPOSICION_2(_PROPOSICION_2);

            // Accion semantica 26.
            _PROPOSICION.lugar = _PROPOSICION_2.lugar;
            // Fin accion semantica 26.
        } else if (preAnalisis.equals("if")) {
            // PROPOSICION -> if CONDICION : PROPOSICIONES_OPTATIVAS else : PROPOSICIONES_OPTATIVAS ::
            emparejar("if");

            // Accion semantica 27.
            _PROPOSICION.siguiente = etiqnueva();
            _CONDICION.verdadera = etiqnueva();
            _CONDICION.falsa = etiqnueva();
            // Fin accion semantica 27.

            CONDICION(_CONDICION);
            emparejar(":");

            // Accion semantica 28.
            emite("label", "", "", _CONDICION.verdadera);
            // Fin accion semantica 28.

            PROPOSICIONES_OPTATIVAS(_PROPOSICIONES_OPTATIVAS1);
            emparejar("else");
            emparejar(":");

            // Accion semantica 29.
            emite("goto", "", "", _PROPOSICION.siguiente);
            emite("label", "", "", _CONDICION.falsa);
            // Fin accion semantica 29.

            PROPOSICIONES_OPTATIVAS(_PROPOSICIONES_OPTATIVAS2);
            emparejarFinBloque();

            // Accion semantica 30.
            emite("label", "", "", _PROPOSICION.siguiente);
            // Fin accion semantica 30.
        } else if (preAnalisis.equals("while")) {
            // PROPOSICION -> while CONDICION : PROPOSICIONES_OPTATIVAS ::
            emparejar("while");

            // Accion semantica 31.
            _PROPOSICION.comienzo = etiqnueva();
            _PROPOSICION.siguiente = etiqnueva();
            _CONDICION.verdadera = etiqnueva();
            _CONDICION.falsa = _PROPOSICION.siguiente;
            emite("label", "", "", _PROPOSICION.comienzo);
            // Fin accion semantica 31.

            CONDICION(_CONDICION);
            emparejar(":");

            // Accion semantica 32.
            emite("label", "", "", _CONDICION.verdadera);
            // Fin accion semantica 32.

            PROPOSICIONES_OPTATIVAS(_PROPOSICIONES_OPTATIVAS1);
            emparejarFinBloque();

            // Accion semantica 33.
            emite("goto", "", "", _PROPOSICION.comienzo);
            emite("label", "", "", _PROPOSICION.siguiente);
            // Fin accion semantica 33.
        } else if (preAnalisis.equals("print")) {
            // PROPOSICION -> print ( EXPRESION )
            emparejar("print");
            emparejar("(");
            EXPRESION(_EXPRESION);
            emparejar(")");

            // Accion semantica 34.
            emite("print", _EXPRESION.lugar, "", "");
            // Fin accion semantica 34.
        } else {
            error("Proposicion invalida");
        }
    }

    // ---------------- Procedure 13 ---------------
    // Decide si un identificador inicia asignacion o llamada a funcion.
    private void PROPOSICION_2(Atributos _PROPOSICION_2) {
        Atributos _EXPRESION = new Atributos();
        Atributos _LISTA_EXPRESIONES = new Atributos();

        if (preAnalisis.equals("opasig")) {
            // PROPOSICION' -> opasig EXPRESION
            emparejar("opasig");
            EXPRESION(_EXPRESION);

            // Accion semantica 35.
            emite(":=", _EXPRESION.lugar, "", _PROPOSICION_2.h);
            _PROPOSICION_2.lugar = _PROPOSICION_2.h;
            // Fin accion semantica 35.
        } else if (preAnalisis.equals("(")) {
            // PROPOSICION' -> ( LISTA_EXPRESIONES )
            emparejar("(");
            LISTA_EXPRESIONES(_LISTA_EXPRESIONES);
            emparejar(")");

            // Accion semantica 36.
            emite("call", etiqfun(_PROPOSICION_2.h), "" + _LISTA_EXPRESIONES.argc, "");
            _PROPOSICION_2.lugar = NIL;
            // Fin accion semantica 36.
        } else {
            error("Se esperaba asignacion o llamada a funcion");
        }
    }

    // ---------------- Procedure 14 ---------------
    // Traduce la lista de argumentos reales de una llamada.
    private void LISTA_EXPRESIONES(Atributos _LISTA_EXPRESIONES) {
        Atributos _EXPRESION = new Atributos();
        Atributos _LISTA_EXPRESIONES_2 = new Atributos();

        if (esInicioExpresion()) {
            // LISTA_EXPRESIONES -> EXPRESION LISTA_EXPRESIONES'
            EXPRESION(_EXPRESION);

            // Accion semantica 37.
            emite("param", _EXPRESION.lugar, "", "");
            // Fin accion semantica 37.

            LISTA_EXPRESIONES_2(_LISTA_EXPRESIONES_2);

            // Accion semantica 38.
            _LISTA_EXPRESIONES.argc = 1 + _LISTA_EXPRESIONES_2.argc;
            // Fin accion semantica 38.
        } else {
            // LISTA_EXPRESIONES -> empty

            // Accion semantica 39.
            _LISTA_EXPRESIONES.argc = 0;
            // Fin accion semantica 39.
        }
    }

    // ---------------- Procedure 15 ---------------
    // Traduce argumentos reales adicionales separados por coma.
    private void LISTA_EXPRESIONES_2(Atributos _LISTA_EXPRESIONES_2) {
        Atributos _EXPRESION = new Atributos();
        Atributos _LISTA_EXPRESIONES_21 = new Atributos();

        if (preAnalisis.equals(",")) {
            // LISTA_EXPRESIONES' -> , EXPRESION LISTA_EXPRESIONES'
            emparejar(",");
            EXPRESION(_EXPRESION);

            // Accion semantica 40.
            emite("param", _EXPRESION.lugar, "", "");
            // Fin accion semantica 40.

            LISTA_EXPRESIONES_2(_LISTA_EXPRESIONES_21);

            // Accion semantica 41.
            _LISTA_EXPRESIONES_2.argc = 1 + _LISTA_EXPRESIONES_21.argc;
            // Fin accion semantica 41.
        } else {
            // LISTA_EXPRESIONES' -> empty

            // Accion semantica 42.
            _LISTA_EXPRESIONES_2.argc = 0;
            // Fin accion semantica 42.
        }
    }

    // ---------------- Procedure 16 ---------------
    // Traduce una condicion relacional a salto condicional y salto falso.
    private void CONDICION(Atributos _CONDICION) {
        Atributos _EXPRESION1 = new Atributos();
        Atributos _EXPRESION2 = new Atributos();
        Linea_BE oprel = new Linea_BE();

        if (esInicioExpresion()) {
            // CONDICION -> EXPRESION oprel EXPRESION
            EXPRESION(_EXPRESION1);
            oprel = cmp.be.preAnalisis;
            emparejar("oprel");
            EXPRESION(_EXPRESION2);

            // Accion semantica 43.
            emite("if" + oprel.getLexema(), _EXPRESION1.lugar, _EXPRESION2.lugar, _CONDICION.verdadera);
            emite("goto", "", "", _CONDICION.falsa);
            // Fin accion semantica 43.
        } else {
            error("Condicion invalida");
        }
    }

    // ---------------- Procedure 17 ---------------
    // Traduce expresiones y sintetiza el lugar donde queda su valor.
    private void EXPRESION(Atributos _EXPRESION) {
        Atributos _TERMINO = new Atributos();
        Atributos _EXPRESION_2 = new Atributos();
        Linea_BE literal = new Linea_BE();

        if (preAnalisis.equals("id")
                || preAnalisis.equals("num")
                || preAnalisis.equals("num.num")
                || preAnalisis.equals("(")) {

            // EXPRESION -> TERMINO EXPRESION'
            TERMINO(_TERMINO);

            // Accion semantica 44.
            _EXPRESION_2.h = _TERMINO.lugar;
            // Fin accion semantica 44.

            EXPRESION_2(_EXPRESION_2);

            // Accion semantica 45.
            _EXPRESION.lugar = _EXPRESION_2.lugar;
            // Fin accion semantica 45.
        } else if (preAnalisis.equals("literal")) {
            // EXPRESION -> literal
            literal = cmp.be.preAnalisis;
            emparejar("literal");

            // Accion semantica 46.
            _EXPRESION.lugar = referencia(literal);
            // Fin accion semantica 46.
        } else {
            error("Expresion invalida");
        }
    }

    // ---------------- Procedure 18 ---------------
    // Traduce la cola de operadores opsuma usando temporales.
    private void EXPRESION_2(Atributos _EXPRESION_2) {
        Atributos _TERMINO = new Atributos();
        Atributos _EXPRESION_21 = new Atributos();
        Linea_BE opsuma = new Linea_BE();

        if (preAnalisis.equals("opsuma")) {
            // EXPRESION' -> opsuma TERMINO EXPRESION'
            opsuma = cmp.be.preAnalisis;
            emparejar("opsuma");
            TERMINO(_TERMINO);

            // Accion semantica 47.
            _EXPRESION_21.h = tempnuevo();
            emite(opsuma.getLexema(), _EXPRESION_2.h, _TERMINO.lugar, _EXPRESION_21.h);
            // Fin accion semantica 47.

            EXPRESION_2(_EXPRESION_21);

            // Accion semantica 48.
            _EXPRESION_2.lugar = _EXPRESION_21.lugar;
            // Fin accion semantica 48.
        } else {
            // EXPRESION' -> empty

            // Accion semantica 49.
            _EXPRESION_2.lugar = _EXPRESION_2.h;
            // Fin accion semantica 49.
        }
    }

    // ---------------- Procedure 19 ---------------
    // Traduce terminos y conserva precedencia sobre operadores opsuma.
    private void TERMINO(Atributos _TERMINO) {
        Atributos _FACTOR = new Atributos();
        Atributos _TERMINO_2 = new Atributos();

        if (preAnalisis.equals("id")
                || preAnalisis.equals("num")
                || preAnalisis.equals("num.num")
                || preAnalisis.equals("(")) {

            // TERMINO -> FACTOR TERMINO'
            FACTOR(_FACTOR);

            // Accion semantica 50.
            _TERMINO_2.h = _FACTOR.lugar;
            // Fin accion semantica 50.

            TERMINO_2(_TERMINO_2);

            // Accion semantica 51.
            _TERMINO.lugar = _TERMINO_2.lugar;
            // Fin accion semantica 51.
        } else {
            error("Termino invalido");
        }
    }

    // ---------------- Procedure 20 ---------------
    // Traduce la cola de multiplicaciones usando temporales.
    private void TERMINO_2(Atributos _TERMINO_2) {
        Atributos _FACTOR = new Atributos();
        Atributos _TERMINO_21 = new Atributos();
        Linea_BE opmult = new Linea_BE();

        if (preAnalisis.equals("opmult")) {
            // TERMINO' -> opmult FACTOR TERMINO'
            opmult = cmp.be.preAnalisis;
            emparejar("opmult");
            FACTOR(_FACTOR);

            // Accion semantica 52.
            _TERMINO_21.h = tempnuevo();
            emite(opmult.getLexema(), _TERMINO_2.h, _FACTOR.lugar, _TERMINO_21.h);
            // Fin accion semantica 52.

            TERMINO_2(_TERMINO_21);

            // Accion semantica 53.
            _TERMINO_2.lugar = _TERMINO_21.lugar;
            // Fin accion semantica 53.
        } else {
            // TERMINO' -> empty

            // Accion semantica 54.
            _TERMINO_2.lugar = _TERMINO_2.h;
            // Fin accion semantica 54.
        }
    }

    // ---------------- Procedure 21 ---------------
    // Traduce factores: ids, constantes, parentesis y llamadas.
    private void FACTOR(Atributos _FACTOR) {
        Atributos _FACTOR_2 = new Atributos();
        Atributos _EXPRESION = new Atributos();
        Linea_BE id = new Linea_BE();
        Linea_BE num = new Linea_BE();
        Linea_BE numNum = new Linea_BE();

        if (preAnalisis.equals("id")) {
            // FACTOR -> id FACTOR'
            id = cmp.be.preAnalisis;
            emparejar("id");

            // Accion semantica 55.
            _FACTOR_2.h = referencia(id);
            // Fin accion semantica 55.

            FACTOR_2(_FACTOR_2);

            // Accion semantica 56.
            _FACTOR.lugar = _FACTOR_2.lugar;
            // Fin accion semantica 56.
        } else if (preAnalisis.equals("num")) {
            num = cmp.be.preAnalisis;
            emparejar("num");

            // Accion semantica 57.
            _FACTOR.lugar = referencia(num);
            // Fin accion semantica 57.
        } else if (preAnalisis.equals("num.num")) {
            numNum = cmp.be.preAnalisis;
            emparejar("num.num");

            // Accion semantica 58.
            _FACTOR.lugar = referencia(numNum);
            // Fin accion semantica 58.
        } else if (preAnalisis.equals("(")) {
            emparejar("(");
            EXPRESION(_EXPRESION);
            emparejar(")");

            // Accion semantica 59.
            _FACTOR.lugar = _EXPRESION.lugar;
            // Fin accion semantica 59.
        } else {
            error("Factor invalido");
        }
    }

    // ---------------- Procedure 22 ---------------
    // Decide si un id usado como factor es variable o llamada con retorno.
    private void FACTOR_2(Atributos _FACTOR_2) {
        Atributos _LISTA_EXPRESIONES = new Atributos();

        if (preAnalisis.equals("(")) {
            // FACTOR' -> ( LISTA_EXPRESIONES )
            emparejar("(");
            LISTA_EXPRESIONES(_LISTA_EXPRESIONES);
            emparejar(")");

            // Accion semantica 60.
            _FACTOR_2.lugar = tempnuevo();
            emite("call", etiqfun(_FACTOR_2.h), "" + _LISTA_EXPRESIONES.argc, _FACTOR_2.lugar);
            // Fin accion semantica 60.
        } else {
            // FACTOR' -> empty

            // Accion semantica 61.
            _FACTOR_2.lugar = _FACTOR_2.h;
            // Fin accion semantica 61.
        }
    }
}
