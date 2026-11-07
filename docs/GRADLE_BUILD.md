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