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
public class OverallHTML implements RaceReportType{
    Race race;

    Boolean showDQ = true;
    Boolean inProgress = false;
    Boolean showSplits = false;
    Boolean showSegments = false;
    Boolean showSegmentPace = false;
    Boolean showDNF = true;
    Boolean showPace = true;
    Boolean showGun = true;
    
    Boolean showCustomAttributes = false;
    List<CustomAttribute> customAttributesList = new ArrayList();

    Map<String,Boolean> supportedOptions = new HashMap();
    
    public OverallHTML(){
        supportedOptions.put("showDQ", true);
        supportedOptions.put("inProgress", false);
        supportedOptions.put("showSplits", false);
        supportedOptions.put("showSegments", true);
        supportedOptions.put("showSegmentPace", false);
        supportedOptions.put("showCustomAttributes", false);

        supportedOptions.put("showDNF", false);
        supportedOptions.put("showPace", true);
        supportedOptions.put("showGun", true);
        supportedOptions.put("hideCustomHeaders", false);
    }
    
    private String escapeHTML(String s){
        return StringEscapeUtils.escapeHtml4(s);
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
        showCustomAttributes = supportedOptions.get("showCustomAttributes");
        
        Boolean showCO = false;
        Boolean showST = false;
        for (ProcessedResult x : prList){
            if (! x.getParticipant().getCountry().isEmpty()) showCO=true;
            if (! x.getParticipant().getState().isEmpty()) showST=true;
        }
        // Stupid lambda workarounds....
        final Boolean showCountry = showCO;
        final Boolean showState = showST;
        
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
        
        final Boolean  penaltiesOrBonuses = prList.stream().anyMatch(s -> (s.getBonus() || s.getPenalty()));

                
                
        Integer dispFormatLength;  // add a space
        if (dispFormat.contains("[HH:]")) dispFormatLength = dispFormat.length()-1; // get rid of the two brackets and add a space
        else dispFormatLength = dispFormat.length()+1;
        
        Duration cutoffTime = Duration.ofNanos(race.getRaceCutoff());
        String cutoffTimeString = DurationFormatter.durationToString(cutoffTime, dispFormat, roundMode);
        
       
        report += "<HTML> " +  System.lineSeparator();
        report += "  <HEAD> " +  System.lineSeparator();
        
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
        if (inProgress) report +=   "<link rel=\"stylesheet\" type=\"text/css\" href=\"https://cdn.datatables.net/v/dt/jq-3.3.1/dt-1.10.20/fh-3.1.6/r-2.2.3/sc-2.0.1/datatables.min.css\"/>\n" ;
        else report +=   "<link rel=\"stylesheet\" type=\"text/css\" href=\"https://cdn.datatables.net/v/dt/jq-3.3.1/jszip-2.5.0/dt-1.10.20/b-1.6.1/b-flash-1.6.1/b-html5-1.6.1/b-print-1.6.1/r-2.2.3/sc-2.0.1/datatables.min.css\"/>" ;
        report +=    " \n";
        if (inProgress) report += "<script type=\"text/javascript\" src=\"https://cdn.datatables.net/v/dt/jq-3.3.1/dt-1.10.20/fh-3.1.6/r-2.2.3/sc-2.0.1/datatables.min.js\"></script>\n" ;
        else report +=   "<script type=\"text/javascript\" src=\"https://cdnjs.cloudflare.com/ajax/libs/pdfmake/0.1.36/pdfmake.min.js\"></script>\n" +
                        "<script type=\"text/javascript\" src=\"https://cdnjs.cloudflare.com/ajax/libs/pdfmake/0.1.36/vfs_fonts.js\"></script>\n" +
                        "<script type=\"text/javascript\" src=\"https://cdn.datatables.net/v/dt/jq-3.3.1/jszip-2.5.0/dt-1.10.20/b-1.6.1/b-flash-1.6.1/b-html5-1.6.1/b-print-1.6.1/r-2.2.3/sc-2.0.1/datatables.min.js\"></script>\n";
            
        

        report +=   " \n" +
                    "<script type=\"text/javascript\" src=\"https://cdn.datatables.net/plug-ins/1.10.20/sorting/natural.js\"></script>\n";
        
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
        report += "<!-- End Stylesheets / JS Indludes-->";

        // DataTables
        report += "<script type=\"text/javascript\" class=\"init\">\n" +
                    "	\n" +
                    "\n" +
                    "$(document).ready(function() {\n" +
                    "var search = \"\";\n" +
                    "	if ( window.location.hash !== \"\" ) {\n" +
                    "		search = window.location.hash.substring( 1 );\n" +
                    "	}" +
                    "var oTable = $('#results').DataTable({\n" +
                        "   \"oSearch\": { \"sSearch\": search }," + 
                        "    responsive: {\n" +
                        "            details: {\n" +
                        "                renderer: $.fn.dataTable.Responsive.renderer.tableAll()," +
                        "                type: 'column',\n" +
                        "                target: 'tr'\n" +
                        "            }\n" +
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
                        "	this.fnAdjustColumnSizing();\n" +
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
                    "   setTimeout( function () {\n" +
                    "		if (search !== \"\") oTable.rows( {search:'applied'} ).every(function(index){\n" +
                    "			var row = oTable.row(index);\n" +
                    "			row.node().click();\n" +
                    "		});\n" +
                    "		\n" +
                    "	}, 300 );";
                if (!inProgress) report += "   oTable.buttons().container().appendTo( document.getElementById('btn') );\n" ;

        report += "} );\n" +
                    "\n" +
                    "\n" +
                    "	</script>";
        report += "<!-- End DataTables -->";
        
        report += "  </HEAD> " +  System.lineSeparator();
        report += "  <BODY> " +  System.lineSeparator();
        
        if (customHeaders){
            if (textOnlyHeaders) report += "<pre>" + race.getStringAttribute("textHeader") + "</pre>";
            else report += race.getStringAttribute("htmlHeader");
            report += System.lineSeparator();
        }
        
        report += "<div class=\"event-info\">" + escapeHTML(event.getEventName()) + System.lineSeparator();;
        if (RaceDAO.getInstance().listRaces().size() > 1) 
            report += "<br>" + escapeHTML(race.getRaceName());
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
            if (textOnlyHeaders) report += "<pre>" + race.getStringAttribute("textMessage") + "</pre>";
            else report += race.getStringAttribute("htmlMessage");
            report += System.lineSeparator();
        }
        if(!dataToShow) {
            report += "    <div class=\"in-progress\">" + "<BR>*No Results Have Been Posted Yet*" + "</div>" + System.lineSeparator();
            report += System.lineSeparator();
            if (customHeaders){
                if (textOnlyHeaders) report += "<pre>" + race.getStringAttribute("textFooter") + "</pre>";
                else report += race.getStringAttribute("htmlFooter");
                report += System.lineSeparator();
            }
        } else {
            // Start the table
            report += "  <TABLE id=\"results\" class=\"display responsive nowrap\" > " +  System.lineSeparator();
            // print the headder
            report += "    <thead><tr>" +  System.lineSeparator();
            report += "      <th></th>"+  System.lineSeparator(); // dummy for control box
            report += "      <th data-priority=\"10\">OA#</th>" +  System.lineSeparator();
            report += "      <th data-priority=\"20\">SEX#</th>" +  System.lineSeparator(); // 5R chars
            report += "      <th data-priority=\"30\">AG#</th>" +  System.lineSeparator(); // 5R chars
            report += "      <th data-priority=\"5\">BIB</th>" +  System.lineSeparator(); // 5R chars for the bib #
            report += "      <th data-priority=\"9\">AGE</th>" +  System.lineSeparator(); // 4R for the age
            report += "      <th data-priority=\"5\">SEX</th>" +  System.lineSeparator(); // 4R for the sex
            report += "      <th data-priority=\"29\">AG</th>" +  System.lineSeparator(); //6L for the AG Group
            report += "      <th data-priority=\"1\">Name</th>" +  System.lineSeparator(); // based on the longest name
            report += "      <th data-priority=\"40\">City</th>" +  System.lineSeparator(); // 18L for the city
            if (showState) report += "      <th data-priority=\"40\">ST</th>" +  System.lineSeparator(); // 4C for the state code
            if (showCountry) report += "      <th data-priority=\"40\">CO</th>" +  System.lineSeparator(); // 4C for the country

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
                Integer dispLeg = dispFormatLength;
                race.raceSegmentsProperty().forEach(seg -> {
                    if(seg.getHidden()) return;
                    chars.append("      <th data-priority=\"80\">" + escapeHTML(seg.getSegmentName())+ "</th>" +  System.lineSeparator());
                    if (showSegmentPace) {
                        if (! (seg.getUseCustomPace() && Pace.NONE.equals(seg.getCustomPace())))
                            chars.append("      <th data-priority=\"95\"> Pace</th>" +  System.lineSeparator());
                    } // pace.getFieldWidth()+1
                });
                report += chars.toString();
            }
            if (penaltiesOrBonuses) report += "      <th data-priority=\"1\">Adj</th>";
            // Chip time
            report += "      <th data-priority=\"1\">Finish</th>" +  System.lineSeparator(); // 9R Need to adjust for the format code

            // gun time
            if (showGun) report += "      <th data-priority=\"90\">Gun</th>" +  System.lineSeparator(); // 9R ibid
            // pace
            if (showPace) report += "      <th data-priority=\"85\">Pace</th>" +  System.lineSeparator(); // 10R

            report += "</tr></thead>" +  System.lineSeparator(); 
            report += "<tbody>"+ System.lineSeparator();



            final StringBuilder chars = new StringBuilder();

            prList.forEach(pr -> {

                // if they are a DNF or DQ swap out the placement stats
                Boolean hideTime = false; 
                Boolean hideSplitTimes = false;

                Boolean dnf = pr.getParticipant().getDNF();
                Boolean dq = pr.getParticipant().getDQ();

                if (pr.getChipFinish() == null && showDNF && !inProgress) dnf = true;

                if (!showDNF && dnf) return;
                if (!showDQ && dq) return;

                if (dnf || dq) hideTime = true;
                if (dq) hideSplitTimes = true; 

                if (!inProgress && pr.getChipFinish() == null && !showDNF) return;

                Boolean oco = false;
                if (dnf || dq) oco = false;
                else if (inProgress && pr.getChipFinish() == null) ; // They havent finished yet
                else if (cutoffTime.isZero() 
                            || cutoffTime.compareTo(pr.getChipFinish()) >= 0 
                            || cutoffTimeString.equals(DurationFormatter.durationToString(pr.getChipFinish(), dispFormat, roundMode))) {
                    oco = false;
                } else {
                    oco=true;
                    if (!showDNF && !inProgress) return;

                }

                chars.append("<td></td>" +  System.lineSeparator()); // dummy for control

                if (dq) chars.append("<td >*****DQ****</td>"
                            + "<td>--</td>\n" +
                            " <td>--</td>" +  System.lineSeparator());
                else if (dnf) chars.append("<td>***DNF***</td>"
                            + "<td>--</td>\n" +
                            " <td>--</td>" +  System.lineSeparator());
                else if (oco) chars.append("<td>***OCO***</td>"
                            + "<td>--</td>\n" +
                            " <td>--</td>" +  System.lineSeparator());
                else if (pr.getChipFinish() != null){ 
                    chars.append("<td>"+ pr.getOverall().toString() + " </td>" +  System.lineSeparator());
                    chars.append("<td>"+ pr.getSexPlace().toString() + " </td>" +  System.lineSeparator());
                    chars.append("<td>"+ pr.getAGPlace().toString() + " </td>" +  System.lineSeparator());
                } else {
                    chars.append("<td> **Started** </td>\n"
                            + "<td>--</td>\n" +
                            " <td>--</td>" +  System.lineSeparator());
                    hideTime = true;
                }




                chars.append("<td>"+ pr.getParticipant().getBib() + "</td>" +  System.lineSeparator());
                chars.append("<td>"+ pr.getAge().toString() + "</td>" +  System.lineSeparator());
                chars.append("<td>"+ pr.getSex() + "</td>" +  System.lineSeparator());
                chars.append("<td>"+ pr.getAGCode() + "</td>" +  System.lineSeparator());
                chars.append("<td>"+ escapeHTML(pr.getParticipant().fullNameProperty().getValueSafe())+ "</td>" +  System.lineSeparator());
                chars.append("<td>"+ escapeHTML(pr.getParticipant().getCity()) + "</td>" +  System.lineSeparator());
                if (showState) chars.append("<td>"+ escapeHTML(pr.getParticipant().getState()) + "</td>" +  System.lineSeparator());
                if (showCountry) chars.append("<td>"+ escapeHTML(pr.getParticipant().getCountry()) + "</td>" +  System.lineSeparator());
                
                if (showCustomAttributes) {
                    for( CustomAttribute a: customAttributesList){
                        chars.append("<td>").append(escapeHTML(pr.getParticipant().getCustomAttribute(a.getID()).getValueSafe())).append("</td>" +  System.lineSeparator());
                    }
                }

                // Insert split stuff here 
                if (showSplits) {
                // do stuff
                    for (int i = 2; i < race.splitsProperty().size(); i++) {
                        if (!race.splitsProperty().get(i-1).getIgnoreTime()){
                            if (!hideSplitTimes) 
                                chars.append("<td>"+ DurationFormatter.durationToString(pr.getSplit(i), dispFormat, roundMode)+ "</td>" +  System.lineSeparator());
                            else chars.append("<td>---</td>" +  System.lineSeparator());
                        }
                    }
                }

                if (showSegments) {
                    Boolean hst = hideSplitTimes;
                    race.raceSegmentsProperty().forEach(seg -> {
                        if (seg.getHidden()) return;
                        if (!hst) 
                            chars.append("<td>"+ DurationFormatter.durationToString(pr.getSegmentTime(seg.getID()), dispFormat, roundMode)+ "</td>" +  System.lineSeparator());
                        else chars.append("<td>---</td>" +  System.lineSeparator());
                        if (showSegmentPace ) {
                            if (! (seg.getUseCustomPace() && Pace.NONE.equals(seg.getCustomPace()))) {
                                if (!hst)
                                    if (pr.getSegmentTime(seg.getID()) != null ) {
                                        if (seg.getUseCustomPace()) chars.append("<td>"+ seg.getCustomPace().getPace(seg.getSegmentDistance(), race.getRaceDistanceUnits(), pr.getSegmentTime(seg.getID()))+ "</td>" +  System.lineSeparator());
                                        else chars.append("<td>"+ pace.getPace(seg.getSegmentDistance(), race.getRaceDistanceUnits(), pr.getSegmentTime(seg.getID()))+ "</td>" +  System.lineSeparator());
                                    } else chars.append("<td>---</td>" +  System.lineSeparator());
                                else chars.append("<td>---</td>" +  System.lineSeparator());
                            }
                        }
                    });
                }

                if (penaltiesOrBonuses){
                    if (pr.getBonus() || pr.getPenalty()) {
                        if (pr.getBonus()) chars.append("<td>-").append(DurationFormatter.durationToString(pr.getBonusTime(), dispFormat, roundMode)).append("</td>" +  System.lineSeparator());
                        else chars.append("<td>+").append(DurationFormatter.durationToString(pr.getPenaltyTime(), dispFormat, roundMode)).append("</td>" +  System.lineSeparator());
                    } else chars.append("<td>---</td>" +  System.lineSeparator());
                }
                // chip time
                if (dq) chars.append("<td>"+ pr.getParticipant().getNote() + "</td>" +  System.lineSeparator());
                else if (! hideTime) chars.append("<td>"+DurationFormatter.durationToString(pr.getChipFinish(), dispFormat, roundMode)+ "</td>" +  System.lineSeparator());
                else chars.append("<td>---</td>" +  System.lineSeparator());

                if (showGun && ! hideTime) chars.append("<td>"+DurationFormatter.durationToString(pr.getGunFinish(), dispFormat, roundMode)+ "</td>" +  System.lineSeparator());
                else if (showGun && hideTime) chars.append("<td>---</td>" +  System.lineSeparator());

                if (showPace && ! hideTime) chars.append("<td>"+pace.getPace(race.getRaceDistance().floatValue(), race.getRaceDistanceUnits(), pr.getChipFinish())+ "</td>" +  System.lineSeparator());
                else if (showPace && hideTime) chars.append("<td>---</td>" +  System.lineSeparator());



                chars.append("</tr>" + System.lineSeparator());


            });

            report += chars.toString();
            report += "</tbody>" +  System.lineSeparator();
            report += "</table>" +  System.lineSeparator();
            if (!inProgress) report += "<div id=\"btn\" class=\"buttons\"></div>" +  System.lineSeparator();
            if (customHeaders){
                if (textOnlyHeaders) report += "<pre>" + race.getStringAttribute("textFooter") + "</pre>";
                else report += race.getStringAttribute("htmlFooter");
                report += System.lineSeparator();
            }
        }
        report += "  </BODY> " +  System.lineSeparator();
        report += "</HTML> " +  System.lineSeparator();
        
        return report;
    }
    
    
}
