// Load the application once the DOM is ready, using `jQuery.ready`:
$(function(){

    var isEditor = function () {
	return app.loginView.user.get('isEditor');
    };

    // Story Model
    // ----------
    window.Story = Backbone.Model.extend({});

    // Story Collection
    // ---------------

    window.StoryList = Backbone.Collection.extend({

	// Reference to this collection's model.
	model: Story,

	comparator: function(story) {
	    return story.get('score');
	}

    });

    // Story View
    // --------------

    // The DOM element for a story...
    window.StoryView = Backbone.View.extend({

	//... is a list tag.
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

    window.EditionView = Backbone.View.extend({

	events: {
	},

	tagName: 'ul',
	className: 'edition',

	stories: new StoryList, // thinkme store in controller?

	initialize: function() {
	    _.bindAll(this, 'addOne', 'addAll', 'render');

	    this.stories.bind('add',     this.addOne);
	    this.stories.bind('refresh', this.addAll);
	    this.stories.bind('all', this.render);
	    $("#main").append(this.el);
	},

	addOne: function(story) {
	    var view = new StoryView({model: story});
	    $(this.el).append(view.render().el);
	},

	addAll: function() {
	    this.stories.each(this.addOne);
	},

	render: function() {
	    if (this.stories.length == 0) {
		$(this.el).append(this.make("li", {class: 'empty'}, 
					    "No stories have been submitted for this edition."));
		$(this.el).append(this.make("li", {class: 'empty'}, 
					    "You have 7 hours until the next edition."));
	    }
	}

    });

    window.PersonView = Backbone.View.extend({

	tagName: "div",
	id: "person",

	initialize: function() {
	    this.template =  _.template($('#person-template').html());
	    // assert(this.model !== undefined)
	    var self = this;
	    this.model.bind('change', function () { self.render() });
	    this.model.view = this;	    
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

    window.LoginView = Backbone.View.extend({
	el: $("#login"),

	user: new Backbone.Model({}),

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
		this.$('a.login').text('login');
		this.$('a.logout').text('');
	    }
	    else {
		this.$('a.login').text(nick);
		this.$('a.logout').text('(logout)');
	    }
	}
    });


    // The Application
    // ---------------

    window.App = Backbone.Controller.extend({
	routes: {
	    // "most-recent": "mostRecent",
	    // "editions":"editions",
	    // "people":"people",
	    // "":"",
	    // "":"",
	    "edition/:ed": "edition",
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
	    this.person_ = new Backbone.Model({});
	    this.loginView = new LoginView;
	    Backbone.history.start();
	},

	edition: function(ed) {	    
	    var self = this;

	    var refresh = function(data) { 
		self.setMainView(new EditionView);
		// get fresh list of models
		self.mainView.stories.refresh
		(_.map(data.stories,
		       function (s) { 
			   return new Backbone.Model(s) 
		       }))};
	    doRequest({ fun: 'edition', ed: ed || 0}, refresh);
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

	hashEdition: function(id) {
	    window.location.hash = 'edition/' +id;
	},
    });

    function defaultAction() {
	if (Backbone.history.getFragment() == '') {
	    window.app.edition(-1);
	}
    }

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
