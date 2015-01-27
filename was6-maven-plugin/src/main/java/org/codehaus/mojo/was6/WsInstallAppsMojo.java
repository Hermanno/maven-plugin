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
	 * @deprecated use {@link targetServers}
	 * @parameter expression="${was6.targetServer}"
	 */
	private String targetServer;

	/**
	 * Optional parameter specifying the name of the cluster containing the
	 * application you wish to start.
	 * 
	 * @parameter expression="${was6.targetCluster}"
	 */
	private String targetCluster;
	
	/**
	 * Optional parameter specifying the servers where the application will be install for multiple servers user separator ";". 
	 * 
	 * @parameter expression="${was6.targetServers}"
	 */
	private String targetServers;
	
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
			if(targetServer!=null && !"".equals(targetServer) ){
				getLog().warn(" The element \"targetServer\" is deprecated use \"targetServers\".");
				if(targetServers!=null && !"".equals(targetServers)){
					throw new MojoExecutionException("Error you couldn't use targetServer with targetServers");
				}else{
					targetServers = targetServer;
				}
			}
			getLog().info("Update application on servers : " + targetServers.toString());
		}
		File script = new File(getWorkingDirectory(), "WsInstallApps." + (System.currentTimeMillis() / 1000) + ".py");
		try {
			
			script.getParentFile().mkdir();
			script.createNewFile();
			
			BufferedWriter bfWriter = new BufferedWriter(new FileWriter(script));

			final StringBuilder scriptToGetServerClusterInfo = new StringBuilder();
			final StringBuilder scriptToGetAppInfo = new StringBuilder();
			final StringBuilder scritpToStopApps = new StringBuilder();
			final StringBuilder scritpToUninstallApps = new StringBuilder();
			final StringBuilder scritpToInstallApps = new StringBuilder();
			final StringBuilder scritpToEnableStartAutoApps = new StringBuilder();
			final StringBuilder scritpToStartApps = new StringBuilder();
			
			scriptToGetServerClusterInfo.append(getServerClusterInfoScript());
			
			for (int i = 0; i < ears.size(); i++) {
				final Ear ear = ears.get(i);
				ear.setLocalId(i);
				scriptToGetAppInfo.append(getAppInfoScript(ear));
				scritpToStopApps.append(getStopAppScript(ear));
				scritpToUninstallApps.append(getUninstallAppScript(ear));
				scritpToInstallApps.append(getInstallAppScript(ear));
				scritpToEnableStartAutoApps.append(getAppStartAutoScript(ear));
				if("START".equals(ear.getStartAfterInstall()) || "CURRENT_STATE".equals(ear.getStartAfterInstall())){
					scritpToStartApps.append(getStartAppScript(ear));
				}
			}
			
			// Set the informations of the server or the cluster
			bfWriter.append(scriptToGetServerClusterInfo);
			bfWriter.append(getSaveScript());
			
			// Set the informations of the application
			bfWriter.append(scriptToGetAppInfo);
			bfWriter.append(getSaveScript());
			
			// Stop old version if exist and if started
			bfWriter.append(scritpToStopApps);
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
			throw new MojoExecutionException("Error on installApps task : ", e);
		}

		super.configureBuildScript(document);
		
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
		strBuilder.append("import java.lang.System as system \n");
		strBuilder.append("lineSeparator = system.getProperty('line.separator') \n");
		strBuilder.append("SERVERS=[] \n");
		strBuilder.append("serversName=[] \n");
		strBuilder.append("\n");
		
		//The info of the cluster or the server 
		if(isClusterMode()){
			strBuilder.append("strmembers = AdminConfig.list('ClusterMember', AdminConfig.getid('/ServerCluster:" + targetCluster + "/' )) \n");
			strBuilder.append("for item in strmembers.split(lineSeparator): \n");
			strBuilder.append("    if (len(item)>0): \n");
			strBuilder.append("       serversName.append(AdminConfig.showAttribute(item, 'memberName')) \n");
		}else{
			String[] tab = targetServers.split(";");
			for (int i = 0; i < tab.length; i++) {
				strBuilder.append("serversName.append('"+tab[i]+"') \n");
			}
		}
		
		strBuilder.append("for cell in AdminConfig.list('Cell').split(lineSeparator): \n");
		strBuilder.append("	 cname = AdminConfig.showAttribute(cell, 'name') \n");
		strBuilder.append("	 for node in AdminConfig.list('Node', cell).split(lineSeparator): \n");
		strBuilder.append("		nname = AdminConfig.showAttribute(node, 'name') \n");
		strBuilder.append("		for sname in serversName: \n");
		strBuilder.append("			serverId = AdminConfig.getid('/Cell:'+cname+'/Node:'+nname+'/Server:'+sname+'/') \n");
		strBuilder.append("			if (serverId != ''): \n");
		strBuilder.append("				SERVERS.append([cname,nname,sname]) \n");
		strBuilder.append("				print 'Server info Cell:'+cname+' Node:'+nname+' Server:'+sname \n");
		
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
		strBuilder.append("IS_INSTALLED_"+pEar.getLocalId()+" = 'false' \n");
		strBuilder.append("IS_STARTED_"+pEar.getLocalId()+" = 'false' \n" );
		strBuilder.append("IS_START_AUTO_ENABLE_"+pEar.getLocalId()+" = 'false' \n" );
		strBuilder.append("\n");
		
		//The App's info
		strBuilder.append("deployment_id = AdminConfig.getid('/Deployment:" + pEar.getAppName() + "/')\n");
		strBuilder.append("if(deployment_id!=\"\"):\n");
		strBuilder.append("    deployment_object = AdminConfig.showAttribute(deployment_id, 'deployedObject')\n");
		strBuilder.append("    target_mappings = AdminConfig.showAttribute(deployment_object, 'targetMappings')\n");
		strBuilder.append("    target_map = target_mappings[1:len(target_mappings)-1].split(\" \")\n");
		strBuilder.append("    IS_INSTALLED_"+pEar.getLocalId()+" = 'true' \n");
		strBuilder.append("    IS_START_AUTO_ENABLE_"+pEar.getLocalId()+" = AdminConfig.showAttribute(target_map[0], 'enable') \n");
		strBuilder.append("    appID = AdminControl.completeObjectName('type=Application,name="+pEar.getAppName()+",*') \n" );
		strBuilder.append("    if (len(appID) > 0): \n" );
		strBuilder.append("        IS_STARTED_"+pEar.getLocalId()+" = 'true' \n" );
		
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
		strBuilder.append("deployment_id = AdminConfig.getid('/Deployment:" + pEar.getAppName() + "/')\n");
		strBuilder.append("if(deployment_id!=\"\"):\n");
		strBuilder.append("    deployment_object = AdminConfig.showAttribute(deployment_id, 'deployedObject')\n");
		strBuilder.append("    target_mappings = AdminConfig.showAttribute(deployment_object, 'targetMappings')\n");
		strBuilder.append("    target_map = target_mappings[1:len(target_mappings)-1].split(\" \")\n");
		strBuilder.append("    for aTarget in target_map:\n");
		if("CURRENT_STATE".equals(pEar.getStartAuto())){
			strBuilder.append("        AdminConfig.modify(aTarget, [['enable', IS_START_AUTO_ENABLE_"+pEar.getLocalId()+"]])\n");
			strBuilder.append("        if(IS_START_AUTO_ENABLE_"+pEar.getLocalId()+"=='true'):\n");
			strBuilder.append("            print 'Auto-Start enable for application " +  pEar.getAppName() + " '\n");
			strBuilder.append("        else:\n");
			strBuilder.append("            print 'Auto-Start disable for application " +  pEar.getAppName() + " '\n");
		}else if ("ENABLE".equals(pEar.getStartAuto())){
			strBuilder.append("        AdminConfig.modify(aTarget, [['enable', 'true']])\n");
			strBuilder.append("        print 'Auto-Start enable for application " +  pEar.getAppName() + " '\n");
		}else{
			strBuilder.append("        AdminConfig.modify(aTarget, [['enable', 'false']])\n");
			strBuilder.append("        print 'Auto-Start disable for application " +  pEar.getAppName() + " '\n");
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
		if (isClusterMode()) {
			strBuilder.append("destinationInfo = ' -cluster "+targetCluster +"' \n");
		} else {
			strBuilder.append("targetInstall=\"\" \n");
			strBuilder.append("for server in SERVERS: \n");
			strBuilder.append("  cellName = server[0] \n");
			strBuilder.append("  nodeName = server[1] \n");
			strBuilder.append("  serverName = server[2] \n");
			strBuilder.append("  if(targetInstall==\"\"):\n");
			strBuilder.append("    targetInstall=\"WebSphere:cell=\"+cellName+\",node=\"+nodeName+\",server=\"+serverName \n");
			strBuilder.append("  else:\n");
			strBuilder.append("    targetInstall=targetInstall+\"+WebSphere:cell=\"+cellName+\",node=\"+nodeName+\",server=\"+serverName \n"); 
			strBuilder.append("destinationInfo = ' -MapModulesToServers [[ .* .*  '+targetInstall+ ' ]] '\n");
		}
		strBuilder.append("print 'Deploy app on : '+ destinationInfo \n");
		final String options = "  -createMBeansForResources -noreloadEnabled -nodeployws -validateinstall warn -processEmbeddedConfig -noallowDispatchRemoteInclude -usedefaultbindings";
		final String commande = "AdminApp.install('" + getEarPath(pEar.getEarFile())
				+ "', '[-nopreCompileJSPs -distributeApp -nouseMetaDataFromBinary -nodeployejb "
				+ " -filepermission .*\\.dll=755#.*\\.so=755#.*\\.a=755#.*\\.sl=755 -appname "
				+ pEar.getAppName() + options +" '+destinationInfo+' ]') \n";
		getLog().info("Command of install : " + commande);
		strBuilder.append(commande);
		strBuilder.append("print 'The application was successefully install ' \n");
		strBuilder.append(getSaveScript());
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
		strBuilder.append("for server in SERVERS: \n");
		strBuilder.append("  print 'Node synchronization for server:'+server[2]+' node:'+server[1]+' ... ' \n");
		strBuilder.append("  sync1=AdminControl.completeObjectName('type=NodeSync,node='+server[1]+',*') \n");
		strBuilder.append("  if ( sync1 != ''): \n");
		strBuilder.append("    AdminControl.invoke(sync1, 'sync') \n");
		strBuilder.append("    print 'Node synchronization for server SUCESS ' \n");
		strBuilder.append("  else: \n");
		strBuilder.append("    print 'Warning the node is not started.' \n");
		return strBuilder;
	}

	/**
	 * Build script to stop application on a single server.
	 * @param pEar : The ear to stop.
	 * @return The script to stop application on a single server.
	 */
	private StringBuilder getStopAppScript(final Ear pEar) {
		final StringBuilder strBuilder = new StringBuilder();
		getLog().info("Stop application on servers : " + targetServers);
		strBuilder.append("if(IS_STARTED_"+pEar.getLocalId()+"=='true'):\n");
		strBuilder.append("  for server in SERVERS: \n");
		strBuilder.append("    print 'Stop application " +  pEar.getAppName() + " on server '+server[2]+' ... '\n");
		strBuilder.append("    appManager = AdminControl.queryNames('WebSphere:*,node='+server[1]+',type=ApplicationManager,process='+server[2]) \n");
		strBuilder.append("    AdminControl.invoke(appManager, 'stopApplication', '" + pEar.getAppName() + "') \n");
		strBuilder.append("    print 'Stop application on server : OK' \n");
		return strBuilder;
	}
	
	/**
	 * Build script to start application.
	 * @param pEar : The ear to start.
	 * @return The script to start application.
	 */
	private StringBuilder getStartAppScript(final Ear pEar) {
		final StringBuilder strBuilder = new StringBuilder();
		getLog().info("Start application on servers : " + targetServers);
		if("CURRENT_STATE".equals(pEar.getStartAfterInstall())){
			strBuilder.append("if(IS_STARTED_"+pEar.getLocalId()+"=='true'):\n");
			strBuilder.append("  for server in SERVERS: \n");
			strBuilder.append("    print 'Start application " +  pEar.getAppName() + " on server '+server[2]+' ... '\n");
			strBuilder.append("    appManager = AdminControl.queryNames('WebSphere:*,node='+server[1]+',type=ApplicationManager,process='+server[2]) \n");
			strBuilder.append("    AdminControl.invoke(appManager, 'startApplication', '" + pEar.getAppName() + "') \n");
			strBuilder.append("    print 'Start application on server : OK' \n");
		}else {
			strBuilder.append("for server in SERVERS: \n");
			strBuilder.append("  print 'Start application " +  pEar.getAppName() + " on server '+server[2]+' ... '\n");
			strBuilder.append("  appManager = AdminControl.queryNames('WebSphere:*,node='+server[1]+',type=ApplicationManager,process='+server[2]) \n");
			strBuilder.append("  AdminControl.invoke(appManager, 'startApplication', '" + pEar.getAppName() + "') \n");
			strBuilder.append("  print 'Start application on server : OK' \n");
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
