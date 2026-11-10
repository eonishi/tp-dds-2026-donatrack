# Sistema de diseño e inventario de vistas

Guía visual de la web de DonaTrack (entrega 4) e inventario de todas las pantallas implementadas,
con capturas reales. Las capturas se sacaron con datos de ejemplo, en escritorio (1280 px) y celular
(390 px).

## 1. Principios

1. **Una tarea por pantalla.** Cada vista responde a un requerimiento del enunciado 4 y tiene un
   solo objetivo principal, con su botón primario.
2. **El estado se entiende sin leer el detalle.** Los chips de color (En camino, Entregada,
   Vencida, Nuevo) cuentan qué pasa antes que el texto.
3. **Nunca un callejón sin salida.** Las listas vacías explican qué falta y cómo seguir. Los errores
   aparecen en la misma vista, junto al dato que los causó, sin perder lo que se había cargado. Las
   páginas de error tienen una salida al inicio o al panel.
4. **Privacidad por defecto.** Lo público (entregas y mapa) nunca muestra datos de las personas
   donantes.
5. **Cliente liviano.** HTML del servidor y formularios. JavaScript solo para los mapas.

## 2. Fundamentos

### Color
| Token | Valor | Uso |
|---|---|---|
| `--verde` | `#2b8a3e` | Acción principal, enlaces y foco. Sale del logo. |
| `--verde-oscuro` | `#1f6b2f` | Hover y texto de los chips. |
| `--verde-claro` | `#e9f6ec` | Fondo de chips y datos destacados. |
| `--tinta` | `#1b2a1f` | Texto principal: casi negro, con un toque verde. |
| `--degrade` | `#1f8a3e → #18a37a → #7cc95a` | Palabra destacada de los títulos de la portada. |
| Bordes | `#e6ebe6` | Tarjetas y separadores. |

Los colores de estado usan los de Bootstrap, para que el significado sea el habitual:

| Estado | Clase | Ejemplos |
|---|---|---|
| Éxito o terminado | `text-bg-success` | Entregada, Cubierta, Nuevo, Terminada |
| En curso | `text-bg-primary` | En camino, Importando |
| Intermedio | `text-bg-info` | Asignada, Lista para entregar |
| Problema | `text-bg-danger` | Vencida, Entrega con problemas, Falló |
| Neutro | `text-bg-secondary` | En depósito |

### Tipografía
* **Outfit** (600–700) para títulos (`h1`–`h3`) y la marca.
* **Inter** (400–600) para el cuerpo, los formularios y las tablas.
* Escala: título de página `fs-2`, título de sección `h4`, título de tarjeta `h5`/`h6`, texto
  secundario `small text-body-secondary`.

### Forma y espacio
* Radio base de 0.75 rem en controles. Las tarjetas usan 1.5 rem y los mapas 1.25 rem. Botones y
  chips son píldoras (`rounded-pill`).
* La grilla es la de Bootstrap 5.3: una columna en celular, y dos o tres tarjetas por fila desde
  `md` y `xl`.
* Espaciado vertical: `py-4` por página y `g-4` entre tarjetas.

## 3. Componentes

| Componente | Clase o plantilla | Uso |
|---|---|---|
| Layout | `layout.hbs` | Navbar, contenido, aviso de éxito (`flash`) y pie. Bloques `titulo`, `contenido`, `head` y `scripts`. |
| Navbar por rol | `layout.hbs` | Público: secciones de la portada e Ingresar. Donante, entidad y admin: solo sus secciones; el admin las agrupa en menús desplegables. |
| Encabezado de página | `.encabezado` + `.chip` | Rol o contexto en el chip, título y una línea que explica para qué sirve la pantalla. |
| Tarjeta | `.tarjeta` | Unidad de contenido: una donación, una necesidad, un camión, un formulario. |
| Chip | `.chip`, `.badge.rounded-pill` | Contexto (rol) y estado. |
| Botón primario | `.btn.btn-primary.rounded-pill` | Una sola acción principal por tarjeta o formulario. |
| Acción destructiva | `.btn-outline-danger`, `.btn-link.text-danger` | Eliminar o marcar vencida. Nunca es la acción por defecto. |
| Error de formulario | `.alert.alert-danger` | Dentro de la tarjeta del dato que lo causó. La respuesta es 400 y los datos cargados se conservan. |
| Aviso de éxito | `flash` en el layout | Después de un POST que salió bien (Post/Redirect/Get). |
| Estado vacío | `.tarjeta.text-center` | Dice qué falta y qué va a pasar. |
| Progreso | `.progress` | Cuánto se recibió de una necesidad. |
| Mapa | `.mapa` + `data-mapa` (`mapas.js`) | Leaflet + OpenStreetMap. Marcadores 🎁 (entregas), 🚚 (camión) y 🏠 (destino). Debajo, un aviso accesible (`aria-live`) con la última actualización. |
| Aviso de bandeja | `.aviso-nuevo` | Borde verde en los avisos que no se habían leído. |

### Accesibilidad
* Enlace "Saltar al contenido", `lang="es"` y foco visible en verde.
* Cada campo tiene su `label`. Los grupos de opciones usan `fieldset` y `legend`.
* Los mapas tienen `role="region"` y `aria-label`, y su información también aparece como lista en la
  misma página: no hace falta ver el mapa para saber dónde está la donación.
* Los colores de estado siempre van con texto, nunca solos.

## 4. Inventario de vistas

| # | Vista | Ruta | Rol | Plantilla | Issue |
|---|---|---|---|---|---|
| 1 | Portada | `/` | Público | `index.hbs` | Enunciado 4.1 |
| 2 | Entregas en mapa | `/entregas` | Público | `entregas.hbs` | #43 |
| 3 | Registro | `/registro` | Público | `registro.hbs` | Enunciado 4.3 |
| 4 | Login | `/login` | Público | `login.hbs` | #44 |
| 5 | Activar cuenta importada | `/activar` | Público | `activar.hbs` | #60 |
| 6 | Legal y privacidad | `/legal` | Público | `legal.hbs` | Enunciado 4.1 |
| 7 | Panel | `/dashboard` | Todos | `dashboard.hbs` | #44 |
| 8 | Mis donaciones con filtros | `/donante/donaciones` | Donante | `donante-donaciones.hbs` | #46 |
| 9 | Seguimiento de mis donaciones | `/donante/seguimiento` | Donante | `seguimiento.hbs` | #50 |
| 10 | Avisos | `/donante/notificaciones` | Donante | `notificaciones.hbs` | #49 |
| 11 | Necesidades | `/entidad/necesidades` | Entidad | `entidad-necesidades.hbs` | #51 |
| 12 | Recibir donaciones con fotos | `/entidad/recepciones` | Entidad | `entidad-recepciones.hbs` | #52, #53 |
| 13 | Entregas que voy a recibir | `/entidad/seguimiento` | Entidad | `seguimiento.hbs` | #55 |
| 14 | Avisos | `/entidad/notificaciones` | Entidad | `notificaciones.hbs` | #54 |
| 15 | Registrar donaciones | `/donaciones` | Admin | `panel.hbs` | #56 |
| 16 | Vencidas en depósito | `/admin/vencidas` | Admin | `admin-vencidas.hbs` | #57 |
| 17 | Asignar entidad final | `/admin/asignaciones` | Admin | `admin-asignaciones.hbs` | #58 |
| 18 | Camiones | `/admin/camiones` | Admin | `admin-camiones.hbs` | #59 |
| 19 | Rutas | `/logistica` | Admin | `panel.hbs` | — |
| 20 | Importar donantes | `/admin/importar` | Admin | `admin-importar.hbs` | #60 |
| 21 | Catálogo | `/catalogo` | Admin | `panel.hbs` | — |
| 22 | Páginas de error | cualquiera | Todos | `error.hbs` | MVC |

### Público

**Entregas en mapa (#43).** Mapa con un marcador por entidad: al tocarlo se ven las entregas, sus
bienes y sus fotos. Debajo está la lista completa.

![Entregas en mapa](assets/vistas/publica-entregas.webp)

**Login (#44) y activación de cuentas importadas (#60).**

![Login](assets/vistas/publica-login.webp)
![Activar cuenta](assets/vistas/publica-activar.webp)

### Persona donante

![Panel de la donante](assets/vistas/donante-panel.webp)

**Mis donaciones (#46).** Filtros por estado, categoría y una o varias subcategorías. Cada tarjeta
muestra el estado, los bienes, la entidad y las fotos de la entrega, y un acceso al mapa si está en
camino.

![Mis donaciones](assets/vistas/donante-donaciones.webp)

**Seguimiento (#50).** Camión 🚚, destino 🏠 y hora de la última ubicación. El mapa se actualiza
cada 30 segundos.

![Seguimiento de la donante](assets/vistas/donante-seguimiento.webp)

**Avisos (#49).**

![Avisos de la donante](assets/vistas/donante-avisos.webp)

### Entidad beneficiaria

![Panel de la entidad](assets/vistas/entidad-panel.webp)

**Necesidades (#51).** Alta de necesidades por única vez o recurrentes, con el progreso de lo que
se recibió.

![Necesidades](assets/vistas/entidad-necesidades.webp)

**Recibir donaciones (#52, #53).** Lo que está en camino, la confirmación con fotos o el reporte de
un problema, y lo ya recibido.

![Recibir donaciones](assets/vistas/entidad-recepciones.webp)

**Seguimiento (#55) y avisos (#54).**

![Seguimiento de la entidad](assets/vistas/entidad-seguimiento.webp)
![Avisos de la entidad](assets/vistas/entidad-avisos.webp)

### Administración

![Panel de la administración](assets/vistas/admin-panel.webp)

**Vencidas en depósito (#57).** Las vencidas aparecen primero y en rojo. Se marcan de a una o todas
juntas.

![Vencidas](assets/vistas/admin-vencidas.webp)

**Asignar entidad final (#58).** Candidatas de los algoritmos con su puntaje. La mejor viene elegida.

![Asignaciones](assets/vistas/admin-asignaciones.webp)

**Camiones (#59).** Alta, edición y baja, operadas en logística.

![Camiones](assets/vistas/admin-camiones.webp)

**Importar donantes (#60).** Progreso de cada importación, conteos y errores por fila.

![Importar donantes](assets/vistas/admin-importar.webp)

### Errores y celular

![Error 404](assets/vistas/error-404.webp)

<p>
<img src="assets/vistas/celular-donante-seguimiento.webp" alt="Seguimiento en el celular" width="300">
<img src="assets/vistas/celular-entidad-necesidades.webp" alt="Necesidades en el celular" width="300">
</p>
