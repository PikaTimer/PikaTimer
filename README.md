# pikatimer
PikaTimer: An OpenSource race timing application

PikaTimer is a JavaFX based race timing application. Named after the American Pika that is often found at high altitudes where the need for simple, easy to use race timing application was born. Because at 14,000 ft, you want things to be simple.

The entire system is released under the GPLv3 Open Source license. PikaTimer is free to use, free to modify, free to redistribute per the GPLv3 license. 


Current Feature Highlights:
* Multiple Races per Event
* Multiple start waves per race
* Multiple Timing Locations
* Multiple Splits per Race, each associated with a given timing location
* Direct import of timing data from RFID Ultra Based systems
* No practical limit on the number of participants, races, splits, etc (but you'll want a faster box beyond a few thousand runners with a dozen splits).
* Support for automatically importing a timing input file and updating the results as new times are entered
* Automatically process reports and post them to a remote server every 30s/1m/2m/5m 
* Ability to flag a timing input as a "backup." Data from these are only used when there are no other reads for a given split (good for select timer data.)
* Time overrides for a participant on a per split basis
* Participants may be entered into multiple Races 
* Regular expression based search for participants and raw results
* Ability to "skew" the time for any given timing input (adjust the clock +/- a given number of seconds to account for setup errors)
* Support for races that are longer than a single day 
* 5 or 10 year Age group setup
* Alphanumeric bib support
* Built in database engine (H2) that stores the entire event in a single file (easy for backups, copying, archiving, etc)
* Outputs basic Overall, Age Group, and Awards text based reports. 
* Output of HTML results table (formatted with DataTables for easy mobile viewing)
* Automatically upload reports to an FTP, FTPS (FTP over TLS), or SFTP (FTP over SSH) server
* Output awards file based on gun or chip time for Overall / Masters / AG
* DQ / DNF / DNS flags for runners and the ability to exclude them on results.
* In-Progress report options that show runners who started but have not yet finished.
* Windows x64 based native application



Road Map (In no particular order):
* Arbitrary participant attributes an the ability to produce reports based on them
* Arbitrary age groups (vs 5/10 year)
* Arbitrary awards categories based on any participant attribute
* Team Reports / Awards
* Ability to change the pace or distance units on a per split/segment basis (e.g. swim in min/100yds, bike in mph, run in min/mile)
* Mandatory Splits
* Non-binary gender/sex attribute
* Ability to support relay races
* Ability to support lap races
* Race series support (May be another app that looks to multiple PikaTimer races)
* Built in web server for satellite systems to enter / view data (race day registration / bib assignments, announcer stations, results lookup stations, etc)
* Sync registrants from Active / RunSignUp / FuseSport / etc
* Result "embellishments" (Course Records, Awards, Boston Qualifying time notifications, etc).
* Linux / MacOS native installation apps

Built on top of:
* Java / JavaFX -- http://java.com
* H2 -- http://www.h2database.com/
* Hibernate -- http://hibernate.org/
* ControlsFX -- http://fxexperience.com/controlsfx/
* And a few other Open Source / Public Domain applications

Building: TODO
