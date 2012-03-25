package org.soluvas.dsembed;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.servlet.ServletContext;

import org.apache.directory.server.configuration.ApacheDS;
import org.apache.directory.server.ldap.LdapServer;

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
		ds.startup();
	}
	
	@PreDestroy public void destroy() throws Exception {
		ds.shutdown();
	}
	
}
