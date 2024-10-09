/*
 * Copyright (C) 2024 John Garner <segfaultcoredump@gmail.com>
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
package com.pikatimer.util;

import org.h2.store.fs.FilePath;
import org.h2.store.fs.FilePathWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */

/**
 *  This is based on the blog entry at http://shuvikov.net/blog/renaming-h2-database-files/
 *  with a bunch of modifications to just deal with the .mv.db <-> .pika use case
 *  
 *  Since this is not performance critical, we use replaceAll to aid in readability over
 *  the performance of the substring routines shown in Andryey's example linked to above 
 */
public class PikaFilePathWrapper extends FilePathWrapper {
    private static final Logger logger = LoggerFactory.getLogger(PikaFilePathWrapper.class);
    
    @Override
    public String getScheme() {
        return "pika";
    }
    @Override
    public FilePathWrapper wrap(FilePath base) {
        logger.trace("\nPikaFilePathWrapper:wrap() called with " + base.toString());
        PikaFilePathWrapper wrapper = (PikaFilePathWrapper) super.wrap(base);
        wrapper.name = getPrefix() + base.toString().replaceAll(".pika$", ".mv.db");
        logger.trace("PikaFilePathWrapper:wrap() returning " + wrapper.name);
        return wrapper;
    }

    @Override
    protected FilePath unwrap(String path) {
        logger.trace("\nPikaFilePathWrapper:unwrap() called with " + path);
        String newName = path.substring(getScheme().length() + 1);
        logger.trace("PikaFilePathWrapper:unwrap() newName is now " + newName);

        newName = newName.replaceAll(".mv.db$", ".pika");
        logger.trace("PikaFilePathWrapper:unwrap() returning " + newName);
        return FilePath.get(newName);
    }

}
