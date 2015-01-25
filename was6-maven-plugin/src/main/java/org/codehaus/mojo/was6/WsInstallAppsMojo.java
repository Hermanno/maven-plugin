package org.codehaus.mojo.was6;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.dom4j.Document;

/**
 * Enables you to install an application from a Server or a Cluster.
 * This task uninstall the application if necessary before install a new version. 
 * 
 * @goal installApps
 * @author apenvern
 */
public class WsInstallAppsMojo extends AbstractAppMojo {

	/**
	 * sets maximum size of the memory for the underlying VM.
	 * 
	 * @parameter expression="${was6.jvmMaxMemory}" default-value="256M"
	 */
	private String jvmMaxMemory;

	/**
	 * Optional parameter specifying the name of the server containing the
	 * application you wish to start.
	 * 
	 * @parameter
	 */
	private String targetServer;

	/**
	 * Optional parameter specifying the name of the cluster containing the
	 * application you wish to start.
	 * 
	 * @parameter
	 */
	private String targetCluster;

	/**
	 * EAR archive to deploy.
	 * 
	 * @parameter expression="${was6.ears}"
	 * @required
	 */
	private List<Ear> ears;

	/**
	 * {@inheritDoc}
	 */
	protected void configureBuildScript(Document document) throws MojoExecutionException {
		if (isClusterMode()) {
		} else {
			getLog().info("Update application on cluster : " + targetCluster);
			getLog().info("Update application on server : " + targetServer);
		}
		File script = new File(getWorkingDirectory(), "WsInstallApps." + (System.currentTimeMillis() / 1000) + ".py");
		try {
			BufferedWriter bfWriter = new BufferedWriter(new FileWriter(script));

			final StringBuilder scritpToUninstallApps = new StringBuilder();
			final StringBuilder scritpToInstallApps = new StringBuilder();
			final StringBuilder scritpToEnableStartAutoApps = new StringBuilder();
			final StringBuilder scritpToStartApps = new StringBuilder();

			for (final Ear ear : ears) {
				scritpToUninstallApps.append(getUninstallAppScript(ear.getAppName()));
				scritpToInstallApps.append(getInstallAppScript(ear));
				scritpToEnableStartAutoApps.append(getAppStartAutoScript(ear.getAppName(),ear.isStartAuto()));
				if(ear.isStartAfterInstall()){
					scritpToStartApps.append(getStartApplScript(ear.getAppName()));
				}
			}

			// Uninstall old version if exist
			bfWriter.append(scritpToUninstallApps);
			bfWriter.append(getEmptyLine());
			bfWriter.append(getSaveScript());
			bfWriter.append(getEmptyLine());

			// Install new version
			bfWriter.append(scritpToInstallApps);
			bfWriter.append(getEmptyLine());
			bfWriter.append(getSaveScript());
			bfWriter.append(getEmptyLine());

			// Enable start auto if needed
			bfWriter.append(scritpToEnableStartAutoApps);
			bfWriter.append(getEmptyLine());
			bfWriter.append(getSaveScript());
			bfWriter.append(getEmptyLine());

			// Sync node
			bfWriter.append(getSyncNode());
			bfWriter.append(getEmptyLine());
			bfWriter.append(getSaveScript());
			bfWriter.append(getEmptyLine());

			// Start new version
			bfWriter.append(scritpToStartApps);
			bfWriter.append(getEmptyLine());
			bfWriter.append(getSaveScript());
			bfWriter.append(getEmptyLine());
			bfWriter.close();

		} catch (IOException e) {
			throw new MojoExecutionException("Error on update task : ", e);
		}

		configureBuildScript(document);
		
		super.configureTaskAttribute(document, "profileName", null);
		super.configureTaskAttribute(document, "profile", null);
		
		super.configureTaskAttribute(document, "lang", "jython");
		super.configureTaskAttribute(document, "properties", null);
		super.configureTaskAttribute(document, "jvmMaxMemory", jvmMaxMemory);
		super.configureTaskAttribute(document, "command", null);
		super.configureTaskAttribute(document, "script", "\"" + script + "\"");
	}

	/**
	 * Build script to enable/disable the Auto-Start for application.
	 * @param pAppNameOnWas
	 *            the name of the application on websphere.
	 * @param pStartAutoEnable true to enable the Auto-Start.
	 * @return the script to enable/disable the Auto-Start for application.
	 */
	private StringBuilder getAppStartAutoScript(final String pAppNameOnWas, final boolean pStartAutoEnable) {
		final StringBuilder strBuilder = new StringBuilder();
		getLog().info("Enable the Auto-Start for application " + pAppNameOnWas);
		strBuilder.append("print 'Enable the Auto-Start for application " + pAppNameOnWas + " ' \n");
		strBuilder.append("deployments_ID = AdminConfig.getid('/Deployment:" + pAppNameOnWas + "/')\n");
		strBuilder.append("deploymentObject = AdminConfig.showAttribute(deployments_ID, 'deployedObject')\n");
		strBuilder.append("targetMapp = AdminConfig.showAttribute(deploymentObject, 'targetMappings')\n");
		strBuilder.append("targetMapp = targetMapp[1:len(targetMapp)-1].split(\" \")\n");
		strBuilder.append("for aTarget in targetMapp:\n");
		strBuilder.append("    AdminConfig.modify(aTarget, [['enable', '"+pStartAutoEnable+"']])\n");
		strBuilder.append("print 'Auto-Start enable for application " + pAppNameOnWas + " '\n");
		return strBuilder;
	}

	/**
	 * Build script to uninstall an application if she is installed.
	 * 
	 * @param pAppNameOnWas
	 *            the name of the application on websphere.
	 * @return the script for uninstall an application.
	 */
	private StringBuilder getUninstallAppScript(final String pAppNameOnWas) {
		final StringBuilder strBuilder = new StringBuilder();
		getLog().info("Uninstallation of application : " + pAppNameOnWas + " only if necessary.");
		strBuilder.append("print 'Check if the application " + pAppNameOnWas + " is installed ' \n");
		strBuilder.append("deployments_ID = AdminConfig.getid('/Deployment:" + pAppNameOnWas + "/')\n");
		strBuilder.append("if(deployments_ID==\"\"):\n");
		strBuilder.append("    isInstalled = 'false' \n");
		strBuilder.append("else:\n");
		strBuilder.append("    isInstalled = 'true' \n");
		strBuilder.append("print 'The application " + pAppNameOnWas + " is installed : ' + isInstalled \n");
		strBuilder.append("\n");
		strBuilder.append("if(isInstalled=='true'):\n");
		strBuilder.append("    print 'Uninstallation of application ... ' \n");
		strBuilder.append("    AdminApp.uninstall('" + pAppNameOnWas + "') \n");
		strBuilder.append("    print 'The application was successefully uninstall' \n");
		return strBuilder;
	}

	/**
	 * Build script to install an application on a single server or a cluster.
	 * 
	 * @param pEar
	 *            the ear to install.
	 * @return the script to install an application on a single server or a
	 *         cluster.
	 * @see http 
	 *      ://pic.dhe.ibm.com/infocenter/wasinfo/v6r0/index.jsp?topic=%2Fcom
	 *      .ibm.websphere.express.doc%2Finfo%2Fexp%2Fae%2Frxml_taskoptions.html
	 */
	private StringBuilder getInstallAppScript(final Ear pEar) throws IOException, MojoExecutionException {
		final StringBuilder strBuilder = new StringBuilder();
		getLog().info("Installation of " + pEar.getAppName());
		strBuilder.append("print 'Installation of application : " + pEar.getAppName() + "' \n");
		String destinationInfo;
		if (isClusterMode()) {
			destinationInfo = " -cluster " + targetCluster;
		} else {
			strBuilder.append("serverObjName = AdminControl.completeObjectName('WebSphere:type=Server,process="+targetServer+",*') \n");
			strBuilder.append("nodeName = AdminControl.getAttribute(serverObjName, 'nodeName') \n");
			destinationInfo = " -server " + targetServer + " -node ' + nodeName + '";
		}
		final String options = "  -createMBeansForResources -noreloadEnabled -nodeployws -validateinstall warn -processEmbeddedConfig -noallowDispatchRemoteInclude -usedefaultbindings";
		final String commande = "AdminApp.install('" + getEarPath(pEar.getEarFile())
				+ "', '[-nopreCompileJSPs -distributeApp -nouseMetaDataFromBinary -nodeployejb -appname "
				+ pEar.getAppName() + options + destinationInfo + "]') \n";
		getLog().info("Command of install : " + commande);
		strBuilder.append(commande);
		strBuilder.append("print 'The application was successefully install ' \n");
		return strBuilder;
	}

	/**
	 * Build EAR file path for installation
	 * 
	 * @param file
	 *            EAR file
	 * @return EAR file completed path
	 * @throws MojoExecutionException
	 *             EAR file not found
	 * @see IBM Java Ant tasks source code
	 */
	protected static String getEarPath(final File file) throws MojoExecutionException {
		String earPath = null;
		if (file != null) {
			earPath = file.getAbsolutePath();
			earPath = earPath.replace('\\', '/');
			if (earPath.indexOf(" ") != -1)
				earPath = "\"" + earPath + "\"";
		} else {
			throw new MojoExecutionException("EarFile doesn't exist");
		}
		return earPath;
	}

	/**
	 * Build script to synchronize nodes.
	 * @return the script to synchronize nodes.
	 */
	private StringBuilder getSyncNode() throws IOException {
		final StringBuilder strBuilder = new StringBuilder();
		if(isClusterMode()){
			getLog().info("Node synchronization for cluster " + targetCluster);
			// Get cluster members
			strBuilder.append("print 'Node synchronization for cluster ...' \n");
			strBuilder.append("nodeServerPairs = [] \n");
			strBuilder.append("cluster_id = AdminConfig.getid('/ServerCluster:" + targetCluster + "/' ) \n");
			strBuilder.append("print 'Cluster Id : ' + cluster_id \n");
			strBuilder.append("strmembers = AdminConfig.list('ClusterMember', cluster_id ) \n");
			strBuilder.append("members=[] \n");
			strBuilder.append("if (len(strmembers)>0 and strmembers[0]=='[' and strmembers[-1]==']'): \n");
			strBuilder.append("    strmembers = strmembers[1:-1] \n");
			strBuilder.append("    tmpList = strmembers.split(\" \") \n");
			strBuilder.append("else: \n");
			strBuilder.append("    tmpList = strmembers.split(\"\\n\") #splits for Windows or Linux \n");
			strBuilder.append("for item in tmpList: \n");
			strBuilder.append("    item = item.rstrip();       #removes any Windows '\\r' \n");
			strBuilder.append("    if (len(item)>0): \n");
			strBuilder.append("       members.append(item) \n");
			strBuilder.append("       \n");
			strBuilder.append("print \n");
			// Start on each member
			strBuilder.append("for member in members: \n");
			strBuilder.append("  nodeName = AdminConfig.showAttribute(member, 'nodeName' ) \n");
			strBuilder.append("  print 'Synchronization of node : ' + nodeName \n");
			strBuilder.append("  sync1 = AdminControl.completeObjectName('type=NodeSync,node='+nodeName+',*') \n");
			strBuilder.append("  AdminControl.invoke(sync1, 'sync') \n");
			strBuilder.append("print 'Node synchronization for cluster SUCESS ' \n");
		}else{
			getLog().info("Node synchronization for server " + targetServer);
			strBuilder.append("print 'Node synchronization for server ... ' \n");
			strBuilder.append("serverObjName = AdminControl.completeObjectName('WebSphere:type=Server,process="+targetServer+",*') \n");
			strBuilder.append("processType = AdminControl.getAttribute(serverObjName, 'processType') \n");
			strBuilder.append("if (processType=='DeploymentManager'): \n");
			strBuilder.append("  nodeName = AdminControl.getAttribute(serverObjName, 'nodeName') \n");
			strBuilder.append("  sync1 = AdminControl.completeObjectName('type=NodeSync,node='+nodeName+',*') \n");
			strBuilder.append("  AdminControl.invoke(sync1, 'sync') \n");
			strBuilder.append("  print 'Node synchronization for server SUCESS ' \n");
			strBuilder.append("else: \n");
			strBuilder.append("  print 'Node synchronization not required for devellopement server.' \n");
		}
		return strBuilder;
	}

	/**
	 * Build script to start application on a single server or on a cluster.
	 * 
	 * @return The script to start application on a single server or on a
	 *         cluster.
	 */
	private StringBuilder getStartApplScript(final String pAppNameOnWas) {
		if (isClusterMode()) {
			return getStartAppForClusterScript(pAppNameOnWas);
		} else {
			return getStartAppForServerScript(pAppNameOnWas);
		}
	}

	/**
	 * Build script to start application on a single server.
	 * 
	 * @return The script to start application on a single server.
	 */
	private StringBuilder getStartAppForServerScript(final String pAppNameOnWas) {
		final StringBuilder strBuilder = new StringBuilder();
		getLog().info("Start application on server : " + targetServer);
		strBuilder.append("print 'Start application on cluster server ...' \n");
		strBuilder.append("serverObjName = AdminControl.completeObjectName('WebSphere:type=Server,process="+targetServer+",*') \n");
		strBuilder.append("nodeName = AdminControl.getAttribute(serverObjName, 'nodeName') \n");
		strBuilder.append("appManager = AdminControl.queryNames('WebSphere:*,node='+nodeName+',type=ApplicationManager,process=" + targetServer + "') \n");
		strBuilder.append("AdminControl.invoke(appManager, 'startApplication', '" + pAppNameOnWas + "') \n");
		strBuilder.append("print 'Start application on cluster server : OK' \n");
		return strBuilder;
	}

	/**
	 * Build script to start application on a cluster.
	 * 
	 * @param pAppNameOnWas
	 *            the name of the application on websphere to uninstall.
	 * @return the script to start application on a cluster.
	 */
	private StringBuilder getStartAppForClusterScript(final String pAppNameOnWas) {
		final StringBuilder strBuilder = new StringBuilder();
		getLog().info("Start application on cluster : " + targetCluster);
		// Get cluster members
		strBuilder.append("print 'Start application on cluster ' \n");
		strBuilder.append("nodeServerPairs = [] \n");
		strBuilder.append("cluster_id = AdminConfig.getid('/ServerCluster:" + targetCluster + "/' ) \n");
		strBuilder.append("print 'Cluster Id : ' + cluster_id \n");
		strBuilder.append("strmembers = AdminConfig.list('ClusterMember', cluster_id ) \n");
		strBuilder.append("members=[] \n");
		strBuilder.append("if (len(strmembers)>0 and strmembers[0]=='[' and strmembers[-1]==']'): \n");
		strBuilder.append("    strmembers = strmembers[1:-1] \n");
		strBuilder.append("    tmpList = strmembers.split(\" \") \n");
		strBuilder.append("else: \n");
		strBuilder.append("    tmpList = strmembers.split(\"\\n\") #splits for Windows or Linux \n");
		strBuilder.append("for item in tmpList: \n");
		strBuilder.append("    item = item.rstrip();       #removes any Windows \"\\r\" \n");
		strBuilder.append("    if (len(item)>0): \n");
		strBuilder.append("       members.append(item) \n");
		strBuilder.append("       \n");
		strBuilder.append("print \n");
		// Start on each member
		strBuilder.append("for member in members: \n");
		strBuilder.append("  node = AdminConfig.showAttribute(member, 'nodeName' ) \n");
		strBuilder.append("  server = AdminConfig.showAttribute(member, 'memberName' ) \n");
		strBuilder.append("  print 'Node = ' + node + ' server = ' + server \n");
		strBuilder
				.append("  am = AdminControl.queryNames('WebSphere:*,type=ApplicationManager,process=' + server + ',node=' + node) \n");
		strBuilder.append("  AdminControl.invoke(am,'startApplication','" + pAppNameOnWas + "') \n");
		strBuilder.append("  print 'Start " + pAppNameOnWas + " OK on ' + server \n");
		strBuilder.append("print 'Start OK on cluster ' \n");
		return strBuilder;
	}

	/**
	 * @return the script part to save our configuration to master environment.
	 */
	private static String getSaveScript() throws IOException {
		return "AdminConfig.save() \n";
	}

	/**
	 * @return the empty line.
	 */
	private static String getEmptyLine() {
		return "\n";
	}

	/**
	 * @return if operation must be run on a cluster
	 */
	private boolean isClusterMode() {
		return targetCluster != null && !targetCluster.trim().equals("");
	}

	/**
	 * {@inheritDoc}
	 */
	protected String getTaskName() {
		return "wsAdmin";
	}
}
