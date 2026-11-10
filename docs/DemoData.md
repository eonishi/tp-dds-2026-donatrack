# Explore DonaTrack with demo data

Enable the optional dataset in a **disposable development database**. It adds linked accounts, donations in all seven states, needs and assignment proposals without sending messages or calling logistics during initialization. It is disabled by default.

## Quick start

1. Set `DONATRACK_DEMO_DATA=true` in your development `.env`. Both Compose files already load this file. For local HTTP, also set `DONATRACK_COOKIE_SECURE=false`.
2. Rebuild and restart the services:
   ```sh
   docker compose --profile local up -d --build
   ```
   Review `DB_URL` first: this command uses the database selected by your `.env`; it does not create an isolated demo database automatically.
3. Open `http://localhost:7070/login` and use an account below. For an existing non-Compose development runner, export `DONATRACK_DEMO_DATA=true` before starting `DonaTrackApplication`.
4. Set the flag back to `false` after the first successful startup. Existing demo records remain available.

Do **not** enable this in production: credentials below are public. Do not configure real Resend/Twilio credentials in a demo environment; later user actions can send notifications through the normal application, even though initialization itself never sends them.

## Accounts

All four accounts use the password **`Demo12345!`**.

| Email | Role | Linked record |
|---|---|---|
| `demo.admin@example.test` | Administrator | All donation-side records |
| `demo.humana@example.test` | Donor | Ana Ejemplo, human donor |
| `demo.juridica@example.test` | Donor | Cooperativa Solidaria, legal donor |
| `demo.entidad@example.test` | Beneficiary | Comedor Encuentro and its needs |

Existing `@donatrack.local` demo accounts are unchanged. Use these new linked accounts to explore the populated dashboards.

## Included scenarios

| Data | Coverage |
|---|---|
| 22 donations | `EN_DEPOSITO`, `ASIGNACION_REALIZADA`, `LISTA_PARA_ENTREGAR`, `EN_TRASLADO`, `ENTREGADA` (four), `ENTREGA_FALLIDA`, `VENCIDA` |
| Goods | Rice with future/past expiration; new/used chairs; new coats. Every donation has exactly one subcategory, expiration and condition. |
| 7 needs | Five extraordinary, one weekly and one monthly recurrent; pending, partially fulfilled and fulfilled examples |
| 2 assignment proposals | One pending and one confirmed |
| History | Valid domain transitions; failed delivery includes a reason. Completed deliveries update the corresponding needs. |
| 14 inbox notices | Both donors and the beneficiary have read/unread examples; no external delivery |

### Explore each role

Both donor accounts now have donations in **all seven states**, current shipments, completed/failed delivery histories and their own notification inbox. Use `/donante/donaciones`, `/donante/seguimiento` and `/donante/notificaciones` after logging in.

`demo.entidad@example.test` is **[DEMO] Comedor Encuentro**. Its `/entidad/necesidades`, `/entidad/seguimiento`, `/entidad/recepciones` and `/entidad/notificaciones` views include pending/partial/fulfilled needs, incoming shipments, completed/failed deliveries and notices. All three accounts also have their role dashboard.

Tracking can display the stationary simulated `DEMO-01` position while the logistics demo is running. It is not a live journey or a destination-linked logistics route; the limitation below still applies.

The additive `donations-demo-views-v2` upgrade also runs on databases initialized with v1. It adds 14 donations, two pending needs and 12 notices once, without replacing existing records or resetting passwords/read status. Changed demo account associations abort this upgrade instead of modifying another owner's records.

Dates are relative to the **first execution of each dataset version** (v1 initialization or the v2 upgrade). Expiration and statuses are not refreshed on restart, so your interactions are preserved. Needs have no status enum: fulfillment is derived from received versus requested quantity.

## Persistence and limits

The initial dataset and applicable `donations-demo-v1` / `donations-demo-views-v2` markers commit in one transaction. Repeated startups do not duplicate or reset records. Account, donor-document or incompatible catalog-name collisions abort initialization without overwriting existing records. Existing application data is never deleted. To start over, use a fresh disposable database rather than deleting marker records from a used database.

## Logistics demo data

The logistics process uses the same `DONATRACK_DEMO_DATA=true` flag, with its own atomic `logistics-demo-v1` marker in its own schema. Restarting does not reset edited trucks or routes. Reserved plate/account collisions abort without overwriting records.

| Data | Coverage |
|---|---|
| 3 trucks | `DEMO-01`: 18 m³ / 3500 kg; `DEMO-02`: 40 m³ / 10000 kg; `DEMO-03`: 10 m³ / 1500 kg, without a route |
| 2 training routes | One active (`DEMO-01`), one pending (`DEMO-02`), with assigned drivers |
| 2 logistics accounts | `demo.chofer@example.test` and `demo.acompanante@example.test`, password `Demo12345!`; separate logistics login |
| Simulated position | `DEMO-01` at Buenos Aires coordinates, stationary; reloaded into the in-memory monitor on startup only if this truck still has an active route |

**Training routes intentionally have no destinations or donation IDs.** Donation IDs are generated in the other service; this seed never invents beneficiary references, rewrites donation data or calls another service. These routes demonstrate fleet listing, driver assignments and route lifecycle, **not end-to-end delivery of the donation fixtures**. The `DEMO-01` identifier used by donation examples now also has a fleet record, but those examples are not attached to these routes.

The domain has only an `enCurso` flag: pending and completed routes are both `false`. It has no refrigeration or availability field; no such states are fabricated. The position is simulated, not a live GPS feed. Starting a route later through the normal UI still uses the normal inter-service callback; initialization itself never dispatches one.
