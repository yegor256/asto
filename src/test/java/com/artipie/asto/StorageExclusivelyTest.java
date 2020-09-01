/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.asto;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests for {@link Storage#exclusively(Key, Function)}.
 *
 * @since 0.27
 */
@ExtendWith(StorageExtension.class)
public final class StorageExclusivelyTest {

    @TestTemplate
    void shouldFailExclusivelyForSameKey(final Storage storage) {
        final Key key = new Key.From("shouldFailConcurrentExclusivelyForSameKey");
        final FakeOperation operation = new FakeOperation();
        storage.exclusively(key, operation);
        operation.started.join();
        final CompletionException completion = Assertions.assertThrows(
            CompletionException.class,
            () -> storage.exclusively(key, new FakeOperation()).toCompletableFuture().join()
        );
        MatcherAssert.assertThat(
            completion.getCause(),
            new IsInstanceOf(IllegalStateException.class)
        );
    }

    @TestTemplate
    void shouldRunExclusivelyForDiffKey(final Storage storage) {
        final Key one = new Key.From("shouldRunExclusivelyForDiffKey-1");
        final Key two = new Key.From("shouldRunExclusivelyForDiffKey-2");
        final FakeOperation operation = new FakeOperation();
        storage.exclusively(one, operation);
        operation.started.join();
        Assertions.assertDoesNotThrow(
            () -> storage.exclusively(two, new FakeOperation(CompletableFuture.allOf()))
                .toCompletableFuture().join()
        );
    }

    @TestTemplate
    void shouldRunExclusivelyWhenPrevFinished(final Storage storage) {
        final Key key = new Key.From("shouldRunExclusivelyWhenPrevFinished");
        final FakeOperation operation = new FakeOperation(CompletableFuture.allOf());
        storage.exclusively(key, operation).toCompletableFuture().join();
        Assertions.assertDoesNotThrow(
            () -> storage.exclusively(key, new FakeOperation(CompletableFuture.allOf()))
                .toCompletableFuture().join()
        );
    }

    /**
     * Fake operation with controllable start and finish.
     * Started future is completed when operation is invoked.
     * It could be used to await operation invocation.
     * Finished future is returned as result of operation.
     * It could be completed in order to finish operation.
     *
     * @since 0.27
     */
    private static final class FakeOperation implements Function<Storage, CompletionStage<Void>> {

        /**
         * Operation started future.
         */
        private final CompletableFuture<Void> started;

        /**
         * Operation finished future.
         */
        private final CompletableFuture<Void> finished;

        private FakeOperation() {
            this(new CompletableFuture<>());
        }

        private FakeOperation(final CompletableFuture<Void> finished) {
            this.started = new CompletableFuture<>();
            this.finished = finished;
        }

        @Override
        public CompletionStage<Void> apply(final Storage storage) {
            this.started.complete(null);
            return this.finished;
        }
    }
}
