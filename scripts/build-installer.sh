#!/bin/bash
echo "Building Unde Server installer..."
echo ""
./gradlew :server:createPackage
if [ $? -eq 0 ]; then
    echo ""
    echo "Success! Installer created in: server/build/jpackage/"
else
    echo ""
    echo "Build failed. See error above."
    exit 1
fi
