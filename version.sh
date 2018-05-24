#!/bin/bash
# You can pass --force as first parameter to force push and tag creation.
BASE_VERSION="v1.6"
TODAYS_VERSION="1"

VERSION="${BASE_VERSION}.$(date '+%y%m%j')$TODAYS_VERSION"

echo "Creating tag $VERSION"

git tag $VERSION $1

echo "Pushing tag"

git push origin $VERSION $1
