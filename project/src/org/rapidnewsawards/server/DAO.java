package org.rapidnewsawards.server;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Logger;

import org.rapidnewsawards.core.Donation;
import org.rapidnewsawards.core.Edition;
import org.rapidnewsawards.core.EditionUserAuthority;
import org.rapidnewsawards.core.Follow;
import org.rapidnewsawards.core.Link;
import org.rapidnewsawards.core.Periodical;
import org.rapidnewsawards.core.Root;
import org.rapidnewsawards.core.ScoreSpace;
import org.rapidnewsawards.core.ScoredLink;
import org.rapidnewsawards.core.SocialEvent;
import org.rapidnewsawards.core.User;
import org.rapidnewsawards.core.Vote;
import org.rapidnewsawards.messages.AllEditions;
import org.rapidnewsawards.messages.FullStoryInfo;
import org.rapidnewsawards.messages.Name;
import org.rapidnewsawards.messages.RecentSocials;
import org.rapidnewsawards.messages.RecentVotes;
import org.rapidnewsawards.messages.RelatedUserInfo;
import org.rapidnewsawards.messages.Return;
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

		// TODO parameterize by periodical
		public AllEditions getAllEditions() {
			LinkedList<Edition> ll = new LinkedList<Edition>();
			for (Edition e : ofy().query(Edition.class)) {
				ll.add(e);
			}
			Edition c = getCurrentEdition();
			AllEditions ae = new AllEditions(ll, c);
			return ae;
		}

		public Edition getCurrentEdition() {
			return getEdition(-1);
		}

		/*
		 * Just get the requested edition
		 * 
		 * @param number the edition number requested, or -1 for current, or -2
		 * for next
		 */
		public Edition getEdition(int number) {

			Objectify o = ofy();

			final Periodical p = getPeriodical();

			if (p == null) {
				throw new IllegalStateException("no periodical");
			}
			
			if (number == -1) {
				if (!p.live)
					return null;

				return o.find(p.getCurrentEditionKey());
			}

			if (number == -2) {
				if (!p.live) {
					log.warning("Next edition requested for dead periodical");
					return null;
				}
				return o.find(
						Edition.getNextKey(p.getCurrentEditionKey().getName()));
			}

			// TODO 2.0 this only works because we assume one periodical
			return o.find(Edition.class, "" + number);
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

		private Edition getLastEdition(final Periodical p) {
			Edition e;
			e = ofy().find(
					Edition.getPreviousKey("" + editions.getNumEditions(p)));
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
			return getEdition(-2);
		}

		// cache the number of editions -- will NEVER change.
		private int numEditions = -1;

		public int getNumEditions() {
			if (numEditions == -1) {
				numEditions = _getNumEditions();
			}
			return numEditions;
		}

		private int _getNumEditions() {
			final Periodical p = findByFieldName(Periodical.class, Name.NAME,
					periodicalName.name, null);

			if (p == null) {
				log.severe("Can't find periodical: " + periodicalName);
				return 0;
			}

			return getNumEditions(p);
		}

		public int getNumEditions(Periodical p) {
			return p.numEditions;
		}

		public RecentVotes getRecentVotes(int edition) {
			Edition e = editions.getEdition(edition);
			RecentVotes s = new RecentVotes();
			s.edition = e;
			s.numEditions = editions.getNumEditions();
			if (e == null) {
				log.warning("no recent votes for bad edition " + edition);
			} else {
				s.list = getLatestUser_Vote_Links(e);
			}
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
			tj.edition = e;
			tj.numEditions = editions.getNumEditions();
			tj.list = getJudges(e);
			return tj;
		}

		public TopStories getTopStories(int editionNum) {
			Edition e = editions.getEdition(editionNum);

			TopStories result = new TopStories();
			LinkedList<StoryInfo> stories = new LinkedList<StoryInfo>();
			result.edition = e;
			result.numEditions = editions.getNumEditions();
			result.list = stories;

			if (e == null)
				return result;

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
				vr.returnVal = Return.NOT_LOGGED_IN;
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
					vr.returnVal = Return.BAD_URL;
				}
			}
			return vr;
		}

		public void updateAuthorities(int next) {
			// TODO 2.0 No transaction here.  This is idempotent and 
			// correct, but not very scalable.
			
			Edition e = editions.getEdition(next);
			LinkedList<User> users = new LinkedList<User>();
			LinkedList<EditionUserAuthority> eaus = new LinkedList<EditionUserAuthority>();
			// TODO 2.0 careful: this could return hundreds of judges
			for (User u : ofy().query(User.class).filter("isEditor", false)) {
				int tmp = ofy().query(Follow.class).filter("judge", u.getKey())
						.count();
				u.authority = tmp;
				users.add(u);
				EditionUserAuthority eua = new EditionUserAuthority(
						u.authority, e.getKey(), u.getKey());
				eaus.add(eua);
			}

			ofy().put(users);
			ofy().put(eaus);
			TransitionTask.setBalance();			
		}

		private Edition getPreviousEdition(LockedPeriodical lp) {
			Key<Edition> current = lp.periodical.getCurrentEditionKey();
			if (current == null) {
				// last edition
				current = new Key<Edition>(Edition.class, ""
						+ (editions.getNumEditions(lp.periodical) - 1));
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
			ScoreSpace s = ofy().query(ScoreSpace.class).filter("edition", key).get();				
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
		public Objectify transaction;
		public Periodical periodical;

		public LockedPeriodical(Objectify o, Periodical p) {
			this.transaction = o;
			this.periodical = p;
		}

		public void release() {
			this.periodical.flag = !this.periodical.flag;
			this.transaction.put(this.periodical);
			this.transaction.getTxn().commit();
		}

		public void rollback() {
			this.transaction.getTxn().rollback();
		}
	}

	public class Social {

		/*
		 * Do a follow, unfollow, or cancel pending follow
		 * 
		 * @param e this should be the next edition after current
		 */
		public Return doSocial(Key<User> from, Key<User> to, Edition e,
				boolean on) {
			LockedPeriodical lp = lockPeriodical();

			// TODO handle the case where this is the last edition

			assert(lp != null);
			
			if (!lp.periodical.getCurrentEditionKey()
					.equals(e.getPreviousKey())) {
				log.warning("Attempted to socialize in old edition");
				lp.release();
				return Return.NO_LONGER_CURRENT;
			}

			if (lp.periodical.inSocialTransition) {
				log.warning("Attempted to socialize during transition");
				lp.release();
				return Return.TRANSITION_IN_PROGRESS;
			}

			Return r = Return.SUCCESS;
			Objectify o = ofy();

			final Follow following = getFollow(from, to, o);
			final SocialEvent aboutToSocial = getAboutToSocial(from, to, e, o);

			if (on) {
				if (aboutToSocial != null && !aboutToSocial.on) {
					// cancel (delete) the pending unfollow
					o.delete(aboutToSocial);
					r = Return.PENDING_UNFOLLOW_CANCELLED;
				} else if (aboutToSocial != null && aboutToSocial.on) {
					log.warning("Already about to follow: [" + from + ", " + to
							+ "]");
					r = Return.ALREADY_ABOUT_TO_FOLLOW;
				} else if (following != null) {
					log.warning("Already isFollowing: [" + from + ", " + to
							+ "]");
					r = Return.ALREADY_FOLLOWING;
				} else if (!users.isEditor(from)) {
					r = Return.NOT_AN_EDITOR;
				} else {
					// this follow won't take effect until a transition
					final SocialEvent follow = new SocialEvent(from, to,
							e.getKey(), new Date(), on);
					r = Return.ABOUT_TO_FOLLOW;
					o.put(follow);
				}
			} else if (following != null) {
				// this unfollow won't take effect until a transition
				final SocialEvent unfollow = new SocialEvent(from, to,
						e.getKey(), new Date(), on);
				o.put(unfollow);
				r = Return.ABOUT_TO_UNFOLLOW;
			} else if (aboutToSocial != null) {
				// cancel (delete) the pending follow
				o.delete(aboutToSocial);
				r = Return.PENDING_FOLLOW_CANCELLED;
			} else {
				log.warning("Can't unfollow unless isFollowing: " + from + ", "
						+ to + ", " + e);
				r = Return.NOT_FOLLOWING;
			}

			lp.release();
			return r;
		}

		public Return doSocial(User to, Boolean on) {
			if (user == null) {
				log.warning("attempt to follow with null user");
				return Return.ILLEGAL_OPERATION;
			}

			// read-only transaction
			Edition e = editions.getEdition(-2);
			if (e == null) {
				log.warning(user
						+ "Attempted to socialize during final edition");
				return Return.FORBIDDEN_DURING_FINAL;
			}
			Return result = social.doSocial(user.getKey(), to.getKey(), e, on);
			return result;
		}

		public SocialEvent getAboutToSocial(Key<User> from, Key<User> to,
				Edition e, Objectify o) {
			if (o == null)
				o = instance.ofy();

			if (e == null)
				return null;

			return o.query(SocialEvent.class).filter("editor", from)
					.filter("judge", to).filter("edition", e.getKey()).get();
		}

		public Follow getFollow(Key<User> from, Key<User> to, Objectify o) {
			if (o == null)
				o = instance.ofy();

			return o.query(Follow.class).ancestor(from).filter("judge", to)
					.get();
		}

		public RecentSocials getRecentSocials(int edition) {
			Edition current = null;
			Edition next = null;

			if (edition == -1) {
				current = editions.getEdition(-1);
				next = editions.getEdition(-2);
			} else {
				// next after edition
				current = editions.getEdition(edition);
				next = editions.getEdition(edition + 1);
			}

			RecentSocials s = new RecentSocials();
			s.edition = current;
			s.numEditions = editions.getNumEditions();
			s.list = editions.getLatestEditor_Judges(next);
			return s;
		}

		public boolean isFollowingOrAboutToFollow(Key<User> from, Key<User> to) {
			Edition e = editions.getEdition(-2);
			SocialEvent about = getAboutToSocial(from, to, e, null);

			if (about != null) {
				return about.on;
			} else {
				Follow f = getFollow(from, to, null);
				return f != null;
			}
		}
	}

	public class Transition {

		private void _transitionEdition(LockedPeriodical lp) {

			final Periodical p = lp.periodical;

			if (p == null) {
				log.severe("No Periodical");
				return;
			}

			if (!p.live) {
				log.warning("tried to transition a dead periodical");
				return;
			}

			Edition current = ofy().find(p.getCurrentEditionKey());

			if (current == null) {
				log.severe("no edition matching" + p.getCurrentEditionKey());
				return;
			}

			int nextNum = current.number + 1;
			int n = editions.getNumEditions(p);

			if (nextNum == n) {
				p.live = false;
			} else if (nextNum > n) {
				// TODO throw?
				log.severe("bug in edition numbers: " + nextNum);
				return;
			} else {
				// Do it! Change current edition.
				p.setcurrentEditionKey(new Key<Edition>(Edition.class, ""
						+ nextNum));
			}

			p.inSocialTransition = true;
			TransitionTask.doSocialTransition(lp.transaction.getTxn(), nextNum);
			lp.release();
			log.info(p.name + ": New current Edition:" + nextNum);
		}

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

			if (lp == null) {
				log.severe("finish failed");
				return;
			}

			final Periodical p = lp.periodical;

			if (p == null) {
				log.severe("No Periodical");
				return;
			}

			if (!p.live) {
				log.warning("tried to finish a dead periodical");
			}

			p.live = false;
			p.setcurrentEditionKey(null);
			lp.release();
		}

		public void setBalance() {

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
			
			if (!p.live) {
				log.warning("spending all remaining revenue");

				e = editions.getLastEdition(p);
				if (e == null) {
					throw new IllegalStateException("no final edition");
				}
				s = editions.getScoreSpace(e.getKey());
				assert (s != null);
				s.revenue = p.balance;
				p.balance = 0;
			} else {
				e = editions.getPreviousEdition(lp);

				if (e == null) {
					throw new IllegalStateException("no previous edition");
				}

				s = editions.getScoreSpace(e.getKey());
				assert (s != null);
				int n = editions.getNumEditions(p);
				s.revenue = p.balance / (n - e.number);
				p.balance -= s.revenue;
			}

			TransitionTask.setRevenue(lp.transaction.getTxn(), e, s.revenue);
			lp.release();

			log.info(e + ": revenue " + Periodical.moneyPrint(s.revenue));
			log.info("balance: " + Periodical.moneyPrint(p.balance));
		}

		/*
		 * makes pending social actions current. part of the transition
		 * machinery.
		 */
		public void socialTransition(int _to) {

			Edition to = editions.getEdition(_to);
			Objectify o = ofy();
			
			// obtain lock
			LockedPeriodical lp = lockPeriodical();
			assert (lp != null);
			
			log.info("Social transition into " + to);

			for (SocialEvent s : o.query(SocialEvent.class)
					.filter("edition", to.getKey())
					.filter("editor !=", User.getRNAEditor())) {
				// TODO If this crashes we're screwed -- parent to periodical
				Follow old = ofy().query(Follow.class).ancestor(s.editor)
						.filter("judge", s.judge).get();

				if (s.on == false) {
					if (old == null) {
						log.warning("Permitted unfollow when not isFollowing"
								+ s);
					} else {
						o.delete(old);
						log.info("Unfollowed: " + old);
					}
				} else {
					if (old != null) {
						log.warning("Permitted follow when already isFollowing"
								+ s);
					} else {
						// put new follow into effect
						Follow f = new Follow(s.editor, s.judge, s.getKey());
						o.put(f);
					}
				}
			}
			TransitionTask.updateAuthorities(lp.transaction.getTxn(), _to);
			lp.release();
		}

		public boolean transitionEdition() {

			LockedPeriodical locked = lockPeriodical();

			if (locked == null) {
				log.severe("publish failed");
				return false;
			}

			transition._transitionEdition(locked);
			
			return true;
		}

		public void finishTransition() {
			// the final step in transition machinery!
			LockedPeriodical lp = lockPeriodical();
			lp.periodical.inSocialTransition = false;
			lp.transaction.put(lp.periodical);	
			lp.transaction.getTxn().commit();
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
			Objectify o = ofy();
			int count = o.query(Vote.class).ancestor(u)
					.filter("edition", e.getKey()).filter("link", l.getKey())
					.count();
			if (count > 1) {
				log.severe("too many eventPanel for user " + u);
			}
			return count == 1;
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
				vr.returnVal = Return.NOT_LOGGED_IN;
				vr.authUrl = userService.createLoginURL(fullLink);
				return vr;
			}

			Link l = DAO.instance.findByFieldName(Link.class, Name.URL, link,
					null);
			if (l == null) {
				return null;
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
		public Return voteFor(User u, Edition e, Link l, Boolean on)
				throws IllegalArgumentException {
			// TODO only judges can vote, ditto for ed follows

			// obtain lock
			LockedPeriodical lp = lockPeriodical();

			if (lp == null || e == null) {
				log.warning("vote failed: " + u + " -> " + l.url);
				return Return.FAILED;
			}

			if (!lp.periodical.getCurrentEditionKey().equals(e.getKey())) {
				log.warning("Attempted to vote in old edition");
				lp.rollback();
				return Return.NO_LONGER_CURRENT;
			}

			if (lp.periodical.inSocialTransition) {
				log.warning("Attempted to vote during transition");
				lp.rollback();
				return Return.TRANSITION_IN_PROGRESS;
			}

			if (on) {
				if (hasVoted(u, e, l)) {
					lp.rollback();
					return Return.ALREADY_VOTED;
				}

				Vote v = new Vote(u.getKey(), e.getKey(), l.getKey(),
						new Date(), u.authority);
				Objectify txn = fact().beginTransaction();
				txn.put(v);
				log.info(u + " " + u.authority + " -> " + l.url);
				TallyTask.tallyVote(txn.getTxn(), v);
				txn.getTxn().commit();
				lp.release();
				return Return.SUCCESS;
			} else {
				Vote v = ofy().query(Vote.class).filter("edition", e.getKey())
						.filter("link", l.getKey()).get();
				if (v == null) {
					lp.rollback();
					return Return.HAS_NOT_VOTED;
				} else {
					Objectify txn = fact().beginTransaction();
					txn.delete(v);
					// release lock
					TallyTask.tallyVote(txn.getTxn(), v);
					lp.release();
					return Return.SUCCESS;
				}
			}
		}

		public User welcomeUser(String nickname, int donation) {

			LockedPeriodical lp = lockPeriodical();

			user.nickname = nickname;
			user.isInitialized = true;

			ofy().put(user);

			Donation don = new Donation(user.getKey(), donation);

			ofy().put(don);

			log.info("welcome: " + user + ": "
					+ Periodical.moneyPrint(donation));

			Edition next = editions.getNextEdition();

			if (next == null) {
				log.warning("join failed");
				return null;
			} else {
				SocialEvent join = new SocialEvent(User.getRNAEditor(),
						user.getKey(), next.getKey(), new Date(), true);
				ofy().put(join);
			}

			// TODO not used
			lp.periodical.balance += donation;
			lp.transaction.put(lp.periodical);
			lp.release();

			log.info("balance: " + Periodical.moneyPrint(lp.periodical.balance));

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
	
	public Name periodicalName = Name.AGGREGATOR_NAME;



	/*
	 * Sub-modules for data access
	 */
	public Social social;

	public Transition transition;

	public Users users;

	public Editions editions;

	public static final Logger log = Logger.getLogger(DAO.class.getName());

	private static int RETRY_TIMES = 5;

	public DAO() {
		user = null;
		social = new Social();
		transition = new Transition();
		editions = new Editions();
		users = new Users();
	}

	public Periodical getPeriodical() {
		// TODO need a cache filter?
		
		Root root = ofy().get(Root.class, 1L);
		// TODO 2.0 Why can't I filter this by name?  BUG.
		return ofy().query(Periodical.class).ancestor(root).get();
	}

	// clients should call convenience methods above
	private <T> T findByFieldName(Class<T> clazz, Name fieldName, Object value,
			Objectify o) {
		if (o == null)
			o = ofy();
		return o.query(clazz).filter(fieldName.name, value).get();
	}

	private LockedPeriodical lockPeriodical() {
		Objectify oTxn = fact().beginTransaction();
		for (int i = 0; i < RETRY_TIMES; i++) {
			try {
				// TODO 2.0 Need a periodical name
				Root root = ofy().find(Root.class, 1);
				Periodical p = oTxn.query(Periodical.class).ancestor(root).get();
				return new LockedPeriodical(oTxn, p);
			} catch (Error e) {
				log.warning("lock failed, i = " + i);
			}
		}
		throw new IllegalStateException("failed to lock periodical");
	}

	int revenue(int score, int totalScore, int editionFunds) {
		return (int) (score / (double) totalScore * editionFunds);
	}

	public void tallyVote(Key<Vote> vote) {
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
		otx.getTxn().commit();
	}

}