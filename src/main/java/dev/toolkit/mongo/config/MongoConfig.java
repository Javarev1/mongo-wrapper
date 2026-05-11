package dev.toolkit.mongo.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.WriteConcern;
import lombok.Builder;
import lombok.Getter;
import org.bson.UuidRepresentation;

import java.util.concurrent.TimeUnit;

/**
 * @author revqz
 */
@Getter
@Builder
public class MongoConfig {

    @Builder.Default
    private final String uri = "mongodb://localhost:27017";

    @Builder.Default
    private final String database = "minecraft";

    @Builder.Default
    private final int maxPoolSize = 20;

    @Builder.Default
    private final int minPoolSize = 2;

    @Builder.Default
    private final long maxWaitTimeMs = 5_000;

    @Builder.Default
    private final long maxConnectionIdleTimeMs = 60_000;

    @Builder.Default
    private final int connectTimeoutMs = 3_000;

    @Builder.Default
    private final int socketTimeoutMs = 10_000;

    public MongoClientSettings toClientSettings() {
        return MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(uri))
                .uuidRepresentation(UuidRepresentation.STANDARD)
                .writeConcern(WriteConcern.MAJORITY)
                .applyToConnectionPoolSettings(pool -> pool
                        .maxSize(maxPoolSize)
                        .minSize(minPoolSize)
                        .maxWaitTime(maxWaitTimeMs, TimeUnit.MILLISECONDS)
                        .maxConnectionIdleTime(maxConnectionIdleTimeMs, TimeUnit.MILLISECONDS))
                .applyToSocketSettings(socket -> socket
                        .connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
                        .readTimeout(socketTimeoutMs, TimeUnit.MILLISECONDS))
                .build();
    }
}
