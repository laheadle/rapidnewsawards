// -*- outline-regexp:  "[ \t]*//[*]+" -*-

// Load the application once the DOM is ready, using `jQuery.ready`:
$(function(){

    //* globals

    var isEditor = function () {
	return app.loginView.user.get('isEditor');
    };

    function defaultAction() {
	flashInfo('');
	if (Backbone.history.getFragment() == '') {
	    window.app.topStories();
	}
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
	    var div = this.make("div", {class: "editionTabs spine large"});
	    $(div).html(rMake('#edition-tabs-template', 
			      {storiesSelected: this.storiesSelected,
			       networkSelected: this.networkSelected}));
	    $(this.el).prepend(div);
	    $(this.el).prepend(rMake('#edition-header-template', 
				     this.edition));
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
	    _copy.revenue = '' +_copy.revenue / 100
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
		this.appendElt(this.make("li", {class: 'empty'}, 
					 "No stories have been funded for this edition."));
		this.appendElt(this.make("li", {class: 'empty'}, 
					 "You have 7 hours until the next edition."));
	    }
	    return this;
	}
    });

    var GenList = function(attrs) {
	this.list = attrs.list;
	this.parent = attrs.parent;
	this.newModel = attrs.newModel;
	var self = this;
	this.list.bind('all', function () { self.parent.render() });
	this.list.bind('refresh', function () { self.addAll(); });

	this.refresh = function(list) {
	    var self = this;
	    this.list.refresh (
		(_.map(list,
		       function (s) { 
			   return new self.list.model(s) 
		       })));
	};

	this.addOne = function(model) {
	    var view = new this.list.view({model: model});
	    attrs.appendElt(view.render().el);
	};

	this.addAll = function() {
	    var self = this;
	    self.list.each(function (item) { self.addOne(self.newModel(item)) });
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
	className: "story", // fixme

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
	    // add fundings list
	    this.list = 
		new GenList({parent: this, 
			     list: new FundingsList,
			     newModel: function(item) {
				 // model is just a VoteLink, add the user
				 return new Backbone.Model(
				     _.extend(item.toJSON(),
					      {user: _.clone(self.user())}))
			     },
			     appendElt: function(el) {
				 self.$('ul').append(el);
			     }});
	    // fixme refactor
	    $(this.el).append(this.make('ul', {class: 'spine large'}));
	    this.list.refresh(this.model.get('userInfo').votes);
	},

	user: function() {
	    return this.model.get('userInfo').user;
	},

	bindEvents: function(self) {
	    this.$('#following').click(function (event) {
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
	    u.following = this.model.get('following');
	    if (isEditor()) {
		$(this.el).prepend(rMake('#person-template', u) +
				   rMake('#following-template', u))
		this.bindEvents(this);
	    }
	    else {
		$(this.el).prepend(rMake('#person-template', u));
	    }
	    return this;
	},
	
    });

    //* VolumeView

    window.Edition = Backbone.Model.extend({});

    window.CollapsedEditionView = Backbone.View.extend({

	tagName:  "li",
	className: "collapsedEdition",

	// Cache the template function for a single story.
	template: _.template($('#collapsed-edition-template').html()),

	events: {

	},

	initialize: function() {
	    var self = this;
	    this.model.bind('change', function () { self.render() });
	    this.model.view = this;
	},

	render: function() {
	    var _copy = this.model.toJSON();
	    $(this.el).html(this.template(_copy));
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
	className: 'volume',
	
	template: _.template($('#volume-template').html()),

	initialize: function(attrs) {
	    this.current = attrs.current;
	    this.list = new EditionList;
	    // fixme refactor
	    var self = this;
	    this.list.bind('add',     function () { self.addOne() });
	    this.list.bind('refresh', function () { self.addAll() });
	    $(this.el).append(this.make('div', {class: 'editionTabs spine large'}));
	    $(this.el).append(this.make('ul', {class: 'spine large'}));
	    this.list.bind('all', function () { self.render() });
	    this.refresh(attrs.data);
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

	refresh: function(list) {
	    var self = this;
	    this.list.refresh (
		(_.map(list,
		       function (s) { 
			   return new self.list.model(s) 
		       })));
	},

	render: function() {
	    var total = this.list.length - 1, 
		published = this.current? this.current.number : total
		remaining = total - published;

	    this.$('div').html(this.template({published: published, remaining: remaining}));
	    return this;
	},

    });

    //* login
    window.LoginView = Backbone.View.extend({
	el: $("#login"),

	user: new Backbone.Model({}), // fixme class AND rename

	events: {
	    "click a.login": "loginOrViewSelf",
	    "click a.logout": "logout"
	},

	initialize: function() {
	    var self = this;
	    this.user.bind('change', function () { self.render() });
	    this.user.view = this;

	    doRequest({ fun: 'sendUserInfo' },
		      function(data) {
			  if (data) {
			      self.user.set(data);
			      if (!data.isInitialized) {
				  popup('popupdiv')
			      }
			  }
			  else {
			      self.user.set({cid: 'guest'});
			  }			      
		      });
	},

	logout: function() { 
	    changeURL('sendLogoutURL'); 
	},

	loginOrViewSelf: function() { 
	    if (this.user.get('nickname') == undefined) {
		changeURL('sendLoginURL');
	    }
	    else {
		window.app.hashPerson(this.user.get('id'));
	    }
	},

	render: function() {
	    var nick = this.user.get('nickname');
	    if (nick === undefined) {
		this.$('a.login').text('[Login]');
		this.$('a.logout').text('')
		.css('padding-left', '0');
	    }
	    else {
		this.$('a.login').text(nick);
		this.$('a.logout').text('[Logout]')
		.css('padding-left', '5px');
	    }
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
	    "recentSocials/:ed": "recentSocials",
	    "topAuthorities/:ed": "topAuthorities",
	    "recentFundings/:ed": "recentFundings",
	    "topStories/:ed": "topStories",
	    "person/:pe": "person",
	    "volume": "volume",
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

	setPersonView: function(data) {
	    this.clearMainView();
	    this.mainView = new PersonView({model: new Backbone.Model(data)});
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
		if (data) {
		    // fixme main list doesn't immediately update 
		    // with welcome message after join
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

	person: function(id) {
	    var self = this;
	    doRequest({ fun: 'relatedUser', id: id}, 
		      function(data) {
			  self.setPersonView(data);
		      },
		      function (err) {
			  flashError(err.toString());
		      });
	},

	volume: function() {
	    var self = this;
	    doRequest({ fun: 'allEditions'}, 
		      function(data) {
			  self.setVolumeView({current: data.current, 
					      data: data.editions});
		      },
		      function (err) {
			  flashError(err.toString());
		      });
	},

	// fixme these don't do anything if you click them a second time
	hashPerson: function(id) { 
	    window.location.hash = 'person/' + (id || ''); 
	},

	hashNetwork: function(ed) { 
	    window.location.hash = 'network/' + (ed || ''); 
	},

	hashTopStories: function(ed) {
	    window.location.hash = 'topStories/' + (ed || '');
	},

	hashTopAuthorities: function(ed) {
	    window.location.hash = 'topAuthorities/' + (ed || '');
	},

	hashRecentSocials: function(ed) {
	    window.location.hash = 'recentSocials/' + (ed || '');
	},

	hashRecentFundings: function(ed) {
	    window.location.hash = 'recentFundings/' + (ed || '');
	},

	hashUpcoming: function() {}, // fixme

	hashRecent: function() {
	    window.location.hash = 'volume';
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
	flashInfo('');
    });

    $('#popupdiv form input[type=submit]').click(function (event) {
	event.preventDefault();
	popup('popupdiv');
	var nickname = $('#popupdiv form input[name=nickname]').attr('value');
	var webpage = $('#popupdiv form input[name=nickname]').attr('webpage');
	doRequest({ fun: 'welcomeUser', nickname: nickname}, 
		  function(data) {
		      if (data) {
			  app.loginView.user.set(data);
		      }
		      else {
			  flashError('no such user');
		      }
		  },
		  function (err) {
		      flashError(err.toString());
		  });	
    });

    log({info: 'loaded'});
});
