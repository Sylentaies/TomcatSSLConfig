# Tomcat SSL config 

## Prerequisite 

### Running Environment
- Docker(For testing localy)
- Java 11 and Groovy 4 or up 

### Tomcat instance deploying 

Run the script build_run.bat under docker_env folder.
It will build the image and deploy 2 tomcat running instance with with ssh connection 
and default http listnener (instance 1: localhost:8080 ;instance 2:localhost:8081 )

## Run the config script

### Tomcat_tls_config.groovy

#### Run `groovy Tomcat_tls_config.groovy` 
Run the groovy script under TomcatSSLConfig folder, it will 
 1. Create keystore file for server and testing client 
 2. Replace the password in the webxml template with the  configuration file(config.yaml)
 3. Push the gegerated key store file and webxml file to the tomcat server 
 4. Restart the Tomcat running instance and update the running instance configuration 

## Run the test script

### Run `groovy Tomcat_tls_config_test.groovy`
Run the groovy script under TomcatSSLConfig folder, it will make call for both http and https port to make sure the connection established.  
The HTTPS call will use the exported server side certificate genreated turst store file to make the https connectionthe test case expect the 200 response code from the response indicate the connection is established and pass the test case.

