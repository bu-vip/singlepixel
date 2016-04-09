#!/bin/sh

# make and clear tmp dir
rm -r "tmp"
mkdir "tmp"

#make the results dir
mkdir "results"

PARAM_FILE="results/parameters.txt";

echo "Concatenating job results..."
# go through all different job results and concat into a single file per data split
for f in out/*.scaled;
do
	# generate name of grid results file
	NO_EXT="${f%.scaled}";
	SPLIT_NAME="${NO_EXT#out/}";
	GRID_FILE="tmp/$SPLIT_NAME.grid";

	#concatenate all job files
	for j in $f/*/*.txt;
	do
		cat $j >> $GRID_FILE;
	done;

	# reorder the result terms to make it easier to sort
	sed -i.bak -r -e 's/log2c=(.*) log2g=(.*) rate=(.*)/\3 \1 \2/' $GRID_FILE;

	# get last letter of split to see if classification
	i=$((${#SPLIT_NAME}-1));
	LAST_LET="${SPLIT_NAME:$i:1}";

	# sort lines and write best result
	if [[ "$LAST_LET" == "C" ]]
	then
		echo "$SPLIT_NAME `sort $GRID_FILE -r | head -n 1`" >> $PARAM_FILE;
		SVM_ARGS="-s 0";
	else
		echo "$SPLIT_NAME `sort $GRID_FILE | head -n 1`" >> $PARAM_FILE;
		SVM_ARGS="-s 3";
	fi

	# get SVM C & G paremeters
	LOG_VARS=`cat $PARAM_FILE | egrep $SPLIT_NAME`;
	VAR_ARRAY=($LOG_VARS);
	LOG2_C=${VAR_ARRAY[2]};
	C=`perl -e 'print 2 ** ($ARGV[0])' -- $LOG2_C`;
	LOG2_G=${VAR_ARRAY[3]};
	G=`perl -e 'print 2 ** ($ARGV[0])' -- $LOG2_G`;

	# train model
	libsvm/svm-train $SVM_ARGS dataSets/ results/$SPLIT_NAME.model;
	TEST_FILE="${SPLIT_NAME/train/test}";
	# test model
	OUT=`libsvm/svm-predict "dataSets/$TEST_FILE.scaled" "results/$SPLIT_NAME.model" "results/$TEST_FILE.predicted"`;

	# also output the actual values
	cut -d' ' -f1 "dataSets/$TEST_FILE.scaled" > "results/$TEST_FILE.actual";


	echo "$SPLIT_NAME" >> out.txt;
	echo "$OUT" >> out.txt;
	echo "" >> out.txt;
done;

# remove the temp dir
rm -r "tmp"
