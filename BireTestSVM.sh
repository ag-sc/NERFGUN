#!/bin/bash

echo "########## START SCRIPT AT `date` ##########"


echo "pwd = `pwd`"

java=`which java`

echo "Java = $java"

modelsFilePath="src/main/resources/*.model"
echo "Search for models in $modelsFilePath"
testset="CoNLLTesta"


for model in $modelsFilePath;
	
	do
		echo "########## LOGGING FROM `date` ##########"
 		echo "Model = $model" 
		echo "Testset = $testset"
		 m=$(basename $model)
		nohup java -Xmx150g -XX:-UseGCOverheadLimit -jar NERFGUN.jar  $model  $testset -r wekaTest -z true  > src/main/resources/all_test_run_$m.log

		mv src/main/resources/all.log src/main/resources/all_test_$m.log	
      


	done
