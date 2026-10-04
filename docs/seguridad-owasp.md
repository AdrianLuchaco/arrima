# Revisión de seguridad de Arrima (OWASP Top 10)

Fecha: 4 de octubre de 2026 · Versión revisada: fase 9 (antes del primer despliegue)
Referencia: [OWASP Top 10:2025](https://owasp.org/Top10/). En esta edición, el SSRF pasa a formar parte de A01, y aparecen A03 (cadena de suministro) y A10 (gestión de condiciones excepcionales).

## Resumen

No hay vulnerabilidades conocidas abiertas de gravedad alta ni media. La revisión encontró **cuatro problemas, todos corregidos en esta fase**, y deja **cinco riesgos residuales bajos** que se aceptan o que necesitan una decisión tuya (regla 1.2), al final del documento.

| # | Encontrado en la revisión | Gravedad | Estado |
|---|---|---|---|
| 1 | Ningún límite de tamaño para los cuerpos JSON: un `POST` de cientos de MB a `/api/auth/login`, que no exige cuenta, podía agotar los 512 MB de Render | Media | Corregido: `RequestBodyLimitFilter` (512 KB) |
| 2 | Las llamadas a Supabase y Brevo no tenían tiempo máximo ni bloqueaban redirecciones | Baja | Corregido: `OutboundHttp` (5 s de conexión, 20 s de lectura, sin redirecciones) |
| 3 | Un 502/503 de Render al renovar la sesión cerraba la sesión del administrador | Baja (disponibilidad) | Corregido: solo un 401/403 cierra la sesión |
| 4 | No quedaba rastro en el log cuando alguien agotaba un límite de intentos | Baja (detección) | Corregido: aviso en el log, con un hash del sujeto en vez del correo o la IP |

Además, faltaban tests de aislamiento entre clubes para la Internacional y los premios; se han añadido.

**Cómo se ha revisado:** lectura del código capa por capa; búsquedas de patrones peligrosos (consultas concatenadas, `findById` sin filtrar por club, `dangerouslySetInnerHTML`, secretos); peticiones reales al backend local para comprobar cabeceras y respuestas; `npm audit`; búsqueda de versiones nuevas de Maven; gitleaks sobre todo el historial; y tests automáticos para cada control (al final, cómo repetir la revisión).

---

## A01:2025 · Control de acceso roto

**Aislamiento entre clubes.** El `clubId` sale siempre del token (`AdminPrincipal`), nunca del cuerpo ni de la URL. Todos los controladores de administración lo reciben del token y lo pasan al servicio. La melé se obtiene con `MeleeAccess.forClub(meleeId, clubId)`, que responde 404 si es de otro club.

Los identificadores hijos se buscan siempre acotados a su melé: `findByIdAndMeleeId` para jugadores, partidos y premios, y `findByIdAndPrizeId` para las fotos. En la Internacional, la ronda se comprueba a través de su grupo (`InternationalService.recordThrow`). Así no se puede tocar un objeto de otro club adivinando su número (IDOR).

Respuesta 404 (no 403) a lo ajeno: no revela si existe.

Tests: `RegistrationIntegrationTest.aClubCannotSeeOrTouchAnotherClubsMelee`, `aParticipantOfAnotherMeleeOfTheSameClubIsNotFound`, `TeamsAndScheduleIntegrationTest.anotherClubCannotDrawOrRecordResults` y, nuevo en esta fase, `PrizesIntegrationTest.anotherClubCannotTouchLaInternacionalNorThePrizes`.

**Denegar por defecto.** En `SecurityConfig` cada ruta está listada y el resto es `denyAll()`. Las rutas públicas son `GET` (`/api/public/**`, `/api/files/**`, `/api/health`, `/api/time`), con una sola excepción: la suscripción «Avísame cuando se acabe el tiempo» (`POST /api/public/melees/{código}/push-subscriptions`). Esa escritura está atada al código de la melé, limitada por IP (20 por hora) y por melé (500), valida que la clave del navegador sea un punto real de la curva P-256 y desaparece al cerrar la melé. Comprobado: cualquier otro `POST` a una ruta pública responde 401.

**Imágenes.** El bucket de Supabase es privado. El navegador solo ve enlaces del backend firmados con HMAC y con caducidad (`FileLinkSigner`). La firma se compara en tiempo constante (`MessageDigest.isEqual`) y la ruta se valida contra una lista blanca (`StoredFilePaths`).

**SSRF.** El backend llama a destinos fijos de la configuración (Supabase y `api.brevo.com`) y, para los avisos del temporizador, a la dirección de push que manda cada navegador. Esa dirección viene de fuera, así que solo se aceptan los servicios de push de los navegadores: Google, Apple, Mozilla y Microsoft, en HTTPS, sin usuario y en el puerto estándar (`PushEndpoints`). Se comprueba al suscribirse y otra vez antes de cada envío. No se siguen redirecciones.

**Supabase.** Las tablas están en el esquema propio `arrima`, que la Data API no expone, y tienen RLS activado sin políticas, así que los roles `anon` y `authenticated` no ven nada. Además, la guía de despliegue indica desactivar la Data API.

## A02:2025 · Configuración de seguridad incorrecta

**Cabeceras del backend** (comprobadas con `curl`): `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer` y `Cache-Control: no-store`. Spring añade además `Strict-Transport-Security` en las peticiones HTTPS, que en Render llegan marcadas como tales gracias a `forward-headers-strategy: native`.

**Cabeceras del frontend** (`frontend/vercel.json`): una CSP estricta (`script-src 'self'`, `style-src 'self'`, sin `unsafe-inline`; el HTML no tiene ni scripts ni estilos en línea), HSTS de dos años, `nosniff`, `X-Frame-Options: DENY`, `Permissions-Policy` restrictiva y `noindex` para las páginas públicas de las melés.

**CORS:** no se configura nada a propósito. El *rewrite* de Vercel hace que el frontend y la API compartan origen, así que cualquier otro origen queda bloqueado por el navegador.

**Superficie mínima:** sin Actuator, sin consola H2, sin Swagger y sin páginas de error de Tomcat. El contenedor corre como usuario sin privilegios (uid 10001) con la imagen JRE. `open-in-view: false`.

**Arranque a prueba de olvidos:** si falta `JWT_SECRET` (o tiene menos de 32 bytes), `SUPABASE_*`, `BREVO_API_KEY`, `MAIL_SENDER_EMAIL` o `APP_URL` (que además debe ser `https://`), la aplicación no arranca y el log dice qué variable falta. Así nunca corre a medias en producción.

## A03:2025 · Fallos en la cadena de suministro de software

- **Pocas dependencias.** El frontend tiene 8 de producción (una veintena de paquetes contando las transitivas). Se descartaron alternativas pesadas: `qrcode` (91 paquetes) por `qrcode-generator` (ninguna dependencia), y vite-plugin-pwa/Workbox por un *service worker* propio de 60 líneas. En el backend, solo *starters* oficiales de Spring Boot más el driver de PostgreSQL y Flyway.
- **Versiones al día:** Spring Boot 4.1.1 es la última estable (solo hay un hito 4.2.0-M2). `npm audit`: 0 vulnerabilidades.
- **Dependabot** (`.github/dependabot.yml`) propone cada semana las actualizaciones de Maven, npm, GitHub Actions y Docker, y el CI las prueba antes de que las aceptes.
- **Builds reproducibles:** `package-lock.json` con `npm ci`, el *wrapper* de Maven con versión fija e imágenes base `eclipse-temurin:25`.
- **WhatsApp:** no se usa ninguna librería no oficial, solo la Web Share API del navegador y enlaces `wa.me`.

## A04:2025 · Fallos criptográficos

- **Contraseñas** con BCrypt (`DelegatingPasswordEncoder`, que guarda el prefijo `{bcrypt}` para poder cambiar de algoritmo y se rehashea al entrar). Política del NIST: mínimo 10 caracteres y máximo 72 bytes (el límite de BCrypt), sin reglas absurdas.
- **Tokens aleatorios** de 256 bits con `SecureRandom`: *refresh*, recuperación e invitaciones (estas, de 48 bits). En la base de datos solo se guarda su SHA-256. Un volcado de la base de datos no permite usarlos.
- **JWT HS256** con una clave derivada de `JWT_SECRET` por HMAC. Los enlaces de imágenes usan otra clave derivada (separación de dominios). El decodificador solo acepta HMAC, así que no hay confusión de algoritmos, y valida el emisor y la caducidad.
- **TLS en todo el camino:** navegador → Vercel → Render (HTTPS), y backend → Supabase (`sslmode=require`) y Brevo (HTTPS).
- **Web Push:** cada notificación va cifrada solo para el navegador que la pidió (RFC 8291, implementación propia con la criptografía del JDK, comprobada byte a byte con el ejemplo del RFC) y firmada con nuestra clave VAPID (RFC 8292, ES256). La clave privada VAPID va en una variable de entorno, como el resto de secretos.

## A05:2025 · Inyección

- **SQL:** solo JPA, consultas derivadas y `@Query` con parámetros con nombre. La única consulta escrita a mano (`DatabaseProbeRepository`) no lleva parámetros. Una búsqueda de concatenaciones en consultas no encontró ninguna.
- **XSS:** React escapa todo el texto, y no hay `dangerouslySetInnerHTML`, `innerHTML` ni `eval` (comprobado). El texto pegado de WhatsApp se normaliza en el backend (`TextSanitizer`: NFC, sin caracteres de control ni de formato invisibles). El correo de recuperación escapa el nombre del club en su versión HTML (`PasswordResetEmail`, con test).
- **Imágenes:** se comprueba el tipo real por sus *magic bytes* (no por la extensión ni el `Content-Type`), el tamaño y las dimensiones. El nombre se regenera y la ruta se valida contra una lista blanca.

## A06:2025 · Diseño inseguro

- **Límites de intentos** (`RateLimitPolicy`): login por IP y por correo; registro; renovación de sesión; códigos públicos (solo cuentan los fallos); y recuperación de contraseña por IP, por correo y en la confirmación. Los límites por correo son los que protegen las cuentas de verdad (ver riesgo residual R1).
- **Sin enumeración de cuentas:** el login responde igual a un correo inexistente que a una contraseña incorrecta, y tarda lo mismo, porque compara contra un hash ficticio. La recuperación responde 202 siempre, y el correo sale en segundo plano para que el tiempo de respuesta no lo delate. El aviso de «correo ya registrado» solo aparece tras validar un código de invitación.
- **Registro solo con invitación** de un solo uso que caduca.
- **Recuperación de contraseña:** el enlace sirve una vez y caduca a los 30 minutos; pedir otro anula el anterior. El token va tras `#`, así que no llega a los logs de Vercel ni de Render, y la página lo borra de la barra de direcciones. La URL base sale de `APP_URL` y no de la cabecera `Host`, lo que impide envenenar los enlaces. Cambiar la contraseña revoca todas las sesiones.
- **Rutas públicas de solo lectura** y vista pública limitada a lo que corresponde a cada fase (los premios aparecen a medida que se entregan).
- **Límites de recursos** contra el abuso: 300 jugadores por melé, 10 fotos por premio y 60 por melé, 1.000 conexiones en directo, cuerpos JSON de 512 KB (nuevo) y subidas de 4 MB.

## A07:2025 · Fallos de autenticación

- **Access token** de 15 minutos guardado solo en memoria, nunca en `localStorage`.
- **Refresh token rotativo** en una cookie `HttpOnly; Secure; SameSite=Strict; Path=/api/auth`. Reutilizar un token ya rotado revoca toda la familia. Hay 60 segundos de margen para las respuestas perdidas en pistas con mala cobertura.
- **CSRF:** la API se autentica con *bearer*, que el navegador nunca añade solo. Los dos *endpoints* con cookie (renovar y salir) son `SameSite=Strict` y además rechazan `Sec-Fetch-Site` de otro origen (`CrossSiteRequestFilter`).
- **Cerrar sesión** revoca la familia del token; cambiar la contraseña revoca todas.
- Ver los riesgos residuales R2 (los 15 minutos del *access token*) y R3 (bloqueo temporal por correo).

## A08:2025 · Fallos de integridad del software o de los datos

- **El CI es la puerta del despliegue:** Render solo despliega si el CI de GitHub pasa (`autoDeployTrigger: checksPass`).
- **Secretos, con tres barreras:** el *hook* `pre-commit` con gitleaks, el job `secrets` del CI sobre todo el historial y la *push protection* de GitHub. gitleaks sobre los 8 commits: limpio. La única coincidencia en disco es `backend/.env` local, que está ignorado y nunca se ha subido.
- **Ninguna deserialización de objetos Java;** solo JSON con Jackson y tipos concretos.
- **Concurrencia:** contador `revision` atómico en la base de datos, restricciones únicas diferibles y bloqueos `SELECT … FOR UPDATE` en la rotación de tokens y en los enlaces de recuperación.
- **Service worker:** solo guarda ficheros estáticos del propio origen y nunca intercepta `/api`, así que no puede servir datos viejos ni ajenos.

## A09:2025 · Fallos de registro y alertas

- **Se registra:** la reutilización de un *refresh token* (WARN, con familia y administrador); que alguien agote un límite de intentos (WARN, una vez por ventana, con un hash corto del correo o la IP: nuevo); los fallos al enviar correo (sin su contenido, que lleva el enlace); y los errores 500 con su traza, solo en el log.
- **No se registra:** contraseñas, tokens, enlaces de recuperación ni el contenido de las peticiones. El emisor de correos que escribe en el log (`LoggingEmailSender`) solo existe en el perfil `local`.
- **Alertas:** cron-job.org avisa por correo si `/api/health/db` falla, y Render avisa de los despliegues fallidos.
- Ver el riesgo residual R4 (conservación de los logs).

## A10:2025 · Gestión incorrecta de condiciones excepcionales

- **Errores sin detalles internos:** `ApiExceptionHandler` responde con RFC 9457 y un `code`, y nunca con trazas. Una caída de la base de datos da 503 y cualquier otra excepción, 500 sin detalle. Los filtros de seguridad usan el mismo formato (`SecurityProblemResponses`).
- **Fallar cerrado:** sin `JWT_SECRET` válido no hay arranque, una firma inválida da 403 y un token inválido, 401.
- **Transacciones coherentes:** los ficheros se borran y los correos se envían solo tras el commit (`StorageCleanup`, `EmailDispatcher`). La revocación de una familia de tokens robada no se deshace aunque la petición falle (`noRollbackFor`).
- **Fallos de red en el móvil:** la cola *offline* reintenta con PUT idempotentes, la sesión no se pierde por falta de cobertura (corregido en la fase 8) ni por un reinicio de Render (corregido en esta fase), y al apagar, las conexiones en directo se cierran limpiamente.

---

## Riesgos residuales

| # | Riesgo | Gravedad | Propuesta |
|---|---|---|---|
| R1 | **Los límites por IP se pueden esquivar llamando directamente a `arrima-api.onrender.com` con un `X-Forwarded-For` inventado.** Vercel sobrescribe esa cabecera, pero quien se salte Vercel puede poner la que quiera. | Baja | Las cuentas siguen protegidas por los límites por correo (10 intentos cada 15 minutos), los códigos de invitación tienen 48 bits y los públicos, ~40 bits. Si lo quieres cerrar del todo, la opción es que Vercel añada una cabecera secreta que el backend exija (con *Routing Middleware* de Vercel, gratis). Es un cambio de arquitectura: **decisión tuya**. |
| R2 | Tras cerrar sesión o cambiar la contraseña, un *access token* ya emitido sigue valiendo hasta 15 minutos. | Baja | Es lo normal en JWT sin estado. Acortarlo a 5 minutos cuesta más renovaciones con mala cobertura. Recomiendo dejarlo así. |
| R3 | Quien conozca el correo del administrador puede bloquearle el login 15 minutos (10 intentos fallidos) o la recuperación una hora (3 peticiones). | Baja | Es el precio de frenar la fuerza bruta. Si ocurre, el log muestra el aviso del límite. |
| R4 | Render gratuito guarda pocos días de logs y no hay alertas por patrones (por ejemplo, muchos avisos de límite). | Baja | Si hace falta, se pueden enviar los logs a un servicio gratuito (Better Stack, Grafana Cloud). Es un servicio nuevo: **decisión tuya**. |
| R5 | El backend usa el usuario `postgres` de Supabase, que puede hacer DDL. | Baja | Para mínimo privilegio, se podría separar un usuario solo para Flyway y otro con permisos DML sobre `arrima`. Añade complejidad al despliegue; lo dejaría para cuando haya más de un club. |

**Privacidad (no técnico):** la vista pública muestra los nombres de los jugadores a cualquiera que tenga el código, y las fotos de los premios son visibles con su enlace durante unas horas. Conviene que el club lo avise a los jugadores, por ejemplo en el grupo de WhatsApp. Supabase gratuito no hace copias de seguridad descargables: en la guía de despliegue tienes el comando para hacer una tú.

## Cómo repetir la revisión

```sh
cd backend && ./mvnw verify                    # ~500 tests: aislamiento, auth, límites, filtros, firmas
cd frontend && npm audit && npm test
gitleaks git --redact                          # todo el historial
curl -sI https://<tu-proyecto>.vercel.app      # cabeceras del frontend
curl -sI https://arrima-api.onrender.com/api/health   # cabeceras del backend
```
