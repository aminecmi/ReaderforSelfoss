#!/bin/bash

BASE_VERSION="1.7"
TODAYS_VERSION="1"

VERSION="${BASE_VERSION//./}$(date '+%y%m%j')$TODAYS_VERSION"

PARAMS_EXCEPT_PUBLISH=$(echo $1 | sed 's/\-\-publish//')

./version.sh ${VERSION} ${PARAMS_EXCEPT_PUBLISH}

if [[ "$@" == *'--publish'* ]]
then
    ./publish-version.sh ${VERSION}
else
    echo "Did not publish. If you wanted to do so, call the script with \"--publish\"."
fi
