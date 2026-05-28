# SITUACION DIDACTICA

El equipo de desarrollo de **TecLag Software** ha logrado un hito más en la ruta a construir el compilador de lenguaje **PaytonTK**. Ahora ya cuenta con un generador de código intermedio que produce sentencias de 3 direcciones equivalentes al programa fuente. Además se tiene la representación en Cuadruplos del código intermedio producido por lo que ya se está en condiciones para enfrentar el reto de desarrollar la siguiente y última etapa que será la Generación de Código Objeto.

El código objeto a generar será lenguaje Ensamblador, el cual será ensamblado con Microsoft Assembler (MASM v5.10).

Como un primer paso para enfrentar este desafiante reto la Junta Directiva quiere ver un resultado en corto tiempo por lo que autoriza que se traduzcan a ensamblador únicamente las sentencias de **asignación**, por tanto en este paso quedan excluidas las sentencias if-else, while así como las llamadas a rutinas y funciones.

El código objeto será código en lenguaje Ensamblador para procesadores Intel, el cual tomará en cuenta lo siguiente:

1. El ensamblador será para código de 16 bits.

2. Las variables serán declaradas como enteros de 16 bits sin signo con el tipo DW e inicializadas en cero.

3. Los registros serán manejados a 16 bits AX, BX, CX y DX.

Para esta etapa se debe implementar el algoritmo de Generación de Código Objeto dentro de la clase Java nombrada como **GenCodigoObj**. Esta clase debe producir un programa en ensamblador bien formado, usando la siguiente plantilla:

```asm
TITLE CodigoObjeto ( codigoObjeto.asm )
; Descripción del programa: Automatas II
; Fecha de creacion: Ene-Jun/2023
; Revisiones:
; Fecha de ult. modificacion:

; INCLUDE Irvine32.inc
; (aqui se insertan las definiciones de simbolos)

.model small
.stack 4096h
.data
    ; (aqui se insertan las variables)

.code
main PROC
    mov ax, @Data
    mov ds, ax

    ; (aqui se insertan las instrucciones ejecutables)

    mov ax,4c00h
    int 21h
main ENDP

; (aqui se insertan los procedimientos adicionales)
END main
```

Para poder presentar un resultado preliminar en corto tiempo como lo pide la Directiva se traducirá cada cuádruplo a su equivalente en sentencias ensamblador sin retener en registros los resultados parciales ni optimizar las operaciones.

---

# EL DESARROLLO DE LA PRACTICA DEBE INCLUIR:

## Sección de **ANÁLISIS**

1. Presentar las instrucciones en ensamblador que se generarán para cada tipo de sentencia de 3 direcciones de asignación siguientes:

    a) `x := y`

    b) `x := y + z`

    c) `x := y * z`

## Sección de **Diseño**

1. Dar el diseño UML de la clase GenCodigoObj.

## Sección **Código**

1. Pegar el código de las clases Cuadruplo, Cuadruplos y GenCodigoObj.

## Sección de **Prueba de Ejecución**

1. Usando el programa de prueba proporcionado enseguida se deberá obtener el código objeto con el compilador de **PaytonTK** y adjuntar las capturas de pantalla siguientes:

    a) Pestaña del código fuente del programa **PaytonTK**.

    b) Pestaña del código intermedio.

    c) Pestaña de cuádruplos.

    d) Pestaña del código objeto generado  
    (como el código objeto puede ser extenso solo capturar la parte inicial del programa y enseguida pegar el código ensamblador completo que se generó).

    e) Ejecución de modo depuración con DEBUG.EXE o DEX.EXE presentando solo capturas donde se aprecien las direcciones de memoria de las variables y el valor que tomaron al terminar la ejecución.

---

# Caso de Prueba #1

## Programa de entrada:

```txt
x = 5
y = 8
x = (2*(x+1))+y
y = 12*x*x+10*y+99
z = 2*((y+3)+12)*1+x
```

## Resultados esperados:

Al final de la ejecución las variables deben tener los siguientes valores:

```txt
x:    20d

y:    4,979d

z:    10,008d
```
