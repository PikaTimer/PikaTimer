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
package com.pikatimer.timing.reader;

import com.pikatimer.timing.TimingListener;
import com.pikatimer.timing.TimingLocationInput;
import com.pikatimer.timing.TimingReader;
import java.io.File;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.layout.Pane;


/**
 *  Notes: Checkbox for time of day vs time since start. 
 *  If time since start, prompt for start time that is
 * independent of the actual race time. 
 * 
 *  
 */
/**
 *
 * @author jcgarner
 */
public class PikaPCTimerFileReader implements TimingReader {
    private TimingLocationInput timingLocationInput;
    private File sourceFile; 
    private SimpleStringProperty fileName; 
    private Pane displayPane; 
    private final BooleanProperty readingStatus = new SimpleBooleanProperty();

    public PikaPCTimerFileReader(){
        
    }

    @Override
    public void setTimingListener(TimingListener t) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void showControls(Pane p) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void readOnce() {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void startReading() {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void stopReading() {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public BooleanProperty getReadingStatus() {
        return readingStatus;
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Boolean chipIsBib() {
        return Boolean.TRUE; 
    }
    
   
    
}
