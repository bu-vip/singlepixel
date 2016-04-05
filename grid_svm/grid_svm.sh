#!/bin/sh

QUEUE="bme.q"
MAX_JOBS=16
C_MIN="-15"
C_MAX="15"
C_STEP="1"
G_MIN="-15"
G_MAX="0"
G_STEP="1"
echo "Enter the SVM args:"
read SVM_ARGS
echo "Enter the job name:"
read NAME

./grid_svm "$QUEUE" "$MAX_JOBS" "$C_MIN" "$C_MAX" "$C_STEP" "$G_MIN" "$G_MAX" "$G_STEP" "$SVM_ARGS" "$NAME"
