/**
 * Copyright (C) 2018 Joscha Düringer
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

import org.apache.commons.io.IOUtils;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.cookie.Cookie;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

class HttpUtils {
    static String readResponse(CloseableHttpResponse response) throws IOException {
        String responseBody = IOUtils.toString(response.getEntity().getContent(),StandardCharsets.UTF_8);
        EntityUtils.consume(response.getEntity());
        return responseBody;
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
