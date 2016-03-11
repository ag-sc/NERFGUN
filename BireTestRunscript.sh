#!/bin/bash

echo "########## START SCRIPT AT `date` ##########"


echo "pwd = `pwd`"

java=`which java`

echo "Java = $java"

modelsFilePath="src/main/resources/models/*"
echo "Seach for models in $modelsFilePath"
testset="CoNLLTesta"

command="java -Xmx32g -classpath ./log4j2.xml -jar NERFGUN_Test.jar $model $testset"
for model in $modelsFilePath;
	do
		echo "########## LOGGING FROM `date` ##########"
 		echo "Model = $model" 
		echo "Testset = $testset"
		echo "Excecute Command "$command""
		`$command`
	
        mv src/main/resources/all.log src/main/resources/all_test_$model.log


	done
