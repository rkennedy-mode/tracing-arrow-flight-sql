#!/bin/bash

set -eux

SCRIPT_DIRECTORY="$(dirname -- "$(readlink -f -- "$0")")"
ARROW_DIRECTORY=$SCRIPT_DIRECTORY/.arrow
MAVEN_DIRECTORY=$SCRIPT_DIRECTORY/.m2_local

# Check out the Arrow project if it isn't already
if [ ! -d "$ARROW_DIRECTORY" ]; then
  echo "Arrow directory ($ARROW_DIRECTORY) does not exist, cloning"
  git clone -b apache-arrow-14.0.2 https://github.com/apache/arrow.git "$ARROW_DIRECTORY"

  # Switch to the Arrow project directory
  pushd "$ARROW_DIRECTORY"

  # Apply our patch that generates both a shaded and un-shaded version of the JDBC driver
  git apply "$SCRIPT_DIRECTORY"/fix_jdbc_shading.patch

  # Switch back out of the Arrow project directory
  popd
fi


# Switch to the Arrow project directory
pushd "$ARROW_DIRECTORY"

# Build and install the JDBC driver to a local repository
mvn -Dmaven.repo.local="$MAVEN_DIRECTORY" -DskipTests --file java/ source:jar install

# Switch back out of the Arrow project directory
popd
