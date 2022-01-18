# Test assignment: File API

For assignment description see the [assignment.md](assignment.md) file.

## Start-up

### Starting the database
    docker-compose up -d

### Configuration

See `variables.env` file

## Usage
In development add

    127.0.0.1    filedb
to your `/etc/hosts` file

For basic auth, username is `admin` and password is `hunter2`

### Start from CLI

    ./do.sh start

For API documentation go to http://localhost:6011/docs
