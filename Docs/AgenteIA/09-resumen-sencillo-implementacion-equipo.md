# Resumen sencillo de lo que implemento el equipo

Este documento sirve como guia rapida para explicar que se trabajo en el
compilador PaytonTK despues de recibir el shell base del Ing. Gil.

La idea no es explicar cada linea de codigo, sino poder decir con claridad:

1. que venia como base,
2. que completamos nosotros,
3. como se conecta cada etapa,
4. que limitaciones tiene actualmente el compilador.

---

## 1. Que nos entrego el shell

El proyecto ya venia con la estructura general del compilador:

- una interfaz grafica en `CompiladorLib-v4.jar`,
- la clase controladora `Compilador`,
- analizador lexico,
- buffer de entrada,
- tabla de simbolos,
- manejador de errores,
- parser descendente recursivo,
- clases base para codigo intermedio, cuadruplos y codigo objeto.

En otras palabras: Gil ya habia dejado la arquitectura del compilador y los
puntos donde cada etapa debia conectarse.

Lo que faltaba era completar varias etapas con logica real, especialmente desde
semantica hasta generacion de codigo objeto.

---

## 2. Flujo general actual del compilador

El flujo actual puede explicarse asi:

```txt
Codigo fuente PaytonTK
        |
        v
Analizador lexico
        |
        v
Buffer de entrada + Tabla de simbolos
        |
        v
Analizador sintactico / semantico
        |
        v
Generacion de Codigo Intermedio C3D
        |
        v
Tabla de Cuadruplos
        |
        v
Generacion de Codigo Objeto ASM con Irvine32
```

La clase que coordina estas llamadas es `Compilador.java`.

---

## 3. `SintacticoSemantico.java`

Esta clase ya venia como el analizador sintactico basado en procedures de la
gramatica.

Lo que se trabajo fue agregar el analisis semantico encima de ese recorrido.

En terminos simples, esta clase ahora revisa cosas como:

- que las variables esten declaradas antes de usarse,
- que no se declaren identificadores repetidos,
- que las asignaciones sean compatibles por tipo,
- que las funciones tengan firma valida,
- que las llamadas a funciones tengan cantidad y tipo correcto de argumentos,
- que el valor de `return` coincida con el tipo de retorno,
- que las variables respeten su ambito,
- que las condiciones comparen tipos compatibles.

El metodo importante es:

```java
analizar(boolean analizarSemantica)
```

Cuando se llama con `false`, solo hace analisis sintactico.

Cuando se llama con `true`, ademas activa las acciones semanticas.

Asi se conserva el estilo didactico de Gil: el mismo parser descendente recursivo
puede usarse para sintaxis o para semantica.

---

## 4. `Atributos.java`

Esta clase es una bolsa sencilla de atributos.

Sirve para pasar informacion entre los procedures del parser, como se hace en
los esquemas de traduccion del Libro del Dragon.

Algunos atributos importantes son:

- `tipo`: tipo semantico de una expresion o simbolo.
- `h`: atributo heredado usado para pasar informacion hacia abajo.
- `lugar`: lugar donde queda el resultado de una expresion.
- `verdadera`: etiqueta a donde salta una condicion verdadera.
- `falsa`: etiqueta a donde salta una condicion falsa.
- `siguiente`: etiqueta de salida de una proposicion.
- `comienzo`: etiqueta inicial de un ciclo.
- `argc`: numero de argumentos en una llamada.
- `listaIds`: lista de identificadores.
- `listaTipos`: lista de tipos.

Ejemplo sencillo:

```txt
x + 1
```

La expresion genera un temporal, por ejemplo `t1`, y ese temporal se guarda en
el atributo `lugar`.

---

## 5. `GenCodigoInt.java`

Esta clase implementa la generacion de codigo intermedio.

Tambien sigue el estilo de Gil: es otro recorrido descendente recursivo sobre la
gramatica, pero ahora con acciones de traduccion.

Su trabajo principal es generar Codigo de Tres Direcciones, tambien llamado C3D.

Ejemplo:

```txt
int x, y
x = 5
y = x + 1
```

Puede producir C3D como:

```txt
[1] := [3]
t1 := [1] + [4]
[2] := t1
```

### De donde salen los lugares

Los identificadores y constantes se referencian usando su entrada en la tabla de
simbolos.

Por ejemplo:

```txt
[1]
```

significa "entrada 1 de la tabla de simbolos".

Eso lo hace el metodo `referencia(...)`.

Los temporales salen de:

```java
tempnuevo()
```

que genera:

```txt
t1, t2, t3, ...
```

Las etiquetas salen de:

```java
etiqnueva()
```

que genera:

```txt
etiq1, etiq2, etiq3, ...
```

### Separacion entre C3D y cuadruplos

Antes, el codigo intermedio y los cuadruplos se generaban al mismo tiempo.

Ahora quedo mas alineado con la idea didactica:

```txt
GenCodigoInt
  primero genera C3D textual

Cuadruplos
  despues transforma ese C3D en tabla de cuadruplos
```

El metodo `emite(...)` en `GenCodigoInt` ya solo emite C3D textual.

Al final de `generar()`, se llama:

```java
cmp.cua.generarDesdeCodigoIntermedio(codigoIntermedio);
```

Eso crea la tabla de cuadruplos a partir del C3D ya generado.

---

## 6. `Cuadruplo.java`

Esta clase representa un renglon de la tabla de cuadruplos.

Tiene cuatro campos clasicos:

```txt
op, arg1, arg2, resultado
```

Ejemplo:

```txt
t1 := [1] + [4]
```

se representa como:

```txt
op        +
arg1      [1]
arg2      [4]
resultado t1
```

Otro ejemplo:

```txt
[2] := t1
```

se representa como:

```txt
op        :=
arg1      t1
arg2
resultado [2]
```

La clase es simple porque solo modela el registro. La logica de llenar la tabla
esta en `Cuadruplos.java`.

---

## 7. `Cuadruplos.java`

Esta clase administra la lista de cuadruplos.

En el shell ya existia como estructura para guardar objetos `Cuadruplo`.

Ahora tambien tiene la responsabilidad de convertir el C3D textual a registros
de cuadruplos.

El metodo principal agregado es:

```java
generarDesdeCodigoIntermedio(ArrayList<String> codigoIntermedio)
```

Este metodo recibe lineas como:

```txt
t1 := [1] + [4]
goto etiq2
etiq1:
```

y las convierte en objetos `Cuadruplo`.

Asi se puede explicar que la tabla de cuadruplos ya no sale "pegada" al GCI,
sino como una representacion formal del codigo intermedio producido.

---

## 8. `GenCodigoObj.java`

Esta clase implementa la generacion de codigo objeto.

El shell ya traia una plantilla de ensamblador, pero el algoritmo real de
traduccion estaba vacio o incompleto.

Lo que se agrego fue un backend sencillo que:

- toma la lista de cuadruplos,
- identifica variables y temporales,
- declara variables en `.data`,
- genera un programa ASM para MASM con `Irvine32.inc`,
- traduce asignaciones simples y operaciones aritmeticas basicas.

Ejemplo de cuadruplo:

```txt
+, [1], [4], t1
```

puede traducirse a algo como:

```asm
mov eax, x
mov ebx, 1
add eax, ebx
mov t1, eax
```

Y una asignacion:

```txt
:=, t1, , [2]
```

puede traducirse a:

```asm
mov eax, t1
mov y, eax
```

### Alcance actual de GCO

Actualmente el GCO esta limitado a asignaciones enteras.

Soporta principalmente:

```txt
x := y
x := y + z
x := y * z
```

El GCO tiene codigo para `-`, pero el lenguaje aceptado por el lexico/parser
actual no reconoce `-` como operador binario de expresion. Por eso, para pruebas
completas hasta GCO, conviene usar `+` y `*`.

No se traduce completamente a ASM:

- `if`,
- `while`,
- `print`,
- llamadas a funciones,
- `float`,
- `string`.

Esas partes pueden existir en sintaxis, semantica o C3D, pero el backend de
codigo objeto se dejo reducido al alcance de la practica.

---

## 9. Que se puede decirle al profesor Gil

Una explicacion corta seria:

> Recibimos el compilador como shell con la arquitectura principal ya armada. A
> partir de ahi completamos el analisis semantico sobre el parser descendente
> recursivo, usando atributos como en el esquema de traduccion. Despues
> implementamos la generacion de codigo intermedio en C3D. Para alinear mejor la
> etapa de cuadruplos con los apuntes, dejamos primero la emision textual de C3D
> y luego convertimos ese C3D en una tabla de objetos `Cuadruplo`. Finalmente,
> implementamos un generador de codigo objeto que consume esos cuadruplos y
> produce ensamblador con Irvine32 para asignaciones enteras.

Otra forma aun mas simple:

> Gil nos dejo el esqueleto del compilador. Nosotros llenamos la semantica, el
> codigo intermedio, la tabla de cuadruplos y el codigo objeto basico.

---

## 10. Programas recomendados para demostrar

### Prueba minima

```txt
int x
x = 5
```

### Prueba con temporales

```txt
int x, y, z
x = 5
y = 8
z = 2 * ( x + 1 ) + y
```

### Prueba tipo practica

```txt
int x, y, z
x = 5
y = 8
x = ( 2 * ( x + 1 ) ) + y
y = 12 * x * x + 10 * y + 99
z = 2 * ( ( y + 3 ) + 12 ) * 1 + x
```

Valores esperados:

```txt
x = 20
y = 4979
z = 10008
```

Estas pruebas son buenas porque pasan por:

```txt
lexico -> sintactico -> semantico -> C3D -> cuadruplos -> GCO
```

---

## 11. Limitaciones importantes para no confundirse

1. El signo `-` no esta aceptado actualmente como resta binaria en expresiones.
2. El GCO esta pensado para asignaciones enteras.
3. Los temporales `t1`, `t2`, etc. se manejan como nombres internos, no como
   entradas nuevas de la tabla de simbolos.
4. La interfaz grafica viene del JAR de apoyo; nuestro trabajo esta en las
   clases del paquete `compilador`.
5. Los cuadruplos se generan a partir del C3D, no directamente desde la accion
   de emision.

