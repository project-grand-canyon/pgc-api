# GrandCanyon project

## Background
API for [projectgrandcanyon.com](projectgrandcanyon.com), a project of [CCL](citizensclimatelobby.org) volunteers, that provides a way to help people call their Member of Congress about climate change on a regular basis.

## Maintainers
* Neal Prager
* Ben Boral

## Local Dev

1. Create a [GCP](https://cloud.google.com/) account, download `gcloud`, and authenticate
2. Ask maintainers to be added to GCP IAM for access to the database. Also ask for access to `config.properties` and `sentry.properties` files, which contain application settings and secrets
3. Place `config.properties` and `sentry.properties` files in `src/main/resources/` directory 
4. Run the command `mvn clean jetty:run -DINSTANCE_CONNECTION_NAME=instanceConnectionName -Duser=root -Dpassword=myPassword -Ddatabase=myDatabase` where instanceConnectionName is the Instance connection name for the CloudSQL instance, myPassword is the DB root user's password, and myDatabase is the database name.

The application expects to connect to a Google CloudSQL database.  The database URL is in defined by the sqlUrl property in pom.xml.

## Deployment
Run this command: 

`mvn clean appengine:deploy -DINSTANCE_CONNECTION_NAME=instanceConnectionName -Duser=root -Dpassword=myPassword -Ddatabase=myDatabase`