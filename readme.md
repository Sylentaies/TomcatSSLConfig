# Tomcat SSL config 

## Prerequisite 

### Running Environment
- Docker(For testing localy)
- Java 11 and Groovy 4 or up 

### Tomcat instance deploying 

Run the script `build_run.bat` under `docker_env` folder.
It will build the image and deploy 2 tomcat running instance with with ssh connection 
and default http listnener (instance 1: localhost:8080 ;instance 2:localhost:8081 )

## Run the config script

### Tomcat_tls_config.groovy

#### Run `groovy Tomcat_tls_config.groovy` 
Run the groovy script under TomcatSSLConfig folder, it will 
 1. Create keystore file for server and testing client 
 2. Replace the password in the `web.xml` template with the configuration file (`config.yaml`) setting
 3. Push the gegerated key store file and generated `web.xml` file to the tomcat server 
 4. Restart the Tomcat instance and update the configuration 

## Run the test script

### Run `groovy Tomcat_tls_config_test.groovy`
Run the groovy script under main folder, it will make call for both http and https port to make sure the connection established with the correct setting.  
The HTTPS call will use the turst store file genreated from server side certificate to make the HTTPS connectionthe test case expect the 200 response code from the response indicate the connection is established and pass the test case.

