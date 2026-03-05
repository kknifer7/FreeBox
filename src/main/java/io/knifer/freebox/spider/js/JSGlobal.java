package io.knifer.freebox.spider.js;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.net.URLEncodeUtil;
import com.fongmi.quickjs.bean.Req;
import com.fongmi.quickjs.utils.Crypto;
import com.github.catvod.utils.Trans;
import com.github.catvod.utils.UriUtil;
import com.google.common.net.HttpHeaders;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.helper.ConfigHelper;
import io.knifer.freebox.util.HttpUtil;
import io.knifer.freebox.util.json.GsonUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 注入给JS的全局方法 GraalJS实现
 * 参考：<a href="https://github.com/FongMi/TV/blob/release/quickjs/src/main/java/com/fongmi/quickjs/method/Global.java">FongMi TV</a>
 *
 * @author Knifer
 */
@Slf4j
public class JSGlobal {

    private final Context context;
    private final Value jsJsonStringify;

    private final static Set<Class<?>> JS_SUPPORTED_JAVA_TYPES = Set.of(
            String.class,
            Integer.class,
            int.class,
            Boolean.class,
            boolean.class,
            Long.class,
            long.class,
            Double.class,
            double.class,
            Float.class,
            float.class,
            Short.class,
            short.class,
            Byte.class,
            byte.class,
            Character.class,
            char.class,
            Map.class,
            List.class
    );

    public JSGlobal(Context context) {
        this.context = context;
        this.jsJsonStringify = context.eval("js", "JSON.stringify");
    }

    private void injectMethodsIntoContext() {
        Value globalBindings = context.getBindings("js");
        Method[] methods = getClass().getMethods();

        for (Method method : methods) {
            if (method.isAnnotationPresent(HostAccess.Export.class)) {
                bindMethod(globalBindings, method);
            }
        }
    }

    private void bindMethod(Value globalBindings, Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();

        log.debug("bind method: {}", method.getName());
        globalBindings.putMember(method.getName(), (ProxyExecutable) arguments -> {
            Object[] convertedArgs = null;
            Class<?> paramType;

            try {
                if (paramTypes.length > 0) {
                    convertedArgs = new Object[paramTypes.length];
                    for (int i = 0; i < paramTypes.length; i++) {
                        paramType = paramTypes[i];
                        convertedArgs[i] = jsValueToObject(arguments[i], paramType);
                    }
                }

                return method.invoke(this, convertedArgs);
            } catch (Exception e) {
                log.error("method invoke error", e);

                return null;
            }
        });
    }

    @Nullable
    private Object jsValueToObject(Value jsValue, Class<?> targetType) {
        if (jsValue.isNull()) {
            return null;
        } else if (targetType == Value.class || targetType == Object.class) {
            return jsValue;
        } else if (JS_SUPPORTED_JAVA_TYPES.contains(targetType)) {
            return jsValue.as(targetType);
        } else {
            return stringify(jsValue);
        }
    }

    private String stringify(Value jsValue) {
        if (jsValue == null || jsValue.isNull()) {
            return "{}";
        }
        try {
            return jsJsonStringify.execute(jsValue).asString();
        } catch (Exception e) {
            log.error("stringify error", e);
            return "{}";
        }
    }

    @HostAccess.Export
    public String s2t(String text) {
        return Trans.s2t(false, text);
    }

    @HostAccess.Export
    public String t2s(String text) {
        return Trans.t2s(false, text);
    }

    @HostAccess.Export
    public Integer getPort() {
        return ConfigHelper.getHttpPort();
    }

    @HostAccess.Export
    public String getProxy(Boolean local) {
        return ConfigHelper.getProxyUrl(BooleanUtils.isNotFalse(local)) + "?do=js";
    }

    @HostAccess.Export
    public String js2Proxy(Boolean dynamic, Integer siteType, String siteKey, String url, Value headers) {
        return getProxy(!BooleanUtils.toBoolean(dynamic)) +
                String.format(
                        "&from=catvod&siteType=%s&siteKey=%s&header=%s&url=%s",
                        siteType,
                        siteKey,
                        URLEncodeUtil.encode(stringify(headers)),
                        URLEncodeUtil.encode(url)
                );
    }

    @Nullable
    @HostAccess.Export
    public Value _http(String url, Value options) {
        Value complete = options.getMember("complete");
        Req req;

        if (complete == null || !complete.canExecute()) {

            return req(url, options);
        }
        req = GsonUtil.fromJson(stringify(options), Req.class);
        newCall(url, req).enqueue(getCallback(complete, req));

        return null;
    }

    @HostAccess.Export
    public Value req(String url, Value options) {
        Req req;
        Response res = null;

        try {
            req = GsonUtil.fromJson(stringify(options), Req.class);
            res = newCall(url, req).execute();

            return createSuccessResponse(context, req, res);
        } catch (Exception e) {
            log.error("req error", e);
            IoUtil.close(res);

            return createEmptyResponse(context);
        }
    }

    private Call newCall(String url, Req req) {
        boolean redirect = req.isRedirect();
        Integer timeout = req.getTimeout();
        OkHttpClient client = HttpUtil.getClient()
                .newBuilder()
                .connectTimeout(timeout, TimeUnit.MILLISECONDS)
                .readTimeout(timeout, TimeUnit.MILLISECONDS)
                .writeTimeout(timeout, TimeUnit.MILLISECONDS)
                .followRedirects(redirect)
                .followSslRedirects(redirect)
                .build();
        Headers headers = Headers.of(req.getHeader());
        String method = req.getMethod();
        Request request;

        if (BaseValues.HTTP_POST.equalsIgnoreCase(method)) {
            request = new Request.Builder().url(url)
                    .headers(headers)
                    .post(parsePostBody(req, headers.get(HttpHeaders.CONTENT_TYPE)))
                    .build();
        } else if (BaseValues.HTTP_HEADER.equalsIgnoreCase(method)) {
            request = new Request.Builder().url(url).headers(headers).head().build();
        } else {
            request = new Request.Builder().url(url).headers(headers).get().build();
        }

        return client.newCall(request);
    }

    private RequestBody parsePostBody(Req req, String contentType) {
        String postType = req.getPostType();

        if (req.getData() != null && "json".equals(postType)) {
            return parseJsonBody(req);
        }
        if (req.getData() != null && "form".equals(postType)) {
            return parseFormBody(req);
        }
        if (req.getData() != null && "form-data".equals(postType)) {
            return parseFormDataBody(req);
        }
        if (req.getBody() != null && contentType != null) {
            return RequestBody.create(req.getBody(), MediaType.get(contentType));
        }

        return RequestBody.create(ArrayUtils.EMPTY_BYTE_ARRAY);
    }

    private RequestBody parseJsonBody(Req req) {
        return RequestBody.create(
                req.getData().toString(),
                MediaType.get("application/json; charset=utf-8")
        );
    }

    private RequestBody parseFormBody(Req req) {
        FormBody.Builder builder = new FormBody.Builder();

        Map<String, String> params = GsonUtil.toStringMap(req.getData());
        for (String key : params.keySet()) {
            builder.add(key, params.get(key));
        }

        return builder.build();
    }

    private RequestBody parseFormDataBody(Req req) {
        String boundary = "--dio-boundary-" +
                new SecureRandom().nextInt(42949) +
                new SecureRandom().nextInt(67296);
        MultipartBody.Builder builder = new MultipartBody.Builder(boundary).setType(MultipartBody.FORM);
        Map<String, String> params = GsonUtil.toStringMap(req.getData());

        for (String key : params.keySet()) {
            builder.addFormDataPart(key, params.get(key));
        }

        return builder.build();
    }

    private Callback getCallback(Value complete, Req req) {
        return new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response res) {
                complete.execute(createSuccessResponse(context, req, res));
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                log.error("http request failure", e);
                complete.execute(createEmptyResponse(context));
            }
        };
    }

    private Value createSuccessResponse(Context context, Req req, Response res) {
        Value response = null;
        Value headers;
        byte[] resBytes;

        try (res) {
            response = createEmptyResponse(context);
            headers = response.getMember("headers");
            putHeaders(res, headers);
            response.putMember("code", res.code());
            resBytes = res.body().bytes();
            response.putMember(
                    "content",
                    switch (req.getBuffer()) {
                        case 0 -> new String(resBytes, req.getCharset());
                        case 1 -> Value.asValue(resBytes);
                        case 2 -> Base64.encode(resBytes);
                        case 3 -> resBytes;
                        default -> StringUtils.EMPTY;
                    }
            );
        } catch (Exception e) {
            log.error("js http response success, but parsing failed", e);
            if (response == null) {
                response = createEmptyResponse(context);
            }
        }

        return response;
    }

    private Value createEmptyResponse(Context context) {
        return context.eval("js", """
            ({
                headers: {},
                content: "",
                code: ""
            })
        """);
    }

    private void putHeaders(Response res, Value jsObject) {
        String key;
        List<String> value;

        for (Map.Entry<String, List<String>> entry : res.headers().toMultimap().entrySet()) {
            key = entry.getKey();
            value = entry.getValue();
            if (value.size() == 1) {
                jsObject.putMember(key, value.get(0));
            }
            if (value.size() >= 2) {
                jsObject.putMember(key, Value.asValue(value));
            }
        }
    }

    @HostAccess.Export
    public String joinUrl(String parent, String child) {
        return UriUtil.resolve(parent, child);
    }

    @HostAccess.Export
    public String md5X(String text) {
        String result = Crypto.md5(text);

        log.debug("text:{}\nresult:\n{}", text, result);

        return result;
    }

    @HostAccess.Export
    public String aesX(String mode, boolean encrypt, String input, boolean inBase64, String key, String iv, boolean outBase64) {
        String result = Crypto.aes(mode, encrypt, input, inBase64, key, iv, outBase64);

        log.debug(
                "mode:{}\nencrypt:{}\ninBase64:{}\noutBase64:{}\nkey:{}\niv:{}\ninput:\n{}\nresult:\n{}",
                mode, encrypt, inBase64, outBase64, key, iv, input, result
        );

        return result;
    }

    public String rsaX(String mode, boolean pub, boolean encrypt, String input, boolean inBase64, String key, boolean outBase64) {
        String result = Crypto.rsa(mode, pub, encrypt, input, inBase64, key, outBase64);

        log.debug(
                "mode:{}\npub:{}\nencrypt:{}\ninBase64:{}\noutBase64:{}\nkey:\n{}\ninput:\n{}\nresult:\n{}",
                mode, pub, encrypt, inBase64, outBase64, key, input, result
        );

        return result;
    }

    public static JSGlobal init(Context context) {
        JSGlobal result = new JSGlobal(context);

        result.injectMethodsIntoContext();

        return result;
    }
}
