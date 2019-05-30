#!/bin/sh
# based on https://stackoverflow.com/questions/11203483/run-a-java-application-as-a-service-on-linux
#
SERVICE_NAME=http-adapter-8888
PATH_TO_JAR=/opt/http-adapter-8888/SmsServer-0.4.0-beta.jar
PID_PATH_NAME=/tmp/http-adapter-8888-pid
CONFIG_PATH_NAME=/opt/http-adapter-8888/config.txt
case $1 in
    start)
        echo "Starting $SERVICE_NAME ..."
        if [ ! -f $PID_PATH_NAME ]; then
            nohup java -jar $PATH_TO_JAR $CONFIG_PATH_NAME 2>> /dev/null >> /dev/null &
            echo $! > $PID_PATH_NAME
            echo "$SERVICE_NAME started ..."
        else
            echo "$SERVICE_NAME is already running ..."
        fi
    ;;
    stop)
        if [ -f $PID_PATH_NAME ]; then
            PID=$(cat $PID_PATH_NAME);
            echo "$SERVICE_NAME stopping ..."
            kill $PID;
            echo "$SERVICE_NAME stopped ..."
            rm $PID_PATH_NAME
        else
            echo "$SERVICE_NAME is not running ..."
        fi
    ;;
    restart)
        if [ -f $PID_PATH_NAME ]; then
            PID=$(cat $PID_PATH_NAME);
            echo "$SERVICE_NAME stopping ...";
            kill $PID;
            echo "$SERVICE_NAME stopped ...";
            rm $PID_PATH_NAME
            echo "$SERVICE_NAME starting ..."
            nohup java -jar $PATH_TO_JAR $CONFIG_PATH_NAME 2>> /dev/null >> /dev/null &
            echo $! > $PID_PATH_NAME
            echo "$SERVICE_NAME started ..."
        else
            echo "$SERVICE_NAME is not running ..."
        fi
    ;;
	*)
		echo "http_adapter service control"
		echo "usage: $0 start|stop|restart"
	;;
esac 
