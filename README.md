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
 móvil ──► Vercel (frontend) ── /api/* (rewrite) ──► Render (Spring Boot) ──► Supabase (PostgreSQL)
                                                          ▲
 cron-job.org ── cada 10 min: GET /api/health/db ─────────┘
```

- **Vercel** sirve la PWA y reenvía `/api/*` al backend. Para el navegador todo es el mismo dominio, así que las cookies funcionan también en Safari de iOS y no hace falta CORS.
- **Render** ejecuta el backend en Docker (512 MB, 0,1 CPU). Si pasa 15 minutos sin tráfico se duerme, y despertar cuesta un par de minutos.
- **Supabase** aloja PostgreSQL. Pausa el proyecto si pasa 7 días sin actividad en la base de datos.
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
./mvnw spring-boot:run

# Terminal 2: frontend (http://localhost:5173)
cd frontend
npm install
npm run dev
```

El frontend reenvía `/api/*` al backend gracias al proxy de Vite, igual que hace Vercel en producción.

### Tests

```sh
cd backend && ./mvnw verify               # unitarios + integración (Testcontainers: necesita Docker)
cd frontend && npm run lint && npm test && npm run build
```

## Variables de entorno

Se definen en `backend/.env` en local y en el panel de Render en producción. **Nunca van al repositorio.** En `backend/.env.example` están los nombres y el formato de cada una.

| Variable | Qué es | Ejemplo de formato |
|---|---|---|
| `DB_URL` | URL JDBC de PostgreSQL | `jdbc:postgresql://aws-0-eu-central-1.pooler.supabase.com:5432/postgres?sslmode=require` |
| `DB_USERNAME` | Usuario de la base de datos | `postgres.abcdefghijklmnop` |
| `DB_PASSWORD` | Contraseña de la base de datos | (la de tu proyecto de Supabase) |
| `PORT` | Puerto HTTP. Lo pone Render solo | — |

Las fases siguientes añadirán más (claves JWT, Supabase Storage, correo). Cada una se documentará aquí.

## Despliegue paso a paso

Hazlo en este orden: cada servicio necesita algún dato del anterior.

### 1. GitHub

1. Crea un repositorio vacío en GitHub, sin README ni `.gitignore` (ya los tenemos).
   - **Público**: Actions ilimitado y protección de secretos gratuita.
   - **Privado**: 2.000 minutos de Actions al mes, de sobra para este proyecto.
2. Sube el código:
   ```sh
   git remote add origin https://github.com/<tu-usuario>/arrima.git
   git push -u origin main
   ```
3. Si el repo es público, en **Settings → Advanced Security** activa **Secret Protection** y **Push protection** si no lo están. GitHub bloqueará cualquier `push` que contenga una clave conocida. En repos privados es de pago; ahí te protegen el *hook* local y el CI.
4. En la pestaña **Actions** comprueba que el workflow **CI** pasa en verde.

### 2. Supabase (base de datos)

1. En [supabase.com](https://supabase.com), **New project**.
   - **Region**: *Central EU (Frankfurt)*, la misma región que el backend en Render, para que la latencia sea mínima.
   - **Database password**: genera una larga y guárdala en tu gestor de contraseñas. La necesitarás en el paso 3.
2. Cuando el proyecto esté listo, pulsa **Connect** (arriba) y elige **Session pooler**. Es la única conexión gratuita que funciona por IPv4, que es lo que tiene Render; la conexión directa es solo IPv6.

   Verás algo así:
   ```
   postgresql://postgres.abcdefghijklmnop:[YOUR-PASSWORD]@aws-0-eu-central-1.pooler.supabase.com:5432/postgres
   ```
   De ahí salen las tres variables:
   - `DB_URL` = `jdbc:postgresql://` + **host** + `:5432/postgres?sslmode=require`
   - `DB_USERNAME` = lo que va antes de `:` → `postgres.abcdefghijklmnop`
   - `DB_PASSWORD` = la contraseña del punto 1
3. **Desactiva la Data API.** En **Project Settings → Data API**, apaga *Enable Data API*. Arrima no la usa: todo pasa por el backend. Aunque las tablas viven en el esquema `arrima`, que no está expuesto, y tienen RLS activado, cerrar la puerta que no se usa es lo más seguro.

Las tablas no se crean a mano. Las crea Flyway la primera vez que arranca el backend.

### 3. Render (backend)

1. En [render.com](https://render.com), **New → Blueprint** y conecta tu repositorio de GitHub.
2. Render lee `render.yaml` y propone el servicio `arrima-api` (Docker, plan Free, Frankfurt). Te pedirá `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`: pega los valores del paso 2. **Apply**.
3. El primer despliegue tarda unos minutos, porque compila con Maven dentro de Docker. Después el backend tarda en torno a un minuto en arrancar con 0,1 CPU. En **Logs** debe aparecer `Successfully applied 1 migration` y luego `Started ArrimaApplication`.
4. Comprueba en el navegador `https://arrima-api.onrender.com/api/health/db`. Debe responder `{"status":"UP"}`.
   - Si Render le ha dado otra URL al servicio (porque el nombre estaba cogido), cámbiala en `frontend/vercel.json` (`destination`) y haz commit.
5. Despliegues automáticos: con `autoDeployTrigger: checksPass`, cada `push` a `main` se despliega **solo si el CI de GitHub pasa**, y solo si cambia algo en `backend/`.

### 4. Vercel (frontend)

1. En [vercel.com](https://vercel.com), **Add New → Project** e importa el repositorio.
2. **Root Directory**: `frontend`. Vercel detecta Vite. No hace falta ninguna variable de entorno. **Deploy**.
3. Abre `https://<tu-proyecto>.vercel.app`. Verás la pantalla de bolas y después «Conectado al servidor».
4. Comprueba también `https://<tu-proyecto>.vercel.app/api/health`. Si responde `{"status":"UP"}`, el *rewrite* hacia Render funciona.

> El plan Hobby de Vercel es **solo para uso no comercial**. Para un club, perfecto. Si algún día cobras por Arrima, habrá que cambiar de plan o de proveedor.

### 5. cron-job.org (mantener todo despierto)

1. Crea una cuenta en [cron-job.org](https://cron-job.org). Es gratuito.
2. **Create cronjob**:
   - **URL**: `https://arrima-api.onrender.com/api/health/db`. Va directo a Render: no hace falta pasar por Vercel.
   - **Schedule**: cada 10 minutos.
   - **Notifications**: activa el aviso por correo cuando falle. Así te enterarás si el backend se cae.
3. Guarda y pulsa **Test run**. Debe salir `200`. Si Render estaba dormido, la primera llamada puede agotar el tiempo de espera: no pasa nada, porque ya lo habrá despertado y la siguiente irá bien.

### 6. Verificación final

| Comprobación | Resultado esperado |
|---|---|
| CI de GitHub | Verde |
| `https://arrima-api.onrender.com/api/health/db` | `{"status":"UP"}` |
| `https://<tu-proyecto>.vercel.app` en el móvil | Bolas y luego «Conectado al servidor» |
| `https://<tu-proyecto>.vercel.app/api/clubs` | `403` (todo lo que no está abierto se deniega) |
| Historial de cron-job.org tras una hora | 6 ejecuciones con `200` |

## Secretos

- Ningún secreto va en el código ni en el historial. Todo va en `backend/.env` (ignorado por git) o en el panel de Render.
- Hay tres barreras:
  1. El *hook* `pre-commit` con gitleaks, en tu máquina.
  2. El job `secrets` del CI, que revisa todo el historial.
  3. La *push protection* de GitHub.
- **Si alguna vez se filtra un secreto, cámbialo inmediatamente** (por ejemplo, regenera la contraseña de la base de datos en Supabase y actualízala en Render). Borrarlo del historial no basta: hay que darlo por comprometido.
