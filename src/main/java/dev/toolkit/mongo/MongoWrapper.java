package dev.toolkit.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.WriteConcern;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoDatabase;
import dev.toolkit.mongo.codec.PlayerCodecProvider;
import dev.toolkit.mongo.config.MongoConfig;
import dev.toolkit.mongo.repository.PlayerRepository;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;

/**
 * @author revqz
 */
public class MongoWrapper implements Closeable {

    private final MongoClient client;
    private final MongoDatabase database;
    private final PlayerRepository playerRepository;

    private MongoWrapper(MongoConfig config) {
        // custom codecs before defaults
        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
                CodecRegistries.fromProviders(new PlayerCodecProvider()),
                MongoClientSettings.getDefaultCodecRegistry()
        );

        var settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(config.getUri()))
                .uuidRepresentation(UuidRepresentation.STANDARD)
                .writeConcern(WriteConcern.MAJORITY)
                .applyToConnectionPoolSettings(pool -> pool
                        .maxSize(config.getMaxPoolSize())
                        .minSize(config.getMinPoolSize())
                        .maxWaitTime(config.getMaxWaitTimeMs(), TimeUnit.MILLISECONDS)
                        .maxConnectionIdleTime(config.getMaxConnectionIdleTimeMs(), TimeUnit.MILLISECONDS))
                .applyToSocketSettings(socket -> socket
                        .connectTimeout(config.getConnectTimeoutMs(), TimeUnit.MILLISECONDS)
                        .readTimeout(config.getSocketTimeoutMs(), TimeUnit.MILLISECONDS))
                .codecRegistry(codecRegistry)
                .build();

        this.client = MongoClients.create(settings);
        this.database = client.getDatabase(config.getDatabase())
                .withCodecRegistry(codecRegistry);
        this.playerRepository = new PlayerRepository(database);
    }

    public static MongoWrapper connect(MongoConfig config) {
        return new MongoWrapper(config);
    }

    // connects with localhost defaults
    public static MongoWrapper connectDefault() {
        return connect(MongoConfig.builder().build());
    }

    public PlayerRepository players() {
        return playerRepository;
    }

    public MongoDatabase database() {
        return database;
    }

    public MongoClient client() {
        return client;
    }

    @Override
    public void close() {
        client.close();
    }
}
