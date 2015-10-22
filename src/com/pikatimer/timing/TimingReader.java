/*
 *  Copyright 2015 John Garner. All rights reserved. 

 */
package com.pikatimer.timing;

import javafx.beans.property.BooleanProperty;
import javafx.scene.layout.Pane;



/**
 *
 * @author jcgarner
 */
public interface TimingReader {
    
    
    
    public void setTimingListener(TimingListener t); 
    
    public void showControls(Pane p); 
    
    public void readOnce();
    
    public void startReading();
    
    public void stopReading(); 
    
    public BooleanProperty getReadingStatus();
    
}
