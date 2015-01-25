# websphere-maven-plugin

Maven plugin for Websphere application server tasks.
Forked from was6-maven-plugin 1.2.2 on codehaus http://svn.codehaus.org/mojo/tags/was6-maven-plugin-1.2.2/

## Install application

For install several applications on one cluster or on one or several servers

```xml
			<plugin>
				<groupId>org.codehaus.mojo</groupId>
				<artifactId>was6-maven-plugin</artifactId>
                <version>1.1.13-SNAPSHOT</version>
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
