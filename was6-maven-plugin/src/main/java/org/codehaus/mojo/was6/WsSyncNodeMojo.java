package org.codehaus.mojo.was6;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.dom4j.Document;

/**
 * The wsSyncNode goal launch synchronization of node. 
 * 
 * @goal wsSyncNode
 * @author apenvern
 */
public class WsSyncNodeMojo extends AbstractWas6Mojo {

    /**
     * The Default type is SOAP. Valid values are SOAP, RMI, and NONE. NONE means that no server connection is made.
     * 
     * @parameter expression="${was6.conntype}" default-value="SOAP"
     */
    private String conntype;

    /**
     * The host attribute is optional and only specified if the conntype is specified. It contains the hostname of the
     * machine to connect to
     * 
     * @parameter expression="${was6.host}" default-value="localhost"
     */
    private String host;

    /**
     * The port on the host to connect to.
     * 
     * @parameter expression="${was6.port}"
     */
    private Integer port;

    /**
     * Contains the user ID to authenticate with.
     * 
     * @parameter expression="${was6.user}"
     */
    private String user;

    /**
     * Contains the password to authenticate with.
     * 
     * @parameter expression="${was6.password}"
     */
    private String password;

    /**
     * sets maximum size of the memory for the underlying VM.
     * 
     * @parameter expression="${was6.jvmMaxMemory}" default-value="256M"
     */
    private String jvmMaxMemory;
    
    /**
     * The target node for deployment.
     * @parameter expression="${was6.targetNode}"
     */
    private String targetNode;

    /**
     * {@inheritDoc}
     */
    protected void configureBuildScript( Document document )
        throws MojoExecutionException
    {
    	File script = new File( getWorkingDirectory(), "was6plugin-script-syncNode." + System.currentTimeMillis() + ".jacl" );
    	try {
			BufferedWriter bfWriter = new BufferedWriter(new FileWriter(script));
			bfWriter.write("set Sync1 [$AdminControl completeObjectName type=NodeSync,node="+targetNode+",*] \n");
			bfWriter.write("$AdminControl invoke $Sync1 sync");
			bfWriter.close();
		} catch (IOException e) {
			throw new MojoExecutionException("Error on syncNode task : ", e);
		}
    	
        super.configureTaskAttribute( document, "profileName", null );
        super.configureTaskAttribute( document, "profile", null );
        super.configureTaskAttribute( document, "lang", "jacl" );
        super.configureTaskAttribute( document, "properties", null );
        super.configureTaskAttribute( document, "user", user );
        super.configureTaskAttribute( document, "password", password );
        super.configureTaskAttribute( document, "host", host );
        super.configureTaskAttribute( document, "port", port );
        super.configureTaskAttribute( document, "conntype", conntype );
        super.configureTaskAttribute( document, "jvmMaxMemory", jvmMaxMemory );
        super.configureTaskAttribute( document, "command", null );
        super.configureTaskAttribute( document, "script", "\"" + script + "\"" );
        
    }

    /**
     * {@inheritDoc}
     */
    protected String getTaskName()
    {
        return "wsAdmin";
    }

}
