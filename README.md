# GrandCanyon project

## Background
API for [projectgrandcanyon.com](projectgrandcanyon.com), a project of [CCL](citizensclimatelobby.org) volunteers, that provides a way to help people call their Member of Congress about climate change on a regular basis.

## Maintainers
* Neal Prager
* Ben Boral

## Local Dev

### Using a locally running MySQL Database

1. Install docker
2. Run `docker run --name pgc-mysql -e MYSQL_ROOT_PASSWORD=pw -d -p 3306:3306 mysql:5.7`
3. Login to the database `mysql -h localhost -P 3306 --protocol=tcp -u root -ppw`
4. Create a database `create database core;`
5. Exit MySQL `exit`
6. Run DDL `mysql -h localhost -P 3306 --protocol=tcp -u root -ppw core < src/main/resources/createTables.sql`
7. Create a config file `cp src/main/resources/config.properties.example src/main/resources/config.properties`
8. Run the app `mvn clean jetty:run`

### Using a shared CloudSQL Database

1. Create a [GCP](https://cloud.google.com/) account, download `gcloud`, and authenticate
2. Ask maintainers to be added to GCP IAM for access to the database. Also ask for access to `config.properties` and `sentry.properties` files, which contain application settings and secrets
3. Place `config.properties` and `sentry.properties` files in `src/main/resources/` directory 
4. Run the command `mvn clean jetty:run -DINSTANCE_CONNECTION_NAME=instanceConnectionName -Duser=root -Dpassword=myPassword -Ddatabase=myDatabase` where instanceConnectionName is the Instance connection name for the CloudSQL instance, myPassword is the DB root user's password, and myDatabase is the database name.

The application expects to connect to a Google CloudSQL database.  The database URL is in defined by the sqlUrl property in pom.xml.

### API Sessions

1. Install [Postman](https://www.getpostman.com/)
2. Import the postman file from `src/main/test` to your Postman app
3. Make request "Create Initial Super Admin"
4. Log into DB and run query `UPDATE admins SET login_enabled = 1 WHERE admin_id = {new admin id};`. Get the `{new admin id}` by querying `SELECT * FROM admins`.
5. Make request "Super Admin Login"
6. Under the GrandCanyon collection, click edit -> Variables, and replace the "CURRENT VALUE" of `rootToken` with the token returned by the previous step. This will allow you to use the admin credentials of your local server in Postman requests.
7. You have created a session for this super admin. Include the variable `rootToken` as the bearer token in requests to authenticated endpoints.

#### Creating a test user

1. Run "Create Admin" in Postman. You may need to alter the `districts` array in the body to match the id present in your database (`SELECT * FROM districts`). This will automatically create a user with login enabled, so you will not have to worry about email validation.
2. Log in as the user (testAlert/password).

## Deployment
Run this command: 

`mvn clean appengine:deploy -DINSTANCE_CONNECTION_NAME=instanceConnectionName -Duser=root -Dpassword=myPassword -Ddatabase=myDatabase`