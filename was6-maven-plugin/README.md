# websphere-maven-plugin

Maven plugin for Websphere application server tasks.
Forked from was6-maven-plugin 1.2.2 on codehaus http://svn.codehaus.org/mojo/tags/was6-maven-plugin-1.2.2/

---

The specific goals :
* [ejbdeploy](#generating-stub-code-for-an-ejb)
* [installApps](#install-application)

For the others goals see : [codehaus](http://mojo.codehaus.org/was6-maven-plugin/plugin-info.html) 

---

## Generating stub code for an EJB

With genEjbClientJar parameter you will generate stub-code and put the stub-code for EjbClient in a specific jar.

You will get two jar 
* XxxxxEjb.jar > Contain all the file
* XxxxxEjbClient.jar > Contain only the stub-code for EbjClient.

```xml
				<plugin>
					<groupId>org.codehaus.mojo</groupId>
					<artifactId>was6-maven-plugin</artifactId>
					<version>1.3.0-SNAPSHOT</version>
					<executions>
						<execution>
							<id>was-ejbdeploy</id>
							<phase>integration-test</phase>
							<goals>
								<goal>ejbdeploy</goal>
							</goals>
							<configuration>
								<wasHome>${was61.home}</wasHome>
								<verbose>true</verbose>
								<genEjbClientJar>true</genEjbClientJar>
								<ejbClientIncludes>com/ibm/websphere/**,fr/canalplus/**,org/omg/stub/**</ejbClientIncludes>
								<ejbClientExcludes>**/_EJS*.class,**/EJS*.class</ejbClientExcludes>
							</configuration>
						</execution>
					</executions>
				</plugin>
```

## Install application

For install several applications on one cluster or on several servers

```xml
			<plugin>
				<groupId>org.codehaus.mojo</groupId>
				<artifactId>was6-maven-plugin</artifactId>
                <version>1.3.0-SNAPSHOT</version>
                <executions>
                    <execution>
                        <id>integration-test</id>
                        <phase>integration-test</phase>
                        <goals>
                            <goal>installApps</goal>
                        </goals>
                    </execution>
                </executions>
				<configuration>
                    <wasHome>${was.home}</wasHome>
                    <host>${was.hostname}</host>
                    <username>${was.username}</username>
                    <user>${was.username}</user>
                    <password>${was.password}</password>
                    <conntype>${was.conntype}</conntype>
                    <port>${was.port}</port>
                    <verbose>${debug.active}</verbose>
					<!-- For install application on servers. Use ; for separator (server1;server2) -->
                    <targetServers>${was.server.cmx}</targetServers>
					<!-- For install application on cluster. -->
					<targetCluster>${was.cluster.mx}</targetCluster>
					<ears>
						<ear>
							<appName>AppName</appName>
							<earFile>${path}/AppName.ear</earFile>
							<!-- Enable or disable the start-auto of the application -->
							<!-- DISABLE : disable the start-auto. -->
							<!-- ENABLE : enable the start-auto. -->
							<!-- CURRENT_STATE : In case that the application is already installed it will take the existing configuration. -->
							<startAuto>CURRENT_STATE</startAuto>
							<!-- Start or don't start the application after its installation. -->
							<!-- START : Start the application after its installation. -->
							<!-- STOP : Don't start the application after its installation. -->
							<!-- CURRENT_STATE : In case that the application is already installed it will start the application if it was started. -->
							<startAfterInstall>CURRENT_STATE</startAfterInstall>
						</ear>
						<ear>
							<appName>AppName2</appName>
							<earFile>${path}/AppName2.ear</earFile>
							<startAuto>CURRENT_STATE</startAuto>
							<startAfterInstall>CURRENT_STATE</startAfterInstall>
						</ear>
					</ears>
				</configuration>
			</plugin>
```
