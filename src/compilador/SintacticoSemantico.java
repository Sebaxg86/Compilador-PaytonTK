/*:-----------------------------------------------------------------------------
 *:                       INSTITUTO TECNOLOGICO DE LA LAGUNA
 *:                     INGENIERIA EN SISTEMAS COMPUTACIONALES
 *:                         LENGUAJES Y AUTOMATAS II           
 *: 
 *:                  SEMESTRE: ___________    HORA: ___________ HRS
 *:                                   
 *:               
 *:         Clase con la funcionalidad del Analizador Sintactico
 *                 
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
 *:-----------------------------------------------------------------------------
 */
package compilador;

import javax.swing.JOptionPane;

public class SintacticoSemantico {

    private Compilador cmp;
    private boolean    analizarSemantica = false;
    private String     preAnalisis;
    
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

        // * * *   INVOCAR AQUI EL PROCEDURE DEL SIMBOLO INICIAL   * * *
        PROGRAMA();
    }

    //--------------------------------------------------------------------------

    private void emparejar(String t) {
        if (cmp.be.preAnalisis.complex.equals(t)) {
            cmp.be.siguiente();
            preAnalisis = cmp.be.preAnalisis.complex;            
        } else {
            errorEmparejar( t, cmp.be.preAnalisis.lexema, cmp.be.preAnalisis.numLinea );
        }
    }
    
    //--------------------------------------------------------------------------
    // Metodo para devolver un error al emparejar
    //--------------------------------------------------------------------------
 
    private void errorEmparejar(String _token, String _lexema, int numLinea ) {
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
        } else {
            msjError += "Se esperaba " + _token;
        }
        msjError += " se encontró " + ( _lexema.equals ( "$" )? "fin de archivo" : _lexema ) + 
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
    //  *  *   *   *    PEGAR AQUI EL CODIGO DE LOS PROCEDURES  *  *  *  *
    //--------------------------------------------------------------------------

    private void PROGRAMA(){
        if (preAnalisis.equals("def") || preAnalisis.equals("id") || preAnalisis.equals("if") ||
            preAnalisis.equals("while") || preAnalisis.equals("print") || preAnalisis.equals("int") ||
            preAnalisis.equals("float") || preAnalisis.equals("string")){
             // PROGRAMA -> INSTRUCCION PROGRAMA 
            INSTRUCCION();
            PROGRAMA();
        }
        else{
            //PROGRAMA -> empty
        }
    }
    
    private void INSTRUCCION(){      
        if (preAnalisis.equals("def")){
            //INSTRUCCION -> FUNCION 
            FUNCION();
        }
        else if (preAnalisis.equals("id") || preAnalisis.equals("if") ||
            preAnalisis.equals("while") || preAnalisis.equals("print")){
            //INSTRUCCION ->  PROPOSICION
            PROPOSICION();
        }
        else
            error("error");
    }
    
    private void FUNCION(){
        if (preAnalisis.equals("def")){
            // FUNCION → def id ( ARGUMENTOS ) : TIPO_RETORNO PROPOSICIONES_OPTATIVAS
            emparejar( "def" );
            emparejar( "id" );
            emparejar( "(" );
            ARGUMENTOS();
            emparejar( ")" );
            emparejar( ":" );
            TIPO_RETORNO();
            PROPOSICIONES_OPTATIVAS();
        }
        else
            error("error");
    }
    
    private void ARGUMENTOS(){
        if (preAnalisis.equals("int") ||
            preAnalisis.equals("float") || preAnalisis.equals("string")){
             // ARGUMENTOS -> TIPO_DATO  id  ARGUMENTOS’
            TIPO_DATO();
            emparejar( "id" );
            ARGUMENTOS_2();
        }
        else{
            //ARGUMENTOS -> empty
        }
    }
    
    private void ARGUMENTOS_2(){
        if (preAnalisis.equals(",")){
            // ARGUMENTOS -> ,  TIPO_DATO  id  ARGUMENTOS’
            emparejar( "," );
            TIPO_DATO();
            emparejar( "id" );
            ARGUMENTOS_2();
        }
        else{
            //ARGUMENTOS_2 -> empty
        }
    }
    
    private void DECLARACION_VARS(){
    if (preAnalisis.equals("int") ||
        preAnalisis.equals("float") ||
        preAnalisis.equals("string")){
        
        // DECLARACION_VARS -> TIPO_DATO id DECLARACION_VARS’
        TIPO_DATO();
        emparejar("id");
        DECLARACION_VARS_2();
    }
    else{
        error("Declaracion de variables invalida");
    }
    }
    
    private void DECLARACION_VARS_2(){
    if (preAnalisis.equals(",")){
        // DECLARACION_VARS’ -> , id DECLARACION_VARS’
        emparejar(",");
        emparejar("id");
        DECLARACION_VARS_2();
    }
    else{
        // DECLARACION_VARS_2 -> empty
    }
}
    
    private void TIPO_DATO(){
    if (preAnalisis.equals("int")){
        emparejar("int");
    }
    else if (preAnalisis.equals("float")){
        emparejar("float");
    }
    else if (preAnalisis.equals("string")){
        emparejar("string");
    }
    else{
        error("Tipo de dato invalido");
    }
}
    
    private void TIPO_RETORNO(){
    if (preAnalisis.equals("void")){
        emparejar("void");
    }
    else if (preAnalisis.equals("int")){
        emparejar("int");
    }
    else if (preAnalisis.equals("float")){
        emparejar("float");
    }
    else if (preAnalisis.equals("string")){
        emparejar("string");
    }
    else{
        error("Tipo de retorno invalido");
    }
}
    
    private void RESULTADO(){
        if (preAnalisis.equals("")){
            // Comentario
            // Comentario 2
        }
        else{
            
        }
    }
    
    private void PROPOSICIONES_OPTATIVAS(){
        if (preAnalisis.equals("")){
            
        }
        else{
            
        }
    }
    
    private void PROPOSICION(){

    if (preAnalisis.equals("int") ||
        preAnalisis.equals("float") ||
        preAnalisis.equals("string")){
        
        // PROPOSICION -> DECLARACION_VARS
        DECLARACION_VARS();
    }

    else if (preAnalisis.equals("id")){
        // PROPOSICION -> id PROPOSICION’
        emparejar("id");
        PROPOSICION_2();
    }

    else if (preAnalisis.equals("if")){
        // PROPOSICION -> if CONDICION : PROPOSICIONES_OPTATIVAS else : PROPOSICIONES_OPTATIVAS ::
        emparejar("if");
        CONDICION();
        emparejar(":");
        PROPOSICIONES_OPTATIVAS();
        emparejar("else");
        emparejar(":");
        PROPOSICIONES_OPTATIVAS();
        emparejar("::");
    }

    else if (preAnalisis.equals("while")){
        // PROPOSICION -> while CONDICION : PROPOSICIONES_OPTATIVAS ::
        emparejar("while");
        CONDICION();
        emparejar(":");
        PROPOSICIONES_OPTATIVAS();
        emparejar("::");
    }

    else if (preAnalisis.equals("print")){
        // PROPOSICION -> print ( EXPRESION )
        emparejar("print");
        emparejar("(");
        EXPRESION();
        emparejar(")");
    }

    else{
        error("Proposicion invalida");
    }
}
    
    private void PROPOSICION_2(){

    if (preAnalisis.equals("opasig")){
        // PROPOSICION’ -> opasig EXPRESION
        emparejar("opasig");
        EXPRESION();
    }

    else if (preAnalisis.equals("(")){
        // PROPOSICION’ -> ( LISTA_EXPRESIONES )
        emparejar("(");
        LISTA_EXPRESIONES();
        emparejar(")");
    }

    else{
        error("Se esperaba asignacion o llamada a funcion");
    }
}
    
    private void LISTA_EXPRESIONES(){

    if (preAnalisis.equals("id") ||
        preAnalisis.equals("num") ||
        preAnalisis.equals("num.num") ||
        preAnalisis.equals("(") ||
        preAnalisis.equals("literal")){
        
        // LISTA_EXPRESIONES -> EXPRESION LISTA_EXPRESIONES’
        EXPRESION();
        LISTA_EXPRESIONES_2();
    }
    else{
        // LISTA_EXPRESIONES -> empty
    }
}
    
    private void LISTA_EXPRESIONES_2(){

    if (preAnalisis.equals(",")){
        // LISTA_EXPRESIONES’ -> , EXPRESION LISTA_EXPRESIONES’
        emparejar(",");
        EXPRESION();
        LISTA_EXPRESIONES_2();
    }
    else{
        // LISTA_EXPRESIONES_2 -> empty
    }
}
    
    private void CONDICION(){

    if (preAnalisis.equals("id") ||
        preAnalisis.equals("num") ||
        preAnalisis.equals("num.num") ||
        preAnalisis.equals("(") ||
        preAnalisis.equals("literal")){
        
        // CONDICION -> EXPRESION oprel EXPRESION
        EXPRESION();
        emparejar("oprel");
        EXPRESION();
    }
    else{
        error("Condicion invalida");
    }
}
    
    private void EXPRESION(){ //CORRECCION DE MI BRO: Corrijo la recursión infinita manteniendo tu estilo:

    if (preAnalisis.equals("id") ||
        preAnalisis.equals("num") ||
        preAnalisis.equals("num.num") ||
        preAnalisis.equals("(")){
        
        // EXPRESION -> TERMINO EXPRESION’
        TERMINO();
        EXPRESION_2();
    }
    else if (preAnalisis.equals("literal")){
        // EXPRESION -> literal
        emparejar("literal");
    }
    else{
        error("Expresion invalida");
    }
}
    
    private void EXPRESION_2(){

    if (preAnalisis.equals("opsuma")){
        // EXPRESION’ -> opsuma TERMINO EXPRESION’
        emparejar("opsuma");
        TERMINO();
        EXPRESION_2();
    }
    else{
        // EXPRESION_2 -> empty
    }
}
    
    private void TERMINO(){

    if (preAnalisis.equals("id") ||
        preAnalisis.equals("num") ||
        preAnalisis.equals("num.num") ||
        preAnalisis.equals("(")){
        
        // TERMINO -> FACTOR TERMINO’
        FACTOR();
        TERMINO_2();
    }
    else{
        error("Termino invalido");
    }
}
    
    private void TERMINO_2(){

    if (preAnalisis.equals("opmult")){
        // TERMINO’ -> opmult FACTOR TERMINO’
        emparejar("opmult");
        FACTOR();
        TERMINO_2();
    }
    else{
        // TERMINO_2 -> empty
    }
}
    
    private void FACTOR(){

    if (preAnalisis.equals("id")){
        // FACTOR -> id FACTOR’
        emparejar("id");
        FACTOR_2();
    }
    else if (preAnalisis.equals("num")){
        emparejar("num");
    }
    else if (preAnalisis.equals("num.num")){
        emparejar("num.num");
    }
    else if (preAnalisis.equals("(")){
        emparejar("(");
        EXPRESION();
        emparejar(")");
    }
    else{
        error("Factor invalido");
    }
}
    
    private void FACTOR_2(){

    if (preAnalisis.equals("(")){
        // FACTOR’ -> ( LISTA_EXPRESIONES )
        emparejar("(");
        LISTA_EXPRESIONES();
        emparejar(")");
    }
    else{
        // FACTOR_2 -> empty
    }
}
    
}
//------------------------------------------------------------------------------
//::