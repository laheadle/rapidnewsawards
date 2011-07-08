package org.rapidnewsawards.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rapidnewsawards.core.Edition;
import org.rapidnewsawards.core.Response;
import org.rapidnewsawards.core.User;
import org.rapidnewsawards.messages.Name;
import org.rapidnewsawards.messages.RecentSocials;
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
	private static final String SERVER_ERROR = "SERVER_ERROR";

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

		commandsMap.put("ping", new AbstractCommand() {
			@Override
			public Object getResult() throws RNAException {
				return "pong";
			}
		});
		
		// TODO 2.0 Rename to getStory
		commandsMap.put("story", new AbstractCommand() {
			@Override
			public Object getResult() throws RNAException {
				int edition = get("edition", Integer.class);
				long link = get("linkId", Long.class);
				return d.editions.getStory(edition, link);
			}
		});

		commandsMap.put("editorFundings", new AbstractCommand() {
			@Override
			public Object getResult() throws RNAException {
				int edition = get("edition", Integer.class);
				long editor = get("editor", Long.class);
				return d.users.getEditorFundings(edition, editor);
			}
		});

		commandsMap.put("recentFundings", new AbstractCommand() {
			@Override
			public Object getResult() throws RNAException {
				return d.editions.getRecentVotes(get("edition", Integer.class));
			}
		});

		commandsMap.put("topEditors", new AbstractCommand() {
			@Override
			public Object getResult() throws RNAException {
				return d.editions.getTopEditors(get("edition", Integer.class));
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
				int edition = get("edition", Integer.class);
				Edition ed = d.editions.getEdition(edition);
				Boolean on = new Boolean(request.getParameter("on"));
				VoteResult vr = d.users.voteFor(link, fullLink, ed.getKey(), on);
				vr.currentEdition = Edition.getNumber(d.editions.getCurrentEdition().getKey());
				return vr;
			}
		});

		commandsMap.put("welcomeUser", new AbstractCommand() {
			@Override
			public Object getResult() throws RNAException {
				// TODO test
				String nickname = request.getParameter("nickname");
				String consent = request.getParameter("consent");
				String webPage = request.getParameter("webPage");
				return d.users.welcomeUser(nickname, consent, webPage);
			}
		});

		commandsMap.put("submitStory", new AbstractCommand() {
			@Override
			public Object getResult() throws RNAException {
				String url = request.getParameter("url");
				String title = request.getParameter("title");
				Edition ed = d.editions.getCurrentEdition();
				try {
					VoteResult vr = d.editions.submitStory(url, title, ed.getKey());
					vr.currentEdition = ed.getNumber();
					return vr;
				}
				catch (MalformedURLException ex) {
					try {
						VoteResult vr = d.editions.submitStory("http://" + url, title, ed.getKey());
						vr.currentEdition = ed.getNumber();
						return vr;
					}
					catch (MalformedURLException ex2) {
						// TODO Test on frontend
						log.warning("bad url " +  url + "submitted by " + d.user == null? "anon" : d.user.toString());
						throw new RNAException("Malformed URL");
					}
				}
			}
		});
		
		commandsMap.put("donate", new AbstractCommand() {
			@Override
			public Object getResult() throws RNAException {
				String name = request.getParameter("name");
				String donation = request.getParameter("donation");
				String webPage = request.getParameter("webPage");
				String statement = request.getParameter("statement");
				String consent = request.getParameter("consent");
				d.donate(name, donation, webPage, statement, consent);
				return Response.SUCCESS;
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
				long userId = 0L;
				try {
					userId = new Long(request.getParameter("id"));
				}
				catch (NumberFormatException e) {
					throw new RNAException("Bad User Id");
				}
				return d.users.getRelatedUserInfo(Name.AGGREGATOR_NAME, d.user,
						User.createKey(userId));
			}
		});
		commandsMap.put("getFollowers", new AbstractCommand() {
			@Override
			public Object getResult() {
				long userId = new Long(request.getParameter("id"));
				return d.users.getFollowers(User.createKey(userId));
			}
		});

		commandsMap.put("doSocial", new AbstractCommand() {
			@Override
			public Object getResult() throws RNAException {
				String _to = request.getParameter("to");
				Key<User> to = User.createKey(new Long(_to));
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
		public User requester;
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
		Gson g = new Gson();
		final AbstractCommand c = commandsMap.get(fun);
		
		// TODO CONCURRENT mod exceptions
		ResponseMessage re = new ResponseMessage();
		try {
			ConcurrentServletCommand command =
				new ConcurrentServletCommand(ConcurrentServletCommand.FEW, ConcurrentServletCommand.LONG) {
				@Override
				public Object perform(HttpServletRequest request, HttpServletResponse resp)
				throws RNAException {
					return c.perform(request);
				}
			};
			re.payload = command.run(request, resp);
			if (command.getRetries() > 0) {
				log.warning(String.format(
						"command %s needed %d retries.", c, command.getRetries()));
			}
			re.status = OK;
		} catch (RNAException e) {
			re.payload = null;
			re.status = BAD_REQUEST;
			re.message = e.message;
		}
		catch (TooBusyException e) {
			re.payload = null;
			re.status = TRY_AGAIN;
			re.message = "Things are busy...please try again!";
			log.severe(String.format("%s command gave up after %d retries!", c, e.tries));
		}
		catch (Exception e) {
			re.payload = null;
			re.status = SERVER_ERROR;
			re.message = "";
			e.printStackTrace();
		}
		finally {
			re.requester = DAO.instance.user;
			out.println(g.toJson(re));
		}
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
