/*
 * Copyright (C) 2017 John Garner <segfaultcoredump@gmail.com>
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    Boolean showDNF = true;
    Boolean showPace = true;
    Boolean showGun = true;

    Map<String,Boolean> supportedOptions = new HashMap();
    
    public OverallHTML5(){
        supportedOptions.put("showDQ", true);
        supportedOptions.put("inProgress", false);
        supportedOptions.put("showSplits", false);
        supportedOptions.put("showSegments", true);
        supportedOptions.put("showSegmentPace", false);
        supportedOptions.put("showDNF", false);
        supportedOptions.put("showPace", true);
        supportedOptions.put("showGun", true);
        supportedOptions.put("hideCustomHeaders", false);
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
        showDNF = supportedOptions.get("showDNF");
        showPace = supportedOptions.get("showPace");
        showGun = supportedOptions.get("showGun");
        
        Boolean showCountry = false;
        for (ProcessedResult x : prList){
            if (! x.getParticipant().countryProperty().isEmpty().get()) showCountry=true;
        }
        
        
        Boolean customHeaders = race.getBooleanAttribute("useCustomHeaders");
        Boolean textOnlyHeaders = race.getBooleanAttribute("textOnlyHeaders");
        if (customHeaders == null || (customHeaders == true && supportedOptions.get("hideCustomHeaders"))) customHeaders = false;
        if (customHeaders && textOnlyHeaders == null) textOnlyHeaders = false;
        
        String dispFormat = race.getStringAttribute("TimeDisplayFormat");
        String roundMode = race.getStringAttribute("TimeRoundingMode");
        Pace pace = Pace.valueOf(race.getStringAttribute("PaceDisplayFormat"));
        
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
        if (inProgress) report +=   "<link rel=\"stylesheet\" type=\"text/css\" href=\"https://cdn.datatables.net/v/dt/jq-2.2.4/dt-1.10.15/fh-3.1.2/r-2.1.1/sc-1.4.2/datatables.min.css\"/>\n" ;
        else report +=   "<link rel=\"stylesheet\" type=\"text/css\" href=\"https://cdn.datatables.net/v/dt/jq-2.2.4/jszip-3.1.3/pdfmake-0.1.27/dt-1.10.15/b-1.3.1/b-flash-1.3.1/b-html5-1.3.1/b-print-1.3.1/r-2.1.1/sc-1.4.2/datatables.min.css\"/>" ;
        report +=   "<link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/font-awesome/4.3.0/css/font-awesome.min.css\">\n" +
                    " \n" ;
        
        // our inline CSS
        report +=   "<link href=\"https://fonts.googleapis.com/css?family=Source+Sans+Pro|Open+Sans\" rel=\"stylesheet\">\n" +
                    "<style>\n" +
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
            if (textOnlyHeaders) report += race.getStringAttribute("textHeader");
            else report += race.getStringAttribute("htmlHeader");
            report += System.lineSeparator();
        }
        
        report += "<div class=\"event-info\">" + event.getEventName() + System.lineSeparator();;
        if (RaceDAO.getInstance().listRaces().size() > 1) 
            report += "<br>" + race.getRaceName();
        report += "</div>" + System.lineSeparator();
        
        report += "<div class=\"event-date\">" + event.getLocalEventDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)) + "</div>" + System.lineSeparator();
        report += System.lineSeparator();
        
        if(inProgress) {
            report += "    <div class=\"in-progress\">" + "*In Progress*" + "</div>" + System.lineSeparator();
            report += System.lineSeparator();
        }
        
        if (customHeaders){
            if (textOnlyHeaders) report += race.getStringAttribute("textMessage");
            else report += race.getStringAttribute("htmlMessage");
            report += System.lineSeparator();
        }
        
        if(prList.isEmpty()) {
            report += "    <div class=\"in-progress\">" + "<BR>*No Results Have Been Posted Yet*" + "</div>" + System.lineSeparator();
            report += System.lineSeparator();
            if (customHeaders){
                if (textOnlyHeaders) report += race.getStringAttribute("textFooter");
                else report += race.getStringAttribute("htmlFooter");
                report += System.lineSeparator();
            }
        } else {
        // Start the table
            report += "    <div id=\"loading\" class=\"in-progress right\">" + "<BR>Loading..." + "</div>" + System.lineSeparator();

            //report += "<div id=\"results_table\" class=\"hide\">" +  System.lineSeparator();
            report += "  <TABLE id=\"results\" class=\"display responsive dtr-column nowrap\" > " +  System.lineSeparator();
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
            report += "      <th data-priority=\"41\">City</th>" +  System.lineSeparator(); 
            report += "      <th data-priority=\"40\">ST</th>" +  System.lineSeparator(); 
            if (showCountry) report += "      <th data-priority=\"45\">Country</th>" +  System.lineSeparator(); 

            // Insert split stuff here
            if (showSplits) {
                for (int i = 2; i < race.splitsProperty().size(); i++) {
                    report += "      <th data-priority=\"100\">" + race.splitsProperty().get(i-1).getSplitName() + "</th>" +  System.lineSeparator();
                }
            }
            if (showSegments) {
                final StringBuilder chars = new StringBuilder();
                Integer dispLeg = dispFormatLength;
                race.getSegments().forEach(seg -> {
                    chars.append("      <th data-priority=\"80\">" + seg.getSegmentName()+ "</th>" +  System.lineSeparator());
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
            if (textOnlyHeaders) report += race.getStringAttribute("textFooter");
            else report += race.getStringAttribute("htmlFooter");
            report += System.lineSeparator();
        }

        report += "<!-- Start DataTables -->\n";
        
        if (inProgress) report += "<script type=\"text/javascript\" src=\"https://cdn.datatables.net/v/dt/jq-2.2.4/dt-1.10.15/fh-3.1.2/r-2.1.1/sc-1.4.2/datatables.min.js\"></script>\n" ;
        else  report +=  "<script type=\"text/javascript\" src=\"https://cdn.datatables.net/v/dt/jq-2.2.4/jszip-3.1.3/pdfmake-0.1.27/dt-1.10.15/b-1.3.1/b-flash-1.3.1/b-html5-1.3.1/b-print-1.3.1/r-2.1.1/sc-1.4.2/datatables.min.js\"></script>\n" ;

        report +=           " \n" +
                    "<script type=\"text/javascript\" src=\"https://cdn.datatables.net/plug-ins/1.10.12/sorting/natural.js\"></script>\n";

        report += "<script type=\"text/javascript\" class=\"init\">\n" +
                    " // nth(n) function from http://stackoverflow.com/questions/13627308/add-st-nd-rd-and-th-ordinal-suffix-to-a-number \n" + 
                    "function nth(s){var n = parseInt(s); if (isNaN(n)) return s; return s + ([\"st\",\"nd\",\"rd\"][((n+90)%100-10)%10-1]||\"th\");}\n" +
                
                "function childData ( rData) {\n" +
                        "                \n" +
                        "				var data = '<div class=\"detail\">';\n" +
                        "				data += '<div class=\"row\">';\n" +
                        "				data += '<div class=\"participant\">' // personal\n" +
                        "				data += '<div class=\"part-name\">' + rData.full_name + '</div>';\n" +
                        "				data += '<div class=\"part-stats\">Bib: ' + rData.bib + '</div>';\n" +
                        "				data += '<div class=\"part-stats\">Age: ' + rData.age + '   Sex: ' + rData.sex + '   AG: ' + rData.ag + '</div>';\n" +
                        "				data += '<div class=\"part-stats\">' + rData.city + ', ' + rData.state + '</div>';\n" ;
            if (showCountry) report +=            "				data += '<div class=\"part-stats\">' + rData.country + '</div>';\n";
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
                        "                               } else if ( rData.oa_place == \"Started\" ) {\n" +
                        "					data += '<div class=\"finish-time\">Started</div>';\n" +
                        "                                   data += '<div class=\"finish-stats bold\">Last Seen: ' + rData.last_seen + '</div>';\n" +
                        "                               } else if ( rData.oa_place == \"OCO\" ) {\n" +
                        "					data += '<div class=\"finish-time\">Over Cut Off  :-/</div>';\n" +
                        "                                   data += '<div class=\"finish-stats\"> Finish time: ' + rData.finish_display + '</div>';\n" +
                        "                                   data += '<div class=\"finish-stats\"> Cutoff: " + race.raceCutoffProperty().getValueSafe() + "</div>';\n" +
                        "				} else {\n" +
                        "                                   data += '<div class=\"finish-time\"><span class=\"hide-mobile\">Finish Time: </span>' + rData.finish_display + '</div>';\n";
            if (showGun) report += "                                   data += '<div class=\"finish-stats\">Gun Time: ' + rData.gun_display + '</div>';\n"; 
            report +=   "                                   data += '<div class=\"finish-stats\">Overall: ' + nth(rData.oa_place) + '   Sex: ' + nth(rData.sex_place) + '   <span class=\"hide-mobile\">Age Group:</span><span class=\"show-mobile\">AG:</span> ' + nth(rData.ag_place) + '</div>';\n" ;
            if (showPace) report += "                                   data += '<div class=\"finish-stats\">Pace: ' + rData.finish_pace + '</div>';\n" ;
            report +=   "				}" +
                        "				data += '</div>'; // time\n" +
                        "				\n" +
                        "                data += '</div>'; // row\n" +
                        "				\n";
//        if (showSplits) {
//            for (int i = 2; i < race.splitsProperty().size(); i++) {
//                report += "      <th data-priority=\"100\">" + race.splitsProperty().get(i-1).getSplitName() + "</th>" +  System.lineSeparator();
//            }
//        }
            if (showSegments) {
                final StringBuilder chars = new StringBuilder();
                chars.append("data += '<div class=\"row\">';\n");
                chars.append("data += '<div class=\"segment segment-title\">Segments: '; // time\n");
                chars.append("data += '</div>';\n");
                chars.append("data += '</div>';\n");

                chars.append("data += '<div class=\"row\">';\n");
                race.getSegments().forEach(seg -> {
                    chars.append("data += '<div class=\"segment\">'; // time\n");
                    chars.append("data += '<div class=\"segment-head\">" + seg.getSegmentName()+ "</div>';\n" );
                    chars.append("data += '<div class=\"segment-time\">Time: ' + rData.segments[\"segment_"+seg.getSegmentName()+ "\"].display + '</div>';\n");
                    chars.append("data += '<div class=\"segment-stats\">Overall: ' + nth(rData.segments[\"segment_"+seg.getSegmentName()+ "\"].oa_place) + '   Sex: ' + nth(rData.segments[\"segment_"+seg.getSegmentName()+ "\"].sex_place) + '   AG: ' + nth(rData.segments[\"segment_"+seg.getSegmentName()+ "\"].ag_place) + '</div>';\n");
                    if (showSegmentPace) chars.append("data += '<div class=\"segment-stats\">Pace:  ' + rData.segments[\"segment_"+seg.getSegmentName()+ "\"].pace + '</div>';\n");
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
                for (int i = 2; i < race.splitsProperty().size(); i++) {
                    report += "data += '<tr><td>" + race.splitsProperty().get(i-1).getSplitName() + ":</td><td class=\"right\">  ' + rData.splits[\"split_" + Integer.toString(i-1) + "_"+ race.splitsProperty().get(i-1).getSplitName() + "\"].display + '</td>';\n";
                    report += "data += '<td class=\"right up-half\">  ' + rData.splits[\"split_"+ Integer.toString(i-1) + "_"+ race.splitsProperty().get(i-1).getSplitName() + "\"].delta_time + '</td>';\n";
                    if (showPace) report += "data += '<td class=\"right up-half\">  ' + rData.splits[\"split_"+ Integer.toString(i-1) + "_"+ race.splitsProperty().get(i-1).getSplitName() + "\"].pace + '</td>';\n";
                    report += "data += '</tr>';\n";
                }
                report += "data += '<tr><td>Finish:</td><td class=\"right\">  ' + rData.finish_display + '</td><td class=\"right up-half\">' + rData.finish_split_delta + '</td>';\n";
                if (showPace) report += "data += '<td class=\"right up-half\">' + rData.finish_split_pace + '</td>';\n";
                report += "data += '</tr>';\n";
                if (showGun) report += "data += '<tr><td>Gun Time:</td><td class=\"right\"> ' + rData.gun_display + '</td><td></td>';\n";
                if (showGun && showPace) report += "data += '<td></td>';\n";
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
                    "	}" +
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
                        "           { \"data\": \"full_name\" },\n" +
                        "           { \"data\": \"city\" },\n" +
                        "           { \"data\": \"state\" },\n";
            if (showCountry) report += "           { \"data\": \"country\" },\n";
            if (showSplits) {
                for (int i = 2; i < race.splitsProperty().size(); i++) {
                    report += "           { \"data\": \"splits.split_" + Integer.toString(i-1) + "_"+ race.splitsProperty().get(i-1).getSplitName() + "\", \n" +
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
                race.getSegments().forEach(seg -> {
                    chars.append("           { \"data\": \"segments.segment_" +  seg.getSegmentName() + "\", \n" +
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
