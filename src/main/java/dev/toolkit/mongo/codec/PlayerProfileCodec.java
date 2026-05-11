package dev.toolkit.mongo.codec;

import dev.toolkit.mongo.model.PlayerProfile;
import org.bson.*;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author revqz
 */
public class PlayerProfileCodec implements Codec<PlayerProfile> {

    @Override
    public void encode(BsonWriter writer, PlayerProfile profile, EncoderContext ctx) {
        writer.writeStartDocument();

        writer.writeString("_id", profile.getPlayerId().toString());
        writer.writeString("username", profile.getUsername());

        if (profile.getRank() != null) {
            writer.writeString("rank", profile.getRank());
        }
        if (profile.getFirstJoin() != null) {
            writer.writeDateTime("first_join", profile.getFirstJoin().toEpochMilli());
        }
        if (profile.getLastSeen() != null) {
            writer.writeDateTime("last_seen", profile.getLastSeen().toEpochMilli());
        }

        writer.writeInt64("play_time_seconds", profile.getPlayTimeSeconds());

        writer.writeName("stats");
        writeMap(writer, profile.getStats());

        writer.writeName("inventory");
        writer.writeStartDocument();
        if (profile.getInventory() != null) {
            profile.getInventory().forEach((slot, item) -> writer.writeString(slot, item));
        }
        writer.writeEndDocument();

        if (profile.getIpHash() != null) {
            writer.writeString("ip_hash", profile.getIpHash());
        }

        writer.writeEndDocument();
    }

    @Override
    public PlayerProfile decode(BsonReader reader, DecoderContext ctx) {
        PlayerProfile.PlayerProfileBuilder builder = PlayerProfile.builder();

        reader.readStartDocument();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            String fieldName = reader.readName();
            switch (fieldName) {
                case "_id" -> builder.playerId(UUID.fromString(reader.readString()));
                case "username" -> builder.username(reader.readString());
                case "rank" -> builder.rank(reader.readString());
                case "first_join" -> builder.firstJoin(Instant.ofEpochMilli(reader.readDateTime()));
                case "last_seen" -> builder.lastSeen(Instant.ofEpochMilli(reader.readDateTime()));
                case "play_time_seconds" -> builder.playTimeSeconds(reader.readInt64());
                case "stats" -> builder.stats(readObjectMap(reader));
                case "inventory" -> builder.inventory(readStringMap(reader));
                case "ip_hash" -> builder.ipHash(reader.readString());
                default -> reader.skipValue();
            }
        }
        reader.readEndDocument();
        return builder.build();
    }

    @Override
    public Class<PlayerProfile> getEncoderClass() {
        return PlayerProfile.class;
    }

    private void writeMap(BsonWriter writer, Map<String, Object> map) {
        writer.writeStartDocument();
        if (map != null) {
            map.forEach((key, value) -> {
                writer.writeName(key);
                // handles mixed numeric types
                if (value instanceof Integer i) writer.writeInt32(i);
                else if (value instanceof Long l) writer.writeInt64(l);
                else if (value instanceof Double d) writer.writeDouble(d);
                else if (value instanceof Boolean b) writer.writeBoolean(b);
                else if (value instanceof String s) writer.writeString(s);
                else writer.writeString(String.valueOf(value));
            });
        }
        writer.writeEndDocument();
    }

    private Map<String, Object> readObjectMap(BsonReader reader) {
        Map<String, Object> map = new HashMap<>();
        reader.readStartDocument();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            String key = reader.readName();
            map.put(key, switch (reader.getCurrentBsonType()) {
                case INT32 -> reader.readInt32();
                case INT64 -> reader.readInt64();
                case DOUBLE -> reader.readDouble();
                case BOOLEAN -> reader.readBoolean();
                case STRING -> reader.readString();
                default -> { reader.skipValue(); yield null; }
            });
        }
        reader.readEndDocument();
        return map;
    }

    private Map<String, String> readStringMap(BsonReader reader) {
        Map<String, String> map = new HashMap<>();
        reader.readStartDocument();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            map.put(reader.readName(), reader.readString());
        }
        reader.readEndDocument();
        return map;
    }
}
