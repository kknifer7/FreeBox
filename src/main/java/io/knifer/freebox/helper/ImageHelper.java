package io.knifer.freebox.helper;

import cn.hutool.core.net.URLEncodeUtil;
import cn.hutool.core.text.StrPool;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.net.HttpHeaders;
import io.knifer.freebox.constant.BaseResources;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.model.domain.ImageLoadingResult;
import io.knifer.freebox.util.ValidationUtil;
import javafx.scene.image.Image;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;
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
    private static final Map<String, String> DEFAULT_HEADERS = Map.of(
            HttpHeaders.USER_AGENT, BaseValues.USER_AGENT
    );
    private static final CacheLoader<String, CompletableFuture<ImageLoadingResult>> CACHE_LOADER = new CacheLoader<>() {
        @NotNull
        @Override
        public CompletableFuture<ImageLoadingResult> load(@NotNull String imageUrl) {
            return loadImageWithProxyRetry(imageUrl);
        }
    };
    private static final LoadingCache<String, CompletableFuture<ImageLoadingResult>> CACHE =
            CacheBuilder.newBuilder()
                    .maximumSize(150)
                    .build(CACHE_LOADER);

    private CompletableFuture<ImageLoadingResult> loadImageWithProxyRetry(String imageUrl) {
        CompletableFuture<ImageLoadingResult> future = new CompletableFuture<>();

        loadImage(future, imageUrl, DEFAULT_HEADERS, false);

        return future;
    }

    private void loadImage(
            CompletableFuture<ImageLoadingResult> future,
            String imageUrl,
            Map<String, String> headers,
            boolean isRetrying
    ) {
        Pair<String, Map<String, String>> fixedImageUrlAndHeaderMap;
        Request request;

        if (!ValidationUtil.isURL(imageUrl)) {
            if (isRetrying) {
                future.complete(DEFAULT_RESULT);

                return;
            }
            fixedImageUrlAndHeaderMap = tryFixUrlAndGetHeaderMap(imageUrl);
            if (fixedImageUrlAndHeaderMap == null) {
                future.complete(DEFAULT_RESULT);
            } else {
                loadImage(
                        future,
                        fixedImageUrlAndHeaderMap.getLeft(),
                        fixedImageUrlAndHeaderMap.getRight(),
                        false
                );
            }

            return;
        }
        request = new Request.Builder()
                .url(imageUrl)
                .headers(Headers.of(headers))
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
                                handleLoadImageFail(future, imageUrl, isRetrying);

                                return;
                            }
                            imageInputStream = new ByteArrayInputStream(response.body().bytes());
                            image = createImage(imageInputStream);
                        } catch (IOException ex) {
                            log.warn("read image failed: {}", imageUrl, ex);
                            handleLoadImageFail(future, imageUrl, isRetrying);

                            return;
                        }
                        if (image.getProgress() < 1.0 || image.isError()) {
                            log.warn("load image failed: {}", imageUrl, image.getException());
                            handleLoadImageFail(future, imageUrl, isRetrying);

                            return;
                        }
                        future.complete(ImageLoadingResult.of(image, true));
                    }

                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        log.warn("download image failed: {}", imageUrl, e);
                        handleLoadImageFail(future, imageUrl, true);
                    }
                });
    }

    /**
     * 尝试修复图片url
     * 部分图片url是这种格式："原始url@请求头1=xxx@请求头2=xxx..."
     * @param imageUrl 图片url
     * @return 修复后的图片url和请求头。如果无法修复，返回null
     */
    @Nullable
    private static Pair<String, Map<String, String>> tryFixUrlAndGetHeaderMap(String imageUrl) {
        String[] splits;
        String fixedImageUrl;
        Map<String, String> headerMap = null;
        String[] headerSplits;

        if (!StringUtils.contains(imageUrl, StrPool.AT)) {

            return null;
        }
        splits = StringUtils.split(imageUrl, StrPool.AT);
        fixedImageUrl = splits[0];
        if (splits.length < 2 || !ValidationUtil.isURL(fixedImageUrl)) {

            return null;
        }
        for (int i = 1; i < splits.length; i++) {
            headerSplits = StringUtils.split(splits[i], "=");
            if (headerSplits.length != 2) {

                return null;
            }
            if (headerMap == null) {
                headerMap = new HashMap<>();
            }
            headerMap.put(headerSplits[0], headerSplits[1]);
        }

        return Pair.of(fixedImageUrl, headerMap);
    }

    /**
     * 处理图片加载失败
     * @param imageUrl 图片url
     * @param isRetrying 重试标志
     */
    private void handleLoadImageFail(
            CompletableFuture<ImageLoadingResult> future,
            String imageUrl,
            boolean isRetrying
    ) {
        if (isRetrying) {
            future.complete(DEFAULT_RESULT);
        } else {
            loadImage(future, buildProxyCdnUrl(imageUrl), DEFAULT_HEADERS, true);
        }
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

    /**
     * 异步加载图片
     * @param imageUrl 图片url。出于性能考虑，对于非影片海报，传入前需要先校验url的格式
     * @return 图片加载结果
     */
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
