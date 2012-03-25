package org.soluvas.dsembed;

import java.util.HashSet;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.servlet.ServletContext;

import org.apache.directory.server.configuration.ApacheDS;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.name.DN;

/**
 * @author ceefour See:
 *         http://directory.apache.org/apacheds/1.5/41-embedding-apacheds
 *         -into-an-application.html
 */
@ApplicationScoped
public class DirectoryEngine {

//	private ApacheDS ds;
	private DefaultDirectoryService service;

	public void start(@Observes ServletContext context) {

	}

	/**
	 * Add a new partition to the server
	 * 
	 * @param partitionId
	 *            The partition Id
	 * @param partitionDn
	 *            The partition DN
	 * @return The newly added partition
	 * @throws Exception
	 *             If the partition can't be added
	 */
	private Partition addPartition(String partitionId, String partitionDn)
			throws Exception {
		// Create a new partition named 'foo'.
		Partition partition = new JdbmPartition();
		partition.setId(partitionId);
		partition.setSuffix(partitionDn);
		service.addPartition(partition);

		return partition;
	}

	/**
	 * Add a new set of index on the given attributes
	 * 
	 * @param partition
	 *            The partition on which we want to add index
	 * @param attrs
	 *            The list of attributes to index
	 */
	private void addIndex(Partition partition, String... attrs) {
		// Index some attributes on the apache partition
		HashSet<Index<?, ServerEntry, Long>> indexedAttributes = new HashSet<Index<?, ServerEntry, Long>>();

		for (String attribute : attrs) {
			indexedAttributes
					.add(new JdbmIndex<String, ServerEntry>(attribute));
		}

		((JdbmPartition) partition).setIndexedAttributes(indexedAttributes);
	}

	@PostConstruct
	public void init() throws Exception {
		service = new DefaultDirectoryService();
		// Disable changelog
		service.getChangeLog().setEnabled(false);

		// Create a new partition named 'apache'.
		Partition apachePartition = addPartition("apache", "dc=apache,dc=org");

		// Index some attributes on the apache partition
		addIndex(apachePartition, "objectClass", "ou", "uid");

		// And start the service
		service.startup();

		// Inject the apache root entry if it does not already exist
		if (!service.getAdminSession().exists(apachePartition.getSuffixDn())) {
			DN dnApache = new DN("dc=Apache,dc=Org");
			ServerEntry entryApache = service.newEntry(dnApache);
			entryApache.add("objectClass", "top", "domain", "extensibleObject");
			entryApache.add("dc", "Apache");
			service.getAdminSession().add(entryApache);
		}

		// We are all done !

		// LdapServer ldapServer = new LdapServer();
		// ds = new ApacheDS(ldapServer);
		//
		// InstanceLayout layout = new InstanceLayout("/tmp/dsembed_data");
		// ds.getDirectoryService().setInstanceLayout(layout);
		// DefaultSchemaManager schemaManager = new DefaultSchemaManager();
		// ds.getDirectoryService().setSchemaManager(schemaManager);
		// SchemaPartition schemaPartition = new SchemaPartition(schemaManager);
		// ds.getDirectoryService().setSchemaPartition(schemaPartition);
		// LdifPartition systemPartition = new LdifPartition(schemaManager);
		// systemPartition.setId("system");
		// systemPartition.setSuffixDn(new Dn("ou=system"));
		// ds.getDirectoryService().setSystemPartition(systemPartition);
		//
		// ds.startup();
	}

	@PreDestroy
	public void destroy() throws Exception {
		service.shutdown();
	}

}
