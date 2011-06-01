package org.rapidnewsawards.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rapidnewsawards.core.Edition;
import org.rapidnewsawards.core.User;
import org.rapidnewsawards.messages.Name;
import org.rapidnewsawards.messages.RecentSocials;
import org.rapidnewsawards.messages.RecentVotes;
import org.rapidnewsawards.messages.TopStories;
import org.rapidnewsawards.messages.VoteResult;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gson.Gson;
import com.googlecode.objectify.Key;

public class JSONServlet extends HttpServlet {
	private static final String OK = "OK";
	private static final String BAD_REQUEST = "BAD_REQUEST";
	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(JSONServlet.class
			.getName());
	private static final String TRY_AGAIN = "TRY_AGAIN";

	private static abstract class Parser {
		public abstract Object parse(String value);
	}

	private static abstract class AbstractCommand {
		HttpServletRequest request;
		public static Map<String, Object> defaults = new HashMap<String, Object>();
		private static Map<Class<?>, Parser> parsers = new HashMap<Class<?>, Parser>();
		public DAO d;

		public AbstractCommand() {
			this.d = DAO.instance;
		}
		
		static {
			defaults.put("edition", DAO.Editions.CURRENT_OR_FINAL);
			parsers.put(Integer.class, new Parser() {
				public Object parse(String value) {
					return Integer.decode(value);
				}
			});
			parsers.put(Long.class, new Parser() {
				public Object parse(String value) {
					return Long.decode(value);
				}
			});
		}

		public <T> T get(String key, Class<T> clazz) {
			String value = request.getParameter(key);
			if (value == null) {
				return clazz.cast(defaults.get(key));
			}
			// TODO handle null pointer returned (throws)
			return clazz.cast(parsers.get(clazz).parse(value));
		}

		public Object perform(HttpServletRequest request) throws RNAException {
			this.request = request;
			return this.getResult();
		}

		@Override
		public String toString() {
			if (this.request != null) {
				return "Command " + this.request;
			}
			else {
				return "Command <?>";
			}
		}

		protected abstract Object getResult() throws RNAException;

	}

	public static Map<String, AbstractCommand> commandsMap;

	static {
		commandsMap = new HashMap<String, AbstractCommand>();

		commandsMap.put("topStories", new AbstractCommand() {
			@Override
			public Object getResult() throws RNAException {
				int edition = get("edition", Integer.class);
				TopStories ts = d.editions.getTopStories(edition);
				assert (ts.numEditions > 0 && ts.edition != null && ts.list != null);
				return ts;
			}
		});

		commandsMap.put("recentSocials", new AbstractCommand() {
			@Override
			public Object getResult() throws RNAException {
				int edition = get("edition", Integer.class);
				RecentSocials rs = d.social.getRecentSocials(edition);
				return rs;
			}
		});

		commandsMap.put("allEditions", new AbstractCommand() {
			@Override
			public Object getResult() {
				return d.editions.getAllEditions();
			}
		});

		commandsMap.put("story", new AbstractCommand() {
			@Override
			public Object getResult() {
				int edition = get("edition", Integer.class);
				long link = get("linkId", Long.class);
				return d.editions.getStory(edition, link);
			}
		});

		commandsMap.put("recentFundings", new AbstractCommand() {
			@Override
			public Object getResult() throws RNAException {
				return d.editions.getRecentVotes(get("edition", Integer.class));
			}
		});

		commandsMap.put("topJudges", new AbstractCommand() {
			@Override
			public Object getResult() throws RNAException {
				return d.editions.getTopJudges(get("edition", Integer.class));
			}
		});

		commandsMap.put("grabTitle", new AbstractCommand() {
			@Override
			public Object getResult() {
				String urlStr = request.getParameter("url");
				return TitleGrabber.getTitle(urlStr);
			}
		});

		commandsMap.put("voteFor", new AbstractCommand() {
			@Override
			public Object getResult() throws RNAException {
				// TODO call typed interface, standardize exceptions 
				// -- e.g. BadRequestException(Response to client)
				String link = request.getParameter("link");
				String fullLink = request.getParameter("fullLink");
				Edition ed = d.editions.getCurrentEdition();
				Boolean on = new Boolean(request.getParameter("on"));
				VoteResult vr = d.users.voteFor(link, fullLink, ed.getKey(), on);
				return vr;
			}
		});

		commandsMap.put("welcomeUser", new AbstractCommand() {
			@Override
			public Object getResult() throws RNAException {
				// TODO test
				if (d.user == null)
					return null;
				String nickname = request.getParameter("nickname");
				return d.users.welcomeUser(nickname, 0);
			}
		});

		commandsMap.put("submitStory", new AbstractCommand() {
			@Override
			public Object getResult() throws RNAException {
				String url = request.getParameter("url");
				String title = request.getParameter("title");
				Edition ed = d.editions.getCurrentEdition();
				VoteResult vr = d.editions
				.submitStory(url, title, ed.getKey(), d.user);
				return vr;
			}
		});
		commandsMap.put("sendLogoutURL", new AbstractCommand() {
			@Override
			public Object getResult() {
				UserService userService = UserServiceFactory.getUserService();
				String url = request.getParameter("url");
				return userService.createLogoutURL(url);
			}
		});
		commandsMap.put("relatedUser", new AbstractCommand() {
			@Override
			public Object getResult() throws RNAException {
				long userId = new Long(request.getParameter("id"));
				return d.users.getRelatedUserInfo(Name.AGGREGATOR_NAME, d.user,
						new Key<User>(User.class, userId));
			}
		});
		commandsMap.put("getFollowers", new AbstractCommand() {
			@Override
			public Object getResult() {
				long userId = new Long(request.getParameter("id"));
				return d.users.getFollowers(new Key<User>(User.class, userId));
			}
		});

		commandsMap.put("doSocial", new AbstractCommand() {
			@Override
			public Object getResult() throws RNAException {
				String _to = request.getParameter("to");
				Key<User> to = User.getKey(new Long(_to));
				// TODO 2.0 pass in from client
				Boolean on = new Boolean(request.getParameter("on"));
				return d.social.doSocial(to, on).s;
			}
		});

		commandsMap.put("sendUser", new AbstractCommand() {
			@Override
			public Object getResult() {
				return d.user;
			}
		});

		commandsMap.put("sendLoginURL", new AbstractCommand() {
			@Override
			public Object getResult() {
				String url = request.getParameter("url");
				UserService userService = UserServiceFactory.getUserService();
				return userService.createLoginURL(url);
			}
		});
	}

	class ResponseMessage {
		public String status;
		public String message;
		public Object payload;
	}
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse resp)
			throws ServletException, IOException {
		String fun = request.getParameter("fun");
		PrintWriter out = resp.getWriter();
		if (fun == null) {
			out.println("invalid function");
			return;
		}
		JSONServlet.log.info("Json Function: " + fun);
		Gson g = new Gson();
		final AbstractCommand c = commandsMap.get(fun);
		
		// TODO CONCURRENT mod exceptions
		ResponseMessage re = new ResponseMessage();
		try {
			ConcurrentServletCommand command = new ConcurrentServletCommand() {
				@Override
				public Object perform(HttpServletRequest request, HttpServletResponse resp) 
				throws RNAException {
					return c.perform(request);	
				}
			};
			re.payload = command.run(request, resp);
			if (command.retries > 0) {
				log.warning(String.format(
						"command %s needed %d retries.", c, command.retries));
			}
			re.status = OK;
			out.println(g.toJson(re));
		} catch (RNAException e) {
			re.payload = null;
			re.status = BAD_REQUEST;
			re.message = e.message;
			out.println(g.toJson(re));
		}
		catch (TooBusyException e) {
			re.payload = null;
			re.status = TRY_AGAIN;
			re.message = "Things are busy...please try again!";
			log.severe(String.format("%s command gave up after %d retries!", c, e.tries));
			out.println(g.toJson(re));
		}
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
