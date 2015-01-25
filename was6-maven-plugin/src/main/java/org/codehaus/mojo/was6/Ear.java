package org.codehaus.mojo.was6;

import java.io.File;

/**
 * This is used for install multiples applications.
 * 
 * @author apenvern
 * @since 2012-11-21
 */
public class Ear {

    /**
     * Required parameter specifying the name of application
     * 
     * @parameter expression="${appName}"
     * @required
     */
    private String appName;
    
    /**
     * EAR archive to deploy.
     * 
     * @parameter expression="${earFile}"
     * @required
     */
    private File earFile;
    
    /**
     * Enable or disable the start-auto of the application.
     *
     * @parameter expression="${startAuto}" default-value="false"
     */
    private boolean startAuto;

    /**
     * Enable or disable the start of the application after its installation.
     *
     * @parameter expression="${startAfterInstall}" default-value="true"
     */
    private boolean startAfterInstall = true;
    
	/**
	 * @return the EAR archive to deploy.
	 */
	public File getEarFile() {
		return earFile;
	}

	/**
	 * @param pEarFile the EAR archive to deploy.
	 */
	public void setEarFile(final File pEarFile) {
		earFile = pEarFile;
	}

	/**
	 * @return the name of application on the Websphere Application Serveur.
	 */
	public String getAppName() {
		return appName;
	}

	/**
	 * @param pAppName the name of application on the Websphere Application Serveur to set.
	 */
	public void setAppNameOnWas(final String pAppName) {
		appName = pAppName;
	}

	/**
	 * @return true if the start-auto of the application is enable.
	 */
	public boolean isStartAuto() {
		return startAuto;
	}

	/**
	 * @param pStartAuto set if the start-auto of the application is enable.
	 */
	public void setStartAuto(final boolean pStartAuto) {
		startAuto = pStartAuto;
	}

	/**
	 * @return if the start of the application after its installation is enable.
	 */
	public boolean isStartAfterInstall() {
		return startAfterInstall;
	}

	/**
	 * @param pStartAfterInstall set if the start of the application after its installation is enable.
	 */
	public void setStartAfterInstall(boolean pStartAfterInstall) {
		startAfterInstall = pStartAfterInstall;
	}

}
