#!/bin/bash
#
# Deploy to Sonatype's snapshot repo.
#
# Adapted from https://coderwall.com/p/9b_lfq,
# http://benlimmer.com/2013/12/26/automatically-publish-javadoc-to-gh-pages-with-travis-ci/
# and https://github.com/square/javawriter

REPO="purchases-android"
USERNAME="RevenueCat"
JDK="oraclejdk8"
VERSION=$(grep "versionName" purchases/build.gradle | awk '{print $2}' | sed -e 's/^"//' -e 's/"$//')

if [ "$CIRCLE_PROJECT_REPONAME" != "$REPO" ]; then
  echo "Skipping snapshot deployment: wrong repository. Expected '$REPO' but was '$CIRCLE_PROJECT_REPONAME'."
elif [ "$CIRCLE_PROJECT_USERNAME" != "$USERNAME" ]; then
  echo "Skipping snapshot deployment: wrong owner. Expected '$USERNAME' but was '$CIRCLE_PROJECT_USERNAME'."
elif [ "$CIRCLE_JDK_VERSION" != "$JDK" ]; then
  # $CIRCLE_JDK_VERSION must be manually set in circle.yml
  echo "Skipping snapshot deployment: wrong JDK. Expected '$JDK' but was '$CIRCLE_JDK_VERSION'."
elif [ "$CIRCLE_BRANCH" == "main" ] || [ "$CIRCLE_BRANCH" == "bug/fixes-deploys" ] && [[ "$VERSION" == *SNAPSHOT ]]; then
  echo "Deploying snapshot..."
 ./gradlew androidSourcesJar androidJavadocsJar uploadArchives -P signing.keyId=$GPG_SIGNING_KEY_ID -Psigning.password=$GPG_SIGNING_KEY_PW -Psigning.secretKeyRingFile=./secring.gpg \
                          -PSONATYPE_NEXUS_USERNAME=$SONATYPE_NEXUS_USERNAME -PSONATYPE_NEXUS_PASSWORD=$SONATYPE_NEXUS_PASSWORD -PRELEASE_SIGNING_ENABLED=true
  echo "Snapshot deployed!"
fi