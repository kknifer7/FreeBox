package io.knifer.freebox.helper;

import cn.hutool.core.net.URLEncodeUtil;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.net.HttpHeaders;
import io.knifer.freebox.constant.BaseResources;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.model.domain.ImageLoadingResult;
import javafx.scene.image.Image;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 图片加载
 *
 * @author Knifer
 */
@Slf4j
@UtilityClass
public class ImageHelper {

    private static final String PROXY_CDN_URL = "https://wsrv.nl/?output=png&url=";
    private static final ImageLoadingResult DEFAULT_RESULT =
            ImageLoadingResult.of(BaseResources.PICTURE_PLACEHOLDER_IMG, false);
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .build();
    private static final LoadingCache<String, CompletableFuture<ImageLoadingResult>> CACHE =
            CacheBuilder.newBuilder()
                    .maximumSize(150)
                    .build(new CacheLoader<>() {
                        @NotNull
                        @Override
                        public CompletableFuture<ImageLoadingResult> load(@NotNull String imageUrl) {
                            return loadImageWithProxyRetry(imageUrl);
                        }
                    });

    private CompletableFuture<ImageLoadingResult> loadImageWithProxyRetry(String imageUrl) {
        CompletableFuture<ImageLoadingResult> future = new CompletableFuture<>();

        loadImage(future, imageUrl, false);

        return future;
    }

    private void loadImage(
            CompletableFuture<ImageLoadingResult> future,
            String imageUrl,
            boolean isRetrying
    ) {
        Request request = new Request.Builder()
                .url(imageUrl)
                .addHeader(HttpHeaders.USER_AGENT, BaseValues.USER_AGENT)
                .build();

        CLIENT.newCall(request)
                .enqueue(new Callback() {
                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) {
                        Image image;
                        ByteArrayInputStream imageInputStream;

                        try (response) {
                            if (!response.isSuccessful()) {
                                log.warn("download image failed, response={}", response);
                                if (isRetrying) {
                                    future.complete(DEFAULT_RESULT);
                                } else {
                                    loadImage(future, buildProxyCdnUrl(imageUrl), true);
                                }

                                return;
                            }
                            imageInputStream = new ByteArrayInputStream(response.body().bytes());
                            image = createImage(imageInputStream);
                        } catch (IOException ex) {
                            log.warn("read image failed: {}", imageUrl, ex);
                            if (isRetrying) {
                                future.complete(DEFAULT_RESULT);
                            } else {
                                loadImage(future, buildProxyCdnUrl(imageUrl), true);
                            }

                            return;
                        }
                        if (image.getProgress() < 1.0 || image.isError()) {
                            log.warn("load image failed: {}", imageUrl, image.getException());
                            if (isRetrying) {
                                future.complete(DEFAULT_RESULT);
                            } else {
                                loadImage(future, buildProxyCdnUrl(imageUrl), true);
                            }
                        }
                        future.complete(ImageLoadingResult.of(image, true));
                    }

                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        log.warn("download image failed: {}", imageUrl, e);
                        if (isRetrying) {
                            future.complete(DEFAULT_RESULT);
                        } else {
                            loadImage(future, buildProxyCdnUrl(imageUrl), true);
                        }
                    }
                });
    }

    private Image createImage(ByteArrayInputStream imageInputStream) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(imageInputStream);
        BufferedImage betterImg;
        int[] bytes;
        IntBuffer buffer;
        PixelFormat<IntBuffer> pixelFormat;
        PixelBuffer<IntBuffer> pixelBuffer;

        if (bufferedImage == null) {
            throw new IOException("no image data");
        }
        betterImg = new BufferedImage(
                bufferedImage.getWidth(), bufferedImage.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE
        );
        betterImg.createGraphics()
                .drawImage(bufferedImage, 0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(), null);
        bytes = ((DataBufferInt) betterImg.getRaster().getDataBuffer()).getData();
        buffer = IntBuffer.wrap(bytes);
        pixelFormat = PixelFormat.getIntArgbPreInstance();
        pixelBuffer = new PixelBuffer<>(betterImg.getWidth(), betterImg.getHeight(), buffer, pixelFormat);

        return new WritableImage(pixelBuffer);
    }

    private String buildProxyCdnUrl(String imageUrl) {
        return PROXY_CDN_URL + URLEncodeUtil.encode(imageUrl);
    }

    public CompletableFuture<ImageLoadingResult> loadAsync(String imageUrl) {
        try {
            return CACHE.get(imageUrl);
        } catch (Throwable e) {
            return CompletableFuture.completedFuture(DEFAULT_RESULT);
        }
    }

    public void clearCache() {
        CACHE.invalidateAll();
    }
}
