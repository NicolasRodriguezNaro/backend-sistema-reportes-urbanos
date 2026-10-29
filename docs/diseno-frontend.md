# Brief de diseño e implementación del frontend

**Proyecto:** Plataforma Inteligente de Gestión de Incidencias Urbanas
**Productos:** dashboard web + app móvil para ciudadanos
**Estado:** guía de diseño e implementación
**Última actualización:** octubre de 2026
**Documentos relacionados:**

- `docs/api-frontend.md` — contrato de endpoints, tipos y cliente HTTP.
- `docs/dashboard-api.md` — detalle técnico de la API del dashboard.
- `docs/seguridad.md` — roles, autenticación y alcance por entidad.

---

## 1. Cómo usar este documento

Este archivo es la **guía completa de diseño y de implementación** del frontend.
No está pensado para pegarse completo en Google Stitch.

Hay tres artefactos distintos:

| Artefacto | Para qué sirve | ¿Se pega en Stitch? |
|---|---|---|
| Este brief | Definir pantallas, reglas, componentes y criterios de aceptación | No |
| Prompt base | Dar contexto visual y restricciones generales | Sí, una vez por proyecto |
| Prompt por pantalla | Generar una pantalla concreta | Sí, una pantalla a la vez |

La regla principal para Stitch es:

> **Una pantalla o un flujo pequeño por prompt. No pedir todo el producto de una vez.**

El brief sirve para revisar que el diseño generado no invente funciones, campos o flujos que el backend no tiene.

---

## 2. Objetivo del producto

La plataforma permite:

- reportar incidencias urbanas desde una app móvil;
- subir una foto y asociar una ubicación;
- clasificar la incidencia con IA;
- asignar una entidad responsable;
- consultar y gestionar los reportes desde un dashboard web;
- visualizar los reportes, barrios y sitios de interés en un mapa.

### Productos

1. **Dashboard web**
   - Para `ADMINISTRADOR`, `FUNCIONARIO` y `EMPRESA`.
   - Orientado a escritorio, con adaptación a tablet.
   - Prioriza tablas, filtros, métricas y mapas.

2. **App móvil**
   - Para `CIUDADANO`.
   - Orientada a reportar rápido y consultar reportes propios.
   - No incluye el dashboard administrativo.

### Fuera de alcance de esta fase

- Notificaciones push.
- Historial completo de cambios de estado.
- Chat entre ciudadano y entidad.
- Panel para crear usuarios o roles.
- Recuperación de contraseña.
- Módulo de reportes públicos en tiempo real.
- Firma digital o adjuntos múltiples por reporte.

No diseñar estas funciones como si ya existieran. El backend actual no las expone.

---

## 3. Roles y permisos

| Rol | Dashboard web | App móvil | Alcance | Acciones |
|---|---|---|---|---|
| `ADMINISTRADOR` | Sí | Opcional | Todos los reportes | Cambiar estado y asignar entidad |
| `FUNCIONARIO` | Sí | No | Reportes de su entidad | Cambiar estado |
| `EMPRESA` | Sí | No | Reportes de su entidad | Solo consulta |
| `CIUDADANO` | No | Sí | Solo sus propios reportes | Crear reportes y consultar los suyos |

Reglas visuales obligatorias:

- El ciudadano no debe ver rutas del dashboard.
- Un funcionario o empresa no debe ver reportes de otra entidad.
- La empresa no debe ver acciones de cambio de estado.
- Solo el administrador debe ver la acción de asignar entidad.
- El filtro por entidad solo debe mostrarse al administrador.
- No mostrar acciones deshabilitadas sin explicación: si un rol no puede hacer algo, no mostrar el control.

---

## 4. Principios de diseño

1. **Claridad operativa antes que decoración.**
   El dashboard es una herramienta de trabajo, no una landing page.

2. **Jerarquía visible.**
   Lo crítico debe destacar sin depender únicamente del color.

3. **El mapa es contexto, no un adorno.**
   Debe ayudar a ubicar reportes, barrios y sitios críticos.

4. **Estados y prioridades legibles.**
   Badges, texto e iconos deben permitir leer la situación de un reporte de un vistazo.

5. **Acciones según el rol.**
   La interfaz no debe sugerir acciones que el backend rechazará.

6. **Formularios rápidos en móvil.**
   Reportar debe requerir los mínimos pasos posibles.

7. **Accesibilidad.**
   Contraste, foco visible, etiquetas, navegación por teclado y objetivos táctiles grandes.

8. **Consistencia.**
   Los mismos colores, badges, cards y espaciados en web y móvil.

---

## 5. Sistema visual

### 5.1 Paleta

| Uso | Color |
|---|---|
| Primario | `#0F4C5C` |
| Primario suave | `#E6F2F3` |
| Fondo general | `#F5F7F8` |
| Superficie / cards | `#FFFFFF` |
| Texto principal | `#1B2A32` |
| Texto secundario | `#64748B` |
| Bordes | `#DDE5E8` |
| Éxito | `#15803D` |
| Advertencia | `#D97706` |
| Crítico | `#C2410C` |
| Informativo | `#2563EB` |
| Asignado | `#7C3AED` |
| Rechazado | `#B91C1C` |

No usar el color como única señal. Siempre acompañarlo con texto o icono.

### 5.2 Estados de reporte

| Estado | Estilo sugerido |
|---|---|
| `PENDIENTE` | Amarillo/ámbar suave, texto oscuro |
| `EN_REVISION` | Azul informativo |
| `ASIGNADO` | Violeta |
| `RESUELTO` | Verde |
| `RECHAZADO` | Rojo/gris |

### 5.3 Prioridades

| Prioridad | Estilo sugerido |
|---|---|
| `BAJA` | Verde |
| `MEDIA` | Azul |
| `ALTA` | Ámbar |
| `CRITICA` | Rojo/naranja fuerte |

### 5.4 Tipografía

- Inter, Manrope o una sans-serif similar.
- Títulos: semibold.
- Texto de interfaz: 14–16 px.
- Evitar textos menores a 12 px.
- Números de métricas con tamaño destacado.

### 5.5 Formas y espaciado

- Bordes redondeados: 12–16 px.
- Sombras suaves, no fuertes.
- Cards con padding de 16–24 px.
- Separación entre secciones: 24–32 px.
- Tablas legibles, con hover y filas cómodas.
- Iconografía lineal y consistente.
- Evitar gradientes decorativos y glassmorphism.

### 5.6 Responsive

| Contexto | Tamaño de referencia |
|---|---|
| Dashboard escritorio | 1440 × 900 |
| Dashboard tablet | 1024 × 768 |
| App móvil | 390 × 844 |

Comportamiento:

- Sidebar fijo en escritorio; colapsable en tablet.
- Tabla en escritorio; tarjetas en tablet/móvil.
- Filtros en barra superior; panel lateral o drawer en tablet.
- Mapa con controles grandes y leyenda visible.
- Formularios móviles a una columna.

### 5.7 Accesibilidad

- Contraste mínimo AA.
- Foco visible en todos los controles.
- Etiquetas visibles en formularios.
- Texto alternativo en imágenes.
- No depender solo del color.
- Targets táctiles mínimos de 44 px.
- Navegación por teclado en web.
- Mensajes de error asociados al campo correspondiente.
- No usar “click aquí” como único texto de acción.

---

## 6. Dashboard web

### 6.1 Estructura general

- **Sidebar**
  - Dashboard.
  - Reportes.
  - Mapa.
  - Entidades, solo administrador.
  - Perfil.
- **Header**
  - Nombre del usuario.
  - Rol.
  - Entidad, si aplica.
  - Botón de cerrar sesión.
- **Contenido**
  - Título de pantalla.
  - Acciones principales.
  - Filtros cuando apliquen.
  - Estado de carga o error.
- **Feedback**
  - Toasts para éxito.
  - Mensajes inline para validación.
  - Confirmación para acciones destructivas o importantes.

### 6.2 Login

**Objetivo:** permitir el acceso del personal autorizado.

Contenido:

- Logo o nombre del producto.
- Email.
- Contraseña con mostrar/ocultar.
- Botón “Iniciar sesión”.
- Estado de carga en el botón.
- Error visible para credenciales incorrectas.
- No incluir registro público.
- No incluir redes sociales.
- No incluir una landing comercial.

Criterios de aceptación:

- El formulario se puede usar con teclado.
- El error no depende solo del color.
- Al autenticarse, redirige según el rol.

### 6.3 Dashboard general

**Objetivo:** dar una lectura rápida del estado de la ciudad.

Contenido:

- Filtros globales:
  - rango de fechas;
  - barrio;
  - entidad, solo administrador.
- Tarjetas:
  - total de reportes;
  - pendientes;
  - en revisión;
  - críticos;
  - sin asignar;
  - resueltos.
- Gráficas:
  - por estado;
  - por categoría;
  - por prioridad;
  - top de barrios.
- Mapa resumen:
  - puntos de reportes;
  - leyenda;
  - acceso al mapa completo.
- Tabla de actividad reciente:
  - miniatura, ID, categoría, descripción, barrio, entidad, estado, prioridad, fecha.

Criterios de aceptación:

- Las cifras principales se leen sin interacción.
- Las gráficas tienen títulos y unidades claras.
- El mapa tiene una leyenda.
- La tabla se adapta a tablet.
- El estado vacío explica que no hay reportes en el rango.

### 6.4 Listado de reportes

**Objetivo:** encontrar y abrir reportes rápidamente.

Contenido:

- Barra de búsqueda por texto.
- Filtros:
  - estado;
  - categoría;
  - prioridad;
  - barrio;
  - entidad, solo administrador;
  - fecha desde;
  - fecha hasta.
- Botón “Limpiar filtros”.
- Tabla:
  - miniatura;
  - ID;
  - categoría;
  - descripción;
  - barrio;
  - entidad asignada;
  - estado;
  - prioridad;
  - fecha;
  - acción “Ver detalle”.
- Paginación:
  - página actual;
  - total de páginas;
  - selector de tamaño.
- Estado vacío.
- Estado de error de carga.

Criterios de aceptación:

- Los filtros se reflejan visualmente.
- La paginación no se pierde al abrir el detalle.
- En tablet, las filas se convierten en tarjetas.
- El administrador ve el filtro por entidad; funcionario y empresa no.

### 6.5 Detalle del reporte

**Objetivo:** revisar toda la información y ejecutar acciones permitidas.

Contenido:

- Imagen principal.
- Miniatura o placeholder si la imagen no está disponible.
- ID.
- Estado.
- Categoría.
- Severidad.
- Prioridad calculada.
- Confianza de IA, si existe.
- Descripción.
- Fecha de creación.
- Fecha de actualización.
- Barrio.
- Entidad asignada.
- Reporte padre, si existe.
- Mapa con la ubicación.

Acciones:

- Cambiar estado: solo `FUNCIONARIO` y `ADMINISTRADOR`.
- Asignar entidad: solo `ADMINISTRADOR`.
- Volver al listado.

Restricciones:

- No mostrar un historial completo de estados.
- La asignación de entidad no cambia automáticamente el estado.
- No permitir editar la descripción, la categoría ni la ubicación en esta fase.

### 6.6 Mapa

**Objetivo:** explorar la distribución geográfica de los reportes.

Contenido:

- Mapa a pantalla completa o layout dividido.
- Capas:
  - reportes;
  - barrios;
  - sitios de interés.
- Filtros laterales:
  - estado;
  - categoría;
  - prioridad;
  - fechas.
- Leyenda de prioridad y estado.
- Popup de reporte:
  - miniatura;
  - categoría;
  - estado;
  - prioridad;
  - fecha;
  - acción “Ver detalle”.
- Controles de zoom y reinicio.

Criterios de aceptación:

- Las capas se pueden activar y desactivar.
- Los puntos no se confunden con los polígonos de barrios.
- El popup no bloquea el mapa.
- El mapa funciona en tablet.

### 6.7 Entidades y asignación

**Objetivo:** administrar el catálogo de entidades y reasignar reportes.

Contenido:

- Lista de entidades:
  - nombre;
  - tipo;
  - correo de contacto.
- Modal de asignación:
  - búsqueda;
  - selección de entidad;
  - resumen del reporte;
  - confirmación.

Restricciones:

- Solo visible para administrador.
- No mostrar acciones de asignación a funcionario o empresa.
- Confirmar antes de guardar.

### 6.8 Perfil

- Nombre.
- Email.
- Rol.
- Entidad asignada, si aplica.
- Cerrar sesión.
- No mostrar opciones de edición de contraseña que el backend no tiene.

### 6.9 Pantallas auxiliares

- `403`: acceso denegado.
- `404`: página no encontrada.
- Sin resultados.
- Error de conexión.
- Carga inicial.
- Imagen no disponible.
- Sesión expirada: redirigir al login.

---

## 7. App móvil ciudadana

### 7.1 Navegación

Barra inferior:

1. Inicio.
2. Reportar.
3. Mis reportes.
4. Perfil.

### 7.2 Bienvenida, login y registro

- Registro con nombre, email y contraseña.
- Login con email y contraseña.
- Mostrar/ocultar contraseña.
- Estado de carga.
- Errores claros.
- Persistencia segura de la sesión.
- No incluir funciones del dashboard.

### 7.3 Inicio

- Saludo al usuario.
- Acción principal destacada: “Reportar incidencia”.
- Accesos rápidos:
  - mis reportes;
  - mapa;
  - perfil.
- Resumen de los últimos reportes propios.
- Estado vacío si aún no hay reportes.

### 7.4 Nuevo reporte

Dividir en pasos claros:

#### Paso 1: foto

- Tomar foto.
- Elegir de galería.
- Previsualización.
- Botón para cambiar la imagen.
- Mensaje si no hay permiso de cámara.

#### Paso 2: ubicación

- Mapa con marcador.
- Botón “Usar mi ubicación”.
- Campo de referencia o dirección.
- Manejo de permiso denegado.
- Confirmación visual de la ubicación.

#### Paso 3: descripción

- Campo opcional.
- Texto de ayuda: “La IA puede completar la clasificación”.
- Validación de longitud.
- Botón “Continuar”.

#### Paso 4: revisión

- Foto.
- Ubicación.
- Categoría sugerida.
- Severidad sugerida.
- Descripción.
- Botón “Enviar reporte”.
- Estado de carga durante la subida.

#### Paso 5: confirmación

- Ícono de éxito.
- Número de reporte.
- Categoría.
- Estado inicial.
- Entidad asignada, si existe.
- Botones:
  - “Ver mi reporte”.
  - “Volver al inicio”.

### 7.5 Mis reportes

- Lista de reportes propios.
- Miniatura.
- Categoría.
- Estado.
- Fecha.
- Barrio.
- Estado vacío.
- Acceso al detalle.

### 7.6 Detalle del reporte

- Foto.
- Estado.
- Categoría.
- Severidad.
- Prioridad.
- Descripción.
- Ubicación.
- Fecha.
- Entidad asignada.

El ciudadano no puede cambiar el estado ni asignar entidades.

### 7.7 Perfil

- Nombre.
- Email.
- Cerrar sesión.
- Información básica de la cuenta.

---

## 8. Datos y contrato de API

La fuente de verdad es `docs/api-frontend.md`.

### 8.1 Campos de un reporte

```text
id
estado
categoriaIa
severidadIa
prioridadCalculada
confianzaIa
descripcion
latitud
longitud
claveImagenHd
urlImagenHd
claveImagenThumb
urlImagenThumb
fechaCreacion
fechaActualizacion
usuarioId
barrioId
entidadAsignadaId
reportePadreId
```

### 8.2 Resumen del dashboard

```text
total
porEstado
porCategoria
porPrioridad
criticos
sinAsignar
porBarrio
```

### 8.3 Datos de ejemplo

Reporte:

```text
ID: 415
Estado: EN_REVISION
Categoría: FUGA_AGUA
Severidad: ALTA
Prioridad: CRITICA
Descripción: Fuga de agua frente al hospital
Barrio: Centro
Entidad asignada: Empresa de Acueducto y Alcantarillado
Fecha: 29 oct 2026, 20:00
```

Métricas:

```text
Total: 42
Pendientes: 20
En revisión: 8
Asignados: 7
Resueltos: 5
Rechazados: 2
Críticos: 3
Sin asignar: 2
```

Top de barrios:

```text
Centro: 8
La Esperanza: 6
Villa Nueva: 4
```

### 8.4 Reglas de datos

- Las fechas que llegan al frontend son ISO 8601.
- Los filtros de fecha se envían como `YYYY-MM-DD`.
- Las imágenes se muestran con `urlImagenThumb` o `urlImagenHd`.
- En modo local las URLs de archivos requieren autenticación.
- En S3/Silo las URLs son firmadas y temporales.
- No inventar campos que no estén en el DTO.
- No mostrar datos personales que el endpoint no devuelve.

---

## 9. Inventario de componentes

### Componentes compartidos

- `StatusBadge`.
- `PriorityBadge`.
- `MetricCard`.
- `ReportThumbnail`.
- `EmptyState`.
- `LoadingSkeleton`.
- `ErrorMessage`.
- `ConfirmDialog`.
- `Toast`.
- `PageHeader`.
- `SectionCard`.
- `FilterBar`.
- `DateRangeFilter`.
- `EntitySelect`.
- `Pagination`.

### Componentes del dashboard

- `Sidebar`.
- `Topbar`.
- `ReportsTable`.
- `ReportsTableRow`.
- `ReportsTableCard` para tablet.
- `DashboardMap`.
- `RecentReportsTable`.
- `MetricsCharts`.
- `ReportDetailPanel`.
- `StatusChangeDialog`.
- `AssignmentDialog`.
- `EntitiesTable`.

### Componentes móviles

- `BottomTabBar`.
- `ReportCard`.
- `PhotoPicker`.
- `LocationPicker`.
- `ReportWizard`.
- `ReportSummary`.
- `SuccessScreen`.
- `PermissionNotice`.

---

## 10. Estados de interfaz

Cada pantalla debe contemplar:

- **Cargando:** skeletons o spinner contextual.
- **Vacío:** explicar por qué no hay datos y qué hacer.
- **Error de red:** mensaje y botón “Reintentar”.
- **Error de permisos:** mensaje de acceso denegado.
- **Validación:** errores junto al campo.
- **Éxito:** toast o pantalla de confirmación.
- **Imagen no disponible:** placeholder, no romper el layout.
- **Ubicación no disponible:** permitir continuar con selección manual, si el flujo lo permite.
- **Permiso de cámara denegado:** explicar cómo habilitarlo.
- **Sesión expirada:** limpiar token y volver al login.

---

## 11. Estrategia de uso de Stitch

### 11.1 Qué no hacer

No pegar este brief completo en Stitch.

No pedir “todo el dashboard” de una vez.

No pedir “una app completa” de una vez.

No dejar que Stitch decida:

- la arquitectura;
- los endpoints;
- los campos de la API;
- los permisos;
- la navegación no especificada.

### 11.2 Qué hacer

1. Crear un proyecto para el dashboard web.
2. Pegar un prompt base corto.
3. Pedir una sola pantalla.
4. Corregir el resultado.
5. Pasar a la siguiente pantalla.
6. Repetir con la app móvil.

### 11.3 Prompt base para el dashboard web

```text
Diseña una interfaz para un dashboard administrativo de gestión de incidencias urbanas.

Es una herramienta interna para funcionarios, empresas y administradores. No es una landing page ni una página de marketing.

Estilo:
- institucional, moderno y sobrio;
- fondo claro;
- azul petróleo como color principal;
- cards blancas;
- bordes suaves;
- tipografía Inter o similar;
- badges para estados y prioridades;
- diseño desktop-first de 1440x900, adaptable a tablet.

Reglas:
- No agregues secciones de marketing.
- No agregues testimonios, precios, footer comercial ni llamadas a la acción promocionales.
- No inventes funcionalidades que no estén en la pantalla solicitada.
- No agregues textos legales.
- Usa español.
- Usa datos realistas, no Lorem ipsum.
- Genera solo la pantalla que se te pida.
```

### 11.4 Prompt de ejemplo: dashboard general

```text
Genera únicamente la pantalla principal del dashboard.

Debe contener:
- sidebar de navegación;
- header con nombre del usuario, rol y botón de cerrar sesión;
- filtros de fecha, barrio y entidad;
- tarjetas superiores con: total de reportes, pendientes, en revisión, críticos, sin asignar y resueltos;
- gráfica de reportes por estado;
- gráfica de reportes por categoría;
- gráfica de reportes por prioridad;
- mapa resumen con puntos;
- tabla de reportes recientes.

Datos de ejemplo:
Total: 42
Pendientes: 20
En revisión: 8
Críticos: 3
Sin asignar: 2
Resueltos: 5

No agregues otras secciones.
No agregues textos introductorios largos.
No agregues imágenes decorativas.
No agregues tablas adicionales.
```

### 11.5 Prompt de ejemplo: listado

```text
Genera únicamente la pantalla de listado de reportes.

Debe contener:
- header y sidebar consistentes con el dashboard;
- barra de búsqueda;
- filtros por estado, categoría, prioridad, barrio y rango de fechas;
- botón para limpiar filtros;
- tabla paginada;
- selector de cantidad de filas;
- paginación.

Columnas:
miniatura, ID, categoría, descripción, barrio, entidad asignada, estado, prioridad, fecha y acción “Ver detalle”.

Datos de ejemplo:
ID 415, FUGA_AGUA, “Fuga frente al hospital”, Centro, Empresa de Acueducto, EN_REVISION, CRITICA.

No agregues tarjetas de métricas.
No agregues mapa.
No agregues secciones promocionales.
No agregues reportes fuera de la tabla.
```

### 11.6 Prompt de ejemplo: nuevo reporte móvil

```text
Genera únicamente la pantalla de nuevo reporte para una app móvil ciudadana.

Debe contener:
- encabezado con título “Nuevo reporte” y botón de volver;
- indicador de pasos;
- foto seleccionada o botón para tomar/elegir foto;
- mapa pequeño con ubicación confirmada;
- campo de descripción opcional;
- resumen de categoría y severidad sugeridas por IA;
- botón principal “Enviar reporte”;
- estado de carga durante el envío.

Usa una pantalla de 390x844.
No agregues dashboard administrativo.
No agregues estadísticas.
No agregues secciones de marketing.
No agregues funciones que no estén en el flujo de reportar.
```

### 11.7 Prompt de corrección

Si Stitch agrega relleno:

```text
Elimina todo lo que no esté en la lista anterior.

No agregues secciones nuevas.
No agregues textos promocionales.
No agregues métricas que no se hayan indicado.
No agregues navegación que no se haya especificado.
Mantén la pantalla enfocada en la tarea indicada.
```

### 11.8 Orden de pantallas para Stitch

#### Web

1. Login.
2. Dashboard general.
3. Listado de reportes.
4. Detalle del reporte.
5. Mapa.
6. Entidades y asignación.
7. Perfil.

#### Móvil

1. Login/registro.
2. Inicio.
3. Nuevo reporte: foto.
4. Nuevo reporte: ubicación.
5. Nuevo reporte: revisión y envío.
6. Confirmación.
7. Mis reportes.
8. Detalle.
9. Perfil.

---

## 12. Implementación recomendada

### 12.1 Web

- React + Vite + TypeScript.
- React Router.
- TanStack Query para datos remotos.
- Tailwind CSS y componentes propios o shadcn/ui.
- Leaflet o MapLibre para el mapa.
- Recharts para las gráficas.
- Tipos compartidos basados en `docs/api-frontend.md`.

Estructura sugerida:

```text
src/
├── api/
├── components/
├── features/
│   ├── auth/
│   ├── dashboard/
│   ├── reportes/
│   ├── mapa/
│   └── entidades/
├── layouts/
├── pages/
├── types/
└── utils/
```

### 12.2 Móvil

- Expo + TypeScript.
- React Navigation.
- NativeWind o StyleSheet.
- Expo SecureStore.
- Expo Image Picker.
- Expo Location.
- React Native Maps o MapLibre.

### 12.3 Orden de implementación

#### Prioridad 0

- Login.
- Layout y rutas protegidas.
- Cliente HTTP y tipos.
- Dashboard general.
- Listado de reportes.
- Detalle de reporte.
- App móvil: login.
- App móvil: nuevo reporte.
- App móvil: mis reportes.

#### Prioridad 1

- Mapa completo.
- Entidades y asignación.
- Cambio de estado.
- Perfil.
- Estados vacíos y errores.
- Adaptación tablet.

#### Prioridad 2

- Animaciones.
- Ajustes de accesibilidad.
- Optimización de imágenes.
- Pruebas end-to-end.

---

## 13. Criterios de aceptación

- [ ] Login funcional con usuarios de los cuatro roles.
- [ ] Rutas protegidas según rol.
- [ ] Ciudadano no puede acceder al dashboard.
- [ ] Funcionario y empresa solo ven su entidad.
- [ ] Solo administrador ve asignación.
- [ ] Empresa no ve cambio de estado.
- [ ] Listado con filtros, paginación y estados vacíos.
- [ ] Detalle con imagen, mapa y metadatos reales.
- [ ] Dashboard con métricas coherentes.
- [ ] Mapa con reportes, barrios y sitios.
- [ ] App móvil permite crear un reporte con foto y ubicación.
- [ ] “Mis reportes” muestra solo los reportes del usuario.
- [ ] Manejo de `401`, `403`, `404` y errores de red.
- [ ] Diseño responsive en escritorio, tablet y móvil.
- [ ] Contraste, foco y etiquetas accesibles.
- [ ] No se inventan endpoints, campos ni funcionalidades.

---

## 14. Decisiones y notas

- La asignación de entidad **no cambia el estado** del reporte.
- No hay historial completo de estados.
- No hay notificaciones ni WebSockets.
- No hay refresh token; al expirar la sesión se vuelve al login.
- La app móvil no incluye el dashboard administrativo.
- El diseño de Stitch es una referencia visual; el código se reconstruye con componentes mantenibles.
- Cualquier cambio de contrato debe reflejarse primero en `docs/api-frontend.md`.

---

## 15. Próximos pasos

1. Aprobar este brief.
2. Generar las pantallas web en Stitch, una por una.
3. Generar las pantallas móviles, una por una.
4. Extraer el sistema visual definitivo.
5. Crear `gestion-frontend` con React + Vite + TypeScript.
6. Implementar la Fase 1 del frontend.
7. Validar con los usuarios de prueba del backend.
