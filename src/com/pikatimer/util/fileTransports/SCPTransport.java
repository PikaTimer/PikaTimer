/*
 * Copyright (C) 2016 jcgarner
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
package com.pikatimer.util.fileTransports;

import com.pikatimer.results.OutputPortal;
import com.pikatimer.util.FileTransport;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 *
 * @author jcgarner
 */
public class SCPTransport implements FileTransport{
    Boolean goodToGo = false;
    String basePath;
    OutputPortal parent;
    StringProperty transferStatus = new SimpleStringProperty("Idle");

    @Override
    public boolean isOK() {
        return goodToGo;
    }

    @Override
    public void save(String filename, String contents) {
        
    }

    @Override
    public void setOutputPortal(OutputPortal op) {
        parent=op;
    }

    @Override
    public void refreshConfig() {
        
    }
    @Override
    public StringProperty statusProperty() {
        return transferStatus;
    }
}
