# PikaTimer: An OpenSource Race Timing Application

PikaTimer is a JavaFX based race timing application named after the American Pika that is often found at high altitudes where the need for simple, easy to use race timing application was born. Because at 14,000 ft, you want things to be simple.

The entire system is released under the GPLv3 Open Source license. PikaTimer is free to use, free to modify, free to redistribute per the GPLv3 license. 


## Current Feature Highlights:
* Multiple races per event
* Multiple start waves per race
* Multiple timing locations
* Multiple splits per race, each associated with a given timing location
* Direct import of timing data from RFID Ultra and Joey based systems
* No practical limit on the number of participants, races, splits, etc. 
* Support for automatically importing a timing input file and updating the results as new times are entered
* Automatically process reports and post them to a remote server every 30s/1m/2m/5m 
* Ability to flag a timing input as a "backup" 
* Time overrides for a participant on a per split basis
* Participants may be entered into multiple races
* Regular expression based search for participants and raw results
* Ability to "skew" the time for any given timing input 
* Support for races that are longer than a single day
* 5 or 10 year age group setup
* Alphanumeric bib support
* Built in database engine (H2) that stores the entire event in a single file for for backups, copying, archiving, etc.
* Outputs basic Overall, Age Group, and Awards text based reports
* Output of HTML results table (formatted with DataTables for easy mobile viewing)
* Automatically upload reports to an FTP, FTPS (FTP over TLS), or SFTP (FTP over SSH) server
* Output awards file based on gun or chip time for Overall / Masters / AG
* DQ / DNF / DNS flags for runners and the ability to exclude them on results
* In-Progress report options that show runners who started but have not yet finished
* Windows x64 based native application

## Screen Snapshots

*Race Setup:*
![Race Setup](https://user-images.githubusercontent.com/19352375/27207276-d47b38d8-51fa-11e7-999f-90d840084dcd.png)

*Participants:*
![Participant Setup](https://user-images.githubusercontent.com/19352375/27207420-f62490be-51fb-11e7-87ca-3b7e6a3bf483.png)

*Timing Setup:*
![Timing Setup](https://user-images.githubusercontent.com/19352375/27207246-9addf0ca-51fa-11e7-9fea-cd8a9d0c5468.png)

*Results and Reports:*
![Results Setup](https://user-images.githubusercontent.com/19352375/27207258-b8c87416-51fa-11e7-9858-388326c9eed4.png)


## Built on top of:
* Java / JavaFX -- http://java.com
* H2 -- http://www.h2database.com/
* Hibernate -- http://hibernate.org/
* ControlsFX -- http://fxexperience.com/controlsfx/
* And a few other Open Source / Public Domain applications
