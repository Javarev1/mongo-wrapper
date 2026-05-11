# Minecraft MongoDB Wrapper

A production-grade MongoDB wrapper built for Minecraft server plugins. Supports both **Reactor** (Mono/Flux) and **CompletableFuture** APIs out of the box.

> Made by **revqz**

---

## Features

- **Single entry point** — `MongoWrapper.connect()` wires everything up
- **Dual API** — every operation available as Mono/Flux or CompletableFuture
- **Custom BSON codec** — UUIDs as strings, Instants as BSON DateTime, no reflection
- **Partial updates** — `$set` and `$inc` operators for high-frequency writes
- **Fluent filter DSL** — type-safe queries without raw BSON strings
- **Sensible defaults** — 20 pool size, 3s connect timeout, majority write concern

---

## Quick Start

### Gradle Dependency

```gradle
repositories {
    mavenCentral()
}

dependencies {
    implementation 'dev.toolkit:minecraft-mongo-wrapper:1.0.0'
}
```

### Build From Source

```bash
./gradlew build
```

---

## Setup

```java
// on plugin enable
MongoWrapper mongo = MongoWrapper.connect(
    MongoConfig.builder()
        .uri("mongodb://localhost:27017")
        .database("minecraft")
        .maxPoolSize(20)
        .build()
);

// on plugin disable
mongo.close();
```

---

## Player Repository — Reactive

```java
PlayerRepository players = mongo.players();

// find by uuid
players.findByUUID(player.getUniqueId())
       .subscribe(profile -> System.out.println("Rank: " + profile.getRank()));

// first join — strict insert (fails on duplicate)
PlayerProfile newProfile = PlayerProfile.builder()
    .playerId(player.getUniqueId())
    .username(player.getName())
    .rank("default")
    .firstJoin(Instant.now())
    .lastSeen(Instant.now())
    .build();

players.insert(newProfile)
       .doOnError(DuplicateDocumentException.class, e -> log.warn("Already registered"))
       .subscribe();

// on quit — partial update (no full doc replace)
players.updateSession(uuid, secondsOnline).subscribe();

// increment a stat
players.incrementStat(uuid, "kills", 1).subscribe();

// save inventory slot
players.setInventorySlot(uuid, "slot_0", "diamond_sword").subscribe();

// delete
players.delete(uuid).subscribe();
```

---

## Player Repository — CompletableFuture

```java
// find by uuid
players.findByUUIDFuture(uuid)
       .thenAccept(opt -> opt.ifPresent(p -> giveReward(p)));

// find by username
players.findByUsernameFuture("Notch")
       .thenAccept(opt -> opt.ifPresent(p -> System.out.println(p.getPlayTimeSeconds())));

// save
players.saveFuture(profile)
       .thenAccept(saved -> System.out.println("Saved: " + saved.getUsername()));

// leaderboard top 10
players.topNFuture("play_time_seconds", 10)
       .thenAccept(list -> list.forEach(p -> System.out.println(p.getUsername())));
```

---

## PlayerFilter DSL

```java
// vip players active in last 7 days
Bson filter = PlayerFilter.where()
    .rank("vip")
    .seenAfter(Instant.now().minus(7, ChronoUnit.DAYS))
    .build();

players.findAll(filter).collectList()
       .subscribe(list -> System.out.println("Active VIPs: " + list.size()));

// players with 10+ kills and 1hr+ playtime
Bson hunters = PlayerFilter.where()
    .statAtLeast("kills", 10)
    .minPlaytime(3600)
    .build();

// username prefix search (case-insensitive)
Bson search = PlayerFilter.where()
    .usernameStartsWith("Not")
    .build();

// combine with raw bson
Bson advanced = PlayerFilter.where()
    .rank("admin")
    .raw(Filters.exists("ip_hash"))
    .build();
```

---

## Architecture

```
MongoWrapper                  ← entry point, holds client + pool
├── MongoConfig               ← connection + pool settings (builder)
├── PlayerCodecProvider       ← registers custom BSON codecs
│   └── PlayerProfileCodec   ← encodes/decodes PlayerProfile ↔ BSON
├── PlayerRepository          ← CRUD + partial updates + queries
│   ├── Mono/Flux methods     ← reactive API
│   └── *Future methods       ← CompletableFuture API
├── PlayerFilter              ← fluent query DSL
└── ReactiveAdapter           ← bridges Mono/Flux ↔ CompletableFuture
```

---

## Configuration

| Field | Default | Description |
|---|---|---|
| `uri` | `mongodb://localhost:27017` | Connection URI (supports Atlas SRV) |
| `database` | `minecraft` | Default database name |
| `maxPoolSize` | `20` | Max connections per host |
| `minPoolSize` | `2` | Idle connections kept warm |
| `maxWaitTimeMs` | `5000` | Pool wait timeout |
| `maxConnectionIdleTimeMs` | `60000` | Idle connection TTL |
| `connectTimeoutMs` | `3000` | Socket connect timeout |
| `socketTimeoutMs` | `10000` | Socket read timeout |

Write concern is set to `MAJORITY` by default to prevent data loss on crashes.

---

## Extending — Add a New Repository

1. Annotate your model with `@MongoDocument(collection = "economy")`
2. Write a codec implementing `Codec<EconomyAccount>`
3. Register it in `PlayerCodecProvider` (or create a new provider)
4. Create `EconomyRepository` following the same pattern
5. Expose it from `MongoWrapper.economy()`

---

## License

MIT
