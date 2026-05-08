/*:-----------------------------------------------------------------------------
 *:                       INSTITUTO TECNOLOGICO DE LA LAGUNA
 *:                     INGENIERIA EN SISTEMAS COMPUTACIONALES
 *:                         LENGUAJES Y AUTOMATAS II
 *:
 *:                  SEMESTRE: ___________    HORA: ___________ HRS
 *:
 *:
 *:         Clase con la funcionalidad del Analizador Sintactico
 *:
 *:
 *: Archivo       : SintacticoSemantico.java
 *: Autor         : Fernando Gil  ( Estructura general de la clase  )
 *:                 Grupo de Lenguajes y Automatas II ( Procedures  )
 *: Fecha         : 03/SEP/2014
 *: Compilador    : Java JDK 7
 *: Descripción   : Esta clase implementa un parser descendente del tipo
 *:                 Predictivo Recursivo. Se forma por un metodo por cada simbolo
 *:                 No-Terminal de la gramatica mas el metodo emparejar ().
 *:                 El analisis empieza invocando al metodo del simbolo inicial.
 *: Ult.Modif.    :
 *:  Fecha      Modificó            Modificacion
 *:=============================================================================
 *: 22/Feb/2015 FGil                -Se mejoro errorEmparejar () para mostrar el
 *:                                 numero de linea en el codigo fuente donde
 *:                                 ocurrio el error.
 *: 08/Sep/2015 FGil                -Se dejo lista para iniciar un nuevo analizador
 *:                                 sintactico.
 *: 20/FEB/2023 F.Gil, Oswi         -Se implementaron los procedures del parser
 *:                                  predictivo recursivo de leng BasicTec.
 *: 07/MAY/2026 Sebas               -Se agrego el analisis semantico siguiendo
 *:                                  el metodo clasico del Libro del Dragon:
 *:                                  atributos, acciones semanticas embebidas y
 *:                                  preregistro de firmas de funciones.
 *:-----------------------------------------------------------------------------
 */
package compilador;

import general.Linea_BE;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class SintacticoSemantico {

    private static final String VACIO = "VACIO";
    private static final String ERROR_TIPO = "ERROR_TIPO";
    private static final String TIPO_INT = "int";
    private static final String TIPO_FLOAT = "float";
    private static final String TIPO_STRING = "string";
    private static final String TIPO_VOID = "void";
    private static final String TIPO_BOOL = "bool";
    private static final String FIRMA_PREFIJO = "func(";
    private static final String AMBITO_GLOBAL = "global";
    private static final String AMBITO_FUNCION = "fun";
    private static final String AMBITO_PARAM_PREFIJO = "param:";
    private static final String AMBITO_LOCAL_PREFIJO = "local:";

    private Compilador cmp;
    private boolean analizarSemantica = false;
    private String preAnalisis;
    private String funcionActual = null;

    // Firmas preregistradas para soportar llamadas a funciones declaradas despues.
    private final HashMap<String, ArrayList<String>> firmasFunciones =
            new HashMap<String, ArrayList<String>>();
    private final HashMap<String, String> retornosFunciones =
            new HashMap<String, String>();
    private final HashSet<String> funcionesDefinidas = new HashSet<String>();

    //--------------------------------------------------------------------------
    // Constructor de la clase, recibe la referencia de la clase principal del
    // compilador.
    //

    public SintacticoSemantico(Compilador c) {
        cmp = c;
    }

    //--------------------------------------------------------------------------
    //--------------------------------------------------------------------------
    // Metodo que inicia la ejecucion del analisis sintactico predictivo.
    // analizarSemantica : true = realiza el analisis semantico a la par del sintactico
    //                     false= realiza solo el analisis sintactico sin comprobacion semantica

    public void analizar(boolean analizarSemantica) {
        this.analizarSemantica = analizarSemantica;
        preAnalisis = cmp.be.preAnalisis.complex;

        if (analizarSemantica) {
            prepararAnalisisSemantico();
        }

        // * * *   INVOCAR AQUI EL PROCEDURE DEL SIMBOLO INICIAL   * * *
        Atributos programa = new Atributos();
        PROGRAMA(programa);

        if (!preAnalisis.equals("$")) {
            errorEmparejar("$", cmp.be.preAnalisis.lexema, cmp.be.preAnalisis.numLinea);
        }
    }

    //--------------------------------------------------------------------------

    private void emparejar(String t) {
        if (cmp.be.preAnalisis.complex.equals(t)) {
            cmp.be.siguiente();
            preAnalisis = cmp.be.preAnalisis.complex;
        } else {
            errorEmparejar(t, cmp.be.preAnalisis.lexema, cmp.be.preAnalisis.numLinea);
        }
    }

    //--------------------------------------------------------------------------
    // Metodo para devolver un error al emparejar
    //--------------------------------------------------------------------------

    private void errorEmparejar(String _token, String _lexema, int numLinea) {
        String msjError = "";

        if (_token.equals("id")) {
            msjError += "Se esperaba un identificador";
        } else if (_token.equals("num")) {
            msjError += "Se esperaba una constante entera";
        } else if (_token.equals("num.num")) {
            msjError += "Se esperaba una constante real";
        } else if (_token.equals("literal")) {
            msjError += "Se esperaba una literal";
        } else if (_token.equals("oparit")) {
            msjError += "Se esperaba un operador aritmetico";
        } else if (_token.equals("oprel")) {
            msjError += "Se esperaba un operador relacional";
        } else if (_token.equals("opasig")) {
            msjError += "Se esperaba operador de asignacion";
        } else if (_token.equals("$")) {
            msjError += "Se esperaba fin de archivo";
        } else {
            msjError += "Se esperaba " + _token;
        }
        msjError += " se encontró " + (_lexema.equals("$") ? "fin de archivo" : _lexema) +
                    ". Linea " + numLinea;        // FGil: Se agregó el numero de linea

        cmp.me.error(Compilador.ERR_SINTACTICO, msjError);

        // Avanzar token para evitar ciclo infinito
        cmp.be.siguiente();
        preAnalisis = cmp.be.preAnalisis.complex;
    }

    // Fin de ErrorEmparejar
    //--------------------------------------------------------------------------
    // Metodo para mostrar un error sintactico

    private void error(String _descripError) {
        System.out.println("Token actual: " + preAnalisis);  //Agregado por Sergio
        cmp.me.error(cmp.ERR_SINTACTICO, _descripError);

        // Avanzar token para evitar ciclo infinito (Agregado por Sergio)
        cmp.be.siguiente();
        preAnalisis = cmp.be.preAnalisis.complex;
    }

    // Fin de error
    //--------------------------------------------------------------------------
    // Rutinas semanticas auxiliares
    //--------------------------------------------------------------------------

    private void prepararAnalisisSemantico() {
        funcionActual = null;
        firmasFunciones.clear();
        retornosFunciones.clear();
        funcionesDefinidas.clear();
        limpiarAnotacionesSemanticas();
        tipificarConstantes();
        preregistrarFirmasFunciones();
    }

    private void limpiarAnotacionesSemanticas() {
        for (int i = 1; i < cmp.ts.getTamaño(); i++) {
            cmp.ts.anadeTipo(i, "");
            cmp.ts.anadeAmbito(i, "");
        }
    }

    private void tipificarConstantes() {
        for (int i = 1; i < cmp.ts.getTamaño(); i++) {
            String complex = cmp.ts.obt_elemento(i).getComplex();

            if ("num".equals(complex)) {
                cmp.ts.anadeTipo(i, TIPO_INT);
            } else if ("num.num".equals(complex)) {
                cmp.ts.anadeTipo(i, TIPO_FLOAT);
            } else if ("literal".equals(complex)) {
                cmp.ts.anadeTipo(i, TIPO_STRING);
            }
        }
    }

    private void preregistrarFirmasFunciones() {
        int total = cmp.be.getTamaño();

        for (int i = 0; i < total; i++) {
            Linea_BE actual = cmp.be.obtElemento(i);

            if (!"def".equals(actual.getComplex())) {
                continue;
            }

            int j = i + 1;
            if (j >= total || !"id".equals(cmp.be.obtElemento(j).getComplex())) {
                continue;
            }

            Linea_BE idFuncion = cmp.be.obtElemento(j);
            String nombreFuncion = idFuncion.getLexema();
            j++;

            if (j >= total || !"(".equals(cmp.be.obtElemento(j).getComplex())) {
                continue;
            }
            j++;

            ArrayList<String> tiposParametros = new ArrayList<String>();

            if (j < total && esTokenTipoDato(cmp.be.obtElemento(j).getComplex())) {
                while (j < total && esTokenTipoDato(cmp.be.obtElemento(j).getComplex())) {
                    tiposParametros.add(cmp.be.obtElemento(j).getComplex());
                    j++;

                    if (j >= total || !"id".equals(cmp.be.obtElemento(j).getComplex())) {
                        break;
                    }
                    j++;

                    if (j < total && ",".equals(cmp.be.obtElemento(j).getComplex())) {
                        j++;
                    } else {
                        break;
                    }
                }
            }

            if (j >= total || !")".equals(cmp.be.obtElemento(j).getComplex())) {
                continue;
            }
            j++;

            if (j >= total || !":".equals(cmp.be.obtElemento(j).getComplex())) {
                continue;
            }
            j++;

            if (j >= total) {
                continue;
            }

            String tipoRetorno = cmp.be.obtElemento(j).getComplex();
            if (!TIPO_VOID.equals(tipoRetorno) && !esTokenTipoDato(tipoRetorno)) {
                continue;
            }

            if (!firmasFunciones.containsKey(nombreFuncion)) {
                firmasFunciones.put(nombreFuncion, copiaLista(tiposParametros));
                retornosFunciones.put(nombreFuncion, tipoRetorno);
            }
        }
    }

    private void emparejarFinBloque() {
        // El lexer actual entrega "::" como dos tokens ":" consecutivos.
        emparejar(":");
        emparejar(":");
    }

    private boolean esTokenTipoDato(String token) {
        return TIPO_INT.equals(token) || TIPO_FLOAT.equals(token) || TIPO_STRING.equals(token);
    }

    private boolean esInicioPrograma() {
        return "def".equals(preAnalisis) || esInicioProposicion();
    }

    private boolean esInicioTipoDato() {
        return TIPO_INT.equals(preAnalisis) || TIPO_FLOAT.equals(preAnalisis)
                || TIPO_STRING.equals(preAnalisis);
    }

    private boolean esInicioProposicion() {
        return esInicioTipoDato() || "id".equals(preAnalisis) || "if".equals(preAnalisis)
                || "while".equals(preAnalisis) || "print".equals(preAnalisis);
    }

    private boolean esInicioExpresion() {
        return "id".equals(preAnalisis) || "num".equals(preAnalisis)
                || "num.num".equals(preAnalisis) || "(".equals(preAnalisis)
                || "literal".equals(preAnalisis);
    }

    private String firmaComoCadena(ArrayList<String> tiposParametros, String tipoRetorno) {
        StringBuilder firma = new StringBuilder();
        firma.append(FIRMA_PREFIJO);

        for (int i = 0; i < tiposParametros.size(); i++) {
            if (i > 0) {
                firma.append(",");
            }
            firma.append(tiposParametros.get(i));
        }

        firma.append(")->").append(tipoRetorno);
        return firma.toString();
    }

    private ArrayList<String> copiaLista(ArrayList<String> origen) {
        return new ArrayList<String>(origen);
    }

    private ArrayList<String> concatLista(String primero, ArrayList<String> resto) {
        ArrayList<String> lista = new ArrayList<String>();
        lista.add(primero);
        lista.addAll(resto);
        return lista;
    }

    private boolean esFirmaFuncion(String tipo) {
        return tipo != null && tipo.startsWith(FIRMA_PREFIJO);
    }

    private String tipoDeEntrada(int entrada) {
        if (entrada <= 0) {
            return "";
        }
        return cmp.ts.buscaTipo(entrada);
    }

    private String ambitoDeEntrada(int entrada) {
        if (entrada <= 0) {
            return "";
        }
        return cmp.ts.buscaAmbito(entrada);
    }

    private int buscarEntrada(String lexema) {
        return cmp.ts.buscar(lexema);
    }

    private boolean tipoAsignado(int entrada) {
        return entrada > 0 && !"".equals(tipoDeEntrada(entrada));
    }

    private boolean existeIdentificador(Linea_BE id) {
        if (id == null) {
            return false;
        }

        if (tipoAsignado(id.getEntrada())) {
            return true;
        }

        return firmasFunciones.containsKey(id.getLexema());
    }

    private boolean esFuncion(Linea_BE id) {
        if (id == null) {
            return false;
        }

        String tipoActual = tipoDeEntrada(id.getEntrada());
        if (!"".equals(tipoActual)) {
            return esFirmaFuncion(tipoActual);
        }

        return firmasFunciones.containsKey(id.getLexema());
    }

    private boolean esFuncion(String lexema) {
        int entrada = buscarEntrada(lexema);
        if (entrada > 0 && tipoAsignado(entrada)) {
            return esFirmaFuncion(tipoDeEntrada(entrada));
        }
        return firmasFunciones.containsKey(lexema);
    }

    private boolean esVisibleEnContexto(int entrada) {
        String ambito = ambitoDeEntrada(entrada);

        if (AMBITO_GLOBAL.equals(ambito)) {
            return true;
        }

        if (funcionActual == null) {
            return false;
        }

        return (AMBITO_PARAM_PREFIJO + funcionActual).equals(ambito)
                || (AMBITO_LOCAL_PREFIJO + funcionActual).equals(ambito);
    }

    private boolean esNumerico(String tipo) {
        return TIPO_INT.equals(tipo) || TIPO_FLOAT.equals(tipo);
    }

    private boolean compatibleAsign(String destino, String origen) {
        if (TIPO_FLOAT.equals(destino) && TIPO_INT.equals(origen)) {
            return true;
        }
        return destino.equals(origen);
    }

    private boolean compatibleRet(String esperado, String obtenido) {
        if (TIPO_VOID.equals(esperado) || TIPO_VOID.equals(obtenido)) {
            return TIPO_VOID.equals(esperado) && TIPO_VOID.equals(obtenido);
        }
        return compatibleAsign(esperado, obtenido);
    }

    private boolean compatibleRel(String t1, String t2) {
        if (esNumerico(t1) && esNumerico(t2)) {
            return true;
        }
        return TIPO_STRING.equals(t1) && TIPO_STRING.equals(t2);
    }

    private String promover(String t1, String t2) {
        if (ERROR_TIPO.equals(t1) || ERROR_TIPO.equals(t2)) {
            return ERROR_TIPO;
        }

        if (esNumerico(t1) && esNumerico(t2)) {
            if (TIPO_FLOAT.equals(t1) || TIPO_FLOAT.equals(t2)) {
                return TIPO_FLOAT;
            }
            return TIPO_INT;
        }

        return ERROR_TIPO;
    }

    private boolean compatibleLista(ArrayList<String> formales, ArrayList<String> actuales) {
        if (formales.size() != actuales.size()) {
            return false;
        }

        for (int i = 0; i < formales.size(); i++) {
            if (!compatibleAsign(formales.get(i), actuales.get(i))) {
                return false;
            }
        }

        return true;
    }

    private boolean verificarListaIdsUnica(ArrayList<String> listaIds) {
        HashSet<String> vistos = new HashSet<String>();

        for (String id : listaIds) {
            if (!vistos.add(id)) {
                return false;
            }
        }

        return true;
    }

    private boolean registrarVariable(Linea_BE id, String tipo) {
        if (id == null || id.getEntrada() <= 0) {
            return false;
        }

        if (tipoAsignado(id.getEntrada())) {
            return false;
        }

        cmp.ts.anadeTipo(id.getEntrada(), tipo);
        cmp.ts.anadeAmbito(id.getEntrada(),
                funcionActual == null ? AMBITO_GLOBAL : AMBITO_LOCAL_PREFIJO + funcionActual);
        return true;
    }

    private boolean registrarFuncion(Linea_BE id, ArrayList<String> tiposParametros, String tipoRetorno) {
        if (id == null || id.getEntrada() <= 0) {
            return false;
        }

        if (tipoAsignado(id.getEntrada())) {
            return false;
        }

        cmp.ts.anadeTipo(id.getEntrada(), firmaComoCadena(tiposParametros, tipoRetorno));
        cmp.ts.anadeAmbito(id.getEntrada(), AMBITO_FUNCION);
        return true;
    }

    private boolean registrarParametros(ArrayList<String> ids, ArrayList<String> tipos, String nombreFuncion) {
        boolean ok = true;
        HashSet<String> vistos = new HashSet<String>();

        for (int i = 0; i < ids.size(); i++) {
            String lexema = ids.get(i);
            String tipo = tipos.get(i);
            int entrada = buscarEntrada(lexema);

            if (!vistos.add(lexema)) {
                ok = false;
                continue;
            }

            if (lexema.equals(nombreFuncion)) {
                errorSemantico("El parametro '" + lexema + "' no puede tener el mismo nombre que la funcion '" +
                        nombreFuncion + "'");
                ok = false;
                continue;
            }

            if (entrada <= 0) {
                ok = false;
                continue;
            }

            if (tipoAsignado(entrada)) {
                errorSemantico("El identificador '" + lexema + "' ya fue declarado previamente");
                ok = false;
                continue;
            }

            cmp.ts.anadeTipo(entrada, tipo);
            cmp.ts.anadeAmbito(entrada, AMBITO_PARAM_PREFIJO + nombreFuncion);
        }

        return ok;
    }

    private boolean firmaValida(String nombreFuncion, ArrayList<String> tiposParametros, String tipoRetorno) {
        ArrayList<String> formales = firmasFunciones.get(nombreFuncion);
        String retorno = retornosFunciones.get(nombreFuncion);

        if (formales == null || retorno == null) {
            return false;
        }

        if (!retorno.equals(tipoRetorno)) {
            return false;
        }

        if (formales.size() != tiposParametros.size()) {
            return false;
        }

        for (int i = 0; i < formales.size(); i++) {
            if (!formales.get(i).equals(tiposParametros.get(i))) {
                return false;
            }
        }

        return true;
    }

    private ArrayList<String> tiposParametros(String nombreFuncion) {
        ArrayList<String> tipos = firmasFunciones.get(nombreFuncion);
        return tipos == null ? new ArrayList<String>() : copiaLista(tipos);
    }

    private String tipoRetorno(String nombreFuncion) {
        String tipo = retornosFunciones.get(nombreFuncion);
        return tipo == null ? ERROR_TIPO : tipo;
    }

    private boolean esTipoImprimible(String tipo) {
        return TIPO_INT.equals(tipo) || TIPO_FLOAT.equals(tipo) || TIPO_STRING.equals(tipo);
    }

    private String ubicacion(Linea_BE token) {
        if (token == null || token.getNumLinea() <= 0 || token.getNumLinea() >= 999) {
            return "";
        }
        return ". Linea " + token.getNumLinea();
    }

    private void errorSemantico(String mensaje) {
        cmp.me.error(Compilador.ERR_SEMANTICO, mensaje);
    }

    private String tipoVisibleDeVariable(Linea_BE id) {
        if (!tipoAsignado(id.getEntrada())) {
            errorSemantico("Identificador '" + id.getLexema() + "' no declarado" + ubicacion(id));
            return ERROR_TIPO;
        }

        if (!esVisibleEnContexto(id.getEntrada())) {
            errorSemantico("Identificador '" + id.getLexema() + "' no visible en el ambito actual" + ubicacion(id));
            return ERROR_TIPO;
        }

        return tipoDeEntrada(id.getEntrada());
    }

    //--------------------------------------------------------------------------
    //  *  *   *   *    PEGAR AQUI EL CODIGO DE LOS PROCEDURES  *  *  *  *
    //--------------------------------------------------------------------------

    // =============================================
    // ================ Sergio =====================
    // =============================================

    // ---------------- Procedure 1 ----------------
    private void PROGRAMA(Atributos _PROGRAMA) {
        Atributos _INSTRUCCION = new Atributos();
        Atributos _PROGRAMA1 = new Atributos();

        if (esInicioPrograma()) {
            // PROGRAMA -> INSTRUCCION PROGRAMA
            INSTRUCCION(_INSTRUCCION);
            PROGRAMA(_PROGRAMA1);

            if (analizarSemantica) {
                // Accion semantica 1.
                _PROGRAMA.tipo = VACIO.equals(_INSTRUCCION.tipo) && VACIO.equals(_PROGRAMA1.tipo)
                        ? VACIO : ERROR_TIPO;
                // Fin accion semantica 1.
            }
        } else {
            //PROGRAMA -> empty
            if (analizarSemantica) {
                // Accion semantica 2.
                _PROGRAMA.tipo = VACIO;
                // Fin accion semantica 2.
            }
        }
    }

    // ---------------- Procedure 2 ----------------
    private void INSTRUCCION(Atributos _INSTRUCCION) {
        Atributos _FUNCION = new Atributos();
        Atributos _PROPOSICION = new Atributos();

        if (preAnalisis.equals("def")) {
            //INSTRUCCION -> FUNCION
            FUNCION(_FUNCION);

            if (analizarSemantica) {
                // Accion semantica 3.
                _INSTRUCCION.tipo = _FUNCION.tipo;
                // Fin accion semantica 3.
            }
        } else if (esInicioProposicion()) {
            //INSTRUCCION ->  PROPOSICION
            PROPOSICION(_PROPOSICION);

            if (analizarSemantica) {
                // Accion semantica 4.
                _INSTRUCCION.tipo = _PROPOSICION.tipo;
                // Fin accion semantica 4.
            }
        } else {
            error("Se esperaba una instruccion o funcion");
        }
    }

    // ---------------- Procedure 3 ----------------
    private void FUNCION(Atributos _FUNCION) {
        Atributos _ARGUMENTOS = new Atributos();
        Atributos _TIPO_RETORNO = new Atributos();
        Atributos _PROPOSICIONES_OPTATIVAS = new Atributos();
        Atributos _RESULTADO = new Atributos();
        Linea_BE id = new Linea_BE();
        String funcionAnterior = funcionActual;

        if (preAnalisis.equals("def")) {
            // FUNCION -> def id ( ARGUMENTOS ) : TIPO_RETORNO PROPOSICIONES_OPTATIVAS return RESULTADO ::
            emparejar("def");
            id = cmp.be.preAnalisis;
            emparejar("id");
            emparejar("(");
            ARGUMENTOS(_ARGUMENTOS);
            emparejar(")");
            emparejar(":");
            TIPO_RETORNO(_TIPO_RETORNO);

            if (analizarSemantica) {
                boolean ok = true;

                // Accion semantica 5.
                String nombreFuncion = id.getLexema();
                // Fin accion semantica 5.

                // Accion semantica 6.
                if (funcionesDefinidas.contains(nombreFuncion)) {
                    errorSemantico("La funcion '" + nombreFuncion + "' ya fue declarada previamente" + ubicacion(id));
                    ok = false;
                } else {
                    funcionesDefinidas.add(nombreFuncion);
                }

                if (!firmaValida(nombreFuncion, _ARGUMENTOS.listaTipos, _TIPO_RETORNO.tipo)) {
                    errorSemantico("La firma de la funcion '" + nombreFuncion +
                            "' no coincide con su preregistro" + ubicacion(id));
                    ok = false;
                }

                if (!registrarFuncion(id, _ARGUMENTOS.listaTipos, _TIPO_RETORNO.tipo)) {
                    String tipoActual = tipoDeEntrada(id.getEntrada());
                    if (!"".equals(tipoActual) && !esFirmaFuncion(tipoActual)) {
                        errorSemantico("El identificador '" + nombreFuncion +
                                "' ya fue declarado y no puede redefinirse como funcion" + ubicacion(id));
                    }
                    ok = false;
                }

                if (!verificarListaIdsUnica(_ARGUMENTOS.listaIds)) {
                    errorSemantico("La lista de parametros de la funcion '" + nombreFuncion +
                            "' contiene identificadores repetidos" + ubicacion(id));
                    ok = false;
                }

                if (!registrarParametros(_ARGUMENTOS.listaIds, _ARGUMENTOS.listaTipos, nombreFuncion)) {
                    ok = false;
                }

                _FUNCION.tmp = ok ? VACIO : ERROR_TIPO;
                funcionActual = nombreFuncion;
                // Fin accion semantica 6.
            }

            PROPOSICIONES_OPTATIVAS(_PROPOSICIONES_OPTATIVAS);
            emparejar("return");
            RESULTADO(_RESULTADO);

            if (analizarSemantica) {
                // Accion semantica 7.
                if (compatibleRet(_TIPO_RETORNO.tipo, _RESULTADO.tipo)) {
                    _FUNCION.ret = VACIO;
                } else {
                    _FUNCION.ret = ERROR_TIPO;
                    errorSemantico("Retorno incompatible en la funcion '" + id.getLexema() +
                            "': se esperaba " + _TIPO_RETORNO.tipo + " y se obtuvo " +
                            _RESULTADO.tipo + ubicacion(id));
                }
                // Fin accion semantica 7.
            }

            emparejarFinBloque();

            if (analizarSemantica) {
                // Accion semantica 8.
                _FUNCION.tipo = VACIO.equals(_FUNCION.tmp)
                        && VACIO.equals(_PROPOSICIONES_OPTATIVAS.tipo)
                        && VACIO.equals(_FUNCION.ret)
                        ? VACIO : ERROR_TIPO;
                funcionActual = funcionAnterior;
                // Fin accion semantica 8.
            }
        } else {
            error("Se esperaba la definicion de una funcion");
        }
    }

    // ---------------- Procedure 4 ----------------
    private void ARGUMENTOS(Atributos _ARGUMENTOS) {
        Atributos _TIPO_DATO = new Atributos();
        Atributos _ARGUMENTOS_2 = new Atributos();
        Linea_BE id = new Linea_BE();

        if (esInicioTipoDato()) {
            // ARGUMENTOS -> TIPO_DATO  id  ARGUMENTOS'
            TIPO_DATO(_TIPO_DATO);
            id = cmp.be.preAnalisis;
            emparejar("id");
            ARGUMENTOS_2(_ARGUMENTOS_2);

            if (analizarSemantica) {
                // Accion semantica 9.
                _ARGUMENTOS.listaIds = concatLista(id.getLexema(), _ARGUMENTOS_2.listaIds);
                _ARGUMENTOS.listaTipos = concatLista(_TIPO_DATO.tipo, _ARGUMENTOS_2.listaTipos);
                _ARGUMENTOS.tipo = _ARGUMENTOS_2.tipo;
                // Fin accion semantica 9.
            }
        } else {
            //ARGUMENTOS -> empty
            if (analizarSemantica) {
                // Accion semantica 10.
                _ARGUMENTOS.listaIds = new ArrayList<String>();
                _ARGUMENTOS.listaTipos = new ArrayList<String>();
                _ARGUMENTOS.tipo = VACIO;
                // Fin accion semantica 10.
            }
        }
    }

    // ---------------- Procedure 5 ----------------
    private void ARGUMENTOS_2(Atributos _ARGUMENTOS_2) {
        Atributos _TIPO_DATO = new Atributos();
        Atributos _ARGUMENTOS_21 = new Atributos();
        Linea_BE id = new Linea_BE();

        if (preAnalisis.equals(",")) {
            // ARGUMENTOS' -> ,  TIPO_DATO  id  ARGUMENTOS'
            emparejar(",");
            TIPO_DATO(_TIPO_DATO);
            id = cmp.be.preAnalisis;
            emparejar("id");
            ARGUMENTOS_2(_ARGUMENTOS_21);

            if (analizarSemantica) {
                // Accion semantica 11.
                _ARGUMENTOS_2.listaIds = concatLista(id.getLexema(), _ARGUMENTOS_21.listaIds);
                _ARGUMENTOS_2.listaTipos = concatLista(_TIPO_DATO.tipo, _ARGUMENTOS_21.listaTipos);
                _ARGUMENTOS_2.tipo = _ARGUMENTOS_21.tipo;
                // Fin accion semantica 11.
            }
        } else {
            //ARGUMENTOS_2 -> empty
            if (analizarSemantica) {
                // Accion semantica 12.
                _ARGUMENTOS_2.listaIds = new ArrayList<String>();
                _ARGUMENTOS_2.listaTipos = new ArrayList<String>();
                _ARGUMENTOS_2.tipo = VACIO;
                // Fin accion semantica 12.
            }
        }
    }


    // =============================================
    // ================ Sebas ======================
    // =============================================

    // ---------------- Procedure 6 ----------------
    private void DECLARACION_VARS(Atributos _DECLARACION_VARS) {
        Atributos _TIPO_DATO = new Atributos();
        Atributos _DECLARACION_VARS_2 = new Atributos();
        Linea_BE id = new Linea_BE();

        if (esInicioTipoDato()) {
            // DECLARACION_VARS -> TIPO_DATO id DECLARACION_VARS'
            TIPO_DATO(_TIPO_DATO);
            id = cmp.be.preAnalisis;
            emparejar("id");

            if (analizarSemantica) {
                // Accion semantica 13.
                if (registrarVariable(id, _TIPO_DATO.tipo)) {
                    _DECLARACION_VARS.tmp = VACIO;
                } else {
                    _DECLARACION_VARS.tmp = ERROR_TIPO;
                    errorSemantico("El identificador '" + id.getLexema() +
                            "' ya fue declarado previamente" + ubicacion(id));
                }

                _DECLARACION_VARS_2.h = _TIPO_DATO.tipo;
                // Fin accion semantica 13.
            }

            DECLARACION_VARS_2(_DECLARACION_VARS_2);

            if (analizarSemantica) {
                // Accion semantica 14.
                _DECLARACION_VARS.tipo = VACIO.equals(_DECLARACION_VARS.tmp)
                        && VACIO.equals(_DECLARACION_VARS_2.tipo)
                        ? VACIO : ERROR_TIPO;
                // Fin accion semantica 14.
            }
        } else {
            error("Declaracion de variables invalida");
        }
    }

    // ---------------- Procedure 7 ----------------
    private void DECLARACION_VARS_2(Atributos _DECLARACION_VARS_2) {
        Atributos _DECLARACION_VARS_21 = new Atributos();
        Linea_BE id = new Linea_BE();

        if (preAnalisis.equals(",")) {
            // DECLARACION_VARS' -> , id DECLARACION_VARS'
            emparejar(",");
            id = cmp.be.preAnalisis;
            emparejar("id");

            if (analizarSemantica) {
                // Accion semantica 15.
                if (registrarVariable(id, _DECLARACION_VARS_2.h)) {
                    _DECLARACION_VARS_2.tmp = VACIO;
                } else {
                    _DECLARACION_VARS_2.tmp = ERROR_TIPO;
                    errorSemantico("El identificador '" + id.getLexema() +
                            "' ya fue declarado previamente" + ubicacion(id));
                }

                _DECLARACION_VARS_21.h = _DECLARACION_VARS_2.h;
                // Fin accion semantica 15.
            }

            DECLARACION_VARS_2(_DECLARACION_VARS_21);

            if (analizarSemantica) {
                // Accion semantica 16.
                _DECLARACION_VARS_2.tipo = VACIO.equals(_DECLARACION_VARS_2.tmp)
                        && VACIO.equals(_DECLARACION_VARS_21.tipo)
                        ? VACIO : ERROR_TIPO;
                // Fin accion semantica 16.
            }
        } else {
            // DECLARACION_VARS_2 -> empty
            if (analizarSemantica) {
                // Accion semantica 17.
                _DECLARACION_VARS_2.tipo = VACIO;
                // Fin accion semantica 17.
            }
        }
    }

    // ---------------- Procedure 8 ----------------
    private void TIPO_DATO(Atributos _TIPO_DATO) {
        if (preAnalisis.equals("int")) {
            emparejar("int");
            if (analizarSemantica) {
                // Accion semantica 20.
                _TIPO_DATO.tipo = TIPO_INT;
                // Fin accion semantica 20.
            }
        } else if (preAnalisis.equals("float")) {
            emparejar("float");
            if (analizarSemantica) {
                // Accion semantica 21.
                _TIPO_DATO.tipo = TIPO_FLOAT;
                // Fin accion semantica 21.
            }
        } else if (preAnalisis.equals("string")) {
            emparejar("string");
            if (analizarSemantica) {
                // Accion semantica 22.
                _TIPO_DATO.tipo = TIPO_STRING;
                // Fin accion semantica 22.
            }
        } else {
            error("Tipo de dato invalido");
        }
    }

    // ---------------- Procedure 9 ----------------
    private void TIPO_RETORNO(Atributos _TIPO_RETORNO) {
        Atributos _TIPO_DATO = new Atributos();

        if (preAnalisis.equals("void")) {
            emparejar("void");
            if (analizarSemantica) {
                // Accion semantica 18.
                _TIPO_RETORNO.tipo = TIPO_VOID;
                // Fin accion semantica 18.
            }
        } else if (esInicioTipoDato()) {
            // TIPO_RETORNO -> TIPO_DATO
            TIPO_DATO(_TIPO_DATO);
            if (analizarSemantica) {
                // Accion semantica 19.
                _TIPO_RETORNO.tipo = _TIPO_DATO.tipo;
                // Fin accion semantica 19.
            }
        } else {
            error("Se esperaba un tipo de retorno");
        }
    }

    // ---------------- Procedure 10 ---------------
    private void RESULTADO(Atributos _RESULTADO) {
        Atributos _EXPRESION = new Atributos();

        if (esInicioExpresion()) {
            //RESULTADO -> EXPRESION
            EXPRESION(_EXPRESION);

            if (analizarSemantica) {
                // Accion semantica 23.
                _RESULTADO.tipo = _EXPRESION.tipo;
                // Fin accion semantica 23.
            }
        } else if (preAnalisis.equals("void")) {
            //RESULTADO -> void
            emparejar("void");

            if (analizarSemantica) {
                // Accion semantica 24.
                _RESULTADO.tipo = TIPO_VOID;
                // Fin accion semantica 24.
            }
        } else {
            error("Se esperaba un resultado");
        }
    }

    // ---------------- Procedure 11 ---------------
    private void PROPOSICIONES_OPTATIVAS(Atributos _PROPOSICIONES_OPTATIVAS) {
        Atributos _PROPOSICION = new Atributos();
        Atributos _PROPOSICIONES_OPTATIVAS1 = new Atributos();

        if (esInicioProposicion()) {
            //PROPOSICIONES_OPTATIVAS -> PROPOSICION PROPOSICIONES_OPTATIVAS
            PROPOSICION(_PROPOSICION);
            PROPOSICIONES_OPTATIVAS(_PROPOSICIONES_OPTATIVAS1);

            if (analizarSemantica) {
                // Accion semantica 25.
                _PROPOSICIONES_OPTATIVAS.tipo = VACIO.equals(_PROPOSICION.tipo)
                        && VACIO.equals(_PROPOSICIONES_OPTATIVAS1.tipo)
                        ? VACIO : ERROR_TIPO;
                // Fin accion semantica 25.
            }
        } else {
            //PROPOSICIONES_OPTATIVAS -> empty
            if (analizarSemantica) {
                // Accion semantica 26.
                _PROPOSICIONES_OPTATIVAS.tipo = VACIO;
                // Fin accion semantica 26.
            }
        }
    }


    // =============================================
    // ================ Derek ======================
    // =============================================

    // ---------------- Procedure 12 ---------------
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

            if (analizarSemantica) {
                // Accion semantica 27.
                _PROPOSICION.tipo = _DECLARACION_VARS.tipo;
                // Fin accion semantica 27.
            }
        } else if (preAnalisis.equals("id")) {
            // PROPOSICION -> id PROPOSICION'
            id = cmp.be.preAnalisis;
            emparejar("id");

            if (analizarSemantica) {
                // Accion semantica 28.
                _PROPOSICION_2.h = id.getLexema();
                // Fin accion semantica 28.
            }

            PROPOSICION_2(_PROPOSICION_2);

            if (analizarSemantica) {
                // Accion semantica 29.
                _PROPOSICION.tipo = _PROPOSICION_2.tipo;
                // Fin accion semantica 29.
            }
        } else if (preAnalisis.equals("if")) {
            // PROPOSICION -> if CONDICION : PROPOSICIONES_OPTATIVAS else : PROPOSICIONES_OPTATIVAS ::
            emparejar("if");
            CONDICION(_CONDICION);
            emparejar(":");
            PROPOSICIONES_OPTATIVAS(_PROPOSICIONES_OPTATIVAS1);
            emparejar("else");
            emparejar(":");
            PROPOSICIONES_OPTATIVAS(_PROPOSICIONES_OPTATIVAS2);
            emparejarFinBloque();

            if (analizarSemantica) {
                // Accion semantica 30.
                _PROPOSICION.tipo = TIPO_BOOL.equals(_CONDICION.tipo)
                        && VACIO.equals(_PROPOSICIONES_OPTATIVAS1.tipo)
                        && VACIO.equals(_PROPOSICIONES_OPTATIVAS2.tipo)
                        ? VACIO : ERROR_TIPO;
                // Fin accion semantica 30.
            }
        } else if (preAnalisis.equals("while")) {
            // PROPOSICION -> while CONDICION : PROPOSICIONES_OPTATIVAS ::
            emparejar("while");
            CONDICION(_CONDICION);
            emparejar(":");
            PROPOSICIONES_OPTATIVAS(_PROPOSICIONES_OPTATIVAS1);
            emparejarFinBloque();

            if (analizarSemantica) {
                // Accion semantica 31.
                _PROPOSICION.tipo = TIPO_BOOL.equals(_CONDICION.tipo)
                        && VACIO.equals(_PROPOSICIONES_OPTATIVAS1.tipo)
                        ? VACIO : ERROR_TIPO;
                // Fin accion semantica 31.
            }
        } else if (preAnalisis.equals("print")) {
            // PROPOSICION -> print ( EXPRESION )
            emparejar("print");
            emparejar("(");
            EXPRESION(_EXPRESION);
            emparejar(")");

            if (analizarSemantica) {
                // Accion semantica 32.
                if (esTipoImprimible(_EXPRESION.tipo)) {
                    _PROPOSICION.tipo = VACIO;
                } else {
                    _PROPOSICION.tipo = ERROR_TIPO;
                    if (!ERROR_TIPO.equals(_EXPRESION.tipo)) {
                        errorSemantico("La sentencia print solo acepta expresiones de tipo int, float o string");
                    }
                }
                // Fin accion semantica 32.
            }
        } else {
            error("Proposicion invalida");
        }
    }

    // ---------------- Procedure 13 ---------------
    private void PROPOSICION_2(Atributos _PROPOSICION_2) {
        Atributos _EXPRESION = new Atributos();
        Atributos _LISTA_EXPRESIONES = new Atributos();
        Linea_BE idBase = new Linea_BE();
        int entrada;
        String tipoDestino;

        if (preAnalisis.equals("opasig")) {
            // PROPOSICION' -> opasig EXPRESION
            emparejar("opasig");
            EXPRESION(_EXPRESION);

            if (analizarSemantica) {
                // Accion semantica 33.
                entrada = buscarEntrada(_PROPOSICION_2.h);
                idBase = new Linea_BE("id", _PROPOSICION_2.h, entrada, cmp.be.preAnalisis.numLinea);

                if (!existeIdentificador(idBase)) {
                    _PROPOSICION_2.tipo = ERROR_TIPO;
                    errorSemantico("Identificador '" + _PROPOSICION_2.h + "' no declarado");
                } else if (esFuncion(idBase)) {
                    _PROPOSICION_2.tipo = ERROR_TIPO;
                    errorSemantico("No se puede asignar a la funcion '" + _PROPOSICION_2.h + "'");
                } else {
                    tipoDestino = tipoVisibleDeVariable(idBase);

                    if (ERROR_TIPO.equals(tipoDestino) || ERROR_TIPO.equals(_EXPRESION.tipo)) {
                        _PROPOSICION_2.tipo = ERROR_TIPO;
                    } else if (compatibleAsign(tipoDestino, _EXPRESION.tipo)) {
                        _PROPOSICION_2.tipo = VACIO;
                    } else {
                        _PROPOSICION_2.tipo = ERROR_TIPO;
                        errorSemantico("Tipos incompatibles en asignacion a '" + _PROPOSICION_2.h +
                                "': se esperaba " + tipoDestino + " y se obtuvo " + _EXPRESION.tipo);
                    }
                }
                // Fin accion semantica 33.
            }
        } else if (preAnalisis.equals("(")) {
            // PROPOSICION' -> ( LISTA_EXPRESIONES )
            emparejar("(");
            LISTA_EXPRESIONES(_LISTA_EXPRESIONES);
            emparejar(")");

            if (analizarSemantica) {
                // Accion semantica 34.
                entrada = buscarEntrada(_PROPOSICION_2.h);
                idBase = new Linea_BE("id", _PROPOSICION_2.h, entrada, cmp.be.preAnalisis.numLinea);

                if (!existeIdentificador(idBase)) {
                    _PROPOSICION_2.tipo = ERROR_TIPO;
                    errorSemantico("Funcion '" + _PROPOSICION_2.h + "' no declarada");
                } else if (!esFuncion(idBase)) {
                    _PROPOSICION_2.tipo = ERROR_TIPO;
                    errorSemantico("El identificador '" + _PROPOSICION_2.h + "' no corresponde a una funcion");
                } else if (!VACIO.equals(_LISTA_EXPRESIONES.tipo)) {
                    _PROPOSICION_2.tipo = ERROR_TIPO;
                } else if (!compatibleLista(tiposParametros(_PROPOSICION_2.h), _LISTA_EXPRESIONES.listaTipos)) {
                    _PROPOSICION_2.tipo = ERROR_TIPO;
                    errorSemantico("Argumentos incompatibles en la llamada a '" + _PROPOSICION_2.h + "'");
                } else {
                    _PROPOSICION_2.tipo = VACIO;
                }
                // Fin accion semantica 34.
            }
        } else {
            error("Se esperaba asignacion o llamada a funcion");
        }
    }

    // ---------------- Procedure 14 ---------------
    private void LISTA_EXPRESIONES(Atributos _LISTA_EXPRESIONES) {
        Atributos _EXPRESION = new Atributos();
        Atributos _LISTA_EXPRESIONES_2 = new Atributos();

        if (esInicioExpresion()) {
            // LISTA_EXPRESIONES -> EXPRESION LISTA_EXPRESIONES'
            EXPRESION(_EXPRESION);
            LISTA_EXPRESIONES_2(_LISTA_EXPRESIONES_2);

            if (analizarSemantica) {
                // Accion semantica 35.
                _LISTA_EXPRESIONES.listaTipos = concatLista(_EXPRESION.tipo, _LISTA_EXPRESIONES_2.listaTipos);
                _LISTA_EXPRESIONES.tipo = !TIPO_VOID.equals(_EXPRESION.tipo)
                        && !ERROR_TIPO.equals(_EXPRESION.tipo)
                        && VACIO.equals(_LISTA_EXPRESIONES_2.tipo)
                        ? VACIO : ERROR_TIPO;
                // Fin accion semantica 35.
            }
        } else {
            // LISTA_EXPRESIONES -> empty
            if (analizarSemantica) {
                // Accion semantica 36.
                _LISTA_EXPRESIONES.listaTipos = new ArrayList<String>();
                _LISTA_EXPRESIONES.tipo = VACIO;
                // Fin accion semantica 36.
            }
        }
    }

    // ---------------- Procedure 15 ---------------
    private void LISTA_EXPRESIONES_2(Atributos _LISTA_EXPRESIONES_2) {
        Atributos _EXPRESION = new Atributos();
        Atributos _LISTA_EXPRESIONES_21 = new Atributos();

        if (preAnalisis.equals(",")) {
            // LISTA_EXPRESIONES' -> , EXPRESION LISTA_EXPRESIONES'
            emparejar(",");
            EXPRESION(_EXPRESION);
            LISTA_EXPRESIONES_2(_LISTA_EXPRESIONES_21);

            if (analizarSemantica) {
                // Accion semantica 37.
                _LISTA_EXPRESIONES_2.listaTipos = concatLista(_EXPRESION.tipo, _LISTA_EXPRESIONES_21.listaTipos);
                _LISTA_EXPRESIONES_2.tipo = !TIPO_VOID.equals(_EXPRESION.tipo)
                        && !ERROR_TIPO.equals(_EXPRESION.tipo)
                        && VACIO.equals(_LISTA_EXPRESIONES_21.tipo)
                        ? VACIO : ERROR_TIPO;
                // Fin accion semantica 37.
            }
        } else {
            // LISTA_EXPRESIONES_2 -> empty
            if (analizarSemantica) {
                // Accion semantica 38.
                _LISTA_EXPRESIONES_2.listaTipos = new ArrayList<String>();
                _LISTA_EXPRESIONES_2.tipo = VACIO;
                // Fin accion semantica 38.
            }
        }
    }

    // ---------------- Procedure 16 ---------------
    private void CONDICION(Atributos _CONDICION) {
        Atributos _EXPRESION1 = new Atributos();
        Atributos _EXPRESION2 = new Atributos();

        if (esInicioExpresion()) {
            // CONDICION -> EXPRESION oprel EXPRESION
            EXPRESION(_EXPRESION1);
            emparejar("oprel");
            EXPRESION(_EXPRESION2);

            if (analizarSemantica) {
                // Accion semantica 39.
                if (compatibleRel(_EXPRESION1.tipo, _EXPRESION2.tipo)) {
                    _CONDICION.tipo = TIPO_BOOL;
                } else {
                    _CONDICION.tipo = ERROR_TIPO;
                    if (!ERROR_TIPO.equals(_EXPRESION1.tipo) && !ERROR_TIPO.equals(_EXPRESION2.tipo)) {
                        errorSemantico("Comparacion incompatible entre tipos " +
                                _EXPRESION1.tipo + " y " + _EXPRESION2.tipo);
                    }
                }
                // Fin accion semantica 39.
            }
        } else {
            error("Condicion invalida");
        }
    }


    // =============================================
    // ================ Ricardo ====================
    // =============================================

    // ---------------- Procedure 17 ---------------
    private void EXPRESION(Atributos _EXPRESION) {
        Atributos _TERMINO = new Atributos();
        Atributos _EXPRESION_2 = new Atributos();

        if (preAnalisis.equals("id")
                || preAnalisis.equals("num")
                || preAnalisis.equals("num.num")
                || preAnalisis.equals("(")) {

            // EXPRESION -> TERMINO EXPRESION'
            TERMINO(_TERMINO);

            if (analizarSemantica) {
                // Accion semantica 40.
                _EXPRESION_2.h = _TERMINO.tipo;
                // Fin accion semantica 40.
            }

            EXPRESION_2(_EXPRESION_2);

            if (analizarSemantica) {
                // Accion semantica 41.
                _EXPRESION.tipo = _EXPRESION_2.tipo;
                // Fin accion semantica 41.
            }
        } else if (preAnalisis.equals("literal")) {
            // EXPRESION -> literal
            emparejar("literal");

            if (analizarSemantica) {
                // Accion semantica 42.
                _EXPRESION.tipo = TIPO_STRING;
                // Fin accion semantica 42.
            }
        } else {
            error("Expresion invalida");
        }
    }

    // ---------------- Procedure 18 ---------------
    private void EXPRESION_2(Atributos _EXPRESION_2) {
        Atributos _TERMINO = new Atributos();
        Atributos _EXPRESION_21 = new Atributos();
        String tipoPromovido;

        if (preAnalisis.equals("opsuma")) {
            // EXPRESION' -> opsuma TERMINO EXPRESION'
            emparejar("opsuma");
            TERMINO(_TERMINO);

            if (analizarSemantica) {
                // Accion semantica 43.
                tipoPromovido = promover(_EXPRESION_2.h, _TERMINO.tipo);
                _EXPRESION_21.h = tipoPromovido;

                if (ERROR_TIPO.equals(tipoPromovido)
                        && !ERROR_TIPO.equals(_EXPRESION_2.h)
                        && !ERROR_TIPO.equals(_TERMINO.tipo)) {
                    errorSemantico("Operacion aritmetica incompatible entre tipos " +
                            _EXPRESION_2.h + " y " + _TERMINO.tipo);
                }
                // Fin accion semantica 43.
            }

            EXPRESION_2(_EXPRESION_21);

            if (analizarSemantica) {
                // Accion semantica 44.
                _EXPRESION_2.tipo = _EXPRESION_21.tipo;
                // Fin accion semantica 44.
            }
        } else {
            // EXPRESION_2 -> empty
            if (analizarSemantica) {
                // Accion semantica 45.
                _EXPRESION_2.tipo = _EXPRESION_2.h;
                // Fin accion semantica 45.
            }
        }
    }

    // ---------------- Procedure 19 ---------------
    private void TERMINO(Atributos _TERMINO) {
        Atributos _FACTOR = new Atributos();
        Atributos _TERMINO_2 = new Atributos();

        if (preAnalisis.equals("id")
                || preAnalisis.equals("num")
                || preAnalisis.equals("num.num")
                || preAnalisis.equals("(")) {

            // TERMINO -> FACTOR TERMINO'
            FACTOR(_FACTOR);

            if (analizarSemantica) {
                // Accion semantica 46.
                _TERMINO_2.h = _FACTOR.tipo;
                // Fin accion semantica 46.
            }

            TERMINO_2(_TERMINO_2);

            if (analizarSemantica) {
                // Accion semantica 47.
                _TERMINO.tipo = _TERMINO_2.tipo;
                // Fin accion semantica 47.
            }
        } else {
            error("Termino invalido");
        }
    }

    // ---------------- Procedure 20 ---------------
    private void TERMINO_2(Atributos _TERMINO_2) {
        Atributos _FACTOR = new Atributos();
        Atributos _TERMINO_21 = new Atributos();
        String tipoPromovido;

        if (preAnalisis.equals("opmult")) {
            // TERMINO' -> opmult FACTOR TERMINO'
            emparejar("opmult");
            FACTOR(_FACTOR);

            if (analizarSemantica) {
                // Accion semantica 48.
                tipoPromovido = promover(_TERMINO_2.h, _FACTOR.tipo);
                _TERMINO_21.h = tipoPromovido;

                if (ERROR_TIPO.equals(tipoPromovido)
                        && !ERROR_TIPO.equals(_TERMINO_2.h)
                        && !ERROR_TIPO.equals(_FACTOR.tipo)) {
                    errorSemantico("Operacion aritmetica incompatible entre tipos " +
                            _TERMINO_2.h + " y " + _FACTOR.tipo);
                }
                // Fin accion semantica 48.
            }

            TERMINO_2(_TERMINO_21);

            if (analizarSemantica) {
                // Accion semantica 49.
                _TERMINO_2.tipo = _TERMINO_21.tipo;
                // Fin accion semantica 49.
            }
        } else {
            // TERMINO_2 -> empty
            if (analizarSemantica) {
                // Accion semantica 50.
                _TERMINO_2.tipo = _TERMINO_2.h;
                // Fin accion semantica 50.
            }
        }
    }

    // ---------------- Procedure 21 ---------------
    private void FACTOR(Atributos _FACTOR) {
        Atributos _FACTOR_2 = new Atributos();
        Atributos _EXPRESION = new Atributos();
        Linea_BE id = new Linea_BE();

        if (preAnalisis.equals("id")) {
            // FACTOR -> id FACTOR'
            id = cmp.be.preAnalisis;
            emparejar("id");

            if (analizarSemantica) {
                // Accion semantica 51.
                _FACTOR_2.h = id.getLexema();
                // Fin accion semantica 51.
            }

            FACTOR_2(_FACTOR_2);

            if (analizarSemantica) {
                // Accion semantica 52.
                _FACTOR.tipo = _FACTOR_2.tipo;
                // Fin accion semantica 52.
            }
        } else if (preAnalisis.equals("num")) {
            emparejar("num");

            if (analizarSemantica) {
                // Accion semantica 53.
                _FACTOR.tipo = TIPO_INT;
                // Fin accion semantica 53.
            }
        } else if (preAnalisis.equals("num.num")) {
            emparejar("num.num");

            if (analizarSemantica) {
                // Accion semantica 54.
                _FACTOR.tipo = TIPO_FLOAT;
                // Fin accion semantica 54.
            }
        } else if (preAnalisis.equals("(")) {
            emparejar("(");
            EXPRESION(_EXPRESION);
            emparejar(")");

            if (analizarSemantica) {
                // Accion semantica 55.
                _FACTOR.tipo = _EXPRESION.tipo;
                // Fin accion semantica 55.
            }
        } else {
            error("Factor invalido");
        }
    }

    // ---------------- Procedure 22 ---------------
    private void FACTOR_2(Atributos _FACTOR_2) {
        Atributos _LISTA_EXPRESIONES = new Atributos();
        Linea_BE idBase = new Linea_BE();
        int entrada;

        if (preAnalisis.equals("(")) {
            // FACTOR' -> ( LISTA_EXPRESIONES )
            emparejar("(");
            LISTA_EXPRESIONES(_LISTA_EXPRESIONES);
            emparejar(")");

            if (analizarSemantica) {
                // Accion semantica 56.
                entrada = buscarEntrada(_FACTOR_2.h);
                idBase = new Linea_BE("id", _FACTOR_2.h, entrada, cmp.be.preAnalisis.numLinea);

                if (!existeIdentificador(idBase)) {
                    _FACTOR_2.tipo = ERROR_TIPO;
                    errorSemantico("Funcion '" + _FACTOR_2.h + "' no declarada");
                } else if (!esFuncion(idBase)) {
                    _FACTOR_2.tipo = ERROR_TIPO;
                    errorSemantico("El identificador '" + _FACTOR_2.h + "' no corresponde a una funcion");
                } else if (!VACIO.equals(_LISTA_EXPRESIONES.tipo)) {
                    _FACTOR_2.tipo = ERROR_TIPO;
                } else if (!compatibleLista(tiposParametros(_FACTOR_2.h), _LISTA_EXPRESIONES.listaTipos)) {
                    _FACTOR_2.tipo = ERROR_TIPO;
                    errorSemantico("Argumentos incompatibles en la llamada a '" + _FACTOR_2.h + "'");
                } else if (TIPO_VOID.equals(tipoRetorno(_FACTOR_2.h))) {
                    _FACTOR_2.tipo = ERROR_TIPO;
                    errorSemantico("La funcion '" + _FACTOR_2.h +
                            "' de tipo void no puede usarse dentro de una expresion");
                } else {
                    _FACTOR_2.tipo = tipoRetorno(_FACTOR_2.h);
                }
                // Fin accion semantica 56.
            }
        } else {
            // FACTOR_2 -> empty
            if (analizarSemantica) {
                // Accion semantica 57.
                entrada = buscarEntrada(_FACTOR_2.h);
                idBase = new Linea_BE("id", _FACTOR_2.h, entrada, cmp.be.preAnalisis.numLinea);

                if (!existeIdentificador(idBase)) {
                    _FACTOR_2.tipo = ERROR_TIPO;
                    errorSemantico("Identificador '" + _FACTOR_2.h + "' no declarado");
                } else if (esFuncion(idBase)) {
                    _FACTOR_2.tipo = ERROR_TIPO;
                    errorSemantico("La funcion '" + _FACTOR_2.h + "' debe invocarse con parentesis");
                } else {
                    _FACTOR_2.tipo = tipoVisibleDeVariable(idBase);
                }
                // Fin accion semantica 57.
            }
        }
    }

}
//------------------------------------------------------------------------------
//::
