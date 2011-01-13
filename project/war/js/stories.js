// -*- outline-regexp:  "[ \t]*//[*]+" -*-

// Load the application once the DOM is ready, using `jQuery.ready`:
$(function(){

    //* globals

    var isEditor = function () {
	return app.loginView.user.get('isEditor');
    };

    function defaultAction() {
	if (Backbone.history.getFragment() == '') {
	    window.app.edition();
	}
    }



    //* stories
    window.Story = Backbone.Model.extend({});

    window.StoryList = Backbone.Collection.extend({

	model: Story,

	comparator: function(story) {
	    return story.get('score');
	}

    });

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
	    $(this.el).html(this.template(this.model.toJSON()));
	    this.$('.score').text(this.model.get('score'));
	    this.$('a.story').text(this.model.get('link').title);
	    return this;
	},

	
    });

    
    window.StoriesView = Backbone.View.extend({

	events: {
	},

	tagName: 'ul',
	className: 'edition',

	list: new StoryList, // thinkme store in controller?

	initialize: function() {
	    _.bindAll(this, 'addOne', 'addAll', 'render');

	    this.list.bind('add',     this.addOne);
	    this.list.bind('refresh', this.addAll);
	    this.list.bind('all', this.render);
	    $("#main").html('');
	    $("#main").append(this.el);
	},

	addOne: function(story) {
	    var view = new StoryView({model: story});
	    $(this.el).append(view.render().el);
	},

	addAll: function() {
	    this.list.each(this.addOne);
	},

	refresh: function(data) {
	    this.list.refresh(_.map(data.stories,
				    function (s) { 
					return new Story(s) 
				    }));
	},

	render: function() {
	    if (this.list.length == 0) {
		$(this.el).append(this.make("li", {class: 'empty'}, 
					    "No stories have been submitted for this edition."));
		$(this.el).append(this.make("li", {class: 'empty'}, 
					    "You have 7 hours until the next edition."));
	    }
	}

    });

    //* socials
    window.Social = Backbone.Model.extend({
	isWelcome: function() {
	    return this.get('editor').id == 1;
	}
    });

    window.SocialList = Backbone.Collection.extend({

	model: Social,

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


    window.NetworkView = Backbone.View.extend({

	events: {
	},

	tagName: 'div',
	className: 'edition',

	list: new SocialList,

	initialize: function() {
	    // fixme refactor
	    _.bindAll(this, 'addOne', 'addAll', 'render');
	    this.list.bind('add',     this.addOne);
	    this.list.bind('refresh', this.addAll);
	    this.list.bind('all', this.render);
	    $("#main").html('');
	    $("#main").append(this.el);
	    $(this.el).append(this.make('ul', {'class': 'spine'}));
	},

	addOne: function(social) {
	    var view = new SocialView({model: social});
	    this.appendElt(view.render().el);
	},

	addAll: function() {
	    this.list.each(this.addOne);
	},

	appendElt: function(el) {
	    this.$('ul').append(el);	    
	},

	refresh: function(data) {
	    this.list.refresh (
		(_.map(data.socials,
		       function (s) { 
			   return new Social(s) 
		       })));
	},

	render: function() {
	    if (this.list.length == 0) {
		this.appendElt(this.make("li", {class: 'empty'}, 
					 "The network has not changed during this edition."));
		this.appendElt(this.make("li", {class: 'empty'}, 
					 "You have 7 hours until the next edition."));
	    }
	}

    });


    //* person
    window.PersonView = Backbone.View.extend({

	tagName: "div",
	id: "person",

	initialize: function() {
	    this.template =  _.template($('#person-template').html());
	    // assert(this.model !== undefined)
	    var self = this;
	    this.model.bind('change', function () { self.render() });
	    this.model.view = this;	    
	    $("#main").html(''); // refactor
	    $('#main').append(this.el);
	},

	user: function() {
	    return this.model.get('userInfo').user;
	},

	render: function() {
	    $(this.el).html(this.template(this.model.toJSON()));
	    this.$('a').text(this.user().email);
	    var fol = this.model.get('following');
	    if (isEditor()) {
		$(this.el).append(this.make('span', {}, 'following'));
		$(this.el).append(
		    this.make('input', 
			      {id: 'following', type: 'checkbox', 
			       checked: fol}));
		var self = this;
		$('#following').click(function (event) {
		    var fol = $(this).is(':checked');		    
		    doPostRequest({ fun: 'doSocial', 
				    to: self.user().id, on: fol},
				  function(data) {
				      flashLog({type: 'success',
						content: data || 'I got confused'});
				  });
		});
	    }

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



    //* The Application


    window.App = Backbone.Controller.extend({

	personLinkTemplate: _.template($('#person-link-template').html()),

	routes: {
	    // "most-recent": "mostRecent",
	    // "editions":"editions",
	    // "people":"people",
	    // "":"",
	    // "":"",
	    "edition/:ed": "edition",
	    "network/:ed": "network",
	    "person/:pe": "person"
	},

	setMainView: function(view) {
	    if (this.mainView !== undefined) {
		log({info: 'removing main ' + this.mainView.toString()});
		this.mainView.remove();
	    }
	    this.mainView = view;
	},

	initialize: function() {
	    this.person_ = new Backbone.Model({}); // fixme make a class
	    this.loginView = new LoginView;
	    Backbone.history.start();
	},

	currentEdition: -1,

	network: function(ed) {	    
	    var self = this;
	    var fetch = function(data) { 
		self.setMainView(new NetworkView);
		self.mainView.refresh(data);
	    }
	    doRequest({ fun: 'recentSocials', ed: ed || this.currentEdition}, fetch);
	},

	stories: function(ed) {	    
	    var self = this;

	    var fetch = function(data) { 
		self.setMainView(new StoriesView);
		self.mainView.refresh(data);
	    }
	    // thinkme trap all exceptions, period
	    doRequest({ fun: 'edition', ed: ed || this.currentEdition}, fetch);
	},

	edition: function(ed) {	    
	    return this.stories(ed); // fixme rename
	},

	person: function(id) {
	    var self = this;
	    doRequest({ fun: 'sendRelatedUser', id: id}, 
		      function(data) {
			  self.setMainView(new PersonView({model: self.person_}));
			  self.person_.set(data);
		      },
		      function (err) {
			  flashError(err.toString());
		      });
	},

	hashPerson: function(id) { 
	    window.location.hash = 'person/' + id; 
	},

	hashNetwork: function(id) { 
	    window.location.hash = 'network/' + id; 
	},

	hashEdition: function(id) {
	    window.location.hash = 'edition/' +id;
	},
    });

    //* init
    window.initRNA();
    window.app = new App;
    defaultAction();

    $(window).bind('hashchange', function () { 
	defaultAction();
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
