package com.coloryr.allmusic.client.core;

import com.coloryr.allmusic.client.core.objs.HttpResObj;
import com.coloryr.allmusic.client.core.objs.api.EncResObj;
import com.coloryr.allmusic.client.core.objs.enums.EncryptType;
import com.coloryr.allmusic.client.core.objs.api.music.trialinfo.TrialInfoObj;
import com.github.tartaricacid.netmusic.NetMusic;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HttpClientUtil {

    private static final int CONNECT_TIMEOUT = 5;
    private static final int READ_TIMEOUT = 7;
    private static final String UserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36 Edg/130.0.0.0";
    private static OkHttpClient client;

    private static Gson gson = new Gson();

    public static void init() {
        try {
            synchronized (OkHttpClient.class) {
                client = new OkHttpClient.Builder().cookieJar(new MyCookieJar())
                        .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                        .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                        .build();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static HttpResObj post(String url, JsonObject data, EncryptType type, String ourl) {
        try {
            RequestBody formBody;
            Request.Builder request = new Request.Builder();
            request = request.addHeader("Content-Type", "application/x-www-form-urlencoded");
            request = request.addHeader("Referer", "https://music.163.com");
            EncResObj res;
            List<Cookie> cookies = NetMusic.cookie.cookieStore.get("music.163.com");
            if (cookies == null) {
                cookies = new ArrayList<>();
            }
//            StringBuilder cookie = new StringBuilder();
//            for (Cookie item : cookies) {
//                cookie.append(item.name()).append("=").append(item.value()).append(";");
//            }
//            cookie.append("cookie=");
//            request.header("Cookie", cookie.toString());
            if (type == EncryptType.WEAPI) {
                request = request.addHeader("User-Agent", UserAgent);
                String csrfToken = "";
                for (Cookie item : cookies) {
                    if (item.name().equalsIgnoreCase("__csrf")) {
                        csrfToken = item.value();
                    }
                }
                data.addProperty("csrf_token", csrfToken);
                res = CryptoUtil.weapiEncrypt(gson.toJson(data));
                url = url.replaceFirst("\\w*api", "weapi");
                request = request.url(url);
                formBody = new FormBody.Builder()
                        .add("params", res.params)
                        .add("encSecKey", res.encSecKey)
                        .build();
            } else if (type == EncryptType.EAPI) {
                request = request.addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 9; PCT-AL10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.64 HuaweiBrowser/10.0.3.311 Mobile Safari/537.36");
                JsonObject header = new JsonObject();
                header.addProperty("appver", "8.10.90");
                header.addProperty("versioncode", "140");
                header.addProperty("buildver", new Date().toString().substring(0, 10));
                header.addProperty("resolution", "1920x1080");
                header.addProperty("os", "android");
                String requestId = "0000" + (new Date() + "_" + Math.floor(Math.random() * 1000));
                header.addProperty("requestId", requestId);
                for (Cookie item : cookies) {
                    if (item.name().equalsIgnoreCase("MUSIC_U")) {
                        header.addProperty("MUSIC_U", item.value());
                    } else if (item.name().equalsIgnoreCase("MUSIC_A")) {
                        header.addProperty("MUSIC_A", item.value());
                    } else if (item.name().equalsIgnoreCase("channel")) {
                        header.addProperty("channel", item.value());
                    } else if (item.name().equalsIgnoreCase("mobilename")) {
                        header.addProperty("mobilename", item.value());
                    } else if (item.name().equalsIgnoreCase("osver")) {
                        header.addProperty("osver", item.value());
                    } else if (item.name().equalsIgnoreCase("__csrf")) {
                        header.addProperty("__csrf", item.value());
                    }
                }

                data.add("header", header);
                res = CryptoUtil.eapi(ourl, data);
                url = url.replaceFirst("\\w*api", "eapi");
                request = request.url(url);
                formBody = new FormBody.Builder()
                        .add("params", res.params)
                        .build();
            } else {
                request = request.url(url);
                FormBody.Builder builder = new FormBody.Builder();
                for (Map.Entry<String, JsonElement> item : data.entrySet()) {
                    builder = builder.add(item.getKey(), item.getValue().getAsString());
                }
                formBody = builder.build();
            }
            request = request.post(formBody);
            Response response = client.newCall(request.build()).execute();
            int httpCode = response.code();
            ResponseBody body = response.body();
            if (body == null) {
                //AllMusic.log.warning("§d[AllMusic3]§c获取网页错误");
                return null;
            }
            InputStream inputStream = body.byteStream();
            boolean ok = httpCode == 200;
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            String data1 = result.toString(StandardCharsets.UTF_8.toString());
            if (!ok) {
                //AllMusic.log.warning("§d[AllMusic3]§c服务器返回错误：" + data1);
            }
            return new HttpResObj(data1, ok);
        } catch (Exception e) {
            //AllMusic.log.warning("§d[AllMusic3]§c获取网页错误");
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取播放链接
     *
     * @param id 音乐ID
     * @return 结果
     */
    public static String getPlayUrl(String id) {
        JsonObject params = new JsonObject();
        params.addProperty("ids", "[" + id + "]");
        params.addProperty("br", "320000");
        HttpResObj res = HttpClientUtil.post("https://music.163.com/weapi/song/enhance/player/url", params, EncryptType.WEAPI, null);
        if (res != null && res.ok) {
            try {
                TrialInfoObj obj = gson.fromJson(res.data, TrialInfoObj.class);
                return obj.getUrl();
            } catch (Exception e) {
                //AllMusic.log.warning("§d[AllMusic3]§c播放连接解析错误：" + res.data);
                e.printStackTrace();
            }
        }
        return null;
    }
}
