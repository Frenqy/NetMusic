package com.coloryr.allmusic.client.core;

import com.github.tartaricacid.netmusic.NetMusic;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MyCookieJar implements CookieJar {
    @Override
    public void saveFromResponse(HttpUrl httpUrl, @NotNull List<Cookie> list) {
        ArrayList<Cookie> cookies = NetMusic.cookie.cookieStore.get("music.163.com");
        if (cookies == null) {
            cookies = new ArrayList<>();
        }
        for (Cookie item : list) {
            for (Cookie item1 : cookies) {
                if (item.name().equalsIgnoreCase(item1.name())) {
                    cookies.remove(item1);
                    break;
                }
            }
            cookies.add(item);
        }
        NetMusic.cookie.cookieStore.put("music.163.com", cookies);
        NetMusic.saveCookie();
    }

    @Override
    public @NotNull List<Cookie> loadForRequest(HttpUrl httpUrl) {
        List<Cookie> cookies = NetMusic.cookie.cookieStore.get("music.163.com");
        return cookies != null ? cookies : new ArrayList<>();
    }
}