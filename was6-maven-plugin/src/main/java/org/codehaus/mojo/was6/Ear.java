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
     * </br>DISABLE : disable the start-auto.
     * </br>ENABLE : enable the start-auto.
     * </br>CURRENT_STATE : In case that the application is already installed it will take the existing configuration.
     *
     * @parameter expression="${startAuto}" default-value="DISABLE"
     */
    private String startAuto;

    /**
     * Enable or disable the start of the application after its installation.
     * </br>START : Start the application after its installation.
     * </br>STOP : Don't start the application after its installation.
     * </br>CURRENT_STATE : In case that the application is already installed it will start the application if it was started.
     *
     * @parameter expression="${startAfterInstall}" default-value="START"
     */
    private String startAfterInstall;
    
    /** The id of the ear use in the jacl script. */
    private int localId;
        
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
	 * @return The state of the start-auto of the application.
	 */
	public String getStartAuto() {
		return startAuto;
	}

	/**
	 * @param pStartAuto set state of the start-auto of the application.
	 */
	public void setStartAuto(final String pStartAuto) {
		startAuto = pStartAuto;
	}

	/**
	 * @return if the application will be started after its installation.
	 */
	public String getStartAfterInstall() {
		return startAfterInstall;
	}

	/**
	 * @param pStartAfterInstall set if the application will be started after its installation.
	 */
	public void setStartAfterInstall(final String pStartAfterInstall) {
		startAfterInstall = pStartAfterInstall;
	}

	/**
	 * @return The id of the ear use in the jacl script.
	 */
	public int getLocalId() {
		return localId;
	}

	/**
	 * @param localId set the id of the ear use in the jacl script.
	 */
	public void setLocalId(int localId) {
		this.localId = localId;
	}

}
