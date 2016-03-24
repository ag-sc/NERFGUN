#!/bin/bash

echo "########## LOGGING FROM `date` ##########"


echo "pwd = `pwd`"

java=`which java`

echo "Java = $java"

echo "deleting prev models"


rm -r src/main/resources/models/
rm -r src/main/resources/*.log

for i in {12..12}
	do
 		echo "Setting : $i" 
		echo "########## LOGGING FROM `date` ##########"
		nohup java -Xmx70g -classpath ./log4j2.xml -jar NERFGUN.jar -s $i -r train -n 900 -d CoNLLTraining -z true -e 3 -u objective > src/main/resources/log_run_$i.log
		mv src/main/resources/all.log src/main/resources/all_$i.log
	
	done
