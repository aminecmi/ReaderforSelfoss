#!/bin/bash

BASE_VERSION="1.6"
TODAYS_VERSION="1"

VERSION="${BASE_VERSION//./}$(date '+%y%m%j')$TODAYS_VERSION"

./version.sh ${VERSION} $1

if [[ "$@" == *'--publish'* ]]
then
    ./publish-version.sh ${VERSION}
else
    echo "Did not publish. If you wanted to do so, call the script with \"--publish\"."
fi
