package io.knifer.freebox.spider;

import io.knifer.freebox.model.domain.FreeBoxApiConfig;
import io.knifer.freebox.model.domain.FreeBoxSourceBean;
import io.knifer.freebox.util.catvod.SpiderInvokeUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * 爬虫Jar加载测试
 *
 * @author Knifer
 */

public class SpiderJarLoaderTest {

    private static FreeBoxApiConfig apiConfig;

    @BeforeAll
    static void init() {
        FreeBoxSourceBean sourceBean = new FreeBoxSourceBean();

        sourceBean.setKey("NCat");
        sourceBean.setName("网飞猫 | 影视");
        sourceBean.setType(3);
        sourceBean.setApi("csp_NCat");
        sourceBean.setSearchable(1);
        apiConfig = new FreeBoxApiConfig();
        apiConfig.setSpider("../jar/spider.jar;md5;074ddf3f17d1a91c461b42f1f701d0e7");
        apiConfig.setUrl("https://catvodspider-49d.pages.dev/json/config.json");
        apiConfig.setSites(List.of(sourceBean));
    }

    @Test
    void test() {
        SpiderJarLoader spiderJarLoader = new SpiderJarLoader();
        Object spider;

        spiderJarLoader.setApiConfig(apiConfig);
        spiderJarLoader.loadJar("Zxzj", apiConfig.getSpider());
        spider = spiderJarLoader.getSpider("Zxzj", "csp_Zxzj", null, apiConfig.getSpider());
        try {
            System.out.println("homeContent: " + SpiderInvokeUtil.homeContent(spider, false));
            /*System.out.println("homeVideoContent: " + SpiderInvokeUtil.homeVideoContent(spider));
            System.out.println(
                    "categoryContent: " +
                            SpiderInvokeUtil.categoryContent(spider, "1", "1", false, null)
            );
            System.out.println("detailContent: " + SpiderInvokeUtil.detailContent(spider, List.of("258208.html")));
            System.out.println("searchContent: " + SpiderInvokeUtil.searchContent(spider, "大", true));
            System.out.println("playerContent: " + SpiderInvokeUtil.playerContent(spider, "1", "258208-37-525162.html", null));
            System.out.println("manualVideoCheck: " + SpiderInvokeUtil.manualVideoCheck(spider));
            System.out.println("proxyLocal: " + Arrays.toString(SpiderInvokeUtil.proxyLocal(spider, null)));
            System.out.println("isVideoFormat: " + SpiderInvokeUtil.isVideoFormat(spider, "https://www.baidu.com"));
            System.out.println("client: " + SpiderInvokeUtil.client(spider));*/
            SpiderInvokeUtil.destroy(spider);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
