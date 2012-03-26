package org.soluvas.dsembed;

import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidAttributeValueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;

public class User implements org.picketlink.idm.api.User {

	private transient Logger log = LoggerFactory.getLogger(User.class);
	String uid;
	String email;
	String firstName;
	String lastName;
	
	Entry entry;
	
	public void load(Entry entry) {
		this.entry = entry;
		log.info("User {}", entry.getDn());
		for (Attribute attr : entry.getAttributes()) {
			log.info("{} = {}", attr.getAttributeType().getNames(), Iterables.toString(attr));
		}
		try {
			uid = entry.get("uid").getString();
			email = entry.get("mail").getString();
			firstName = entry.get("gn").getString();
			lastName = entry.get("sn").getString();
		} catch (LdapInvalidAttributeValueException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public String getKey() {
		return entry.getDn().getName();
	}

	@Override
	public String getId() {
		try {
			return entry.get("uid").getString();
		} catch (LdapInvalidAttributeValueException e) {
			throw new RuntimeException(e);
		}
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	
	public String getName() {
		try {
			return entry.get("cn").getString();
		} catch (LdapInvalidAttributeValueException e) {
			throw new RuntimeException(e);
		}
	}
	
	public String getFbId() {
		try {
			return entry.get("fbId").getString();
		} catch (LdapInvalidAttributeValueException e) {
			throw new RuntimeException(e);
		}
	}


}
