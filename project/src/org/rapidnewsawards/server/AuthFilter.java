package org.rapidnewsawards.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Date;
import java.util.LinkedList;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.rapidnewsawards.core.ShadowUser;
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
			_doFilter(request, response, chain);
	}


	private void _doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		UserService userService = UserServiceFactory.getUserService();
		com.google.appengine.api.users.User serviceUser = userService.getCurrentUser();
		
		if (serviceUser == null) {
			d.user = null;
		}
		else {
			User u = DAO.instance.users.findUserByLogin(serviceUser.getEmail(), serviceUser.getAuthDomain());
			boolean isAdmin = adminEmail.equals(serviceUser.getEmail());
			if (u == null && !isAdmin) {
				// first time logging in; create new user unless this is the admin or an editor
				u = new User(serviceUser.getEmail(), serviceUser.getAuthDomain(), false);
				DAO.instance.ofy().put(u);
				d.user = u;
			}
			else if (isAdmin) {
				ShadowUser shadow = DAO.instance.ofy().query(ShadowUser.class).get();
				if (shadow != null) {
					User realUser = DAO.instance.ofy().get(shadow.user);
					if (realUser != null) {
						d.user = realUser;
					}
				}
				else if (u == null) {
					d.user = null;
				}
				else {
					d.user = u;
				}
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
