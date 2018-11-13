#!/bin/bash

git fetch --tags -p

BASE_VERSION="1.7"
LAST_TAG=$(git tag -l | sort -V | tail -1)

INITIAL_VERSION="${BASE_VERSION//./}$(date '+%y%m%j')"

LAST_DAY_VERSION=$(echo $LAST_TAG | sed "s/v${INITIAL_VERSION}//")
LAST_DAY_VERSION_LENGTH=$(echo "${#LAST_DAY_VERSION}")

if [[ "$LAST_DAY_VERSION_LENGTH" == "1" ]]
then
    TODAYS_VERSION=$(( $LAST_DAY_VERSION + 1 ))
else
    TODAYS_VERSION="1"
fi

VERSION="${INITIAL_VERSION}${TODAYS_VERSION}"

PARAMS_EXCEPT_PUBLISH=$(echo $1 | sed 's/\-\-publish//')

./version.sh ${VERSION} ${PARAMS_EXCEPT_PUBLISH}

if [[ "$@" == *'--publish'* ]]
then
    ./publish-version.sh ${VERSION}
else
    echo "Did not publish. If you wanted to do so, call the script with \"--publish\" or \"--publish-local\"."
fi
