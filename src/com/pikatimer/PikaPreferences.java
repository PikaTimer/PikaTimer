/*
 * Copyright 2016 John Garner
 * All Rights Reserved 
 * 
 */
package com.pikatimer;

import java.io.File;

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
    
    
    
}
