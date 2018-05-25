#!/bin/bash

BASE_VERSION="v1.6"
TODAYS_VERSION="1"

VERSION="${BASE_VERSION}.$(date '+%y%m%j')$TODAYS_VERSION"

./version.sh ${VERSION} $@

./publish-version.sh ${VERSION}
