# Cómo probar Turbo Log Companion en vivo

1. Cuando abra el sandbox (`./gradlew runIde`), abrí este archivo:
   `demo/src/main/java/com/example/demo/OrderService.java`
2. Poné el cursor sobre la variable `subtotal` (dentro de
   `calculateTotal`, línea `double total = subtotal * (1 + taxRate);`).
3. Click derecho → buscá **"Insert Log Statement"** en el menú
   contextual (o `Ctrl+Shift+A` → Find Action → escribí "Insert Log
   Statement").
4. Debería insertarse una línea nueva justo después, algo como:
   `System.out.println("TCLC OrderService.calculateTotal:12 subtotal = " + subtotal);`
5. Repetí sobre `total`, `retryCount` (en `retry()`), y `status` en
   `processOrder` (método estático).
6. Probá lo mismo en el archivo Kotlin:
   `demo/src/main/kotlin/com/example/demo/OrderService.kt` — cursor
   sobre `subtotal` o `status`.
7. Una vez insertados 2-3 statements, click derecho de nuevo → **"Remove
   All Log Statements"** → deberían desaparecer todos los que insertó
   el plugin (y solo esos, no otro código).

## Qué reportar

- ¿Se insertó el statement en el lugar correcto (después de la línea,
  no antes/en medio)?
- ¿El texto generado tiene sentido (nombre de clase/método/línea
  correctos)?
- ¿"Remove All" borró exactamente lo que insertó el plugin, sin tocar
  nada más?
- ¿Algo tiró una excepción visible (notification roja, popup de
  error)?
