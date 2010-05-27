package org.rapidnewsawards.server;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.mortbay.log.Log;
import org.rapidnewsawards.shared.User;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

public class AuthFilter implements Filter {
	private static final Logger log = Logger.getLogger(AuthFilter.class.getName());
	private static DAO d = DAO.instance;

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		UserService userService = UserServiceFactory.getUserService();
		com.google.appengine.api.users.User appUser = userService.getCurrentUser();
		
		if (appUser == null) {
			d.user = null;
		}
		else {
			User u = DAO.instance.findUserByLogin(appUser.getEmail(), appUser.getAuthDomain());
			if (u == null) {
				// first time logging in; create new user
				u = new User();
				u.email = appUser.getEmail();
				u.domain = appUser.getAuthDomain();
				DAO.instance.ofy().put(u);
				d.user = u;
			}
			else {
				d.user = u;
			}
		}

		log.info("User is " + d.user);
		
		// response.sendRedirect(userService.createLoginURL(request.getRequestURI())); 

		chain.doFilter(request, response);
	}
	
	@Override
	public void init(FilterConfig arg0) throws ServletException {
		log.info("init filter");
	}

	@Override
	public void destroy() {
		log.info("destroy filter");		
	}

}
