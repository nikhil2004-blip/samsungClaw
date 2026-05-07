#!/bin/bash

# SIGNAL App — Test Runner Script
# This script runs all unit and instrumented tests and generates a JaCoCo coverage report.

set -e

echo "🚀 Starting SIGNAL Test Suite..."

# 1. Run Unit Tests
echo "🧪 Running Unit Tests..."
./gradlew testDebugUnitTest

# 2. Run Instrumented Tests (Requires Emulator/Device)
echo "📱 Running Instrumented Tests..."
# Note: Ensure an emulator is running or a device is connected.
./gradlew connectedDebugAndroidTest || echo "⚠️ Instrumented tests failed. Ensure an emulator is connected."

# 3. Generate Coverage Report
echo "📊 Generating JaCoCo Coverage Report..."
./gradlew jacocoTestReport

echo "✅ Test Suite Execution Finished."
echo "Coverage report can be found at: app/build/reports/jacoco/jacocoTestReport/html/index.html"
