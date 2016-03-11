#!/bin/bash

echo "########## LOGGING FROM `date` ##########"


echo "pwd = `pwd`"

java=`which java`

echo "Java = $java"

echo "deleting prev models"


rm -r src/main/resources/models/
rm -r src/main/resources/*.log

for i in {0..14}
	do
 		echo "Setting : $i" 
		echo "########## LOGGING FROM `date` ##########"
		java -Xmx32g -classpath ./log4j2.xml -jar NERFGUN.jar -s $i train
		mv src/main/resources/all.log src/main/resources/all_$i.log
	
	done
