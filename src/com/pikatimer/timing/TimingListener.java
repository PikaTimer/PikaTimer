/*
 *  Copyright 2015 John Garner. All rights reserved. 

 */
package com.pikatimer.timing;

import java.time.LocalDate;

/**
 *
 * @author jcgarner
 */
public interface TimingListener {
    
    public String getAttribute(String key);
    
    public void setAttribute(String key, String value); 
    
    public void processRead(RawTimeData r); 
    
    public LocalDate getEventDate();
    
}
