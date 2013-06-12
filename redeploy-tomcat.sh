set -v

./gradlew clean build

# Change the line below to the location of Tomcat built from trunk
TOMCAT=~/Projects/tomcat/output/build/

rm -rf $TOMCAT/webapps/spring4-websock*

cp build/libs/spring4-websockets-1.0.war $TOMCAT/webapps/spring4-websockets.war

$TOMCAT/bin/shutdown.sh
sleep 3

$TOMCAT/bin/startup.sh
