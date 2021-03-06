#!/bin/bash

set -e

echo "TRAVIS_PULL_REQUEST $TRAVIS_PULL_REQUEST"
echo "TRAVIS_TAG $TRAVIS_TAG"

export TAG=`if [ "$TRAVIS_PULL_REQUEST" = "false" -a -n "$TRAVIS_TAG" ] ; then echo "$TRAVIS_TAG" ; fi`

if [ "$TAG" ]; then
  echo "Build and upload Javadoc to https://commercetools.github.io/commercetools-payment-integration-java/javadoc/v/$TAG"
  ./gradlew --info -Dbuild.version=$TRAVIS_TAG aggregateJavadocs gitPublishPush

  echo "Upload tag $TAG to bintray"
  ./gradlew --info -Dbuild.version=$TRAVIS_TAG bintrayUpload
else
  echo "The tag is empty - publish is skipped"
fi
