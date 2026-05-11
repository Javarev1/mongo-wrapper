package dev.toolkit.mongo.async;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * @author revqz
 */
public final class ReactiveAdapter {

    private ReactiveAdapter() {}

    // mono to optional future
    public static <T> CompletableFuture<Optional<T>> toFuture(Mono<T> mono) {
        return mono
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .toFuture();
    }

    // mono to raw future
    public static <T> CompletableFuture<T> toFutureRaw(Mono<T> mono) {
        return mono.toFuture();
    }

    // flux to list future
    public static <T> CompletableFuture<List<T>> toFutureList(Flux<T> flux) {
        return flux.collectList().toFuture();
    }

    // publisher to mono
    public static <T> Mono<T> fromPublisher(Publisher<T> publisher) {
        return Mono.from(publisher);
    }

    // publisher to flux
    public static <T> Flux<T> fromPublisherMany(Publisher<T> publisher) {
        return Flux.from(publisher);
    }
}
