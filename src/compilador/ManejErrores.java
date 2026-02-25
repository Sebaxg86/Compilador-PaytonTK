/*:-----------------------------------------------------------------------------
 *:                       INSTITUTO TECNOLOGICO DE LA LAGUNA
 *:                     INGENIERIA EN SISTEMAS COMPUTACIONALES
 *:                         LENGUAJES Y AUTOMATAS II           
 *: 
 *:                  SEMESTRE: AGO-DIC/2013     HORA: xx - xx HRS
 *:                                   
 *:               
 *:    # Clase que implementa la funcionalidad del Manejador de Errores del Compilador.
 *                 
 *:                           
 *: Archivo       : ManejErrores.java
 *: Autor         : Fernando Gil  
 *: Fecha         : 03/SEP/2013
 *: Compilador    : Java JDK 7
 *: Descripción   : Esta clase recibe la invocacion de alguno de sus metodos 
 *:                 error ()  y transfiere la llamada a la Interfaz de Usuario.
 *:                  
 *:           	     
 *: Ult.Modif.    :
 *:  Fecha      Modificó            Modificacion
 *:=============================================================================
 *: 11/Sep/2023 F.Gil               -Mantener guardado en memoria el 1er mensaje
 *:                                 de error detectado, segun la etapa de compilacion.
 *:-----------------------------------------------------------------------------
 */



package compilador;

public class ManejErrores {

    private Compilador compilador;
    private int        totErrLexico      = 0;
    private int        totErrSintacticos = 0;
    private int        totErrSemanticos  = 0;
    private int        totErrCodInt      = 0;
    private int        totErrCodObj      = 0;
    private int        totWarningsSem    = 0;
    
    private String     primerMensErrLexico     = "";
    private String     primerMensErrSintactico = "";
    private String     primerMensErrSemantico  = "";
    private String     primerMensErrCodInt     = "";
    private String     primerMensErrCodObj     = "";
    
    //--------------------------------------------------------------------------
    
    public ManejErrores ( Compilador c ) {
        compilador = c;
    }

    //--------------------------------------------------------------------------
	
	//--------------------------------------------------------------------------
    public void inicializar () {
        totErrLexico = totErrSintacticos = totErrSemanticos = totErrCodInt = 0;
        totWarningsSem = 0;
        
        primerMensErrLexico = primerMensErrSintactico = primerMensErrSemantico = "";
        primerMensErrCodInt = primerMensErrCodObj = "";
    }
    //--------------------------------------------------------------------------
    
    public void error ( int tipoError, String errorMensaje ) {
      // Contabilizar el error, incrementando la cuenta en 1. 
        switch  ( tipoError ) {
           case Compilador.ERR_LEXICO      : totErrLexico++; 
		                             errorMensaje = "ERROR LEXICO: " + errorMensaje; 
                                             if ( totErrLexico == 1 )
                                                 // Si es el primer error lexico detectado, guardar el mensaje.
                                                 primerMensErrLexico = errorMensaje;
                                             break;
           case Compilador.ERR_SINTACTICO  : totErrSintacticos++;
		                             errorMensaje = "ERROR SINTACTICO: " + errorMensaje; 
                                             if ( totErrSintacticos == 1 )
                                                 // Si es el primer error sintactico detectado, guardar el mensaje.
                                                 primerMensErrSintactico = errorMensaje;                                             
                                             break;
           case Compilador.ERR_SEMANTICO   : totErrSemanticos++;
		                             errorMensaje = "ERROR SEMANTICO: " + errorMensaje; 
                                             if ( totErrSemanticos == 1 )
                                                 // Si es el primer error semantico detectado, guardar el mensaje.
                                                 primerMensErrSemantico = errorMensaje;                                              
                                             break;
           case Compilador.ERR_CODINT      : totErrCodInt++;
		                             errorMensaje = "ERROR COD. INT.: " + errorMensaje; 
                                             if ( totErrCodInt == 1 )
                                                 // Si es el primer error de Codigo Interm detectado, guardar el mensaje.
                                                 primerMensErrCodInt = errorMensaje;                                              
                                             break;
           case Compilador.ERR_CODOBJ      : totErrCodObj++;
		                             errorMensaje = "ERROR COD. OBJ.: " + errorMensaje; 
                                             if ( totErrCodObj == 1 )
                                                 // Si es el primer error codigo objeto detectado, guardar el mensaje.
                                                 primerMensErrCodObj = errorMensaje;                                              
                                             break;                                             
           case Compilador.WARNING_SEMANT  : totWarningsSem++;
		                             errorMensaje = "WARNING SEMANTICO: " + errorMensaje; 
                                             break;
		}

        if ( tipoError == Compilador.WARNING_SEMANT )
            // Manejo de Warnings
            compilador.iuListener.mostrarWarning ( errorMensaje );
        else
            // Invocar el despliegue del error a la GUI
            compilador.iuListener.mostrarErrores ( errorMensaje );
    }
    
    //--------------------------------------------------------------------------
    public int getTotErrLexico      () { return totErrLexico;      }
    //--------------------------------------------------------------------------
    public int getTotErrSintacticos () { return totErrSintacticos; }
    //--------------------------------------------------------------------------
    public int getTotErrSemanticos  () { return totErrSemanticos;  }
    //--------------------------------------------------------------------------
    public int getTotErrCodInt      () { return totErrCodInt;      }
    //--------------------------------------------------------------------------
    public int getTotErrCodObj      () { return totErrCodObj;      }
    //--------------------------------------------------------------------------
    public int getTotWarningsSem    () { return totWarningsSem;    }
    //--------------------------------------------------------------------------
    public String getPrimerMensErrLexico    () { return primerMensErrLexico;     }
    //--------------------------------------------------------------------------
    public String getPrimerMensErrSintatcio () { return primerMensErrSintactico; }
    //--------------------------------------------------------------------------
    public String getPrimerMensErrSemantico () { return primerMensErrSemantico;  }
    //--------------------------------------------------------------------------
    public String getPrimerMensErrCodInt    () { return primerMensErrCodInt;     }
    //--------------------------------------------------------------------------
    public String getPrimerMensErrCodObj    () { return primerMensErrCodObj;     }
    //--------------------------------------------------------------------------
    
}
