# Arrima

Gestor de melés de petanca para clubes: inscripción, sorteo de equipos, cuadro de pistas, resultados, la Internacional, premios e histórico, desde el móvil. Es una PWA con backend en Spring Boot.

```
backend/    API REST · Java 25 · Spring Boot 4.1 · PostgreSQL · Flyway
frontend/   PWA · React · Vite · TypeScript · Tailwind
render.yaml Blueprint de Render (backend)
.github/    CI (tests + búsqueda de secretos) y Dependabot
.githooks/  pre-commit con gitleaks
```

## Arquitectura en producción (todo en planes gratuitos)

```
 móvil ──► Vercel (frontend) ── /api/* (rewrite) ──► Render (Spring Boot) ──► Supabase (PostgreSQL + fotos)
                                                          ▲        └────────► Brevo (correos)
 cron-job.org ── cada 10 min: GET /api/health/db ─────────┘
```

- **Vercel** sirve la PWA y reenvía `/api/*` al backend. Para el navegador todo es el mismo dominio, así que las cookies funcionan también en Safari de iOS y no hace falta CORS.
- **Render** ejecuta el backend en Docker (512 MB, 0,1 CPU). Si pasa 15 minutos sin tráfico se duerme, y despertar cuesta un par de minutos.
- **Supabase** aloja PostgreSQL y las fotos (en un bucket privado). Pausa el proyecto si pasa 7 días sin actividad en la base de datos.
- **Brevo** envía los correos de recuperación de contraseña por su API HTTP (Render gratuito bloquea el SMTP).
- **cron-job.org** llama cada 10 minutos a `/api/health/db`, que hace una consulta real. Así Render no se duerme y Supabase no se pausa. Un servicio despierto 24/7 gasta unas 744 h al mes, dentro de las 750 h gratuitas de Render.

## Desarrollo local

### Requisitos

| Herramienta | Versión | Comprobar |
|---|---|---|
| JDK | 25 | `/usr/libexec/java_home -v 25` |
| Docker Desktop | arrancado | `docker info` |
| Node.js | 24 | `node -v` |
| gitleaks | cualquiera | `gitleaks version` (`brew install gitleaks`) |

Si tu `java` por defecto es otro (por ejemplo, el 21), apunta `JAVA_HOME` al 25 en cada terminal donde trabajes con el backend:

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
```

### Primera vez

```sh
git config core.hooksPath .githooks        # activa la revisión de secretos antes de cada commit
cp backend/.env.example backend/.env       # y rellénalo (en local, cualquier usuario y contraseña)
```

### Arrancar

```sh
# Terminal 1: base de datos + backend (http://localhost:8080)
cd backend
docker compose up -d                       # PostgreSQL 17 en el puerto 5433
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 2: frontend (http://localhost:5173)
cd frontend
npm install
npm run dev
```

El frontend reenvía `/api/*` al backend gracias al proxy de Vite, igual que hace Vercel en producción.

El perfil `local` lee `backend/.env`, guarda las imágenes en `backend/.local-files/` en vez de en Supabase y, en vez de enviar correos, los escribe en el log del backend: para probar «¿Has olvidado la contraseña?», copia de ahí el enlace `http://localhost:5173/restablecer#…`.

### Crear un club en local

Registrar un club exige un código de invitación. Para crear uno en tu base de datos local:

```sh
cd backend
set -a && . ./.env && set +a
docker compose exec -T postgres psql -U "$DB_USERNAME" -d arrima -tA < ../scripts/create-invitation.sql
```

Abre http://localhost:5173/registro y usa el código que sale.

### Tests

```sh
cd backend && ./mvnw verify               # unitarios + integración (Testcontainers: necesita Docker)
cd frontend && npm run lint && npm test && npm run build
```

### Probar la PWA en local

El *service worker* solo existe en el build de producción (en desarrollo estorbaría a la recarga de Vite):

```sh
cd frontend
npm run build && npx vite preview         # http://localhost:4173, con el mismo proxy a /api
```

Qué hace la PWA:

- `public/manifest.webmanifest` e iconos en `public/icons/` (generados desde `pwa/app-icon.svg`; si cambias el SVG, vuelve a exportar los PNG a 180, 192 y 512 px).
- `pwa/sw.js` guarda en el móvil la página, el código, los estilos y los iconos, para que la app abra al momento aunque no haya cobertura. **Nunca toca `/api`**: resultados, tiempo real y sesión van siempre a la red. Cada build lo regenera con una versión nueva (`pwa/serviceWorkerPlugin.ts`) y los móviles se actualizan solos la siguiente vez que abren la app.
- En la pantalla de melés aparece «Instala Arrima en el móvil»: en Android abre el diálogo de instalación de Chrome; en iPhone explica los pasos de Safari (Compartir → «Añadir a pantalla de inicio»).

Nota para pruebas automáticas con el WebKit de Playwright: no guarda cookies `Secure` en `http://localhost`, así que al recargar la página se pierde la sesión. En Safari real con HTTPS no pasa.

## Variables de entorno

Se definen en `backend/.env` en local y en el panel de Render en producción. **Nunca van al repositorio.** En `backend/.env.example` están los nombres y el formato de cada una.

| Variable | Qué es | Ejemplo de formato |
|---|---|---|
| `DB_URL` | URL JDBC de PostgreSQL | `jdbc:postgresql://aws-0-eu-central-1.pooler.supabase.com:5432/postgres?sslmode=require` |
| `DB_USERNAME` | Usuario de la base de datos | `postgres.abcdefghijklmnop` |
| `DB_PASSWORD` | Contraseña de la base de datos | (la de tu proyecto de Supabase) |
| `JWT_SECRET` | Clave que firma las sesiones: base64 de 32 bytes aleatorios. En Render la genera el Blueprint | salida de `openssl rand -base64 32` |
| `SUPABASE_URL` | URL del proyecto de Supabase (solo producción) | `https://abcdefghijklmnop.supabase.co` |
| `SUPABASE_SECRET_KEY` | Clave secreta de Supabase (solo producción) | `sb_secret_...` |
| `SUPABASE_BUCKET` | Bucket privado de las imágenes (opcional) | `arrima` (por defecto) |
| `BREVO_API_KEY` | Clave de la API de Brevo, para los correos (solo producción) | `xkeysib-...` |
| `MAIL_SENDER_EMAIL` | Remitente de los correos, verificado en Brevo (solo producción) | `tuclub@gmail.com` |
| `MAIL_SENDER_NAME` | Nombre del remitente (opcional) | `Arrima` (por defecto) |
| `APP_URL` | Dirección pública del frontend, para los enlaces de los correos (solo producción) | `https://arrima.vercel.app` |
| `VAPID_PUBLIC_KEY` | Clave pública de Web Push, para el aviso «¡Tiempo!» (opcional) | salida de `scripts/generate-vapid-keys.sh` |
| `VAPID_PRIVATE_KEY` | Clave privada de Web Push (opcional, va con la pública) | salida de `scripts/generate-vapid-keys.sh` |
| `VAPID_SUBJECT` | Contacto para los servicios de push (opcional, va con las claves) | `mailto:tu@correo.com` |
| `PORT` | Puerto HTTP. Lo pone Render solo | — |

## Despliegue

La guía completa, paso a paso y con las comprobaciones de cada servicio, está en **[docs/DESPLIEGUE.md](docs/DESPLIEGUE.md)**. El orden es GitHub → Supabase → Brevo → Vercel → Render → cron-job.org → alta del primer club.

La revisión de seguridad (OWASP Top 10) está en [docs/seguridad-owasp.md](docs/seguridad-owasp.md).

## Secretos

- Ningún secreto va en el código ni en el historial. Todo va en `backend/.env` (ignorado por git) o en el panel de Render.
- Hay tres barreras:
  1. El *hook* `pre-commit` con gitleaks, en tu máquina.
  2. El job `secrets` del CI, que revisa todo el historial.
  3. La *push protection* de GitHub.
- **Si alguna vez se filtra un secreto, cámbialo inmediatamente** (por ejemplo, regenera la contraseña de la base de datos en Supabase y actualízala en Render). Borrarlo del historial no basta: hay que darlo por comprometido.
