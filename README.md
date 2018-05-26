# PikaTimer: An OpenSource Race Timing Application

PikaTimer is a JavaFX based race timing application named after the American Pika that is often found at high altitudes; where the need for a simple, easy to use race timing application was born. Because at 14,000 ft, you want things to be simple.

The entire system is released under the GPLv3 Open Source license. PikaTimer is free to use, free to modify, free to redistribute per the GPLv3 license. 


## Current Feature Highlights:
* Multiple races per event
* Multiple start waves per race
* Multiple timing locations
* Multiple splits per race, each associated with a given timing location
* Direct import of timing data from RFID Ultra and Joey based systems
* Custom Participant Attributes
* Custom Award Definitions
* No practical limit on the number of participants, races, splits, etc. 
* Support for automatically importing a timing input file and updating the results as new times are entered
* Automatically process reports and post them to a remote server every 30s/1m/2m/5m 
* Ability to flag a timing input as a "backup" 
* Time overrides for a participant on a per split basis
* Participants may be entered into multiple races
* Regular expression based search for participants and raw results
* Ability to "skew" the time for any given timing input 
* Support for races that are longer than a single day
* 1, 5, 10, or Custom year age group setup
* Alphanumeric bib support
* Built in database engine (H2) that stores the entire event in a single file for for backups, copying, archiving, etc.
* Outputs basic Overall, Age Group, and Awards text based reports
* Output of HTML results table (formatted with DataTables for easy mobile viewing)
* Automatically upload reports to an FTP, FTPS (FTP over TLS), or SFTP (FTP over SSH) server
* Output awards file based on gun or chip time for Overall / Masters / AG
* DQ / DNF / DNS flags for runners and the ability to exclude them on results
* Ability to add a time bonus or penalty to any participant
* On-course cutoff enforcement
* In-Progress report options that show runners who started but have not yet finished
* Windows x64 based native application or jar files for other supported platforms

## Screen Snapshots

*Race Setup:*
![Race Setup](https://user-images.githubusercontent.com/19352375/40580418-d065f7be-60fa-11e8-8a6e-5522de9bcd1d.png)

*Award Setup:*
![Award Setup](https://user-images.githubusercontent.com/19352375/40580421-e86bfb9c-60fa-11e8-9a6c-908f338233f1.png)

*Participants:*
![Participant Setup](https://user-images.githubusercontent.com/19352375/40580423-f4aab934-60fa-11e8-8fe4-350a568bee95.png)

*Timing Setup:*
![Timing Setup](https://user-images.githubusercontent.com/19352375/40580427-16d72d30-60fb-11e8-9bec-e86c261f75f9.png)

*Results and Reports:*
![Results Setup](https://user-images.githubusercontent.com/19352375/40580432-279cb644-60fb-11e8-85f5-5215bbc92acb.png)


## Built on top of:
* Java / JavaFX -- http://java.com
* H2 -- http://www.h2database.com/
* Hibernate -- http://hibernate.org/
* ControlsFX -- http://fxexperience.com/controlsfx/
* And other Open Source / Public Domain applications
