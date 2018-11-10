# PikaTimer: An OpenSource Race Timing Application

PikaTimer is a JavaFX based race timing application named after the American Pika that is often found at high altitudes; where the need for a simple, easy to use race timing application was born. Because at 14,000 ft, you want things to be simple.

The entire system is released under the GPLv3 Open Source license. PikaTimer is free to use, free to modify, free to redistribute per the GPLv3 license. 

## Downloads
To download, go to https://github.com/PikaTimer/pikatimer/releases/

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
![Race Setup](https://user-images.githubusercontent.com/19352375/48306069-7c188380-e4f2-11e8-8dc2-867437ddea57.png)

*Award Setup:*
![Award Setup](https://user-images.githubusercontent.com/19352375/48306072-86d31880-e4f2-11e8-9b98-95650ddca140.png)

*Participants:*
![Participant Setup](https://user-images.githubusercontent.com/19352375/48306074-905c8080-e4f2-11e8-81ab-83e1a85ba8a5.png)

*Timing Setup:*
![Timing Setup](https://user-images.githubusercontent.com/19352375/48306076-96526180-e4f2-11e8-9cb2-8aab396f34bc.png)

*Results and Reports:*
![Results Setup](https://user-images.githubusercontent.com/19352375/48306080-9e120600-e4f2-11e8-9caa-162b13c95920.png)


## Built on top of:
* Java / JavaFX -- http://java.com
* H2 -- http://www.h2database.com/
* Hibernate -- http://hibernate.org/
* ControlsFX -- http://fxexperience.com/controlsfx/
* And other Open Source / Public Domain applications
