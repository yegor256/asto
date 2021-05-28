/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto.memory;

import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.Concatenation;
import com.artipie.asto.Content;
import com.artipie.asto.FailedCompletionStage;
import com.artipie.asto.Key;
import com.artipie.asto.KeyComparator;
import com.artipie.asto.OneTimePublisher;
import com.artipie.asto.Remaining;
import com.artipie.asto.Storage;
import com.artipie.asto.ValueNotFoundException;
import com.artipie.asto.ext.CompletableFutureSupport;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.vavr.NotImplementedError;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Function;

/**
 * Storage implementation for benchmarks.
 * @since 1.1.0
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class BenchmarkStorage implements Storage {
    /**
     * Backend storage.
     */
    private final InMemoryStorage backend;

    /**
     * Local storage.
     */
    private final Map<Key, byte[]> local;

    /**
     * Set which contains deleted keys.
     */
    private final Set<Key> deleted;

    /**
     * Ctor.
     * @param backend Backend storage
     */
    public BenchmarkStorage(final InMemoryStorage backend) {
        this.backend = backend;
        this.local = new ConcurrentSkipListMap<>(new KeyComparator<>());
        this.deleted = ConcurrentHashMap.newKeySet();
    }

    @Override
    public CompletableFuture<Boolean> exists(final Key key) {
        if (this.deleted.contains(key)) {

        }
        throw new NotImplementedError("Not implemented yet");
    }

    @Override
    public CompletableFuture<Collection<Key>> list(final Key prefix) {
        throw new NotImplementedError("Not implemented yet");
    }

    @Override
    public CompletableFuture<Void> save(final Key key, final Content content) {
        final CompletableFuture<Void> res;
        if (Key.ROOT.equals(key)) {
            res = new CompletableFutureSupport.Failed<Void>(
                new ArtipieIOException("Unable to save to root")
            ).get();
        } else {
            res = new Concatenation(new OneTimePublisher<>(content)).single()
                .to(SingleInterop.get())
                .thenApply(Remaining::new)
                .thenApply(Remaining::bytes)
                .thenAccept(bytes -> this.local.put(key, bytes))
                .toCompletableFuture();
        }
        return res;
    }

    @Override
    public CompletableFuture<Void> move(final Key source, final Key destination) {
        throw new NotImplementedError("Not implemented yet");
    }

    @Override
    public CompletableFuture<Long> size(final Key key) {
        throw new NotImplementedError("Not implemented yet");
    }

    @Override
    public CompletableFuture<Content> value(final Key key) {
        // @checkstyle NestedIfDepthCheck (30 lines)
        final CompletionStage<Content> res;
        if (Key.ROOT.equals(key)) {
            res = new FailedCompletionStage<>(new ArtipieIOException("Unable to load from root"));
        } else {
            synchronized (this.deleted) {
                if (this.deleted.contains(key)) {
                    res = new FailedCompletionStage<>(new ValueNotFoundException(key));
                } else {
                    final byte[] lcl = this.local.getOrDefault(key, null);
                    if (lcl == null) {
                        synchronized (this.backend.data) {
                            final byte[] bcknd = this.backend.data.get(key.string());
                            if (bcknd == null) {
                                res = new FailedCompletionStage<>(new ValueNotFoundException(key));
                            } else {
                                res = bytesToContentCompletion(bcknd);
                            }
                        }
                    } else {
                        res = bytesToContentCompletion(lcl);
                    }
                }
            }
        }
        return res.toCompletableFuture();
    }

    @Override
    public CompletableFuture<Void> delete(final Key key) {
        throw new NotImplementedError("Not implemented yet");
    }

    @Override
    public <T> CompletionStage<T> exclusively(
        final Key key,
        final Function<Storage, CompletionStage<T>> operation
    ) {
        throw new NotImplementedError("Not implemented yet");
    }

    /**
     * Converts array of bytes to completion stage.
     * @param bytes Bytes which should be converted
     * @return Completion stage with content.
     */
    private static CompletionStage<Content> bytesToContentCompletion(final byte[] bytes) {
        return  CompletableFuture.completedFuture(
            new Content.OneTime(new Content.From(bytes))
        );
    }
}