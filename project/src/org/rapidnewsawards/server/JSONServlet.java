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
	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(DoSomethingServlet.class
			.getName());

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
			defaults.put("edition", -1);
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

		public Object perform(HttpServletRequest request) {
			this.request = request;
			return this.getResult();
		}

		protected abstract Object getResult();

	}

	public static Map<String, AbstractCommand> commandsMap;

	static {
		commandsMap = new HashMap<String, AbstractCommand>();

		commandsMap.put("topStories", new AbstractCommand() {
			public Object getResult() {
				int edition = get("edition", Integer.class);
				TopStories ts = d.editions.getTopStories(edition);
				if (ts.edition == null) {
					ts = d.editions.getTopStories(ts.numEditions - 1);
				}
				assert (ts.numEditions > 0 && ts.edition != null && ts.list != null);
				return ts;
			}
		});

		commandsMap.put("recentSocials", new AbstractCommand() {
			public Object getResult() {
				int edition = get("edition", Integer.class);
				// TODO BUG IN edition = 0
				RecentSocials rs = d.social.getRecentSocials(edition);
				// TODO Thinkme
				if (rs.edition == null) {
					rs = d.social.getRecentSocials(rs.numEditions - 1);
				}
				return rs;
			}
		});

		commandsMap.put("allEditions", new AbstractCommand() {
			public Object getResult() {
				return d.editions.getAllEditions();
			}
		});

		commandsMap.put("story", new AbstractCommand() {
			public Object getResult() {
				int edition = get("edition", Integer.class);
				long link = get("linkId", Long.class);
				return d.editions.getStory(edition, link);
			}
		});

		commandsMap.put("recentFundings", new AbstractCommand() {
			public Object getResult() {
				RecentVotes rv = d.editions.getRecentVotes(get("edition",
						Integer.class));
				return rv;
			}
		});

		commandsMap.put("topJudges", new AbstractCommand() {
			public Object getResult() {
				return d.editions.getTopJudges(get("edition", Integer.class));
			}
		});

		commandsMap.put("grabTitle", new AbstractCommand() {
			public Object getResult() {
				String urlStr = request.getParameter("url");
				return TitleGrabber.getTitle(urlStr);
			}
		});

		commandsMap.put("voteFor", new AbstractCommand() {
			public Object getResult() {
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
			public Object getResult() {
				// TODO test
				if (d.user == null)
					return null;
				String nickname = request.getParameter("nickname");
				return d.users.welcomeUser(nickname, 0);
			}
		});

		commandsMap.put("submitStory", new AbstractCommand() {
			public Object getResult() {
				String url = request.getParameter("url");
				String title = request.getParameter("title");
				Edition ed = d.editions.getCurrentEdition();
				VoteResult vr = d.editions
				.submitStory(url, title, ed.getKey(), d.user);
				return vr;
			}
		});
		commandsMap.put("sendLogoutURL", new AbstractCommand() {
			public Object getResult() {
				UserService userService = UserServiceFactory.getUserService();
				String url = request.getParameter("url");
				return userService.createLogoutURL(url);
			}
		});
		commandsMap.put("relatedUser", new AbstractCommand() {
			public Object getResult() {
				long userId = new Long(request.getParameter("id"));
				return d.users.getRelatedUserInfo(Name.AGGREGATOR_NAME, d.user,
						new Key<User>(User.class, userId));
			}
		});
		commandsMap.put("getFollowers", new AbstractCommand() {
			public Object getResult() {
				long userId = new Long(request.getParameter("id"));
				return d.users.getFollowers(new Key<User>(User.class, userId));
			}
		});

		commandsMap.put("doSocial", new AbstractCommand() {
			public Object getResult() {
				String _to = request.getParameter("to");
				String _edition = request.getParameter("edition");
				Key<User> to = User.getKey(new Long(_to));
				// TODO 2.0 pass in from client
				Boolean on = new Boolean(request.getParameter("on"));
				return d.social.doSocial(to, on).s;
			}
		});

		commandsMap.put("sendUser", new AbstractCommand() {
			public Object getResult() {
				return d.user;
			}
		});

		commandsMap.put("sendLoginURL", new AbstractCommand() {
			public Object getResult() {
				String url = request.getParameter("url");
				UserService userService = UserServiceFactory.getUserService();
				return userService.createLoginURL(url);
			}
		});
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
		AbstractCommand c = commandsMap.get(fun);
		
		// TODO CONCURRENT mod exceptions
		out.println(g.toJson(c.perform(request)));
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
