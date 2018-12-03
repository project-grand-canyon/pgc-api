# GrandCanyon project

Expects to connect to a Google CloudSQL database.  The database URL is in defined by the sqlUrl property in pom.xml.

To run the Jetty service locally, use the command:

mvn clean jetty:run -DINSTANCE_CONNECTION_NAME=instanceConnectionName -Duser=root -Dpassword=myPassword -Ddatabase=myDatabase

where:
   instanceConnectionName is the Instance connection name for the CloudSQL instance
   myPassword is the DB root user's password
   myDatabase is the database name

