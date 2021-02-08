#!/bin/bash

# This file is for uploading the python code and the jar files to the carleton server
# If this is the first time running the file you need to run: chmod +x deploy2.sh
# To run this script: ./deploy2.sh username

mkdir -p Catan-Project2/build
cp -vr build/libs Catan-Project2/build
rm -R Catan-Project2/build/libs/.gradle
cd Catan-Project2
mkdir python
cd ..
cp python/* Catan-Project2/python

echo "Username is: $1"
sftp "$1@command.dmz.carleton.edu" << EOF
    mkdir Catan-Project2
    put -r Catan-Project2/
EOF
rm -R Catan-Project2

echo "Done"

