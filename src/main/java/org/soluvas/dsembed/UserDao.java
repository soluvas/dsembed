package org.soluvas.dsembed;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.shared.ldap.model.cursor.EntryCursor;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.name.Dn;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

@ApplicationScoped
public class UserDao {

	@Inject LdapConnection ldap;
	
	public List<User> findAll() {
		EntryCursor cursor;
		try {
			cursor = ldap.search(new Dn( "dc=apache,dc=org" ), "(objectclass=person)", SearchScope.ONELEVEL);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		Iterable<User> users = Iterables.transform(cursor, new Function<Entry, User>() {
			public User apply(Entry input) {
				User user = new User();
				user.load(input);
				return user;
			};
		});
		return Lists.newArrayList(users);
	}
}
