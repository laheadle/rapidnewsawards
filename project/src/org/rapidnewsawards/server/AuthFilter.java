package org.rapidnewsawards.server;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.rapidnewsawards.core.User;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

public class AuthFilter implements Filter {
	private static final Logger log = Logger.getLogger(AuthFilter.class.getName());
	private static DAO d = DAO.instance;
	private String adminEmail;

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		UserService userService = UserServiceFactory.getUserService();
		com.google.appengine.api.users.User appUser = userService.getCurrentUser();
		
		if (appUser == null) {
			d.user = null;
		}
		else {
			User u = DAO.instance.users.findUserByLogin(appUser.getEmail(), appUser.getAuthDomain());
			if (u == null && !adminEmail.equals(appUser.getEmail())) {
				// first time logging in; create new user unless this is the admin
				u = new User(appUser.getEmail(), appUser.getAuthDomain(), false);
				DAO.instance.ofy().put(u);
				d.user = u;
			}
			// thinkme this is for u == null and appUser is admin
			else if (u == null) {
				d.user = null;
			}
			else {
				u.lastLogin = new Date();
				d.user = u;
				DAO.instance.ofy().put(u);
			}
		}

		log.fine("User is " + d.user);
		
		// response.sendRedirect(userService.createLoginURL(request.getRequestURI())); 

		chain.doFilter(request, response);
	}
	
	@Override
	public void init(FilterConfig config) throws ServletException {
		adminEmail = config.getInitParameter("adminEmail");
		if (adminEmail == null || adminEmail.equals("")) {
			adminEmail = "";
			log.warning("no admin email specified.  Watch out for auto-creation of a duplicate user with the admin's email");
		}
		else {
			log.info("Admin email: " + adminEmail);
		}
	}

	@Override
	public void destroy() {
		log.info("destroy filter");		
	}

}
