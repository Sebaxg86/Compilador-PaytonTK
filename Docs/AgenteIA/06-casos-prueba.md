# Casos de prueba

## Programas de prueba correctos para la etapa Sintáctica

Ejemplo 1: 00-ProgramaMinimo.py
```

```

Ejemplo 2: 01-DeclaracionVariables.py
```
int x
float y
string z
int a, b, c
float d, e, f, g
string j, k
```

Ejemplo 3: 02-DeclUnaFuncion.py
```
def funcion_1 ( ) : void
  return void
::
```

Ejemplo 4: 03-DeclVariasFunciones.py
```
def  func_2 () : string
   return "hola"
::

def  func_3 () : int
   return 0
::

def  func_4 () : float
   return 3.14
::

def  func_5 ( int a ) : string
   return "Payton"
::

def  func_6 ( int a, float b ) : int
   return 0
::

def  func_7 ( int a, float b, string c ) : float
   return 3.14
::
```

Ejemplo 5: 04-Asignaciones.py
```
x  = 666
y  = 3.1416
z  = (  2 *  ( x + 1 ) ) + y 
a  = 1 + aleatorio ()* 0.5
c  = promedio ( x, y, z )
```

Ejemplo 6: 05-IfElse.py
```
if   a >  b * 2  :
else :
::

if   a >  b * 2  :
  a = a + 1
else :
  b = b + 1 
::

if  a <= b * 2  :
  a = a + 1
  b = b + 1
else :
::

if  a <= b * 2  :
else :
  a = a + 1
  b = b + 1
::
```

Ejemplo 7: 06-While.py
```
while   a >  b * 2  :
::

while   a >  b * 2  :
  a = a + 1
::

while  a <= b * 2  :
  a = a + 1
  b = b + 1
::
```

Ejemplo 8: 07-LlamadaFunciones.py
```
imprimir_resultado ()
calcular ( 2 * a, b + 1, c )
```

Ejemplo 9: 08-print.py
```
print ( x )
print ( "hola chiquitines" )
print ( 2 * b + 1 )
```

Ejemplo 10: 09-Combo_Decl_Asig.py
```
print ( "hola chiquitines" )
int x
float b
print ( x )
b = 10.25
print ( 2 * b + 1 )
string j, k
```

Ejemplo 11: 10-Combo_Asig_fElse.py
```
z  = (  2 *  ( x + 1 ) ) + y 
a  = 1 + aleatorio () * 0.5

if   a >  b * 2  :
  x  = 666
  y  = 3.1416
  z  = (  2 *  ( x + 1 ) ) + y 
else :
::

a  = 1 + aleatorio () * 0.5
c  = promedio ( x, y, z )

if   a >  b * 2  :
else :
  x  = 666
  y  = 3.1416
  z  = (  2 *  ( x + 1 ) ) + y 
::

a  = 1 + aleatorio () * 0.5
c  = promedio ( x, y, z )
```

Ejemplo 12: 11-Combo_Asig_While.py
```
while   a >  b * 2  :
::

x  = 666
y  = 3.1416

while   a >  b * 2  :
  a = a + 1
  z  = (  2 *  ( x + 1 ) ) + y 
  c  = promedio ( x, y, z )
::

a  = 1 + aleatorio ()* 0.5

while  a <= b * 2  :
  a = a + 1
  b = b + 1
::
```

Ejemplo 13: 12-Combo_Peligroso.py
```
int x
float y
x  = 666
y  = 3.1416

def  func_2 () : string
   z  = (  2 *  ( x + 1 ) ) + y 
   a  = 1 + aleatorio ()* 0.5
   c  = promedio ( x, y, z )
   return "hola"
::

print ( "hola chiquitines" )
print ( 2 * b + 1 )

def  func_3 () : int
   if   a >  b * 2  :
     a = a + 1
   else :
     b = b + 1 
   ::
   return 0
::

def  func_4 () : float
   while  a <= b * 2  :
     a = a + 1
     b = b + 1
   ::
   return 3.14
::

print ( "hola chiquitines" )
print ( 2 * b + 1 )

float d, e, f, g
string j, k

def  func_5 ( int a ) : string
   c  = promedio ( x, y, z )
   while  a <= b * 2  :
     if   a >  b * 2  :
       a = a + 1
     else :
       b = b + 1 
     ::
   ::
   return "Payton"
::

print ( "hola chiquitines" )
print ( 2 * b + 1 )

string z
int a, b, c
```

## Programas de prueba correctos para la etapa Semántica

Ejemplo 1: 00-ProgramaMinimo.py
```

```

Ejemplo 2: 01-DeclaracionVariables.py
```
int x
float y
string z
int a, b, c
float d, e, f, g
string j, k
```

Ejemplo 3: 03-DeclVariasFunciones.py
```
def  func_2 () : string
   return "hola"
::

def  func_3 () : int
   return 0
::

def  func_4 () : float
   return 3.14
::

def  func_5 ( int a ) : string
   return "Payton"
::

def  func_6 ( int aa, float bb ) : int
   return 0
::

def  func_7 ( int aaa, float bbb, string ccc ) : float
   return 3.14
::
```

Ejemplo 4: 04-Asignaciones.py
```
int x
x  = 666

float y
y  = 3.1416

float z
z  = (  2 *  ( x + 1 ) ) + y 

string s
s = "hola"
```

Ejemplo 5: 05-IfElse.py
```
int    a, i
float  b, f
string c, s

if  0 <= 0.1  :
else :
::

if   a >  b :
else :
::

if   a + 2 * a >  b * 2.1 + 1.0  :
else :
::

if  c == s  :
else :
::

if  c == "hola"  :
else :
::
```

Ejemplo 6: 06-While.py
```
int    a, i
float  b, f
string c, s

while  0 <= 0.1  :
::

while   a >  b :
::

while   a + 2 * a >  b * 2.1 + 1.0  :
::

while  c == s  :
::

while  c == "hola"  :
::
```

Ejemplo 7: 08-print.py
```
string x
float  b

print ( x )
print ( "hola chiquitines" )
print ( 2 * b + 1 )
```

Ejemplo 8: 09-llamadas a funciones.py
```
def func_0 () : void
  return void
::

def  func_2 () : string
   return "hola"
::

def  func_3 () : int
   return 0
::

def  func_4 () : float
   return 3.14
::

def  func_5 ( int a ) : string
   return "Payton"
::

def  func_6 ( int aa, float bb ) : int
   return 0
::

def  func_7 ( int aaa, float bbb, string ccc ) : float
   return 3.14
::

int    i
float  f
string s

func_0 ()

s = func_2 ()
i = func_3 ()
f = func_4 ()
s = func_5 ( 1 )
i = func_6 ( 2 * i, f )
f = func_7 ( i, i * 2 + 6, "hola" )
```