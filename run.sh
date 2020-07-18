#!/bin/bash

set -eu

mysql-run() {
  docker exec -i pgc-mysql mysql -u root -ppw core "$@"
}

mysql-shell() {
  docker exec -it pgc-mysql mysql -u root -ppw core "$@"
}

mysql-dump() {
  docker exec -i pgc-mysql mysqldump -u root -ppw core "$@"
}

create-admin() {
  cat <<EOF | mysql-run
INSERT INTO admins (user_name, token, login_enabled, is_root)
VALUES ('admin', 'OVrhTP2wUe1S8UBKZv9cCr_uVa3ZeSRKEc6RXLSm_HI', true, true);
EOF
}

setup-dummy-data() {
  node_modules/newman/bin/newman.js run src/test/DummyData.postman_collection.json -e src/test/Data.postman_environment.json
}

init-db() {
  create-admin
  setup-dummy-data
}

start() {
  mvn clean jetty:run
}

"$@"