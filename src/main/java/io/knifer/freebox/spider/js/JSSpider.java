package io.knifer.freebox.spider.js;

import cn.hutool.core.codec.Base64;
import com.fongmi.quickjs.bean.Res;
import com.github.catvod.crawler.spider.Spider;
import io.knifer.freebox.constant.BaseResources;
import io.knifer.freebox.util.json.GsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * JS 爬虫
 *
 * @author Knifer
 */
@Slf4j
public class JSSpider extends Spider {

    private final Path jar;
    private Context context;
    private Value spiderObject;
    private Value jsonParseFunction;

    private final static int SUBMIT_TIMEOUT = 10;
    private final static String SPIDER_OBJECT_NAME = "__JS_SPIDER__";

    public JSSpider(String siteKey, Path jar) {
        super.siteKey = siteKey;
        this.jar = jar;
    }

    @Override
    public void init(String extend) throws Exception {
        boolean catFlag;

        catFlag = initializeJS();
        if (catFlag) {
            call("init", cfg(extend));
        } else {
            call("init", GsonUtil.isJson(extend) ? jsonParseFunction.execute(extend) : extend);
        }
    }

    private Value cfg(String ext) {
        Value cfg = context.eval(
                "js",
                """
                    ({
                        stype: 3,
                        skey: "%s",
                        code: ""
                    })
                """.formatted(siteKey)
        );

        if (GsonUtil.isJson(ext)) {
            cfg.putMember("ext", jsonParseFunction.execute(ext));
        } else {
            cfg.putMember("ext", ext);
        }

        return cfg;
    }

    @Override
    public String homeContent(boolean filter) throws Exception {
        return (String) call("home", filter);
    }

    @Override
    public String homeVideoContent() throws Exception {
        return (String) call("homeVod");
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        return (String) call("category", tid, pg, filter, extend);
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        return (String) call("detail", ids.get(0));
    }

    @Override
    public String searchContent(String key, boolean quick) throws Exception {
        return (String) call("search", key, quick);
    }

    @Override
    public String searchContent(String key, boolean quick, String pg) throws Exception {
        return (String) call("search", key, quick, pg);
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        return (String) call("play", flag, id, vipFlags);
    }

    @Override
    public boolean manualVideoCheck() throws Exception {
        return (Boolean) call("sniffer");
    }

    @Override
    public boolean isVideoFormat(String url) throws Exception {
        return (Boolean) call("isVideo", url);
    }

    public Object[] proxy(Map<String, String> params) throws Exception {
        return "catvod".equals(params.get("from")) ? proxy2(params) : proxy1(params);
    }

    private Object[] proxy1(Map<String, String> params) {
        Value proxyResultArray = jsonParseFunction.execute(spiderObject.invokeMember("proxy", params));
        long resultArrayLength;
        Map<String, String> headers;
        boolean base64;
        Object[] result;

        if (!proxyResultArray.hasArrayElements()) {

            return null;
        }
        resultArrayLength = proxyResultArray.getArraySize();
        headers = resultArrayLength > 3 ?
                GsonUtil.toStringMap(proxyResultArray.getArrayElement(3).asString()) : null;
        base64 = resultArrayLength > 4 && proxyResultArray.getArrayElement(4).asInt() == 1;
        result = new Object[4];
        result[0] = proxyResultArray.getArrayElement(0).asInt();
        result[1] = proxyResultArray.getArrayElement(1).asString();
        result[2] = getStream(proxyResultArray.getArrayElement(2), base64);
        result[3] = headers;

        return result;
    }

    private ByteArrayInputStream getStream(Value jsValue, boolean base64) {
        String content;
        byte[] bytes = toByteArray(jsValue);

        if (bytes != null) {

            return new ByteArrayInputStream(bytes);
        } else {
            content = jsValue.asString();
            if (base64 && content.contains("base64,")) {
                content = content.split("base64,")[1];
            }

            return new ByteArrayInputStream(base64 ? Base64.decode(content) : content.getBytes());
        }
    }

    @Nullable
    private static byte[] toByteArray(Value value) {
        Object hostObj;

        if (value == null || value.isNull()) {

            return null;
        }
        if (value.isHostObject()) {
            hostObj = value.asHostObject();

            return hostObj instanceof byte[] ? (byte[]) hostObj : null;
        }

        return null;
    }

    private Object[] proxy2(Map<String, String> params) throws Exception {
        String url = params.get("url");
        String header = params.get("header");
        Value array = Value.asValue(url.split("/"));
        Object headerObject = jsonParseFunction.execute(header);
        String json = (String) call("proxy", array, headerObject);
        Res res = GsonUtil.fromJson(json, Res.class);
        Object[] result = new Object[3];

        result[0] = res.getCode();
        result[1] = res.getContentType();
        result[2] = res.getStream();

        return result;
    }

    private Object call(String function, Object... args) throws Exception {
        CompletableFuture<Object> result = new CompletableFuture<>();

        Promise.of(spiderObject.invokeMember(function, args), String.class)
                .then(result::complete)
                .catchError(error -> {
                    log.error("call '{}' failed, errorMessage={}", function, error);
                    result.complete(StringUtils.EMPTY);
                });

        return result.get(SUBMIT_TIMEOUT, TimeUnit.SECONDS);
    }

    /**
     * 初始化JS爬虫
     * @return 是否为cat爬虫
     * @throws Exception 初始化失败抛出异常
     */
    private boolean initializeJS() throws Exception {
        Path workingDirectory = BaseResources.JS_SPIDER_WORKING_DIRECTORY;
        boolean catFlag = false;
        Value global;
        String spiderJsCode;
        Source spiderSource;
        Value spiderModule;
        Value spiderExport;

        context = Context.newBuilder("js")
                .allowIO(IOAccess.ALL)
                .allowHostAccess(HostAccess.ALL)
                .currentWorkingDirectory(workingDirectory)
                .option("js.esm-eval-returns-exports", "true")
                .build();
        // 初始化JS本地缓存库
        global = context.getBindings("js");
        global.putMember("local", new Local());
        // 初始化JS全局变量
        JSGlobal.init(context);
        log.info("initialize GraalJs context succeed");
        // 初始化JS爬虫
        spiderJsCode = Files.readString(jar);
        log.info("initialize js spider, js code size: {}", spiderJsCode.length());
        spiderSource = Source.newBuilder("js", spiderJsCode, jar.getFileName().toString())
                .mimeType("application/javascript+module")
                .build();
        spiderModule = context.eval(spiderSource);
        if (!global.hasMember(SPIDER_OBJECT_NAME)) {
            spiderExport = spiderModule.getMember("__jsEvalReturn");
            if (spiderExport != null && spiderExport.canExecute()) {
                catFlag = true;
                global.putMember(SPIDER_OBJECT_NAME, spiderExport.execute());
            } else if ((spiderExport = spiderModule.getMember("default")) != null) {
                if (spiderExport.canExecute()) {
                    spiderExport = spiderExport.execute();
                }
                global.putMember(SPIDER_OBJECT_NAME, spiderExport);
            } else {
                global.putMember(SPIDER_OBJECT_NAME, spiderModule);
            }
        }
        spiderObject = global.getMember(SPIDER_OBJECT_NAME);
        jsonParseFunction = context.eval("js", "JSON.parse");
        log.info("initialize js spider succeed");

        return catFlag;
    }

    @Override
    public void destroy() {
        try {
            context.close(true);
        } catch (Exception e) {
            log.error("destroy failed", e);
        }
    }
}
