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

import lombok.extern.slf4j.Slf4j;
import net.beardbot.lastfm.unscrobble.exception.UnscrobblerAuthenticationException;
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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static net.beardbot.lastfm.unscrobble.Constants.*;

@Slf4j
public class Unscrobbler {
    private CloseableHttpClient httpClient;
    private HttpClientContext httpContext;
    private CookieStore cookieStore;

    private String userUrl;
    private String unscrobbleUrl;

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        cookieStore.clear();
        httpClient.close();
    }

    /*  LOGIN  */

    /**
     * Performs a login to last.fm
     * @param username The username of the last.fm account
     * @param password The password of the last.fm account
     * @throws UnscrobblerAuthenticationException When the authentication failed. This will most likely be due to invalid credentials.
     */
    public void login(String username, String password) throws UnscrobblerAuthenticationException {
        httpContext = HttpClientContext.create();
        cookieStore = new BasicCookieStore();
        httpContext.setCookieStore(cookieStore);
        httpClient = HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategy()).build();

        if (!fetchCSRFToken()){
            throw new UnscrobblerAuthenticationException("Unable to fetch CSRF Token from Lastfm.");
        }

        userUrl = URL_USER.replace(PLACEHOLDER_USER,username);
        unscrobbleUrl = URL_UNSCROBBLE.replace(PLACEHOLDER_USER,username);

        if(!authenticate(username,password)){
            throw new UnscrobblerAuthenticationException("Authentication failed! Are username and password correct?");
        }
    }

    private boolean authenticate(String username, String password) {
        log.debug(String.format("Logging in with username \"%s\" and password %s",username,"********"));

        HttpPost request = new HttpPost(URL_LOGIN);
        request.setHeader(FIELD_REFERER,URL_LOGIN);

        List<BasicNameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair(FIELD_CSRFTOKEN, HttpUtils.getCookieValue(cookieStore, COOKIE_CSRFTOKEN)));
        params.add(new BasicNameValuePair(FIELD_USERNAME, username));
        params.add(new BasicNameValuePair(FIELD_PASSWORD, password));

        try {
            UrlEncodedFormEntity paramEntity = new UrlEncodedFormEntity(params);
            request.setEntity(paramEntity);
        } catch (UnsupportedEncodingException e) {
            log.warn(String.format("Failed to create parameter list: %s",e.getMessage()));
            return false;
        }

        CloseableHttpResponse response = null;

        try {
            response = httpClient.execute(request, httpContext);
            int statusCode = response.getStatusLine().getStatusCode();

            if(statusCode != 200){
                if(statusCode == 403 || statusCode == 401){
                    log.warn("Authentication failed! Are username and password correct?");
                }else {
                    log.warn(String.format("Login failed! HTTP status %s: %s",statusCode,response.getStatusLine().getReasonPhrase()));
                }
                return false;
            } else {
                log.debug(String.format("Succesfully logged in to account %s",username));
            }
        } catch (IOException e) {
            log.warn(String.format("Failed post to %s: %s",URL_LOGIN,e.getMessage()));
        } finally {
            try {
                if (response != null){
                    HttpUtils.readResponse(response);
                }
            } catch (IOException ignored) {}
        }

        return true;
    }

    private boolean fetchCSRFToken() {
        log.debug("Fetching CSRF token from %s",URL_LOGIN);
        HttpGet request = new HttpGet(URL_LOGIN);
        CloseableHttpResponse response = null;
        
        try {
            response = httpClient.execute(request, httpContext);
            int statusCode = response.getStatusLine().getStatusCode();

            if(statusCode != 200){
                log.warn("The server answered with an unexpected status code %s: %s", statusCode,response.getStatusLine().getReasonPhrase());
                return false;
            } else {
                String csrfToken = HttpUtils.getCookieValue(cookieStore, "csrftoken");
                if(csrfToken == null){
                    log.warn(String.format("%s did not answer with a CSRF token!",URL_LOGIN));
                    return false;
                }else {
                    log.debug(String.format("Fetched CSRF token: %s",csrfToken));
                }
            }
        } catch (IOException e) {
            log.warn(String.format("Failed to fetch the page %s: %s",URL_LOGIN,e.getMessage()));
            return false;
        } finally {
            try {
                if (response != null){
                    HttpUtils.readResponse(response);
                }
            } catch (IOException ignored) {}
        }

        return true;
    }

    /*  METHODS  */

    /**
     * Unscrobbles a track from a last.fm account
     * @param artist The name of the artist
     * @param trackName The name of the track
     * @param timestamp The UTC scrobble date
     * @return true if the unscrobble request was succesful; false otherwise
     */
    public boolean unscrobble(String artist, String trackName, Date timestamp){
        int seconds = (int) (timestamp.getTime()/1000);
        return unscrobble(artist,trackName,seconds);
    }

    /**
     * Unscrobbles a track from a last.fm account
     * @param artist The name of the artist
     * @param trackName The name of the track
     * @param timestamp The UNIX timestamp (the number of seconds that have elapsed since January 1, 1970 midnight UTC)
     * @return true if the unscrobble request was succesful; false otherwise
     */
    public boolean unscrobble(String artist, String trackName, int timestamp){
        if(timestamp < 0){
            return false;
        }

        return unscrobble(artist,trackName,"" + timestamp);
    }

    /**
     * Unscrobbles a track from a last.fm account
     * @param artist The name of the artist
     * @param trackName The name of the track
     * @param timestamp The UNIX timestamp (the number of seconds that have elapsed since January 1, 1970 midnight UTC)
     * @return true if the unscrobble request was succesful; false otherwise
     */
    public boolean unscrobble(String artist, String trackName, String timestamp) {
        if(artist == null || trackName == null){
            return false;
        }

        String trackString = String.format("%s - %s (%s)",artist,trackName,timestamp);
        log.debug(String.format("Unscrobbling track %s -> %s",trackString,unscrobbleUrl));

        HttpPost request = new HttpPost(unscrobbleUrl);
        request.setHeader(FIELD_REFERER,userUrl);

        List<BasicNameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair(FIELD_CSRFTOKEN, HttpUtils.getCookieValue(cookieStore, COOKIE_CSRFTOKEN)));
        params.add(new BasicNameValuePair(FIELD_ARTIST, artist));
        params.add(new BasicNameValuePair(FIELD_TRACK, trackName));
        params.add(new BasicNameValuePair(FIELD_TIMESTAMP, timestamp));

        try {
            UrlEncodedFormEntity paramEntity = new UrlEncodedFormEntity(params);
            request.setEntity(paramEntity);
        } catch (UnsupportedEncodingException e) {
            log.warn(String.format("Failed to create parameter list (%s)",e.getMessage()));
            return false;
        }

        CloseableHttpResponse response = null;

        try {
            response = httpClient.execute(request, httpContext);
            int statusCode = response.getStatusLine().getStatusCode();

            if(statusCode != 200){
                log.warn(String.format("Failed to unscrobble track %s! HTTP status %s: %s",trackString,statusCode,response.getStatusLine()));
                return false;
            } else {
                log.debug(String.format("Succesfully unscrobbled track %s",trackString));
            }
        } catch (Exception e) {
            log.warn(String.format("Failed to post to %s: %s",unscrobbleUrl,e.getMessage()));
        } finally {
            try {
                if (response != null){
                    HttpUtils.readResponse(response);
                }
            } catch (Exception ignored) {}
        }

        return true;
    }
}
