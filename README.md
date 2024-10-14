# Test assignment: File API

For assignment description see the [assignment.md](assignment.md) file.

## Notes

There is no Java version of this. As on a daily basis we work with Kotlin projects here, the test-assignment is a short introduction into the technologies that we use here.

The aim of the test-assignment is not to only test Your development skills, but to give You an overview of how and what we do here.

### Tips

Kotlin and Java classes work very well together (https://kotlinlang.org/docs/mixing-java-kotlin-intellij.html#adding-kotlin-source-code-to-an-existing-java-project).

## Start-up

### Starting the database
    docker-compose up -d
This spins up both the development/production-ready database and testing database

### Configuration

See `variables.env` file

Also `application.properties` for additional configuration, `application-test.properties`/`application-auth-test.properties` for testing properties

You might need to set environment variables manually when launching from an IDE:
`ENV_MONGODB_DATABASE=files;ENV_MONGODB_HOST=filedb;ENV_MONGODB_PORT=27027`

Create directories for your upload paths if they don't exist yet, see `application.properties`:
    
    files-api.file-directory=uploads/development

## Usage
In development add

    127.0.0.1    filedb
    127.0.0.1    testdb
to your `/etc/hosts` file

For basic request auth, username is `admin` and password is `hunter2`

### Start from CLI
Use the startup script

    ./do.sh start

or

    spring-boot:run
to run the project

    mvn test

to test the project

For API documentation go to http://localhost:6011/docs
