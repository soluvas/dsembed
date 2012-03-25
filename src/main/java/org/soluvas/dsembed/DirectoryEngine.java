package org.soluvas.dsembed;

import java.io.File;
import java.util.HashSet;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InstanceLayout;
import org.apache.directory.server.core.api.LdapCoreSessionConnection;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.schema.SchemaPartition;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.schemaextractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaextractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemamanager.impl.DefaultSchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ceefour
 *
 */
@ApplicationScoped
public class DirectoryEngine {

	private transient Logger log = LoggerFactory.getLogger(DirectoryEngine.class);
	
    /** The directory service */
    private DirectoryService service;

    /** The LDAP server */
    private LdapServer server;
	private static File workDir;

    /**
     * Add a new partition to the server
     *
     * @param partitionId The partition Id
     * @param partitionDn The partition DN
     * @return The newly added partition
     * @throws Exception If the partition can't be added
     */
    private Partition addPartition( String partitionId, String partitionDn ) throws Exception
    {
        // Create a new partition named 'foo'.
        JdbmPartition partition = new JdbmPartition(service.getSchemaManager());
        partition.setId( partitionId );
        partition.setPartitionPath( new File( workDir, partitionId ).toURI() );
        partition.setSuffixDn( new Dn(partitionDn) );
        service.addPartition( partition );

        return partition;
    }

    /**
     * Add a new partition to the server
     *
     * @param partitionId The partition Id
     * @param partitionDn The partition DN
     * @return The newly added partition
     * @throws Exception If the partition can't be added
     */
    private Partition addSystemPartition( String partitionId, String partitionDn ) throws Exception
    {
        // Create a new partition named 'foo'.
        JdbmPartition partition = new JdbmPartition(service.getSchemaManager());
        partition.setId( partitionId );
        partition.setPartitionPath( new File( workDir, partitionId ).toURI() );
        partition.setSuffixDn( new Dn(partitionDn) );

        return partition;
    }


    /**
     * Add a new set of index on the given attributes
     *
     * @param partition The partition on which we want to add index
     * @param attrs The list of attributes to index
     */
    private void addIndex( Partition partition, String... attrs )
    {
        // Index some attributes on the apache partition
        HashSet<Index<?, Entry, Long>> indexedAttributes = new HashSet<Index<?, Entry, Long>>();

        for ( String attribute : attrs )
        {
            indexedAttributes.add( new JdbmIndex<String, Entry>( attribute ) );
        }

        ( ( JdbmPartition ) partition ).setIndexedAttributes( indexedAttributes );
    }

    
    /**
     * initialize the schema manager and add the schema partition to diectory service
     *
     * @throws Exception if the schema LDIF files are not found on the classpath
     */
    private void initSchemaPartition() throws Exception
    {
    	SchemaPartition schemaPartition = new SchemaPartition(service.getSchemaManager());
    	service.setSchemaPartition(schemaPartition);

        // Init the LdifPartition
        LdifPartition ldifPartition = new LdifPartition(service.getSchemaManager());
        File schemaDir = new File(workDir, "schema");
        log.info("Schema directory: {}", schemaDir);
		ldifPartition.setPartitionPath( schemaDir.toURI() );

        // Extract the schema on disk (a brand new one) and load the registries
        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( workDir );
        extractor.extractOrCopy( true );

        schemaPartition.setWrappedPartition( ldifPartition );

//        SchemaLoader loader = new LdifSchemaLoader( schemaDir );
//        SchemaManager schemaManager = new DefaultSchemaManager( loader );
//        service.setSchemaManager( schemaManager );
//
//        // We have to load the schema now, otherwise we won't be able
//        // to initialize the Partitions, as we won't be able to parse 
//        // and normalize their suffix DN
//        schemaManager.loadAllEnabled();
//
//        schemaPartition.setSchemaManager( schemaManager );
//
//        List<Throwable> errors = schemaManager.getErrors();
//
//        if ( errors.size() != 0 )
//        {
//            throw new Exception( "Schema load failed : " + errors );
//        }
    }
    
    
    /**
     * Initialize the server. It creates the partition, adds the index, and
     * injects the context entries for the created partitions.
     *
     * @param workDir the directory to be used for storing the data
     * @throws Exception if there were some problems while initializing the system
     */
    private void initDirectoryService( File workDir ) throws Exception
    {
        // Initialize the LDAP service
        service = new DefaultDirectoryService();
        service.setSchemaManager(new DefaultSchemaManager());
//      service.setWorkingDirectory( workDir );
        service.setInstanceLayout(new InstanceLayout(workDir));
        
        // first load the schema
        initSchemaPartition();
        
        // then the system partition
        // this is a MANDATORY partition
        Partition systemPartition = addSystemPartition( "system", ServerDNConstants.SYSTEM_DN );
        service.setSystemPartition( systemPartition );
        
        // Disable the ChangeLog system
        service.getChangeLog().setEnabled( false );
        service.setDenormalizeOpAttrsEnabled( true );

        // Now we can create as many partitions as we need
        // Create some new partitions named 'foo', 'bar' and 'apache'.
        Partition fooPartition = addPartition( "foo", "dc=foo,dc=com" );
        Partition barPartition = addPartition( "bar", "dc=bar,dc=com" );
        Partition apachePartition = addPartition( "apache", "dc=apache,dc=org" );

        // Index some attributes on the apache partition
        addIndex( apachePartition, "objectClass", "ou", "uid" );

        // And start the service
        service.startup();

        // Inject the foo root entry if it does not already exist
        try
        {
            service.getAdminSession().lookup( fooPartition.getSuffixDn() );
        }
        catch ( LdapException lnnfe )
        {
            Dn dnFoo = new Dn( "dc=foo,dc=com" );
            Entry entryFoo = service.newEntry( dnFoo );
            entryFoo.add( "objectClass", "top", "domain", "extensibleObject" );
            entryFoo.add( "dc", "foo" );
            service.getAdminSession().add( entryFoo );
        }

        // Inject the bar root entry
        try
        {
            service.getAdminSession().lookup( barPartition.getSuffixDn() );
        }
        catch ( LdapException lnnfe )
        {
            Dn dnBar = new Dn( "dc=bar,dc=com" );
            Entry entryBar = service.newEntry( dnBar );
            entryBar.add( "objectClass", "top", "domain", "extensibleObject" );
            entryBar.add( "dc", "bar" );
            service.getAdminSession().add( entryBar );
        }

        // Inject the apache root entry
        if ( !service.getAdminSession().exists( apachePartition.getSuffixDn() ) )
        {
            Dn dnApache = new Dn( "dc=Apache,dc=Org" );
            Entry entryApache = service.newEntry( dnApache );
            entryApache.add( "objectClass", "top", "domain", "extensibleObject" );
            entryApache.add( "dc", "Apache" );
            service.getAdminSession().add( entryApache );
        }

        // We are all done !
    }


    /**
     * starts the LdapServer
     *
     * @throws Exception
     */
    public void startServer() throws Exception
    {
        server = new LdapServer();
        int serverPort = 10389;
        server.setTransports( new TcpTransport( serverPort ) );
        server.setDirectoryService( service );
        
        server.start();
    }

	@PostConstruct
	public void start() throws Exception {
    	String tmpDir = System.getProperty( "java.io.tmpdir" );
    	if (System.getenv("OPENSHIFT_DATA_DIR") != null)
    		tmpDir = System.getenv("OPENSHIFT_DATA_DIR"); 
        workDir = new File(tmpDir, "server-work" );
        workDir.mkdirs();
        
        // Create the server
        initDirectoryService( workDir );

//        CoreSession session = service.getAdminSession();
        LdapConnection ldap = new LdapCoreSessionConnection(service);
        
        // Read an entry
        Entry result = ldap.lookup( new Dn( "dc=apache,dc=org" ) );

        // And print it if available
        System.out.println( "Found entry : " + result );
        
        // optionally we can start a server too
        startServer();
	}
	
	@Produces LdapConnection createConnection() {
		return new LdapCoreSessionConnection(service);
	}
	
	@PreDestroy public void destroy() throws Exception {
		if (server != null && server.isStarted())
			server.stop();
		if (service != null)
			service.shutdown();
	}
	
}
