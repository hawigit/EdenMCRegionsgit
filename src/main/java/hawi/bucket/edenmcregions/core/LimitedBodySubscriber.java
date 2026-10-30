package hawi.bucket.edenmcregions.core;

import java.io.*;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.Flow;

final class LimitedBodySubscriber implements HttpResponse.BodySubscriber<byte[]> {
    private final CompletableFuture<byte[]> body = new CompletableFuture<>();
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private final int limit;
    private Flow.Subscription subscription;
    LimitedBodySubscriber(int limit) { this.limit = limit; }
    public CompletionStage<byte[]> getBody() { return body; }
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription; subscription.request(1);
    }
    public void onNext(List<ByteBuffer> buffers) {
        for (ByteBuffer buffer : buffers) {
            if (buffer.remaining() > limit - bytes.size()) {
                subscription.cancel(); body.completeExceptionally(new IOException("Map response too large")); return;
            }
            byte[] chunk = new byte[buffer.remaining()]; buffer.get(chunk); bytes.writeBytes(chunk);
        }
        subscription.request(1);
    }
    public void onError(Throwable error) { body.completeExceptionally(error); }
    public void onComplete() { body.complete(bytes.toByteArray()); }
}


