#!/bin/bash

./gradlew clean --info
./gradlew assembleAndroidTest --info
./gradlew assembleDebug --info
./gradlew assemble --info
