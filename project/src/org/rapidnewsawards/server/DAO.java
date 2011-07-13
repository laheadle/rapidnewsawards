package org.rapidnewsawards.server;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheException;
import net.sf.jsr107cache.CacheManager;

import org.rapidnewsawards.core.Donation;
import org.rapidnewsawards.core.Edition;
import org.rapidnewsawards.core.EditorInfluence;
import org.rapidnewsawards.core.EditorVote;
import org.rapidnewsawards.core.Follow;
import org.rapidnewsawards.core.FollowedBy;
import org.rapidnewsawards.core.JudgeInfluence;
import org.rapidnewsawards.core.Link;
import org.rapidnewsawards.core.Periodical;
import org.rapidnewsawards.core.Response;
import org.rapidnewsawards.core.Root;
import org.rapidnewsawards.core.ScoreRoot;
import org.rapidnewsawards.core.ScoreSpace;
import org.rapidnewsawards.core.ScoredLink;
import org.rapidnewsawards.core.SocialEvent;
import org.rapidnewsawards.core.User;
import org.rapidnewsawards.core.Vote;
import org.rapidnewsawards.messages.AllEditions;
import org.rapidnewsawards.messages.EditionMessage;
import org.rapidnewsawards.messages.EditorFundings;
import org.rapidnewsawards.messages.FullStoryInfo;
import org.rapidnewsawards.messages.InfluenceMessage;
import org.rapidnewsawards.messages.Name;
import org.rapidnewsawards.messages.RecentSocials;
import org.rapidnewsawards.messages.RecentVotes;
import org.rapidnewsawards.messages.RelatedUserInfo;
import org.rapidnewsawards.messages.SocialInfo;
import org.rapidnewsawards.messages.StoryInfo;
import org.rapidnewsawards.messages.TopEditors;
import org.rapidnewsawards.messages.TopJudges;
import org.rapidnewsawards.messages.TopStories;
import org.rapidnewsawards.messages.UserInfo;
import org.rapidnewsawards.messages.User_Vote_Link;
import org.rapidnewsawards.messages.VoteResult;
import org.rapidnewsawards.messages.Vote_Link;

import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.repackaged.com.google.common.base.Strings;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.NotFoundException;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Query;
import com.googlecode.objectify.util.DAOBase;

public class DAO extends DAOBase {

	private static final int HUGE_DONATION_DOLLARS = 5000;

	private static final int CENTS_PER_DOLLAR = 100;

	static {
		ObjectifyService.factory().register(Donation.class);
		ObjectifyService.factory().register(Edition.class);
		ObjectifyService.factory().register(EditorInfluence.class);
		ObjectifyService.factory().register(EditorVote.class);
		ObjectifyService.factory().register(Follow.class);
		ObjectifyService.factory().register(FollowedBy.class);
		ObjectifyService.factory().register(JudgeInfluence.class);
		ObjectifyService.factory().register(Link.class);
		ObjectifyService.factory().register(Periodical.class);
		ObjectifyService.factory().register(Root.class);
		ObjectifyService.factory().register(ScoredLink.class);
		// TODO Replace with edition?
		ObjectifyService.factory().register(ScoreRoot.class);
		ObjectifyService.factory().register(ScoreSpace.class);
		ObjectifyService.factory().register(SocialEvent.class);
		ObjectifyService.factory().register(User.class);
		ObjectifyService.factory().register(Vote.class);
	}
	public static DAO instance = new DAO();

	/*
	 * The currently logged-in user. See AuthFilter
	 */
	public User user;
	
	public static final Name periodicalName = Name.AGGREGATOR_NAME;

	/*
	 * Sub-modules for data access
	 */
	public Social social;

	public Transition transition;

	public Users users;

	public Editions editions;

	private Cache cache;

	public boolean useCacheLock = false;
	
	public static final Logger log = Logger.getLogger(DAO.class.getName());

	static {
		log.addHandler(new ErrorMailer());
	}
	
	public DAO() {
        try {
            cache = CacheManager.getInstance().getCacheFactory().createCache(Collections.emptyMap());
            cache.put("locked", Boolean.FALSE);
        } catch (Exception e) {
        	log.severe("unable to init cache! " + e.toString());
        	cache = null;
        }
	
		user = null;
		social = new Social();
		transition = new Transition();
		editions = new Editions();
		users = new Users();
	}
	
	
	public class Editions {

		// TODO 2.0 make enums
		public static final int INITIAL = 0;
		public static final int CURRENT = -1;
		public static final int NEXT = -2;
		public static final int FINAL = -3;
		static final int PREVIOUS = -4;
		public static final int CURRENT_OR_FINAL = -5;

		private EditionMessage makeEditionMessage(Objectify ofy, Edition e) {
			return new EditionMessage(e,
					ofy.get(ScoreSpace.keyFromEditionKey(e.getKey())));		
		}


		// TODO parameterize by periodical
		public AllEditions getAllEditions() {
			LinkedList<EditionMessage> ll = new LinkedList<EditionMessage>();
			Map<Integer, ScoreSpace> spaces = 
				new HashMap<Integer, ScoreSpace>(); 
			for (ScoreSpace s : ofy().query(ScoreSpace.class)) {
				spaces.put(s.getNumber(), s);
			}
			for (Edition e : ofy().query(Edition.class)) {
				ll.add(new EditionMessage(e, spaces.get(e.getNumber())));
			}
			
			try {
				Edition c = getCurrentEdition();
				EditionMessage em = new EditionMessage(c, spaces.get(c.getNumber()));
				return new AllEditions(ll, em);
			} catch (RNAException e1) {
				return new AllEditions(ll, null);
			}
		}

		public Edition getCurrentEdition() throws RNAException {
			return getEdition(CURRENT);
		}

		/*
		 * Get the requested edition
		 * 
		 * @param number the edition number requested
		 */
		public Edition getEdition(final int editionNum) throws RNAException {

			final Objectify o = ofy();
			final Periodical p = getPeriodical();

			if (editionNum == CURRENT || editionNum == CURRENT_OR_FINAL) {
				if (p.isFinished()) {
					if (editionNum == CURRENT_OR_FINAL) {
						return getEdition(FINAL);
					}
					else {
						throw new RNAException("No next edition, that's all folks.");
					}
				}
				return o.get(p.getcurrentEditionKey());
			}

			else if (editionNum == NEXT) {
				if (p.isFinished()) {
					throw new RNAException(
							"After-Next edition requested for finished periodical");
				}
				Key<Edition> nextKey = Edition.getNextKey(p.getcurrentEditionKey());
				if (Edition.isAfterFinal(nextKey, getNumEditions())) {
					throw new RNAException(
					"There is no edition after the final one");					
				}
				return o.get(nextKey);
			}
			else if (editionNum == FINAL) {
				return o.get(Edition.getFinalKey(getNumEditions()));
			}
			else if (editionNum == PREVIOUS) {
				if (p.isFinished())	
					return getEdition(FINAL);
				if (Edition.getNumber(p.getcurrentEditionKey()) == INITIAL) {
					throw new RNAException("There is no current edition because nothing has been published. Now is the time for joining and following.");
				}
				return o.get(Edition.getPreviousKey(p.getcurrentEditionKey()));
			}
			else {
				// TODO 2.0 this only works because we assume one periodical
				return o.get(Edition.class, "" + editionNum);
			}
		}

		// TODO refactor with getvoters
		public LinkedList<InfluenceMessage> getJudges(Objectify txn, Edition e) {
			LinkedList<InfluenceMessage> result = new LinkedList<InfluenceMessage>();

			Map<Key<User>, Integer> funds = new HashMap<Key<User>, Integer>();
			ArrayList<Key<User>> judges = new ArrayList<Key<User>>();

			ScoreSpace space = getScoreSpace(txn, e.getKey());

			for (JudgeInfluence ji : txn.query(
					JudgeInfluence.class).ancestor(ScoreSpace.keyFromEditionKey(e.getKey()))) {
				funds.put(ji.user, funding(ji.score, space.totalScore, space.balance));
				judges.add(ji.user);
			}
			txn.getTxn().commit();

			if (judges.size() > 0) {
				Map<Key<User>, User> vmap = ofy().get(judges);

				for (int i = 0; i < judges.size(); i++) {
					result.add(new InfluenceMessage(vmap.get(judges.get(i)),
							funds.get(judges.get(i))));
				}

				Collections.sort(result);
			}
			
			return result;
		}

		// TODO refactor with getvoters
		public LinkedList<InfluenceMessage> getTopEditors(Objectify txn, Edition e) {
			LinkedList<InfluenceMessage> result = new LinkedList<InfluenceMessage>();

			Map<Key<User>, Integer> funds = new HashMap<Key<User>, Integer>();
			ArrayList<Key<User>> editors = new ArrayList<Key<User>>();

			ScoreSpace space = getScoreSpace(txn, e.getKey());
			for (EditorInfluence ei : txn.query(
					EditorInfluence.class).ancestor(ScoreSpace.keyFromEditionKey(e.getKey()))) {
				funds.put(ei.editor, funding(ei.score, space.totalScore, space.balance));
				editors.add(ei.editor);
			}
			txn.getTxn().commit();

			if (editors.size() > 0) {
				Map<Key<User>, User> vmap = ofy().get(editors);

				for (int i = 0; i < editors.size(); i++) {
					result.add(new InfluenceMessage(vmap.get(editors.get(i)),
							funds.get(editors.get(i))));
				}

				Collections.sort(result);
			}
			
			return result;
		}

		private LinkedList<SocialInfo> getLatestSocialInfoInEdition(Edition e) {
			return editions.createSocialInfoList(ofy().query(SocialEvent.class).filter("edition", e.getKey()).order("-time"));
		}

		public LinkedList<SocialInfo> getLatestSocialInfoforEditor(Key<User> user) {
			return editions.createSocialInfoList(ofy().query(SocialEvent.class).ancestor(user).order("-time"));
		}

		/*
		 * Runs three queries: first get keys, then use the keys to get 2 sets
		 * of entities
		 */
		private LinkedList<SocialInfo> createSocialInfoList(Query<SocialEvent> q) {
			LinkedList<SocialInfo> result = new LinkedList<SocialInfo>();
			ArrayList<Key<User>> editors = new ArrayList<Key<User>>();
			ArrayList<Key<User>> judges = new ArrayList<Key<User>>();
			ArrayList<Boolean> bools = new ArrayList<Boolean>();
			// think - does this show everything we want?
			// todo ignore welcomes on eds and donors

			ArrayList<Date> times = new ArrayList<Date>();
			for (SocialEvent event : q) {
				editors.add(event.editor);
				judges.add(event.judge);
				bools.add(event.on);
				times.add(event.time);
			}
			Map<Key<User>, User> umap = ofy().get(editors);
			Map<Key<User>, User> lmap = ofy().get(judges);

			for (int i = 0; i < editors.size(); i++) {
				result.add(new SocialInfo(umap.get(editors.get(i)), lmap
						.get(judges.get(i)), bools.get(i), times.get(i)));
			}
			return result;
		}

		/*
		 * Runs three queries: first get keys, then use the keys to get 2 sets
		 * of entities
		 */
		public LinkedList<User_Vote_Link> getLatestUser_Vote_Links(Key<Edition> e) {
			LinkedList<Vote> votes = new LinkedList<Vote>();
			for (Vote v : ofy().query(Vote.class).filter("edition", e).order("-time")) {
				votes.add(v);
			}
			return getUser_Vote_Links(votes);
		}

		public LinkedList<User_Vote_Link> getLatestUser_Vote_Links(Key<Edition> edition, Key<User> editor) {
			Set<Key<Vote>> votekeys = new HashSet<Key<Vote>>();
			for(EditorVote ev : ofy().query(EditorVote.class).filter("editor", editor)) {
				votekeys.add(ev.vote);
			}
			List<Vote> votes = new LinkedList<Vote>(ofy().get(votekeys).values());
			Collections.sort(votes);
			return getUser_Vote_Links(votes);
		}

		/*
		 * Runs three queries: first get keys, then use the keys to get 2 sets
		 * of entities
		 */
		public LinkedList<User_Vote_Link> getUser_Vote_Links(List<Vote> votes) {
			LinkedList<User_Vote_Link> result = new LinkedList<User_Vote_Link>();

			ArrayList<Key<User>> users = new ArrayList<Key<User>>();
			ArrayList<Key<Link>> links = new ArrayList<Key<Link>>();

			for (Vote v : votes) {
				users.add(v.voter);
				links.add(v.link);
			}

			Map<Key<User>, User> umap = ofy().get(users);
			Map<Key<Link>, Link> lmap = ofy().get(links);

			int i = 0;
			for (Vote v : votes) { // iterate again in the same order
				result.add(new User_Vote_Link(umap.get(users.get(i)), v, lmap
						.get(links.get(i))));
				i++;
			}

			return result;
		}

		public Edition getNextEdition() throws RNAException {
			return getEdition(NEXT);
		}

		// cache the number of editions -- will NEVER change.
		private int numEditions = -1;

		public int getNumEditions() {
			if (numEditions == -1) {
				numEditions = getPeriodical().numEditions;
			}
			return numEditions;
		}

		public RecentVotes getRecentVotes(int edition) throws RNAException {
			Edition e = getEdition(edition);
			RecentVotes recent = new RecentVotes();
			// no aggregate score info needed
			recent.edition = makeEditionMessage(ofy(), e);
			recent.numEditions = getNumEditions();
			recent.isCurrent = editions.isPrevious(e);
			recent.isNext = editions.isCurrent(e);
			recent.list = getLatestUser_Vote_Links(e.getKey());
			return recent;
		}

		public ScoredLink getScoredLink(Objectify ofy, Key<Edition> e, Key<Link> l) {
			return ofy.query(ScoredLink.class).ancestor(ScoreRoot.keyFromEditionKey(e))
					.filter("link", l).get();
		}

		public LinkedList<ScoredLink> getScoredLinks(Objectify ofy, Edition e, int minScore) {
			LinkedList<ScoredLink> result = new LinkedList<ScoredLink>();
			if (e == null)
				return result;

			for (ScoredLink sl : ofy.query(ScoredLink.class).ancestor(ScoreRoot.keyFromEditionKey(e.getKey()))
					.filter("score >=", minScore).order("-score")) {
				result.add(sl);
			}
			return result;
		}

		public FullStoryInfo getStory(int editionNum, Long linkId) throws RNAException {
			Key<Link> linkKey = Link.createKey(linkId);
			Key<Edition> editionKey = Edition.createKey(editionNum);

			// Edition e = getEdition(Name.AGGREGATOR_NAME, editionNum, null);
			Objectify txn = fact().beginTransaction();
			ScoredLink sl = editions.getScoredLink(txn, editionKey, linkKey);

			Link link = ofy().find(linkKey);
			if (link == null || sl == null) {
				txn.getTxn().rollback();
				throw new RNAException("Story Link not found");
			}

			ScoreSpace space = getScoreSpace(txn, editionKey);
			StoryInfo si = new StoryInfo();
			si.link = link;
			si.score = sl.score;
			si.edition = makeEditionMessage(ofy(), getEdition(editionNum));
			si.submitter = ofy().get(link.submitter);
			si.setFunding(funding(sl.score, space.totalScore, space.balance));
			si.isCurrent = isCurrent(editionKey);
			
			if (user != null) {
				si.userIsFunding = users.hasVoted(user, editionKey, link);
			}

			FullStoryInfo fsi = new FullStoryInfo();
			fsi.info = si;
			fsi.funds = getVoters(txn, linkKey, editionKey);
			txn.getTxn().commit();
			return fsi;
		}


		private boolean isCurrent(Key<Edition> editionKey) {
			try {
				return editions.getEdition(CURRENT).getKey().equals(editionKey);
			} catch (RNAException e) {
				return false;
			}
		}

		public TopJudges getTopJudges(int edition) throws RNAException {
			Edition e = editions.getEdition(edition);
			TopJudges tj = new TopJudges();
			Objectify txn = fact().beginTransaction();
			tj.edition = makeEditionMessage(txn, e);
			tj.isCurrent = editions.isPrevious(e);
			tj.isNext = editions.isCurrent(e);
			tj.numEditions = editions.getNumEditions();
			tj.list = getJudges(txn, e);
			return tj;
		}

		public TopEditors getTopEditors(int edition) throws RNAException {
			Edition e = editions.getEdition(edition);
			TopEditors tj = new TopEditors();
			Objectify txn = fact().beginTransaction();
			tj.edition = makeEditionMessage(txn, e);
			tj.isCurrent = editions.isPrevious(e);
			tj.isNext = editions.isCurrent(e);
			tj.numEditions = editions.getNumEditions();
			tj.list = getTopEditors(txn, e);
			return tj;
		}

		
		public TopStories getTopStories(int editionNum) throws RNAException {
			Edition e = editions.getEdition(editionNum);
			Objectify txn = fact().beginTransaction();
			TopStories result = new TopStories();
			LinkedList<StoryInfo> stories = new LinkedList<StoryInfo>();
			result.edition = makeEditionMessage(txn, e);
			result.numEditions = editions.getNumEditions();
			result.isCurrent = editions.isPrevious(e);
			result.isNext = editions.isCurrent(e);
			result.list = stories;
			LinkedList<ScoredLink> scored = editions.getScoredLinks(txn, e, 1);
			LinkedList<Key<Link>> linkKeys = new LinkedList<Key<Link>>();

			for (ScoredLink sl : scored) {
				linkKeys.add(sl.link);
			}

			Map<Key<Link>, Link> linkMap = ofy().get(linkKeys);

			// for the submitter of each vote
			LinkedList<Key<User>> submitterKeys = new LinkedList<Key<User>>();

			for (Link l : linkMap.values()) {
				submitterKeys.add(l.submitter);
			}

			Map<Key<User>, User> userMap = ofy().get(submitterKeys);

			ScoreSpace space = getScoreSpace(txn, e.getKey());
			txn.getTxn().commit();

			for (ScoredLink sl : scored) {
				StoryInfo si = new StoryInfo();
				si.link = linkMap.get(sl.link);
				si.score = sl.score;
				si.edition = makeEditionMessage(ofy(), e);
				si.submitter = userMap.get(si.link.submitter);
				si.setFunding(funding(sl.score, space.totalScore, space.balance));
				stories.add(si);
			}

			return result;
		}

		private boolean isPrevious(Edition e) {
			try {
				return getEdition(PREVIOUS).getKey().equals(e.getKey());
			} catch (RNAException e1) {
				return false;
			}
		}


		private boolean isCurrent(Edition e) {
			try {
				return getEdition(CURRENT).getKey().equals(e.getKey());
			} catch (RNAException e1) {
				return false;
			}
		}


		public VoteResult submitStory(String url, String title,
				Key<Edition> e) throws RNAException, MalformedURLException {
			// TODO put this and vote in transaction along with task
			VoteResult vr = new VoteResult();

			if (user == null) {
				vr.returnVal = Response.NOT_LOGGED_IN;
				// TODO Think
				vr.authUrl = null; // userService.createLoginURL(fullLink);
				return vr;
			}
			if (user.isEditor) {
				vr.returnVal = Response.ONLY_JUDGES_CAN_VOTE;
				return vr;
			}
			if (url.length() > 499) {
				vr.returnVal = Response.URL_TOO_LONG;
				return vr;				
			}
			if (title.length() > 499) {
				vr.returnVal = Response.TITLE_TOO_LONG;
				return vr;	
			}
			else {
				Link l = users.createLink(url, title, user.getKey());
				vr.linkId = l.id;
				vr.returnVal = users.voteFor(user, e, l, true);
			}
			return vr;
		}

		public LinkedList<InfluenceMessage> getVoters(Objectify txn, Key<Link> l, Key<Edition> e) {
			LinkedList<InfluenceMessage> result = new LinkedList<InfluenceMessage>();

			Map<Key<User>, Integer> authorities = new HashMap<Key<User>, Integer>();
			Map<Key<User>, Set<Key<User>>> followers = new HashMap<Key<User>, Set<Key<User>>>();
			
			ArrayList<Key<User>> voters = new ArrayList<Key<User>>();

			for (Vote v : ofy().query(Vote.class).filter("link", l)
					.filter("edition", e)) {
				authorities.put(v.voter, v.authority);
				voters.add(v.voter);
			}

			if (voters.size() == 0) {
				log.warning("requested voters for empty link " + l);
				return result;
			}

			ScoreSpace space = getScoreSpace(txn, e);
			Map<Key<User>, User> vmap = ofy().get(voters);

			// fetch all supporting editors now -- avoid linear query for followers per voter
			for(EditorVote ev : ofy().query(EditorVote.class).filter("link", l).filter("edition", e)) {
				Set<Key<User>> eds = followers.get(ev.judge);
				if (eds == null) {
					eds = new HashSet<Key<User>>();
				}
				eds.add(ev.editor);
				followers.put(ev.judge, eds);
			}
			
			Set<Key<User>> allEds = new HashSet<Key<User>>();
			for(Set<Key<User>> sku : followers.values()) {
				for (Key<User> ku : sku) {
					if (!allEds.contains(ku)) {
						allEds.add(ku);
					}
				}
			}
			Map<Key<User>, User> allEdObjs = ofy().get(allEds);
			
			for (int i = 0; i < voters.size(); i++) {
				InfluenceMessage message = new InfluenceMessage(vmap.get(voters.get(i)),
						funding(authorities.get(voters.get(i)), space.totalScore, space.balance));
				addSupportingEditors(message, followers.get(voters.get(i)), allEdObjs);
				result.add(message);
			}
			Collections.sort(result);
			return result;
		}

		private void addSupportingEditors(InfluenceMessage message, Set<Key<User>> followers, Map<Key<User>, User> allEdObjs) {
			LinkedList<User> support = new LinkedList<User>();
			if (!message.user.isEditor && followers != null) {
				for (Key<User> ku : followers) {
					support.add(allEdObjs.get(ku));
				}
			}
			message.supportingEditors = support;
		}


		public ScoreSpace getScoreSpace(Objectify ofy, Key<Edition> key) {
			Key<ScoreRoot> sroot = new Key<ScoreRoot>(ScoreRoot.class, key.getName());
			Key<ScoreSpace> skey = new Key<ScoreSpace>(sroot,
					ScoreSpace.class, key.getName());
			ScoreSpace s;
			if ((s = ofy.find(skey)) == null) { throw new IllegalStateException(); }
			return s;
		}

		public void setSpaceBalance(int edition, int balance) {
			assert(getPeriodical().inTransition);
			Objectify oTxn = fact().beginTransaction();
			ScoreSpace s = getScoreSpace(oTxn, Edition.createKey(edition));
			s.balance = balance;
			oTxn.put(s);
			TransitionTask.finish(oTxn.getTxn());
			oTxn.getTxn().commit();
		}


		public void setEditionFinished(int edition) throws RNAException {
			Objectify oTxn = fact().beginTransaction();
			ScoreSpace s = getScoreSpace(oTxn, Edition.createKey(edition));
			s.finished = true;
			oTxn.put(s);
			if (getPeriodical().isFinished()) {
				TransitionTask.finish(oTxn.getTxn());
			}
			else {
				TransitionTask.setPeriodicalBalance(oTxn.getTxn());
			}
			oTxn.getTxn().commit();
		}

	}

	private class LockedPeriodical {
		public final Objectify transaction;
		public final Periodical periodical;

		public void checkState() {
			if (periodical.inTransition && periodical.userlocked) {
				throw new IllegalStateException();				
			}
		}
		
		public LockedPeriodical() throws RNAException {		
			Objectify oTxn = fact().beginTransaction();
			
			// retry task on nullpointer
			// TODO 2.0 assumes one periodical
			Periodical p = oTxn.query(Periodical.class).ancestor(Periodical.rootKey()).get();
			if (p.isFinished() && !p.inTransition) {
				throw new RNAException("The periodical is finished");
			}
			this.transaction = oTxn;
			this.periodical = p;
			checkState();
		}

		public void commit() {
			checkState();
			this.transaction.put(this.periodical);
			this.transaction.getTxn().commit();
		}
		public void rollback() {
			this.transaction.getTxn().rollback();
		}

		public void releaseUserLock() {
			this.periodical.userlocked = false;
		}

		public void setUserLock() {
			this.periodical.userlocked = true;
		}
	}

	public class Social {

		public void writeSocialEvent(
				final Key<User> from, final Key<User> to, final Key<Edition> e, 
				boolean on, boolean cancelPending) throws RNAException {
			assert(getPeriodical().userlocked);
			assert(!getPeriodical().inTransition);
			if (from.equals(to)) {
				throw new RNAException("BUG! can't follow self");
			}
			final Objectify txn = fact().beginTransaction();
			
			Set<Key<Edition>> laters = getThisAndFutureEditionKeys(e);

			if (cancelPending) {
				// cancel follow: delete future social, delete future follows
				// cancel unfollow: delete future social, insert future follows
				QueryResultIterable<SocialEvent> queryResult = txn.query(SocialEvent.class)
				.ancestor(from).filter("judge", to).filter("edition", e)
				.fetch();
				assert(queryResult.iterator().hasNext());
				SocialEvent social = queryResult.iterator().next();
				assert(on ? !social.on : social.on);					
				txn.delete(queryResult);
				if (on) {
					// cancel unfollow
					assert(!social.on);					
					insertFutureFollows(from, to, txn, laters, social);					
				}
				else {
					// cancel follow
					assert(social.on);
					deleteFutureFollows(from, to, e, txn);
				}
			}
			else {
				// follow: insert future social event, insert future follows
				// unfollow: insert future social event, delete future follows
				SocialEvent social = new SocialEvent(from, to, e, new Date(), on);
				txn.put(social);				

				if (on) {
					insertFutureFollows(from, to, txn, laters, social);
				}
				else {
					deleteFutureFollows(from, to, e, txn);

				}
			}
			SocialTask.writeFutureFollowedBys(to, from, e, on, txn.getTxn());
			txn.getTxn().commit();
		}

		public void writeFutureFollowedBys(
				final Key<User> judge, final Key<User> editor, final Key<Edition> e, 
				boolean on) throws RNAException {
			assert(getPeriodical().userlocked);
			assert(!getPeriodical().inTransition);
			if (editor.equals(judge)) {
				throw new RNAException("BUG! can't follow self");
			}
			final Objectify txn = fact().beginTransaction();

			Set<Key<Edition>> laters = getThisAndFutureEditionKeys(e);

			// follow or cancel unfollow: insert future followedbys
			if (on) {
				insertFutureFollowedBys(judge, editor, txn, laters);					
			}
			// unfollow or cancel follow: delete future followedbys
			else {
				deleteFutureFollowedBys(judge, editor, e, txn);
			}

			int amount = on ? 1 : -1;
			SocialTask.changePendingAuthority(judge, e, amount, txn.getTxn());
			txn.getTxn().commit();
		}

		
		private void deleteFutureFollowedBys(Key<User> judge, Key<User> editor,
				Key<Edition> e, Objectify txn) {
			txn.delete(txn.query(FollowedBy.class)
					.ancestor(judge).filter("editor", editor)
					.filter("edition >=", e).fetchKeys());
		}

		private void insertFutureFollowedBys(Key<User> judge, Key<User> editor,
				Objectify txn, Set<Key<Edition>> laters) {
			List<FollowedBy> fols = new LinkedList<FollowedBy>();
			for (Key<Edition> later : laters) {
				fols.add(new FollowedBy(judge, editor, later));
			}
			txn.put(fols);
		}

		private Set<Key<Edition>> getThisAndFutureEditionKeys(final Key<Edition> e) {
			Set<Key<Edition>> result = new HashSet<Key<Edition>>();
			for (Key<Edition> ek : ofy().query(Edition.class).fetchKeys()) {
				if (Edition.getNumber(ek) >= Edition.getNumber(e)) {
					result.add(ek);
				}
			}
			return result;
		}

		private void deleteFutureFollows(final Key<User> from, final Key<User> to,
				final Key<Edition> e, final Objectify txn) {
			txn.delete(txn.query(Follow.class)
					.ancestor(from).filter("judge", to).filter("edition >=", e).fetchKeys());
		}

		private void insertFutureFollows(final Key<User> from, final Key<User> to,
				final Objectify txn, final Set<Key<Edition>> laters,
				SocialEvent social) {
			List<Follow> fols = new LinkedList<Follow>();
			for (Key<Edition> later : laters) {
				fols.add(new Follow(from, to, later, social.getKey()));
			}
			txn.put(fols);
		}

		/* Wrapper which assumes a <from> of the current user */
		public Response doSocial(Key<User> to, boolean on) throws RNAException {
			if (user == null) {
				log.warning("attempt to follow with null user");
				return Response.ILLEGAL_OPERATION;
			}
			Key<User> from = user.getKey();
			if (from.equals(to)) {
				throw new RNAException("can't follow self");
			}
			if (isCacheLocked() && useCacheLock ) {
				throw new CacheWait();
			}
			
			return doSocial(from, to, editions.getEdition(
					Editions.CURRENT_OR_FINAL).getKey(), on);
		}
		
		/* Do a follow, unfollow, or cancel pending follow, unfollow */
		public Response doSocial(Key<User> from, Key<User> to, Key<Edition> e, boolean on) 
		throws RNAException {

			if (Edition.isFinal(e, editions.getNumEditions())){
				log.warning(String.format(
						"Attempted to socialize during final edition: User %s, Edition %s",
						user, e));
				return Response.FORBIDDEN_DURING_FINAL;
			}

			Objectify socialTxn = fact().beginTransaction();
			final Follow following = getFollow(from, to, e, socialTxn);
			final SocialEvent aboutToSocial = getAboutToSocial(from, to, e, socialTxn);

			if (on) {
				if (!users.isEditor(from)) {
					socialTxn.getTxn().rollback();
					return Response.NOT_AN_EDITOR;
				}
				// TODO make sure to is a judge
				else if (aboutToSocial != null && aboutToSocial.on) {
					log.warning(String.format("%s is already about to follow %s", 
							from, to));
					socialTxn.getTxn().rollback();
					return Response.ALREADY_ABOUT_TO_FOLLOW;
				} else if (following != null && aboutToSocial == null) {
					log.warning("Already isFollowing: [" + from + ", " + to
							+ "]");
					socialTxn.getTxn().rollback();
					return Response.ALREADY_FOLLOWING;
				}
			}
			else if (following == null && aboutToSocial == null){
				assert(!on);
				log.warning("Can't unfollow unless isFollowing: " + from + ", "
						+ to + ", " + e);
				socialTxn.getTxn().rollback();
				return Response.NOT_FOLLOWING;
			}
			else if (following != null && aboutToSocial != null && !aboutToSocial.on){
				assert(!on);
				log.warning("Already about to unfollow: " + from + ", "
						+ to + ", " + e);
				socialTxn.getTxn().rollback();
				return Response.ALREADY_ABOUT_TO_UNFOLLOW;
			}
			LockedPeriodical lp = lockPeriodical();
			
			assert(lp != null);
			
			if (!lp.periodical.getcurrentEditionKey().equals(e)) {
				log.warning("Attempted to socialize in old edition");
				lp.rollback(); socialTxn.getTxn().rollback();
				return Response.EDITION_NOT_CURRENT;
			}

			if (lp.periodical.userlocked) {
				lp.rollback(); socialTxn.getTxn().rollback();
				throw new ConcurrentModificationException("waiting to social"); 
			}

			// If a transition is in progress, ask them to wait.
			// This should ease the pressure a bit on the concurrent system.
			if (lp.periodical.inTransition) {
				log.warning("Attempted to socialize during transition");
				lp.rollback(); socialTxn.getTxn().rollback();
				return Response.TRANSITION_IN_PROGRESS;
			}

			// we are now clear to commit to the social task

			try {
				if (aboutToSocial == null) {
					return following == null ? 
							Response.ABOUT_TO_FOLLOW : Response.ABOUT_TO_UNFOLLOW;
				} else {
					return following == null ? 
							Response.PENDING_FOLLOW_CANCELLED : 
								Response.PENDING_UNFOLLOW_CANCELLED;
				}
			} finally {
				lp.setUserLock();
				SocialTask.writeSocialEvent(
						from, to, Edition.getNextKey(e), on, 
						aboutToSocial != null, lp.transaction.getTxn());
				lp.commit();
				lockCache("social"); 
				// no-op: only used for ancestor read query
				socialTxn.getTxn().commit();
				assert(!getPeriodical().inTransition);
			}
		}

		public void changePendingAuthority(Key<User> judge, Key<Edition> edition,
				int amount) {
			assert(getPeriodical().userlocked);
			assert(!getPeriodical().inTransition);

			Objectify txn = fact().beginTransaction();
			Key<ScoreSpace> space = ScoreSpace.keyFromEditionKey(edition);
			JudgeInfluence ji = txn.query(JudgeInfluence.class).ancestor(space)
			.filter("user", judge).get();
			if (ji == null) {
				assert(amount > 0);
				ji = new JudgeInfluence(0, space, judge);
			}
			ji.authority += amount;
			txn.put(ji);
			Key<Edition> next = Edition.getNextKey(edition);
			if (Edition.isFinal(edition, editions.getNumEditions())) {
				TallyTask.releaseUserLock(txn.getTxn());
			}
			else {
				SocialTask.changePendingAuthority(judge, next, amount, txn.getTxn());				
			}
			txn.getTxn().commit();
		}

		public SocialEvent getAboutToSocial(Key<User> from, Key<User> to,
				Key<Edition> e, Objectify o) {
			assert(o != null);

			if (e == null)
				throw new IllegalArgumentException();

			// checks the next edition
			return o.query(SocialEvent.class).ancestor(from).filter("judge", to)
			.filter("edition", Edition.getNextKey(e)).get();
		}

		public Follow getFollow(Key<User> from, Key<User> to, Key<Edition> e, Objectify o) {
			assert(o != null);
			return o.query(Follow.class)
			.ancestor(from).filter("judge", to).filter("edition", e).get();
		}

		public RecentSocials getRecentSocials(int edition) throws RNAException {
			Edition e = editions.getEdition(edition);

			RecentSocials rs = new RecentSocials();
			// No transaction because no aggregate score data needed
			rs.edition = editions.makeEditionMessage(ofy(), e);
			rs.numEditions = editions.getNumEditions();
			rs.isCurrent = editions.isPrevious(e);
			rs.isNext = editions.isCurrent(e);

			Edition succeeding = null;
			if (!Edition.isFinal(e.getKey(), editions.getNumEditions())) {
				succeeding = 
					ofy().get(Edition.getNextKey(e.getKey()));
				rs.list = editions.getLatestSocialInfoInEdition(succeeding);
			}
			else {
				rs.list = new LinkedList<SocialInfo>();
			}
			return rs;
		}

		public boolean willBeFollowingNextEdition(Key<User> from, Key<User> to) {
			Edition e;
			try {
				e = editions.getEdition(Editions.CURRENT);
				SocialEvent about = getAboutToSocial(from, to, e.getKey(), ofy());
				if (about != null) {
					return about.on;
				}
			}
			catch(RNAException ex) {
				try {
					e = editions.getEdition(Editions.FINAL);					
				}
				catch(Exception exc) {
					throw new AssertionError(); // impossible
				}
			}
			Follow f = getFollow(from, to, e.getKey(), ofy());
			return f != null;
		}

	}

	public class Transition {

		public void setPeriodicalBalance() throws RNAException {
			assert(getPeriodical().inTransition);
			assert(!getPeriodical().isFinished());
			assert(Edition.getNumber(editions.getEdition(Editions.CURRENT).getKey()) > 0);
			LockedPeriodical lp = lockPeriodical();

			if (lp == null) {
				log.warning("failed");
				return;
			}

			final Periodical p = lp.periodical;

			if (p == null) {
				log.severe("No Periodical");
				return;
			}

			Edition e;
			
			e = editions.getEdition(Editions.CURRENT);
			int n = editions.getNumEditions();
			assert (e.number > 0);
			int spaceBalance = p.balance / (n - e.number);
			p.balance -= spaceBalance;
			TransitionTask.setSpaceBalance(lp.transaction.getTxn(), e, spaceBalance);
			lp.commit();
			log.info(e + ": balance " + Periodical.moneyPrint(spaceBalance));
			log.info("periodical balance: " + Periodical.moneyPrint(p.balance));				
		}

		public void transitionEdition() throws RNAException {

			LockedPeriodical locked = lockPeriodical();

			assert (locked != null);

			final Periodical p = locked.periodical;
			
			if (p.userlocked) {
				locked.rollback();
				throw new ConcurrentModificationException("waiting to transition"); 
			}

			if (p.isFinished()) {
				// DIE FOREVER
				log.severe("tried to transition a dead periodical");
				locked.rollback();
				return;
			}
			
			Edition current = editions.getEdition(Editions.CURRENT);

			int nextNum = current.number + 1;
			int n = editions.getNumEditions();

			// Do it! Change current edition.
			if (nextNum == n) {
				p.setFinished();
				log.info("End of periodical; last edition was " + current);
			} else if (Edition.isBad(Edition.createKey(nextNum), n) || 
					nextNum == 0) {
				locked.rollback();
				throw new RNAException(String.format(
						"bug in edition numbers: %d > %d", nextNum, n));
			} else {
				p.setcurrentEditionKey(Edition.createKey(nextNum));
				log.info(p.idName + ": New current Edition:" + nextNum);
			}

			p.inTransition = true;
			TransitionTask.setEditionFinished(locked.transaction.getTxn(), 
					current.getKey());
			locked.commit();
			lockCache("transition");
		}

		public void finishTransition() throws RNAException {
			unLockCache();
			assert(getPeriodical().inTransition);
			// the final step in transition machinery!
			LockedPeriodical lp = lockPeriodical();
			lp.periodical.inTransition = false;
			lp.commit();
		}

	}

	public class Users {

		public Link createLink(String url, String title, Key<User> submitter) 
		throws MalformedURLException {
			assert (submitter != null);
			Objectify txn = fact().beginTransaction();
			String domain = new java.net.URL(url).getHost();
			Link l = txn.query(Link.class).ancestor(Link.rootKey()).filter(Name.URL.name, url).get();
			if (l == null) {
				l = new Link(url, title, domain, submitter);
				txn.put(l);
			}
			txn.getTxn().commit();
			return l;
		}

		private User rnaUser;
		public User getRNAUser() {
			if (rnaUser == null) {
				rnaUser = findUserByLogin(User.RNA_EDITOR_EMAIL, User.GMAIL);
			}
			if (rnaUser == null) {
				throw new IllegalStateException("No RNA User");
			}
			
			return rnaUser;
		}

		public User findUserByLogin(String email, String domain) {
			if (email == null || domain == null)
				return null;
			email = email.toLowerCase();
			domain = domain.toLowerCase();
			return ofy().query(User.class).filter("email", email)
					.filter("domain", domain).get();
		}

		public LinkedList<User> getFollowers(Key<User> judge) {
			LinkedList<Key<User>> keys = new LinkedList<Key<User>>();
			for (FollowedBy f : ofy().query(FollowedBy.class).ancestor(judge)) {
				keys.push(f.editor);
			}
			LinkedList<User> editors = new LinkedList<User>();
			for (User ed : ofy().get(keys).values()) {
				editors.push(ed);
			}
			return editors;
		}

		public LinkedList<User> getFollows(Key<User> editor) {
			LinkedList<Key<User>> keys = new LinkedList<Key<User>>();

			for (Follow f : ofy().query(Follow.class).ancestor(editor)) {
				keys.push(f.judge);
			}
			LinkedList<User> judges = new LinkedList<User>();
			for (User judge : ofy().get(keys).values()) {
				judges.push(judge);
			}
			return judges;
		}

		private LinkedList<Vote_Link> getLatestVote_Links(Key<User> user) {
			LinkedList<Vote_Link> result = new LinkedList<Vote_Link>();

			ArrayList<Vote> votes = new ArrayList<Vote>();
			ArrayList<Key<Link>> links = new ArrayList<Key<Link>>();

			Query<Vote> q = ofy().query(Vote.class).ancestor(user)
					.order("-time");

			for (Vote v : q) {
				votes.add(v);
				links.add(v.link);
			}
			Map<Key<Link>, Link> lmap = ofy().get(links);

			for (int i = 0; i < votes.size(); i++) {
				result.add(new Vote_Link(votes.get(i), lmap.get(links.get(i))));
			}

			return result;
		}

		/*
		 * Return information about 'to' and his relation to 'from'
		 */
		public RelatedUserInfo getRelatedUserInfo(Name periodical, User from,
				Key<User> to) throws RNAException {
			UserInfo ui = getUserInfo(periodical, to);
			RelatedUserInfo rui = new RelatedUserInfo();
			rui.userInfo = ui;
			rui.isFollowing = from != null ? social.willBeFollowingNextEdition(
					from.getKey(), to) : false;
			return rui;
		}

		public UserInfo getUserInfo(Name periodical, Key<User> user) throws RNAException {
			UserInfo ui = new UserInfo();
			try {
				ui.user = getUser(user);
				if (ui.user.isEditor) {
					ui.follows = getFollows(user);
					ui.socials = editions.getLatestSocialInfoforEditor(user);
					ui.followers = new LinkedList<User>();
					ui.votes = new LinkedList<Vote_Link>();
				} else {
					ui.votes = getLatestVote_Links(user);
					ui.followers = getFollowers(user);
					ui.follows = new LinkedList<User>();
				}
				return ui;
			} catch (NotFoundException e1) {
				throw new RNAException("No such user exists");
			}
		}

		private User getUser(Key<User> user) throws RNAException {
			try {
				return ofy().get(user);
			}
			catch (NotFoundException e) {
				throw new RNAException("User not found");
			}
		}

		public boolean hasVoted(User u, Key<Edition> e, Link l) {
			Objectify o = fact().beginTransaction();
			try {
				int count = o.query(Vote.class).ancestor(u)
				.filter("edition", e).filter("link", l.getKey())
				.count();
				if (count > 1) {
					throw new IllegalStateException("too many votes for user " + u);
				}
				return count == 1;
			}
			finally {
				o.getTxn().commit();
			}
		}

		private boolean isEditor(Key<User> from) {
			User u = ofy().find(from);
			return u.isEditor;
		}

		public VoteResult voteFor(String link, String fullLink,
				Key<Edition> edition, Boolean on) throws RNAException {
			VoteResult vr = new VoteResult();

			try {
				UserService userService = UserServiceFactory.getUserService();

				// TODO test user login state for votes
				if (user == null) {
					vr.returnVal = Response.NOT_LOGGED_IN;
					vr.authUrl = userService.createLoginURL(fullLink);
					return vr;
				}

				if (user.isEditor) {
					vr.returnVal = Response.ONLY_JUDGES_CAN_VOTE;
					return vr;
				}

				if (Edition.getNumber(edition) == 0) {
					vr.returnVal = Response.VOTING_FORBIDDEN_DURING_SIGNUP;
					return vr;
				}

				Link l = ofy().query(Link.class).ancestor(Link.rootKey()).filter(Name.URL.name, link).get();
				if (l == null) {
					String title = TitleGrabber.getTitle(link);
					vr.suggestedTitle = title;
					vr.returnVal = Response.SUCCESS;
					vr.submit = true;
				} else {
					vr.returnVal = voteFor(
							user, edition, l, on);
					vr.linkId = l.id;
					// TODO test user login state for votes
					vr.authUrl = userService.createLogoutURL("FIXME");
				}
				return vr;
			}
			finally {
				if (!vr.returnVal.equals(Response.SUCCESS)) {
					log.warning(vr.returnVal.toString());
				}
			}
		}

		public Response voteFor(User u, Key<Edition> e, Link l, boolean on)
				throws RNAException {
			assert(e != null && l != null);
			
			if (isCacheLocked() && useCacheLock) {
				throw new CacheWait();
			}
			
			// obtain lock
			LockedPeriodical lp = lockPeriodical();

			if (lp.periodical.isFinished()) {
				log.warning("Attempted to vote in finished periodical");
				lp.rollback();
				return Response.IS_FINISHED;
			}

			if (!lp.periodical.getcurrentEditionKey().equals(e)) {
				log.warning("Attempted to vote in old edition");
				lp.rollback();
				return Response.EDITION_NOT_CURRENT;
			}

			if (lp.periodical.inTransition) {
				log.warning("Attempted to vote during transition");
				lp.rollback();
				return Response.TRANSITION_IN_PROGRESS;
			}

			if (lp.periodical.userlocked) {
				lp.rollback();
				throw new ConcurrentModificationException("waiting to vote");
		 	}

			boolean hasv = hasVoted(u, e, l);
			if (hasv && on) {
				lp.rollback();
				return Response.ALREADY_VOTED;
			}
			
			if (!on && !hasv) {
				lp.rollback();
				return Response.HAS_NOT_VOTED;
			}
			
			lp.setUserLock();
			TallyTask.writeVote(lp.transaction.getTxn(), u.getKey(), e, l.getKey(), on);
			lp.commit();
			lockCache("vote");
			log.info(u + (on ? " FUND-> " : " DEFUND-> ") + l.url);
			return Response.SUCCESS;
		}
		
		public void writeVote(Key<User> uk, Key<Edition> ek,
				Key<Link> lk, boolean on) {
				// lock 
			assert(getPeriodical().userlocked);
			Objectify txn = fact().beginTransaction();
			Vote v = null;
			if (on) {
				// TODO pass in date
				Objectify txn2 = fact().beginTransaction();
				JudgeInfluence ji = getJudgeInfluence(txn2, uk, ek);
				txn2.getTxn().commit();
				int authority = ji.authority;
				v = new Vote(uk, ek, lk, new Date(), authority);
				txn.put(v);
			} else {
				// non-ancestor query: we know this vote is present because the task was enqueued
				v = ofy().query(Vote.class).filter("edition", ek).filter("link", lk).get();
			}
			TallyTask.tallyVote(txn.getTxn(), v.getKey(), on);
			txn.getTxn().commit();
		}

		public JudgeInfluence getJudgeInfluence(Objectify ofy, Key<User> uk, Key<Edition> ek) {
			JudgeInfluence ji = ofy.query(JudgeInfluence.class).ancestor(ScoreSpace.keyFromEditionKey(ek))
			.filter("user", uk).get();
			if (ji == null) { log.severe("no Judge Influence for " + uk); }
			return ji;
		}

		public EditorInfluence getEditorInfluence(Objectify ofy, Key<User> editor, Key<Edition> ek) {
			EditorInfluence ei = ofy.query(EditorInfluence.class).ancestor(ScoreSpace.keyFromEditionKey(ek))
			.filter("editor", editor).get();
			return ei;
		}

		public User welcomeUser(String nickname, String consent, String webPage) 
		throws RNAException {
			editions.getEdition(Editions.CURRENT);
			// TODO Transactions!
			if (user == null) {
				throw new RNAException("You must log in first");
			}
			if (Strings.isNullOrEmpty(nickname)) {
				throw new RNAException("A name or nickname is required.");			
			}
			if (Strings.isNullOrEmpty(webPage)) {
				throw new RNAException("A web page is required (if not yours, then one you like).");			
			}
			boolean c = Boolean.parseBoolean(consent);
			if (!c && !user.isEditor) {
				throw new RNAException("You did not check the consent form.");
			}
			if (ofy().query(User.class).filter("nickname", nickname).get() != null) {
				throw new RNAException("That name is already in use; please modify yours slightly.");				
			}
			user.nickname = nickname;
			user.isInitialized = true;
			user.webPage = normalizeWebPage(webPage);

			Objectify txn = fact().beginTransaction();
						
			txn.put(user);
			txn.getTxn().commit();
			
			// GO ahead and accept their changes, but this should not happen.
			if (hasAlreadyJoined(user)) {
				log.warning("user changed their information: " + user);
				return user;
			}

			log.info("welcome: " + user);

			Edition next = editions.getNextEdition();
			SocialEvent join = new SocialEvent(users.getRNAUser().getKey(),
					user.getKey(), next.getKey(), new Date(), true);
			ofy().put(join);
			
			if (!user.isEditor) {
					
				for(int i = editions.getCurrentEdition().number;
				i < editions.getNumEditions();
				i++) {
					Key<ScoreSpace> sKey = ScoreSpace.keyFromEditionKey(Edition.createKey(i));
					ofy().put(new JudgeInfluence(0, 
							sKey, user.getKey()));
				}
			}
			return user;
		}

		private boolean hasAlreadyJoined(User u) {
			return ofy().query(SocialEvent.class)
					.ancestor(users.getRNAUser()).filter("judge", u.getKey()).get()
					!= null;
		}

		public EditorFundings getEditorFundings(int editionNum, long editor) throws RNAException {
			EditorFundings ef = new EditorFundings();
			Edition e = editions.getEdition(editionNum);
			ef.edition = editions.makeEditionMessage(ofy(), e);
			ef.numEditions = editions.getNumEditions();
			ef.isCurrent = editions.isPrevious(e);
			ef.isNext = editions.isCurrent(e);
			ef.editor = getUser(User.createKey(editor));
			ef.list = editions.getLatestUser_Vote_Links(e.getKey(), User.createKey(editor));
			return ef;
		}
	}


	public Periodical getPeriodical() {
		return ofy().get(Periodical.getKey(periodicalName.name));
	}

	private LockedPeriodical lockPeriodical() throws RNAException {
		return new LockedPeriodical();
	}

	int funding(int score, int totalScore, int editionFunds) {
		return (int) (score / (double) totalScore * editionFunds);
	}

	/* Increment the ScoreSpace (score and fund) for this edition */
	public void tallyVote(Key<Vote> vote, boolean on) {
		assert(getPeriodical().userlocked);
		Vote v = ofy().get(vote);
		Objectify otx = fact().beginTransaction();
		ScoreSpace space = editions.getScoreSpace(otx, v.edition);
		
		ScoredLink sl = editions.getScoredLink(otx, v.edition, v.link);
		
		space.totalScore += on? v.authority : -v.authority;
		space.totalSpend = funding(space.totalScore, space.totalScore, space.balance);
		
		if (sl == null) {
			if (on) {
				sl = new ScoredLink(space.root, v.link, v.authority);
				if (v.authority > 0) {
					space.numFundedLinks++;
				}
				otx.put(sl);
			}
			else {
				log.severe("we are cancelling a null vote!");
			}
		}
		else {
			sl.score += on? v.authority : -v.authority;
			if (sl.score == 0) {
				otx.delete(sl);
				space.numFundedLinks--;
			}
			else {
				otx.put(sl);
			}
		}
		otx.put(space);
		TallyTask.addJudgeScore(otx.getTxn(), v.getKey(), v.authority, on);			
		otx.getTxn().commit();
	}

	public void addJudgeScore(Key<Vote> vkey, int authority, boolean on) {
		assert(getPeriodical().userlocked);
		Vote v = ofy().get(vkey);
		Objectify otx = fact().beginTransaction();
		JudgeInfluence ji = users.getJudgeInfluence(otx, v.voter, v.edition);
		ji.score += on? authority : -authority;
		otx.put(ji);
		TallyTask.findEditorsToScore(otx.getTxn(), vkey, on);
		otx.getTxn().commit();
	}
	
	public void findEditorsToScore(Key<Vote> vkey, boolean on) {
		assert(getPeriodical().userlocked);
		Vote v = ofy().get(vkey);
		Objectify otx = fact().beginTransaction();
		Set<Key<User>> editors = new HashSet<Key<User>>();
		for (FollowedBy fb : otx.query(FollowedBy.class).ancestor(v.voter)
				.filter("edition", v.edition)) {
			editors.add(fb.editor);
		}
		
		if (!on) {
			TallyTask.deleteEditorVotes(otx.getTxn(), vkey, editors, v.edition);
		}
		else {
			TallyTask.addEditorVotes(otx.getTxn(), v.getKey(), editors, v.edition, on, v.voter, v.link);
		}
		otx.getTxn().commit();
	}

	public void deleteEditorVotes(Key<Vote> v, Set<Key<User>> editors, Key<Edition> edition) {
		Objectify otx = fact().beginTransaction();
		otx.delete(otx.query(EditorVote.class).ancestor(v));
		TallyTask.deleteVote(otx.getTxn(), v, editors, edition);
		otx.getTxn().commit();
	}

	public void addEditorVotes(Key<Vote> v, Set<Key<User>> editors, Key<Edition> edition, Key<User> judge, Key<Link> link) {
		Objectify otx = fact().beginTransaction();
		Set<EditorVote> evset = new HashSet<EditorVote>();
		for (Key<User> editor : editors) {
		EditorVote ev = new EditorVote(v, editor, judge, link, edition);
			evset.add(ev);
		}
		otx.put(evset);
		TallyTask.addEditorScore(otx.getTxn(), editors, edition, true);
		otx.getTxn().commit();
	}

	public void deleteVote(Key<Vote> v, Set<Key<User>> editors, Key<Edition> edition) {
		Objectify otx = fact().beginTransaction();
		otx.delete(v);
		TallyTask.addEditorScore(otx.getTxn(), editors, edition, false);		
		otx.getTxn().commit();
	}

	public void addEditorScore(Set<Key<User>> editors, Key<Edition> edition, boolean on) {
		Objectify otx = fact().beginTransaction();
		Set<EditorInfluence> eiset = new HashSet<EditorInfluence>();
		for (Key<User> editor : editors) {
			EditorInfluence ei = otx.query(EditorInfluence.class).ancestor(ScoreSpace.keyFromEditionKey(edition))
			.filter("editor", editor).get();
			// TODO Write a test for this invariant that checks this and judge influence
			ei.score += on ? 1 : -1;
			eiset.add(ei);
		}
		if (eiset.size() > 0) {
			otx.put(eiset);
		}
		TallyTask.releaseUserLock(otx.getTxn());		
		otx.getTxn().commit();
	}

	public void releaseUserLock() throws RNAException {
		unLockCache();        
		LockedPeriodical lp = lockPeriodical();
		lp.releaseUserLock();
		lp.commit();
	}

	private void unLockCache() {
		if (cache != null) {
			if (!isCacheLocked()) {
				log.warning("cache not locked!");
			} else {
				Operation op = getCacheLock();
				long when = op.when;
				long diff = new Date().getTime() - when;
				if (diff > 2000) {
					log.severe(String.format("time to unlock %s (ms): %d", op.what, diff));
				}
				else if (diff > 1000) {
					log.warning(String.format("time to unlock %s (ms): %d", op.what, diff));
				}
				else {
					log.info(String.format("time to unlock %s (ms): %d", op.what, diff));
				}
			}
			cache.put("locked", Boolean.FALSE);
		}
	}

	private Operation getCacheLock() {
		Object o = cache.get("op");
		return o == null? null : (Operation) o;
	}

	private static class Operation implements Serializable {
		private static final long serialVersionUID = 1L;
		public long when; public String what; 
	}
	
	private void lockCache(String func) {
		if (cache != null) {
			cache.put("locked", Boolean.TRUE);
			Operation op = new Operation();
			op.when = new Date().getTime();
			op.what = func;
			cache.put("op", op);	
		}
	}

	private boolean isCacheLocked() {
		if (cache != null) {
			Object o = cache.get("locked");
			if (o != null) {
				return (Boolean) o;
			}
		}
		return false;
	}

	public void donate(String name, String donation, String webPage,
			String statement, String consent) throws RNAException {
		int amount = parseDonation(donation);
		if (Strings.isNullOrEmpty(name)) {
			throw new RNAException("A name is required.");			
		}
		if (ofy().query(Donation.class).filter("name", name).get() != null) {
			throw new RNAException(name + " has already donated.");			
		}
		boolean c = Boolean.parseBoolean(consent);
		if (!c) {
			throw new RNAException("You did not check the consent form.");
		}
		Donation d = new Donation(name, amount, normalizeWebPage(webPage), statement);
		log.info("Donation succeeded");
		ofy().put(d);
	}

	private int parseDonation(String donation) throws RNAException {
		int amount;
		try {
			amount = (int) (Double.parseDouble(donation) * CENTS_PER_DOLLAR);
			if (amount > HUGE_DONATION_DOLLARS * CENTS_PER_DOLLAR) {
				throw new RNAException("The maximum donation is $" + HUGE_DONATION_DOLLARS);
			}
		}
		catch (NumberFormatException e) {
			throw new RNAException("Donation amount must be a number.");
		}
		return amount;
	}

	private String normalizeWebPage(String webPage) throws RNAException {
		if (Strings.isNullOrEmpty(webPage)) {
			return "";
		}
		try {
			new java.net.URL(webPage);
			return webPage;
		} catch (MalformedURLException e) {
			try {
				String wp = "http://" + webPage;
				new java.net.URL(wp);
				return wp;
			} catch (MalformedURLException mfe) {
				throw new RNAException("Invalid web page: " + webPage);
			}
		}
	}


}