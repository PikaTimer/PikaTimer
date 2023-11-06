/*
 * Copyright (C) 2023 John Garner <segfaultcoredump@gmail.com>
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
package com.pikatimer.results.reports;

import com.pikatimer.event.Event;
import com.pikatimer.participant.CustomAttribute;
import com.pikatimer.participant.ParticipantDAO;
import com.pikatimer.race.Race;
import com.pikatimer.race.RaceDAO;
import com.pikatimer.results.ProcessedResult;
import com.pikatimer.results.RaceReport;
import com.pikatimer.results.RaceReportType;
import com.pikatimer.util.DurationFormatter;
import com.pikatimer.util.Pace;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringEscapeUtils;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
public class OverallHTML5 implements RaceReportType{
    Race race;

    Boolean showDQ = true;
    Boolean inProgress = false;
    Boolean showSplits = false;
    Boolean showSegments = false;
    Boolean showSegmentPace = false;
    Boolean showSegmentSplits = false;
    Boolean showDNF = true;
    Boolean showPace = true;
    Boolean showGun = true;
    Boolean showAwards = true;
    Boolean showCourseRecords = true;
    
    Boolean showCustomAttributes = false;
    List<CustomAttribute> customAttributesList = new ArrayList();

    Map<String,Boolean> supportedOptions = new HashMap();
    Map<String,CustomAttribute> flattenedCustomNames = new HashMap();
    
    public OverallHTML5(){
        supportedOptions.put("showDQ", true);
        supportedOptions.put("inProgress", false);
        supportedOptions.put("showSplits", false);
        supportedOptions.put("showSegments", true);
        supportedOptions.put("showSegmentPace", false);
        supportedOptions.put("showSegmentSplits", false);
        supportedOptions.put("showCustomAttributes", false);
        supportedOptions.put("showDNF", false);
        supportedOptions.put("showPace", true);
        supportedOptions.put("showGun", true);
        supportedOptions.put("hideCustomHeaders", false);
        supportedOptions.put("showAwards", true);
        supportedOptions.put("showCourseRecords", true);
    }
    
    private String escape(String s){
        return StringEscapeUtils.escapeHtml4(s).replace("'", "\\'").replace("\t", " ").replace("\\R", " ");
    }
    private String escapeHTML(String s){
        return StringEscapeUtils.escapeHtml4(s);
    }
    
    private String escapeAndFlatten(CustomAttribute a){
        String s = escape(a.getName()).replace(" ", "_").replaceAll("[^A-Za-z0-9_]{1}", "_");
        if ((flattenedCustomNames.containsKey(s) && !flattenedCustomNames.get(s).equals(a)) || s.replace("_", "").isEmpty()) return a.getUUID().replaceAll("[^A-Za-z0-9_]", "_");
        flattenedCustomNames.put(s,a);
        return s;
    }
    
    @Override
    public void init(Race r) {
        race = r;
    }

    @Override
    public Boolean optionSupport(String feature) {
        return supportedOptions.containsKey(feature);
    }
    
    @Override
    public String process(List<ProcessedResult> prList, RaceReport rr) {
        System.out.println("OverallHTML.process() Called... ");
        String report = new String();
        
        race = rr.getRace();
        OverallJSON json = new OverallJSON();
        json.init(race);
        
        
        Event event = Event.getInstance();  // fun with singletons... 
        
        supportedOptions.keySet().forEach(k -> supportedOptions.put(k, rr.getBooleanAttribute(k)));


        showDQ = supportedOptions.get("showDQ");
        inProgress = supportedOptions.get("inProgress");
        showSplits = supportedOptions.get("showSplits");
        showSegments = supportedOptions.get("showSegments");
        showSegmentPace = supportedOptions.get("showSegmentPace");
        showSegmentSplits = supportedOptions.get("showSegmentSplits");
        showDNF = supportedOptions.get("showDNF");
        showPace = supportedOptions.get("showPace");
        showGun = supportedOptions.get("showGun");
        showAwards = supportedOptions.get("showAwards");
        showCourseRecords = supportedOptions.get("showCourseRecords");
        showCustomAttributes = supportedOptions.get("showCustomAttributes");

        Boolean showCountry = false;
        Boolean showState = false;
        for (ProcessedResult x : prList){
            if (! x.getParticipant().getCountry().isEmpty()) showCountry=true;
            if (! x.getParticipant().getState().isEmpty()) showState=true;
        }
        

        
        Boolean customHeaders = race.getBooleanAttribute("useCustomHeaders");
        Boolean textOnlyHeaders = race.getBooleanAttribute("textOnlyHeaders");
        if (customHeaders == null || (customHeaders == true && supportedOptions.get("hideCustomHeaders"))) customHeaders = false;
        if (customHeaders && textOnlyHeaders == null) textOnlyHeaders = false;
        
        String dispFormat = race.getStringAttribute("TimeDisplayFormat");
        String roundMode = race.getStringAttribute("TimeRoundingMode");
        Pace pace = Pace.valueOf(race.getStringAttribute("PaceDisplayFormat"));
        
        if (showCustomAttributes) customAttributesList= ParticipantDAO.getInstance().getCustomAttributes().stream().filter(a -> { 
            if (rr.getBooleanAttribute(a.getUUID()) != null )
                return rr.getBooleanAttribute(a.getUUID());
            return false;
        }).collect(Collectors.toList());
        if (showCustomAttributes && customAttributesList.isEmpty()) showCustomAttributes = false;
        
        Integer dispFormatLength;  // add a space
        if (dispFormat.contains("[HH:]")) dispFormatLength = dispFormat.length()-1; // get rid of the two brackets and add a space
        else dispFormatLength = dispFormat.length()+1;
        
        Duration cutoffTime = Duration.ofNanos(race.getRaceCutoff());
        String cutoffTimeString = DurationFormatter.durationToString(cutoffTime, dispFormat, roundMode);
        
       
        report += "<HTML lang=\"en\" xml:lang=\"en\" > " +  System.lineSeparator();
        report += "  <HEAD> " +  System.lineSeparator();
        report +=   " <meta charset=\"UTF-8\">\n" +
                    //"<meta name=\"google\" content=\"notranslate\">\n" +
                    "<meta http-equiv=\"Content-Language\" content=\"en\">\n\n";
        
        report += "<TITLE> " + event.getEventName();
        if (RaceDAO.getInstance().listRaces().size() > 1) 
            report += " " + race.getRaceName() ;
        report += " Results " + event.getLocalEventDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)) + "</TITLE>" + System.lineSeparator();
        report += System.lineSeparator();
        report += "<meta name=\"viewport\" content=\"width=device-width; initial-scale=1.0; maximum-scale=1.0; minimum-scale=1.0; user-scalable=0;\" />\n";
        
// Google Analytics
        if (customHeaders && race.getStringAttribute("GACode") != null && !race.getStringAttribute("GACode").isEmpty()) {
            report +=   "<!-- Google Analytics -->\n" +
                        "<script>\n" +
                        "window.ga=window.ga||function(){(ga.q=ga.q||[]).push(arguments)};ga.l=+new Date;\n" +
                        "ga('create', '" + race.getStringAttribute("GACode") + "', 'auto');\n" +
                        "ga('send', 'pageview');\n" +
                        "</script>\n" +
                        "<script async src='https://www.google-analytics.com/analytics.js'></script>\n" +
                        "<!-- End Google Analytics -->\n" ;
                        
            report += System.lineSeparator();
        }
        
        report +=   "<!-- Stylesheets / JS Includes-->\n" ;
        if (inProgress) report +=   "<link rel=\"stylesheet\" type=\"text/css\" href=\"https://cdn.datatables.net/v/dt/jq-3.7.0/dt-1.13.7/fh-3.4.0/r-2.5.0/sc-2.3.0/datatables.min.css\"/>\n" ;
        else report +=   "<link rel=\"stylesheet\" type=\"text/css\" href=\"https://cdn.datatables.net/v/dt/jq-3.7.0/jszip-3.10.1/dt-1.13.7/b-2.4.2/b-html5-2.4.2/b-print-2.4.2/r-2.5.0/sc-2.3.0/datatables.min.css\"/>" ;
        report +=   "<link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/font-awesome/4.3.0/css/font-awesome.min.css\">\n" +
                "<link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/flag-icon-css/3.4.6/css/flag-icon.min.css\">\n" +
                    " \n" ;
        
        // our inline CSS
        report +=   "<link href=\"https://fonts.googleapis.com/css?family=Source+Sans+Pro|Open+Sans\" rel=\"stylesheet\">\n" +
                    "<style>\n" +
                    ".fa.fa-trophy {\n" +
                    "    color: gold;\n" +
                    "}\n" +
                    ".fa.fa-bolt {\n" +
                    "    color: red;\n" +
                    "}\n" +
                    "table.dataTable.display tbody tr.child {\n" +
                    "    background: white;\n" +
                    "}\n" +
                    "table.dataTable.display tbody tr.child:hover {\n" +
                    "    background: white !important;\n" +
                    "}" +
                    ".row{padding-bottom: 5px;}\n" +
                    ".row::after {\n" +
                    "    content: \"\";\n" +
                    "    clear: both;\n" +
                    "    display: table;\n" +
                    "}\n" +
                    ".right {text-align: right;}\n" +
                    ".hide {display: none;}\n" +
                    ".up-half {transform: translateY(-50%); border-top: none !important;}\n" +
                    ".event-info {font-family: 'Open Sans'; font-size: 36px; text-align: center;}\n" +
                    ".event-date {font-family: 'Open Sans'; font-size: 24px; text-align: center;}\n" +
                    ".in-progress {font-family: 'Open Sans'; font-size: 30px; text-align: center; color: red;}\n" +
                    ".participant {float: left; padding-right: 25px;}\n" +
                    ".overall {float: left; padding-right: 40px;}\n" +
                    ".part-name {font-family: 'Source Sans Pro'; font-size: 36px; text-align: left; white-space: pre-wrap; margin-left: -2px;}\n" +
                    ".part-stats {font-family: 'Source Sans Pro'; font-size: 20px; padding-left: 10px; text-align: left; white-space: pre-wrap; border-left-style: solid; border-left-width: medium; border-left-color: gray; padding-left: 4px}\n" +
                    ".finish-time {font-family: 'Source Sans Pro'; font-size: 36px; text-align: left; margin-left: -2px;}\n" +
                    ".finish-stats {font-family: 'Source Sans Pro'; font-size: 20px; padding-left: 10px; text-align: left; white-space: pre-wrap; border-left-style: solid; border-left-width: medium; border-left-color: gray; padding-left: 4px}\n" +
                    ".segment {float: left; padding-right: 40px; }\n" +
                    ".segment-title {font-family: 'Source Sans Pro'; font-size: 30px; text-align: left; white-space: pre-wrap; padding-top: 15px; margin-left: -2px;}\n" +
                    ".segment-head {font-family: 'Source Sans Pro'; font-size: 24px; text-align: left; white-space: pre-wrap; margin-left: -2px;}\n" +
                    ".segment-time {font-family: 'Source Sans Pro'; font-size: 20px; text-align: left; white-space: pre-wrap; border-left-style: solid; border-left-width: medium; border-left-color: gray; padding-left: 4px}\n" +
                    ".segment-stats {font-family: 'Source Sans Pro'; font-size: 18px; text-align: left; white-space: pre-wrap; border-left-style: solid; border-left-width: medium; border-left-color: gray; padding-left: 4px}\n" +
                    ".split {float: left; }\n" +
                    ".split-title {font-family: 'Source Sans Pro'; font-size: 24px; text-align: left; white-space: pre-wrap; padding-top: 15px;}\n" +
                    ".split-time {font-family: 'Source Sans Pro'; font-size: 16px; text-align: left; white-space: pre-wrap;}\n" +
                    ".share {float: left; padding-right: 15px; padding-bottom: 10px;}\n" +
                    ".share-title {font-family: 'Source Sans Pro'; font-size: 20px; text-align: left; white-space: pre-wrap;}\n" +
                    ".share-link {font-family: 'Source Sans Pro'; font-size: 16px; text-align: left; padding-left: 5px; white-space: pre-wrap;}\n" +
                    ".share-link a:link {text-decoration: none;}\n" +
                    ".toolbar {float:left; padding-right: 2px; font-family: 'Source Sans Pro'; font-size: 16px; text-align: left; white-space: pre-wrap;}\n" +
                    ".buttons {float:right; padding-right: 2px;}\n " +
                    ".up-10 {transform: translateY(-10%);}\n" +
                    ".bold {font-weight: bold;}\n" +
                    ".show-mobile {display:none}\n" +
                    "@media only screen and (max-width: 600px) {\n" +
                    "    /* For smart phones in portrait mode: */\n" +
                    "    .participant {width: 100%; padding-right: 0px; }\n" +
                    "    .part-stats {padding-left: 5px;}\n" +
                    "    .finish-stats {padding-left: 5px;}\n" +
                    "    .overall {width: 100%; padding-right: 0px;}\n" +
                    "    .segment {width: 100%; padding-right: 0px;}\n" +
                    "    .split {width: 100%; padding-right: 0px;}\n" +
                    "    .toolbar {width:100%; text-align: center; float:none;}\n" +
                    "    .hide-mobile {display:none}\n" +
                    "    .show-mobile {display:initial}\n" +
                    "}\n" +
                    "</style>\n";
        // Custom CSS 
        if (customHeaders && race.getStringAttribute("CSSUrl") != null && !race.getStringAttribute("CSSUrl").isEmpty()) {
            report += "<link rel=\"stylesheet\" type=\"text/css\" href=\"" + race.getStringAttribute("CSSUrl") + "\"/>\n";
            report += System.lineSeparator();
        }
        report += "<!-- End Stylesheets / JS Indludes-->\n";
        
        report += "  </HEAD> " +  System.lineSeparator();
        report += "  <BODY> " +  System.lineSeparator();
        
        if (customHeaders){
            if (textOnlyHeaders) report += "<pre>" + race.getStringAttribute("textHeader") +"</pre>";
            else report += race.getStringAttribute("htmlHeader");
            report += System.lineSeparator();
        }
        
        report += "<div class=\"event-info\">" + event.getEventName() + System.lineSeparator();;
        if (RaceDAO.getInstance().listRaces().size() > 1) 
            report += "<br>" + race.getRaceName();
        report += "</div>" + System.lineSeparator();
        
        report += "<div class=\"event-date\">" + event.getLocalEventDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)) + "</div>" + System.lineSeparator();
        report += System.lineSeparator();
        
        Boolean dataToShow = false;
        if(inProgress) {
            report += "    <div class=\"in-progress\">" + "*In Progress*" + "</div>" + System.lineSeparator();
            report += System.lineSeparator();
            if (!prList.isEmpty()) dataToShow = true;
        } else {
            if (! prList.isEmpty()){
                if (showDNF) dataToShow = true;
                else {
                    for(ProcessedResult f: prList){
                        if (f.getChipFinish() != null ) {
                            dataToShow = true;
                            break;
                        }
                    }
                }
            }
        }
        
        if (customHeaders){
            if (textOnlyHeaders) report += "<pre>" + race.getStringAttribute("textMessage") +"</pre>";
            else report += race.getStringAttribute("htmlMessage");
            report += System.lineSeparator();
        }
        
        
        
        if(!dataToShow) {
            report += "    <div class=\"in-progress\">" + "<BR>*No Results Have Been Posted Yet*" + "</div>" + System.lineSeparator();
            report += System.lineSeparator();
            if (customHeaders){
                if (textOnlyHeaders) report += "<pre>" + race.getStringAttribute("textFooter") +"</pre>";
                else report += race.getStringAttribute("htmlFooter");
                report += System.lineSeparator();
            }
        } else {
            final Boolean  penaltiesOrBonuses = prList.stream().anyMatch(s -> (s.getBonus() || s.getPenalty()));
            final Boolean onCourseOCO = prList.stream().anyMatch(s -> s.getSplitOCO());
        
        // Start the table
            report += "    <div id=\"loading\" class=\"in-progress right\">" + "<BR>Loading..." + "</div>" + System.lineSeparator();

            //report += "<div id=\"results_table\" class=\"hide\">" +  System.lineSeparator();
            report += "  <TABLE id=\"results\" class=\"display responsive dtr-column nowrap compact\" > " +  System.lineSeparator();
            // print the headder
            report += "    <thead><tr>" +  System.lineSeparator();
            report += "      <th class=\"all\"></th>"+  System.lineSeparator(); // dummy for control box
            report += "      <th data-priority=\"2\">OA#</th>" +  System.lineSeparator();
            report += "      <th data-priority=\"4\">SEX#</th>" +  System.lineSeparator();
            report += "      <th data-priority=\"6\">AG#</th>" +  System.lineSeparator();
            report += "      <th data-priority=\"20\">Bib</th>" +  System.lineSeparator(); 
            report += "      <th data-priority=\"21\">Age</th>" +  System.lineSeparator(); 
            report += "      <th data-priority=\"3\">Sex</th>" +  System.lineSeparator(); 
            report += "      <th data-priority=\"5\">AG</th>" +  System.lineSeparator(); 
            report += "      <th data-priority=\"1\" class=\"all\">Name</th>" +  System.lineSeparator(); 
            report += "      <th data-priority=\"1000\">First</th>" +  System.lineSeparator();
            report += "      <th data-priority=\"1000\">Middle</th>" +  System.lineSeparator();
            report += "      <th data-priority=\"1000\">Last</th>" +  System.lineSeparator();
            report += "      <th data-priority=\"41\">City</th>" +  System.lineSeparator(); 
            if (showState) report += "      <th data-priority=\"40\">ST</th>" +  System.lineSeparator(); 
            if (showCountry) report += "      <th data-priority=\"45\">Country</th>" +  System.lineSeparator(); 

            if (showCustomAttributes) {
                for( CustomAttribute a: customAttributesList){
                    report += "      <th data-priority=\"200\">"+escapeHTML(a.getName())+ "</th>" +  System.lineSeparator();
                }
            }
            // Insert split stuff here
            if (showSplits) {
                for (int i = 2; i < race.splitsProperty().size(); i++) {
                    if (!race.splitsProperty().get(i-1).getIgnoreTime()) report += "      <th data-priority=\"100\">" + escapeHTML(race.splitsProperty().get(i-1).getSplitName()) + "</th>" +  System.lineSeparator();
                }
            }
            if (showSegments) {
                final StringBuilder chars = new StringBuilder();
                race.raceSegmentsProperty().forEach(seg -> {
                    if (seg.getHidden()) return;
                    chars.append("      <th data-priority=\"80\">" + escapeHTML(seg.getSegmentName())+ "</th>" +  System.lineSeparator());
                    //if (showSegmentPace) chars.append("      <th data-priority=\"95\"> Pace</th>" +  System.lineSeparator()); // pace.getFieldWidth()+1
                });
                report += chars.toString();
            }
            // Chip time
            report += "      <th data-priority=\"1\" class=\"all\">Finish</th>" +  System.lineSeparator(); // 9R Need to adjust for the format code

            // gun time
            if (showGun) report += "      <th data-priority=\"90\">Gun</th>" +  System.lineSeparator(); // 9R ibid
            // pace
            if (showPace) report += "      <th data-priority=\"85\">Pace</th>" +  System.lineSeparator(); // 10R

            report += "</tr></thead>" +  System.lineSeparator(); 
            report += "</table>" +  System.lineSeparator();
            if (!inProgress) report += "<div id=\"btn\" class=\"buttons\"></div>" +  System.lineSeparator();

        
        
        if (customHeaders){
            if (textOnlyHeaders) report += "<pre>" + race.getStringAttribute("textFooter") + "</pre>";
            else report += race.getStringAttribute("htmlFooter");
            report += System.lineSeparator();
        }

        report += "<!-- Start DataTables -->\n";
        
        
        if (inProgress) report += "<script type=\"text/javascript\" src=\"https://cdn.datatables.net/v/dt/jq-3.7.0/dt-1.13.7/fh-3.4.0/r-2.5.0/sc-2.3.0/datatables.min.js\"></script>\n" ;
        else report +=   "<script type=\"text/javascript\" src=\"https://cdnjs.cloudflare.com/ajax/libs/pdfmake/0.1.36/pdfmake.min.js\"></script>\n" +
                        "<script type=\"text/javascript\" src=\"https://cdnjs.cloudflare.com/ajax/libs/pdfmake/0.1.36/vfs_fonts.js\"></script>\n" +
                        "<script type=\"text/javascript\" src=\"https://cdn.datatables.net/v/dt/jq-3.7.0/jszip-3.10.1/dt-1.13.7/b-2.4.2/b-html5-2.4.2/b-print-2.4.2/r-2.5.0/sc-2.3.0/datatables.min.js\"></script>\n";

        report +=           " \n" +
                    "<script type=\"text/javascript\" src=\"https://cdn.datatables.net/plug-ins/1.13.6/sorting/natural.js\"></script>\n";

        report += "<script type=\"text/javascript\" class=\"init\">\n" +
                    " // nth(n) function from http://stackoverflow.com/questions/13627308/add-st-nd-rd-and-th-ordinal-suffix-to-a-number \n" + 
                    "function nth(s){var n = parseInt(s); if (isNaN(n)) return s; return s + ([\"st\",\"nd\",\"rd\"][((n+90)%100-10)%10-1]||\"th\");}\n" +
                
                "function childData ( rData) {\n" +
                        "                \n" +
                        "                               if (typeof rData == \"undefined\") { return \"\";}" +
                        "				var data = '<div class=\"detail\">';\n" +
                        "				data += '<div class=\"row\">';\n" +
                        "				data += '<div class=\"participant\">' // personal\n" +
                        "				data += '<div class=\"part-name\">' + rData.full_name + '</div>';\n" +
                        "				data += '<div class=\"part-stats\">Bib: ' + rData.bib + '</div>';\n" +
                        "				data += '<div class=\"part-stats\">Age: ' + rData.age + '   Sex: ' + rData.sex + '   AG: ' + rData.ag + '</div>';\n";
            if (showState) report +=                        "				data += '<div class=\"part-stats\">' + rData.city + ', ' + rData.state + '</div>';\n" ;
            if (showCountry) report +=            "				data += '<div class=\"part-stats\">' + rData.country + ' <span class=\"flag-icon flag-icon-' +  rData.country.toLowerCase() +'\" ></span></div>';\n";
            if (showCustomAttributes) {
                for( CustomAttribute a: customAttributesList){
                    report += "				data += '<div class=\"part-stats\">" + escape(a.getName()) +": ' + rData.custom_" + escapeAndFlatten(a) +" + '</div>';\n";
                }
            }
            
            
            report +=  "				data += '</div>'; // personal\n" +
                        "				data += '<div class=\"overall\">';// time\n" +
                        "                               if ( rData.oa_place == \"DQ\" ) {\n" +
                        "					data += '<div class=\"finish-time\">DISQUALIFIED</div>';\n" +
                        "					data += '<div class=\"finish-stats\">' + rData.note + '</div>';\n" +
                        "                                       data += '</div>'; // time\n" +
                        "                                       data += '</div>'; // row\n" +
                        "                                       data += '</div>'; // detail\n" +
                       "                                       return data; // row\n" +
                        "                               } else if ( rData.oa_place == \"DNF\" ) {\n" +
                        "					data += '<div class=\"finish-time\">Did Not Finish  :-( </div>';\n" +
                       "					if (rData.note != \"\" ) data += '<div class=\"finish-stats\">' + rData.note + '</div>';\n";
            if (inProgress) report += "					if (rData.last_seen != \"\" ) data += '<div class=\"finish-stats\">Last Seen: ' + rData.last_seen + '</div>';\n";
            report +=   "                               } else if ( rData.oa_place == \"Started\" ) {\n" +
                        "					data += '<div class=\"finish-time\">Started</div>';\n" +
                        "                                   data += '<div class=\"finish-stats bold\">Last Seen: ' + rData.last_seen + '</div>';\n" +
                        "					if (rData.note != \"\" ) data += '<div class=\"finish-stats\">' + rData.note + '</div>';\n" +
                        "                               } else if ( rData.oa_place == \"OCO\" ) {\n" +
                        "					data += '<div class=\"finish-time\">Over Cut Off  :-/</div>';\n" +
                        "					if (rData.on_course_oco == \"true\") {\n" +
                        "						data += '<div class=\"finish-stats\"> Split: ' + rData.oco_split + '</div>';\n" +
                        "						data += '<div class=\"finish-stats\"> Split time: ' + rData.oco_time + '</div>';\n" +
                        "                                               data += '<div class=\"finish-stats\"> Cutoff time: ' + rData.oco_cutoff_time + '</div>';\n" +
                        "					} else {" +
                        "                                           data += '<div class=\"finish-stats\"> Finish time: ' + rData.finish_display + '</div>';\n" +
                        "                                           data += '<div class=\"finish-stats\"> Cutoff: " + race.raceCutoffProperty().getValueSafe() + "</div>';\n" +
                        "					}\n" +
                        "				} else {\n" +
                        "                                   data += '<div class=\"finish-time\"><span class=\"hide-mobile\">Finish Time: </span>' + rData.finish_display + '</div>';\n" ;
            if (showGun) report += "                                   data += '<div class=\"finish-stats\">Gun Time: ' + rData.gun_display + '</div>';\n"; 
            report +=   "                                   data += '<div class=\"finish-stats\">Overall: ' + nth(rData.oa_place) + '   Sex: ' + nth(rData.sex_place) + '   <span class=\"hide-mobile\">Age Group:</span><span class=\"show-mobile\">AG:</span> ' + nth(rData.ag_place) + '</div>';\n" ;
            if (showPace) report += "                                   data += '<div class=\"finish-stats\">Pace: ' + rData.finish_pace + '</div>';\n" ;
            if (penaltiesOrBonuses) report +=   "                                    if (rData.penalty == \"true\" || rData.bonus == \"true\") { \n" +
                                                "										if (rData.bonus == \"true\") { data += '<div class=\"finish-time\"><span class=\"hide-mobile\">Time </span>Bonus: ' + rData.bonus_time + '</div>';}\n" +
                                                "										if (rData.penalty == \"true\") { data += '<div class=\"finish-time\"><span class=\"hide-mobile\">Time </span>Penalty: ' + rData.penalty_time + '</div>';}\n" +
                                                "										data += '<div class=\"finish-stats\">Reason: ' + rData.penalty_bonus_note + '</div>';\n" +
                                                "										data += '<div class=\"finish-stats\">Raw Chip Time: ' + rData.raw_chip_time + '</div>';\n" ;
            if (penaltiesOrBonuses && showGun) report +=              "										data += '<div class=\"finish-stats\">Raw Gun Time: ' + rData.raw_gun_time + '</div>';\n" ;
            if (penaltiesOrBonuses) report +=   "										}\n" ;
            
            report +=   "				}" +
                        "				data += '</div>'; // time\n" +
                        "				\n" +
                        "                data += '</div>'; // row\n" +
                        "				\n";
            if (showAwards) {
                report += " if (rData.award_winner == \"yes\") {\n" +
                    "					data += '<div class=\"row\">';\n" +
                    "					data += '<div class=\"segment segment-title\">Award Winner:';\n" +
                    "					data += '</div>';\n" +
                    "					data += '</div>';\n" +
                    "					data += '<div class=\"row\">';\n" +
                    "					data += '<div class=\"segment\">'; \n" +
                    "					for (i in rData.awards){\n" +
                    "						data += '<div class=\"segment-time\">' + rData.awards[i];\n" +
                    "						data += '</div>';\n" +
                    "					}\n" +
                    "					data += '</div>';\n" +
                    "					data += '</div>';\n" +
                    "				}";
            }
            
            if (showCourseRecords) {
                report += " if (rData.course_records == \"yes\") {\n" +
                    "					data += '<div class=\"row\">';\n" +
                    "					data += '<div class=\"segment segment-title\">Course Records:';\n" +
                    "					data += '</div>';\n" +
                    "					data += '</div>';\n" +
                    "					data += '<div class=\"row\">';\n" +
                    "					data += '<div class=\"segment\">'; \n" +
                    "					for (i in rData.course_record_detail){\n" +
                    "						data += '<div class=\"segment-time\"> ';\n" +    
                    "                                           data += rData.course_record_detail[i].segment + ' '; \n" +
                    "                                           if (rData.course_record_detail[i].category != \"OVERALL\") { data += rData.course_record_detail[i].category;} \n" + 
                    "						data += ': New Record ' +rData.course_record_detail[i].new_time ;\n" +
                    "						data += '</div>';\n" +
                    "						data += '<div class=\"segment-time\"> ';\n" +     
                    "						data += 'Previous: ' + rData.course_record_detail[i].old_time + ' in ' + rData.course_record_detail[i].old_year + ' by ' + rData.course_record_detail[i].old_name+'';\n" +
                    "						data += '</div>';\n" +
                    "                                           data += '<div class=\"segment-time\"><hr></div> ';\n" +
                    "					}\n" +
                    "					data += '</div>';\n" +
                    "					data += '</div>';\n" +
                    "				}";
            }
        
            if (showSegments && race.raceSegmentsProperty().stream().anyMatch(s -> !s.getHidden())) {
                final StringBuilder chars = new StringBuilder();
                chars.append("data += '<div class=\"row\">';\n");
                chars.append("data += '<div class=\"segment segment-title\">Segments: '; // time\n");
                chars.append("data += '</div>';\n");
                chars.append("data += '</div>';\n");

                chars.append("data += '<div class=\"row\">';\n");
                race.raceSegmentsProperty().forEach(seg -> {
                    if (seg.getHidden()) return;
                    chars.append("data += '<div class=\"segment\">'; // time\n");
                    chars.append("data += '<div class=\"segment-head\">" + escape(seg.getSegmentName())+ "</div>';\n" );
                    chars.append("data += '<div class=\"segment-time\">Time: ' + rData.segments[\"segment_"+seg.getID()+ "\"].display + '</div>';\n");
                    chars.append("data += '<div class=\"segment-stats\">Overall: ' + nth(rData.segments[\"segment_"+seg.getID()+ "\"].oa_place) + '   Sex: ' + nth(rData.segments[\"segment_"+seg.getID()+ "\"].sex_place) + '   AG: ' + nth(rData.segments[\"segment_"+seg.getID()+ "\"].ag_place) + '</div>';\n");
                    if (showSegmentPace) {
                        if (! (seg.getUseCustomPace() && Pace.NONE.equals(seg.getCustomPace()))) 
                            chars.append("data += '<div class=\"segment-stats\">Pace:  ' + rData.segments[\"segment_"+seg.getID()+ "\"].pace + '</div>';\n");
                    }
                    if (showSegmentSplits){
                        chars.append("data += '<div class=\"row\">'; \n");
                        chars.append("data += '<div class=\"segment-stats\">'; \n");
                        chars.append("data += '<table class=\"split-time\">' ;\n");
                        chars.append("data += '<thead><tr>';\n");
                        chars.append("data += '<th>Split</th>';\n");
                        chars.append("data += '<th>Elapsed</th>';\n");
                        chars.append("data += '<th>Difference</th>';\n");
                        if (showSegmentPace) chars.append("data += '<th class=\"right\">Pace</th>';\n");
                        chars.append("data += '</tr></thead>';\n");
                        for (int i = seg.getStartSplitPosition(); i < seg.getEndSplitPosition()+1; i++) {
                            if (i == 1){ // the start, so pull the data from the start
                                chars.append("data += '<tr><td>Start:</td><td class=\"right\">' + rData.start_display + '</td><td></td>';\n");
                                if (showSegmentPace) chars.append("data += '<td></td>';\n");
                                chars.append("data += '</tr>';\n");
                            } else if (!race.splitsProperty().get(i-1).getIgnoreTime()) {
                                chars.append("data += '<tr><td>" + escape(race.splitsProperty().get(i-1).getSplitName()) + ":</td><td class=\"right\">  ' + rData.splits[\"split_" + Integer.toString(i-1) + "\"].display + '</td>';\n");
                                // if the first split of the segment
                                // don't show the elapsed time from the previous segment
                                if (i == seg.getStartSplitPosition()){ 
                                    chars.append("data += '<td></td>';\n");
                                    if (showSegmentPace) chars.append("data += '<td></td>';\n");
                                } else {
                                    chars.append("data += '<td class=\"right up-half\">  ' + rData.splits[\"split_"+ Integer.toString(i-1) + "\"].delta_time + '</td>';\n");
                                    if (showSegmentPace) chars.append("data += '<td class=\"right up-half\">  ' + rData.splits[\"split_"+ Integer.toString(i-1) + "\"].segment_" + seg.getID() + "_pace + '</td>';\n");
                                }
                                chars.append("data += '</tr>';\n");
                            }
                            }
                        chars.append("data += '</tr>';\n");
                        chars.append("data += '</table>';\n");
                        chars.append("data += '</div>';\n"); // split
                        chars.append("data += '</div>';\n"); // row
                    }
                    chars.append("data += '</div>';\n"); // segment
                });
                chars.append("data += '</div>';\n"); // row
                report += chars.toString();
            }
            if (showSplits) {
                report += "data += '<div class=\"row\">'; \n";
                report += "data += '<div class=\"split\">'; \n";
                report += "data += '<div class=\"split-title\">Splits:</div>';\n" ;
                report += "data += '<table class=\"split-time\">' ;\n" ;
                report += "data += '<thead><tr>';\n";
                report += "data += '<th>Split</th>';\n";
                report += "data += '<th>Elapsed</th>';\n";
                report += "data += '<th>Difference</th>';\n";
                if (showPace) report += "data += '<th class=\"right\">Pace</th>';\n";
                report += "data += '</tr></thead>';\n";
                report += "data += '<tr><td>Start:</td><td class=\"right\">' + rData.start_display + '</td><td></td>';\n" ;
                if (showPace) report += "data += '<td></td>';\n";
                report += "data += '</tr>';\n";
                for (int i = 2; i <= race.splitsProperty().size(); i++) {
                    if (!race.splitsProperty().get(i-1).getIgnoreTime()) {
                    report += "data += '<tr><td>" + escape(race.splitsProperty().get(i-1).getSplitName()) + ":</td><td class=\"right\">  ' + rData.splits[\"split_" + Integer.toString(i-1) + "\"].display + '</td>';\n";
                    report += "data += '<td class=\"right up-half\">  ' + rData.splits[\"split_"+ Integer.toString(i-1) + "\"].delta_time + '</td>';\n";
                    if (showPace) report += "data += '<td class=\"right up-half\">  ' + rData.splits[\"split_"+ Integer.toString(i-1) + "\"].pace + '</td>';\n";
                    report += "data += '</tr>';\n";
                    }
                }
                report += "data += '</tr>';\n";
                report += "data += '</table>';\n";
                report += "data += '</div>';\n"; // split
                report += "data += '</div>';\n"; // row
            }
            report += "data += '    <div class=\"share\" >';\n" +
                        "data += '    <div class=\"share-title\">Share:</div>';\n" +
                        "data += '    <div class=\"share-link\"><a href=\"http://twitter.com/intent/tweet?text=' + window.location.origin + window.location.pathname + '%23' + rData.bib + encodeURI(encodeURI(' ' + rData.full_name)) + '\" target=\"_blank\">';\n" +
                        "data += '<i class=\"fa fa-twitter\" aria-hidden=\"true\"></i> Twitter';\n" +
                        "data += '</a></div>';\n" +
                        "data += '    <div class=\"share-link\"><a href=\"http://www.facebook.com/sharer.php?u=' + window.location.origin + window.location.pathname + '#' + rData.bib + encodeURI(' ' +rData.full_name) +'\" target=\"_blank\">';\n" +
                        "data += '<i class=\"fa fa-facebook\" aria-hidden=\"true\"></i> Facebook';\n" +
                        "data += '</a></div>';\n" +
                        "data += '  </div>';\n" ;
                                        
                            

                        
            report +=   " \n" +
                        "       data += '</div>'; // detail\n" +
                        "\n" +
                        "       return data;\n" +

                        "   }\n\n" +
                
                
                
                    " var resultsData = " + json.process(prList, rr) +
                    "\n" +
                    "$(document).ready(function() {\n" +
                    "var search = \"\";\n" +
                    "	if ( window.location.hash !== \"\" ) {\n" +
                    "		search = decodeURI(window.location.hash.substring( 1 ));\n" +
                    "	}\n" +
                    "var oTable = $('#results').DataTable({\n" +
                        "   data: resultsData,\n" +
                        "        \"columns\": [\n" +
                        "           { \"data\": null, \"defaultContent\": \"\", className: 'control', orderable: false, targets:   0 },\n" +
                        "           { \"data\": \"oa_place\" },\n" +
                        "           { \"data\": \"sex_place\" },\n" +
                        "           { \"data\": \"ag_place\" },\n" +
                        "           { \"data\": \"bib\" },\n" +
                        "           { \"data\": \"age\" },\n" +
                        "           { \"data\": \"sex\" },\n" + // If this index changes, change the filter below
                        "           { \"data\": \"ag\" },\n" +  // ibid
                         "           { \"data\": \"full_name\", \"render\":  \n" +
"					 function (data, type, row) {\n" +
"						if (type == \"sort\" || type === 'type') return data;\n" +
"						if (type == \"filter\" || type === 'type') return row.full_name_filter;\n"    +              
"						if (row.award_winner == \"yes\" && row.course_records == \"yes\") \n" +
"						   return '<i class=\"fa fa-trophy\" aria-hidden=\"true\"></i><i class=\"fa fa-bolt\" aria-hidden=\"true\"></i> ' + row.full_name;\n" +
"						if (row.award_winner == \"yes\") \n" +
"						   return '<i class=\"fa fa-trophy\" aria-hidden=\"true\"></i> ' + row.full_name;\n" +
"						if (row.course_records == \"yes\") \n" +
"						   return '<i class=\"fa fa-bolt\" aria-hidden=\"true\"></i> ' + row.full_name;\n" +                    
"						else return row.full_name;\n" +
"					 }\n" +
"				 \n" +
"			}," +
                        "           { \"data\": \"first_name\" },\n" +
                        "           { \"data\": \"middle_name\" },\n" +
                        "           { \"data\": \"last_name\" },\n" +

                        "           { \"data\": \"city\" },\n" ;
            if (showState) report +=             "           { \"data\": \"state\" },\n";
            if (showCountry) {
                        report +=  "			{ \"data\": \"country\", \"render\": \n" +
                        "				function (data, type, row) {\n" +
                        "					if (type == \"sort\" || type === 'type') return data;\n" +
                        "					if (type == \"filter\" || type === 'type') return data;\n" +
                        "					return  row.country + ' <span class=\"flag-icon flag-icon-' + row.country.toLowerCase() +'\" ></span>';\n" +
                        "				}\n" +
                        "				 \n" +
                        "			},\n"  ;
            }
            if (showCustomAttributes) {
                for( CustomAttribute a: customAttributesList){
                    report += "           { \"data\": \"custom_" + escapeAndFlatten(a) +"\" },\n";
                }
            }
            if (showSplits) {
                for (int i = 2; i < race.splitsProperty().size(); i++) {
                    if (!race.splitsProperty().get(i-1).getIgnoreTime()) report += "           { \"data\": \"splits.split_" + Integer.toString(i-1) + "\", \n" +
                                "				\"render\": {\n" +
                                "					_: 'display',\n" +
                                "					sort: 'sort'\n" +
                                "				} \n" +
                                "			},\n"; //+ race.splitsProperty().get(i-1).getSplitName() + "</th>" +  System.lineSeparator();
                }
            }
            if (showSegments) {
                final StringBuilder chars = new StringBuilder();
                Integer dispLeg = dispFormatLength;
                race.raceSegmentsProperty().forEach(seg -> {
                    if (seg.getHidden()) return;
                    chars.append("           { \"data\": \"segments.segment_" +  seg.getID()+ "\", \n" +
                                "				\"render\": {\n" +
                                "					_: 'display',\n" +
                                "					sort: 'sort'\n" +
                                "				} \n" +
                                "			},\n");
                    //if (showSegmentPace) chars.append("           { \"data\": \"segments.segment_" +  seg.getSegmentName() + ".pace\" },\n"); // pace.getFieldWidth()+1
                });
                report += chars.toString();
            }
            report +=   "           { \"data\": { \n" +
                        "				\"_\": \"finish_display\",\n" +
                        "				\"sort\": \"finish_sort\"}\n" +
                        "			},\n";
            
            if (showGun) report +=  "           { \"data\": { \n" +
                                    "				\"_\": \"gun_display\",\n" +
                                    "				\"sort\": \"finish_sort\"}\n" +
                                    "			},\n";
            if (showPace) report += "           { \"data\": \"finish_pace\" }\n";
            report +=   "        ],\n" +
                        "   \"oSearch\": { \"sSearch\": search },\n" + 
                        "    responsive: {\n" +
                        "            details: false \n" +
                        "        },\n" +
                        "    columnDefs: [ " +
                        "            { type: 'natural', targets: '_all' },\n" +
                        "            {className: 'control', orderable: false, targets:   0}\n" +
                        "        ],"  +
                        "    scrollY: '60vh',\n" +
                        "    scroller:    true,\n" +
                        "    deferRender: true,\n" +
                        "    \"dom\": '<\"toolbar\">frtip',\n" ;
            if (!inProgress) report +=             "     buttons: [\n" +
                        "        {\n" +
                        "            extend: 'csv',\n" +
                        "            text: 'Export to CSV',\n" +
                        "                    bom: 'true',\n" +
                        "            exportOptions: {\n" +
                        "                modifier: {\n" +
                        "                    search: 'none'\n" +
                        "                }\n" +
                        "            }\n" +
                        "        }, \n" +
                        "		'print'\n" +
                        "    ],\n";
            report +=   "   \"fnInitComplete\": function () {\n" +
                        //"	this.fnAdjustColumnSizing();\n" +
                        "	$('div.dataTables_filter input').focus();\n" +
                        "		$(\"div.toolbar\").html('<Label class=\"up-10 hide-mobile bold\">Filter: </label><label class=\"up-10\">Sex <span id=\"filter-index-6\" ></span></label><label class=\"up-10\">  Age-Group <span id=\"filter-index-7\" ></span></label>');\n" +
                        "\n" +
                        "		this.api().columns([6,7]).every( function () {\n" +
                        "			var column = this;\n" +
                        "			var select = $('<select><option value=\"\"></option></select>')\n" +
                        "				.appendTo( document.getElementById('filter-index-' + this.index()) )\n" +
                        "				.on( 'change', function () {\n" +
                        "					var val = $.fn.dataTable.util.escapeRegex(\n" +
                        "						$(this).val()\n" +
                        "					);\n" +
                        "\n" +
                        "					column\n" +
                        "						.search( val ? '^'+val+'$' : '', true, false )\n" +
                        "						.draw();\n" +
                        "				} );\n" +
                        "\n" +
                        "			column.data().unique().sort().each( function ( d, j ) {\n" +
                        "				select.append( '<option value=\"'+d+'\">'+d+'</option>' )\n" +
                        "			} );\n" +
                        "		} );" +
                        "   }\n" +
                        "});\n" +
                    "   // Add event listener for opening and closing details\n" +
                    "    $('#results tbody').on('click', 'tr', function () {\n" +
                    "        var tr = $(this).closest('tr');\n" +
                    "        var row = oTable.row( tr );\n" +
                    " \n" +
                    "        if ( row.child.isShown() ) {\n" +
                    "            // This row is already open - close it\n" +
                    "            row.child.hide();\n" +
                    "            tr.removeClass('parent');\n" +
                    "        }\n" +
                    "        else {\n" +
                    "            // Open this row\n" +
                    "            row.child( childData(row.data()), 'child' ).show();\n" +
                    "            tr.addClass('parent');\n" +
                    "        }\n" +
                    "    } );\n" +
                    "   setTimeout( function () {\n" +
                    "		if (search !== \"\") oTable.rows( {search:'applied'} ).every(function(index){\n" +
                    "			var row = oTable.row(index);\n" +
                    "			row.node().click();\n" +
                    "		});\n" +
                    "		\n" +
                    "	}, 300 );\n" +
                    "   document.getElementById('loading').style.display = 'none';\n";
        if (!inProgress) report += "   oTable.buttons().container().appendTo( document.getElementById('btn') );\n" ;
                    //"   document.getElementById('results_table').style.display = 'initial';\n" +
                    //"   $( $.fn.dataTable.tables(true) ).DataTable().responsive.rebuild();\n" +
                    //"   $( $.fn.dataTable.tables(true) ).DataTable().responsive.recalc();\n" +
        report += "} );\n" +
                    "\n" +
                    "\n" +
                    "	</script>\n";
        report += "<!-- End DataTables -->\n";

        }
        report += "  </BODY> " +  System.lineSeparator();
        report += "</HTML> " +  System.lineSeparator();
        
        return report;
    }
    
    
}
