#!/bin/bash

# NOTE: This is copy/pasted in jenkins

rm -f version.txt
printf "versionName=$1-github\nversionCode=$1" >> version.txt

# You'll need to change server as your server and define a VERSION_PATH.
scp version.txt server:$VERSION_PATH

rm version.txt
