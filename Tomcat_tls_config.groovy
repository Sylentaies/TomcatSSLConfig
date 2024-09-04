// @Grab(group='commons-io', module='commons-io', version='2.16.1')
// import org.apache.commons.io.IOUtils
@Grab(group='log4j', module='log4j', version='1.2.17')
import org.apache.log4j.*
import groovy.util.logging.*

import java.util.Properties
import java.io.*
import java.lang.RuntimeException
import java.util.Iterator
import groovy.xml.*
import groovy.yaml.YamlSlurper
// @Grab(group='com.google.guava', module='guava', version='r05')
// import com.google.common.io.CharStreams
@Grab(group='com.jcraft', module='jsch', version='0.1.54')
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session

def jobCode = UUID.randomUUID().toString()
def logger = LogManager.getLogger(this.getClass())
logger.info (jobCode + "-" + "Tomcat TLS Configuration process started")
//Parse config file
def ys = new YamlSlurper()
def config = ys.parse(new File('config.yaml'))
def pw = config.keystorePassword
generateServerKeyStore(pw,logger,jobCode)
generateClientKeyStore(pw,logger,jobCode)

logger.info (jobCode + " - " + "New KeyStore generated with new password")

modifyServerConfig(pw)
logger.info (jobCode + " - " + "server.xml template modified with new password")

for(server: config.servers){
    def session = setupSshSession(server.url,server.port);
    try {
        /*if the serverside configuration need to be perserved,
        the process can pulling the oringinal file could be added,
        instead of modify the local template
        */
        session.connect();
        logger.info (jobCode + " - " + server.url +":"+ server.port +" - "+"Session Connected")
        uploadFiles(session,config.tomcatHome);
        logger.info (jobCode + " - " +server.url+":"+server.port +" - "+"Configfile and keystore file uploaded")
        restratComcat(session,config.tomcatHome)
        logger.info (jobCode + " - " +server.url+":"+server.port +" - "+"Tomcat server restarted")
    //Todo perserve the config file in to deployment repo.
    } catch (RuntimeException e) {
        logger.err(e.getMessage())
        throw new RuntimeException(e)
    } finally {
        session.disconnect();
    }
}

void uploadFiles(Session session,String tomcatHome){
    ChannelSftp sftp = (ChannelSftp)session.openChannel('sftp');
    sftp.connect();
    def sessionsFile = new File('server.xml');
    sftp.put('server.xml',"$tomcatHome/conf/");
    sftp.put('serverKeystore.jks',"$tomcatHome/conf/");
    sftp.disconnect();

}
//Modified the original server.xml and enable the TLS settings.
//here only modified one attribute as the keystore renewal process with new password
void modifyServerConfig(String ksPassword){
    def xmlTemplateFile = "server_template.xml"
    def xmlFile = "server.xml"
    def xml = new XmlParser().parse(xmlTemplateFile);
    def it = xml.Service.Connector.iterator();
    def flag = true;
    while(it.hasNext()){
        def connectorEle= it.next();
        // println connectorEle;
        if (connectorEle.@port.equals('8443')){
            connectorEle.SSLHostConfig.Certificate.@certificateKeystorePassword=ksPassword;
            flag = false;
            break;
        }
    }
    if (flag){
        logger.error(jobCode + "-" + "No Connector attribute file with 8443 port, please check the template xml")
        throw new Exception("No Connector attribute file with 8443 port, please check the template xml");
    }
    new XmlNodePrinter(new PrintWriter(new FileWriter(xmlFile))).print(xml)
}

void generateServerKeyStore(String ksPassword,Logger logger,String jobCode){
    newKeyStore(ksPassword,logger,jobCode,"serverKeystore.jks","localhost","tomcat")
}

void generateClientKeyStore(String ksPassword,Logger logger,String jobCode){
    newKeyStore(ksPassword,logger,jobCode,"clientKeystore.jks","client.com","client")
    createClientTrustore(ksPassword,logger,jobCode,
                    "serverKeystore.jks","clientTruststore.jks",
                    "tomcat")
}

void newKeyStore(String ksPassword,Logger logger,String jobCode,String keystoreName,String cnName,String alias){
    new File(keystoreName).delete();
    def cmd=
    "keytool -genkey -keystore $keystoreName"+
    " -alias $alias -keyalg rsa -storepass $ksPassword "+
    "-dname \"CN=$cnName,OU=dev,O=sas,L=cary,ST=nc,C=us\"";
    runcommand(cmd)
}

void createClientTrustore(String ksPassword,Logger logger,String jobCode,
                    String serverKeystore,String clientTruststoreName,
                    String alias){
    new File(clientTruststoreName).delete();
    def exportCmd=
    "keytool -export -alias $alias -keystore $serverKeystore "+
    "-storepass $ksPassword -keypass $ksPassword -file server.cer"
    runcommand(exportCmd)
    def importCmd=
    "keytool -importcert -alias $alias -keystore $clientTruststoreName "+
    "-storepass $ksPassword -keypass $ksPassword -file server.cer -noprompt"
    runcommand(importCmd)
                    }

void runcommand(String cmd){
    def proc=cmd.execute();
    def result = new StringBuilder()
    def error = new StringBuilder()
    proc.consumeProcessOutput(error,result);
    proc.waitForOrKill(1000);
    // println(error);
    if (!error.toString().equals("")){
        logger.error(jobCode + "-" + error.toString());
        throw new RuntimeException(error.toString());
    }
}

boolean restratComcat(Session session,String tomcatHome) {
    ChannelExec channel = (ChannelExec) session.openChannel("exec");
    String command ="$tomcatHome/bin/catalina.sh stop;" +
    "$tomcatHome/bin/catalina.sh start";
    channel.setCommand('. ~/.bash_aliases;'+command);
    channel.setInputStream(null);
    InputStream output = channel.getInputStream();
    channel.connect();
    // String result = CharStreams.toString(new InputStreamReader(output));
    channel.disconnect();
    return true
}

private static Session setupSshSession(String host,int port) {
    Session session = new JSch().getSession('root', host, port);
    session.setPassword('Demo');
    session.setConfig("StrictHostKeyChecking", "no"); // disable check for RSA key
    return session;
}
