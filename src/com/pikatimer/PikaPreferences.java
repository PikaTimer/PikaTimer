/* 
 * Copyright (C) 2016 John Garner
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
package com.pikatimer;

import java.io.File;
import java.util.prefs.Preferences;

/**
 *
 * @author John
 */
public class PikaPreferences {
    /**
    * SingletonHolder is loaded on the first execution of Singleton.getInstance() 
    * or the first access to SingletonHolder.INSTANCE, not before.
    */
    
    private File recentFile;
    Preferences prefs = Preferences.userRoot().node("PikaTimer");
    
    private static class SingletonHolder { 
            private static final PikaPreferences INSTANCE = new PikaPreferences();
    }

    public static PikaPreferences getInstance() {
            return SingletonHolder.INSTANCE;
    }
    
    public void setRecentFile(File f) {
        recentFile = f;
    }
    
    public File getRecentFile() {
        return recentFile;
    }
    
    public Preferences getGlobalPreferences(){
        return prefs;
    }
    
}
