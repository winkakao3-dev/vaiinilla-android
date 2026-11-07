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