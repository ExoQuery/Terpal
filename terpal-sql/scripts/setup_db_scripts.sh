#!/usr/bin/env bash

# These scripts are copied into the docker `setup` container when it is started. The root
# is the directory from where the docker-compose is called and everything under that directory
# is included in the image. So for example if docker compose in a directory 'foo' then
# everything under 'foo/*' is included inside including the DB setup scripts. We write these paths based on that.
export SQLITE_SCRIPT=src/test/resources/db/sqlite-schema.sql
export MYSQL_SCRIPT=src/test/resources/db/mysql-schema.sql
export SQL_SERVER_SCRIPT=src/test/resources/db/sqlserver-schema.sql
export ORACLE_SCRIPT=src/test/resources/db/oracle-schema.sql

function get_host() {
    if [ -z "$1" ]; then
        echo "127.0.0.1"
    else
        echo "$1"
    fi
}
# usage: setup_x <script>

function setup_sqlite() {
    # DB File in terpal-sql
    echo "Creating sqlite DB File"
    DB_FILE=terpal_test.db
    echo "Removing Previous sqlite DB File (if any)"
    rm -f $DB_FILE
    echo "Creating sqlite DB File"
    echo "(with the $SQLITE_SCRIPT script)"
    sqlite3 $DB_FILE < $SQLITE_SCRIPT
    echo "Setting permissions on sqlite DB File"
    chmod a+rw $DB_FILE

    echo "Sqlite ready!"
}

function setup_mysql() {
    port=$2
    password=''
    if [ -z "$port" ]; then
        echo "MySQL Port not defined. Setting to default: 3306  "
        port="3306"
    else
        echo "MySQL Port specified as $port"
    fi

    connection=$1
    MYSQL_ROOT_PASSWORD=root

    echo "Waiting for MySql"
    # If --protocol not set, --port is silently ignored so need to have it
    until mysql --protocol=tcp --host=$connection --password="$MYSQL_ROOT_PASSWORD" --port=$port -u root -e "select 1" &> /dev/null; do
        echo "Tapping MySQL Connection, this may show an error> mysql --protocol=tcp --host=$connection --password='$MYSQL_ROOT_PASSWORD' --port=$port -u root -e 'select 1'"
        mysql --protocol=tcp --host=$connection --password="$MYSQL_ROOT_PASSWORD" --port=$port -u root -e "select 1" || true
        sleep 5;
    done

    echo "**Verifying MySQL Connection> mysql --protocol=tcp --host=$connection --password='...' --port=$port -u root -e 'select 1'"
    mysql --protocol=tcp --host=$connection --password="$MYSQL_ROOT_PASSWORD" --port=$port -u root -e "select 1"
    echo "MySql: Create terpal_test"
    mysql --protocol=tcp --host=$connection --password="$MYSQL_ROOT_PASSWORD" --port=$port -u root -e "CREATE DATABASE terpal_test;"
    echo "MySql: Write Schema to terpal_test"
    mysql --protocol=tcp --host=$connection --password="$MYSQL_ROOT_PASSWORD" --port=$port -u root terpal_test < $MYSQL_SCRIPT
}

function setup_sqlserver() {
    host=$(get_host $1)
    echo "Waiting for SqlServer"
    until /opt/mssql-tools/bin/sqlcmd -S $1 -U SA -P "TerpalRocks!" -Q "select 1" &> /dev/null; do
        sleep 5;
    done
    echo "Connected to SqlServer"

    /opt/mssql-tools/bin/sqlcmd -S $1 -U SA -P "TerpalRocks!" -Q "CREATE DATABASE terpal_test"
    /opt/mssql-tools/bin/sqlcmd -S $1 -U SA -P "TerpalRocks!" -d terpal_test -i $2
}

# Do a simple netcat poll to make sure the oracle database is ready.
# All internal database creation and schema setup scripts are handled
# by the container and docker-compose steps.

function setup_oracle() {
    while ! nc -z $1 1521; do
        echo "Waiting for Oracle"
        sleep 2;
    done;
    sleep 2;

    echo "Creating Oracle Database"
    java -cp '/sqlline/sqlline.jar:/sqlline/ojdbc.jar' 'sqlline.SqlLine' \
      -u 'jdbc:oracle:thin:@oracle:1521:xe' \
      -n secretsysuser -p 'secretpassword' \
      -e 'CREATE USER terpal_test IDENTIFIED BY "TerpalRocks!" QUOTA 50M ON system;' \
      --showWarnings=false

    # For some reason need to do `GRANT DBA TO terpal_test;` in a separate command from the the `CREATE USER` command.
    echo "Granting Oracle Roles"
    java -cp '/sqlline/sqlline.jar:/sqlline/ojdbc.jar' 'sqlline.SqlLine' \
      -u 'jdbc:oracle:thin:@oracle:1521:xe' \
      -n secretsysuser -p 'secretpassword' \
      -e "GRANT DBA TO terpal_test;" \
      --showWarnings=false

    echo "Running Oracle Setup Script"
    java -cp '/sqlline/sqlline.jar:/sqlline/ojdbc.jar' 'sqlline.SqlLine' \
      -u 'jdbc:oracle:thin:@oracle:1521:xe' \
      -n terpal_test -p 'TerpalRocks!' \
      -f "$ORACLE_SCRIPT" \
      --showWarnings=false

    echo "Extending Oracle Expirations"
    java -cp '/sqlline/sqlline.jar:/sqlline/ojdbc.jar' 'sqlline.SqlLine' \
      -u 'jdbc:oracle:thin:@oracle:1521:xe' \
      -n terpal_test -p 'TerpalRocks!' \
      -e "alter profile DEFAULT limit PASSWORD_REUSE_TIME unlimited; alter profile DEFAULT limit PASSWORD_LIFE_TIME  unlimited; alter profile DEFAULT limit PASSWORD_GRACE_TIME unlimited;" \
      --showWarnings=false

    echo "Connected to Oracle"
    sleep 2
}

function send_script() {
  echo "Send Script Args: 1: $1 - 2 $2 - 3: $3"
  docker cp $2 "$(docker-compose ps -q $1)":/$3
}

export -f setup_sqlite
export -f setup_mysql
export -f setup_sqlserver
export -f setup_oracle
export -f send_script