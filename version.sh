#!/bin/bash
# You can pass --force as first parameter to force push and tag creation.

echo "Creating tag $@"

TAG="v$@"
git tag ${TAG}

echo "Pushing tag"

git push origin ${TAG}
