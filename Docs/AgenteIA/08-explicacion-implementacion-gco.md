# Explicacion de la Implementacion de Generacion de Codigo Objeto

## Proposito

Este documento explica **exactamente que se toco en codigo** para la etapa de Generacion de Codigo Objeto de PaytonTK, **en que archivo**, **en que orden se ejecuta**, y **que hace cada parte**.

La idea es que sirva como apoyo para explicar la implementacion al profesor durante la entrega.

---

## Archivo que se modifico

Solo se modifico este archivo:

- `src/compilador/GenCodigoObj.java`

No se modificaron para esta entrega:

- el lexico,
- el sintactico,
- la semantica,
- ni la generacion de codigo intermedio.

Es decir, esta etapa toma como entrada los **cuadruplos** que ya produce `GenCodigoInt`.

---

## Objetivo real de lo implementado

Lo que se implemento en `GenCodigoObj.java` fue un **backend sencillo** que:

1. toma los cuadruplos del C3D,
2. identifica variables y temporales,
3. genera un programa ASM con plantilla de `Irvine32`,
4. traduce solo cuadruplos de **asignacion**,
5. y al final imprime los valores de las variables.

La implementacion esta hecha con el alcance preliminar de la practica:

- sin optimizacion,
- sin retener resultados parciales en registros,
- y traduciendo de forma directa cada cuadruplo a ensamblador.

---

## Idea general del flujo

El flujo de ejecucion dentro de `GenCodigoObj.java` quedo asi:

1. `generar()`
2. `prepararMetadata()`
3. `genEncabezadoASM()`
4. `genDeclaraVarsASM()`
5. `genSegmentoCodigo()`
6. `algoritmoGCO()`
7. `genPieASM()`
8. `emitirImpresionVariables()`

---

## 1. Constantes y atributos de clase

En la parte superior de `GenCodigoObj.java` se definieron o ajustaron estas piezas:

- `AX = "EAX"`
- `BX = "EBX"`
- `FIRMA_PREFIJO = "func("`
- `AMBITO_FUNCION = "fun"`
- `AMBITO_PARAM_PREFIJO = "param:"`

Tambien quedaron estos atributos:

- `cmp`
  Referencia al compilador principal.

- `cuadruplos`
  Lista de cuadruplos que vienen de `cmp.cua`.

- `nombresVariables`
  Conjunto de variables reales del programa.

- `nombresTemporales`
  Conjunto de temporales generados por el C3D, por ejemplo `t1`, `t2`, etc.

### Que hace esta parte

Esta seccion prepara el contexto minimo para poder emitir ASM en 32 bits con Irvine32 y para separar:

- variables del programa,
- temporales del compilador,
- y simbolos que no deben declararse como datos ejecutables, por ejemplo funciones o parametros especiales.

---

## 2. Metodo `generar()`

Este metodo es el punto de entrada de la etapa.

### Que hace

1. Si todavia no hay cuadruplos, fuerza la generacion de C3D:
   - `cmp.be.restablecer()`
   - `cmp.cua.inicializar()`
   - `cmp.gci.generar()`

2. Recupera la lista de cuadruplos:
   - `cuadruplos = cmp.cua.getCuadruplos()`

3. Llama, en orden, a:
   - `prepararMetadata()`
   - `genEncabezadoASM()`
   - `genDeclaraVarsASM()`
   - `genSegmentoCodigo()`
   - `algoritmoGCO()`
   - `genPieASM()`

### Como explicarlo

Si el profesor pregunta por este metodo, la idea es:

> `generar()` es el orquestador de la etapa. Primero asegura que ya exista codigo intermedio, luego prepara los simbolos que se van a declarar en ASM, y finalmente arma el programa ensamblador completo.

---

## 3. Metodo `prepararMetadata()`

Este metodo llena los conjuntos `nombresVariables` y `nombresTemporales`.

### Que hace

1. Limpia ambos conjuntos.
2. Recorre la tabla de simbolos.
3. Toma solo entradas con `complex = "id"`.
4. Excluye:
   - funciones,
   - simbolos con ambito de parametro,
   - firmas de funcion.
5. Agrega a `nombresVariables` solo variables reales del programa.
6. Recorre los cuadruplos y detecta temporales con `registrarTemporal(...)`.

### Por que se hizo

Porque al generar ASM necesitamos saber que declarar en la seccion `.data`.

En particular:

- las variables del usuario se declaran como `DWORD 0`,
- y los temporales del C3D tambien se declaran como `DWORD 0`.

### Metodos auxiliares involucrados

- `registrarTemporal(String operando)`
- `nombreDesdeOperando(String operando)`
- `esTemporal(String nombre)`

---

## 4. Metodo `genEncabezadoASM()`

Aqui se adapto la plantilla de salida para que sea compatible con `Irvine32`.

### Que se cambio

Antes la salida estaba pensada como ASM de 16 bits estilo DOS.  
Ahora se genera:

```asm
.386
.model flat, stdcall
.stack 4096
INCLUDE Irvine32.inc
.data
```

### Que hace

Emite:

- titulo,
- comentarios del archivo,
- modo 32 bits,
- modelo de memoria flat,
- inclusion de `Irvine32.inc`,
- y arranque de la seccion `.data`.

### Como explicarlo

> Aqui se hizo el retarget del backend: en vez de producir ASM 16-bit con `int 21h`, se adapto a un programa compatible con MASM + Irvine32.

---

## 5. Metodo `genDeclaraVarsASM()`

Este metodo genera la declaracion de datos.

### Que hace

1. Declara cada variable del programa como:

```asm
x DWORD 0
```

2. Declara cada temporal del C3D como:

```asm
t1 DWORD 0
```

3. Agrega mensajes auxiliares para imprimir resultados, por ejemplo:

```asm
msg_x BYTE "x = ",0
```

### Por que se hizo

Se necesitan tres grupos de datos:

- variables del programa,
- temporales intermedios,
- y cadenas para imprimir el nombre de cada variable al final.

### Como explicarlo

> Esta parte traduce la informacion estructural del compilador a la seccion `.data` del ensamblador.

---

## 6. Metodo `genSegmentoCodigo()`

Este metodo emite el inicio del segmento ejecutable.

### Que hace

Genera:

```asm
.code
main PROC
```

y deja un comentario donde se insertan las instrucciones generadas.

### Como explicarlo

> Esta parte abre el procedimiento principal donde luego se pegan las instrucciones correspondientes a los cuadruplos.

---

## 7. Metodo `algoritmoGCO()`

Este es el corazon de la etapa.

### Que hace

Recorre la lista de cuadruplos y decide como traducir cada uno.

#### Casos soportados

- `:=`
  Se manda a `traducirAsignacionSimple(...)`

- `+`, `-`, `*`
  Se mandan a `traducirAsignacionBinaria(...)`

#### Casos no soportados en esta entrega

Si aparece otro cuadruplo, por ejemplo de:

- `if`
- `while`
- `call`
- `return`
- `print`

entonces marca error:

```java
errorCodObj("Esta practica preliminar solo traduce sentencias de asignacion");
```

### Por que se hizo asi

Porque esta entrega se alinea al alcance preliminar de la practica: **solo sentencias de asignacion**.

### Como explicarlo

> En vez de implementar todo el universo de cuadruplos, aqui se recorto el backend al subconjunto autorizado por la practica, para que la salida sea correcta y defendible academicamente.

---

## 8. Metodo `traducirAsignacionSimple(Cuadruplo cuadruplo)`

Este metodo traduce cuadruplos del tipo:

```txt
x := y
```

### Que hace

1. Obtiene:
   - destino,
   - fuente.

2. Verifica que ambos sean operandos enteros validos.

3. Emite:

```asm
mov eax, fuente
mov destino, eax
```

### Ejemplo

Si el cuadruplo es:

```txt
x := 5
```

la salida es:

```asm
mov eax, 5
mov x, eax
```

### Como explicarlo

> Esta es la traduccion mas directa posible: se carga la fuente en `eax` y luego se copia al destino.

---

## 9. Metodo `traducirAsignacionBinaria(Cuadruplo cuadruplo)`

Este metodo traduce cuadruplos del tipo:

```txt
x := y op z
```

donde `op` puede ser:

- `+`
- `-`
- `*`

### Que hace

1. Obtiene:
   - destino,
   - operando izquierdo,
   - operando derecho.

2. Valida que todos sean enteros validos.

3. Carga operandos:

```asm
mov eax, izquierdo
mov ebx, derecho
```

4. Aplica la operacion:

- suma:

```asm
add eax, ebx
```

- resta:

```asm
sub eax, ebx
```

- multiplicacion:

```asm
imul eax, ebx
```

5. Guarda el resultado:

```asm
mov destino, eax
```

### Ejemplo

Para:

```txt
t1 := x + 1
```

la salida es:

```asm
mov eax, x
mov ebx, 1
add eax, ebx
mov t1, eax
```

### Como explicarlo

> Cada cuadruplo binario se traduce de manera local y directa: se cargan los dos operandos, se ejecuta la operacion y se guarda el resultado en su destino.

---

## 10. Metodo `emitirImpresionVariables()`

Este metodo se agrego para que el programa ensamblado no solo calcule valores, sino que tambien los **muestre en consola** con Irvine32.

### Que hace

Para cada variable declarada genera algo como:

```asm
mov edx, OFFSET msg_x
call WriteString
mov eax, x
call WriteDec
call Crlf
```

### Por que se agrego

Porque asi, al ensamblar y ejecutar el programa `.asm`, se pueden observar los resultados directamente en consola sin tener que inspeccionar memoria manualmente.

### Como explicarlo

> Esta parte no participa en el calculo, pero mejora la demostracion del resultado final del programa al usar las rutinas de salida de Irvine32.

---

## 11. Metodo `genPieASM()`

Este metodo cierra el programa ensamblador.

### Que hace

1. Llama a `emitirImpresionVariables()`.
2. Emite:

```asm
exit
main ENDP
END main
```

### Como explicarlo

> Aqui se cierra el procedimiento principal y se deja el programa listo para ser ensamblado y ejecutado con Irvine32.

---

## 12. Metodos auxiliares de resolucion de operandos

Se usan varios helpers pequenos para traducir correctamente desde la representacion interna del compilador.

### `nombreDesdeOperando(String operando)`

Convierte un operando interno a un nombre utilizable en ASM.

Ejemplos:

- `[3]` -> `x`
- `t1` -> `t1`
- `5` -> `5`

### `operandoEnteroValido(String operando)`

Verifica si el operando puede usarse en esta traduccion preliminar.

Acepta:

- inmediatos enteros,
- temporales,
- variables reales.

### `esReferenciaTS(String operando)`

Dice si el operando viene como referencia a la tabla de simbolos, por ejemplo `[7]`.

### `entradaDeReferencia(String operando)`

Extrae el numero de entrada de la TS.

### `esTemporal(String nombre)`

Reconoce temporales como `t1`, `t2`, etc.

### `esInmediato(String operando)`

Reconoce numeros enteros inmediatos.

### `esOperacionBinaria(String op)`

Reconoce las operaciones soportadas:

- `+`
- `-`
- `*`

### `esFirmaFuncion(String tipo)`

Ayuda a excluir simbolos de funciones al preparar la metadata.

---

## 13. Que no se toco

Para esta entrega no se tocaron ni rehicieron:

- `Lexico.java`
- `SintacticoSemantico.java`
- `GenCodigoInt.java`
- `Cuadruplo.java`
- `Cuadruplos.java`

Tampoco se implementaron en `GenCodigoObj`:

- `if`,
- `while`,
- `call`,
- `return`,
- `print` del lenguaje fuente,
- optimizacion,
- asignacion de registros,
- bloques basicos,
- ni descriptores.

Eso fue intencional para respetar el alcance preliminar de la practica.

---

## 14. Resumen corto para explicar al profesor

Si el profesor pregunta "¿que hicieron en codigo?", una respuesta corta y clara podria ser:

> Implementamos la etapa de Generacion de Codigo Objeto dentro de `GenCodigoObj.java`. Esta clase toma los cuadruplos del codigo intermedio, identifica variables y temporales, arma una plantilla ASM compatible con Irvine32, traduce directamente los cuadruplos de asignacion `:=`, `+`, `-` y `*` a instrucciones ensamblador, y al final imprime los valores de las variables con `WriteString`, `WriteDec` y `Crlf`. No tocamos las etapas anteriores; solo conectamos el backend final al C3D ya existente.

---

## 15. Programa fuente usado para probar

Programa fuente compatible con PaytonTK:

```txt
int x, y, z
x = 5
y = 8
x = ( 2 * ( x + 1 ) ) + y
y = 12 * x * x + 10 * y + 99
z = 2 * ( ( y + 3 ) + 12 ) * 1 + x
```

Resultado esperado al ejecutar el ASM:

```txt
x = 20
y = 4979
z = 10008
```

---

## 16. Idea clave de defensa

La defensa mas importante de esta implementacion es esta:

- **si esta conectada al compilador real**,
- **si consume los cuadruplos reales del C3D**,
- **si produce ASM ejecutable**,
- y **si deliberadamente esta recortada al subconjunto de asignaciones** para cumplir el alcance de la practica.

Ese ultimo punto es importante porque justifica por que la implementacion es simple y directa.
