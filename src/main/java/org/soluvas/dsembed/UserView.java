package org.soluvas.dsembed;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

@Named @RequestScoped
public class UserView {

	@Inject UserDao userDao;
	
	public List<User> getUsers() {
		return userDao.findAll();
	}
}
