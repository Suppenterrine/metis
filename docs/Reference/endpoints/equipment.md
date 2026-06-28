# `GET /api/equipment`

Held items and visibly worn armor — the gear a nearby observer can see.

## At a glance

| | |
|---|---|
| Path | `/api/equipment` |
| Methods | `GET`, `OPTIONS` |
| Perception class | `PROXIMATE` |
| Source | `PerceptionSnapshot#equipmentPayload()` |

## Why it exists

What you're wielding is context. "Holding a pickaxe" reads as mining; "sword and
shield" as combat. A bot that sees the player switch tools can adjust what it
expects and offers. Armor tells a similar story about how equipped the player is.

This is what is **visible on the body** — held items and worn armor — not the
hidden contents of the inventory, which a person could not see and which Metis
never exposes. It is `PROXIMATE`: only legible up close.

## Request

```
GET /api/equipment
```

## Response

`200 application/json`

| Field | Type | Null? | Description |
|---|---|---|---|
| `perception` | `string` | no | Always `"PROXIMATE"`. |
| `selectedSlot` | `integer` | no | Active hotbar slot, `0`–`8`. |
| `mainHand` | `object` | yes | Item in the main hand, or `null` if empty. |
| `offHand` | `object` | yes | Item in the off hand, or `null` if empty. |
| `armor` | `object` | no | The four armor slots (values nullable). |
| `armor.head` | `object` | yes | Helmet, or `null`. |
| `armor.chest` | `object` | yes | Chestplate, or `null`. |
| `armor.legs` | `object` | yes | Leggings, or `null`. |
| `armor.feet` | `object` | yes | Boots, or `null`. |

### Item object

Used for every slot above.

| Field | Type | Null? | Description |
|---|---|---|---|
| `id` | `string` | no | Item registry id, e.g. `minecraft:diamond_pickaxe`. |
| `count` | `integer` | no | Stack size. |
| `name` | `string` | no | Display name (custom name or default). |
| `damage` | `integer` | yes | Durability used. `null` for non-damageable items. |
| `maxDamage` | `integer` | yes | Maximum durability. `null` for non-damageable items. |

Remaining durability is `maxDamage - damage`.

### Example

```json
{
  "perception": "PROXIMATE",
  "selectedSlot": 0,
  "mainHand": { "id": "minecraft:diamond_pickaxe", "count": 1, "name": "Pickaxe", "damage": 12, "maxDamage": 1561 },
  "offHand": { "id": "minecraft:torch", "count": 32, "name": "Torch", "damage": null, "maxDamage": null },
  "armor": {
    "head": null,
    "chest": { "id": "minecraft:iron_chestplate", "count": 1, "name": "Iron Chestplate", "damage": 0, "maxDamage": 240 },
    "legs": null,
    "feet": null
  }
}
```

## Errors

Standard — see [`../conventions.md`](../conventions.md).

## Perception & gating

`PROXIMATE`. Gate on **distance + line of sight** with accuracy that falls off
with range. Note the scope is deliberately *visible* gear only — held and worn —
never the hidden inventory. Metis applies no gating.

## Reads (Minecraft API)

Verified against Yarn `1.21.1`:

| Field | Source call |
|---|---|
| `selectedSlot` | `PlayerEntity#getInventory().selectedSlot` |
| `mainHand` | `LivingEntity#getMainHandStack()` |
| `offHand` | `LivingEntity#getOffHandStack()` |
| `armor.*` | `LivingEntity#getEquippedStack(EquipmentSlot.HEAD/CHEST/LEGS/FEET)` |
| item `id` | `ItemStack#getItem()` → `Registries.ITEM.getId` |
| item `count` | `ItemStack#getCount()` |
| item `name` | `ItemStack#getName().getString()` |
| item `damage`/`maxDamage` | `ItemStack#isDamageable()` → `getDamage()` / `getMaxDamage()` |

## Example call

```bash
curl http://localhost:25566/api/equipment
```
