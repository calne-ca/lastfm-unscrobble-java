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

class Constants {
    static final String PLACEHOLDER_USER = "{user}";

    static final String URL_LOGIN = "https://secure.last.fm/login";
    static final String URL_USER = "https://www.last.fm/user/" + PLACEHOLDER_USER;
    static final String URL_UNSCROBBLE = "https://www.last.fm/user/" + PLACEHOLDER_USER + "/unscrobble";

    static final String FIELD_REFERER = "Referer";
    static final String FIELD_CSRFTOKEN = "csrfmiddlewaretoken";
    static final String COOKIE_CSRFTOKEN = "csrftoken";

    static final String FIELD_USERNAME = "username";
    static final String FIELD_PASSWORD = "password";

    static final String FIELD_ARTIST = "artist_name";
    static final String FIELD_TRACK = "track_name";
    static final String FIELD_TIMESTAMP = "timestamp";
}
