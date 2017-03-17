#!/bin/sh

# Queue to submit jobs to
QUEUE="bme.q"
# Max number of jobs we're allowed to submit
MAX_JOBS=16
# C
C_MIN="-15"
C_MAX="15"
C_STEP="1"
# Gamma
G_MIN="-15"
G_MAX="0"
G_STEP="1"
# Epsilon
P_MIN="-8"
P_MAX="-1"
P_STEP="1"

./grid_svr "$QUEUE" "$MAX_JOBS" "$C_MIN" "$C_MAX" "$C_STEP" "$G_MIN" "$G_MAX" "$G_STEP" "$P_MIN" "$P_MAX" "$P_STEP" "$1" "$2"
