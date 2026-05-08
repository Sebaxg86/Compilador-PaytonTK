# GRAMATICA LIBRE DE CONTEXTO PARA LENGUAJE “PaytonTK”

```

PROGRAMA                    -> INSTRUCCION PROGRAMA | ϵ

INSTRUCCION                 -> FUNCION | PROPOSICION

FUNCION                     -> def id ( ARGUMENTOS ) : TIPO_RETORNO PROPOSICIONES_OPTATIVAS return RESULTADO ::

ARGUMENTOS                  -> TIPO_DATO id ARGUMENTOS' | ϵ

ARGUMENTOS'                 -> , TIPO_DATO id ARGUMENTOS' | ϵ

DECLARACION_VARS            -> TIPO_DATO id DECLARACION_VARS'

DECLARACION_VARS'           -> , id DECLARACION_VARS' | ϵ

TIPO_RETORNO                -> void | TIPO_DATO

TIPO_DATO                   -> int | float | string

RESULTADO                   -> EXPRESION | void

PROPOSICIONES_OPTATIVAS     -> PROPOSICION PROPOSICIONES_OPTATIVAS | ϵ

PROPOSICION                 -> DECLARACION_VARS | id PROPOSICION' | if CONDICION : PROPOSICIONES_OPTATIVAS else : PROPOSICIONES_OPTATIVAS :: | while CONDICION : PROPOSICIONES_OPTATIVAS :: | print ( EXPRESION )

PROPOSICION'                -> opasig EXPRESION | ( LISTA_EXPRESIONES )

LISTA_EXPRESIONES           -> EXPRESION LISTA_EXPRESIONES' | ϵ

LISTA_EXPRESIONES'          -> , EXPRESION LISTA_EXPRESIONES' | ϵ

CONDICION                   -> EXPRESION oprel EXPRESION

EXPRESION                   -> TERMINO EXPRESION' | literal

EXPRESION'                  -> opsuma TERMINO EXPRESION' | ϵ

TERMINO                     -> FACTOR TERMINO'

TERMINO'                    -> opmult FACTOR TERMINO' | ϵ

FACTOR                      -> id FACTOR' | num | num.num | ( EXPRESION )

FACTOR'                     -> ( LISTA_EXPRESIONES ) | ϵ
```
