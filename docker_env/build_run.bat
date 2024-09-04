docker stop tomcat_test
docker stop tomcat_test1
docker rm tomcat_test
docker image rm tomcat
docker build -t tomcat .
docker run -d -p 8080:8080 -p 22:22 -p 8443:8443 --name tomcat_test tomcat 
docker run -d -p 8081:8080 -p 23:22 -p 8444:8443 --name tomcat_test2 tomcat 