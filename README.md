# pikatimer
PikaTimer: An OpenSource race timing application

PikaTimer is a JavaFX based race timing application. Named after the American Pika that is often found at high altitudes where the need for simple, easy to use race timing application was born. Because at 14,000 ft, you want things to be simple.

The entire system is released under the GPLv3 Open Source license. It is free to use, free to modify, 

Initial support will be for RFID Timing System based readers (http://rfidtiming.com/), but there is nothing preventing the system from being able to read from from other timing systems in the future.

Current Features:
* Multiple Races per Event
* Multiple start waves per race
* Multiple Timing Locations
* Multiple readers per timing location
* Multiple Splits per Race, each associated with a given timing location
* Support for "watching" a timing input file and automatically updating the results as new times are entered
* Time overrides for a participant on a per split basis
* Participants may be entered into multiple Races 
* Regular expression based search for participants and raw results
* Ability to "skew" the time for any given timing input (adjust the clock +/- a given number of seconds to account for setup errors)
* Support for races that are longer than a single day (the back end supports it, the UI is a bit wonky)
* 5 or 10 year Age group setup
* Alphanumeric bib support
* No practical limit on the number of participants, races, splits, etc (but you'll want a faster box beyond a few thousand runners with a dozen splits).
* Built in database engine (H2) that stores the entire event in a single file (easy for backups, copying, archiving, etc)
* Outputs basic Overall, Age Group, and Awards text based reports. 
* Output of HTML results table (formatted with DataTables for easy mobile viewing)
* Post a file to an FTPS (FTP over SSL) server
* Output awards file based on gun or chip time for Overall / Masters / AG.
* DQ / DNF flags for runners and the ability to exclude them on results.
* In-Progress report options that show runners who started but have not yet finished.

Short List of pending features:
* Automatically post the results to a local file or remote web server via ftp/sftp/scp every X seconds
* Windows / Linux / MacOS native installation apps
* Ability to report on a "segment" (delta between two splits)
* Bulk bib assignments

Long Term Features:
* Arbitrary participant attributes an the ability to produce reports based on them
* Arbitrary age groups (vs 5/10 year)
* Arbitrary awards categories based on any participant attribute
* Ability to flag a timing input as a "backup" that is only used when there are no other reads for a given split
* Ability to ignore individual chip reads
* Ability to change the pace or distance units on a per split/segment basis (swim in meters, bike in mph, run in min/mile)
* Non-binary gender/sex attribute
* Team Reports / Awards
* Ability to support relay races
* Race series support (May be another app that looks to multiple PikaTimer races)
* Built in web server for satellite systems to enter / view data (race day registration / bib assignments, announcer stations, results lookup stations, etc)
* Post results to social media (twitter, Facebook, etc)
* Sync registrants from Active / RunSignUp / FuseSport / etc
* Result "embellishments" (Course Records, Awards, Boston Qualifying time notifications, etc).

Built on top of:
* Java / JavaFX -- http://java.com
* H2 -- http://www.h2database.com/
* Hibernate -- http://hibernate.org/
* ControlsFX -- http://fxexperience.com/controlsfx/
* And a few other Open Source / Public Domain applications

Building: TODO
