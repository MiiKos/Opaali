# Running http_adapter as a service in Linux

The script _http_adapter.sh_ can be used for starting/stopping the http\_adapter as a _service_ when running in _Linux_.

The script is based on one from https://stackoverflow.com/questions/11203483/run-a-java-application-as-a-service-on-linux posted by user PbxMan.

You will need to adapt the following settings in the script to your environment. If your system automatically clears the /tmp directory each day, you should store the PID file somewhere else.
```
SERVICE_NAME=http-adapter-8888
PATH_TO_JAR=/opt/http-adapter-8888/SmsServer-0.4.0-beta.jar
PID_PATH_NAME=/tmp/http-adapter-8888-pid
CONFIG_PATH_NAME=/opt/http-adapter-8888/config.txt
```

The script supports the following commands
* __http_adapter.sh start__ _to start http_adapter as a service_
* __http_adapter.sh stop__ _to stop http_adapter_
* __http_adapter.sh restart__ _to restart http_adapter as a service_

(_if the PID file has been deleted then stop and restart will not work; if the process has died and left an orphaned PID file then start will not work until you have deleted the PID file_)
 