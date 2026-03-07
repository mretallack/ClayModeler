#!/bin/bash
set -e

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}ClayModeler - Headless Emulator Test Script${NC}"
echo ""

# Configuration
AVD_NAME="ClayModeler_Test"
API_LEVEL=29
ABI="x86_64"
DEVICE="pixel_3a"
EMULATOR_PORT=5554

# Check if KVM is available
if [ ! -e /dev/kvm ]; then
    echo -e "${RED}Error: KVM not available. Hardware acceleration required.${NC}"
    exit 1
fi

echo -e "${GREEN}✓ KVM available${NC}"

# Check if Android SDK is set up
if [ -z "$ANDROID_HOME" ]; then
    if [ -f "local.properties" ]; then
        ANDROID_HOME=$(grep "sdk.dir" local.properties | cut -d'=' -f2)
        export ANDROID_HOME
        export PATH="$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
    else
        echo -e "${RED}Error: ANDROID_HOME not set and local.properties not found${NC}"
        exit 1
    fi
fi

echo -e "${GREEN}✓ Android SDK: $ANDROID_HOME${NC}"

# Check if AVD exists
if ! $ANDROID_HOME/emulator/emulator -list-avds | grep -q "^${AVD_NAME}$"; then
    echo -e "${YELLOW}Creating AVD: $AVD_NAME${NC}"
    
    # Check if system image is installed
    if ! $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --list_installed | grep -q "system-images;android-${API_LEVEL};default;${ABI}"; then
        echo -e "${YELLOW}Installing system image...${NC}"
        yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "system-images;android-${API_LEVEL};default;${ABI}"
    fi
    
    # Create AVD
    echo no | $ANDROID_HOME/cmdline-tools/latest/bin/avdmanager create avd \
        -n "$AVD_NAME" \
        -k "system-images;android-${API_LEVEL};default;${ABI}" \
        -d "$DEVICE" \
        --force
    
    echo -e "${GREEN}✓ AVD created${NC}"
else
    echo -e "${GREEN}✓ AVD exists: $AVD_NAME${NC}"
fi

# Check if emulator is already running
if adb devices | grep -q "emulator-${EMULATOR_PORT}"; then
    echo -e "${YELLOW}Emulator already running, killing...${NC}"
    adb -s "emulator-${EMULATOR_PORT}" emu kill
    sleep 2
fi

# Start emulator in headless mode
echo -e "${YELLOW}Starting headless emulator...${NC}"
$ANDROID_HOME/emulator/emulator -avd "$AVD_NAME" \
    -no-window \
    -no-audio \
    -no-boot-anim \
    -gpu swiftshader_indirect \
    -port "$EMULATOR_PORT" \
    &> emulator.log &

EMULATOR_PID=$!
echo -e "${GREEN}✓ Emulator started (PID: $EMULATOR_PID)${NC}"

# Wait for emulator to boot
echo -e "${YELLOW}Waiting for emulator to boot...${NC}"
$ANDROID_HOME/platform-tools/adb wait-for-device
sleep 5

# Wait for boot to complete
BOOT_COMPLETE=0
for i in {1..60}; do
    if adb shell getprop sys.boot_completed 2>/dev/null | grep -q "1"; then
        BOOT_COMPLETE=1
        break
    fi
    echo -n "."
    sleep 2
done
echo ""

if [ $BOOT_COMPLETE -eq 0 ]; then
    echo -e "${RED}Error: Emulator failed to boot${NC}"
    kill $EMULATOR_PID 2>/dev/null || true
    exit 1
fi

echo -e "${GREEN}✓ Emulator booted${NC}"

# Run tests
echo -e "${YELLOW}Running tests...${NC}"
./gradlew connectedDebugAndroidTest --console=plain

TEST_RESULT=$?

# Kill emulator
echo -e "${YELLOW}Stopping emulator...${NC}"
adb -s "emulator-${EMULATOR_PORT}" emu kill
sleep 2

if [ $TEST_RESULT -eq 0 ]; then
    echo -e "${GREEN}✓ Tests passed${NC}"
    exit 0
else
    echo -e "${RED}✗ Tests failed${NC}"
    exit 1
fi
