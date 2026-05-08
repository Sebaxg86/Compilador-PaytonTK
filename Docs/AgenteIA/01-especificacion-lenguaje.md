# Especificación del lenguaje PaytonTK

- PaytonTK es una variación (dialecto) del reconocido lenguaje de alto nivel Python.
 
- PaytonTK es un lenguaje de tipado estático, es decir se deben declarar todas las variables con su tipo de dato, esta es una diferencia relevante respecto a  Python el cual es un lenguaje de tipado dinámico.

## La estructura general de los programas en PaytonTK es la siguiente:

```
​instruccion1
​instruccion2
​instruccion3
​instruccion4
​. . .
```

donde  las instrucciones pueden ser de declaración de variables, funciones o proposiciones.  

## Ejemplo de un programa sencillo en PaytonTK:

```
int x
x = 0

def imprimir ( int valor ) : void
    print ( valor )
    return void
::

imprimir ( x )
```

Las variables se pueden declarar en cualquier punto del programa, lo mismo las funciones, es decir no hay un orden establecido para definirlos.


## SINTAXIS

PaytonTK requiere que todos las sentencias estén escritas en letra minúscula.

1.  Identificadores
Inician con letra o guión bajo seguido de una sucesión de letras, digitos o guiones bajo. Los nombres de identificadores pueden ser tan largos como se necesiten.

2.  Delimitadores de sentencias
PaytonTK no requiere ningun carácter delimitador para marcar el final de una sentencia.

3.  Tipos de dato
PaytonTK soporta los tipos basicos int, float y string.

4.  Constantes numéricas y literales
PaytonTK soporta constantes estáticas numéricas enteras y de punto flotante tales como  0, 314, 1.12, 3.14159.
De igual forma las constantes literales se representan entre doble comilla, ejemplo “Hola chiquitines”.

5.  Reglas de Sintaxis
A continuación se presentan las reglas sintacticas de lenguaje PaytonTK en notacion  BNF.  

5.1  Sintaxis de declaración de variables

```
<declaración_variables> :=  <tipo_dato> identificador [ , identificador [...] ]
```

donde
`<tipo_dato> := { int |  float  | string  }`

Ejemplo 1: `​float a,  b,  c`

Ejemplo 2: `​int   d`

> Nota: Todas las variables deben ser declaradas antes de usarse.

5.2  Sintaxis de la declaración de funciones

```
<funcion>  :=  def   identificador ( [ <argumentos> ]  )  : <tipo_retorno>
  [ <cuerpo-de-sentencias> ]  
  return <resultado>
::
```

donde
```
<argumentos> := <tipo_dato> identificador [ , <tipo_dato> identificador [...] ]

<tipo_retorno> :=  { void  | <tipo_dato> }

<cuerpo-de-sentencias> :=   ( ver sección 5.3 )

<resultado>  :=   { <expresión> | void }
```

Ejemplos:
```
def funcion1 () : void
    return void
::
```

```
def funcion2 (  int arg1,  float arg2 ) : void
  return void
::
```

```
def funcion3 ( int x, string y ) : int
  return x + 1
​::
```


5.3  Sintaxis de un cuerpo de sentencias

```
<cuerpo-de-sentencias>  :=   <proposicion> [ <proposicion> [ … ]  ]
```

donde
```
<proposicion> := { <declaración_variables> | <asignacion> | <if-elif> |
   <while> | <llamada-func> | <print> }
```

5.4  Sintaxis de la sentencia de asignación

`<asignacion>  :=  identificador  =  <expresion>`

donde​​
```
<expresion>  :=  ​expresiones aritmeticas de suma y multiplicacion pueden incluir parentesis asociativos y puede incluir invocaciones a funciones con o sin argumentos.   Expresión puede ser también una constante literal.
```
​​
Ejemplos:
```
​x  = 666
​y  = 3.1416
​z  = (  2 *  ( x + 1 ) ) + y
​a  = 1 + aleatorio ()* 0.5
​c  = promedio ( x, y, z )
```

5.5  Sintaxis de la sentencia condicional  if-else

```
<if-elif>  :=​if  <expresion>  operador-relacional  <expresion>  :
​​      <cuerpo-sentencias>    
​     ​else :
<cuerpo-sentencias>
::
```

donde
```
operador-relacional  :=   {  >  |  <   |   >=   |   <=   |   !=   |   ==  }
```
---

```
Ejemplo 1​if   a >  b * 2  :
​​    a = a + 1
​​else :
​​    b = b + 1
​​::
```

```
Ejemplo 2
​​if  a <= b * 2  :
​​    a = a + 1
​​    b = b + 1
​​else :
​​::
```
​​​ 

5.6  Sintaxis de la sentencia repetitiva  while
```
<while >   :=  while <expresion>  operador-relacional  <expresion> :
   <cuerpo-sentencias>  
 :: 
```

```
Ejemplo 1:​​
​while   a >  b * 2  :
​    a = a + 1
​::
```

```
Ejemplo 2:​​
​while  a <= b * 2  :
​    a = a + 1
​    b = b + 1
​::
```


5.7  Sintaxis de la sentencia de llamada a funciones:
```
<llamada-func>  :=   identificador (  [ <argumentos> ] )
```

donde
```
​<argumentos> :=   <expresion> [ , <expresión> [ … ] ]
```

Ejemplo:​​
```
​imprimir_resultado ()
​calcular ( 2 * a, b + 1, c )
```
​​​  
5.8  Sintaxis de la sentencia print:

`<print>  :=   print (  <expresion> )`

Ejemplo:​​
```
​print ( x )
​print ( “hola chiquitines” )
​print ( 2 * b + 1 )
```
