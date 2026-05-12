package dev.toolkit.mongo.repository;

import com.mongodb.client.model.*;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import dev.toolkit.mongo.async.ReactiveAdapter;
import dev.toolkit.mongo.exception.DocumentNotFoundException;
import dev.toolkit.mongo.exception.DuplicateDocumentException;
import dev.toolkit.mongo.filter.PlayerFilter;
import dev.toolkit.mongo.model.PlayerProfile;
import org.bson.Document;
import org.bson.conversions.Bson;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * @author revqz
 */
public class PlayerRepository {

    private final MongoCollection<PlayerProfile> collection;

    public PlayerRepository(MongoDatabase database) {
        this.collection = database.getCollection("players", PlayerProfile.class);
        ensureIndexes();
    }

    // runs off main thread
    private void ensureIndexes() {
        Flux.merge(
                Mono.from(collection.createIndex(
                        Indexes.ascending("username"),
                        new IndexOptions().unique(true).background(true))),
                Mono.from(collection.createIndex(
                        Indexes.ascending("rank"),
                        new IndexOptions().background(true))),
                Mono.from(collection.createIndex(
                        Indexes.descending("last_seen"),
                        new IndexOptions().background(true)))
        ).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
         .subscribe();
    }

    // reactive find operations

    public Mono<PlayerProfile> findByUUID(UUID uuid) {
        return ReactiveAdapter.fromPublisher(
                collection.find(Filters.eq("_id", uuid.toString())).first()
        );
    }

    public Mono<PlayerProfile> findByUsername(String username) {
        return ReactiveAdapter.fromPublisher(
                collection.find(Filters.eq("username", username)).first()
        );
    }

    // throws if player missing
    public Mono<PlayerProfile> getByUUID(UUID uuid) {
        return findByUUID(uuid)
                .switchIfEmpty(Mono.error(new DocumentNotFoundException(
                        "Player not found: " + uuid
                )));
    }

    // cheaper than full document fetch
    public Mono<Boolean> existsByUUID(UUID uuid) {
        return ReactiveAdapter.fromPublisher(
                collection.countDocuments(Filters.eq("_id", uuid.toString()), new CountOptions().limit(1))
        ).map(count -> count > 0);
    }

    public Flux<PlayerProfile> findAll(PlayerFilter filter) {
        return ReactiveAdapter.fromPublisherMany(
                collection.find(filter.build())
        );
    }

    public Flux<PlayerProfile> findAll(Bson filter) {
        return ReactiveAdapter.fromPublisherMany(collection.find(filter));
    }

    public Flux<PlayerProfile> findByRank(String rank) {
        return findAll(PlayerFilter.where().rank(rank));
    }

    public Flux<PlayerProfile> findPaged(Bson filter, String sortField,
                                         boolean ascending, int skip, int limit) {
        Bson sort = ascending ? Sorts.ascending(sortField) : Sorts.descending(sortField);
        return ReactiveAdapter.fromPublisherMany(
                collection.find(filter).sort(sort).skip(skip).limit(limit)
        );
    }

    // reactive save operations

    // upserts — inserts or replaces
    public Mono<PlayerProfile> save(PlayerProfile profile) {
        return ReactiveAdapter.fromPublisher(
                collection.findOneAndReplace(
                        Filters.eq("_id", profile.getPlayerId().toString()),
                        profile,
                        new FindOneAndReplaceOptions()
                                .upsert(true)
                                .returnDocument(ReturnDocument.AFTER)
                )
        );
    }

    // strict insert, fails on duplicate
    public Mono<Void> insert(PlayerProfile profile) {
        return ReactiveAdapter.fromPublisher(collection.insertOne(profile))
                .onErrorMap(com.mongodb.MongoWriteException.class, e ->
                        new DuplicateDocumentException(
                                "Player already exists: " + profile.getPlayerId(), e
                        )
                )
                .then();
    }

    // batch insert in single round trip
    public Mono<Void> insertMany(List<PlayerProfile> profiles) {
        return ReactiveAdapter.fromPublisher(collection.insertMany(profiles))
                .then();
    }

    // partial field update via $set/$inc
    public Mono<Void> update(UUID uuid, Bson update) {
        return ReactiveAdapter.fromPublisher(
                collection.updateOne(
                        Filters.eq("_id", uuid.toString()),
                        update
                )
        ).then();
    }

    // updates last_seen + playtime
    public Mono<Void> updateSession(UUID uuid, long additionalSeconds) {
        return update(uuid, Updates.combine(
                Updates.set("last_seen", Instant.now()),
                Updates.inc("play_time_seconds", additionalSeconds)
        ));
    }

    // atomic stat increment
    public Mono<Void> incrementStat(UUID uuid, String statKey, long amount) {
        return update(uuid, Updates.inc("stats." + statKey, amount));
    }

    public Mono<Void> setInventorySlot(UUID uuid, String slot, String itemId) {
        return update(uuid, Updates.set("inventory." + slot, itemId));
    }

    // reactive delete

    public Mono<Void> delete(UUID uuid) {
        return ReactiveAdapter.fromPublisher(
                collection.deleteOne(Filters.eq("_id", uuid.toString()))
        ).then();
    }

    // reactive aggregation

    public Mono<Long> count(Bson filter) {
        return ReactiveAdapter.fromPublisher(collection.countDocuments(filter));
    }

    public Mono<Long> countAll() {
        return ReactiveAdapter.fromPublisher(collection.countDocuments());
    }

    // top n by descending field
    public Flux<PlayerProfile> topN(String field, int n) {
        return ReactiveAdapter.fromPublisherMany(
                collection.find()
                        .sort(Sorts.descending(field))
                        .limit(n)
        );
    }

    // completablefuture wrappers

    public CompletableFuture<Optional<PlayerProfile>> findByUUIDFuture(UUID uuid) {
        return ReactiveAdapter.toFuture(findByUUID(uuid));
    }

    public CompletableFuture<Optional<PlayerProfile>> findByUsernameFuture(String username) {
        return ReactiveAdapter.toFuture(findByUsername(username));
    }

    public CompletableFuture<List<PlayerProfile>> findAllFuture(PlayerFilter filter) {
        return ReactiveAdapter.toFutureList(findAll(filter));
    }

    public CompletableFuture<Boolean> existsByUUIDFuture(UUID uuid) {
        return ReactiveAdapter.toFutureRaw(existsByUUID(uuid));
    }

    public CompletableFuture<PlayerProfile> saveFuture(PlayerProfile profile) {
        return ReactiveAdapter.toFutureRaw(save(profile));
    }

    public CompletableFuture<Void> insertFuture(PlayerProfile profile) {
        return ReactiveAdapter.toFutureRaw(insert(profile));
    }

    public CompletableFuture<Void> insertManyFuture(List<PlayerProfile> profiles) {
        return ReactiveAdapter.toFutureRaw(insertMany(profiles));
    }

    public CompletableFuture<Void> deleteFuture(UUID uuid) {
        return ReactiveAdapter.toFutureRaw(delete(uuid));
    }

    public CompletableFuture<Long> countAllFuture() {
        return ReactiveAdapter.toFutureRaw(countAll());
    }

    public CompletableFuture<List<PlayerProfile>> topNFuture(String field, int n) {
        return ReactiveAdapter.toFutureList(topN(field, n));
    }
}
