@Grab(group='org.spockframework', module='spock-core', version='2.4-M4-groovy-4.0', scope='test')
import spock.lang.*
import groovy.yaml.YamlSlurper

import java.util.Properties

class TomcatTLSconfigTesting extends Specification {
    def ys = new YamlSlurper()
    def config = ys.parse(new File('config.yaml'))
    def servers = config.servers
    def "http Connection"() {
    given:
        def resultsCode=new ArrayList();
    when:
        for(server:servers){
            def get = new URL("http://"+server.url+":"+server.httpPort).openConnection();
            def getRC = get.getResponseCode();
            resultsCode.add(getRC);
        }
    then:
        for(code:resultsCode){
            assert code == 200;
        }
    }
    def "https Connection"() {
    given:
        
        Properties systemProps = System.getProperties()
        systemProps.put("javax.net.ssl.keyStore","./clientKeystore.jks")
        systemProps.put("javax.net.ssl.keyStorePassword",config.keystorePassword)
        // systemProps.put("ssl.KeyManagerFactory.algorithm","RSA")
        systemProps.put("javax.net.ssl.trustStore", "./clientTruststore.jks")
        systemProps.put("javax.net.ssl.trustStorePassword",config.keystorePassword)
        // systemProps.put("ssl.TrustManagerFactory.algorithm","RSA")
        // systemProps.put("ssl.TrustManagerFactory.algorithm","RSA")
        System.setProperties(systemProps);
        def resultsCode=new ArrayList();
    when:
        for(server:servers){
            def get = new URL("https://"+server.url+":"+server.httpsPort).openConnection();
            // get.requestMethod ='GET'
            def getRC = get.getResponseCode();
            resultsCode.add(getRC);
        }
    then:
        for(code:resultsCode){
            assert code == 200;
        }
    }
}
