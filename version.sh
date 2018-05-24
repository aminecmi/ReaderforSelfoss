#!/bin/bash

BASE_VERSION="v1.6"
TODAYS_VERSION="1"

VERSION="${BASE_VERSION}.$(date '+%y%m%j')$TODAYS_VERSION"

echo "Creating tag $VERSION"

git tag $VERSION

echo "Pushing tag"

git push origin $VERSION
