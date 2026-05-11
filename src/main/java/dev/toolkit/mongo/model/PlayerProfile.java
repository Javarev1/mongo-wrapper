package dev.toolkit.mongo.model;

import dev.toolkit.mongo.annotation.MongoDocument;
import dev.toolkit.mongo.annotation.MongoField;
import dev.toolkit.mongo.annotation.MongoId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author revqz
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@MongoDocument(collection = "players")
public class PlayerProfile {

    @MongoId
    private UUID playerId;

    @MongoField("username")
    private String username;

    @MongoField(value = "rank", index = true)
    private String rank;

    @MongoField("first_join")
    private Instant firstJoin;

    @MongoField("last_seen")
    private Instant lastSeen;

    @MongoField("play_time_seconds")
    private long playTimeSeconds;

    @MongoField("stats")
    @Builder.Default
    private Map<String, Object> stats = new HashMap<>();

    @MongoField("inventory")
    @Builder.Default
    private Map<String, String> inventory = new HashMap<>();

    // server-side only, not synced
    @MongoField(value = "ip_hash", ignore = false)
    private String ipHash;
}
