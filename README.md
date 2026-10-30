# Ya Merito

App Android para que estudiantes de economía no se les pase ninguna entrega.
Cada quien anota sus tareas con su materia (agrupadas por color), ve todo en un timeline
por fecha de entrega y recibe avisos antes de que se venza cada una. Funciona sin internet
y sin cuentas: todo se guarda en el propio teléfono.

Desarrollada por Tecnobasics.

## Descargar

Última versión: **https://github.com/beto69chico-debug/yamerito/releases/latest/download/YaMerito.apk**

1. Abre el enlace en el teléfono y descarga `YaMerito.apk`.
2. Ábrelo. Android pedirá permitir "instalar apps de origen desconocido" para el navegador o WhatsApp: acéptalo.
3. Abre Ya Merito y acepta las notificaciones. Si aparece un aviso amarillo, toca **Activar**.
4. En **Ajustes → Probar un aviso** confirma que te llega.

Las actualizaciones se instalan encima de la anterior sin perder nada.

## Cómo funciona

- **Materias:** nombre, profesor y color. Las tareas se agrupan y se filtran por materia.
- **Tareas:** qué hay que entregar, materia, fecha y hora de entrega, y notas.
- **Timeline:** las pendientes se ordenan en *Ya se te pasó*, *Hoy*, *Mañana*, *Esta semana* y *Más adelante*,
  con cuenta regresiva de color (verde → mostaza → naranja → rojo). Arriba aparece *Lo que sigue*.
- **Entregadas:** se palomean en la lista o desde la notificación con **Ya la entregué**.
- **Avisos:** de inicio 3 días, 1 día y 3 horas antes. Cada quien cambia sus "avisos de siempre" en Ajustes
  o pone avisos propios a una tarea. Si el teléfono estuvo apagado, al prender llega solo el aviso más reciente
  de cada tarea, y nunca de tareas ya vencidas.

## Compilar

Cada push a `main` compila en GitHub Actions, corre las pruebas de la lógica de avisos y publica el APK
en Releases. Para compilar localmente: abre el proyecto en Android Studio y ejecuta `assembleRelease`.
