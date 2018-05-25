#!/bin/bash
# You can pass --force as first parameter to force push and tag creation.

echo "Creating tag $1"

git tag $1 $@

echo "Pushing tag"

git push origin $1 $@
