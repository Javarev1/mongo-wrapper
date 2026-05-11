package dev.toolkit.mongo.filter;

import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author revqz
 */
public class PlayerFilter {

    private final List<Bson> conditions = new ArrayList<>();

    private PlayerFilter() {}

    public static PlayerFilter where() {
        return new PlayerFilter();
    }

    public PlayerFilter playerId(UUID uuid) {
        conditions.add(Filters.eq("_id", uuid.toString()));
        return this;
    }

    public PlayerFilter username(String name) {
        conditions.add(Filters.eq("username", name));
        return this;
    }

    // case-insensitive prefix match
    public PlayerFilter usernameStartsWith(String prefix) {
        conditions.add(Filters.regex("username", "^" + prefix, "i"));
        return this;
    }

    public PlayerFilter rank(String rank) {
        conditions.add(Filters.eq("rank", rank));
        return this;
    }

    public PlayerFilter rankIn(String... ranks) {
        conditions.add(Filters.in("rank", (Object[]) ranks));
        return this;
    }

    public PlayerFilter seenAfter(Instant since) {
        conditions.add(Filters.gt("last_seen", since));
        return this;
    }

    public PlayerFilter seenBefore(Instant before) {
        conditions.add(Filters.lt("last_seen", before));
        return this;
    }

    public PlayerFilter minPlaytime(long seconds) {
        conditions.add(Filters.gte("play_time_seconds", seconds));
        return this;
    }

    public PlayerFilter statAtLeast(String statKey, Number value) {
        conditions.add(Filters.gte("stats." + statKey, value));
        return this;
    }

    public PlayerFilter hasItem(String itemId) {
        conditions.add(Filters.eq("inventory", itemId));
        return this;
    }

    // escape hatch for raw bson
    public PlayerFilter raw(Bson bson) {
        conditions.add(bson);
        return this;
    }

    // empty filter matches all docs
    public Bson build() {
        if (conditions.isEmpty()) return new org.bson.Document();
        if (conditions.size() == 1) return conditions.get(0);
        return Filters.and(conditions);
    }
}
