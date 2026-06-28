# The Law of Embodied Perception

**The rule system that governs what Metis is allowed to sense — and what it must never expose.**

This document is normative. It is meant to be read by humans *and* by AI agents.
Any agent building on, extending, or consuming Metis should be able to orient on
it and decide, on its own, whether a given piece of data is legitimate to surface.

---

## 0. Essence

Metis is a **sensory organ**, not a data tap. It models what an embodied
observer — a person standing in the world — can see, watch, and reasonably
infer. It does not read minds, it does not read memory, it does not read numbers
off an invisible status screen.

If a second human player, standing in the same world next to the subject, could
not know it, **Metis must not let an agent know it either.**

---

## 1. The Cardinal Rule

> **A Metis-fed agent must never be able to know anything about another player
> that a second human player, embodied in the same world, could not also
> perceive or reasonably infer.**

Everything below is a consequence of this one rule. When a future feature is
ambiguous, resolve it by asking the only question that matters:

> *"Could a person standing there, with eyes, actually know this?"*

If the honest answer is no, it does not belong in Metis.

### 1.1 The privacy corollary

> **Internal state is private.** No observer can read another being's health,
> hunger, breath, experience, active effects, or thoughts. The subject knows it;
> everyone else learns it **only if the subject chooses to say it** — and saying
> is the domain of `coda` (sound/voice out), not of Metis.

This is why health & co. are not "gated" in Metis — they are simply **absent**.
There is no endpoint to gate.

### 1.2 Why this matters

The MCDS bots are meant to feel like **embodied peers** — actors who share the
world with the player under roughly the same perceptual limits a teammate would
have. A bot that reads the player's exact HP, or their look-direction from across
the map through a wall, stops feeling like a companion. That's the immersion the
Cardinal Rule protects.

This is a *guideline for what data is honest to expose*, not a vow of maximal
realism. Metis and MCDS are also a **tool**, and the tool has to stay usable —
where immersion and usability pull against each other, that balance is MCDS's to
strike, case by case. The rule below is the floor, not a mandate to make
everything maximally immersive.

---

## 2. Division of Responsibility

Metis and MCDS have strictly separated jobs. **Do not blur them.**

| | **Metis** (this project) | **MCDS** (the orchestrator) |
|---|---|---|
| Role | Dumb, generic **sensor** | Smart **orchestration & gating** |
| Does | Reads client-observable data, exposes it raw over the local API, **tags each datum with a perception class** | Decides *if / when / how* a bot may use a datum: applies distance gates, line-of-sight checks, accuracy degradation, timing |
| Contains logic? | **No** — except one thing (below) | Yes — all of it |
| Enforces | **Omission only**: never exposes `PRIVATE`-class data | Everything else |

**The one thing Metis enforces:** it refuses to expose `PRIVATE`-class data at
all. There is no switch, no config, no gate for it — it does not exist as an
endpoint. Everything else Metis exposes freely and **labels honestly**, then
gets out of the way.

**Metis never computes a gate.** It never checks line of sight, never decides
"too far," never decides "the player is busy." It hands MCDS the raw observable
facts *and* the perception class, and MCDS — which owns the bot's body, position,
and intent — does the gating. Zack, zack, zack: here is the data, here is its
class, done.

**MCDS owns how the data is used — including consuming it raw.** The perception
classes below are *guidance* about what each datum honestly represents, not
restrictions Metis imposes on MCDS. Some data is meant to be consumed raw and
ungated, and that is correct: e.g. the player's exact position feeds the
radio/walkie-talkie audio model in `coda`, where real player↔bot distance
determines signal strength. That is a legitimate, intended raw use — it doesn't
hand the bot a perceptual super-power, it drives a simulation. Likewise "come
here," typed coordinates, and saved locations are normal, wanted tool behaviour.
Metis draws exactly one line — expose or don't. What happens after is MCDS's
call.

---

## 3. The Perception Classes

Every datum Metis can expose falls into exactly one of five classes. The class
is guidance for the consumer: it describes *what kind of perception the datum
honestly represents*, so MCDS can decide what gating (if any) is faithful before
that datum informs a bot's behaviour.

Metis **tags** each field/endpoint with its class. MCDS **decides** what to do
with the tag — gate it, degrade it, or consume it raw where that's the right call.

### `PRIVATE` — internal state, perceivable by no one
Health, absorption, hunger, saturation, breath/air, exact experience, active
status effects, the full hidden inventory, intentions not yet acted on.

- **Metis action:** ❌ **Never exposed.** No endpoint exists.
- **Gate:** n/a (cannot leak what is not surfaced).
- **Rationale:** a person cannot read another person's vitals or mind.

### `AMBIENT` — the observer's *own* environment
Time of day / day-night, moon phase, weather, sky/block light level, the
dimension and biome **at the perceiving body's own location**.

- **Metis action:** ✅ Exposed, **sourced from the perceiving body itself.**
- **Gate:** none — these are the observer's *own* senses, not a claim about
  anyone else.
- **Critical framing:** ambient data belongs to whoever's body senses it. A bot
  that wants to know "is it raining / is it night / how dark is it here" must
  read **its own** ambient, at **its own** position — **never** inherited or
  relayed from the player. Each Metis instance reports *its own* sky. The bot
  feels its own weather.

### `BROADCAST` — out-of-band common knowledge
Information the game itself hands every player through a UI affordance,
regardless of proximity: the tab / player list (who is online, latency),
server-side scoreboard sidebar, a player's name and UUID as shown on nameplates
and in tab.

- **Metis action:** ✅ Exposed.
- **Gate:** none required by the Cardinal Rule (every player already has this
  channel), but consumers should treat it as **meta, low-embodiment** knowledge.
- **Note:** allowed, but it is *not* the kind of embodied sensing Metis exists
  for. Use sparingly; prefer real perception where a choice exists.

### `DISTANT` — observable at range (silhouette & gross behaviour)
What you can read off a body you can *see* from a distance, without precision:
that someone is walking / sprinting / sneaking / swimming / climbing / flying
with an elytra / riding a vehicle, their gross heading and motion, that they are
on fire or in water. Their rough whereabouts when in view.

- **Metis action:** ✅ Exposed raw.
- **Suggested gate (MCDS's call):** subject plausibly **in view**. Coarse data,
  so a lenient gate fits — reading a silhouette through terrain or out of render
  range is the thing to avoid.
- **Why it's great:** this is the tactical goldmine. A bot that sees *how* the
  player is moving can adapt its own navigation automatically — match a sprint,
  follow onto a boat, hang back when they sneak. Pure embodied tactics.

### `PROXIMATE` — observable only up close, with line of sight
Fine detail that only a nearby observer with a clear sightline can make out:
the item held in hand / off-hand and worn (visible) equipment, the precise block
the subject is looking at (crosshair target), the block they are standing on,
fine head/look direction.

- **Metis action:** ✅ Exposed raw — **no gating, no logic, Metis just sends it.**
- **Suggested gate (MCDS's call):** distance + line-of-sight, with accuracy that
  falls off as distance grows. Up close and in sight: full fidelity — the bot may
  anticipate, pre-walk toward where the player is aiming, react to the held tool.
  Far away or without sightline: the detail should fade out. The gate logic, if
  any, is **built in MCDS, not here.**
- **Rationale:** you *can* see what a teammate is wielding or pointing at — but
  only when you are near enough and actually looking. At a hundred blocks you
  cannot.

---

## 4. Classification of Known Data Points

This is the working register. Each row is a concrete client-observable datum,
its perception class, whether Metis exposes it, and the gate that would be
*faithful* for MCDS to apply (guidance, not a Metis mandate). Extend this table
as new senses are proposed; the class is the orientation, MCDS makes the call.

| Datum | Class | Metis exposes | Suggested perception gate (MCDS's call) |
|---|---|---|---|
| Health / absorption | `PRIVATE` | ❌ | — (never surfaced) |
| Hunger / saturation / exhaustion | `PRIVATE` | ❌ | — |
| Air / breath | `PRIVATE` | ❌ | — |
| Experience level / progress | `PRIVATE` | ❌ | — |
| Active status effects | `PRIVATE` | ❌ | — |
| Full carried inventory (non-visible) | `PRIVATE` | ❌ | — |
| Time of day / day-night | `AMBIENT` | ✅ (self) | none — read at the bot's own position |
| Moon phase | `AMBIENT` | ✅ (self) | none — bot's own sky |
| Weather (rain / thunder) | `AMBIENT` | ✅ (self) | none — bot's own weather, never relayed |
| Light level (sky / block) | `AMBIENT` | ✅ (self) | none — at the bot's own location |
| Dimension / biome (of the sensing body) | `AMBIENT` | ✅ (self) | none |
| Tab / player list, online players | `BROADCAST` | ✅ | none (meta, low-embodiment) |
| Player latency / ping | `BROADCAST` | ✅ | none (meta) |
| Username / UUID | `BROADCAST` | ✅ | none (nameplate/tab knowledge) |
| Server scoreboard sidebar | `BROADCAST` | ✅ | none (meta) |
| Movement & pose state (sprint/sneak/swim/climb/elytra/vehicle) | `DISTANT` | ✅ | in-view |
| Gross velocity / heading | `DISTANT` | ✅ | in-view |
| On-fire / in-water / visibly burning | `DISTANT` | ✅ | in-view |
| Position (x/y/z) | `DISTANT` | ✅ | MCDS's call — legitimately consumed **raw** for some uses (e.g. radio signal-strength distance in `coda`); how it informs bot *navigation/knowledge* is MCDS policy |
| Held item (main / off hand) | `PROXIMATE` | ✅ | distance + line-of-sight, degrade with range |
| Worn / visible equipment (armor) | `PROXIMATE` | ✅ | distance + line-of-sight, degrade with range |
| Crosshair target (looked-at block/entity) | `PROXIMATE` | ✅ | distance + line-of-sight, **degrade with range** |
| Block the subject is standing on | `PROXIMATE` | ✅ | distance + line-of-sight |
| Fine look / head direction (yaw/pitch) | `PROXIMATE` | ✅ | distance + line-of-sight, degrade with range |
| Chat / spoken messages | — | ❌ (belongs to `coda`) | — (hearing/voice is another organ) |
| Block/region scan beyond what's in view | — | ❌ | — (this is x-ray, not perception) |

---

## 5. The Line-of-Sight Gate (lives in MCDS)

Some `DISTANT`/`PROXIMATE` data is most faithful when the bot can actually *see*
the subject. A natural pattern: a small function where the bot tests whether it
has a clear sightline to the player, and the proximate/distant senses sharpen as
that holds.

**That function lives in MCDS, not Metis.** But note the clean handoff:

- **Metis supplies the geometry inputs** — its own body's position and the
  observable position/heading of the subject — as raw `DISTANT`/`PROXIMATE`
  facts.
- **MCDS computes the gate** — distance, raycast/occlusion line-of-sight,
  accuracy falloff — using the bot's body, which only MCDS owns.

Metis must never try to compute "can the bot see the player." It cannot — it
does not know where the bot is. It only knows what its own body perceives.

---

## 6. How a Consuming Agent Should Use This

1. Read the datum **and its perception class** from the Metis API.
2. Look up the suggested gate for that class (this document).
3. Decide what's faithful for your use, using your *own* body's situation
   (position, sightline) — gate it, degrade it, or consume it raw where that's
   the legitimate, intended use.
4. Keep the balance: the Cardinal Rule is the floor for *what is honest to know*;
   beyond that, usability matters too. Don't cripple the tool in the name of
   maximal realism, and don't hand a bot a perceptual super-power it shouldn't have.

`PRIVATE` data never arrives, so there is nothing to handle — that is by design.

---

## 7. Decision Procedure for New Senses

Before adding any new datum to Metis, walk this:

1. **Could a co-located human perceive or infer it?** No → it's `PRIVATE`,
   stop, do not expose.
2. **Is it the observer's own environment?** Yes → `AMBIENT`, expose self-sourced.
3. **Does the game already broadcast it to every player via UI?** Yes →
   `BROADCAST`, expose (but it's meta, low value).
4. **Is it readable from a distance as silhouette/behaviour?** Yes → `DISTANT`,
   expose raw, MCDS gates on in-view.
5. **Does it need nearness + a clear sightline to make out?** Yes → `PROXIMATE`,
   expose raw, MCDS gates on distance + line-of-sight with accuracy falloff.

If a datum seems to span classes, **pick the most restrictive** that still keeps
it honest, and document why in §4.

---

## 8. Invariants (never violate)

- Metis contains **no gating logic**. (Only omission of `PRIVATE`.)
- Metis never relays one body's `AMBIENT` as another body's.
- Metis never claims to know whether a bot can see something — it cannot.
- `PRIVATE` data has **no endpoint**. Not gated, not configurable — absent.
- Hearing/voice is `coda`; memory is `blackbox`; knowledge is `thread`. Metis is
  sight. Stay in your organ.
- Every exposed datum carries its perception class. No unlabeled data.

---

*Metis is the organ of perception in the MCDS ecosystem. Its discipline is not
what it can read, but what it refuses to.*
