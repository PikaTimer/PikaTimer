/*
 *  Copyright 2015 John Garner. All rights reserved. 

 */
package com.pikatimer.timing;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 *
 * @author jcgarner
 */
public class RawTimeData {
    private LocalDateTime timestamp;
    private String chip;
    private String bib;
    private Integer timingLocationId;


    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getChip() {
        return chip;
    }

    public void setChip(String chip) {
        this.chip = chip;
    }

    public String getBib() {
        return chip;
    }

    public void setBib(String bib) {
        this.bib = bib;
    }
    
    public Integer getTimingLocationId() {
        return timingLocationId;
    }

    public void setTimingLocationId(Integer timingLocationId) {
        this.timingLocationId = timingLocationId;
    }

    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Objects.hashCode(this.timestamp);
        hash = 67 * hash + Objects.hashCode(this.chip);
        hash = 67 * hash + Objects.hashCode(this.bib);
        hash = 67 * hash + Objects.hashCode(this.timingLocationId);
        
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RawTimeData other = (RawTimeData) obj;
        if (!Objects.equals(this.timestamp, other.timestamp)) {
            return false;
        }
        if (!Objects.equals(this.chip, other.chip)) {
            return false;
        }
        if (!Objects.equals(this.bib, other.bib)) {
            return false;
        }
        if (!Objects.equals(this.timingLocationId, other.timingLocationId)) {
            return false;
        }
        
        return true;
    }
        
    
    
}
