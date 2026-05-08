# Guía para elaborar el esquema de traducción de Generación de Código Intermedio

Fuente base: **Apuntes — Unidad III: Generación de Código Intermedio**.

> Objetivo: concentrar la información fundamental del documento para que un agente de IA pueda usarla como guía al construir un esquema de traducción para generación de Código de 3 Direcciones, manteniendo el estándar de los apuntes.

---

## 1. Contexto general

Un compilador, en su etapa inicial, traduce un programa fuente a una **representación intermedia**, a partir de la cual la etapa final genera el código objeto.

Flujo mostrado en los apuntes:

```txt
Analizador Semántico
Comprobador Estático de Tipos
Generador de Código Intermedio
Generador de Código Intermedio Óptimo
Código Intermedio
Código Objeto
```

---

## 2. Códigos intermedios

Los apuntes mencionan como códigos intermedios:

```txt
- Árbol Sintáctico
- Notación Postfija
- Código de 3 Direcciones
```

El enfoque principal de la unidad es el **Código de 3 Direcciones**, abreviado como **C3D**.

---

## 3. Código de 3 Direcciones, C3D

El Código de 3 Direcciones recibe este nombre porque en cada proposición participan, como máximo, tres variables y normalmente se utiliza un operador.

Forma general:

```txt
x := y op z
```

Donde:

```txt
x, y, z  Son nombres, variables, constantes o variables temporales.
op       Es cualquier operador lógico o aritmético.
```

Ejemplo:

```txt
x + y * z
```

Se traduce a C3D como:

```txt
t1 := y * z
t2 := x + t1
```

Donde:

```txt
t1 y t2 son variables temporales creadas por el compilador.
```

La descomposición de expresiones aritméticas complejas y proposiciones de flujo de control anidadas hace que el C3D sea adecuado para generar código objeto y para optimización.

---

## 4. Ejemplo base de C3D

Expresión:

```txt
a := b * - c + b * - c
```

C3D correspondiente:

```txt
t1 := - c
t2 := b * t1
t3 := - c
t4 := b * t3
a := t2 + t4
```

---

## 5. Tipos de proposiciones de 3 direcciones

Las proposiciones de 3 direcciones son análogas al ensamblador. Las más comunes son:

### a) Asignación binaria

```txt
x := y op z
```

### b) Asignación unaria

```txt
x := op y
```

### c) Proposición de copia

```txt
x := y
```

### d) Salto incondicional

```txt
goto E
```

### e) Salto condicional

```txt
if x oprel y goto E
```

### f) Llamadas a procedimientos

```txt
param x
call p, m
return y
```

Donde:

```txt
p  Nombre del procedimiento
m  Número de argumentos
```

### g) Asociaciones con índices

```txt
x := y [ i ]
x [ i ] := y
```

### h) Direcciones y apuntadores

```txt
x := & y
x := * y
* x := y
```

---

## 6. Proposiciones de asignación

### Esquema de traducción para convertir asignaciones a C3D

```txt
S → id := E {
    p := busca ( id.nombre )
    if p ≠ nil then emite ( p ":=" E.Lugar )
    else ERROR
}

E → E1 + E2 {
    E.Lugar := tempnuevo()
    Emite ( E.Lugar ":=" E1.Lugar "+" E2.Lugar )
}

E → - E1 {
    E.Lugar := tempnuevo()
    Emite ( E.Lugar ":=" "-" E1.Lugar )
}

E → E1 * E2 {
    E.Lugar := tempnuevo()
    Emite ( E.Lugar ":=" E1.Lugar "*" E2.Lugar )
}

E → ( E1 ) {
    E.Lugar := E1.Lugar
}

E → id {
    p := busca ( id.nombre )
    if p ≠ nil then E.Lugar := p
    else ERROR
}
```

---

## 7. Funciones auxiliares necesarias

### `busca(id.nombre)`

Comprueba si existe una entrada para el nombre en la tabla de símbolos.

```txt
Si existe: devuelve el apuntador a la entrada.
Si no existe: devuelve nil.
```

### `emite(...)`

Escribe la proposición de C3D en un archivo de salida.

### `tempnuevo()`

Devuelve una secuencia de nombres temporales distintos en llamadas sucesivas.

Ejemplo:

```txt
t1, t2, t3, ...
```

### `etiqnueva()`

Devuelve una nueva etiqueta para flujo de control.

Ejemplo:

```txt
etiq1, etiq2, etiq3, ...
```

---

## 8. Análisis léxico, sintáctico y generación de C3D

Ejemplo del documento:

```txt
a := b * - c + b * - c
```

### 1. Análisis léxico

```txt
id1 := id2 * - id3 + id2 * - id3
```

Tabla de símbolos:

```txt
Entrada | Lexema | Complex | Tipo
--------|--------|---------|------
1       | a      | id      | ...
2       | b      | id      | ...
3       | c      | id      | ...
```

### 2. Esquema de traducción usado

```txt
S → id := E {1}
E → E1 + E2 {2}
E → - E1 {3}
E → E1 * E2 {4}
E → ( E1 ) {5}
E → id {6}
```

### 3. Generación de Código Intermedio

Recorrido en profundidad:

```txt
Símbolo | Lugar
--------|------
E1      | t5
E2      | t2
E3      | [2]
E4      | t1
E5      | [3]
E6      | t4
E7      | [2]
E8      | t3
E9      | [3]
```

Salidas C3D:

```txt
t1 := - [3]
t2 := [2] * t1
t3 := - [3]
t4 := [2] * t3
t5 := t2 + t4
[1] := t5
```

---

## 9. Generación de C3D para proposiciones de flujo de control

Gramática base:

```txt
S → if E then S1
S → if E then S1 else S2
S → while E do S1
```

En esta gramática, el no-terminal `E` representa una expresión booleana y se asocia con dos etiquetas:

```txt
E.verdadera
E.falsa
```

Donde:

```txt
E.verdadera  Etiqueta a la que fluye el control si E es verdadera.
E.falsa      Etiqueta a la que fluye el control si E es falsa.
```

Además:

```txt
S.siguiente
```

Es una etiqueta asociada a la primera instrucción de tres direcciones que se ejecuta después del código correspondiente a `S`.

---

## 10. Traducción básica de una condición

Supóngase que `E` es de la forma:

```txt
a < b
```

Entonces el código generado es:

```txt
if a < b goto E.verdadera
goto E.falsa
```

---

## 11. Esquemas de flujo de control

### a) `if then`

Estructura conceptual:

```txt
E.codigo
E.verdadera:
S1.codigo
E.falsa:
...
```

### b) `if then else`

Estructura conceptual:

```txt
E.codigo
E.verdadera:
S1.codigo
goto S.siguiente
E.falsa:
S2.codigo
S.siguiente:
...
```

### c) `while do`

Estructura conceptual:

```txt
S.comienzo:
E.codigo
E.verdadera:
S1.codigo
goto S.comienzo
E.falsa:
...
```

---

## 12. Definiciones dirigidas por la sintaxis para flujo de control

### Producción: `if E then S1`

```txt
S → if E then S1

E.verdadera := etiqnueva();
E.falsa := S.siguiente;
S.codigo := E.codigo || emite(E.verdadera ':') || S1.codigo;
```

### Producción: `if E then S1 else S2`

```txt
S → if E then S1 else S2

E.verdadera := etiqnueva();
E.falsa := etiqnueva();
S1.siguiente := S.siguiente;
S2.siguiente := S.siguiente;
S.codigo := E.codigo || emite(E.verdadera ':')
          || S1.codigo || emite('goto' S.siguiente)
          || emite(E.falsa ':') || S2.codigo;
```

### Producción: `while E do S1`

```txt
S → while E do S1

S.comienzo := etiqnueva();
E.verdadera := etiqnueva();
E.falsa := S.siguiente;
S1.siguiente := S.comienzo;
S.codigo := emite(S.comienzo ':') || E.codigo
          || emite(E.verdadera ':') || S1.codigo
          || emite('goto' S.comienzo);
```

---

## 13. Ejemplo de C3D para `mientras`

Programa en lenguaje PROGRA:

```txt
mientras x > 10 hacer
inicio
    y := 1;
fin
```

### Análisis léxico

```txt
mientras id1 oprel num2 hacer
inicio
    id3 := num4;
fin
```

### Salidas C3D

```txt
etiq1: if [1] > [2] goto etiq3
       goto etiq2
etiq3:
etiq3: [3] := [4]
       goto etiq1
etiq2:
```

---

## 14. Traducción a flujo de control de expresiones booleanas

Las expresiones booleanas se usan en construcciones como:

```txt
if E then S1 else S2
while E do S1
```

Si `E` es de la forma:

```txt
E1 OR E2
```

Entonces:

```txt
Si E1 es verdadera, E también es verdadera.
Si E1 es falsa, se debe evaluar E2.
```

Por eso:

```txt
E1.verdadera = E.verdadera
E1.falsa     = etiqueta de inicio del código de E2
```

Si `E` es de la forma:

```txt
E1 AND E2
```

Entonces se aplican consideraciones análogas:

```txt
Si E1 es verdadera, se evalúa E2.
Si E1 es falsa, E también es falsa.
```

Si `E` es de la forma:

```txt
NOT E1
```

Entonces se intercambian las salidas verdadera y falsa de `E1`.

---

## 15. Producciones y reglas semánticas para expresiones booleanas

### Producción: `E → E1 OR E2`

```txt
E1.verdadera := E.verdadera;
E1.falsa := etiqnueva();
E2.verdadera := E.verdadera;
E2.falsa := E.falsa;
E.codigo := E1.codigo || emite(E1.falsa ':') || E2.codigo;
```

### Producción: `E → E1 AND E2`

```txt
E1.verdadera := etiqnueva();
E1.falsa := E.falsa;
E2.verdadera := E.verdadera;
E2.falsa := E.falsa;
E.codigo := E1.codigo || emite(E1.verdadera ':') || E2.codigo;
```

### Producción: `E → NOT E1`

```txt
E1.verdadera := E.falsa;
E1.falsa := E.verdadera;
E.codigo := E1.codigo;
```

### Producción: `E → ( E1 )`

```txt
E1.verdadera := E.verdadera;
E1.falsa := E.falsa;
E.codigo := E1.codigo;
```

### Producción: `E → id1 oprel id2`

```txt
E.codigo := emite('if' id1.Lugar oprel.lexema id2.Lugar 'goto' E.verdadera)
         || emite('goto' E.falsa);
```

### Producción: `E → true`

```txt
E.codigo := emite('goto' E.verdadera);
```

---

## 16. Ejemplo de C3D para `si ... entonces ... sino`

Código fuente:

```txt
Si a >= 5 Y a <= 10 entonces
inicio
    a := 0;
fin
Sino
inicio
    a := 100;
fin
```

### Análisis léxico

```txt
si id1 oprel num2 Y id1 oprel num3 entonces
inicio
    id1 := num4;
fin
sino
inicio
    id1 := num5;
fin
```

Tabla de símbolos:

```txt
Entrada | Lexema | Complex | Tipo
--------|--------|---------|------
1       | a      | id      | ...
2       | 5      | num     | ...
3       | 10     | num     | ...
4       | 0      | num     | ...
5       | 100    | num     | ...
```

### Salidas C3D

```txt
if [1] >= [2] goto etiq3
goto etiq2
etiq3:
if [1] <= [3] goto etiq4
goto etiq2
etiq4:
[1] := [4]
goto etiq1
etiq2:
[1] := [5]
etiq1:
```

---

## 17. Ejercicio del documento: lenguaje Simple

Implementar en Java el siguiente esquema de traducción que genera C3D para lenguaje Simple.

### Gramática

```txt
P  → V C
V  → id : T V1 | empty
T  → entero | real | character
C  → inicio S fin
S  → id opasig E {1} S | S
E  → num E' {5} | num.num E' {6} | id E' {7}
E' → oparit E {8} | empty {9}
```

### Acciones semánticas

#### Acción 1

```txt
p := busca ( id.lexema )
if p != NIL then
    emite ( p ':=' E.Lugar )
else
    ERROR
```

#### Acción 5

```txt
if E'.op == "" then
    E.Lugar := num.entrada
else
begin
    E.lugar := tempnuevo ()
    emite ( E.Lugar || ':=' || num.entrada || E'.op || E'.Lugar )
end
```

#### Acción 6

```txt
if E'.op == "" then
    E.Lugar := num.num.entrada
else
begin
    E.lugar := tempnuevo ()
    emite ( E.Lugar || ':=' || num.num.entrada || E'.op || E'.Lugar )
end
```

#### Acción 7

```txt
if E'.op == "" then
    E.Lugar := id.entrada
else
begin
    E.lugar := tempnuevo ()
    emite ( E.Lugar || ':=' || id.entrada || E'.op || E'.Lugar )
end
```

#### Acción 8

```txt
E'.op := oparit.lexema
E'.Lugar := E.Lugar
```

#### Acción 9

```txt
E'.op := ""
```

---

## 18. Caso de estudio: Generador de Código Intermedio para Lenguaje PROGRA

### Gramática con acciones semánticas

```txt
P -> D C
D -> V ; D | ε
V -> id : T
T -> caracter | entero | real | &T | arreglo [ num ] of T
C -> inicio S fin
S -> Z ; S | ε
Z -> id A := E {1}
Z -> {19} I {20} L {21}
I -> si {22} B entonces inicio S fin
L -> sino inicio {23} S fin | ε {24}
Z -> mientras {16} B {17} hacer inicio S fin {18}
E -> H {5} R {6}
R -> oparit H {7} R {8} | ε {9}
H -> ( E ) {10} | -E {11} | id A {12} | num {13} | num.num {14} | literal {15}
A -> [ E ] {2} | & {3} | ε {4}
B -> {25} X {26} W
W -> O {27} X {28} W | Y {29} X {28} W | ε {30}
X -> ( {33} X ) | id oprel F {31} | cierto {32} | falso
F -> id A {37} | num {34} | num.num {35} | literal {36}
```

---

## 19. Tabla de acciones semánticas del Generador de Código Intermedio para PROGRA

### Acción 1

```txt
p := id.entrada;
if p <> nil then
begin
    u := if A.lugar = nil then p else p || ',' || A.lugar;
    emite ( u ':=' E.lugar );
end
else
    error;
```

Error del GCI:

```txt
101
```

### Acción 2

```txt
A.lugar := E.lugar;
```

### Acción 3

```txt
A.lugar := nil;
```

### Acción 4

```txt
A.lugar := nil;
```

### Acción 5

```txt
R.h := H.lugar;
```

### Acción 6

```txt
E.lugar := R.lugar;
```

### Acción 7

```txt
R1.h := tempnuevo;
emite ( R1.h ':=' R.h oparit.lexema H.lugar );
```

### Acción 8

```txt
R.lugar := R1.lugar;
```

### Acción 9

```txt
R.lugar := R.h;
```

### Acción 10

```txt
H.lugar := E.lugar;
```

### Acción 11

```txt
H.lugar := tempnuevo;
emite ( H.lugar ':= menosu' E.lugar );
```

### Acción 12

```txt
p := id.entrada;
if p <> nil then
begin
    u := if A.lugar = nil then p else p || ',' || A.lugar;
    H.lugar := u;
end
else
    error;
```

Error del GCI:

```txt
102
```

### Acción 13

```txt
p := num.entrada;
if p <> nil then H.lugar := p else error;
```

Error del GCI:

```txt
103
```

### Acción 14

```txt
p := num.num.entrada;
if p <> nil then H.lugar := p else error;
```

Error del GCI:

```txt
104
```

### Acción 15

```txt
p := literal.entrada;
if p <> nil then H.lugar := p else error;
```

Error del GCI:

```txt
105
```

### Acción 16

```txt
Z.comienzo := etiqnueva;
Z.siguiente := etiqnueva;
B.verdadera := etiqnueva;
B.falsa := Z.siguiente;
S.siguiente := Z.comienzo;
emite ( Z.comienzo ':' );
```

### Acción 17

```txt
emite ( B.verdadera ':' );
```

### Acción 18

```txt
emite ( 'goto' Z.comienzo );
emite ( B.falsa ':' );
```

### Acción 19

```txt
Z.siguiente := etiqnueva;
I.siguiente := etiqnueva;
```

### Acción 20

```txt
L.h := I.siguiente;
L.siguiente := Z.siguiente;
```

### Acción 21

```txt
emite ( L.siguiente ':' );
```

### Acción 22

```txt
B.verdadero := etiqnueva;
B.falsa := I.siguiente;
S.siguiente := I.siguiente;
```

### Acción 23

```txt
emite ( 'goto' L.siguiente );
emite ( L.h ':' );
```

### Acción 24

```txt
L.siguiente := L.h;
```

### Acción 25

```txt
X.verdadero := B.verdadero;
X.falso := B.falsa;
```

### Acción 26

```txt
W.verdadero := X.verdadero;
W.falsa := X.falsa;
```

### Acción 27

```txt
X.verdadero := W.verdadero;
X.falsa := W.falsa;
W.falsa := etiqnueva;
emite ( 'goto' W.falsa );
emite ( W.falsa ':' );
```

### Acción 28

```txt
W1.verdadero := X.verdadero;
W1.falsa := X.falsa;
```

### Acción 29

```txt
X.verdadero := W.verdadero;
X.falsa := W.falsa;
W.verdadero := etiqnueva;
emite ( 'goto' X.falsa );
emite ( X.verdadero ':' );
X.verdadero := W.verdadero;
```

### Acción 30

```txt
emite ( 'goto' W.falsa );
emite ( W.verdadera ':' );
```

### Acción 31

```txt
p := id.entrada;
if p <> nil then
    emite ( 'if' p oprel.lexema F.lugar 'goto' X.verdadera )
else
    error;
```

Error del GCI:

```txt
106
```

### Acción 32

```txt
emite ( 'goto' X.verdadera );
```

### Acción 33

```txt
X1.verdadera := X.verdadera;
X1.falsa := X.falsa;
```

### Acción 34

```txt
p := num.entrada;
if p <> nil then F.lugar := p else error;
```

Error del GCI:

```txt
107
```

### Acción 35

```txt
p := num.num.entrada;
if p <> nil then F.lugar := p else error;
```

Error del GCI:

```txt
108
```

### Acción 36

```txt
p := literal.entrada;
if p <> nil then F.lugar := p else error;
```

Error del GCI:

```txt
109
```

### Acción 37

```txt
p := id.entrada;
if p <> nil then
begin
    u := if A.lugar = nil then p else p || ',' || A.lugar;
    F.lugar := u;
end
else
    error;
```

Error del GCI:

```txt
110
```

---

## 20. Atributos clave para el agente de IA

Para construir un esquema de traducción de generación de código intermedio, considerar los siguientes atributos:

```txt
.lugar       Guarda el lugar donde queda el resultado de una expresión.
.h           Atributo heredado usado para propagar lugares o tipos según la producción.
.verdadera   Etiqueta de salida cuando una condición es verdadera.
.falsa       Etiqueta de salida cuando una condición es falsa.
.siguiente   Etiqueta de la instrucción que sigue después de una proposición.
.comienzo    Etiqueta inicial de una estructura de repetición.
```

---

## 21. Estándar de implementación sugerido a partir del documento

Un esquema de traducción para generación de código intermedio debe seguir este patrón:

1. Mantener la gramática del lenguaje.
2. Insertar acciones semánticas numeradas entre llaves `{n}`.
3. Usar atributos como `.lugar`, `.h`, `.verdadera`, `.falsa`, `.siguiente` y `.comienzo`.
4. Usar `emite(...)` para producir líneas de C3D.
5. Usar `tempnuevo` para crear temporales en expresiones aritméticas.
6. Usar `etiqnueva` para crear etiquetas en flujo de control.
7. Usar `id.entrada`, `num.entrada`, `num.num.entrada` y `literal.entrada` para referenciar entradas de la tabla de símbolos.
8. Para asignaciones, emitir:

```txt
u := E.lugar
```

9. Para expresiones aritméticas, crear temporales:

```txt
t := operando1 operador operando2
```

10. Para condiciones, emitir saltos:

```txt
if x oprel y goto etiqueta_verdadera
goto etiqueta_falsa
```

11. Para `while`, generar:

```txt
comienzo:
condición
verdadera:
cuerpo
goto comienzo
falsa:
```

12. Para `if-else`, generar:

```txt
condición
verdadera:
bloque_then
goto siguiente
falsa:
bloque_else
siguiente:
```

---

## 22. Recomendación de uso para un agente de IA

Cuando un agente de IA genere el esquema de traducción para una gramática propia, debe:

```txt
1. Identificar las producciones de asignación.
2. Identificar las producciones de expresiones aritméticas.
3. Identificar las producciones de condiciones booleanas.
4. Identificar las producciones de flujo de control: if, if-else y while.
5. Añadir atributos .lugar para expresiones.
6. Añadir atributos .verdadera y .falsa para condiciones.
7. Añadir atributos .siguiente y .comienzo para proposiciones.
8. Insertar acciones semánticas después de los puntos donde ya se conocen los atributos necesarios.
9. Emitir C3D con emite(...).
10. Crear temporales y etiquetas únicamente mediante tempnuevo y etiqnueva.
```

