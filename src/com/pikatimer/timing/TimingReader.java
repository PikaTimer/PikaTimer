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
    
    public Boolean chipIsBib();
    
    public void setTimingListener(TimingListener t); 
    
    public void showControls(Pane p); 
    
    // This should go byby now that the Reader is displaying the file open/close dialog
    public void readOnce();
    
    // This should go away once we have the Reader display the "watch file" or whatever button
    public void startReading();
    
    // Used when the reader is no longer needed. It should 
    public void stopReading(); 
    
    // We'll keep this so that if somebody is actively reading, it floats a notification or UI to the top. 
    // i.e., the "Timing" tab turns green or something. 
    public BooleanProperty getReadingStatus();
    
}
