#!/bin/bash

# Database connection details
DB_NAME="userdb"
DB_USER="admin"
DB_PASS="pass"

# SQL to drop the users table
SQL="
DROP TABLE IF EXISTS users;
"

# Export PGPASSWORD to avoid password prompt
export PGPASSWORD=$DB_PASS

# Execute the SQL command
psql -U $DB_USER -d $DB_NAME -c "$SQL"

# Check if the table was dropped successfully
if [ $? -eq 0 ]; then
    echo "Users table dropped successfully!"
else
    echo "Error dropping users table."
fi
