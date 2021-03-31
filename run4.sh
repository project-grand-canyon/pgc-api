#!/bin/bash

set -eu

dbcontainer=pgc-mysql
dbport=3306
dummy_data_path=db/dummyData.sql

create_db_container() {
  docker run --name "$dbcontainer" -e MYSQL_ROOT_PASSWORD=pw -d -p $dbport:3306 mysql:5
  cat <<EOF | docker exec -i "$dbcontainer" /bin/sh -c 'cat > /root/mysql-root.cnf'
[client]
user=root
password=pw
EOF
}

start_db_container() {
  if docker container inspect "$dbcontainer" > /dev/null
  then
    docker start "$dbcontainer"
  else
    create_db_container
  fi
}

stop_db_container() {
  docker stop "$dbcontainer"
}

create_admin() {
  # creates admin user with credentials "admin" / "password"
  cat <<EOF | mysql core
INSERT INTO admins (user_name, token, login_enabled, is_root)
VALUES ('admin', 'OVrhTP2wUe1S8UBKZv9cCr_uVa3ZeSRKEc6RXLSm_HI', true, true);
EOF
}

seed_database_newman() {
  # Requires the api server to be running
  node_modules/newman/bin/newman.js run src/test/DummyData.postman_collection.json -e src/test/Data.postman_environment.json
}

server_pid=
regenerate_dummy_data() {
  echo "Before running this command, make sure your database container and server are stopped."
  echo "Nothing bad will happen if they are running, it just won't work."
  read -p "Press enter to continue." x
  (
    export dbcontainer=pgc-mysql-dummy-regen
    export dbport=3316
    local -r logfile=regen-dummy-data.server.log

    stop_db_container && _destroy_db || true

    start_db_container
    await_db_startup
    init_db_schema
    create_admin
    export PGC_DB_URL=jdbc:mysql://root:pw@localhost:3306/core?autoReconnect=true&useSSL=false
    echo "Server output will be logged to $logfile"
    mvn clean jetty:run > "$logfile" 2>&1 &
    server_pid=$!
    trap "set -x; kill $server_pid; wait $server_pid" EXIT
    echo "Server PID:" $server_pid

    await_server_startup

    seed_database_newman
    mysql_dump core -t > "$dummy_data_path"
    echo "Saved to $dummy_data_path"
    stop_db_container
    _destroy_db
  )
}

_tty_opt() { [ -t 0 ] && printf -- '-t'; }

db_container_shell() {
  docker exec -i $(_tty_opt) "$dbcontainer" /bin/bash "$@"
}

mysql() {
  docker exec -i $(_tty_opt) "$dbcontainer" mysql --defaults-extra-file='~/mysql-root.cnf' "$@"
}

mysql_dump() {
  docker exec -i "$dbcontainer" mysqldump --defaults-extra-file='~/mysql-root.cnf' "$@"
}

init_db_schema() {
  mysql <src/main/resources/createTables.sql
  for f in db/migrations/*
  do
    echo "Running migration: $f"
    mysql core <"$f"
  done
}

init_db() {
  init_db_schema
  echo "Importing dummy data from $dummy_data_path"
  mysql core <"$dummy_data_path"
}

await_server_startup() {
  while ! curl -s 'http://localhost:8080/' >/dev/null; do
    echo waiting for server startup...
    sleep 1;
  done
}

await_db_startup() {
  while ! mysql -e 'select null' 2>/dev/null; do
    echo waiting for db startup...
    sleep 1;
  done | grep -v NULL
}

setup_db() {
  echo "Creating database container..."
  start_db_container
  await_db_startup
  echo "Database started, initializing..."
  init_db
  echo "Database container created successfully!"
}

drop_and_reinit_core_db() {
  echo "DROP DATABASE IF EXISTS core;" | mysql
  init_db
}

_destroy_db() {
  docker rm -v "$dbcontainer"
}

destroy_db() {
  read -p 'This will throw away all your local data.  Are you sure? [y/N]> ' confirm
  confirm=${confirm-N}
  if ! [[ "$confirm" == 'Y' || "$confirm" == 'y' ]]
  then
    return
  fi
  stop_db_container
  _destroy_db
}

start() {
  mvn clean jetty:run
}


"$@"


