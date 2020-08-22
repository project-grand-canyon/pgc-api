# GrandCanyon project

## Background
API for [projectgrandcanyon.com](projectgrandcanyon.com), a project of [CCL](citizensclimatelobby.org) volunteers, that provides a way to help people call their Member of Congress about climate change on a regular basis.

## Maintainers
* Ben Boral

## Local Dev

### Using a locally running MySQL Database
1. Install and Run Docker
 * MacOS
     - Download from [Docker Hub](https://docs.docker.com/docker-for-mac/install/)
 * Windows
     * Verify that virtualization is turned on on your computer: Enter Task Manager. Switch to the performance tab at top, and check if Virtualization is enabled. If Virtualization is not enabled, turn off your computer, access your  computer bios (command key may vary by machine). In the bios there should be an option to turn on Virtualization setting in bios.
     * [Download and install Docker Desktop](https://docs.docker.com/get-docker/ "Download and install Docker Desktop"): If you are unable to simply download and run the installer, begin troubleshooting by verifying that your computer meets the system requirements for Docker.
     * Run Docker Desktop: An icon should appear on your taskbar. If you are unable to start docker try opening docker settings by right clicking on the icon and selecting settings then turning on “Expose daemon on tcp://localhost:2375 without TLS” setting. You can verify that docker is working properly by running `docker hello-world`

2. Create and set up the db container by running `./run.sh setup-db` from within the repo directory.  You should see a
   lot of output followed by "Container created successfully!".  There will now be an admin user available for your use
   with username `admin` and password `password`.
   
   You can use `./run.sh stop-db-container`, to shut it down, and `./run.sh start-db-container` to relaunch it in a
   future session.
   
   To start a mysql shell, use `./run.sh mysql`.
   
   To destroy the database in order to start over from scratch, you can use `./run.sh destroy-db`.

3. Set the app config for running locally `$ cp src/main/resources/config.properties.local src/main/resources/config.properties`

### Using the CloudSQL Database
1. Create a [GCP](https://cloud.google.com/) account, download gcloud, and authenticate
2. Ask maintainers to be added to GCP IAM for access to the database. Also ask for access to `config.properties` and `sentry.properties` files, which contain application settings and secrets
3. Place `config.properties` and `sentry.properties` files in `src/main/resources/` directory
4. Run the command: `mvn clean jetty:run -DINSTANCE_CONNECTION_NAME=instanceConnectionName -Duser=root -Dpassword=myPassword -Ddatabase=myDatabase` where instanceConnectionName is the Instance connection name for the CloudSQL instance, myPassword is the DB root user's password, and myDatabase is the database name.

The application expects to connect to a Google CloudSQL database. The database URL is defined by the sqlUrl property in pom.xml.

### After the database has been setup
1. Download and install Maven:
 * MacOS
      * Use manual [install instructions](https://maven.apache.org/install.html) or use [homebrew](https://brew.sh/) to install with `brew install maven`
 * [Download for Windows](https://maven.apache.org/install.html). 
      * You must update the `PATH` environment variable to include `apache-maven-3.6.3/bin`. On windows this can be accessed by searching for `Advanced System Settings`, then switching to the advanced tab, selecting environment variables button, highlighting the `PATH` variable, clicking edit, clicking new, then finding the directory (probably in `Program Files` or `Program Files x86`). 
2. Install Java 8 JDk
 * You can use [jenv](https://www.jenv.be/) to manage different versions of Java for different projects. Or...
 * You can simply [Download Java 8 JDK](https://www.oracle.com/java/technologies/javase/javase-jdk8-downloads.html)
 * Similar to Maven, we also need to change an environment variable to reflect this new Download. You'll then set the `JAVA_HOME` environment variable the Java 8 JDK path. Unlike using jenv, this is a global setting.
 * Open Intellij and go to file->Project Structure (CTRL+ALT+SHIFT+S). Click on JDK’s and select Java 1.8.
3. Confirm that Maven is using Java 8 with `mvn -version`
4. You can now run the app: `mvn clean jetty:run`

### Regenerate the database dummy data via API
Running ./run.sh setup-db will handle everything in normal cases, but in case you want to re-generate dummyData.sql
using API calls (such as after changing how data is saved in the db) you can follow the process below.
This will not affect your pgc-mysql database container -- it creates a temporary
db container, and starts the API service pointing at that.
1. Install [Postman](https://www.postman.com/downloads/)
2. Install Newman using `npm install newman`
   * This step requires `npm` to be installed. It should install by default when you download [Node.js](https://nodejs.org/en/)
3. Stop your database container and server if they are running.
4. Run
`./run.sh regenerate-dummy-data`

###Modifying Automated Emails
1. Emails can be found in the `src/main/resources` folder. 
2. To edit an email, copy the contents of the  `emailName.mjml` into a [convenient mjml editor](https://mjml.io/try-it-live).
3. Then copy both the modified `emailName.mjml` and the resulting `emailName.html` back into their appropriate files. 

## Deployment
Run: `mvn clean appengine:deploy -DINSTANCE_CONNECTION_NAME=instanceConnectionName -Duser=root -Dpassword=myPassword -Ddatabase=myDatabase`
 For a local MySQL Database, instanceConnectionName = `pgc-mysql`, myPassword = `pw`, and myDatabase = `core`. 
