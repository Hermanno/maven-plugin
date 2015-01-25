package org.codehaus.mojo.was6;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.dom4j.Document;

/**
 * Generates EJB RMIC stub sources.
 * <p />
 * This goal will fork a parallel life cycle up to package phase. This is required because an archive is required as
 * input to the underlying tasks.
 * 
 * @goal ejbdeploy
 * @phase generate-sources
 * @requiresDependencyResolution compile
 * @execute phase="package"
 * @author <a href="mailto:david@codehaus.org">David J. M. Karlsen</a>
 * @author <a href="mailto:javier.murciego@gmail.com">Javier Murciego</a>
 */
public class EjbDeployMojo
    extends AbstractEjbMojo
{
    /**
     * Reference to project which was forked in parallel.
     * 
     * @parameter default-value="${executedProject}"
     * @required
     * @readonly
     */
    private MavenProject executedProject;

    /**
     * Set to true to disable validation messages.
     * 
     * @parameter expression="${was6.noValidate}" default-value="false"
     */
    private boolean noValidate;

    /**
     * Set to true to disable warning and informational messages.
     * 
     * @parameter expression="${was6.noWarnings}" default-value="false"
     */
    private boolean noWarnings;

    /**
     * Set to true to disable informational messages.
     * 
     * @parameter expression="${was6.noInform}" default-value="false"
     */
    private boolean noInform;

    /**
     * Set this to true if you've got an old rational SDP version (7.0.0.4/interimfix 001), or an old WAS base/ND
     * installation (lower than fixpack 007).
     * 
     * @parameter expression="${was6.legacyMode}" default-value="false"
     */
    private boolean legacyMode;

    /**
     * Specifies the name of the database to create.
     * 
     * @parameter expression="${was6.dbname}"
     */
    private String dbname;

    /**
     * Specifies the name of the database schema to create.
     * 
     * @parameter expression="${was6.dbschema}"
     */
    private String dbschema;

    /**
     * Specifies the type of database the EJBs will use.
     * 
     * @parameter expression="${was6.dbvendor}"
     */
    private String dbvendor;

    /**
     * Specifies to enable dynamic query support.
     * 
     * @parameter expression="${was6.dynamic}"
     */
    private boolean dynamic;

    /**
     * Set to true to use WebSphere 3.5 compatible mapping rules.
     * 
     * @parameter expression="${was6.compatible35}"
     */
    private boolean compatible35;

    /**
     * Set to true to generate SQL/J persistor code.
     * 
     * @parameter expression="${was6.sqlj}"
     */
    private boolean sqlj;

	/**
	 * Classifier to add to the artifact generated. If given, the artifact will
	 * be an attachment instead.
	 * 
	 * @parameter expression="${ejb.classifier}"
	 */
	private String classifier;
     	
	/**
	 * The directory for the generated EJB.
	 * 
	 * @parameter default-value="${project.build.directory}"
	 * @required
	 * @readonly
	 */
	private File basedir;

	/**
	 * The name of the EJB file to generate.
	 * 
	 * @parameter default-value="${project.build.finalName}" expression="${jarName}"
	 * @required
	 */
	private String jarName;
    
	/**
	 * The Maven project's helper.
	 * 
	 * @component
	 */
	private MavenProjectHelper projectHelper;     
    
    /**
     * JDK compliance level. Valid values are: 1.4 or 5.0 This parameter will only be taken into consideration if
     * legacyMode is false. IBM didn't support this flag in earlier versions.
     * 
     * @parameter expression="${was6.jdkComplianceLevel}" default-value="${project.build.java.target}"
     */
    private String jdkComplianceLevel;
    
    /**
     * WAS used version. Possible values are was60, was61, was70 ...
     * By the moment, only was60 has special treatment
     * 
     * @parameter expression="${was6.wasVersion}" default-value="was61"
     * @since 1.2
     */
    private String wasVersion;


    protected File getOutputJarFile()
    {
		if (classifier == null) {
			classifier = "";
		} else if (classifier.trim().length() > 0
				&& !classifier.startsWith("-")) {
			classifier = "-" + classifier;
		}
      	File outputJarFile = new File(getWorkingDirectory(), executedProject
				.getArtifact().getArtifactId()
				+ classifier + "-deployed.jar");

		return outputJarFile;
    }
    
    /**
     * {@inheritDoc}
     */
    protected String getTaskName()
    {
        return "wsEjbDeploy";
    }

    /**
     * {@inheritDoc}
     */
    protected void configureBuildScript( Document document )
        throws MojoExecutionException
    {
        //hack to avoid IBM bug: http://jira.codehaus.org/browse/MWAS-7
        document.getRootElement().addElement( "property" ).addAttribute( "name", "user.install.root" ).addAttribute( "location", getWasHome().getAbsolutePath() );

        File inputFile = getEJBJarFile( basedir, jarName, classifier ); 
        if ( !inputFile.canRead() )
        {
            throw new MojoExecutionException( "Invalid archive: " + inputFile.getAbsolutePath() );
        }
        configureTaskAttribute( document, "inputJar", inputFile.getAbsolutePath() );
        configureTaskAttribute( document, "outputJar", getOutputJarFile() );
        configureTaskAttribute( document, "workingDirectory", getWorkingDirectory().getAbsolutePath() );
        configureTaskAttribute( document, "trace", Boolean.toString( isVerbose() ) );
        configureTaskAttribute( document, "noInform", Boolean.toString( noInform ) );
        configureTaskAttribute( document, "noWarnings", Boolean.toString( noWarnings ) );
        configureTaskAttribute( document, "noValidate", Boolean.toString( noValidate ) );
        configureTaskAttribute( document, "classpath", getRuntimeClasspath() );
        configureTaskAttribute( document, "dbname", dbname );
        configureTaskAttribute( document, "dbvendor", dbvendor );
        configureTaskAttribute( document, "dbschema", dbschema );
        configureTaskAttribute( document, "dynamic", Boolean.toString( dynamic ) );
        configureTaskAttribute( document, "compatible35", Boolean.toString( compatible35 ) );
        configureTaskAttribute( document, "sqlj", Boolean.toString( sqlj ) );

        if ( legacyMode )
        {
            getLog().warn( "Legacy mode - jdkComplianceLevel will NOT be taken into consideration (default will be used)" );
            configureTaskAttribute( document, "jdkComplianceLevel", null );
        }
        else
        {
            configureTaskAttribute( document, "jdkComplianceLevel", 
                                    "1.5".equals( jdkComplianceLevel ) ? "5.0" : jdkComplianceLevel );
        }
    }

    /**
     * {@inheritDoc}
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if(!isSkip()){
            if ( !getMavenProject().getPackaging().equalsIgnoreCase( "ejb" ) )
            {
                throw new MojoExecutionException( "Invalid packaging type, this plugin can only be applied to ejb packaging type projects" );
            }

            super.execute();

            if ( !getOutputJarFile().exists() )  //TODO: Solve generically - MWAS-14 - why doesn't failOnError fail the build and ws_ant return a returncode != 0?
            {
                throw new MojoExecutionException( "Deployment failed - see previous errors" );
            }

            File[] workingDirectorySubdirs =
                getWorkingDirectory().listFiles( (java.io.FileFilter) DirectoryFileFilter.DIRECTORY );
            if ( workingDirectorySubdirs.length == 1 )
            {
               if (!"was60".equalsIgnoreCase( wasVersion ) )
            	{
            		processWas61(workingDirectorySubdirs);
            	}
            	else
            	{
            		processWas60(workingDirectorySubdirs);
            	} 
            }
            else
            {
                getLog().warn( "No sources were generated" );
            }

            getLog().info( "ejbDeploy finished" );
        }else{
            getLog().info( "Skipping execution" );
    	}
    }

    /**
     * Computes the runtime classpath.
     * 
     * @return A representation of the computed runtime classpath.
     * @throws MojoExecutionException in case of dependency resolution failure
     */
    private String getRuntimeClasspath()
        throws MojoExecutionException
    {
        try
        {
            // get the union of compile- and runtime classpath elements
            Set dependencySet = new HashSet();
            dependencySet.addAll( getMavenProject().getCompileClasspathElements() );
            dependencySet.addAll( getMavenProject().getRuntimeClasspathElements() );
            String compileClasspath = StringUtils.join( dependencySet, File.pathSeparator );
            
            return compileClasspath;
        }
        catch ( DependencyResolutionRequiredException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }

	/**
	 * Returns the EJB Jar file to generate, based on an optional classifier.
	 * 
	 * @param basedir
	 *            the output directory
	 * @param finalName
	 *            the name of the ear file
	 * @param classifier
	 *            an optional classifier
	 * @return the EJB file to generate
	 */
	private static File getEJBJarFile(File basedir, String finalName,
			String classifier) {
		if (classifier == null) {
			classifier = "";
		} else if (classifier.trim().length() > 0
				&& !classifier.startsWith("-")) {
			classifier = "-" + classifier;
		}
		File f = new File(basedir, finalName + classifier + ".jar");
		return f;
	}
    
    /**
     * Process the output from the ws_ant ejbDeploy task for WAS 6.1 or 7.0.
     */
    private void processWas61(File[] workingDirectorySubdirs) throws MojoExecutionException
    {
        // copy sources
        File generatedSources = new File( workingDirectorySubdirs[0], getMavenProject().getBuild().getFinalName() + classifier + File.separator + "ejbModule" );
        try
        {
            FileUtils.copyDirectory( generatedSources, getGeneratedSourcesDirectory() );
            FileUtils.deleteDirectory( new File( getGeneratedSourcesDirectory(), "META-INF" ) );
            List compileSourceRoots = getMavenProject().getCompileSourceRoots();
            compileSourceRoots.add( getGeneratedSourcesDirectory().getPath() );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error copying generated sources", e );
        }

        // copy generated classes
        File generatedClasses =
            new File( workingDirectorySubdirs[0], getMavenProject().getBuild().getFinalName() + classifier + File.separator +
                "build" + File.separator + "classes" );
        try
        {
            FileUtils.copyDirectory( generatedClasses, getGeneratedClassesDirectory() );
            Resource resource = new Resource();
            resource.setDirectory( getGeneratedClassesDirectory().getPath() );
            getMavenProject().getResources().add( resource );
       }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error copying generated classes", e );
        }
    }
    
    /**
     * Process the output from the ws_ant ejbDeploy task for WAS 6.0 (and possibly below).
     */
    private void processWas60(File[] workingDirectorySubdirs)
    {
    	// Get a list of directories in the workspace.
    	// Should be .metadata and the name of the ejb project.
    	// Under WAS 6.1 it will be the final name of the project.
    	// However, under WAS 6.0 it is meaningless (an actual example: e74f842a).
    	// So we need to search for it.
    	// The array should be 2 items long. The first should probably be .metadata.
        File[] workspaceSubdirs = workingDirectorySubdirs[0].listFiles( (java.io.FileFilter) DirectoryFileFilter.DIRECTORY );

        getLog().debug( "getWorkingDirectory()                       : " + getWorkingDirectory() );
        getLog().debug( "getMavenProject().getBuild().getFinalName() : " + getMavenProject().getBuild().getFinalName() );
        getLog().debug( "workingDirectorySubdirs[0]                  : " + workingDirectorySubdirs[0] );
        getLog().debug( "workspaceSubdirs[0]                         : " + workspaceSubdirs[0] );
        getLog().debug( "workspaceSubdirs[1]                         : " + workspaceSubdirs[1] );

        if (workspaceSubdirs.length == 2)
        {
            // copy generated sources
            File generatedSources = new File( workspaceSubdirs[1], "ejbModule" );
            try
            {
                FileUtils.copyDirectory( generatedSources, getGeneratedSourcesDirectory() );
                FileUtils.deleteDirectory( new File( getGeneratedSourcesDirectory(), "META-INF" ) );
	            List compileSourceRoots = getMavenProject().getCompileSourceRoots();
	            compileSourceRoots.add( getGeneratedSourcesDirectory().getPath() );
	            // Copied everything, java + classes, so now delete the class files.
	            String[] extensions = new String[1];
	            extensions[0] = "class";
	            cleanupFiles(getGeneratedSourcesDirectory(), extensions);
            }
            catch ( IOException e )
            {
                //throw new MojoExecutionException( "Error copying generated sources", e );
            	// Don't fail build, just warn. Because we only warn if there are no sources generated.
                getLog().warn( "Error copying generated sources" );
            }

            // copy generated classes
            File generatedClasses = new File( workspaceSubdirs[1], "ejbModule" );
            try
            {
                FileUtils.copyDirectory( generatedClasses, getGeneratedClassesDirectory() );
                Resource resource = new Resource();
                resource.setDirectory( getGeneratedClassesDirectory().getPath() );
                getMavenProject().getResources().add( resource );
	            // Copied everything, java + classes, so now delete the java files.
	            String[] extensions = new String[1];
	            extensions[0] = "java";
	            cleanupFiles(getGeneratedClassesDirectory(), extensions);
            }
            catch ( IOException e )
            {
                //throw new MojoExecutionException( "Error copying generated classes", e );
            	// Don't fail build, just warn. Because we only warn if there are no sources generated.
                getLog().warn( "Error copying generated classes" );
            }
        }
        else
        {
        	// More than two dirs were returned in the workspace dir.
        	// There should only be two. The .metadata [0] and the project dir [1].
            getLog().warn( "Unable to make sense of ejbDeploy workbench directory output." );
        }
    }

    /**
     * Because WAS 6.0 does not segregate the compiled classes and generated sources
     * we are forced to copy everything and cleanup the bits that are not needed later.
     * This is a helper routine to assist with the cleanup.
     * @param directory
     * @param extensions
     */
    private void cleanupFiles(File directory, String[] extensions)
    {
        Collection cFiles = FileUtils.listFiles(directory, extensions, true);
        File[] files = FileUtils.convertFileCollectionToFileArray(cFiles);
        getLog().debug( "files.length                                : " + files.length );
        for (int i=0; i<files.length; i++)
        {
            getLog().debug( "deleting file                               : " + files[i].getPath() );
            files[i].delete();
        }
    }
}