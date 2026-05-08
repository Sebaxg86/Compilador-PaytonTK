# Semántica de PaytonTK

## Objetivo

Definir la etapa de análisis semántico de PaytonTK como un comprobador estático de tipos, con reglas claras de compatibilidad, acciones semánticas y un esquema de traducción que pueda implementarse después sobre el parser descendente recursivo del proyecto.

Este documento se basa en:

- `Docs/AgenteIA/01-especificacion-lenguaje.md`
- `Docs/AgenteIA/03-gramatica.md`
- `Docs/AsignacionesMateria/En progreso/Etapa Semántica.pdf`
- `Docs/Apuntes/Apuntes-semantica.pdf`

## Alcance

La fase semántica de PaytonTK deberá validar:

- Declaración y uso correcto de identificadores.
- Compatibilidad de tipos en asignaciones.
- Compatibilidad de tipos en expresiones aritméticas.
- Compatibilidad de tipos en condiciones relacionales.
- Correcta declaración de funciones y de sus parámetros.
- Correcta invocación de funciones, verificando cantidad y tipo de argumentos.
- Compatibilidad entre el tipo declarado de retorno de una función y el valor devuelto en `return`.

Esta fase no genera todavía código intermedio ni código objeto. Su responsabilidad inmediata es aceptar o rechazar programas desde el punto de vista semántico.

## Supuestos de diseño

### Tipos visibles del lenguaje

PaytonTK soporta los siguientes tipos de dato fuente:

- `int`
- `float`
- `string`

Además existe el pseudo-tipo:

- `void`

`void` solo es válido como tipo de retorno de una función y como resultado de `return void`. No puede ser tipo de variable ni de parámetro.

### Tipos internos del analizador semántico

Para simplificar la implementación y evitar cascadas de error, el analizador manejará además dos tipos internos:

- `bool`
- `error`

`bool` es interno y representa el resultado semántico de una condición relacional. No existe como tipo declarable en PaytonTK.

`error` es un marcador interno que permite continuar el análisis cuando ya se detectó una incompatibilidad previa.

### Restricción de unicidad de identificadores

De acuerdo con la consigna de la etapa semántica:

- No se permite redeclarar un identificador con el mismo o diferente tipo.
- Todas las variables declaradas y todos los argumentos de funciones deben tener nombres únicos, incluso si conceptualmente pertenecen a ámbitos distintos.

Para mantener el diseño consistente y simple, en esta especificación se adopta la siguiente regla global:

- Todo identificador definido por el usuario debe ser único en todo el programa.

Esto aplica a:

- Variables.
- Parámetros.
- Funciones.

Con esta decisión no se permite:

- Una variable y una función con el mismo nombre.
- Un parámetro con el mismo nombre que una variable global.
- Dos parámetros con el mismo nombre, aunque pertenezcan a funciones distintas.

### Declaración antes del uso

Se adoptan estas reglas:

- Toda variable o parámetro debe estar declarado antes de usarse.
- Toda función debe tener registrada su firma antes de validar una llamada.

Como la especificación del lenguaje permite que las funciones aparezcan en cualquier orden, se recomienda una estrategia semántica de dos pasadas lógicas:

1. Pasada de encabezados: registrar nombres de funciones, tipos de retorno y lista de tipos de parámetros.
2. Pasada de comprobación: validar cuerpo, expresiones, asignaciones, llamadas y `return`.

Esta decisión permite soportar llamadas a funciones definidas después en el archivo, sin romper el diseño actual del compilador.

## Modelo semántico de la tabla de símbolos

Conceptualmente, cada entrada de la tabla de símbolos debe poder responder estas preguntas:

- ¿Qué clase de identificador es?
- ¿Qué tipo tiene?
- Si es función, ¿cuál es su firma?
- ¿En qué contexto fue declarada?

### Modelo conceptual recomendado

| Campo | Significado |
| --- | --- |
| `lexema` | Nombre del identificador o constante |
| `clase` | `variable`, `parametro`, `funcion`, `constante` |
| `tipo` | `int`, `float`, `string`, `void`, `bool`, `error` |
| `firma` | Solo para funciones, por ejemplo `func(int,float)->void` |
| `ambito` | Información auxiliar para diagnóstico |

### Recomendación práctica para la tabla actual del proyecto

La tabla de símbolos existente del proyecto hoy solo guarda:

- `complex`
- `lexema`
- `tipo`
- `ambito`

Por ello, para la implementación posterior se recomienda esta codificación:

- Variables y parámetros:
  - `tipo = int | float | string`
- Constantes:
  - `num -> int`
  - `num.num -> float`
  - `literal -> string`
- Funciones:
  - `tipo = func(T1,T2,...,Tn)->TR`

Ejemplos:

- Variable `x` de tipo entero:
  - `tipo = int`
- Parámetro `a` de tipo real perteneciente a `suma`:
  - `tipo = float`
  - `ambito = param:suma`
- Función `suma(int,float): float`
  - `tipo = func(int,float)->float`
  - `ambito = fun`

## Reglas semánticas

### 1. Tipificación de constantes

- Toda constante `num` es de tipo `int`.
- Toda constante `num.num` es de tipo `float`.
- Toda constante `literal` es de tipo `string`.

Estas entradas deben quedar tipadas al inicio de la fase semántica.

### 2. Declaración de variables

- Toda variable declarada en `DECLARACION_VARS` se registra con el tipo producido por `TIPO_DATO`.
- Si el identificador ya existe en la tabla de símbolos como variable, parámetro o función, se reporta error de redeclaración.

Ejemplos válidos:

```txt
int x
float promedio
string nombre, apellido
```

Ejemplos inválidos:

```txt
int x
float x
```

```txt
def suma(int a): int
    return a
::

int suma
```

### 3. Uso de identificadores

- Un identificador usado en una expresión, asignación, condición o llamada debe existir en la tabla de símbolos.
- Si no existe, se reporta error semántico indicando el nombre del identificador.

Ejemplo inválido:

```txt
y = 10
```

Si `y` no fue declarada previamente:

```txt
ERROR SEMANTICO: identificador 'y' no declarado
```

### 4. Asignación

En una asignación:

```txt
id = EXPRESION
```

la compatibilidad se define así:

- `int <- int` permitido.
- `float <- int` permitido.
- `float <- float` permitido.
- `string <- string` permitido.
- `int <- float` no permitido.
- `int <- string` no permitido.
- `float <- string` no permitido.
- `string <- int` no permitido.
- `string <- float` no permitido.
- `string <- literal` permitido porque `literal` es `string`.

Si el identificador del lado izquierdo corresponde a una función, la asignación es inválida.

### 5. Expresiones aritméticas

Las expresiones aritméticas manejadas por la gramática actual son las derivadas de:

- `EXPRESION`
- `TERMINO`
- `FACTOR`

Reglas:

- Los operadores de `opsuma` y `opmult` solo aceptan operandos numéricos.
- Se consideran numéricos únicamente `int` y `float`.
- `string` no participa en expresiones aritméticas.
- Una llamada a función usada como parte de una expresión toma el tipo de retorno de esa función.
- Una función `void` no puede usarse como factor dentro de una expresión.

Tabla de resultado para operadores aritméticos:

| Operando 1 | Operando 2 | Resultado |
| --- | --- | --- |
| `int` | `int` | `int` |
| `int` | `float` | `float` |
| `float` | `int` | `float` |
| `float` | `float` | `float` |
| `string` | cualquier tipo | `error` |
| cualquier tipo | `string` | `error` |

Ejemplos válidos:

```txt
int a
float b

a = 2 + 3
b = a * 0.5
b = (a + 1) * b
```

Ejemplos inválidos:

```txt
string s
int x

x = s + 1
```

```txt
def imprimir(int a): void
    return void
::

int x
x = imprimir(5)
```

### 6. Condiciones relacionales

Una condición tiene la forma:

```txt
EXPRESION oprel EXPRESION
```

Reglas:

- Se permite comparar una expresión `int` con otra `int`.
- Se permite comparar una expresión `float` con otra `float`.
- Se permite comparar una expresión `int` con una `float`.
- Se permite comparar una expresión `float` con una `int`.
- Se permite comparar una expresión `string` con otra `string`.
- No se permite comparar una expresión numérica con una `string`.

El resultado semántico interno de `CONDICION` es `bool`.

Ejemplos válidos:

```txt
if a < b :
    print(a)
else :
    print(b)
::
```

```txt
if nombre == "ana" :
    print(nombre)
else :
    print("otro")
::
```

Ejemplo inválido:

```txt
if nombre < 10 :
    print(nombre)
else :
    print("x")
::
```

### 7. Declaración de funciones

Al declarar una función:

```txt
def id ( ARGUMENTOS ) : TIPO_RETORNO
    ...
    return RESULTADO
::
```

se deben validar estas reglas:

- El nombre de la función no debe existir previamente.
- Cada parámetro debe registrarse con su tipo.
- Ningún parámetro puede repetir nombre con otro parámetro.
- Ningún parámetro puede repetir nombre con otro identificador ya declarado.
- La función debe registrar su firma completa.

Ejemplo de firma semántica:

```txt
func(int,float,string)->void
```

### 8. Llamadas a función

Para una llamada:

```txt
id ( LISTA_EXPRESIONES )
```

se verifica:

- `id` debe existir.
- `id` debe corresponder a una función.
- La cantidad de argumentos reales debe coincidir con la cantidad de argumentos formales.
- Cada argumento real debe ser compatible con el parámetro formal correspondiente.

La compatibilidad por parámetro sigue las mismas reglas de asignación:

- Parámetro `int` acepta solo `int`.
- Parámetro `float` acepta `int` o `float`.
- Parámetro `string` acepta solo `string`.

Si la llamada aparece dentro de una expresión, su tipo es el tipo de retorno de la función.

Si la función devuelve `void`, entonces:

- Puede invocarse como proposición aislada.
- No puede formar parte de una expresión.

Ejemplos válidos:

```txt
def suma(int a, float b): float
    return a + b
::

float x
x = suma(2, 3.5)
```

```txt
def imprimir(string s): void
    print(s)
    return void
::

imprimir("hola")
```

Ejemplos inválidos:

```txt
float x
x = suma("hola", 2)
```

```txt
int x
x = imprimir("hola")
```

### 9. Retorno de funciones

Reglas para `return`:

- Si la función declara retorno `void`, entonces debe devolver `void`.
- Si la función declara retorno `int`, el resultado debe ser compatible con `int`.
- Si la función declara retorno `float`, el resultado debe ser compatible con `float`.
- Si la función declara retorno `string`, el resultado debe ser compatible con `string`.

Compatibilidad de retorno:

- `return int` en función `int` es válido.
- `return int` en función `float` es válido.
- `return float` en función `int` es inválido.
- `return string` en función `string` es válido.
- `return void` en función `void` es válido.
- `return void` en función no `void` es inválido.
- `return EXPRESION` en función `void` es inválido.

### 10. Sentencia `print`

`print(EXPRESION)` es semánticamente válida si `EXPRESION.tipo` es:

- `int`
- `float`
- `string`

No es válida si `EXPRESION.tipo` es:

- `void`
- `error`

## Rutinas semánticas auxiliares

Se propone definir estas rutinas auxiliares para la implementación posterior:

```txt
esNumerico(t)
  retorna true si t ∈ {int, float}

compatibleAsign(dest, src)
  true si:
    dest == src
    o (dest == float y src == int)

compatibleRet(retEsperado, retObtenido)
  misma regla que compatibleAsign,
  excepto que void solo es compatible con void

compatibleRel(t1, t2)
  true si:
    ambos son numéricos
    o ambos son string

promoverNumerico(t1, t2)
  si alguno es error -> error
  si ambos son numéricos:
    si alguno es float -> float
    si ambos son int -> int
  en cualquier otro caso -> error

existe(id)
  true si el identificador existe en TS

esFuncion(id)
  true si la entrada del identificador corresponde a una función

tipoDe(id)
  devuelve el tipo simple del identificador

firmaDe(id)
  devuelve la firma de la función

tiposParametros(id)
  devuelve la lista de tipos formales de la función

tipoRetorno(id)
  devuelve el tipo de retorno de la función

compatibleLista(formales, actuales)
  true si ambas listas tienen la misma longitud
  y cada actual[i] es compatible con formal[i]

verificarListaIdsUnica(lista)
  true si no hay nombres repetidos dentro de la lista

insertarVariable(id, tipo)
insertarParametro(id, tipo, funcion)
insertarFuncion(id, firma)
asegurarFirmaRegistrada(id, tipos, retorno)
  confirma que la firma pre-registrada para la función coincide
  con la definición actualmente analizada
errorSem(msj)
```

## Atributos semánticos

Los atributos propuestos se alinean bien con una gramática LL(1) y con un parser descendente recursivo.

### Atributos sintetizados

| Símbolo | Atributo | Significado |
| --- | --- | --- |
| `TIPO_DATO` | `tipo` | Tipo simple producido por el token |
| `TIPO_RETORNO` | `tipo` | Tipo de retorno de la función |
| `ARGUMENTOS` | `ids` | Lista de nombres de parámetros |
| `ARGUMENTOS` | `tipos` | Lista de tipos de parámetros |
| `ARGUMENTOS'` | `ids` | Lista parcial de nombres |
| `ARGUMENTOS'` | `tipos` | Lista parcial de tipos |
| `LISTA_EXPRESIONES` | `tipos` | Lista de tipos de argumentos actuales |
| `LISTA_EXPRESIONES'` | `tipos` | Cola de tipos de argumentos |
| `RESULTADO` | `tipo` | Tipo del valor devuelto |
| `CONDICION` | `tipo` | Siempre `bool` si la comparación es válida |
| `EXPRESION` | `tipo` | Tipo final de la expresión |
| `EXPRESION'` | `tipo` | Tipo resultante acumulado |
| `TERMINO` | `tipo` | Tipo final del término |
| `TERMINO'` | `tipo` | Tipo resultante acumulado |
| `FACTOR` | `tipo` | Tipo del factor |
| `FACTOR'` | `tipo` | Tipo derivado del uso de `id` |

### Atributos heredados

| Símbolo | Atributo | Significado |
| --- | --- | --- |
| `DECLARACION_VARS'` | `tipoH` | Tipo que deben heredar los identificadores restantes |
| `PROPOSICION'` | `idH` | Identificador usado en asignación o llamada |
| `FACTOR'` | `idH` | Identificador cuyo uso se está resolviendo |
| `EXPRESION'` | `tipoH` | Tipo acumulado desde la izquierda |
| `TERMINO'` | `tipoH` | Tipo acumulado desde la izquierda |
| `RESULTADO` | `retEsperado` | Tipo declarado de retorno de la función actual |

## Acciones previas al esquema

Antes de aplicar el esquema de traducción formal se ejecutan dos pasos globales:

1. Tipificación inicial de constantes en la tabla de símbolos:
   - `num -> int`
   - `num.num -> float`
   - `literal -> string`
2. Registro previo de firmas de funciones:
   - nombre de la función
   - lista de tipos de parámetros
   - tipo de retorno

Estas acciones previas no forman parte de la numeración `{1}`, `{2}`, ... del esquema, pero son necesarias para soportar llamadas a funciones declaradas después en el programa.

## Esquema de traducción

### Convención de presentación

Para la entrega con el Ing. Gil, el esquema se presenta con la misma estructura del ejemplo de la página 32:

- Las producciones llevan marcas de acción numéricas, por ejemplo `{1}`, `{2}`, `{3}`.
- Cada número corresponde a una acción semántica en una tabla separada.
- `VACIO` representa aceptación semántica.
- `ERROR_TIPO` representa error semántico.
- Cuando una producción repite un no terminal, la ocurrencia de la derecha se identifica con sufijo `1`, `2`, etc., dentro de la tabla de acciones.
- Se permiten atributos auxiliares como `h`, `tmp`, `ret`, `listaIds` y `listaTipos`.

### Funciones auxiliares usadas

En las acciones se usarán las siguientes funciones auxiliares:

- `buscaTipo(id)` devuelve el tipo de un identificador.
- `añadeTipo(id, t)` asigna el tipo `t` a un identificador.
- `noExiste(id)` indica si el identificador aún no ha sido declarado.
- `esFuncion(id)` indica si el identificador corresponde a una función.
- `firmaValida(id, listaTipos, ret)` valida que la firma registrada para la función coincida con la definición actual.
- `registrarParametros(listaIds, listaTipos, fun)` registra los parámetros de la función actual.
- `tiposParametros(id)` devuelve la lista de tipos formales de una función.
- `tipoRetorno(id)` devuelve el tipo de retorno de una función.
- `compatibleAsign(dest, src)` valida compatibilidad de asignación.
- `compatibleRet(dest, src)` valida compatibilidad de retorno.
- `compatibleLista(formales, actuales)` valida cantidad y tipo de argumentos.
- `compatibleRel(t1, t2)` valida compatibilidad en comparaciones relacionales.
- `promover(t1, t2)` devuelve `int`, `float` o `ERROR_TIPO` para operaciones aritméticas.

### Producciones con acciones numeradas

```txt
PROGRAMA                -> INSTRUCCION PROGRAMA {1} | ϵ {2}

INSTRUCCION             -> FUNCION {3} | PROPOSICION {4}

FUNCION                 -> def id {5} ( ARGUMENTOS ) : TIPO_RETORNO {6}
                           PROPOSICIONES_OPTATIVAS return RESULTADO {7} :: {8}

ARGUMENTOS              -> TIPO_DATO id ARGUMENTOS' {9} | ϵ {10}

ARGUMENTOS'             -> , TIPO_DATO id ARGUMENTOS' {11} | ϵ {12}

DECLARACION_VARS        -> TIPO_DATO id {13} DECLARACION_VARS' {14}

DECLARACION_VARS'       -> , id {15} DECLARACION_VARS' {16} | ϵ {17}

TIPO_RETORNO            -> void {18} | TIPO_DATO {19}

TIPO_DATO               -> int {20} | float {21} | string {22}

RESULTADO               -> EXPRESION {23} | void {24}

PROPOSICIONES_OPTATIVAS -> PROPOSICION PROPOSICIONES_OPTATIVAS {25} | ϵ {26}

PROPOSICION             -> DECLARACION_VARS {27}
                        | id {28} PROPOSICION' {29}
                        | if CONDICION : PROPOSICIONES_OPTATIVAS else :
                          PROPOSICIONES_OPTATIVAS :: {30}
                        | while CONDICION : PROPOSICIONES_OPTATIVAS :: {31}
                        | print ( EXPRESION ) {32}

PROPOSICION'            -> opasig EXPRESION {33}
                        | ( LISTA_EXPRESIONES ) {34}

LISTA_EXPRESIONES       -> EXPRESION LISTA_EXPRESIONES' {35} | ϵ {36}

LISTA_EXPRESIONES'      -> , EXPRESION LISTA_EXPRESIONES' {37} | ϵ {38}

CONDICION               -> EXPRESION oprel EXPRESION {39}

EXPRESION               -> TERMINO {40} EXPRESION' {41} | literal {42}

EXPRESION'              -> opsuma TERMINO {43} EXPRESION' {44} | ϵ {45}

TERMINO                 -> FACTOR {46} TERMINO' {47}

TERMINO'                -> opmult FACTOR {48} TERMINO' {49} | ϵ {50}

FACTOR                  -> id {51} FACTOR' {52}
                        | num {53}
                        | num.num {54}
                        | ( EXPRESION ) {55}

FACTOR'                 -> ( LISTA_EXPRESIONES ) {56} | ϵ {57}
```

### Tabla de acciones semánticas

| No. Acción | Acción Semántica |
| --- | --- |
| 1 | `PROGRAMA.tipo := if INSTRUCCION.tipo == VACIO and PROGRAMA1.tipo == VACIO then VACIO else ERROR_TIPO` |
| 2 | `PROGRAMA.tipo := VACIO` |
| 3 | `INSTRUCCION.tipo := FUNCION.tipo` |
| 4 | `INSTRUCCION.tipo := PROPOSICION.tipo` |
| 5 | `funActual := id.lexema` |
| 6 | `tipoRetActual := TIPO_RETORNO.tipo ; FUNCION.tmp := if verificarListaIdsUnica(ARGUMENTOS.listaIds) and firmaValida(funActual, ARGUMENTOS.listaTipos, TIPO_RETORNO.tipo) and registrarParametros(ARGUMENTOS.listaIds, ARGUMENTOS.listaTipos, funActual) == VACIO then VACIO else ERROR_TIPO` |
| 7 | `FUNCION.ret := if compatibleRet(tipoRetActual, RESULTADO.tipo) then VACIO else ERROR_TIPO` |
| 8 | `FUNCION.tipo := if FUNCION.tmp == VACIO and PROPOSICIONES_OPTATIVAS.tipo == VACIO and FUNCION.ret == VACIO then VACIO else ERROR_TIPO` |
| 9 | `ARGUMENTOS.listaIds := [ id.lexema ] + ARGUMENTOS'1.listaIds ; ARGUMENTOS.listaTipos := [ TIPO_DATO.tipo ] + ARGUMENTOS'1.listaTipos ; ARGUMENTOS.tipo := ARGUMENTOS'1.tipo` |
| 10 | `ARGUMENTOS.listaIds := [ ] ; ARGUMENTOS.listaTipos := [ ] ; ARGUMENTOS.tipo := VACIO` |
| 11 | `ARGUMENTOS'.listaIds := [ id.lexema ] + ARGUMENTOS'1.listaIds ; ARGUMENTOS'.listaTipos := [ TIPO_DATO.tipo ] + ARGUMENTOS'1.listaTipos ; ARGUMENTOS'.tipo := ARGUMENTOS'1.tipo` |
| 12 | `ARGUMENTOS'.listaIds := [ ] ; ARGUMENTOS'.listaTipos := [ ] ; ARGUMENTOS'.tipo := VACIO` |
| 13 | `DECLARACION_VARS'.h := TIPO_DATO.tipo ; DECLARACION_VARS.tmp := if noExiste(id.lexema) then begin añadeTipo(id.lexema, TIPO_DATO.tipo); VACIO end else ERROR_TIPO` |
| 14 | `DECLARACION_VARS.tipo := if DECLARACION_VARS.tmp == VACIO and DECLARACION_VARS'.tipo == VACIO then VACIO else ERROR_TIPO` |
| 15 | `DECLARACION_VARS'1.h := DECLARACION_VARS'.h ; DECLARACION_VARS'.tmp := if noExiste(id.lexema) then begin añadeTipo(id.lexema, DECLARACION_VARS'.h); VACIO end else ERROR_TIPO` |
| 16 | `DECLARACION_VARS'.tipo := if DECLARACION_VARS'.tmp == VACIO and DECLARACION_VARS'1.tipo == VACIO then VACIO else ERROR_TIPO` |
| 17 | `DECLARACION_VARS'.tipo := VACIO` |
| 18 | `TIPO_RETORNO.tipo := void` |
| 19 | `TIPO_RETORNO.tipo := TIPO_DATO.tipo` |
| 20 | `TIPO_DATO.tipo := int` |
| 21 | `TIPO_DATO.tipo := float` |
| 22 | `TIPO_DATO.tipo := string` |
| 23 | `RESULTADO.tipo := EXPRESION.tipo` |
| 24 | `RESULTADO.tipo := void` |
| 25 | `PROPOSICIONES_OPTATIVAS.tipo := if PROPOSICION.tipo == VACIO and PROPOSICIONES_OPTATIVAS1.tipo == VACIO then VACIO else ERROR_TIPO` |
| 26 | `PROPOSICIONES_OPTATIVAS.tipo := VACIO` |
| 27 | `PROPOSICION.tipo := DECLARACION_VARS.tipo` |
| 28 | `PROPOSICION'.h := id.lexema` |
| 29 | `PROPOSICION.tipo := PROPOSICION'.tipo` |
| 30 | `PROPOSICION.tipo := if CONDICION.tipo == bool and PROPOSICIONES_OPTATIVAS1.tipo == VACIO and PROPOSICIONES_OPTATIVAS2.tipo == VACIO then VACIO else ERROR_TIPO` |
| 31 | `PROPOSICION.tipo := if CONDICION.tipo == bool and PROPOSICIONES_OPTATIVAS.tipo == VACIO then VACIO else ERROR_TIPO` |
| 32 | `PROPOSICION.tipo := if EXPRESION.tipo != void and EXPRESION.tipo != ERROR_TIPO then VACIO else ERROR_TIPO` |
| 33 | `PROPOSICION'.tipo := if noExiste(PROPOSICION'.h) then ERROR_TIPO else if esFuncion(PROPOSICION'.h) then ERROR_TIPO else if compatibleAsign(buscaTipo(PROPOSICION'.h), EXPRESION.tipo) then VACIO else ERROR_TIPO` |
| 34 | `PROPOSICION'.tipo := if noExiste(PROPOSICION'.h) then ERROR_TIPO else if !esFuncion(PROPOSICION'.h) then ERROR_TIPO else if compatibleLista(tiposParametros(PROPOSICION'.h), LISTA_EXPRESIONES.tipos) then VACIO else ERROR_TIPO` |
| 35 | `LISTA_EXPRESIONES.tipos := [ EXPRESION.tipo ] + LISTA_EXPRESIONES'1.tipos ; LISTA_EXPRESIONES.tipo := if EXPRESION.tipo != void and LISTA_EXPRESIONES'1.tipo == VACIO then VACIO else ERROR_TIPO` |
| 36 | `LISTA_EXPRESIONES.tipos := [ ] ; LISTA_EXPRESIONES.tipo := VACIO` |
| 37 | `LISTA_EXPRESIONES'.tipos := [ EXPRESION.tipo ] + LISTA_EXPRESIONES'1.tipos ; LISTA_EXPRESIONES'.tipo := if EXPRESION.tipo != void and LISTA_EXPRESIONES'1.tipo == VACIO then VACIO else ERROR_TIPO` |
| 38 | `LISTA_EXPRESIONES'.tipos := [ ] ; LISTA_EXPRESIONES'.tipo := VACIO` |
| 39 | `CONDICION.tipo := if compatibleRel(EXPRESION1.tipo, EXPRESION2.tipo) then bool else ERROR_TIPO` |
| 40 | `EXPRESION'.h := TERMINO.tipo` |
| 41 | `EXPRESION.tipo := EXPRESION'.tipo` |
| 42 | `EXPRESION.tipo := string` |
| 43 | `EXPRESION'1.h := promover(EXPRESION'.h, TERMINO.tipo)` |
| 44 | `EXPRESION'.tipo := EXPRESION'1.tipo` |
| 45 | `EXPRESION'.tipo := EXPRESION'.h` |
| 46 | `TERMINO'.h := FACTOR.tipo` |
| 47 | `TERMINO.tipo := TERMINO'.tipo` |
| 48 | `TERMINO'1.h := promover(TERMINO'.h, FACTOR.tipo)` |
| 49 | `TERMINO'.tipo := TERMINO'1.tipo` |
| 50 | `TERMINO'.tipo := TERMINO'.h` |
| 51 | `FACTOR'.h := id.lexema` |
| 52 | `FACTOR.tipo := FACTOR'.tipo` |
| 53 | `FACTOR.tipo := int` |
| 54 | `FACTOR.tipo := float` |
| 55 | `FACTOR.tipo := EXPRESION.tipo` |
| 56 | `FACTOR'.tipo := if noExiste(FACTOR'.h) then ERROR_TIPO else if !esFuncion(FACTOR'.h) then ERROR_TIPO else if !compatibleLista(tiposParametros(FACTOR'.h), LISTA_EXPRESIONES.tipos) then ERROR_TIPO else if tipoRetorno(FACTOR'.h) == void then ERROR_TIPO else tipoRetorno(FACTOR'.h)` |
| 57 | `FACTOR'.tipo := if noExiste(FACTOR'.h) then ERROR_TIPO else if esFuncion(FACTOR'.h) then ERROR_TIPO else buscaTipo(FACTOR'.h)` |

## Errores semánticos esperados

El analizador semántico debe ser capaz de detectar, como mínimo, estos casos:

- Identificador no declarado.
- Redeclaración de identificador.
- Asignación incompatible.
- Uso de `string` dentro de expresiones aritméticas.
- Comparación incompatible en `if` o `while`.
- Llamada a identificador que no es función.
- Cantidad incorrecta de argumentos.
- Tipos incompatibles en argumentos.
- Uso de una función `void` dentro de una expresión.
- `return` incompatible con el tipo declarado de la función.

## Ejemplos guía

### Válido

```txt
int a
float b

def suma(int x, float y): float
    return x + y
::

b = suma(a, 3.5)
```

### Inválido por redeclaración

```txt
int a
string a
```

### Inválido por variable no declarada

```txt
x = 10
```

### Inválido por llamada incompatible

```txt
def suma(int a, int b): int
    return a + b
::

int x
x = suma("hola", 2)
```

### Inválido por retorno incorrecto

```txt
def imprimir(int a): void
    return a
::
```

## Criterio de aceptación para la implementación posterior

La implementación de la fase semántica se considerará alineada con este documento cuando:

- Tipifique correctamente constantes, variables, parámetros y funciones.
- Detecte uso de identificadores no declarados.
- Detecte redeclaraciones.
- Aplique correctamente la promoción `int -> float`.
- Rechace expresiones aritméticas con `string`.
- Verifique cantidad y tipos de argumentos en llamadas.
- Use correctamente el tipo de retorno de funciones dentro de expresiones.
- Rechace el uso de funciones `void` dentro de expresiones.
- Verifique correctamente el `return` final de cada función.
