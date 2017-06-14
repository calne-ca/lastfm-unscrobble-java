/**
 * Copyright (C) 2017 Joscha Düringer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.beardbot.lastfm.unscrobble;

import org.apache.http.HttpEntity;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.cookie.Cookie;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;

class HttpUtils {
    static String readResponse(CloseableHttpResponse response) throws Exception {
        BufferedReader reader;
        String content = "";
        String line;
        HttpEntity entity = response.getEntity();

        reader = new BufferedReader(new InputStreamReader(entity.getContent()));
        while ((line = reader.readLine()) != null) {
            content += line;
        }
        EntityUtils.consume(entity);
        return content;
    }

    static String getCookieValue(CookieStore cookieStore, String cookieName) {
        String value = null;
        for (Cookie cookie: cookieStore.getCookies()) {
            if (cookie.getName().equals(cookieName)) {
                value = cookie.getValue();
            }
        }
        return value;
    }
}
