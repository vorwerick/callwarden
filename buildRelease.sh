#!/bin/bash

./gradlew clean
./gradlew bundleRelease

open app/build/outputs/bundle/release