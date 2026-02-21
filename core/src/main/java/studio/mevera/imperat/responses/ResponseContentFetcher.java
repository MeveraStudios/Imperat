package studio.mevera.imperat.responses;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface ResponseContentFetcher {

    static ResponseContentFetcher blocking() {
        return (supplier) -> CompletableFuture.completedFuture(supplier.get());
    }

    static ResponseContentFetcher async() {
        return CompletableFuture::supplyAsync;
    }

    @NotNull CompletableFuture<String> fetch(@NotNull Supplier<String> contentSupplier);

}
