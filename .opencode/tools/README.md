# 🛠️ Herramientas Propias de OpenCode (.opencode/tools)

En esta carpeta puedes colocar scripts y herramientas ejecutables personalizadas para tu proyecto (Node.js, Python, Bash, PowerShell).

## Cómo llamar herramientas desde el chat
- Escribe `@` en el chat para ver la lista de todas las herramientas y habilidades disponibles.
- Escribe `/` en el chat para ver la lista de comandos rápidos disponibles.

## Añadir nuevas habilidades (Skills)
Crea una carpeta dentro de `.opencode/skills/<nombre-herramienta>/` con un archivo `SKILL.md`:
```markdown
---
name: mi-tarea
description: Describe aquí qué hace tu herramienta
---
# Instrucciones de la herramienta...
```
¡Aparecerá automáticamente en el autocompletado `@mi-tarea` del chat!
