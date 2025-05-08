#!/bin/bash

# Database connection details
DB_NAME="userdb"
DB_USER="admin"
DB_PASS="pass"

# SQL to create the users table
SQL="
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    public_key VARCHAR(255) NOT NULL
);
"

# Export PGPASSWORD to avoid prompting for password
export PGPASSWORD=$DB_PASS

# Execute the SQL command
psql -U $DB_USER -d $DB_NAME -c "$SQL"

# Check if the table was created successfully
if [ $? -eq 0 ]; then
    echo "Users table created successfully!"
else
    echo "Error creating users table."
fi
