package org.rapidnewsawards.server;

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

import org.rapidnewsawards.core.Donation;
import org.rapidnewsawards.core.Edition;
import org.rapidnewsawards.core.EditorInfluence;
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

	public class Editions {

		// TODO 2.0 make enums
		public static final int CURRENT = -1;
		public static final int NEXT = -2;
		public static final int FINAL = -3;
		private static final int PREVIOUS = -4;
		public static final int CURRENT_OR_FINAL = -5;

		private EditionMessage makeEditionMessage(Edition e) {
			return new EditionMessage(e,
					ofy().get(ScoreSpace.keyFromEditionKey(e.getKey())));		
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
		public Edition getEdition(final int edition) throws RNAException {

			final Objectify o = ofy();

			if (edition == CURRENT || edition == CURRENT_OR_FINAL) {
				final Periodical p = getPeriodical();
				if (p.isFinished()) {
					if (edition == CURRENT_OR_FINAL) {
						return getEdition(FINAL);
					}
					else {
						throw new RNAException("no current edition");
					}
				}
				return o.get(p.getcurrentEditionKey());
			}

			else if (edition == NEXT) {
				final Periodical p = getPeriodical();
				if (p.isFinished()) {
					throw new RNAException(
							"Next edition requested for finished periodical");
				}
				Key<Edition> nextKey = Edition.getNextKey(p.getcurrentEditionKey());
				if (Edition.isAfterFinal(nextKey, getNumEditions())) {
					throw new RNAException(
					"Next edition requested after final Edition");					
				}
				return o.get(nextKey);
			}
			else if (edition == FINAL) {
				return o.get(Edition.getFinalKey(getNumEditions()));
			}
			else if (edition == PREVIOUS) {
				if (edition == 0) {
					throw new RNAException("There is no previous edition");
				}
				final Periodical p = getPeriodical();
				if (p.isFinished())	
					return getEdition(FINAL);
				return o.get(Edition.getPreviousKey(p.getcurrentEditionKey()));
			}
			else {
				// TODO 2.0 this only works because we assume one periodical
				return o.get(Edition.class, "" + edition);
			}
		}

		// TODO refactor with getvoters
		public LinkedList<InfluenceMessage> getJudges(Edition e) {
			LinkedList<InfluenceMessage> result = new LinkedList<InfluenceMessage>();

			Map<Key<User>, Integer> authorities = new HashMap<Key<User>, Integer>();
			Map<Key<User>, Integer> funds = new HashMap<Key<User>, Integer>();
			ArrayList<Key<User>> judges = new ArrayList<Key<User>>();

			for (JudgeInfluence ji : ofy().query(
					JudgeInfluence.class).ancestor(e.getKey())) {
				authorities.put(ji.user, ji.authority);
				funds.put(ji.user, ji.funded);
				judges.add(ji.user);
			}

			if (judges.size() > 0) {
				Map<Key<User>, User> vmap = ofy().get(judges);

				for (int i = 0; i < judges.size(); i++) {
					result.add(new InfluenceMessage(vmap.get(judges.get(i)),
							authorities.get(judges.get(i)),
							funds.get(judges.get(i))));
				}

				Collections.sort(result);
			}
			
			return result;
		}

		// TODO refactor with getvoters
		public LinkedList<InfluenceMessage> getTopEditors(Edition e) {
			LinkedList<InfluenceMessage> result = new LinkedList<InfluenceMessage>();

			Map<Key<User>, Integer> funds = new HashMap<Key<User>, Integer>();
			ArrayList<Key<User>> editors = new ArrayList<Key<User>>();

			for (EditorInfluence ei : ofy().query(
					EditorInfluence.class).ancestor(e.getKey())) {
				funds.put(ei.editor, ei.funded);
				editors.add(ei.editor);
			}

			if (editors.size() > 0) {
				Map<Key<User>, User> vmap = ofy().get(editors);

				for (int i = 0; i < editors.size(); i++) {
					result.add(new InfluenceMessage(vmap.get(editors.get(i)),
							0,
							funds.get(editors.get(i))));
				}

				Collections.sort(result);
			}
			
			return result;
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
			LinkedList<User_Vote_Link> result = new LinkedList<User_Vote_Link>();

			ArrayList<Key<User>> users = new ArrayList<Key<User>>();
			ArrayList<Key<Link>> links = new ArrayList<Key<Link>>();

			Query<Vote> q = ofy().query(Vote.class)
					.filter("edition", e).order("-time");

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
			RecentVotes s = new RecentVotes();
			s.edition = makeEditionMessage(e);
			s.numEditions = getNumEditions();
			s.list = getLatestUser_Vote_Links(e.getKey());
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
			si.editionId = editionNum;
			si.submitter = ofy().get(link.submitter);
			si.setFunding(sl.funding);

			FullStoryInfo fsi = new FullStoryInfo();
			fsi.info = si;
			fsi.funds = getVoters(linkKey, editionKey);
			return fsi;
		}

		public TopJudges getTopJudges(int edition) throws RNAException {
			Edition e = editions.getEdition(edition);
			TopJudges tj = new TopJudges();
			tj.edition = makeEditionMessage(e);
			tj.numEditions = editions.getNumEditions();
			tj.list = getJudges(e);
			return tj;
		}

		public TopEditors getTopEditors(int edition) throws RNAException {
			Edition e = editions.getEdition(edition);
			TopEditors tj = new TopEditors();
			tj.edition = makeEditionMessage(e);
			tj.numEditions = editions.getNumEditions();
			tj.list = getTopEditors(e);
			return tj;
		}

		
		public TopStories getTopStories(int editionNum) throws RNAException {
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
				si.editionId = e.getNumber();
				si.submitter = userMap.get(si.link.submitter);
				si.setFunding(sl.funding);
				stories.add(si);
			}

			return result;
		}

		public VoteResult submitStory(String url, String title,
				Key<Edition> e) throws RNAException {
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
				try {
					Link l = users.createLink(url, title, user.getKey());
					vr.returnVal = users.voteFor(
							user,
							e, l, true);
				}
				catch (MalformedURLException ex) {
					// TODO Just Catch this in parent?
					// TODO Test on frontend
					log.warning("bad url " + url + "submitted by " + user);
					vr.returnVal = Response.BAD_URL;
				}
			}
			return vr;
		}

		public LinkedList<InfluenceMessage> getVoters(Key<Link> l, Key<Edition> e) {
			LinkedList<InfluenceMessage> result = new LinkedList<InfluenceMessage>();

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
				result.add(new InfluenceMessage(vmap.get(voters.get(i)),
						// TODO 2.0 Add Funding here.
						authorities.get(voters.get(i)), 0));
			}

			Collections.sort(result);
			return result;
		}

		public ScoreSpace getScoreSpace(Key<Edition> key) {
			Key<ScoreRoot> sroot = new Key<ScoreRoot>(ScoreRoot.class, key.getName());
			Key<ScoreSpace> skey = new Key<ScoreSpace>(sroot,
					ScoreSpace.class, key.getName());
			ScoreSpace s;
			if ((s = ofy().find(skey)) == null) { throw new IllegalStateException(); }
			return s;
		}

		public void setSpaceBalance(int edition, int balance) {
			assert(getPeriodical().inTransition);
			ScoreSpace s = getScoreSpace(Edition.createKey(edition));
			Objectify oTxn = fact().beginTransaction();
			s.balance = balance;
			oTxn.put(s);
			TransitionTask.finish(oTxn.getTxn());
			oTxn.getTxn().commit();
		}


		public void setEditionFinished(int edition) throws RNAException {
			ScoreSpace s = getScoreSpace(Edition.createKey(edition));
			s.finished = true;
			Objectify oTxn = fact().beginTransaction();
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
			Periodical p = oTxn.get(Periodical.getKey(periodicalName.name));
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
				lp.commit(); socialTxn.getTxn().commit();
				assert(!getPeriodical().inTransition);
			}
		}

		public void changePendingAuthority(Key<User> judge, Key<Edition> edition,
				int amount) {
			assert(getPeriodical().userlocked);
			assert(!getPeriodical().inTransition);

			Objectify txn = fact().beginTransaction();
			JudgeInfluence ji = txn.query(JudgeInfluence.class).ancestor(edition)
			.filter("user", judge).get();
			if (ji == null) {
				assert(amount > 0);
				ji = new JudgeInfluence(0, edition, judge);
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
			Edition preceeding = editions.getEdition(edition);

			RecentSocials s = new RecentSocials();
			s.edition = editions.makeEditionMessage(preceeding);
			s.numEditions = editions.getNumEditions();

			Edition succeeding = null;
			if (!Edition.isFinal(preceeding.getKey(), editions.getNumEditions())) {
				succeeding = 
					ofy().get(Edition.getNextKey(preceeding.getKey()));
			}
			s.list = editions.getLatestEditor_Judges(succeeding);
			return s;
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

		public void doTransition(int editionNum) throws RNAException {
			Edition current = editions.getCurrentEdition();
			if (Edition.getNumber(current.getKey()) != editionNum) {
				throw new RNAException(Integer.toString(editionNum) + " != " + current);
			}
			else {
				transition.transitionEdition();
			}
		}

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
				throw new ConcurrentModificationException(); 
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
		}

		public void finishTransition() throws RNAException {
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
				throw new RNAException("No such user exists");
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
			if (u == null) {
				log.severe("bad input");
				return false;
			}
			return u.isEditor;
		}

		public VoteResult voteFor(String link, String fullLink,
				Key<Edition> edition, Boolean on) throws RNAException {
			VoteResult vr = new VoteResult();
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

			Link l = ofy().query(Link.class).filter(Name.URL.name, link).get();
			if (l == null) {
				return null; // User must submit the link
			} else {
				vr.returnVal = voteFor(
						user, edition, l, on);
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
		 * @throws RNAException 
		 */
		public Response voteFor(User u, Key<Edition> e, Link l, boolean on)
				throws RNAException {
			// TODO only judges can vote, ditto for ed follows

			assert(e != null);
			
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
				JudgeInfluence ji = getJudgeInfluence(ofy(), uk, ek);
				int authority = ji.authority;
				Vote v = new Vote(uk, ek, lk, new Date(), authority);
				txn.put(v);
				TallyTask.tallyVote(txn.getTxn(), v);
				txn.getTxn().commit();
			} else {
				// TODO this
				throw new IllegalArgumentException("no negative voting yet");
				/*
				Vote v = ofy().query(Vote.class).filter("edition", ek)
				.filter("link", lk).get();
				txn.delete(v);
				// TODO pass in actual values -- vote is gone!
				TallyTask.tallyVote(txn.getTxn(), v); */
			}
		}

		public JudgeInfluence getJudgeInfluence(Objectify ofy, Key<User> uk, Key<Edition> ek) {
			JudgeInfluence ji = ofy.query(JudgeInfluence.class).ancestor(ek)
			.filter("user", uk).get();
			return ji;
		}

		public EditorInfluence getEditorInfluence(Objectify ofy, Key<User> editor, Key<Edition> ek) {
			EditorInfluence ei = ofy.query(EditorInfluence.class).ancestor(ek)
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
				throw new RNAException("A web page is required so editors can get to know you.");			
			}
			boolean c = Boolean.parseBoolean(consent);
			if (!c) {
				throw new RNAException("You did not check the consent form.");
			}
			
			user.nickname = nickname;
			user.isInitialized = true;
			user.webPage = normalizeWebPage(webPage);
			
			ofy().put(user);

			// GO ahead and accept their changes, but this should not happen.
			if (hasAlreadyJoined(user)) {
				log.warning("user changed their information: " + user);
				return user;
			}

			log.info("welcome: " + user);

			Edition next = editions.getNextEdition();

			SocialEvent join = new SocialEvent(User.getRNAEditor(),
					user.getKey(), next.getKey(), new Date(), true);
			ofy().put(join);
				
			if (!user.isEditor) {
				for(int i = editions.getCurrentEdition().number;
				i < editions.getNumEditions();
				i++) {
					Key<Edition> eKey = Edition.createKey(i);
					ofy().put(new JudgeInfluence(0, 
							eKey, user.getKey()));
				}
			}
			return user;
		}

		private boolean hasAlreadyJoined(User u) {
			return ofy().query(SocialEvent.class)
					.ancestor(User.getRNAEditor()).filter("judge", u.getKey()).get()
					!= null;
		}

	}

	static {
		ObjectifyService.factory().register(Donation.class);
		ObjectifyService.factory().register(Edition.class);
		ObjectifyService.factory().register(EditorInfluence.class);
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

	public static final Logger log = Logger.getLogger(DAO.class.getName());

	public DAO() {
		user = null;
		social = new Social();
		transition = new Transition();
		editions = new Editions();
		users = new Users();
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
	public void tallyVote(Key<Vote> vote) {
		assert(getPeriodical().userlocked);
		Vote v = ofy().get(vote);
		Objectify otx = fact().beginTransaction();
		ScoreSpace space = editions.getScoreSpace(v.edition);
		
		ScoredLink sl = otx.query(ScoredLink.class)
		.ancestor(space.root).filter("link", v.link).get();
		
		space.totalScore += v.authority;
		int fund = funding(v.authority, space.totalScore, space.balance);
		space.totalSpend += fund;
		
		if (sl == null) {
			sl = new ScoredLink(v.edition, space.root, v.link, v.authority, fund);
			space.numFundedLinks++;
		}
		else {
			sl.score += v.authority;
			sl.funding += fund;
		}
		otx.put(sl);
		otx.put(space);
		TallyTask.addJudgeFunding(otx.getTxn(), v, fund);
		otx.getTxn().commit();
	}

	public void addJudgeFunding(Key<Vote> vkey, int fund) {
		assert(getPeriodical().userlocked);
		Vote v = ofy().get(vkey);
		Objectify otx = fact().beginTransaction();
		JudgeInfluence ji = users.getJudgeInfluence(otx, v.voter, v.edition);
		ji.funded += fund;
		otx.put(ji);
		TallyTask.findEditorsToFund(otx.getTxn(), v, fund);
		otx.getTxn().commit();
	}
	
	public void findEditorsToFund(Key<Vote> vkey, int fund) {
		assert(getPeriodical().userlocked);
		Vote v = ofy().get(vkey);
		Objectify otx = fact().beginTransaction();
		Set<Key<User>> editors = new HashSet<Key<User>>();
		for (FollowedBy fb : otx.query(FollowedBy.class).ancestor(v.voter)
				.filter("edition", v.edition)) {
			editors.add(fb.editor);
		}
		TallyTask.addEditorFunding(otx.getTxn(), editors, v.edition, fund);
		otx.getTxn().commit();
	}

	public void addEditorFunding(Set<Key<User>> editors, Key<Edition> edition, int fund) {
		assert(fund % editors.size() == 0);
		Objectify otx = fact().beginTransaction();
		Set<EditorInfluence> eiset = new HashSet<EditorInfluence>();
		for (Key<User> editor : editors) {
			EditorInfluence ei = otx.query(EditorInfluence.class).ancestor(edition)
			.filter("editor", editor).get();
			// TODO Write a test for this invariant that checks this and judge influence
			ei.funded += fund / editors.size();
			eiset.add(ei);
		}
		otx.put(eiset);
		TallyTask.releaseUserLock(otx.getTxn());		
		otx.getTxn().commit();
	}

	public void releaseUserLock() throws RNAException {
		LockedPeriodical lp = lockPeriodical();
		lp.releaseUserLock();
		lp.commit();
	}

	public void donate(String name, String donation, String webPage,
			String statement, String consent) throws RNAException {
		int amount = parseDonation(donation);
		if (Strings.isNullOrEmpty(name)) {
			throw new RNAException("A name is required.");			
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
				throw new RNAException("That amount is too high.");
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