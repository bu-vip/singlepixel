#!/bin/sh

# make and clear tmp dir
mkdir "tmp"

#make the results dir
mkdir "results"

PARAM_FILE="results/parameters.txt";

# go through all different job results and concat into a single file per data split
for f in out/sixPrivate*Y.scaled;
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
	sed -i.bak -E -e 's/log2c=(.*) log2g=(.*) rate=(.*)/\3 \1 \2/' $GRID_FILE;
	sed -i.bak -E -e 's/log2c=(.*) log2g=(.*) log2p=(.*) mse=(.*)/\4 \1 \2 \3/' $GRID_FILE;

	# get last letter of split to see if classification
	i=$((${#SPLIT_NAME}-1));
	LAST_LET="${SPLIT_NAME:$i:1}";

	# sort lines and write best result
	if [[ "$LAST_LET" == "C" ]]
	then
		PARAMS=`sort $GRID_FILE -r | head -n 1`;
		echo "$SPLIT_NAME $PARAMS" >> $PARAM_FILE;
		SVM_ARGS="-s 0";
	else
		PARAMS=`sort $GRID_FILE | head -n 1`
		echo "$SPLIT_NAME $PARAMS" >> $PARAM_FILE;
		SVM_ARGS="-s 3";
	fi

	# get SVM C & G parameters
	VAR_ARRAY=($PARAMS);
	if [[ "$LAST_LET" == "C" ]]
	then
		LOG2_C=${VAR_ARRAY[1]};
		C=`perl -e 'print 2 ** ($ARGV[0])' -- $LOG2_C`;
		LOG2_G=${VAR_ARRAY[2]};
		G=`perl -e 'print 2 ** ($ARGV[0])' -- $LOG2_G`;
		SVM_ARGS=$SVM_ARGS" -c "$C" -g "$G
	else
		LOG2_C=${VAR_ARRAY[1]};
		C=`perl -e 'print 2 ** ($ARGV[0])' -- $LOG2_C`;
		LOG2_G=${VAR_ARRAY[2]};
		G=`perl -e 'print 2 ** ($ARGV[0])' -- $LOG2_G`;
		LOG2_P=${VAR_ARRAY[3]};
		P=`perl -e 'print 2 ** ($ARGV[0])' -- $LOG2_P`;
		SVM_ARGS=$SVM_ARGS" -c "$C" -g "$G" -p "$P
	fi
	echo $f
	echo $SVM_ARGS

	# train model
	libsvm/svm-train $SVM_ARGS "dataSets/$SPLIT_NAME.scaled" "results/$SPLIT_NAME.model";
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
#rm -r "tmp"
