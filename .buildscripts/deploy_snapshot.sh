#!/bin/bash
#
# Deploy to Sonatype's snapshot repo.
#
# Adapted from https://coderwall.com/p/9b_lfq,
# http://benlimmer.com/2013/12/26/automatically-publish-javadoc-to-gh-pages-with-travis-ci/
# and https://github.com/square/javawriter

REPO="purchases-android"
USERNAME="jeiting"
JDK="oraclejdk8"
BRANCH="master"

if [ "$CIRCLE_PROJECT_REPONAME" != "$REPO" ]; then
  echo "Skipping snapshot deployment: wrong repository. Expected '$REPO' but was '$CIRCLE_PROJECT_REPONAME'."
elif [ "$CIRCLE_PROJECT_USERNAME" != "$USERNAME" ]; then
  echo "Skipping snapshot deployment: wrong owner. Expected '$USERNAME' but was '$CIRCLE_PROJECT_USERNAME'."
elif [ "$CIRCLE_JDK_VERSION" != "$JDK" ]; then
  # $CIRCLE_JDK_VERSION must be manually set in circle.yml
  echo "Skipping snapshot deployment: wrong JDK. Expected '$JDK' but was '$CIRCLE_JDK_VERSION'."
elif [ "$CIRCLE_BRANCH" != "$BRANCH" ]; then
  echo "Skipping snapshot deployment: wrong branch. Expected '$BRANCH' but was '$CIRCLE_BRANCH'."
else
  echo "Deploying snapshot..."
  ./gradlew uploadArchives
  echo "Snapshot deployed!"
fi