Este repo se trabaja bajo el Senado de Vaiinilla: lee `$SENADO_DIR/Vaiinilla/Senado/PROTOCOLO.md` (`SENADO_DIR` es la raíz de la bóveda de Obsidian; los hooks de `.claude/settings.json` lo necesitan).
Al inicio de cada turno aplica la regla 1: lee `REGLAS.md`, `DECISIONES.md`, `PREGUNTAS-KAO.md` y el resumen de `ESTADO.md`, y escribe 5 líneas de "dónde vamos" antes de decidir nada.
Todo commit lleva los ids que toca: `fix(android): COMPRA-06, PEDIDO-11 — …`.
Nada se pushea a `main` ni a `develop` sin `aprobaciones/APROBADO-<rama>` creado por Kao; nunca `push --force`.
Lo que no puedas decidir va a `PREGUNTAS-KAO.md`, y sigues con lo que no depende de eso.
