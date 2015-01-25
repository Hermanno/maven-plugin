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
			getLog().info("Update application on cluster : " + targetCluster);
		} else {
			getLog().info("Update application on server : " + targetServer);
		}
		File script = new File(getWorkingDirectory(), "WsInstallApps." + (System.currentTimeMillis() / 1000) + ".py");
		try {
			BufferedWriter bfWriter = new BufferedWriter(new FileWriter(script));

			final StringBuilder scriptToGetServerClusterInfo = new StringBuilder();
			final StringBuilder scriptToGetAppInfo = new StringBuilder();
			final StringBuilder scriptToPreviousInstallationInfo = new StringBuilder();
			final StringBuilder scritpToUninstallApps = new StringBuilder();
			final StringBuilder scritpToInstallApps = new StringBuilder();
			final StringBuilder scritpToEnableStartAutoApps = new StringBuilder();
			final StringBuilder scritpToStartApps = new StringBuilder();
			
			scriptToGetServerClusterInfo.append(getServerClusterInfoScript());
			
			for (int i = 0; i < ears.size(); i++) {
				final Ear ear = ears.get(i);
				ear.setLocalId(i);
				scriptToGetAppInfo.append(getAppInfoScript(ear));
				scriptToPreviousInstallationInfo.append(getPreviousInstallationInfoScript(ear));
				scritpToUninstallApps.append(getUninstallAppScript(ear));
				scritpToInstallApps.append(getInstallAppScript(ear));
				scritpToEnableStartAutoApps.append(getAppStartAutoScript(ear));
				if("START".equals(ear.getStartAfterInstall()) || "CURRENT_STATE".equals(ear.getStartAfterInstall())){
					scritpToStartApps.append(getStartApplScript(ear));
				}
			}
			
			// Set the informations of the server or the cluster
			bfWriter.append(scriptToGetServerClusterInfo);
			bfWriter.append(getSaveScript());
			
			// Set the informations of the application
			bfWriter.append(scriptToGetAppInfo);
			bfWriter.append(getSaveScript());
			
			// Set the informations of the application
			bfWriter.append(scriptToPreviousInstallationInfo);
			bfWriter.append(getSaveScript());
			
			// Uninstall old version if exist
			bfWriter.append(scritpToUninstallApps);
			bfWriter.append(getSaveScript());

			// Install new version
			bfWriter.append(scritpToInstallApps);
			bfWriter.append(getSaveScript());

			// Enable start auto if needed
			bfWriter.append(scritpToEnableStartAutoApps);
			bfWriter.append(getSaveScript());

			// Sync node
			bfWriter.append(getSyncNode());
			bfWriter.append(getSaveScript());

			// Start new version
			bfWriter.append(scritpToStartApps);
			bfWriter.append(getSaveScript());
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
	 * Build script to get the information for the server or the cluster.
	 * @param pEar
	 *            the ear to install.
	 * @return the script to get the information for the server or the cluster.
	 */
	private Object getServerClusterInfoScript() {
		getLog().info("Build the script to get the information for the server or the cluster . ");
		final StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("CLUSTER_ID = \"\" \n");
		strBuilder.append("MEMBERS=[] \n");
		strBuilder.append("SERVER_OBJECT_NAME = \"\" \n");
		strBuilder.append("\n");
		
		//The info of the cluster or the server 
		if(isClusterMode()){
			strBuilder.append("CLUSTER_ID = AdminConfig.getid('/ServerCluster:" + targetCluster + "/' ) \n");
			strBuilder.append("strmembers = AdminConfig.list('ClusterMember', CLUSTER_ID ) \n");
			strBuilder.append("if (len(strmembers)>0 and strmembers[0]=='[' and strmembers[-1]==']'): \n");
			strBuilder.append("    strmembers = strmembers[1:-1] \n");
			strBuilder.append("    tmpList = strmembers.split(\" \") \n");
			strBuilder.append("else: \n");
			strBuilder.append("    tmpList = strmembers.split(\"\\n\") #splits for Windows or Linux \n");
			strBuilder.append("for item in tmpList: \n");
			strBuilder.append("    item = item.rstrip();       #removes any Windows '\\r' \n");
			strBuilder.append("    if (len(item)>0): \n");
			strBuilder.append("       MEMBERS.append(item) \n");
			strBuilder.append("print '########################################################################'\n");
			strBuilder.append("print 'The Cluster Id : ' + CLUSTER_ID \n");
			strBuilder.append("print '########################################################################'\n");
		}else{
			strBuilder.append("SERVER_OBJECT_NAME = AdminControl.completeObjectName('WebSphere:type=Server,process="+targetServer+",*') \n");
			strBuilder.append("print '########################################################################'\n");
			strBuilder.append("print 'The Server Object Name : ' + SERVER_OBJECT_NAME \n");
			strBuilder.append("print '########################################################################'\n");
		}
		return strBuilder;
	}

	/**
	 * Build script to get the information for the application on the server.
	 * @param pEar
	 *            the ear to install.
	 * @return the script to get the information for the application on the server.
	 */
	private StringBuilder getAppInfoScript(final Ear pEar) {
		getLog().info("Build the script to get the information for the application " + pEar.getAppName() + " on the server. ");
		final StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("DEPLOYMENT_ID_"+pEar.getLocalId()+" = \"\" \n");
		strBuilder.append("TARGET_MAP_"+pEar.getLocalId()+" =  \"\" \n");
		strBuilder.append("IS_INSTALLED_"+pEar.getLocalId()+" = 'false' \n");
		strBuilder.append("\n");
		
		//The App's info
		strBuilder.append("DEPLOYMENT_ID_"+pEar.getLocalId()+" = AdminConfig.getid('/Deployment:" + pEar.getAppName() + "/')\n");
		strBuilder.append("if(DEPLOYMENT_ID_"+pEar.getLocalId()+"!=\"\"):\n");
		strBuilder.append("    deployment_object = AdminConfig.showAttribute(DEPLOYMENT_ID_"+pEar.getLocalId()+", 'deployedObject')\n");
		strBuilder.append("    target_mappings = AdminConfig.showAttribute(deployment_object, 'targetMappings')\n");
		strBuilder.append("    TARGET_MAP_"+pEar.getLocalId()+" = target_mappings[1:len(target_mappings)-1].split(\" \")\n");
		strBuilder.append("    IS_INSTALLED_"+pEar.getLocalId()+" = 'true' \n");
		
		strBuilder.append("print '########################################################################'\n");
		strBuilder.append("print 'The application " + pEar.getAppName() + " is already installed : ' + IS_INSTALLED_"+pEar.getLocalId()+" \n");
		strBuilder.append("print 'The deployment ID : ' + DEPLOYMENT_ID_"+pEar.getLocalId()+" \n");
		strBuilder.append("print '########################################################################'\n");
		return strBuilder;
	}
	
	/**
	 * Build script to get the information for the application on the server. The application was already installed so we get the information of the old install. 
	 * @param pEar
	 *            the ear to install.
	 * @return the script to get the information for the application on the server.
	 */
	private StringBuilder getPreviousInstallationInfoScript(final Ear pEar) {
		getLog().info("Build the script to get the information for the previous installation of the application " + pEar.getAppName() + " on the server. ");
		final StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("IS_STARTED_"+pEar.getLocalId()+" = 'false' \n");
		strBuilder.append("IS_START_AUTO_ENABLE_"+pEar.getLocalId()+" = 'false' \n");
		strBuilder.append("\n");
		
		//The App's info
		strBuilder.append("if(DEPLOYMENT_ID_"+pEar.getLocalId()+"!=\"\"):\n");
		strBuilder.append("    for aTarget in TARGET_MAP_"+pEar.getLocalId()+":\n");
		strBuilder.append("       IS_START_AUTO_ENABLE_"+pEar.getLocalId()+" = AdminConfig.showAttribute(aTarget, 'enable') \n");
		if(isClusterMode()){
			strBuilder.append("    for member in MEMBERS: \n");
			strBuilder.append("        nodeName = AdminConfig.showAttribute(member, 'nodeName' ) \n");
			strBuilder.append("        appID = AdminControl.completeObjectName('type=Application,node='+nodeName+',Server="+targetServer+",name="+pEar.getAppName()+",*') \n" );
			strBuilder.append("        if (len(appID) > 0): \n" );
			strBuilder.append("            IS_STARTED_"+pEar.getLocalId()+" = 'true' \n" );
			strBuilder.append("            break \n" );
		}else{
			strBuilder.append("    nodeName = AdminControl.getAttribute(SERVER_OBJECT_NAME, 'nodeName') \n");
			strBuilder.append("    appID = AdminControl.completeObjectName('type=Application,node='+nodeName+',Server="+targetServer+",name="+pEar.getAppName()+",*') \n" );
			strBuilder.append("    if (len(appID) > 0): \n" );
			strBuilder.append("        IS_STARTED_"+pEar.getLocalId()+" = 'true' \n" );
		}
		
		strBuilder.append("print '########################################################################'\n");
		strBuilder.append("print 'The application " + pEar.getAppName() + " is already installed : ' + IS_INSTALLED_"+pEar.getLocalId()+" \n");
		strBuilder.append("print 'The application " + pEar.getAppName() + " is already started : ' + IS_STARTED_"+pEar.getLocalId()+" \n");
		strBuilder.append("print 'The auto start is already enabled for application " + pEar.getAppName() +" : ' + IS_START_AUTO_ENABLE_"+pEar.getLocalId()+" \n");
		strBuilder.append("print '########################################################################'\n");
		return strBuilder;
	}
	
	/**
	 * Build script to enable/disable the Auto-Start for application.
	 * @param pEar
	 *            the ear to install.
	 * @return the script to enable/disable the Auto-Start for application.
	 */
	private StringBuilder getAppStartAutoScript(final Ear pEar) {
		final StringBuilder strBuilder = new StringBuilder();
		getLog().info("Enable the Auto-Start for application " +  pEar.getAppName());
		strBuilder.append("print 'Enable or disable the Auto-Start for application " +  pEar.getAppName() + " ' \n");
		strBuilder.append("for aTarget in TARGET_MAP_"+pEar.getLocalId()+":\n");
		if("CURRENT_STATE".equals(pEar.getStartAuto())){
			strBuilder.append("    AdminConfig.modify(aTarget, [['enable', IS_START_AUTO_ENABLE_"+pEar.getLocalId()+"]])\n");
			strBuilder.append("    if(IS_START_AUTO_ENABLE_"+pEar.getLocalId()+"=='true'):\n");
			strBuilder.append("        print 'Auto-Start enable for application " +  pEar.getAppName() + " '\n");
			strBuilder.append("    else:\n");
			strBuilder.append("        print 'Auto-Start disable for application " +  pEar.getAppName() + " '\n");
		}else if ("ENABLE".equals(pEar.getStartAuto())){
			strBuilder.append("    AdminConfig.modify(aTarget, [['enable', 'true']])\n");
			strBuilder.append("    print 'Auto-Start enable for application " +  pEar.getAppName() + " '\n");
		}else{
			strBuilder.append("    AdminConfig.modify(aTarget, [['enable', 'false']])\n");
			strBuilder.append("    print 'Auto-Start disable for application " +  pEar.getAppName() + " '\n");
		}
		return strBuilder;
	}
	
	/**
	 * Build script to uninstall an application if she is installed.
	 * 
	 * @param pAppNameOnWas
	 *            the name of the application on websphere.
	 * @return the script for uninstall an application.
	 */
	private StringBuilder getUninstallAppScript(final Ear pEar) {
		final StringBuilder strBuilder = new StringBuilder();
		getLog().info("Uninstallation of application : " + pEar.getAppName() + " only if necessary.");
		strBuilder.append("if(IS_INSTALLED_"+pEar.getLocalId()+"=='true'):\n");
		strBuilder.append("    print 'Uninstallation of application " +  pEar.getAppName() + " ... ' \n");
		strBuilder.append("    AdminApp.uninstall('" + pEar.getAppName() + "') \n");
		strBuilder.append("    print 'The application " +  pEar.getAppName() + " was successefully uninstall' \n");
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
			strBuilder.append("nodeName = AdminControl.getAttribute(SERVER_OBJECT_NAME, 'nodeName') \n");
			destinationInfo = " -server " + targetServer + " -node ' + nodeName + '";
		}
		final String options = "  -createMBeansForResources -noreloadEnabled -nodeployws -validateinstall warn -processEmbeddedConfig -noallowDispatchRemoteInclude -usedefaultbindings";
		final String commande = "AdminApp.install('" + getEarPath(pEar.getEarFile())
				+ "', '[-nopreCompileJSPs -distributeApp -nouseMetaDataFromBinary -nodeployejb -appname "
				+ pEar.getAppName() + options + destinationInfo + "]') \n";
		getLog().info("Command of install : " + commande);
		strBuilder.append(commande);
		strBuilder.append("print 'The application was successefully install ' \n");
		//Re-initialization the App's info
		strBuilder.append(getAppInfoScript(pEar));
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
			strBuilder.append("for member in MEMBERS: \n");
			strBuilder.append("  nodeName = AdminConfig.showAttribute(member, 'nodeName' ) \n");
			strBuilder.append("  print 'Synchronization of node : ' + nodeName \n");
			strBuilder.append("  sync1 = AdminControl.completeObjectName('type=NodeSync,node='+nodeName+',*') \n");
			strBuilder.append("  AdminControl.invoke(sync1, 'sync') \n");
			strBuilder.append("print 'Node synchronization for cluster SUCESS ' \n");
		}else{
			getLog().info("Node synchronization for server " + targetServer);
			strBuilder.append("print 'Node synchronization for server ... ' \n");
			strBuilder.append("processType = AdminControl.getAttribute(SERVER_OBJECT_NAME, 'processType') \n");
			strBuilder.append("if (processType=='DeploymentManager'): \n");
			strBuilder.append("  nodeName = AdminControl.getAttribute(SERVER_OBJECT_NAME, 'nodeName') \n");
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
	private StringBuilder getStartApplScript(final Ear pEar) {
		if (isClusterMode()) {
			return getStartAppForClusterScript(pEar);
		} else {
			return getStartAppForServerScript(pEar);
		}
	}

	/**
	 * Build script to start application on a single server.
	 * 
	 * @return The script to start application on a single server.
	 */
	private StringBuilder getStartAppForServerScript(final Ear pEar) {
		final StringBuilder strBuilder = new StringBuilder();
		getLog().info("Start application on server : " + targetServer);
		if("CURRENT_STATE".equals(pEar.getStartAfterInstall())){
			strBuilder.append("if(IS_STARTED_"+pEar.getLocalId()+"=='true'):\n");
			strBuilder.append("    print 'Start application on server " +  pEar.getAppName() + " ... '\n");
			strBuilder.append("    nodeName = AdminControl.getAttribute(SERVER_OBJECT_NAME, 'nodeName') \n");
			strBuilder.append("    appManager = AdminControl.queryNames('WebSphere:*,node='+nodeName+',type=ApplicationManager,process=" + targetServer + "') \n");
			strBuilder.append("    AdminControl.invoke(appManager, 'startApplication', '" + pEar.getAppName() + "') \n");
			strBuilder.append("    print 'Start application on server : OK' \n");
		}else {
			strBuilder.append("print 'Start application on server " +  pEar.getAppName() + " ... '\n");
			strBuilder.append("nodeName = AdminControl.getAttribute(SERVER_OBJECT_NAME, 'nodeName') \n");
			strBuilder.append("appManager = AdminControl.queryNames('WebSphere:*,node='+nodeName+',type=ApplicationManager,process=" + targetServer + "') \n");
			strBuilder.append("AdminControl.invoke(appManager, 'startApplication', '" + pEar.getAppName() + "') \n");
			strBuilder.append("print 'Start application on server : OK' \n");
		}
		return strBuilder;
	}

	/**
	 * Build script to start application on a cluster.
	 * 
	 * @param pAppNameOnWas
	 *            the name of the application on websphere to uninstall.
	 * @return the script to start application on a cluster.
	 */
	private StringBuilder getStartAppForClusterScript(final Ear pEar) {
		final StringBuilder strBuilder = new StringBuilder();
		getLog().info("Start application on cluster : " + targetCluster);
		if("CURRENT_STATE".equals(pEar.getStartAfterInstall())){
			strBuilder.append("if(IS_STARTED_"+pEar.getLocalId()+"=='true'):\n");
			strBuilder.append("    for member in MEMBERS: \n");
			strBuilder.append("        node = AdminConfig.showAttribute(member, 'nodeName' ) \n");
			strBuilder.append("        server = AdminConfig.showAttribute(member, 'memberName' ) \n");
			strBuilder.append("        print 'Node = ' + node + ' server = ' + server \n");
			strBuilder.append("        am = AdminControl.queryNames('WebSphere:*,type=ApplicationManager,process=' + server + ',node=' + node) \n");
			strBuilder.append("        AdminControl.invoke(am,'startApplication','" + pEar.getAppName() + "') \n");
			strBuilder.append("        print 'Start " + pEar.getAppName() + " OK on ' + server \n");
			strBuilder.append("    print 'Start OK on cluster ' \n");
		}else {
			strBuilder.append("for member in MEMBERS: \n");
			strBuilder.append("  node = AdminConfig.showAttribute(member, 'nodeName' ) \n");
			strBuilder.append("  server = AdminConfig.showAttribute(member, 'memberName' ) \n");
			strBuilder.append("  print 'Node = ' + node + ' server = ' + server \n");
			strBuilder.append("  am = AdminControl.queryNames('WebSphere:*,type=ApplicationManager,process=' + server + ',node=' + node) \n");
			strBuilder.append("  AdminControl.invoke(am,'startApplication','" + pEar.getAppName() + "') \n");
			strBuilder.append("  print 'Start " + pEar.getAppName() + " OK on ' + server \n");
			strBuilder.append("print 'Start OK on cluster ' \n");
		}
		return strBuilder;
	}

	/**
	 * @return the script part to save our configuration to master environment.
	 */
	private static String getSaveScript() throws IOException {
		return "\nAdminConfig.save() \n\n";
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
