package org.rapidnewsawards.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.Link;
import org.rapidnewsawards.shared.Name;
import org.rapidnewsawards.shared.RecentSocials;
import org.rapidnewsawards.shared.RecentStories;
import org.rapidnewsawards.shared.RecentVotes;
import org.rapidnewsawards.shared.RelatedUserInfo;
import org.rapidnewsawards.shared.Return;
import org.rapidnewsawards.shared.User;
import org.rapidnewsawards.shared.VoteResult;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.googlecode.objectify.Key;

public class JSONServlet extends HttpServlet {
		private static final Logger log = Logger.getLogger(DoSomethingServlet.class.getName());
		private static DAO d = DAO.instance;

		// TODO: on client side, null means current edition; on server, -1 does.
		private int ed(Integer edition) { return edition == null ? -1 : edition; }

		@Override
		public void doGet(HttpServletRequest request, HttpServletResponse resp)
		throws ServletException, IOException {
			String fun = request.getParameter("fun");
			PrintWriter out = resp.getWriter();
			Gson g = new Gson();
			log.info("json " + fun);
			Integer edition = null;
			try {
				edition = new Integer(request.getParameter("ed"));
			}
			catch (Exception e) {}
			
			// yeah, i know. very funny.
			if (fun.equals("recentStories")) {
				RecentStories rs = d.getTopStories(ed(edition), Name.AGGREGATOR_NAME);
				if (rs.edition == null) {
					rs = d.getTopStories(rs.numEditions - 1, Name.AGGREGATOR_NAME);
				}
				out.println(g.toJson(rs));
			}
			else if (fun.equals("recentSocials")) {	
				RecentSocials rs = d.getRecentSocials(edition);
				out.println(g.toJson(rs));
			}
			else if (fun.equals("allEditions")) {	
				out.println(g.toJson(d.getAllEditions()));
			}
			else if (fun.equals("grabTitle")) {	
				String urlStr = request.getParameter("url");
				out.println(g.toJson(TitleGrabber.getTitle(urlStr)));
			}
			else if (fun.equals("voteFor")) {
				String link = request.getParameter("link");
				String fullLink = request.getParameter("fullLink"); 
				Edition ed = d.getCurrentEdition(Name.AGGREGATOR_NAME);
				Boolean on = new Boolean(request.getParameter("on"));
				VoteResult vr = d.voteFor(link, fullLink, ed, on);				
				out.println(g.toJson(vr));
			}
			else if (fun.equals("welcomeUser")) {
				if (d.user == null)
					out.println(g.toJson(null));
				String nickname = request.getParameter("nickname");				
				out.println(g.toJson(d.welcomeUser(nickname, 0)));
			}
			else if (fun.equals("submitStory")) {
				String url = request.getParameter("url");
				String title = request.getParameter("title"); 
				Edition ed = d.getCurrentEdition(Name.AGGREGATOR_NAME);
				VoteResult vr = d.submitStory(url, title, ed);
				out.println(g.toJson(vr));
			}
			else if (fun.equals("recentVotes")) {
				Integer id = new Integer(request.getParameter("id"));
				RelatedUserInfo ru =
						d.getRelatedUserInfo(Name.AGGREGATOR_NAME, d.user, new Key<User>(User.class, id));
				out.println(g.toJson(ru));
			}
			else if (fun.equals("doSocial")) {
				String _to = request.getParameter("to");
				User to = d.ofy().get(User.getKey(new Long(_to)));
				Boolean on  = new Boolean(request.getParameter("on"));
				out.println(g.toJson(d.doSocial(to, on).s));
			}
			else if (fun.equals("recentVotes")) {
				RecentVotes rv = d.getRecentVotes(ed(edition), Name.AGGREGATOR_NAME);
				out.println(g.toJson(rv));
			}
			else if (fun.equals("sendUserInfo")) {
				out.println(g.toJson(d.user));
			}
			else if (fun.equals("relatedUser")) {
				long userId = new Long(request.getParameter("id"));
				out.println(g.toJson(
						d.getRelatedUserInfo(Name.AGGREGATOR_NAME, d.user, 
								new Key<User>(User.class, userId))));
			}

			else if (fun.equals("sendLogoutURL")) {
		        UserService userService = UserServiceFactory.getUserService();
				String url = request.getParameter("url");
				out.println(g.toJson(userService.createLogoutURL(url)));
			}
			else if (fun.equals("sendLoginURL")) {
				String url = request.getParameter("url");
				UserService userService = UserServiceFactory.getUserService();
				out.println(g.toJson(userService.createLoginURL(url)));
			}
		}
		

		  public void doPost(HttpServletRequest request,
		                     HttpServletResponse response)
		      throws ServletException, IOException {
		    doGet(request, response);
		  }
}
