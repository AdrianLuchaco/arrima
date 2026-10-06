# Desplegar Arrima paso a paso

Esta guía lleva Arrima de tu ordenador a internet, gratis, en unas dos horas. Sigue los pasos **en orden**: cada servicio necesita algún dato del anterior. En cada paso pone qué tienes que copiar y dónde vas a pegarlo.

## Antes de empezar

### Qué vas a montar

```
Móvil ──► Vercel (frontend + /api/*) ──► Render (backend Java) ──► Supabase (PostgreSQL + fotos)
                                                     └──────────► Brevo (correos)
cron-job.org ──► Render cada 10 minutos (para que no se duerman ni Render ni Supabase)
```

### Qué tienes que cambiar en el código

Casi nada. Toda la configuración va en variables de entorno, en los paneles de cada servicio. Solo hay un fichero que **quizá** tengas que tocar:

| Fichero | Qué | Cuándo |
|---|---|---|
| `frontend/vercel.json` | La dirección del backend (`https://arrima-api.onrender.com`) | Solo si Render le da otro nombre a tu servicio (paso 6.5) |

### Cuentas que necesitas (todas gratuitas)

Crea las cuentas antes de empezar; mejor con el mismo correo en todas:

- [ ] [GitHub](https://github.com): el código.
- [ ] [Supabase](https://supabase.com): la base de datos y las fotos. Puedes entrar con GitHub.
- [ ] [Brevo](https://www.brevo.com): los correos de «¿Has olvidado la contraseña?».
- [ ] [Vercel](https://vercel.com): el frontend. Entra con GitHub.
- [ ] [Render](https://render.com): el backend. Entra con GitHub.
- [ ] [cron-job.org](https://cron-job.org): el despertador.

### Una libreta para los datos

Vas a ir apuntando datos de un servicio para pegarlos en otro. Abre una **nota segura en tu gestor de contraseñas**, nunca un fichero del proyecto, y copia esta plantilla:

```
DB_URL              =
DB_USERNAME         =
DB_PASSWORD         =
SUPABASE_URL        =
SUPABASE_SECRET_KEY =
BREVO_API_KEY       =
MAIL_SENDER_EMAIL   =
APP_URL             =   (la dirección de Vercel)
VAPID_PUBLIC_KEY    =
VAPID_PRIVATE_KEY   =
VAPID_SUBJECT       =   mailto:tu@correo.com
URL del backend     =   (la de Render)
```

> **Regla de oro:** nada de esto va nunca a un fichero del repositorio, a un commit, a un chat ni a una captura de pantalla.

---

## Paso 1 · Comprobar el proyecto en tu ordenador

1. Abre una terminal en la carpeta del proyecto.
2. Comprueba que el *hook* que bloquea secretos está activo:
   ```sh
   git config core.hooksPath
   ```
   Debe responder `.githooks`. Si no responde nada, actívalo:
   ```sh
   git config core.hooksPath .githooks
   ```
3. Genera las claves de los avisos del temporizador («¡Tiempo!»):
   ```sh
   ./scripts/generate-vapid-keys.sh
   ```
   Copia las dos líneas que salen a tu libreta, como `VAPID_PUBLIC_KEY` y `VAPID_PRIVATE_KEY`. En `VAPID_SUBJECT` pon `mailto:` seguido de tu correo: es el contacto que verán Google y Apple si algo va mal. Sin estas tres variables la app funciona igual, pero sin notificaciones con el móvil bloqueado.
4. Comprueba que no hay nada sin guardar:
   ```sh
   git status
   ```
   Debe decir `nothing to commit, working tree clean`. El fichero `prompt-arrima-claude-code.md` no sale porque está excluido a propósito.

---

## Paso 2 · GitHub

1. En GitHub, pulsa **+ → New repository**.
   - **Repository name**: `arrima`.
   - **Public** o **Private**: en un repositorio público, Actions es ilimitado y la protección de secretos, gratuita. En uno privado tienes 2.000 minutos de Actions al mes, de sobra.
   - **No** marques «Add a README», «.gitignore» ni «license»: ya existen.
   - **Create repository**.
2. En tu terminal, sube el código (cambia `<tu-usuario>`):
   ```sh
   git remote add origin https://github.com/<tu-usuario>/arrima.git
   git push -u origin main
   ```
3. Solo si el repositorio es **público**: ve a **Settings → Advanced Security** y activa **Secret Protection** y **Push protection**, si no lo están ya.
4. Abre la pestaña **Actions**. Verás el workflow **CI** en marcha. Espera a que salga en verde (tarda unos 5 minutos); los tres jobs deben salir en verde: `backend`, `frontend` y `secrets`.
   - Si el repositorio es de una **organización** y no de tu usuario, el job `secrets` necesita una licencia gratuita de gitleaks. Pídela en [gitleaks.io](https://gitleaks.io) y guárdala en **Settings → Secrets and variables → Actions → New repository secret** con el nombre `GITLEAKS_LICENSE`.

✅ **Comprobación:** CI en verde.

---

## Paso 3 · Supabase (base de datos y fotos)

### 3.1 Crear el proyecto

1. En Supabase, **New project**.
   - **Name**: `arrima`.
   - **Database Password**: pulsa **Generate a password** y cópiala a tu libreta como `DB_PASSWORD`.
   - **Region**: **Central EU (Frankfurt)**, la misma que el backend, para que la latencia sea mínima.
   - **Create new project**. Tarda un par de minutos.

### 3.2 Datos de conexión

1. Arriba, pulsa **Connect**.
2. Elige la pestaña **Connection String** y, en **Method**, **Session pooler**.
   > No uses la *Direct connection*: es solo IPv6 y Render no llega. El *Transaction pooler* tampoco sirve para Spring y Flyway.
3. Verás una cadena así:
   ```
   postgresql://postgres.abcdefghijklmnop:[YOUR-PASSWORD]@aws-0-eu-central-1.pooler.supabase.com:5432/postgres
   ```
4. Sácale tres datos para tu libreta:
   - `DB_URL` = `jdbc:postgresql://` + el host + `:5432/postgres?sslmode=require`. Ejemplo:
     `jdbc:postgresql://aws-0-eu-central-1.pooler.supabase.com:5432/postgres?sslmode=require`
   - `DB_USERNAME` = lo que va entre `//` y `:`. Ejemplo: `postgres.abcdefghijklmnop`
   - `DB_PASSWORD` = la que generaste en el paso 3.1 (sin corchetes).

### 3.3 Cerrar la Data API

Arrima no la usa: todo pasa por el backend.

1. **Project Settings → Data API**.
2. Desactiva **Enable Data API** y guarda.

### 3.4 El almacén de fotos

1. En el menú de la izquierda, **Storage → New bucket**.
   - **Name**: `arrima` (exactamente así).
   - **Public bucket**: **desactivado**. Es importante: las fotos solo se ven con enlaces firmados por el backend.
   - En las opciones adicionales, si aparecen: **Restrict file size** a `5 MB` y **Allowed MIME types** `image/jpeg, image/png`. Es una barrera extra, porque el backend ya lo comprueba.
   - **Create**.

### 3.5 Las claves del proyecto

1. **Project Settings → General**: copia el **Project ID** (algo como `abcdefghijklmnop`). En tu libreta:
   `SUPABASE_URL` = `https://abcdefghijklmnop.supabase.co`
2. **Project Settings → API Keys**, sección **Secret keys**:
   - Si ya hay una, pulsa el ojo para verla y cópiala. Si no, **Add new secret key**, con el nombre `render`.
   - Empieza por `sb_secret_`. A tu libreta como `SUPABASE_SECRET_KEY`.
   > La clave *publishable* (`sb_publishable_…`) no se usa en Arrima. La secreta da acceso total: solo va a Render.

✅ **Comprobación:** tienes en la libreta `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SUPABASE_URL` y `SUPABASE_SECRET_KEY`. Las tablas no se crean a mano: las crea el backend la primera vez que arranca.

---

## Paso 4 · Brevo (correos)

Render gratuito bloquea el SMTP, así que los correos salen por la API de Brevo (300 al día gratis).

1. Al crear la cuenta, Brevo te pide datos de la organización y, a veces, verificar el móvil. Complétalo: **sin eso no envía correos**. Pon el club como organización.
2. **El remitente.** Arriba a la derecha, en el menú de la cuenta, entra en **Senders, Domains & Dedicated IPs → Senders → Add sender**.
   - **From name**: `Arrima`.
   - **From email**: el correo desde el que saldrán los avisos (puede ser el del club o el tuyo, aunque sea de Gmail).
   - Brevo te manda un correo de confirmación: ábrelo y confirma.
   - A tu libreta: `MAIL_SENDER_EMAIL` = ese correo.
   > Con una dirección gratuita (Gmail, Outlook…), algunos correos pueden acabar en spam. Para un club funciona. Si algún día molesta, la solución es un dominio propio verificado en Brevo (**Domains**); no hace falta cambiar nada en Arrima.
3. **La clave de la API.** En el mismo menú, **SMTP & API → API Keys → Generate a new API key**.
   - **Name**: `arrima-render`.
   - Cópiala en el momento, porque solo se muestra una vez. Empieza por `xkeysib-`. A tu libreta como `BREVO_API_KEY`.
4. Si en **Security → Authorised IPs** está activado el bloqueo de IPs desconocidas, desactívalo. Render gratuito no tiene una IP fija, y Brevo rechazaría los envíos.

✅ **Comprobación:** remitente confirmado (con una marca verde en **Senders**) y `BREVO_API_KEY` y `MAIL_SENDER_EMAIL` en la libreta.

---

## Paso 5 · Vercel (frontend)

Va antes que Render porque el backend necesita saber la dirección del frontend para los enlaces de los correos.

1. En Vercel, **Add New… → Project**.
2. En **Import Git Repository**, conecta GitHub si te lo pide y pulsa **Import** junto a `arrima`.
3. Configura el proyecto:
   - **Project Name**: define la dirección pública. Por ejemplo, `arrima-petanca-tuclub` da `https://arrima-petanca-tuclub.vercel.app`. Elige uno corto, que lo verán los jugadores en el QR.
   - **Framework Preset**: **Vite** (lo detecta solo).
   - **Root Directory**: pulsa **Edit**, elige `frontend` y **Continue**.
   - **Environment Variables**: ninguna.
   - **Deploy**.
4. Cuando termine, copia la dirección del dominio de producción (`https://<nombre>.vercel.app`) a tu libreta como `APP_URL`, sin barra al final.
5. Si la abres ahora, verás las bolas de petanca y «Despertando el servidor…». Es lo esperado: el backend aún no existe.

> El plan Hobby de Vercel es **solo para uso no comercial**. Para un club, perfecto. Si algún día cobras por Arrima, habrá que cambiar de plan.

✅ **Comprobación:** `APP_URL` en la libreta.

---

## Paso 6 · Render (backend)

### 6.1 Crear el servicio desde el Blueprint

1. En Render, **New → Blueprint**.
2. Conecta GitHub si te lo pide y elige el repositorio `arrima`.
3. **Blueprint Name**: `arrima`. **Branch**: `main`.
4. Render lee `render.yaml` y muestra el servicio `arrima-api` (Docker, Free, Frankfurt) y las variables que necesita. Rellénalas desde tu libreta:

   | Variable | Valor |
   |---|---|
   | `DB_URL` | de la libreta (paso 3.2) |
   | `DB_USERNAME` | de la libreta |
   | `DB_PASSWORD` | de la libreta |
   | `SUPABASE_URL` | de la libreta (paso 3.5) |
   | `SUPABASE_SECRET_KEY` | de la libreta |
   | `BREVO_API_KEY` | de la libreta (paso 4) |
   | `MAIL_SENDER_EMAIL` | de la libreta |
   | `APP_URL` | de la libreta (paso 5), sin barra final |
   | `VAPID_PUBLIC_KEY` | de la libreta (paso 1) |
   | `VAPID_PRIVATE_KEY` | de la libreta (paso 1) |
   | `VAPID_SUBJECT` | de la libreta (paso 1) |

   Las que no aparecen ya vienen resueltas en `render.yaml`: `JWT_SECRET` la genera Render solo, y `SUPABASE_BUCKET=arrima` y `MAIL_SENDER_NAME=Arrima` van fijas.
5. **Deploy Blueprint**.

### 6.2 El primer despliegue

1. Entra en el servicio `arrima-api` → **Logs**.
2. La primera vez tarda entre 5 y 10 minutos, porque compila con Maven dentro de Docker.
3. Busca estas líneas, en este orden:
   - `Successfully applied 5 migrations to schema "arrima"`: ha creado las tablas.
   - `Started ArrimaApplication in … seconds`: está en marcha. Con 0,1 CPU tarda en torno a minuto y medio.
   - Al principio salen unos avisos `[warning][aot…]`: son normales (la caché de arranque de Java ajustándose a la máquina).
4. Si en vez de eso sale un error, mira la tabla [Si algo falla](#si-algo-falla).

### 6.3 Comprobar el backend

1. Arriba, en la ficha del servicio, está su dirección, normalmente `https://arrima-api.onrender.com`. Cópiala a tu libreta como «URL del backend».
2. Ábrela en el navegador añadiendo `/api/health/db`, por ejemplo `https://arrima-api.onrender.com/api/health/db`. Debe responder:
   ```json
   {"status":"UP"}
   ```

### 6.4 Comprobar que Vercel llega al backend

1. Abre `APP_URL` + `/api/health`, por ejemplo `https://arrima-petanca-tuclub.vercel.app/api/health`.
2. Debe responder `{"status":"UP"}`. Si sale así, **sáltate el paso 6.5**.

### 6.5 Solo si Render le dio otra dirección al servicio

Si el nombre `arrima-api` estaba cogido, Render pone otro (por ejemplo `arrima-api-x7k2.onrender.com`) y Vercel estará apuntando a una dirección que no es la tuya.

1. Abre `frontend/vercel.json` y cambia esta línea por tu dirección de Render:
   ```json
   "destination": "https://arrima-api.onrender.com/api/:path*"
   ```
2. Sube el cambio:
   ```sh
   git add frontend/vercel.json
   git commit -m "Point the Vercel rewrite to the Render service"
   git push
   ```
3. Vercel vuelve a desplegar solo. Repite el paso 6.4.

### 6.6 Despliegues a partir de ahora

No tienes que hacer nada:

- **Backend:** cada `push` a `main` que cambie algo en `backend/` se despliega en Render, **solo si el CI de GitHub pasa** (`autoDeployTrigger: checksPass`).
- **Frontend:** cada `push` se despliega en Vercel.

✅ **Comprobación:** `/api/health/db` en Render y `/api/health` a través de Vercel responden `{"status":"UP"}`.

---

## Paso 7 · cron-job.org (que nada se duerma)

Render gratuito se duerme tras 15 minutos sin visitas (y tarda casi un minuto en despertar), y Supabase gratuito se pausa tras 7 días sin actividad. Una llamada cada 10 minutos evita las dos cosas.

1. En cron-job.org, **Create cronjob**.
   - **Title**: `Arrima`.
   - **URL**: tu URL del backend + `/api/health/db`, por ejemplo `https://arrima-api.onrender.com/api/health/db`. Va directo a Render; no hace falta pasar por Vercel. Escríbela entera, **con `https://`**. Con `http://`, Render responde `301 Moved Permanently` desde su proxy, cron-job.org lo cuenta como fallo y la llamada no llega al backend: Render se dormiría igual.
   - **Execution schedule**: **Every 10 minutes**.
   - **Notifications** (o *Advanced*): activa **Notify me when an execution fails** y **when it succeeds again**. Así te enterarás si el backend se cae.
   - **Create**.
2. Pulsa **Test run**: debe salir `200 OK`. Si Render estaba dormido, la primera puede agotar el tiempo; no pasa nada, porque ya lo has despertado. Repítelo.

> Render da 750 horas gratis al mes por cuenta. Un servicio siempre despierto gasta como mucho 744 (un mes de 31 días): cabe justo. **No crees otro servicio gratuito en la misma cuenta de Render** o se acabarían las horas antes de fin de mes.

✅ **Comprobación:** tras una hora, el historial del cronjob muestra 6 ejecuciones con `200`.

---

## Paso 8 · Dar de alta tu club

Registrarse exige un código de invitación de un solo uso. Lo generas tú en la base de datos.

1. En Supabase, **SQL Editor → New query**.
2. Copia y pega el contenido entero de `scripts/create-invitation.sql`.
3. Cambia la nota `'Club de Petanca ...'` por el nombre del club (es solo para ti).
4. **Run**. Abajo sale una fila con `invitation_code`, por ejemplo `3F9A1C07B2E4`. Cópialo: **no se puede volver a ver**, porque solo se guarda su hash. Caduca en 30 días.
5. En el móvil, abre `APP_URL` + `/registro`, por ejemplo `https://arrima-petanca-tuclub.vercel.app/registro`.
6. Rellena el código, el nombre del club, el correo del administrador (el que recibirá los correos de recuperación) y una contraseña de al menos 10 caracteres.
7. Entras directamente en el club. En **Club** puedes subir el logo y ajustar los puntos y valores por defecto.

Para cada club nuevo, repite los pasos 1 a 4 y pásale el código.

---

## Paso 9 · Probarlo todo de verdad

Hazlo con dos móviles si puedes (o un móvil y un ordenador).

| Prueba | Qué debe pasar |
|---|---|
| Entrar y salir | Tras «Salir» y volver a entrar, ves tus melés |
| Cerrar el navegador y volver a abrir | Sigues dentro sin escribir la contraseña |
| **¿Has olvidado la contraseña?** | Llega un correo (mira en spam) con un enlace a `APP_URL/restablecer#…`. Al elegir la nueva, puedes entrar con ella. Si no llega, mira [Si algo falla](#si-algo-falla) |
| Crear una melé de prueba y pegar una lista de WhatsApp | Salen los jugadores numerados |
| Sortear, generar el cuadro y apuntar un resultado | El contador se actualiza |
| **Compartir con los jugadores (QR)** y abrirlo en el otro móvil | Se ve la melé en directo sin cuenta. Al apuntar un resultado en el primero, el segundo se actualiza solo |
| Modo avión en el móvil del administrador y apuntar un resultado | Sale «Sin conexión · 1 cambio guardado en el móvil, se enviará solo». Al quitar el modo avión se envía |
| Entrega de premios con una foto | La foto se sube y se puede compartir por WhatsApp |
| **Instalar en Android** (Chrome) | En la pantalla de melés, «Instala Arrima en el móvil» → **Instalar**. Aparece el icono |
| **Instalar en iPhone** (Safari) | Compartir → «Añadir a pantalla de inicio» → «Añadir». Se abre a pantalla completa |
| Abrir la app instalada sin cobertura | Abre al momento y dice «Sin conexión»; al volver la cobertura, sigue sola |
| **Pagos**: melé con cuota, tocar nombres y marcar Sí/No, dejar a alguien sin marcar y pulsar «Generar equipos» | Avisa con los nombres sin marcar; solo sortea a los que han pagado; en la vista pública salen como «NO JUEGA» |
| **Temporizador**: crea una melé de prueba con **5 minutos** por partida, genera el cuadro y pulsa «Empezar partida 1» | La cuenta atrás se ve en los dos móviles y en la vista pública |
| En el móvil del jugador, «Avísame cuando se acabe el tiempo» y bloquea el móvil | A los 5 minutos llega la notificación «¡Tiempo! Partida 1 terminada». En iPhone solo con la app añadida a la pantalla de inicio |
| Con la app abierta al llegar a 0 | Suena la alarma, vibra (en Android) y sale «¡Tiempo!» a pantalla completa |

Al terminar, borra la melé de prueba (**Más opciones → Borrar esta melé**).

Lo que depende de cada móvil (compartir en WhatsApp, avisos con el móvil bloqueado, sonido) tiene su propia lista, para Android y para iPhone: [comprobacion-en-moviles.md](comprobacion-en-moviles.md).

### Verificación técnica (opcional)

| Comprobación | Resultado esperado |
|---|---|
| CI de GitHub | Verde |
| `https://<backend>/api/health/db` | `{"status":"UP"}` |
| `https://<vercel>/api/health` | `{"status":"UP"}` |
| `https://<vercel>/api/melees` sin sesión | `401` con `{"status":401,"code":"UNAUTHENTICATED"}` |
| `curl -sI https://<vercel>` | Cabeceras `content-security-policy`, `strict-transport-security`, `x-frame-options: DENY` |
| Historial de cron-job.org | Todo `200` |

---

## Si algo falla

| Síntoma | Causa probable | Solución |
|---|---|---|
| Render no arranca: `JWT_SECRET is not set` | La variable no se generó | Render → servicio → **Environment** → añade `JWT_SECRET` con **Generate** → **Save and deploy** |
| Render no arranca: `APP_URL is required` o `APP_URL must start with https://` | Falta `APP_URL` o le falta `https://` | Corrígela en **Environment** |
| Render no arranca: `BREVO_API_KEY is required` / `MAIL_SENDER_EMAIL is required` | Falta esa variable | Paso 4 y añádela en **Environment** |
| Render no arranca: `SUPABASE_URL is required…` | Falta la variable de Supabase | Paso 3.5 |
| Render no arranca: `VAPID_PUBLIC_KEY, VAPID_PRIVATE_KEY and VAPID_SUBJECT go together` | Has puesto solo alguna de las tres | Pon las tres (paso 1.3) o ninguna |
| No llega la notificación «¡Tiempo!» | Permiso denegado, iPhone sin la app instalada, o el ahorro de batería de Android | En el móvil: permitir notificaciones de la web (o de Arrima instalada). En iPhone, añadirla a la pantalla de inicio y pulsar «Avísame» desde ahí. En los logs de Render busca `Push service refused` |
| `Connection refused`, `Network is unreachable` o `UnknownHost` hacia la base de datos | Has usado la conexión directa (IPv6) o un puerto equivocado | Repite el paso 3.2 con **Session pooler**, puerto `5432` |
| `password authentication failed` | Usuario o contraseña mal | El usuario es `postgres.<project-id>`, no `postgres` a secas. Si dudas de la contraseña, en Supabase: **Project Settings → Database → Reset database password**, y actualízala en Render |
| La web se queda en «Despertando el servidor…» más de 3 minutos | El backend no está en marcha, o Vercel apunta a otra dirección | Mira los **Logs** de Render y repite los pasos 6.3 y 6.4 |
| No llega el correo de recuperación | Remitente sin confirmar, cuenta de Brevo sin activar, IPs bloqueadas o spam | En Brevo, **Transactional → Logs** muestra cada envío y su estado. Revisa el paso 4. En los logs de Render busca `Could not send the e-mail` |
| No se sube el logo ni las fotos | El bucket no se llama `arrima`, o la clave secreta está mal | Repite los pasos 3.4 y 3.5. Los logs de Render muestran el error de Supabase |
| El CI falla en el job `secrets` | gitleaks ha encontrado en el historial algo que parece un secreto | Lee el log del job. **Si es un secreto real, cámbialo ya en su servicio**: borrarlo del código no basta |
| cron-job.org da *timeout* de vez en cuando | Render tardó en responder | No pasa nada si es aislado. Si es continuo, mira los logs de Render |
| cron-job.org marca todas las ejecuciones como fallidas con `301 Moved Permanently` | La URL del cronjob empieza por `http://` | Edita el cronjob y pon la URL con `https://` (paso 7). Mientras tanto la llamada no llega al backend: Render se duerme y Supabase no ve actividad |

---

## Mantenimiento

### Actualizaciones

Dependabot abre cada semana *pull requests* con las versiones nuevas. El CI las prueba: si salen en verde, **Merge** y se despliegan solas. Si algo sale en rojo, no lo aceptes y míralo con calma.

### Copias de seguridad

Supabase gratuito no guarda copias que puedas descargar. Haz una de vez en cuando, por ejemplo una vez al mes y antes de cada temporada. Con Docker en tu ordenador no tienes que instalar nada más:

```sh
docker run --rm -it -v "$PWD":/backup postgres:17-alpine \
  pg_dump "host=<host-del-pooler> port=5432 dbname=postgres user=postgres.<project-id> sslmode=require" \
  --schema=arrima --format=custom --file=/backup/arrima-$(date +%F).dump
```

Te pide la contraseña de la base de datos. El fichero `.dump` contiene correos y datos de los clubes: guárdalo en un sitio privado, **nunca en el repositorio**. Las fotos se quedan en Supabase Storage y no van en la copia.

### Cambiar un secreto (si se filtra o por higiene)

| Secreto | Cómo cambiarlo | Efecto |
|---|---|---|
| Contraseña de la base de datos | Supabase → **Project Settings → Database → Reset database password**. Después, Render → **Environment** → `DB_PASSWORD` → **Save and deploy** | Unos minutos sin servicio mientras redespliega |
| `JWT_SECRET` | Render → **Environment** → `JWT_SECRET` → **Generate** → **Save and deploy** | Todos los administradores tienen que volver a entrar |
| `SUPABASE_SECRET_KEY` | Supabase → **API Keys** → crea una nueva → pégala en Render → **Save and deploy** → borra la vieja en Supabase | Ninguno |
| `BREVO_API_KEY` | Brevo → **API Keys** → genera una nueva → pégala en Render → **Save and deploy** → borra la vieja | Ninguno |

Si un secreto llega a aparecer en GitHub, en un chat o en una captura, **cámbialo inmediatamente**: hay que darlo por comprometido aunque lo borres.

### Dónde mirar si algo va mal

- **Render → Logs:** errores del backend, avisos de límites de intentos (`Rate limit … reached`) y de sesiones robadas (`Refresh token reused`).
- **Brevo → Transactional → Logs:** los correos enviados.
- **cron-job.org:** el historial de si el backend responde.
- **GitHub → Actions:** el CI de cada cambio.
