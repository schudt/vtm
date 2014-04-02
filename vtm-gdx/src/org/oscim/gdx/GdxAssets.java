/*
 * Copyright 2013 Hannes Janetzek
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
package org.oscim.gdx;

import java.io.InputStream;

import org.oscim.backend.AssetAdapter;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class GdxAssets extends AssetAdapter {
	static String pathPrefix = "";

	private GdxAssets(String path) {
		pathPrefix = path;
	}

	@Override
	public InputStream openFileAsStream(String fileName) {
		FileHandle file = Gdx.files.internal(pathPrefix + fileName);
		if (file == null)
			throw new IllegalArgumentException("missing file " + fileName);

		try {
			return file.read();
		} catch (GdxRuntimeException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static void init(String path) {
		g = new GdxAssets(path);
	}
}