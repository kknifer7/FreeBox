package io.knifer.freebox.misc;

import io.knifer.freebox.handler.impl.SmartM3u8AdFilterHandler;
import io.knifer.freebox.model.domain.M3u8AdFilterResult;
import io.knifer.freebox.util.HttpUtil;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * m3u8广告过滤测试
 *
 * @author Knifer
 */
public class M3U8AdFilterTest {

    @Test
    void test() throws ExecutionException, InterruptedException {
        String url = "https://yzzy.play-cdn8.com/20220915/19934_e4384d94/index.m3u8?sign=220294fc500e80ca9fc28d44deaa4f1c&t=1763351262";
//        String url = "https://v.lzcdn25.com/20251115/10086_293714c3/2000k/hls/mixed.m3u8";
        String content = HttpUtil.getAsync(url).get();
        SmartM3u8AdFilterHandler filter = new SmartM3u8AdFilterHandler();
        M3u8AdFilterResult result = filter.handle(
                url,
                content,
//                Map.of(SmartM3u8AdFilterHandler.EXTRA_KEY_DTF, 0)
                Map.of(SmartM3u8AdFilterHandler.EXTRA_KEY_DTF, 0.3)
        );
        System.out.println("adLineCount: " + result.getAdLineCount());
        System.out.println("content length: " + result.getContent().length());
    }
}
