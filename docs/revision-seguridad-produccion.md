# Revisión de seguridad en producción

Fecha: 6 de octubre de 2026 · Versión desplegada: `902bdf3` (pagos, temporizador y envío a WhatsApp)
Qué se ha revisado: https://arrima-petanca.vercel.app, el backend en Render, el repositorio público de GitHub y una melé completa jugada en producción con la cuenta de un club real.

Complementa a [seguridad-owasp.md](seguridad-owasp.md), que revisa el código. Aquí todo se ha comprobado **con peticiones reales a producción**, desde fuera y con la cuenta del club.

## Resumen

**No se filtra nada.** No hay secretos ni en el repositorio ni en el frontend publicado. Nadie sin cuenta puede escribir ni leer datos de administración. Un club no ve nada de otro club. La vista pública no enseña ni los pagos ni las marcas del administrador.

- **RLS de Supabase:** no afecta a Arrima. La explicación y una comprobación de un minuto están en el apartado 3.
- **Melé de prueba en producción:** completa de principio a fin, con **55 comprobaciones correctas**. El único fallo era un error de la propia prueba, no de la app (apartado 5).
- **Hallazgos:** el diálogo «Crear melé» en el móvil (corregido), R1 confirmado en producción (aceptado) y dos detalles menores (apartado 4).

## 1. Desde fuera, sin cuenta

| Comprobación | Resultado |
|---|---|
| Repositorio público: historial completo (13 commits) y ficheros actuales con gitleaks | Limpio. Ni `.env` ni los prompts. `TestSecrets.java` solo genera secretos aleatorios para los tests |
| Los 23 ficheros JavaScript que sirve Vercel | Ninguna mención a Supabase, ninguna clave ni token, ni siquiera la dirección de Render |
| Mapas de código fuente (`.js.map`) | No se sirven (403) |
| `http://` | Redirige a `https://`, tanto en Vercel (308) como en Render (301) |
| Cabeceras de Vercel | CSP estricta, HSTS, `nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy`, `Permissions-Policy`, `noindex` en `/m/*` y `sw.js` sin caché |
| Cabeceras del backend | `Content-Security-Policy: default-src 'none'`, HSTS y `Cache-Control: no-store` |
| Cualquier ruta `/api/*` de administración sin token | 401 |
| Rutas típicas de ataque (actuator, swagger, h2-console, `/error`) | 401 |
| `POST`, `PUT` o `DELETE` en rutas públicas | 401. La única excepción es la suscripción a avisos, prevista en el diseño |
| Token sin firma (`alg: none`) | Lo bloquea el cortafuegos de Vercel (403) y, contra Render directamente, el backend (401) |
| Token firmado con otra clave | 401 |
| CORS desde otro origen | No hay cabeceras CORS, así que el navegador lo bloquea |
| Renovar la sesión desde otra web (`Sec-Fetch-Site: cross-site`) | 403 |
| JSON mal formado, XML, rutas con `../` codificado | 400, 415 y 400, sin trazas ni versiones |
| Probar códigos de melé al azar | 429 a partir de 30 fallos por minuto |
| Probar contraseñas de un correo | 429 en el 11.º intento |
| Registrarse con una invitación inventada | 400 `INVITATION_INVALID`. No se crea nada |
| Enlace de foto con la firma o la caducidad cambiadas a mano | 403 |
| Suscripción a avisos que apunta a una dirección interna (`169.254.169.254`, SSRF) | 400 |
| Suscripción a avisos de una melé cerrada | Rechazada |

## 2. Con la cuenta del club

| Comprobación | Resultado |
|---|---|
| Cookie de sesión | `HttpOnly; Secure; SameSite=Strict; Path=/api/auth` |
| Respuestas de la API | `no-store`: ni el navegador ni Vercel las guardan |
| Contenido del token | Solo emisor, club, administrador y fechas. Caduca a los 15 minutos. Ni correo ni nombre |
| Perfil del club | Ningún campo sensible (ni hash ni tokens) |
| Pedir o modificar melés, jugadores, partidos, premios o fotos de otros números | 404, igual que si no existieran |
| Mandar otro `clubId` en el cuerpo | Se ignora: el club sale siempre del token |
| Vista pública mientras se cobra | No muestra quién ha pagado ni cuánto se ha recaudado |
| Vista pública con equipos | Quien no pagó aparece como «No juega», sin decir por qué |
| Vista pública de los premios | Muestra los premios entregados, pero no la marca de «enviado al grupo» |
| Melé cerrada | Solo lectura: un cambio (marcar un pago) responde 409 |

## 3. Supabase y el RLS (tu pregunta)

**Respuesta corta: no importa para Arrima, y aun así las tablas de Arrima ya lo tienen activado.**

**Qué hace el RLS.** El RLS (*Row Level Security*) decide qué filas puede ver cada usuario de Postgres. En Supabase sirve para las apps que hablan **directamente** con la base de datos desde el navegador, a través de la *Data API* (`https://<proyecto>.supabase.co/rest/v1/...`) y con la clave `anon` o *publishable*. En esas apps, la clave va dentro del JavaScript y cualquiera puede usarla, así que el RLS es la única barrera.

**Arrima no funciona así.** El navegador solo habla con nuestro backend, y es el backend el que se conecta a Postgres, con el usuario `postgres`, que es el dueño de las tablas. El dueño se salta el RLS, así que activarlo o desactivarlo no cambia nada en la app. Quien protege los datos es el backend: el club sale del token y cada consulta va filtrada por él.

**Para que alguien leyera la base de datos saltándose el backend**, tendría que superar cuatro barreras a la vez:

1. **La Data API** está desactivada según la guía de despliegue. Y aunque estuviera activa, por defecto solo expone el esquema `public`, que está vacío. Las tablas de Arrima están en `arrima`.
2. **Los roles `anon` y `authenticated`**, los que usa la Data API, no reciben permisos sobre un esquema nuevo como `arrima` (Supabase solo se los da en `public`). La consulta de abajo lo confirma.
3. **El RLS:** las migraciones (`V1`, `V2` y `V4`) lo activan en las 19 tablas de Arrima y no crean ninguna política, así que esos roles no verían ni una fila.
4. **La clave `anon`** no está publicada en ningún sitio. Comprobado: no aparece en el frontend desplegado.

**Qué puede ser el «no activado» que viste.** Hay dos posibilidades:

- **La tabla `flyway_schema_history`.** Es la que usa Flyway para apuntar qué migraciones ha aplicado, y no lleva RLS. Solo contiene los nombres y las sumas de control de las migraciones, que ya son públicos en GitHub.
- **La opción de activar el RLS automáticamente al crear el proyecto.** Solo afecta a tablas nuevas del esquema `public`, que Arrima no usa.

**Comprobación (un minuto).** En Supabase, abre *SQL Editor*, pega esto y pulsa *Run*. Solo lee; no cambia nada.

```sql
-- RLS de cada tabla de Arrima, y si los roles de la Data API pueden leerla
select c.relname as tabla,
       c.relrowsecurity as rls,
       has_table_privilege('anon', c.oid, 'select') as anon_lee,
       has_table_privilege('authenticated', c.oid, 'select') as authenticated_lee
from pg_class c
join pg_namespace n on n.oid = c.relnamespace
where n.nspname = 'arrima' and c.relkind = 'r'
order by 1;

-- ¿Pueden entrar siquiera en el esquema?
select has_schema_privilege('anon', 'arrima', 'usage') as anon_entra,
       has_schema_privilege('authenticated', 'arrima', 'usage') as authenticated_entra;
```

**Qué deberías ver:**

- En la primera consulta, `rls = true` en todas las tablas menos `flyway_schema_history`, y `false` en `anon_lee` y `authenticated_lee` en todas.
- En la segunda, `false` y `false`.
- Si sale algo distinto, avísame y lo cerramos con una migración que retire esos permisos.

**Dos comprobaciones más en el panel de Supabase:**

- **Data API:** en la configuración del proyecto debe estar desactivada. Si está activa, la lista de esquemas expuestos no debe incluir `arrima`.
- **Storage:** el bucket de las fotos no debe tener la etiqueta *Public*.

**Lo que sí importa en Supabase** es que la contraseña de la base de datos y la clave secreta (`SUPABASE_SECRET_KEY`) estén solo en las variables de Render, y así es: no están ni en el repositorio ni en el frontend. Esas dos sí se saltan todo.

## 4. Hallazgos

| # | Hallazgo | Gravedad | Estado |
|---|---|---|---|
| 1 | **Diálogo «Crear melé» en el móvil.** El botón «Crear melé» quedaba al final de una lista más alta que la pantalla: en un iPhone 13, 995 px de contenido en 519 px visibles. Además, Safari mostraba flechas en los campos numéricos | Usabilidad | **Corregido** (sin subir todavía) |
| 2 | **R1, confirmado en producción.** Llamando directamente a `arrima-api.onrender.com` con un `X-Forwarded-For` público inventado, los límites por IP vuelven a empezar. Con IPs privadas (`10.x`) no funciona | Baja | **Aceptado** el 6 de octubre de 2026 (opción A, abajo) |
| 3 | La contraseña de la cuenta del club ha pasado por el chat de esta revisión | Media para esa cuenta | **Cámbiala.** Al cambiarla se cierran todas las sesiones |
| 4 | `/api/files` sin firma responde 400 con el texto genérico de Spring («Required parameter 'expires' is not present») en vez de 403 | Informativa | Se puede dejar: solo revela el nombre de un parámetro, que ya está en el código público |
| 5 | Dos PR de Dependabot abiertos: dependencias del frontend y Maven 3.10.0 | Informativa | El de Maven 3.10.0 no pasa los tests del backend: no aceptarlo hasta que se vea por qué |
| 6 | **El job `secrets` del CI salió en rojo en el commit desplegado.** No era una fuga: el primer push llevaba el commit inicial del repositorio, y `gitleaks-action` intentó revisar desde su padre, que no existe (error de git, código 1; reproducido en local). Además, esa acción solo revisa lo que se sube en cada push, no todo el historial como decía el comentario | Baja (el control no hacía lo prometido) | **Corregido:** el CI descarga gitleaks 8.30.1, comprueba su SHA-256 y revisa el historial completo en cada ejecución. Historial completo limpio |

**Cómo queda el diálogo.** Arriba, «¿Dupletas o tripletas?». Debajo, una tarjeta con el resumen de los ajustes del club (por ejemplo, «3 partidas de 45 min · 5 premios · 12 pistas · cuota de 5 €») y el botón «Cambiar solo para hoy». Al pulsarlo aparecen los ajustes, que se desplazan, mientras «Crear melé» queda **fijo abajo, siempre visible**. El diálogo de ajustes de una melé ya creada usa el mismo pie fijo. Comprobado en iPhone 13 (Safari), Pixel 7 (Chrome) y ordenador.

### R1: opciones y decisión

**Decisión (6 de octubre de 2026): opción A.** Se acepta el riesgo y queda documentado. Si algún día hay muchos clubes o se ve abuso en los logs (avisos de límite agotado), se puede pasar a la B.

**Por qué la gravedad es baja.** Las cuentas siguen protegidas por el límite por correo (10 intentos cada 15 minutos). Los códigos de invitación tienen 48 bits y los de las melés, unos 40: ni sin límite por IP se pueden adivinar en un tiempo razonable.

| Opción | A favor | En contra |
|---|---|---|
| **A. Aceptarlo y dejarlo documentado** (elegida) | Nada nuevo que mantener | Los límites por IP solo frenan a quien entra por Vercel |
| **B. Cabecera secreta.** Vercel *Routing Middleware* (gratis) añade una cabecera con un secreto, y el backend rechaza lo que no la traiga | Cierra R1 y además impide usar Render directamente | Un fichero de *middleware* más y un secreto más en Vercel y Render. Hay que comprobar que el directo (SSE) sigue funcionando a través del *middleware* |

## 5. Melé completa en producción

Jugada con la API de producción y comprobada contra un **cálculo independiente** hecho por la propia prueba, no contra lo que dice la app.

**Preparación.** Lista pegada de WhatsApp con cabecera y emojis. Mezclaba formatos «1.», «2)», «3-» y «4 », tenía un nombre tachado (`~Antonio~`) y un número vacío. Ajustes del día: 3 partidas de 5 minutos, 5 premios, 12 pistas y cuota de 5 €.

| Fase | Comprobado |
|---|---|
| Inscripción | 13 apuntados. El tachado se marca y no entra. El número vacío se salta |
| Pagos | «11 de 12 han pagado · 55 €» (Pilar, de baja, no cuenta). José, sin marcar |
| Generar equipos | Primero avisa de que José está sin marcar. Después, de que 11 no caben en dupletas. Al final, 4 dupletas y 1 tripleta con **exactamente** los 11 que pagaron. Si algo se rechaza, no cambia nada |
| Cuadro | 3 partidas de 2 partidos. Descansa un equipo distinto en cada ronda. Nadie repite rival. Todos los partidos tienen pista |
| Contador «por cifra final» | Coincide con el cálculo propio antes de empezar y después de cada una de las 3 rondas |
| Temporizador | 5 minutos. Una pausa de 4 s retrasa el final 4,3 s. **El servidor solo** marcó «Tiempo cumplido» en el instante exacto y avisó en directo, sin que nadie tocara la melé. La ronda 2 empieza sin preguntar. La 3 pregunta y para la 2 sin alarma |
| Internacional | Grupos por victorias (3, 2 y 1). El equipo solo en su grupo tiene premio sin jugar. Los puntos de cada tirada siguen la tabla del club. Los empates se deshacen con desempate (2 rondas) |
| Premios | Del 1.º al 5.º: primero por victorias y, dentro de cada grupo, por la Internacional |
| Fotos | Vuelven idénticas por su enlace firmado. Un fichero que no es imagen se rechaza |
| Envío al grupo | Queda registrado. La vista pública no lo muestra |
| Cierre | Pasa al historial, solo lectura, y ya no admite suscripciones |

En total, **55 comprobaciones correctas** y 72 avisos en directo recibidos. La única comprobación fallida esperaba que la cabecera de la lista («MELÉ DOMINGO…») saliera marcada en la vista previa. Pero el lector descarta las líneas sin número, como dice su especificación. Solo marca las líneas **con** número que van antes del «1.», como «4 de octubre, melé a las 10», y eso lo cubre el test `numberedHeaderLinesBeforeTheListAreFlaggedNotDropped`.

**Lo que esta prueba no cubre** porque depende del móvil (que llegue la notificación con la pantalla bloqueada, el sonido, el envío real a WhatsApp) está en [comprobacion-en-moviles.md](comprobacion-en-moviles.md).
