package org.soluvas.dsembed;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.servlet.ServletContext;

import org.apache.directory.server.configuration.ApacheDS;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.api.CacheService;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.DnFactory;
import org.apache.directory.server.core.api.InstanceLayout;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.api.OperationEnum;
import org.apache.directory.server.core.api.OperationManager;
import org.apache.directory.server.core.api.ReferralManager;
import org.apache.directory.server.core.api.administrative.AccessControlAdministrativePoint;
import org.apache.directory.server.core.api.administrative.CollectiveAttributeAdministrativePoint;
import org.apache.directory.server.core.api.administrative.SubschemaAdministrativePoint;
import org.apache.directory.server.core.api.administrative.TriggerExecutionAdministrativePoint;
import org.apache.directory.server.core.api.changelog.ChangeLog;
import org.apache.directory.server.core.api.event.EventService;
import org.apache.directory.server.core.api.interceptor.Interceptor;
import org.apache.directory.server.core.api.journal.Journal;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.partition.PartitionNexus;
import org.apache.directory.server.core.api.schema.SchemaPartition;
import org.apache.directory.server.core.api.subtree.SubentryCache;
import org.apache.directory.server.core.api.subtree.SubtreeEvaluator;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.shared.ldap.codec.api.LdapApiService;
import org.apache.directory.shared.ldap.model.csn.Csn;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.ldif.LdifEntry;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.schemamanager.impl.DefaultSchemaManager;
import org.apache.directory.shared.ldap.util.tree.DnNode;

/**
 * @author ceefour
 *
 */
@ApplicationScoped
public class DirectoryEngine {

	private ApacheDS ds;

	public void start(@Observes ServletContext context) {
		
	}
	
	@PostConstruct
	public void init() throws Exception {
		LdapServer ldapServer = new LdapServer();
		ds = new ApacheDS(ldapServer);
		
		InstanceLayout layout = new InstanceLayout("/tmp/dsembed_data");
		ds.getDirectoryService().setInstanceLayout(layout);
		DefaultSchemaManager schemaManager = new DefaultSchemaManager();
		ds.getDirectoryService().setSchemaManager(schemaManager);
		SchemaPartition schemaPartition = new SchemaPartition(schemaManager);
		ds.getDirectoryService().setSchemaPartition(schemaPartition);
		
		ds.startup();
	}
	
	@PreDestroy public void destroy() throws Exception {
		ds.shutdown();
	}
	
}
