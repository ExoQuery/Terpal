#!/usr/bin/env bash

set -e

# import setup functions
. /app/scripts/setup_db_scripts.sh

time setup_sqlite

time setup_mysql mysql
# SQL Server needs to be passed different script paths based on environment (based on exports in setup_db_scripts). Therefore it has a 2nd arg.
time setup_sqlserver sqlserver $SQL_SERVER_SCRIPT

time setup_oracle oracle


echo "Databases are ready!"