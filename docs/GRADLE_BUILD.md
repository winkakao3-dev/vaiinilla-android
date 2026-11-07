# Compilar con una sola Gradle a la vez

Esta Mac tiene 8 núcleos y 8 GB de RAM. Varias Gradle a la vez (una por worktree) saturan el swap y todo tarda varias veces más.

## Uso

```bash
scripts/gradle-build.sh :app:assembleDevCajaDebug
```

Corre `./gradlew` en la carpeta actual con los argumentos que le pases. También sirve `vbuild`, que usa el mismo candado.

## Reglas

- **Una sola Gradle a la vez.** Ambos scripts toman el candado `/tmp/vaiinilla-gradle.lock` (carpeta con `pid` y `dir`). Si otra Gradle lo tiene, esperan; si el dueño murió, el candado se limpia solo.
- **JDK 17:** `/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`.
- **`GRADLE_USER_HOME` compartido:** `/Volumes/KINGSTON1TB/Dev/gradle` (el mismo que `~/.gradle`). Así todos los worktrees comparten la caché de compilación.
- **Sin `nice` y sin flags de memoria.** Heap (2 GB Gradle, 1.5 GB Kotlin) y los 8 workers están en `~/.gradle/gradle.properties`. Pasar `-Dorg.gradle.jvmargs` levanta un segundo daemon.
- **Un APK por comando.** Nunca dos sabores juntos.
## Cola de compilaciones

Además del candado, las compilaciones siguen un orden fijado en `~/.cache/gradle-queue/cola.txt`, una línea por worktree: `<posición> | <nombre> | <ruta del worktree> | <rama>`.

- `scripts/gradle-build.sh` y `vbuild` solo arrancan si el worktree actual es el primer pendiente de la cola. Si no es su turno, dicen a quién le toca y esperan.
- Al terminar con éxito marcan su línea con ` | HECHA`.
- Las líneas con ruta `?` se saltan. Un worktree que no está en la cola, o una cola vacía, no espera turno (el candado sigue valiendo).
- `GRADLE_QUEUE_BYPASS=1` salta la cola.
- Para pruebas de los scripts: `GRADLE_QUEUE_FILE` cambia el archivo de la cola y `GRADLE_LOCK_DIR` cambia el candado.
### Liberación automática

Una entrada pendiente se marca ` | HECHA` sola cuando quien espera su turno detecta que:
- su rama ya entró a `origin/main` (y no es la misma punta que `main`), o
- su worktree ya no existe, o
- lleva más de 10 min sin ningún proceso vivo con esa carpeta como directorio de trabajo (gradlew, `gradle-build.sh`, claude, etc.). El tiempo se mide con marcas en `~/.cache/gradle-queue/vivo/`.

No se mata ningún proceso. Las ramas con cabeza suelta (`(detached ...)`) solo se liberan por inactividad.
## Daemons al terminar

- Al terminar, `gradle-build.sh` y `vbuild` ejecutan `./gradlew --stop`, pero solo si no hay otra `gradlew`, `gradle-build.sh` o `vbuild` viva y ningún worktree con entrada pendiente en la cola tiene procesos. Es best-effort: nunca hace fallar el build. `GRADLE_KEEP_DAEMON=1` lo desactiva.
- `org.gradle.daemon.idletimeout` en `~/.gradle/gradle.properties` baja de 900000 a 300000 (5 min). Los daemons ociosos retenían ~5 GB de los 8 GB de la Mac.

El umbral de inactividad es de 10 min por omisión y se cambia con `GRADLE_QUEUE_IDLE_MIN` (minutos).