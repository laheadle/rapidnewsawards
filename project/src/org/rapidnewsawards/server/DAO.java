package org.rapidnewsawards.server;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.rapidnewsawards.core.Donation;
import org.rapidnewsawards.core.Edition;
import org.rapidnewsawards.core.EditionUserAuthority;
import org.rapidnewsawards.core.Follow;
import org.rapidnewsawards.core.Link;
import org.rapidnewsawards.core.Periodical;
import org.rapidnewsawards.core.Root;
import org.rapidnewsawards.core.ScoreRoot;
import org.rapidnewsawards.core.ScoreSpace;
import org.rapidnewsawards.core.ScoredLink;
import org.rapidnewsawards.core.SocialEvent;
import org.rapidnewsawards.core.User;
import org.rapidnewsawards.core.Vote;
import org.rapidnewsawards.messages.AllEditions;
import org.rapidnewsawards.messages.EditionMessage;
import org.rapidnewsawards.messages.FullStoryInfo;
import org.rapidnewsawards.messages.Name;
import org.rapidnewsawards.messages.RecentSocials;
import org.rapidnewsawards.messages.RecentVotes;
import org.rapidnewsawards.messages.RelatedUserInfo;
import org.rapidnewsawards.messages.Response;
import org.rapidnewsawards.messages.SocialInfo;
import org.rapidnewsawards.messages.StoryInfo;
import org.rapidnewsawards.messages.TopJudges;
import org.rapidnewsawards.messages.TopStories;
import org.rapidnewsawards.messages.UserInfo;
import org.rapidnewsawards.messages.User_Authority;
import org.rapidnewsawards.messages.User_Vote_Link;
import org.rapidnewsawards.messages.VoteResult;
import org.rapidnewsawards.messages.Vote_Link;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.NotFoundException;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Query;
import com.googlecode.objectify.util.DAOBase;

public class DAO extends DAOBase {
	public class Editions {

		public static final int CURRENT = -1;
		public static final int NEXT = -2;

		private EditionMessage makeEditionMessage(Edition e) {
			return new EditionMessage(e,
					ofy().get(ScoreSpace.keyFromEditionKey(e.getKey())));		
		}


		// TODO parameterize by periodical
		public AllEditions getAllEditions() {
			LinkedList<EditionMessage> ll = new LinkedList<EditionMessage>();
			Map<String, ScoreSpace> spaces = 
				new HashMap<String, ScoreSpace>(); 
			for (ScoreSpace s : ofy().query(ScoreSpace.class)) {
				spaces.put(s.id, s);
			}
			for (Edition e : ofy().query(Edition.class)) {
				ll.add(new EditionMessage(e, spaces.get(e.id)));
			}
			Edition c = getCurrentEdition();
			AllEditions ae = new AllEditions(ll, 
					new EditionMessage(c, spaces.get(c.id)));
			return ae;
		}

		public Edition getCurrentEdition() {
			return getEdition(CURRENT);
		}

		/*
		 * Get the requested edition
		 * 
		 * @param number the edition number requested, or CURRENT for current, or NEXT
		 * for next
		 */
		public Edition getEdition(final int number) {

			final Objectify o = ofy();

			if (number == CURRENT) {
				final Periodical p = getPeriodical();
				if (p.isFinished())
					throw new IllegalStateException("no current edition");

				return o.get(p.getcurrentEditionKey());
			}

			else if (number == NEXT) {
				final Periodical p = getPeriodical();
				if (p.isFinished()) {
					throw new IllegalStateException(
							"Next edition requested for finished periodical");
				}
				Key<Edition> nextKey = Edition.getNextKey(p.getcurrentEditionKey().getName());
				if (Edition.isAfterFinal(nextKey, getNumEditions())) {
					throw new IllegalStateException(
					"Next edition requested after final Edition");					
				}
				return o.get(nextKey);
			}
			else {
				// TODO 2.0 this only works because we assume one periodical
				return o.get(Edition.class, "" + number);
			}
		}

		// fixme refactor with getvoters
		public LinkedList<User_Authority> getJudges(Edition e) {
			LinkedList<User_Authority> result = new LinkedList<User_Authority>();

			Map<Key<User>, Integer> authorities = new HashMap<Key<User>, Integer>();
			ArrayList<Key<User>> judges = new ArrayList<Key<User>>();

			for (EditionUserAuthority eua : ofy().query(
					EditionUserAuthority.class).filter("edition", e.getKey())) {
				authorities.put(eua.user, eua.authority);
				judges.add(eua.user);
			}

			if (judges.size() == 0) {
				log.warning("requested judges for empty edition " + e);
				return result;
			}

			Map<Key<User>, User> vmap = ofy().get(judges);

			for (int i = 0; i < judges.size(); i++) {
				result.add(new User_Authority(vmap.get(judges.get(i)),
						authorities.get(judges.get(i))));
			}

			Collections.sort(result);
			return result;
		}

		private Edition getLastEdition() {
			Edition e;
			e = ofy().find(
					Edition.getPreviousKey("" + editions.getNumEditions()));
			return e;
		}

		/*
		 * Runs three queries: first get keys, then use the keys to get 2 sets
		 * of entities
		 */
		private LinkedList<SocialInfo> getLatestEditor_Judges(Edition e) {
			LinkedList<SocialInfo> result = new LinkedList<SocialInfo>();

			if (e == null)
				return result;

			ArrayList<Key<User>> editors = new ArrayList<Key<User>>();
			ArrayList<Key<User>> judges = new ArrayList<Key<User>>();
			ArrayList<Boolean> bools = new ArrayList<Boolean>();
			// think - does this show everything we want?
			// todo ignore welcomes on eds and donors
			Query<SocialEvent> q = ofy().query(SocialEvent.class)
					.filter("edition", e.getKey()).order("-time");

			for (SocialEvent event : q) {
				editors.add(event.editor);
				judges.add(event.judge);
				bools.add(event.on);
			}
			Map<Key<User>, User> umap = ofy().get(editors);
			Map<Key<User>, User> lmap = ofy().get(judges);

			for (int i = 0; i < editors.size(); i++) {
				result.add(new SocialInfo(umap.get(editors.get(i)), lmap
						.get(judges.get(i)), bools.get(i)));
			}

			return result;

		}

		/*
		 * Runs three queries: first get keys, then use the keys to get 2 sets
		 * of entities
		 */
		public LinkedList<User_Vote_Link> getLatestUser_Vote_Links(Edition e) {
			LinkedList<User_Vote_Link> result = new LinkedList<User_Vote_Link>();

			ArrayList<Key<User>> users = new ArrayList<Key<User>>();
			ArrayList<Key<Link>> links = new ArrayList<Key<Link>>();

			Query<Vote> q = ofy().query(Vote.class)
					.filter("edition", e.getKey()).order("-time");

			LinkedList<Vote> votes = new LinkedList<Vote>();
			for (Vote v : q) {
				votes.add(v);
			}

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

		public Edition getNextEdition() {
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

		public RecentVotes getRecentVotes(int edition) {
			Edition e = getEdition(edition);
			RecentVotes s = new RecentVotes();
			s.edition = makeEditionMessage(e);
			s.numEditions = getNumEditions();
			s.list = getLatestUser_Vote_Links(e);
			return s;
		}

		public ScoredLink getScoredLink(Key<Edition> e, Key<Link> l) {
			return ofy().query(ScoredLink.class).filter("edition", e)
					.filter("link", l).get();
		}

		public LinkedList<ScoredLink> getScoredLinks(Edition e, int minScore) {
			LinkedList<ScoredLink> result = new LinkedList<ScoredLink>();
			if (e == null)
				return result;

			for (ScoredLink sl : ofy().query(ScoredLink.class)
					.filter("edition", e.getKey()).filter("score >=", minScore)
					.order("-score")) {
				result.add(sl);
			}
			return result;
		}

		public FullStoryInfo getStory(int editionNum, Long linkId) {
			Key<Link> linkKey = new Key<Link>(Link.class, linkId);
			Key<Edition> editionKey = new Key<Edition>(Edition.class, ""
					+ editionNum);

			// Edition e = getEdition(Name.AGGREGATOR_NAME, editionNum, null);
			ScoredLink sl = editions.getScoredLink(editionKey, linkKey);

			Link link = ofy().get(linkKey);

			StoryInfo si = new StoryInfo();
			si.link = link;
			si.score = sl.score;
			si.editionId = "" + editionNum;
			si.submitter = ofy().get(link.submitter);
			si.revenue = sl.revenue;

			FullStoryInfo fsi = new FullStoryInfo();
			fsi.info = si;
			fsi.funds = getVoters(linkKey, editionKey);
			return fsi;
		}

		public TopJudges getTopJudges(int edition) {
			Edition e = editions
					.getEdition(edition);
			TopJudges tj = new TopJudges();
			tj.edition = makeEditionMessage(e);
			tj.numEditions = editions.getNumEditions();
			tj.list = getJudges(e);
			return tj;
		}

		public TopStories getTopStories(int editionNum) {
			Edition e = editions.getEdition(editionNum);

			TopStories result = new TopStories();
			LinkedList<StoryInfo> stories = new LinkedList<StoryInfo>();
			result.edition = makeEditionMessage(e);
			result.numEditions = editions.getNumEditions();
			result.list = stories;
			LinkedList<ScoredLink> scored = editions.getScoredLinks(e, 1);
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

			for (ScoredLink sl : scored) {
				StoryInfo si = new StoryInfo();
				si.link = linkMap.get(sl.link);
				si.score = sl.score;
				si.editionId = e.id;
				si.submitter = userMap.get(si.link.submitter);
				si.revenue = sl.revenue;
				stories.add(si);
			}

			return result;
		}

		public VoteResult submitStory(String url, String title,
				Edition edition, User submitter) {
			// TODO put this and vote in transaction along with task
			VoteResult vr = new VoteResult();

			if (submitter == null) {
				vr.returnVal = Response.NOT_LOGGED_IN;
				vr.authUrl = null; // userService.createLoginURL(fullLink);
				return vr;
			}
			else {
				try {
					Link l = users.createLink(url, title, submitter.getKey());
					vr.returnVal = users.voteFor(
							submitter,
							edition == null ? editions
									.getCurrentEdition()
									: edition, l, true);
					vr.authUrl = null; // userService.createLogoutURL(home);
				}
				catch (MalformedURLException e) {
					// TODO Test on frontend
					log.warning("bad url " + url + "submitted by " + submitter);
					vr.returnVal = Response.BAD_URL;
				}
			}
			return vr;
		}

		private Edition getPreviousOrLastEdition(LockedPeriodical lp) {
			Key<Edition> current = lp.periodical.getcurrentEditionKey();
			if (current == null) {
				return getLastEdition();
			}
			return ofy().find(Edition.getPreviousKey(current.getName()));
		}

		public LinkedList<User_Authority> getVoters(Key<Link> l, Key<Edition> e) {
			LinkedList<User_Authority> result = new LinkedList<User_Authority>();

			Map<Key<User>, Integer> authorities = new HashMap<Key<User>, Integer>();
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

			Map<Key<User>, User> vmap = ofy().get(voters);

			for (int i = 0; i < voters.size(); i++) {
				result.add(new User_Authority(vmap.get(voters.get(i)),
						authorities.get(voters.get(i))));
			}

			Collections.sort(result);
			return result;
		}

		public ScoreSpace getScoreSpace(Key<Edition> key) {
			Key<ScoreRoot> sroot = new Key<ScoreRoot>(ScoreRoot.class, key.getName());
			Key<ScoreSpace> skey = new Key<ScoreSpace>(sroot,
					ScoreSpace.class, key.getName());

			if (ofy().find(sroot) == null) { throw new IllegalStateException(); }
			if (ofy().find(skey) == null) { throw new IllegalStateException(); }

			assert(ofy().find(sroot) != null);
			assert(ofy().find(skey) != null);
			ScoreSpace s = ofy().find(skey);
			if (s == null) {
				throw new IllegalStateException(
						"no associated score space for edition "+ key);
			}
			return s;
		}

		public void setRevenue(long edition, int revenue) {			
			ScoreSpace s = getScoreSpace(
					new Key<Edition>(Edition.class, Long.toString(edition)));
			Objectify oTxn = fact().beginTransaction();
			s.revenue = revenue;
			TransitionTask.finish(oTxn.getTxn());
			oTxn.getTxn().commit();
		}

	}

	private class LockedPeriodical {
		public final Objectify transaction;
		public final Periodical periodical;

		public void checkState() {
			if (periodical.inTransition && periodical.userlocked) {
				throw new IllegalStateException("deadlock!");				
			}
		}
		
		public LockedPeriodical() {		
			Objectify oTxn = fact().beginTransaction();
			Periodical p = oTxn.get(Periodical.getKey(periodicalName.name));
			if (p.isFinished()) {
				throw new IllegalStateException("The periodical is finished");
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
				boolean on) {
			assert(getPeriodical().userlocked);
			assert(!getPeriodical().inTransition);
			final Objectify txn = fact().beginTransaction();
			
			SocialEvent social = new SocialEvent(from, to, e, new Date(), on);
			txn.put(social);				

			if (on) {
				Query<Edition> eds = ofy().query(Edition.class).filter("id >=", e.getName());
				List<Follow> fols = new LinkedList<Follow>();
				for (Key<Edition> ekey : eds.fetchKeys()) {
					fols.add(new Follow(from, to, ekey, social.getKey()));
				}
				txn.put(fols);
			}
			else {
				txn.delete(txn.query(Follow.class)
						.ancestor(from).filter("to", to).filter("edition >=", e).fetchKeys());
			}
			int amount = on ? 1 : -1;
			SocialTask.changePendingAuthority(to, e, amount, txn.getTxn());
			txn.getTxn().commit();
		}

		/* Wrapper which assumes a <from> of the current user */
		public Response doSocial(Key<User> to, boolean on) {
			if (user == null) {
				log.warning("attempt to follow with null user");
				return Response.ILLEGAL_OPERATION;
			}
			Key<User> from = user.getKey();
			return doSocial(from, to, editions.getEdition(Editions.CURRENT).getKey(), on);
		}
		
		/* Do a follow, unfollow, or cancel pending follow, unfollow */
		public Response doSocial(Key<User> from, Key<User> to, Key<Edition> e, boolean on) {
			
			if (Edition.isFinalOrBad(e, editions.getNumEditions())) {
				log.warning(String.format(
						"Attempted to socialize during illegal edition: User %s, Edition %s",
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
				return Response.NO_LONGER_CURRENT;
			}

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
						from, to, e, on, lp.transaction.getTxn());
				lp.commit();
				assert(!getPeriodical().inTransition);
			}
		}

		public void changePendingAuthority(Key<User> judge, Key<Edition> edition,
				int amount) {
			assert(getPeriodical().userlocked);
			assert(!getPeriodical().inTransition);

			Objectify txn = fact().beginTransaction();
			EditionUserAuthority eua = txn.query(EditionUserAuthority.class).ancestor(edition)
			.filter("user", judge).get();
			if (eua == null) {
				assert(amount > 0);
				eua = new EditionUserAuthority(0, edition, judge);
			}
			eua.authority += amount;
			txn.put(eua);
			Key<Edition> next = Edition.getNextKey(edition.getName());
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
				return null;

			// checks the next edition
			return o.query(SocialEvent.class).ancestor(from).filter("judge", to)
			.filter("edition", Edition.getNextKey(e.getName())).get();
		}

		public Follow getFollow(Key<User> from, Key<User> to, Key<Edition> e, Objectify o) {
			assert(o != null);
			return o.query(Follow.class)
			.ancestor(from).filter("judge", to).filter("edition", e).get();
		}

		public RecentSocials getRecentSocials(int edition) {
			Edition current = null;
			Edition next = null;

			if (edition == Editions.CURRENT) {
				current = editions.getEdition(Editions.CURRENT);
				next = editions.getEdition(Editions.NEXT);
			} else {
				// next after edition
				current = editions.getEdition(edition);
				next = editions.getEdition(edition + 1);
			}

			RecentSocials s = new RecentSocials();
			s.edition = editions.makeEditionMessage(current);
			s.numEditions = editions.getNumEditions();
			s.list = editions.getLatestEditor_Judges(next);
			return s;
		}

		public boolean isFollowingOrAboutToFollow(Key<User> from, Key<User> to) {
			Edition e = editions.getEdition(Editions.CURRENT);
			SocialEvent about = getAboutToSocial(from, to, e.getKey(), ofy());

			if (about != null) {
				return about.on;
			} else {
				Follow f = getFollow(from, to, e.getKey(), ofy());
				return f != null;
			}
		}

	}

	public class Transition {

		public void doTransition(int editionNum) {
			Edition from = editions.getEdition(editionNum);

			Edition current = editions.getCurrentEdition();
			Edition next = editions.getNextEdition();

			if (from == null) {
				throw new IllegalStateException("Edition " + editionNum + " does not exist");
			}
			if (!from.equals(current)) {
				throw new IllegalStateException("edition 1 not current (2 is): " + from + ", "
						+ current);
			}

			if (next == null) {
				transition.finishPeriodical();
				log.info("End of periodical; last edition is" + current);
			} else {
				transition.transitionEdition();
			}
		}

		public void finishPeriodical() {
			LockedPeriodical lp = lockPeriodical();
			final Periodical p = lp.periodical;
			p.setFinished();
			lp.commit();
		}

		public void setBalance() {
			assert(getPeriodical().inTransition);
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
			ScoreSpace s;
			
			if (p.isFinished()) {
				log.warning("spending all remaining revenue");

				e = editions.getLastEdition();
				if (e == null) {
					throw new IllegalStateException("no final edition");
				}
				s = editions.getScoreSpace(e.getKey());
				assert (s != null);
				s.revenue = p.balance;
				p.balance = 0;
			} else {
				e = editions.getPreviousOrLastEdition(lp);

				if (e == null) {
					throw new IllegalStateException("no previous edition");
				}

				s = editions.getScoreSpace(e.getKey());
				assert (s != null);
				int n = editions.getNumEditions();
				if (e.number > 0) {
					s.revenue = p.balance / (n - e.number);
					p.balance -= s.revenue;
				}
			}

			TransitionTask.setRevenue(lp.transaction.getTxn(), e, s.revenue);
			lp.commit();

			log.info(e + ": revenue " + Periodical.moneyPrint(s.revenue));
			log.info("balance: " + Periodical.moneyPrint(p.balance));
		}

		public void transitionEdition() {

			LockedPeriodical locked = lockPeriodical();

			assert (locked != null);

			final Periodical p = locked.periodical;
			
			if (p.userlocked) { throw new ConcurrentModificationException(); }

			if (p.isFinished()) {
				// DIE FOREVER
				log.severe("tried to transition a dead periodical");
				return;
			}
			
			Edition current = ofy().find(p.getcurrentEditionKey());

			if (current == null) {
				// DIE FOREVER
				log.severe("no edition matching" + p.getcurrentEditionKey());
				return;
			}

			int nextNum = current.number + 1;
			int n = editions.getNumEditions();

			// Do it! Change current edition.
			if (nextNum == n) {
				p.setFinished();
			} else if (nextNum > n) {
				throw new IllegalStateException(String.format(
						"bug in edition numbers: %d > %d", nextNum, n));
			} else {
				p.setcurrentEditionKey(new Key<Edition>(Edition.class, ""
						+ nextNum));
			}

			p.inTransition = true;
			TransitionTask.setBalance(locked.transaction.getTxn());
			locked.commit();
			log.info(p.idName + ": New current Edition:" + nextNum);
		}

		public void finishTransition() {
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
			String domain = new java.net.URL(url).getHost();
			Link l = new Link(url, title, domain, submitter);
			ofy().put(l);
			return l;
		}

		public User findRNAUser() {
			Objectify o = ofy();

			Query<User> q = o.query(User.class).filter("isRNA", true);
			if (q.count() != 1) {
				log.severe("bad rnaEditor count: " + q.count());
				return null;
			}

			return q.get();
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
			for (Follow f : ofy().query(Follow.class).filter("judge", judge)) {
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
				Key<User> to) {
			UserInfo ui = getUserInfo(periodical, to);
			RelatedUserInfo rui = new RelatedUserInfo();
			rui.userInfo = ui;
			rui.isFollowing = from != null ? social.isFollowingOrAboutToFollow(
					from.getKey(), to) : false;
			return rui;
		}

		public UserInfo getUserInfo(Name periodical, Key<User> user) {
			UserInfo ui = new UserInfo();
			try {
				ui.user = ofy().get(user);
				if (ui.user.isEditor) {
					ui.follows = getFollows(user);
					ui.followers = new LinkedList<User>();
					ui.votes = new LinkedList<Vote_Link>();
				} else {
					ui.votes = getLatestVote_Links(user);
					ui.followers = getFollowers(user);
					ui.follows = new LinkedList<User>();
				}
				return ui;
			} catch (NotFoundException e1) {
				log.warning("Bad user info: " + user);
				return null;
			}
		}

		public boolean hasVoted(User u, Edition e, Link l) {
			Objectify o = fact().beginTransaction();
			try {
				int count = o.query(Vote.class).ancestor(u)
				.filter("edition", e.getKey()).filter("link", l.getKey())
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
			if (u == null) {
				log.severe("bad input");
				return false;
			}
			return u.isEditor;
		}

		public VoteResult voteFor(String link, String fullLink,
				Edition edition, Boolean on) {
			VoteResult vr = new VoteResult();
			UserService userService = UserServiceFactory.getUserService();

			// TODO test user login state for votes
			if (user == null) {
				vr.returnVal = Response.NOT_LOGGED_IN;
				vr.authUrl = userService.createLoginURL(fullLink);
				return vr;
			}

			Link l = ofy().query(Link.class).filter(Name.URL.name, link).get();
			if (l == null) {
				return null; // User must submit the link
			} else {
				vr.returnVal = voteFor(
						user,
						edition == null ? editions
								.getCurrentEdition()
								: edition, l, on);
				// TODO test user login state for votes
				vr.authUrl = userService.createLogoutURL("FIXME");
			}
			return vr;
		}

		/**
		 * Store a new vote in the DB by a user for a link
		 * 
		 * @param r
		 *            the user voting
		 * @param e
		 *            the edition during which the vote occurs
		 * @param l
		 *            the link voted for
		 * @throws IllegalArgumentException
		 */
		public Response voteFor(User u, Edition e, Link l, boolean on)
				throws IllegalArgumentException {
			// TODO only judges can vote, ditto for ed follows

			assert(e != null);
			
			// obtain lock
			LockedPeriodical lp = lockPeriodical();

			if (lp.periodical.isFinished()) {
				log.warning("Attempted to vote in finished periodical");
				lp.rollback();
				return Response.IS_FINISHED;
			}

			if (!lp.periodical.getcurrentEditionKey().equals(e.getKey())) {
				log.warning("Attempted to vote in old edition");
				lp.rollback();
				return Response.NO_LONGER_CURRENT;
			}

			if (lp.periodical.inTransition) {
				log.warning("Attempted to vote during transition");
				lp.rollback();
				return Response.TRANSITION_IN_PROGRESS;
			}

			if (lp.periodical.userlocked) {
				log.warning("waiting to vote");
				lp.rollback();
				throw new ConcurrentModificationException("waiting");
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
			TallyTask.writeVote(lp.transaction.getTxn(), u, e, l, on);
			lp.commit();
			log.info(u + (on ? " +++-> " : " xxx-> ") + l.url);
			return Response.SUCCESS;
		}
		
		public void writeVote(Key<User> uk, Key<Edition> ek,
				Key<Link> lk, boolean on) {
			assert(getPeriodical().userlocked);
			Objectify txn = fact().beginTransaction();
			if (on) {
				// TODO pass in date
				int authority = ofy().query(EditionUserAuthority.class).ancestor(ek)
				.filter("user", uk).get().authority;
				Vote v = new Vote(uk, ek, lk, new Date(), authority);
				txn.put(v);
				TallyTask.tallyVote(txn.getTxn(), v);
				txn.getTxn().commit();
			} else {
				throw new IllegalArgumentException("no negative voting yet");
				/*
				Vote v = ofy().query(Vote.class).filter("edition", ek)
				.filter("link", lk).get();
				txn.delete(v);
				// TODO pass in actual values -- vote is gone!
				TallyTask.tallyVote(txn.getTxn(), v); */
			}
		}

		public User welcomeUser(String nickname, int donation) {
			// TODO Transactions!
			user.nickname = nickname;
			user.isInitialized = true;

			ofy().put(user);

			log.info("welcome: " + user);

			Edition next = editions.getNextEdition();

			if (next == null) {
				log.warning("join failed");
				return null;
			} else {
				// TODO double check they're not present
				SocialEvent join = new SocialEvent(User.getRNAEditor(),
						user.getKey(), next.getKey(), new Date(), true);
				ofy().put(join);
			}
			
			for(int i = editions.getCurrentEdition().number;
					i < editions.getNumEditions();
					i++) {
				Key<Edition> eKey = Edition.getKey(i);
				ofy().put(new EditionUserAuthority(0, 
						eKey, user.getKey()));
			}
			// TODO 2.0 not used 
			/*
			LockedPeriodical lp = lockPeriodical();

			lp.periodical.balance += donation;
			lp.commit();

			log.info("balance: " + Periodical.moneyPrint(lp.periodical.balance));
						Donation don = new Donation(user.getKey(), donation);

			ofy().put(don);

			*/	
			return user;
		}

	}

	static {
		ObjectifyService.factory().register(Donation.class);
		ObjectifyService.factory().register(Edition.class);
		ObjectifyService.factory().register(EditionUserAuthority.class);
		ObjectifyService.factory().register(Follow.class);
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

	public static final Logger log = Logger.getLogger(DAO.class.getName());

	public DAO() {
		user = null;
		social = new Social();
		transition = new Transition();
		editions = new Editions();
		users = new Users();
	}

	public Periodical getPeriodical() {
		Periodical p = ofy().get(Periodical.getKey(periodicalName.name));
		if (p == null) {
			throw new IllegalStateException("no periodical");
		}
		return p;
	}


	private LockedPeriodical lockPeriodical() {
		return new LockedPeriodical();
	}

	int revenue(int score, int totalScore, int editionFunds) {
		return (int) (score / (double) totalScore * editionFunds);
	}

	public void tallyVote(Key<Vote> vote) {
		assert(getPeriodical().userlocked);
		Vote v = ofy().get(vote);
		Objectify otx = fact().beginTransaction();
		ScoreSpace space = editions.getScoreSpace(v.edition);
		
		ScoredLink sl = otx.query(ScoredLink.class)
		.ancestor(space).filter("link", v.link).get();
		
		space.totalScore += v.authority;
		int rev = revenue(v.authority, space.totalScore, space.revenue);
		space.totalSpend += rev;
		
		if (sl == null) {
			sl = new ScoredLink(v.edition, space.root, v.link, v.authority, rev);
		}
		else {
			sl.score += v.authority;
			sl.revenue += rev;
		}
		otx.put(sl);
		otx.put(space);
		TallyTask.releaseUserLock(otx.getTxn());
		otx.getTxn().commit();
	}

	public void releaseUserLock() {
		LockedPeriodical lp = lockPeriodical();
		lp.releaseUserLock();
		lp.commit();
	}

}