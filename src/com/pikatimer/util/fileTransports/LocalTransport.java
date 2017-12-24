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

import com.pikatimer.results.ReportDestination;
import com.pikatimer.util.FileTransport;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author jcgarner
 */
public class LocalTransport implements FileTransport {
    Boolean goodToGo = false;
    Boolean stripAccents = false;
    String basePath;
    ReportDestination parent;
    StringProperty transferStatus = new SimpleStringProperty("Idle");

    @Override
    public boolean isOK() {
        return goodToGo;
    }

    @Override
    public void save(String filename, String contents) {
        System.out.println("LocalTransport.save called for " + filename);
        
        //String Accented chars if needed
        if (stripAccents) contents = StringUtils.stripAccents(contents);
        
        //Fix the newlines
        contents = contents.replaceAll("\\R", System.lineSeparator()); 
        
        
        if (goodToGo && ! basePath.isEmpty()) {
            
            try {
                Platform.runLater(() -> {transferStatus.set("Saving: " + filename);});
//                try {
//                    Thread.sleep(3000);
//                } catch (InterruptedException ex) {
//                    Logger.getLogger(LocalTransport.class.getName()).log(Level.SEVERE, null, ex);
//                }
                FileUtils.writeStringToFile(new File(FilenameUtils.concat(basePath, filename)), '\ufeff' + contents, StandardCharsets.UTF_8);
                Platform.runLater(() -> {transferStatus.set("Idle");});
            } catch (IOException ex) {
                Platform.runLater(() -> {transferStatus.set("ERROR! " + filename);});
                Logger.getLogger(LocalTransport.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void setOutputPortal(ReportDestination op) {
        parent = op; 
        refreshConfig();
    }

    @Override
    public void refreshConfig() {
        if(parent != null && parent.getBasePath() != null && ! parent.getBasePath().isEmpty()) {
            basePath = FilenameUtils.normalizeNoEndSeparator(parent.getBasePath());
            
            File baseDir = new File(basePath);
            
            stripAccents = parent.getStripAccents();

            // does it exist?
            if (!baseDir.exists()) {
              baseDir.mkdirs();
            }
            
            // it should now... 
            if (baseDir.exists()) {
                goodToGo = true;
            } else {
                goodToGo = false;
            }
            
        } else {
            basePath = null; 
        }
        
    }

    @Override
    public StringProperty statusProperty() {
        return transferStatus;
    }

    @Override
    public void test(ReportDestination parent, StringProperty output) {
        
        
        basePath = FilenameUtils.normalizeNoEndSeparator(parent.getBasePath());
            
        File baseDir = new File(basePath);

        stripAccents = parent.getStripAccents();

        

        
        if (baseDir.exists() && baseDir.isDirectory()) {
            Platform.runLater(() -> output.set(output.getValueSafe() + "Target Directory exists." ));
        } else {
            // try and create it
            Platform.runLater(() -> output.set(output.getValueSafe() + "Target Directory does not exist\nAttempting to create it..." ));
            baseDir.mkdirs();

            if (baseDir.exists() && baseDir.isDirectory()) {
                Platform.runLater(() -> output.set(output.getValueSafe() + "\nSuccessfuly created target directory." ));
            } else {
                Platform.runLater(() -> output.set(output.getValueSafe() + "\n\nFailure! Unable to create target directory." ));
                return;
            }

        }
        
        UUID tmpFileName = UUID.randomUUID();
        File tmpFile = new File(baseDir,tmpFileName.toString());
        try {
            tmpFile.createNewFile();
            tmpFile.delete();
            Platform.runLater(() -> output.set(output.getValueSafe() + "\n\nSuccess! the target directory is writable." ));

        } catch (IOException ex) {
            Platform.runLater(() -> output.set(output.getValueSafe() + "\n\nFailure! Unable to write to the target directory." ));

        } 
    }
    
    
}
