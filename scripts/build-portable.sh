#!/bin/bash
echo "Building Unde Server portable application..."
echo ""
./gradlew :server:createPortable
if [ $? -eq 0 ]; then
    echo ""
    echo "Success! Portable app created in: server/build/jpackage/"
else
    echo ""
    echo "Build failed. See error above."
    exit 1
fi
