/*
 * Copyright 2014 Charles Greb
 * Copyright 2014 Hannes Janetzek
 * Copyright 2017 devemux86
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.oscim.tiling.source;

import org.oscim.core.Tile;
import org.oscim.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Map.Entry;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OkHttpEngine implements HttpEngine {

    private static final Logger log = LoggerFactory.getLogger(OkHttpEngine.class);

    private final OkHttpClient mClient;
    private final UrlTileSource mTileSource;

    public static class OkHttpFactory implements HttpEngine.Factory {
        private final OkHttpClient mClient;

        public OkHttpFactory() {
            mClient = new OkHttpClient();
        }

        public OkHttpFactory(Cache cache) {
            mClient = new OkHttpClient.Builder()
                    .cache(cache)
                    .build();
        }

        @Override
        public HttpEngine create(UrlTileSource tileSource) {
            return new OkHttpEngine(mClient, tileSource);
        }
    }

    private InputStream inputStream;

    public OkHttpEngine(OkHttpClient client, UrlTileSource tileSource) {
        mClient = client;
        mTileSource = tileSource;
    }

    @Override
    public InputStream read() throws IOException {
        return inputStream;
    }

    @Override
    public void sendRequest(Tile tile) throws IOException {
        if (tile == null) {
            throw new IllegalArgumentException("Tile cannot be null.");
        }
        try {
            URL url = new URL(mTileSource.getTileUrl(tile));
            Request.Builder builder = new Request.Builder()
                    .url(url);
            for (Entry<String, String> opt : mTileSource.getRequestHeader().entrySet())
                builder.addHeader(opt.getKey(), opt.getValue());
            Request request = builder.build();
            Response response = mClient.newCall(request).execute();
            inputStream = response.body().byteStream();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        if (inputStream == null)
            return;

        final InputStream is = inputStream;
        inputStream = null;
        new Thread(new Runnable() {
            @Override
            public void run() {
                IOUtils.closeQuietly(is);
            }
        }).start();
    }

    /**
     * OkHttp cache implemented through {@link OkHttpClient.Builder#cache(Cache)}.
     */
    @Override
    public void setCache(OutputStream os) {
    }

    @Override
    public boolean requestCompleted(boolean success) {
        IOUtils.closeQuietly(inputStream);
        inputStream = null;

        return success;
    }
}
