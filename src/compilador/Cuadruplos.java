package compilador;

import java.util.ArrayList;


public class Cuadruplos {
    public  ArrayList<Cuadruplo> cuadruplos;
    private Compilador cmp;
    
    
    public Cuadruplos ( Compilador c ) {
        cmp = c;
        cuadruplos = new ArrayList<> ();
    }
          
    public void inicializar () {
        vaciar ();
    }
    
    public void agregar ( Cuadruplo cuadruplo ) {
        cuadruplos.add ( cuadruplo );
    }

    public void generarDesdeCodigoIntermedio ( ArrayList<String> codigoIntermedio ) {
        vaciar ();

        for ( String sentencia : codigoIntermedio ) {
            Cuadruplo cuadruplo = convertirSentenciaC3D ( sentencia );
            if ( cuadruplo != null ) {
                agregar ( cuadruplo );
            }
        }
    }
    
    public void vaciar () {
        cuadruplos.clear ();
    }
    
    public int getTamano () {
        return cuadruplos.size();
    }
    
    public ArrayList<Cuadruplo> getCuadruplos () {
        return cuadruplos;
    }

    private Cuadruplo convertirSentenciaC3D ( String sentencia ) {
        if ( sentencia == null ) {
            return null;
        }

        sentencia = sentencia.trim ();
        if ( "".equals ( sentencia ) ) {
            return null;
        }

        if ( sentencia.endsWith ( ":" ) ) {
            return new Cuadruplo ( "", "", "", sentencia.substring ( 0, sentencia.length () - 1 ) );
        }

        if ( sentencia.startsWith ( "goto " ) ) {
            return new Cuadruplo ( "goto", "", "", sentencia.substring ( 5 ).trim () );
        }

        if ( sentencia.startsWith ( "if " ) ) {
            return convertirSaltoCondicional ( sentencia );
        }

        if ( sentencia.startsWith ( "param " ) ) {
            return new Cuadruplo ( "param", sentencia.substring ( 6 ).trim (), "", "" );
        }

        if ( sentencia.startsWith ( "call " ) ) {
            return convertirLlamada ( sentencia.substring ( 5 ).trim (), "" );
        }

        if ( "return".equals ( sentencia ) ) {
            return new Cuadruplo ( "return", "", "", "" );
        }

        if ( sentencia.startsWith ( "return " ) ) {
            return new Cuadruplo ( "return", sentencia.substring ( 7 ).trim (), "", "" );
        }

        if ( sentencia.startsWith ( "print " ) ) {
            return new Cuadruplo ( "print", sentencia.substring ( 6 ).trim (), "", "" );
        }

        int posAsig = sentencia.indexOf ( " := " );
        if ( posAsig >= 0 ) {
            String resultado = sentencia.substring ( 0, posAsig ).trim ();
            String expresion = sentencia.substring ( posAsig + 4 ).trim ();
            return convertirAsignacion ( resultado, expresion );
        }

        return new Cuadruplo ( "", "", "", sentencia );
    }

    private Cuadruplo convertirSaltoCondicional ( String sentencia ) {
        String [] partes = sentencia.split ( "\\s+" );
        if ( partes.length >= 6 && "goto".equals ( partes [ 4 ] ) ) {
            return new Cuadruplo ( partes [ 2 ], partes [ 1 ], partes [ 3 ], partes [ 5 ] );
        }

        return new Cuadruplo ( "if", sentencia, "", "" );
    }

    private Cuadruplo convertirAsignacion ( String resultado, String expresion ) {
        if ( expresion.startsWith ( "call " ) ) {
            return convertirLlamada ( expresion.substring ( 5 ).trim (), resultado );
        }

        String [] partes = expresion.split ( "\\s+" );
        if ( partes.length == 3 ) {
            return new Cuadruplo ( partes [ 1 ], partes [ 0 ], partes [ 2 ], resultado );
        }

        if ( partes.length == 2 ) {
            return new Cuadruplo ( partes [ 0 ], partes [ 1 ], "", resultado );
        }

        return new Cuadruplo ( ":=", expresion, "", resultado );
    }

    private Cuadruplo convertirLlamada ( String llamada, String resultado ) {
        int posComa = llamada.lastIndexOf ( "," );
        if ( posComa >= 0 ) {
            String funcion = llamada.substring ( 0, posComa ).trim ();
            String argumentos = llamada.substring ( posComa + 1 ).trim ();
            return new Cuadruplo ( "call", funcion, argumentos, resultado );
        }

        return new Cuadruplo ( "call", llamada, "", resultado );
    }
    
}
