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
package com.artipie.asto.ext;

import com.artipie.asto.Content;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Flowable;
import java.security.MessageDigest;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;
import org.apache.commons.codec.binary.Hex;

/**
 * Digest of content.
 * @since 0.6
 */
public final class ContentDigest {

    /**
     * Content.
     */
    private final Content content;

    /**
     * Message digest.
     */
    private final Supplier<MessageDigest> digest;

    /**
     * Digest of content.
     * @param content Content
     * @param digest Digest
     */
    public ContentDigest(final Content content, final Supplier<MessageDigest> digest) {
        this.content = content;
        this.digest = digest;
    }

    /**
     * Hex of the digest.
     * @return Hex string
     */
    public CompletionStage<String> hex() {
        return Flowable.fromPublisher(this.content)
            .reduceWith(
                this.digest::get,
                (dgst, buf) -> {
                    dgst.update(buf);
                    return dgst;
                }
            )
            .map(MessageDigest::digest)
            .map(Hex::encodeHexString)
            .to(SingleInterop.get());
    }
}
