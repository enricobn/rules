SBT_OPTS="-Xms512M -Xmx1536M -Xss1M -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=512M"
if [ -z ${JAVA_HOME+x} ]; then CMD="java"; else CMD=$JAVA_HOME"/bin/java"; fi
echo "Java command: "$CMD
$CMD $SBT_OPTS -jar `dirname $0`/sbt-launch.jar "$@"
