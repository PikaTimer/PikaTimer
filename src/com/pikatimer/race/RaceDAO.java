/*
 *  Copyright 2014 John Garner. All rights reserved. 

 */
package com.pikatimer.race;

/**
 *
 * @author jcgarner
 */
public class RaceDAO {
    
    
    /**
    * SingletonHolder is loaded on the first execution of Singleton.getInstance() 
    * or the first access to SingletonHolder.INSTANCE, not before.
    */
    private static class SingletonHolder { 
            private static final RaceDAO INSTANCE = new RaceDAO();
    }

    public static RaceDAO getInstance() {
            return SingletonHolder.INSTANCE;
    }
    
}
