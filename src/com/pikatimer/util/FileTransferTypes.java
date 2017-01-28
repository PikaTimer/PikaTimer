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
package com.pikatimer.util;

import com.pikatimer.util.fileTransports.FTPSTransport;
import com.pikatimer.util.fileTransports.LocalTransport;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author jcgarner
 */
public enum FileTransferTypes {
    LOCAL,
    //SCP,
    FTPS;
    //FTP;
    
    private static final Map<FileTransferTypes, String> InputMap = createMap();

    private static Map<FileTransferTypes, String> createMap() {
        Map<FileTransferTypes, String> result = new HashMap<>();
        result.put(LOCAL, "Local File");
        //result.put(SCP, "SCP/SFTP");
        result.put(FTPS, "FTP/FTPS");
        //result.put(FTP,"FTP (Insecure)");

        return Collections.unmodifiableMap(result);
    }
    
    @Override 
    public String toString(){
        return InputMap.get(this);
    }

    
    public final  FileTransport getNewTransport() {
                
        switch(this){
            case LOCAL:
                return new LocalTransport();
//            case SCP:
//                return new SCPTransport();
            case FTPS:
                return new FTPSTransport();
//            case FTP:
//                return new FTPTransport();
        }
        
        return null;

    }
}
