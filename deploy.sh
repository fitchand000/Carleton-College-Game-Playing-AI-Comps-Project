#!/bin/bash

# This file is for uploading the python code and the jar files to the carleton server
# If this is the first time running the file you need to run: chmod +x deploy.sh
# To run this script: ./deploy.sh username

mkdir -p Catan-Project/build
cp -vr build/libs Catan-Project/build
rm -R Catan-Project/build/libs/.gradle
cd Catan-Project
mkdir python
cd ..
cd python
cp jsserver.properties old_trainer.py simulation.py test_set_up.py timer.py trainer.py ../Catan-Project/python
cd ..

echo "Username is: $1"
sftp "$1@command.dmz.carleton.edu" << EOF
    mkdir Catan-Project
    put -r Catan-Project/
EOF
rm -R Catan-Project

echo "Done"

