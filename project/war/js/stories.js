// -*- outline-regexp:  "[ \t]*//[*]+" -*-

// Load the application once the DOM is ready, using `jQuery.ready`:
$(function(){

    //* globals

    function defaultAction() {
	flashInfo('');
	if (Backbone.history.getFragment() == '') {
	    window.app.topStories();
	}
	app.loginView.checkCreatingAccount();
    }



    //* EditionView - Base Class

    window.EditionView = Backbone.View.extend({

	events: {
	    "click .stories": 'stories',
	    "click .network": 'network',
	},

	tagName: 'div',
	className: 'edition',

	initialize: function(attrs) {
	    this.edition = attrs.edition;
	    this.numEditions = attrs.numEditions;
	    this.list = attrs.list;
	    this.getAttrs = attrs.getAttrs;
	    this.itemView = attrs.itemView;
	    this.networkSelected = attrs.networkSelected;
	    this.storiesSelected = attrs.storiesSelected;
	    // fixme refactor
	    var self = this;
	    this.list.bind('add',     function () { self.addOne() });
	    this.list.bind('refresh', function () { self.addAll() });
	    $(this.el).append(this.make('div', {id: 'bodyLine', class: 'hugeBottom'}));
	    $(this.el).append(this.make('ul', {class: 'spine large'}));
	},

	addOne: function(model) {
	    var view = new this.list.view({model: model});
	    this.appendElt(view.render().el);
	},

	addAll: function() {
	    var view = this;
	    view.list.each(function (item) { view.addOne(item) });
	},

	appendElt: function(el) {
	    this.$('ul').append(el);
	},

	/* Called by constructor in subclass */
	refresh: function(list) {
	    var self = this;
	    this.list.refresh (
		(_.map(list,
		       function (s) { 
			   return new self.list.model(self.getAttrs? self.getAttrs(s) : s); 
		       })));
	},

	stories: function() {
	    app.hashTopStories(this.edition.number);
	},

	network: function() {
	    app.hashNetwork(this.edition.number);
	},

	render: function() {
	    // these go above the edition list, so they are prepended
	    // fixme change to a table
	    var div = this.make("div", {class: "editionTabs large"});
	    $(div).html(rMake('#edition-tabs-template', 
			      {storiesSelected: this.storiesSelected,
			       networkSelected: this.networkSelected}));
	    $(this.el).prepend(div);
	    if (this.edition.number == 0) {
		$(this.el).prepend(rMake('#signup-round-template', 
					 this.edition));		
	    }
	    else {
		$(this.el).prepend(rMake('#edition-header-template', 
					 this.edition));
	    }
	    return this;
	},

    });
    

    //* Stories - Top or Recent
    window.Story = Backbone.Model.extend({});

    window.StoryView = Backbone.View.extend({

	tagName:  "li",
	className: "story",

	// Cache the template function for a single story.
	template: _.template($('#story-template').html()),

	events: {
	    "click .submitter": 'submitter',
	},

	initialize: function() {
	    var self = this;
	    this.model.bind('change', function () { self.render() });
	    this.model.view = this;
	},

	submitter: function() {
	    app.hashPerson(this.model.get('submitter').id);
	},

	render: function() {
	    var _copy = this.model.toJSON();
	    _copy.revenue = '' +_copy.revenue / 100;
	    _copy.href= '#story/' + _copy.editionId + '/' + _copy.link.id;
	    $(this.el).html(this.template(_copy));
	    return this;
	},

	
    });

    window.StoriesList = Backbone.Collection.extend({

	model: Story,
	view: StoryView,

	comparator: function(story) {
	    return story.get('score');
	}

    });

    window.Funding = Backbone.Model.extend({

    });

    window.FundingView = Backbone.View.extend({

	tagName:  "li",
	className: "story", // fixme

	events: {
	    "click a": 'person'
	}, 

	initialize: function() {
	    var self = this;
	    this.model.bind('change', function () { self.render() });
	    this.model.view = this;
	},

	person: function() {
	    app.hashPerson(this.model.get('id'));
	},

	render: function() {
	    $(this.el).html(rMake('#funding-template', this.model.toJSON()));
	    return this;
	},	
    });

    window.FundingsList = Backbone.Collection.extend({
	model: Funding,
	view: FundingView,

	comparator: function(model) {
	    return - model.get('vote').time;
	}

    });


    window.StoriesView = EditionView.extend({

	constructor: function (options) {
	    options.list = (options.order == 'top'? 
			    new StoriesList : new FundingsList);
	    this.order = options.order;
	    // run super.initialize
	    Backbone.View.apply(this.constructor.__super__, [options]);
	    // bind this.render
	    var self = this;
	    this.list.bind('all', function () { self.render() });
	    this.refresh(options.data);
	    this.bindEvents();
	},

	
	bindEvents: function () {
	    var self = this;
	    this.$("a.top").click(function(event) {
		self.topStories();
	    });
	    this.$("a.recent").click(function(event) {
		self.recentFundings();
	    });
	},

	topStories: function() {
	    window.app.hashTopStories(this.edition.number);
	},

	recentFundings: function() {
	    window.app.hashRecentFundings(this.edition.number);
	},

	render: function() {
	    this.constructor.__super__.render();
	    var args = 
		{topSelected: this.order == 'top'? 'selected' : 'unselected',
		 recentSelected: this.order == 'recent'? 'selected' : 'unselected'};
	    this.$('#editionTabsMinor').html(rMake('#stories-order-tab-template', args));

	    if (this.list.length == 0) {
		var message = this.edition.number > 0 ?
		    "The judges have not funded this edition." :
		    "Funding will begin after the signup round.";
		this.appendElt(this.make("li", {class: 'empty'}, 
					 message));
	    }
	    return this;
	}
    });

    var GenList = function(attrs) {
	this.list = attrs.list;
	this.parent = attrs.parent;
	this.newModel = attrs.newModel == undefined?
	    function (item) { return item; } : attrs.newModel;

	var self = this;
	this.list.bind('all', function () { self.parent.render() });
	this.list.bind('refresh', function () { self.addAll(); });

	this.appendElt = attrs.appendElt == undefined? 
	    function(el) {
		this.parent.$('ul').append(el);
	    }
	: attrs.appendElt;


	this.newModel = attrs.newModel == undefined? 
	    function(item) {
		return new Backbone.Model(item.toJSON());
	    }
	: attrs.newModel;


	this.addOne = function(model) {
	    var view = new this.list.view({model: model});
	    this.appendElt(view.render().el);
	};

	this.addAll = function() {
	    var self = this;
	    this.list.each(function (item) { self.addOne(self.newModel(item)) });
	};

	this.refresh = function(list) {
	    var self = this;
	    this.list.refresh (
		(_.map(list,
		       function (s) { 
			   return new self.list.model(s) 
		       })));
	};
    };

    //* Social Network  - Top or Recent

    window.Social = Backbone.Model.extend({
	isWelcome: function() {
	    return this.get('editor').id == 1;
	}
    });

    window.SocialView = Backbone.View.extend({

	tagName:  "li",
	className: "social", 

	followTemplate: _.template($('#follow-template').html()),
	welcomeTemplate: _.template($('#welcome-template').html()),

	events: {
	    "click .subject": 'subject',
	    "click .object": 'object',
	}, 

	initialize: function() {
	    var self = this;
	    this.model.bind('change', function () { self.render() });
	    this.model.view = this;
	},

	subject: function() {
	    // fixme app.hashPerson(this.model.get('editor').id);
	},

	object: function() {
	    // fixme app.hashPerson(this.model.get('judge').id);
	},

	show: function(template, data) {
	    $(this.el).html(template(data));
	},

	render: function() {
	    var _copy = this.model.toJSON();
	    _copy.objectLink = app.personLinkTemplate(this.model.get('judge'));
	    if (!this.model.isWelcome()) {
		_copy.subjectLink = app.personLinkTemplate(this.model.get('editor'));
	    }
	    this.show(this.model.isWelcome() ?
		      this.welcomeTemplate : this.followTemplate,
		      _copy);
	    return this;
	},	
    });

    window.SocialList = Backbone.Collection.extend({
	model: Social,
	view: SocialView,
    });

    window.Authority = Backbone.Model.extend({

    });

    window.AuthorityView = Backbone.View.extend({

	tagName:  "li",
	className: "story", // fixme

	events: {
	    "click a": 'person'
	}, 

	initialize: function() {
	    var self = this;
	    this.model.bind('change', function () { self.render() });
	    this.model.view = this;
	},

	person: function() {
	    app.hashPerson(this.model.get('id'));
	},

	render: function() {
	    $(this.el).html(rMake('#authority-template', this.model.toJSON()));
	    return this;
	},	
    });

    window.AuthoritiesList = Backbone.Collection.extend({
	model: Authority,
	view: AuthorityView,

	comparator: function(model) {
	    return model.get('authority');
	}

    });

    window.NetworkView = EditionView.extend({

	bindEvents: function () {
	    var self = this;
	    this.$("a.top").click(function(event) {
		self.topAuthorities();
	    });
	    this.$("a.recent").click(function(event) {
		self.recentSocials();
	    });
	},

	constructor: function (options) {
	    options.list = (options.order == 'top'? 
			    new AuthoritiesList : new SocialList);
	    this.order = options.order;
	    // run super.initialize
	    Backbone.View.apply(this.constructor.__super__, [options]);
	    // bind this.render
	    var self = this;
	    this.list.bind('all', function () { self.render() });
	    this.refresh(options.data);
	    this.bindEvents();
	},

	topAuthorities: function() {
	    window.app.hashTopAuthorities(this.edition.number);
	},

	recentSocials: function() {
	    window.app.hashRecentSocials(this.edition.number);
	},

	render: function() {
	    this.constructor.__super__.render();
	    var args = 
		{topSelected: this.order == 'top'? 'selected' : 'unselected',
		 recentSelected: this.order == 'recent'? 'selected' : 'unselected'};
	    this.$('#editionTabsMinor').html(rMake('#network-order-tab-template', args));

	    if (this.list.length == 0) {
		if (this.order == 'top') {
		    // fixme display 0 judges if there's nothing else.
		    this.appendElt(rMake('#none-followed-template'));
		}
		else {
		    this.appendElt(this.make("li", {class: 'empty'}, 
					     "The network has not changed during this edition."));
		}
	    }
	    return this;
	}

    });


    //* Person
    window.PersonView = Backbone.View.extend({

	tagName: "div",
	id: "person",

	initialize: function() {
	    var self = this;
	    this.model.bind('change', function () { self.render() });
	    this.model.view = this;
	    // fixme refactor
	    $(this.el).append(this.make('ul', {class: 'spine large'}));
	    // add fundings list
	    this.list = 
		new GenList({parent: this, 
			     list: new FundingsList,
			     newModel: function(item) {
				 // don't display a (redundant) user for personView's Fundings
				 return new Backbone.Model(
				     _.extend(item.toJSON(),
					      {user: null}))
			     },
			     appendElt: function(el) {
				 this.parent.$('ul').append(el);
			     }});
	    this.list.refresh(this.model.get('userInfo').votes);
	},

	user: function() {
	    return this.model.get('userInfo').user;
	},

	bindEvents: function(self) {
	    this.$('#is-following').click(function (event) {
		var fol = $(this).is(':checked');		    
		doPostRequest({fun: 'doSocial', 
			       to: self.user().id, on: fol},
			      function(data) {
				  flashLog({type: 'success',
					    content: data || 'I got confused'});
			      });
	    });
	},

	render: function() {
	    var u = _.clone(this.user());
	    u.isFollowing = this.model.get('isFollowing');
	    var html = rMake('#person-template', u);

	    // Bookmarklet
	    if (app.loginView.isCurrentUser(u) && !this.user().isEditor) {
		var link = '<a href="javascript:(function(){window.open(\'http://localhost:8888/vote2.html?href=\'+document.location.href)})()"> RNA Submit </a>';
		flashLog({type: 'notice', 
			  header: 'For Submitting Stories:',
			  content: 'Drag ' + link + ' to your bookmarks toolbar now.'});
	    }

	    // isFollowing checkBox
	    if (app.loginView.canFollow(u)) {
		html += rMake('#is-following-template', u);
	    }
	    if (this.user().isEditor) {
		// follows
		var follows = this.model.get('userInfo').follows;
		if (follows) {
		    html += rMake('#follows-template', {follows: follows});
		}
	    }
	    else {
		// followers
		var followers = this.model.get('userInfo').followers;
		if (followers) {
		    html += rMake('#followers-template', {followers: followers});
		}
	    }

	    $(this.el).prepend(html);
	    this.bindEvents(this);
	    return this;
	},
	
    });

    //* CreateAccountView

    window.CreateAccountView = Backbone.View.extend({

	tagName: "div",
	id: "createAccount",

	initialize: function(attrs) {
	    var self = this;
	    this.andThen = attrs.andThen;
	    this.render();
	},

	bindEvents: function(self) {
	    var self = this;
	    this.$('form input[type=submit]').click(function (event) {
		event.preventDefault();
		var nickname = self.$('form input[name=nickname]').attr('value');
		var webpage = self.$('form input[name=webpage]').attr('value');
		doRequest({ fun: 'welcomeUser', nickname: nickname}, 
			  function(data) {
			      if (data) {
				  app.loginView.model.set(data);
				  window.app.setLocation(self.andThen);
			      }
			      else {
				  flashError('failed to create account');
			      }
			  },
			  function (err) {
			      flashError(err.toString());
			  });	
	    });
	},

	render: function() {
	    $(this.el).append(rMake('#create-account-template'));
	    this.bindEvents(this);
	    return this;
	},
	
    });

    //* FullStoryView

    window.StoryFundingView = Backbone.View.extend({

	tagName:  "li",
	className: "story", // fixme

	events: {
	    "click a": 'person'
	}, 

	initialize: function() {
	    var self = this;
	    this.model.bind('change', function () { self.render() });
	    this.model.view = this;
	},

	person: function() {
	    app.hashPerson(this.model.get('user').id);
	},

	render: function() {
	    $(this.el).html(rMake('#story-funding-template', 
				  this.model.toJSON()));
	    return this;
	},	
    });


    window.StoryFundingsList = Backbone.Collection.extend({
	model: Backbone.Model,
	view: StoryFundingView,

	comparator: function(model) {
	    return - model.get('authority');
	}

    });

    window.FullStoryView = Backbone.View.extend({

	tagName: "div",
	id: "fullStory",

	initialize: function() {
	    var self = this;
	    this.model.bind('change', function () { self.render() });
	    this.model.view = this;	    
	    // fixme refactor
	    $(this.el).append(this.make('ul', {class: 'spine large'}));
	    // add StoryFundingsList
	    this.list = 
		new GenList({parent: this, 
			     list: new StoryFundingsList});
	    var self = this;
	    var dollars = function(storyInfo, fund) {
		if (storyInfo.score == 0) { return 0; }
		return (storyInfo.revenue / storyInfo.score) 
		    * fund.authority / 100;
	    };

	    var pennies = function(storyInfo, fund) {
		if (storyInfo.score == 0) { return 0; }
		return (storyInfo.revenue / storyInfo.score) 
		    * fund.authority % 100;
	    };

	    var pennyString = function(storyInfo, fund) {
		var a = ""+ pennies(storyInfo, fund);
		if (a.length == 1) { 
		    return "0" + a; 
		} else if (a.length == 2) {
		    return a; 
		} 
		else { 
		    return a.substring(0,2);
		}
	    };
	    var storyFunds = 
		_.map(this.model.get('funds'),
		      function (fund) {
			  var storyInfo =_.clone(self.model.get('info'));
			  return _.extend(fund, 
					  {dollars: dollars(storyInfo, fund),
					   pennies: pennyString(storyInfo, fund)});
		      });
	this.list.refresh(storyFunds);

	},

	render: function() {
	    var _copy = this.model.get('info');
	    _copy.revenue = '' +_copy.revenue / 100;
	    $(this.el).prepend(rMake('#full-story-template', 
				     // story info
				     _copy));
	}
    });


    //* VolumeView

    window.Edition = Backbone.Model.extend({});

    window.CollapsedEditionView = Backbone.View.extend({

	tagName:  "li",
	className: "collapsedEdition",

	events: {

	},

	initialize: function() {
	    var self = this;
	    this.model.bind('change', function () { self.render() });
	    this.model.view = this;
	},

	render: function() {
	    var _copy = this.model.toJSON();
	    if (this.model.get('number') == 0) {
		$(this.el).html(rMake('#collapsed-signup-round-template', _copy));
	    }
	    else {
		$(this.el).html(rMake('#collapsed-edition-template' , _copy));
	    }
	    return this;
	},
	
    });


    window.EditionList = Backbone.Collection.extend({

	model: Edition,
	view: CollapsedEditionView,

	comparator: function(edition) {
	    return - edition.get('number');
	}

    });

    window.VolumeView = Backbone.View.extend({

	events: {
	},

	tagName: 'div',
	id: 'volume',
	
	initialize: function(attrs) {
	    this.current = attrs.current;
	    // fixme refactor
	    $(this.el).append(this.make('div', {class: 'volumeHeader large'}));
	    $(this.el).append(this.make('ul', {class: 'spine large'}));
	    this.glist = 
		new GenList({parent: this, 
			     list: new EditionList});
	    this.total = attrs.data.length;
	    if (this.current) {
		// chop off unpublished editions
		attrs.data.splice(this.current.number, 
				  this.total - this.current.number + 1);
	    }
	    this.glist.refresh(attrs.data);
	},

	render: function() {
	    if (this.total == 0) {
		this.$('div').html(rMake('#volume-template',
					 {published: 0, remaining: 0}));
		return this;
	    }
	    var published = this.current? this.current.number - 1: this.total -1;
	    if (published < 0) {
		published = 0;
	    }
	    var remaining = this.total - 1 - published;

	    this.$('div').html(rMake('#volume-template',
				     {published: published, remaining: remaining}));
	    return this;
	},

    });

    //* login

    window.LoginView = Backbone.View.extend({
	el: $("#login"),

	model: new Backbone.Model({}),

	events: {
	    "click a.login": "loginOrViewSelf",
	    "click a.logout": "logout"
	},

	loggedIn: function() {
	    return this.model.get('email') != undefined;
	},

	isEditor: function () {
	    return this.model.get('isEditor');
	},

	canFollow: function(user) {
	    return this.isEditor() && !this.isCurrentUser(user);
	},


	isCurrentUser: function(user) {
	    return this.loggedIn() && this.model.get('email') == user.email;
	},

	isCreatingAccount: function() {
	    return this.model.get('isInitialized') == false;
	},

	checkCreatingAccount: function() {
	    if (this.isCreatingAccount() && !window.location.hash.match(/#createAccount/)) {
		flashLog({type: 'notice',
			  content: rMake('#please-finish-registering')});
	    }
	},

	initialize: function() {
	    var self = this.model.view = this;

	    this.model.bind('change', function () { 
		self.render();
	    });

	    self.stillWaiting = true; // this has to coordinate with createAccount
	    doRequest({ fun: 'sendUser' },
		      function(data) {
			  self.stillWaiting = false;
			  console.log('data ' + data);
			  if (data) {
			      self.model.set(data);
			  }
			  else {
			      self.model.set({cid: 'guest'});
			  }			      
		      });
	},

	logout: function() { 
	    redirectForLogout('sendLogoutURL'); 
	},
	
	loginOrViewSelf: function() { 
	    if (!this.loggedIn()) {
		redirectForLogin('sendLoginURL');
	    }
	    else {
		window.app.hashPerson(this.model.get('id'));
	    }
	},

	render: function() {
	    if (this.loggedIn()) {
		var nick = this.model.get('nickname');
		this.$('a.login').text(nick);
		this.$('a.logout').text('[Logout]')
		.css('padding-left', '5px');
	    }
	    else {
		this.$('a.login').text('[Login / Join]');
		this.$('a.logout').text('')
		.css('padding-left', '0');
	    }
	    this.checkCreatingAccount();
	}
    });



    //* app


    window.App = Backbone.Controller.extend({

	personLinkTemplate: _.template($('#person-link-template').html()),

	routes: {
	    // "most-recent": "mostRecent",
	    // "editions":"editions",
	    // "people":"people",
	    // "":"",
	    // "":"",
	    "network/:ed": "network",
	    "createAccount/:andThen": "createAccount",
	    "recentSocials/:ed": "recentSocials",
	    "topAuthorities/:ed": "topAuthorities",
	    "recentFundings/:ed": "recentFundings",
	    "topStories/:ed": "topStories",
	    "person/:pe": "person",
	    "volume": "volume",
	    "story/:ed/:id": "story",
	},

	clearMainView: function () {
	    if (this.mainView !== undefined) {
		log({info: 'removing main'});
		$(this.mainView.el).html('');
		this.mainView.remove();
	    }
	},

	setEditionView: function(viewType, attrs) {
	    this.clearMainView();
	    this.mainView = new viewType(attrs);
	    $('#main').append(this.mainView.el);
	},

	setMainView: function(type, data) {
	    this.clearMainView();
	    if (data) {
		this.mainView = new type({model: new Backbone.Model(data)});
	    }
	    else {
		this.mainView = type;
	    }
	    $('#main').append(this.mainView.el);
	},

	setVolumeView: function(attrs, data) {
	    this.clearMainView();
	    this.mainView = new VolumeView(attrs);
	    $('#main').append(this.mainView.el);
	},

	initialize: function() {
	    this.loginView = new LoginView;
	    Backbone.history.start();
	},

	currentEdition: -1,

	_edition: function(edNum, fun, order, view, getAttrs) {	    
	    var self = this;
	    var fetch = function(data) { 
		if (data && data.edition) {
		    // fixme main list doesn't immediately update 
		    // with welcome message after join
		    data.edition = window.Utils.processEdition(data.edition);
		    self.setEditionView(view,
					{order: order,
					 edition: data.edition,
					 numEditions: data.numEditions,
					 storiesSelected: view === StoriesView ?
 					 'selected' : 'unselected',
					 networkSelected: view === NetworkView ?
					 'selected' : 'unselected',
					 getAttrs: getAttrs,
					 data: data.list});
		}
	    };
	    doRequest({ fun: fun, 
			ed: edNum || this.currentEdition}, 
		      fetch);
	},

	topAuthorities: function(edNum) {
	    this._edition(edNum, 'topJudges', 'top', NetworkView,
			  function (userAuth) {
			      var a = _.clone(userAuth.user);
			      a.authority = userAuth.authority;
			      return a;
			  });
	},

	recentSocials: function(edNum) {
	    this._edition(edNum, 'recentSocials', 'recent', NetworkView);
	},

	topStories: function(edNum) {
	    this._edition(edNum, 'topStories', 'top', StoriesView);
	},

	recentFundings: function(edNum) {
	    this._edition(edNum, 'recentFundings', 'recent', StoriesView);
	},

	// default network view
	network: function(edNum) {
	    this.recentSocials(edNum);
	},

	// fixme this is broken at the end of a volume
	// default view for edition
	edition: function(edNum) {	    
	    return this.topStories(edNum);
	},

	story: function(edNum, linkId) {
	    var self = this;
	    doRequest({fun: 'story', 
		       ed: edNum, 
		       linkId: linkId}, 
		      function(data) {
			  self.setMainView(FullStoryView, data);
		      },
		      function (err) {
			  flashError(err.toString());
		      });
	},

	person: function(id) {
	    var self = this;
	    doRequest({ fun: 'relatedUser', id: id}, 
		      function(data) {
			  self.setMainView(PersonView, data);
		      },
		      function (err) {
			  flashError(err.toString());
		      });
	},

	createAccount: function(andThen) {
	    var andThen = decodeURIComponent(andThen);
	    var self = this;
	    if (this.loginView.stillWaiting) {
		window.requester.state = {
		    interrupted: true,

		    supercede: function(success, arg) {
			// clear Wait state
			console.log('success');
			success(arg);
			// recurse (no longer waiting)
			self.createAccount(andThen);
			return {interrupted: false};
		    }
		}
		return;
	    }
	    if (this.loginView.isCreatingAccount()) {
		console.log('creating');
		flashClear();
		this.setMainView(new CreateAccountView({andThen: andThen}));
	    }
	    else {
		console.log('not creating');
		window.location = andThen;
	    }
	},

	volume: function() {
	    var self = this;

	    doRequest({ fun: 'allEditions'}, 
		      function(data) {
			      self.setVolumeView({current: data.current, 
						  data: _.map(data.editions,
							  window.Utils.processEdition)});
		      },
		      function (err) {
			  flashError(err.toString());
		      });
	},

	setLocation: function(loc) {
	    window.location = loc;
	},

	setHash: function(hash) {
	    console.log('setHash: ' + hash);
	    window.location.hash = hash;
	},
	
	// fixme these don't do anything if you click them a second time
	hashPerson: function(id) { 
	    this.setHash('person/' + (id || ''));
	},

	hashStory: function(edition, story) { 
	    this.setHash( 'story/' + edition + '/' + story);
	},

	hashNetwork: function(ed) { 
	    this.setHash( 'network/' + (ed || '')); 
	},

	hashTopStories: function(ed) {
	    this.setHash( 'topStories/' + (ed || ''));
	},

	hashTopAuthorities: function(ed) {
	    this.setHash( 'topAuthorities/' + (ed || ''));
	},

	hashRecentSocials: function(ed) {
	    this.setHash( 'recentSocials/' + (ed || ''));
	},

	hashRecentFundings: function(ed) {
	    this.setHash('recentFundings/' + (ed || ''));
	},

	hashCreateAccount: function() {
	    this.setHash('createAccount/');
	},

	hashUpcoming: function() {}, // fixme

	hashRecent: function() {
	    this.setHash('volume');
	},
    });

    //* init
    window.initRNA();
    window.app = new App;
    defaultAction();

    $(window).bind('hashchange', function () { 
	defaultAction();
    });

    // fixme
    $('#upcoming').click(function (event) {
	app.hashTopStories();
    });

    $('#recent').click(function (event) {
	app.hashRecent();
    });

    $('a').live('click', function() {
	flashClear('');
    });

    $('#loadMessage').html('');
    log({info: 'loaded'});
});
