#!/bin/sh

rm -rf db/database.db*

export CLASSPATH="/data/local/tmp/timetracker/timetracker-dex.jar"

APP_CMD="app_process -Dslf4j.provider=org.slf4j.simple.SimpleServiceProvider /data/local/tmp/timetracker com.ppo.tech_ui.TechUiAppKt"

$APP_CMD register admin Administrator secret ADMIN
$APP_CMD login admin secret

$APP_CMD create-base-category "Work" "#FF0000"
$APP_CMD create-base-category "Study" "#0000FF"

CAT_LIST=$($APP_CMD categories)
echo "$CAT_LIST"

WORK_UUID=$(echo "$CAT_LIST" | grep "Work" | awk '{print $1}')
echo "Found Work UUID: $WORK_UUID"

$APP_CMD start "$WORK_UUID"
sleep 2
$APP_CMD stop

$APP_CMD create-category "Gym" "#00FF00"

GYM_CAT=$($APP_CMD categories | grep "Gym")
GYM_UUID=$(echo "$GYM_CAT" | awk '{print $1}')
echo "Found Gym UUID: $GYM_UUID"

$APP_CMD add-manual "$GYM_UUID" "2026-05-21T10:00:00Z" "2026-05-21T11:30:00Z" "Morning workout"

$APP_CMD records

$APP_CMD delete-category "$GYM_UUID"
$APP_CMD delete-base-category "$WORK_UUID"

$APP_CMD categories
$APP_CMD logout
