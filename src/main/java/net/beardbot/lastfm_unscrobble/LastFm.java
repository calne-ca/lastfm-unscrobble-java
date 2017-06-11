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
package net.beardbot.lastfm_unscrobble;

import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class LastFm implements LastFmMethods {
    private static final Logger log = LoggerFactory.getLogger(LastFm.class);

    private CloseableHttpClient httpClient;
    private HttpClientContext httpContext;
    private CookieStore cookieStore;

    private String userUrl = Constants.URL_USER;
    private String unscrobbleUrl = Constants.URL_UNSCROBBLE;

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        cookieStore.clear();
        httpClient.close();
    }

    /*  METHODS  */

    @Override
    public boolean unscrobble(String artist, String trackName, int timestamp) {
        String trackString = String.format("%s - %s (%d)",artist,trackName,timestamp);
        log.debug(String.format("Unscrobbling track %s -> %s",trackString,unscrobbleUrl));

        HttpPost post = new HttpPost(unscrobbleUrl);
        post.setHeader("Referer",userUrl);

        List<BasicNameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("csrfmiddlewaretoken", HttpUtils.getCookieValue(cookieStore, "csrftoken")));
        params.add(new BasicNameValuePair("artist_name", artist));
        params.add(new BasicNameValuePair("track_name", trackName));
        params.add(new BasicNameValuePair("timestamp", "" + timestamp));
        params.add(new BasicNameValuePair("ajax", "1"));

        try {
            UrlEncodedFormEntity paramEntity = new UrlEncodedFormEntity(params);
            post.setEntity(paramEntity);
        } catch (UnsupportedEncodingException e) {
            log.debug(String.format("Could not create parameter list (%s)",e.getMessage()));
        }

        CloseableHttpResponse response = null;

        try {
            response = httpClient.execute(post, httpContext);
            int statusCode = response.getStatusLine().getStatusCode();

            if(statusCode != 200){
                log.debug(String.format("Failed to unscrobble track %s! HTTP status: %s",trackString,response.getStatusLine()));
                return false;
            } else {
                log.debug(String.format("Succesfully unscrobbled track %s",trackString));
            }
        } catch (Exception e) {
            log.debug(String.format("Could not post to %s (%s)",unscrobbleUrl,e.getMessage()));
        } finally {
            try {
                HttpUtils.readResponse(response);
            } catch (Exception ignored) {}
        }

        return true;
    }

    /*  LOGIN  */

    public boolean init(String username, String password){
        httpContext = HttpClientContext.create();
        cookieStore = new BasicCookieStore();
        httpContext.setCookieStore(cookieStore);
        httpClient = HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategy()).build();

        if (!fetchCSRFToken()){
            return false;
        }

        userUrl = userUrl.replace(Constants.PLACEHOLDER_USER,username);
        unscrobbleUrl = unscrobbleUrl.replace(Constants.PLACEHOLDER_USER,username);

        return login(username,password);
    }

    private boolean login(String username, String password) {
        log.debug(String.format("Logging in with username \"%s\" and password %s",username,"********"));

        HttpPost post = new HttpPost(Constants.URL_LOGIN);
        post.setHeader("Referer",Constants.URL_LOGIN);

        List<BasicNameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("csrfmiddlewaretoken", HttpUtils.getCookieValue(cookieStore, "csrftoken")));
        params.add(new BasicNameValuePair("username", username));
        params.add(new BasicNameValuePair("password", password));

        try {
            UrlEncodedFormEntity paramEntity = new UrlEncodedFormEntity(params);
            post.setEntity(paramEntity);
        } catch (UnsupportedEncodingException e) {
            log.debug(String.format("Could not create parameter list (%s)",e.getMessage()));
        }

        CloseableHttpResponse response = null;

        try {
            response = httpClient.execute(post, httpContext);
            int statusCode = response.getStatusLine().getStatusCode();

            if(statusCode != 200){
                if(statusCode == 403 || statusCode == 401){
                    log.debug("Authentication failed! Are username and password correct?");
                }else {
                    log.debug(String.format("Login failed! HTTP status: %s",response.getStatusLine()));
                }
                return false;
            } else {
                log.debug(String.format("Succesfully logged in to account %s",username));
            }
        } catch (Exception e) {
            log.debug(String.format("Could not post to %s (%s)",Constants.URL_LOGIN,e.getMessage()));
        } finally {
            try {
                HttpUtils.readResponse(response);
            } catch (Exception ignored) {}
        }

        return true;
    }

    private  boolean fetchCSRFToken() {
        log.debug("Fetching CSRF token...");
        HttpGet post = new HttpGet(Constants.URL_LOGIN);
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(post, httpContext);
            int statusCode = response.getStatusLine().getStatusCode();

            if(statusCode != 200){
                log.debug("The server answered with a status code different from 200");
                log.debug(String.format("HTTP status: %s",response.getStatusLine()));
                return false;
            } else {
                String csrfToken = HttpUtils.getCookieValue(cookieStore, "csrftoken");
                if(csrfToken == null){
                    log.debug(String.format("%s did not answer with a CSRF token!",Constants.URL_LOGIN));
                    return false;
                }else {
                    log.debug(String.format("Fetched CSRF token: %s",csrfToken));
                }
            }

        } catch (IOException e) {
            log.debug(String.format("Could not fetch the page %s (%s)",Constants.URL_LOGIN,e.getMessage()));
        } finally {
            try {
                HttpUtils.readResponse(response);
            } catch (Exception ignored) {}
        }

        return true;
    }
}
