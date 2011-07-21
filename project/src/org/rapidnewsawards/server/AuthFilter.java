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
import javax.servlet.http.HttpServletResponse;

import org.rapidnewsawards.core.ShadowUser;
import org.rapidnewsawards.core.User;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.googlecode.objectify.NotFoundException;

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
		
		User user = null;
		
		if (serviceUser == null) {
			user = null;
		}
		else {
			User u = DAO.instance.users.findUserByLogin(serviceUser.getEmail(), serviceUser.getAuthDomain());
			boolean isAdmin = adminEmail.equals(serviceUser.getEmail());
			if (u == null && !isAdmin) {
				// first time logging in; create new user unless this is the admin or an editor
				u = new User(serviceUser.getEmail(), serviceUser.getAuthDomain(), false);
				DAO.instance.ofy().put(u);
				user = u;
			}
			else if (isAdmin) {
				ShadowUser shadow = null;
				try { shadow = d.ofy().query(ShadowUser.class).get(); }
				catch (NotFoundException nfe) {}
				if (shadow != null) {
					User realUser = null;
					try { realUser = d.ofy().get(shadow.user); }
					catch (NotFoundException nfe) {}
					if (realUser != null) {
						user = realUser;
					}
				}
				else if (u == null) {
					user = null;
				}
				else {
					user = u;
				}
			}
			else {
				u.lastLogin = new Date();
				user = u;
				DAO.instance.ofy().put(u);
			}
		}

		//log.info("User is " + (user == null? "null" : user) + " request: " + request.toString());
		
		request.setAttribute("user", user);

		chain.doFilter(request, response);
		try {
			HttpServletResponse hr = (HttpServletResponse) response;
			hr.setHeader("Cache-Control","no-cache"); //HTTP 1.1
			hr.setHeader("Pragma","no-cache"); //HTTP 1.0
			hr.setDateHeader ("Expires", 0); //prevents caching at the proxy server
		} catch (Exception e) {
			DAO.log.severe("failed to set cache headers!");
		}
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
