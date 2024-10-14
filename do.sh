#!/usr/bin/env bash
echo_info() {
  printf "[ \033[1;34m..\033[0m ] $1\n"
}
export $(cat ./variables.env | xargs)

echo "MongoDB Host: $ENV_MONGODB_HOST"
echo "MongoDB Port: $ENV_MONGODB_PORT"

if [ $# -eq 0 ]; then
  echo_error "No arguments supplied!\n"

  echo_info "Supported arguments:"

  echo_info "-----"
  echo_info "start - To Start the application"

  echo_info "-----"
  echo_info "test - Run the unit tests"
  echo "Press Enter to continue..."
  read
  exit 1
fi

if [ $1 = "start" ]; then
  echo_info "Starting File API"

  export ENV_DEBUG_MODE=true

  mvn spring-boot:run
  echo "Press Enter to continue..."
  read
elif [ $1 = "test" ]; then
  echo_info "Running tests..."

  mvn test
  echo "Press Enter to continue..."
  read
else
  echo_error "Unknown command"
    echo "Press Enter to continue..."
    read
fi
