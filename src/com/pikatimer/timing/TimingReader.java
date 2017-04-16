/* 
 * Copyright (C) 2017 John Garner
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
