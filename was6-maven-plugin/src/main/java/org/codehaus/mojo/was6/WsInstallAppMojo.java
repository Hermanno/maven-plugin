package org.codehaus.mojo.was6;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;
import org.dom4j.Document;

/**
 * Installs an EAR into WebSphere Application Server.
 * 
 * @goal installApp
 * @author <a href="mailto:david@codehaus.org">David J. M. Karlsen</a>
 * @author <a href="mailto:javier.murciego@gmail.com">Javier Murciego</a>
 */
public class WsInstallAppMojo
    extends AbstractAppMojo
{
    
    /**
     * Flag for deploying web services
     *
     * @parameter expression="${was6.deployWebServices}" default-value="false"
     */
    private boolean deployWebServices;
    
    /**
     * Flag for updating existing application or installing a brand new.
     * 
     * @parameter expression="${was6.updateExisting}" default-value="true"
     */
    private boolean updateExisting;

    /**
     * @deprecated Use cluster instead
     * 
     * @parameter expression="${was6.targetCluster}"
     */
    private String targetCluster;
    
    /**
     * Name of target cluster to deploy to.
     * 
     * @parameter expression="${was6.cluster}"
     */
    private String cluster;
    
    /**
     * @deprecated Use cell instead
     * @parameter expression="${was6.targetCell}"
     */
    private String targetCell;
    
    /**
     * The target cell for deployment.
     * @parameter expression="${was6.cell}"
     */
    private String cell;
    
    /**
     * @deprecated Use node instead
     * @parameter expression="${was6.targetNode}"
     */
    private String targetNode;
    
     /**
     * The target node for deployment.
     * @parameter expression="${was6.node}"
     */
    private String node;
    
    /**
     * @deprecated Use server instead
     * @parameter expression="${was6.targetServer}"
     */
    private String targetServer;
    
     /**
     * The target server for deployment.
     * @parameter expression="${was6.server}"
     */
    private String server;

    /**
     * EAR archive to deploy.
     * 
     * @parameter expression="${was6.earFile}" default-value="${project.artifact.file}"
     */
    private File earFile;

    /**
     * Provide ability to map module to multiple servers. -MapModulesToServers
     * 
     * @parameter expression="${was6.mapModulesToServers}"
     */
    private String mapModulesToServers;
    
    /**
     * The security roles mapping information to use.
     * 
     * @parameter
     * @since 1.1.1
     */
    private List roles;
    
    /**
     * The web modules to virtual host mapping information to use.
     * 
     * @parameter expression="${was6.mapWebModToVH}"
     * @since 1.2.1
     */
    private String mapWebModToVH;
    
    /**
     * The context root of the web module (mandatory for deploying WARs).
     * 
     * @parameter expression="${was6.contextRoot}"
     * @since 1.2.1
     */
    
    private String contextRoot;

    /**
     * {@inheritDoc}
     */
    protected String getTaskName()
    {
        return "wsInstallApp";
    }

    /**
     * {@inheritDoc}
     */
    protected void configureBuildScript( Document document )
        throws MojoExecutionException
    {
        super.configureBuildScript( document );

        if ( earFile == null ) {
            throw new MojoExecutionException( "Earfile not specified" );
        }
        if ( !earFile.canRead() )
        {
            throw new MojoExecutionException( "Bad archive: " + earFile.getAbsolutePath() );
        }
        configureTaskAttribute( document, "ear", earFile.getAbsolutePath() );

        StringBuffer options = new StringBuffer();

        options.append( "-appname " ).append( applicationName );

        if ( contextRoot !=null )
        {
            options.append( " -contextroot " ).append( contextRoot );
        }
        
        if ( mapWebModToVH !=null )
        {
            options.append( " -MapWebModToVH " ).append( mapWebModToVH );
        }
        
        if ( updateExisting )
        {
            options.append( " -update" );
        }

        String clusterToUse = targetCluster != null ? cluster : null;
        
        if ( StringUtils.isNotEmpty( clusterToUse ) )
        {
            options.append( " -cluster " ).append( clusterToUse );
        }
        
        String cellToUse = targetCell != null ? cell : null;
        
        if ( StringUtils.isNotEmpty( cellToUse ) )
        {
            options.append( " -cell " ).append( cellToUse );
        }
        
        String nodeToUse =  targetNode != null ? node : null;
        if ( StringUtils.isNotEmpty( nodeToUse ) )
        {
            options.append( " -node " ).append( nodeToUse );
        }
        
        String serverToUse = targetServer != null ? server : null;
        if ( StringUtils.isNotEmpty( serverToUse ) )
        {
            options.append( " -server " ).append( serverToUse );
        }

        if ( mapModulesToServers != null )
        {
            options.append( " -MapModulesToServers " ).append( mapModulesToServers );
        }

        if ( CollectionUtils.isNotEmpty( roles ) )
        {
            options.append( " -MapRolesToUsers { " );

            for (Iterator iterator = roles.iterator(); iterator.hasNext();) {
                Role role = (Role) iterator.next();
                options.append( role.getRoleMapping() );
            }
            
            options.append( " } " );
        }
        
        if ( deployWebServices )
        {
            options.append( " -deployws " );
        }
        
        configureTaskAttribute( document, "options", options );
    }

}
