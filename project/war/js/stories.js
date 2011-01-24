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
	    window.app.edition();
	}
    }



    //* EditionView

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

	refresh: function(list) {
	    var self = this;
	    this.list.refresh (
		(_.map(list,
		       function (s) { 
			   return new self.list.model(self.getAttrs? self.getAttrs(s) : s); 
		       })));
	},

	stories: function() {
	    app.hashStories(this.edition.number);
	},

	network: function() {
	    app.hashNetwork(this.edition.number);
	},

	render: function() {
	    this.tabsTemplate =  _.template($('#edition-header-template').html());
	    var div = this.make("div", {class: "editionTabs spine large"});
	    $(this.el).prepend(div);
	    var e = _.clone(this.edition);
	    e.selected = 'network';
	    $(div).html(this.tabsTemplate(e));
	    return this;
	},

    });


    //* stories
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

    window.StoryList = Backbone.Collection.extend({

	model: Story,
	view: StoryView,

	comparator: function(story) {
	    return story.get('score');
	}

    });

    window.StoriesView = EditionView.extend({

	constructor: function (options) {
	    options.list = new StoryList;
	    // run super.initialize
	    Backbone.View.apply(this.constructor.__super__, [options]);
	    // bind this.render
	    var self = this;
	    this.list.bind('all', function () { self.render() });
	    this.refresh(options.data);
	},

	render: function() {
	    this.constructor.__super__.render();
	    if (this.list.length == 0) {
		this.$('.orderBy').html('');
		this.appendElt(this.make("li", {class: 'empty'}, 
					 "No stories have been funded for this edition."));
		this.appendElt(this.make("li", {class: 'empty'}, 
					 "You have 7 hours until the next edition."));
	    }
	    else {
		this.$('.orderBy').html('top funded | recent');
	    }
	    return this;
	}
    });

    //* Social Network

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
	    app.hashPerson(this.model.id);
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

	orderTabTemplate: _.template($("#network-order-tab-template").html()),

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
	    this.$('.orderBy').html(this.orderTabTemplate(args));

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


    //* person
    window.PersonView = Backbone.View.extend({

	tagName: "div",
	id: "person",
	className: "person spine",

	initialize: function() {
	    this.template =  _.template($('#person-template').html());
	    this.following_template = 
		_.template($('#following-template').html());
	    // assert(this.model !== undefined)
	    this.model._assert = true
	    var self = this;
	    this.model.bind('change', function () { self.render() });
	    this.model.view = this;	    
	},

	user: function() {
	    return this.model.get('userInfo').user;
	},

	render: function() {
	    var u = _.clone(this.user());
	    u.following = this.model.get('following');
	    if (isEditor()) {
		$(this.el).html(this.template(u) + this.following_template(u));
		var self = this;
		this.$('#following').click(function (event) {
		    var fol = $(this).is(':checked');		    
		    doPostRequest({fun: 'doSocial', 
				   to: self.user().id, on: fol},
				  function(data) {
				      flashLog({type: 'success',
						content: data || 'I got confused'});
				  });
		});
	    }
	    else {
		$(this.el).html(this.template(u));
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
	    "edition/:ed": "edition", //fixme rename
	    "network/:ed": "network",
	    "recentSocials/:ed": "recentSocials",
	    "topAuthorities/:ed": "topAuthorities",
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

	setPersonView: function(attrs, data) {
	    this.clearMainView();
	    this.mainView = new PersonView(attrs);
	    this.person_.set(data); // fixme put in view
	    $('#main').append(this.mainView.el);
	},

	setVolumeView: function(attrs, data) {
	    this.clearMainView();
	    this.mainView = new VolumeView(attrs);
	    $('#main').append(this.mainView.el);
	},

	initialize: function() {
	    this.person_ = new Backbone.Model({}); // fixme make a class
	    this.loginView = new LoginView;
	    Backbone.history.start();
	},

	currentEdition: -1,

	_network: function(ed, fun, order, getAttrs) {	    
	    var self = this;
	    var fetch = function(data) { 
		if (data) {
		    // fixme main list doesn't immediately update 
		    // with welcome message after join
		    self.setEditionView(NetworkView,
					{order: order,
					 edition: data.edition,
					 numEditions: data.numEditions,
					 getAttrs: getAttrs,
					 data: data.list});
		}
	    };
	    doRequest({ fun: fun, 
			ed: ed || this.currentEdition}, 
		      fetch);
	},

	topAuthorities: function(ed) {
	    this._network(ed, 'topJudges', 'top',
			  function (userAuth) {
			      var a = _.clone(userAuth.user);
			      a.authority = userAuth.authority;
			      return a;
			  });
	},

	recentSocials: function(ed) {
	    this._network(ed, 'recentSocials', 'recent');
	},

	// default network view
	network: function(ed) {
	    this._network(ed, 'recentSocials', 'recent');
	},

	// fixme this is broken at the end of a volume
	stories: function(ed) {	    
	    var self = this;

	    var fetch = function(data) { 
		if (data) {
		    self.setEditionView(StoriesView,
					// fixme this can be null
					{edition: data.edition,
					 numEditions: data.numEditions,
					 data: data.stories});
		}
	    };
	    // thinkme trap all exceptions, period
	    doRequest({ fun: 'topStories', ed: ed || this.currentEdition}, fetch);
	},

	edition: function(ed) {	    
	    return this.stories(ed); // fixme rename
	},

	person: function(id) {
	    var self = this;
	    doRequest({ fun: 'relatedUser', id: id}, 
		      function(data) {
			  self.setPersonView({model: self.person_}, data);
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

	hashStories: function(ed) {
	    window.location.hash = 'edition/' + (ed || '');
	},

	hashTopAuthorities: function(ed) {
	    window.location.hash = 'topAuthorities/' + (ed || '');
	},

	hashRecentSocials: function(ed) {
	    window.location.hash = 'recentSocials/' + (ed || '');
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
	app.hashStories();
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
