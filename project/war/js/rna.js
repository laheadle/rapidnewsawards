// -*- outline-regexp:  "[ \t]*//[*]+" -*-

// Load the application once the DOM is ready, using `jQuery.ready`:
$(function(){

    //* globals

    // see DAO.java
    var NEXT = -1;
    var AFTER_NEXT = -2;
    var FINAL = -3;
    var CURRENT = -4;
    var NEXT_OR_FINAL = -5;

    function defaultAction() {
	flashInfo('');
	if (Backbone.history.getFragment() === '') {
	    window.app.defaultAction();
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
	    this.isNext = attrs.isNext;
	    this.isCurrent = attrs.isCurrent;
	    this.list = attrs.list;
	    this.getAttrs = attrs.getAttrs;
	    this.itemView = attrs.itemView;
	    this.networkSelected = attrs.networkSelected;
	    this.storiesSelected = attrs.storiesSelected;
	    // fixme refactor
	    var self = this;
	    this.list.bind('add',     function () { self.addOne() });
	    this.list.bind('refresh', function () { self.addAll() });
	    $(this.el).append(this.make('div', {'class': 'list'}));
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
	    this.$('.list').append(el);
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
	    if ($('#editionTabsMinor .top').hasClass('selected')) {
		app.hashTopStories(this.edition.number);
	    }
	    else {
		app.hashRecentFundings(this.edition.number);
	    }
	},

	network: function() {
	    if ($('#editionTabsMinor .top').hasClass('selected')) {
		app.hashTopAuthorities(this.edition.number);
	    }
	    else {
		app.hashNetwork(this.edition.number);
	    }
	},

	render: function() {
	    // these go above the edition list, so they are prepended
	    // fixme change to a table
	    var div = this.make("div", {'class': "editionTabs enormous"});
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

	    if (this.isCurrent) {
		app.selectMenuItem('#current');
	    }
	    else if (this.isNext) {
		app.selectMenuItem('#next');
	    }

	    if (this.edition.number === 0) {
		if (this.edition.finished) {
		    $(rMake('#signup-finished-explanation'))
			.insertAfter($(this.el).children().first());
		}
		else {
		    $(rMake('#signup-explanation'))
			.insertAfter($(this.el).children().first());
		}
	    }
	    else {
		$(rMake('#funding-amounts-explanation', {finished: this.edition.finished}))
		    .insertAfter($(this.el).children().first());
	    }
	    return this;
	},

    });
    

    //* Stories - Top or Recent
    window.Story = Backbone.Model.extend({});

    window.StoryView = Backbone.View.extend({

	tagName:  "div",
	className: "listItem",

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
	    _copy.funding = '' +_copy.funding / 100;
	    _copy.href= '#story/' + _copy.edition.number + '/' + _copy.link.id;
	    $(this.el).html(this.template(_copy));
	    return this;
	},

	
    });

    window.StoriesList = Backbone.Collection.extend({

	model: Story,
	view: StoryView,

	comparator: function(story) {
	    return -story.get('score');
	}

    });

    window.Funding = Backbone.Model.extend({

    });

    window.FundingView = Backbone.View.extend({

	tagName:  "div",
	className: "listItem", // fixme

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
    });


    window.StoriesView = EditionView.extend({

	constructor: function (options) {
	    options.list = (options.order === 'top'? 
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
		{topSelected: this.order === 'top'? 'selected' : 'unselected',
		 recentSelected: this.order === 'recent'? 'selected' : 'unselected'};
	    this.$('#editionTabsMinor').html(rMake('#stories-order-tab-template', args));

	    if (this.order == 'top') {
		$(rMake('#list-header', {text: 'Top Stories',
					 subtext: 'Stories with the most funding for this edition appear on top.'}))
		    .insertAfter(this.$('.editionTabs'));
	    } else {
		$(rMake('#list-header', {text: 'Recent Story Awards',
					 subtext: 'Recent awards for this edition appear on top.'}))
		    .insertAfter(this.$('.editionTabs'));
	    }

	    if (this.list.length == 0) {
		if (this.edition.finished) {
 		    var message = this.edition.number > 0 ?
			"The judges did not fund any stories, " + 
			(this.order == 'top'? 'so there were no top stories.' :
			 'so there were no recently funded stories.') :
		    "No awards were given during the signup round, because the signup round was for socializing.";
		    this.appendElt(this.make("div", {'class': 'empty listItem'}, 
					     message));
		}
		else {
 		    var message = this.edition.number > 0 ?
			"The judges have not funded any stories yet, " + 
			(this.order == 'top'? 'so there are no top stories.' :
			 'so there are no recently funded stories.') :
		    "No stories will be funded during the signup round, because the signup round is for socializing.";
		    this.appendElt(this.make("div", {'class': 'empty listItem'}, 
					     message));
		}
	    }
	    return this;
	}
    });

    var GenList = function(attrs) {
	this.list = attrs.list;
	if (this.list.model == undefined) {
	    this.list.model = Backbone.Model;
	}
	this.parent = attrs.parent;
	this.newModel = attrs.newModel == undefined?
	    function (item) { return item; } : attrs.newModel;

	var self = this;
	this.list.bind('all', function () { self.parent.render() });
	this.list.bind('refresh', function () { self.addAll(); });

	this.appendElt = attrs.appendElt == undefined? 
	    function(el) {
		this.parent.$('.list').append(el);
	    }
	: attrs.appendElt;


	if (attrs.newModel == undefined) {
	    var self = this;
	    this.newModel = function(item) {
		    // fixme this should apply a getattrs to item, like editionView does
		    // the real fix is to make editionView use GenList...
		    return new self.list.model(item.toJSON());
		}
	}
	else {
	    this.newModel = attrs.newModel;
	}


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
	    var RNA_EDITOR_EMAIL = "__rnaeditor@gmail.com";
	    return this.get('editor').email == RNA_EDITOR_EMAIL;
	}
    });

    window.SocialView = Backbone.View.extend({

	tagName:  "div",
	className: "listItem", 

	followTemplate: _.template($('#social-template').html()),
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

	tagName:  "div",
	className: "listItem", // fixme

	initialize: function() {
	    var self = this;
	    this.model.bind('change', function () { self.render() });
	    this.model.view = this;
	},

	render: function() {
	    $(this.el).html(rMake('#top-authority-template', this.model.toJSON()));
	    return this;
	},	
    });

    window.AuthoritiesList = Backbone.Collection.extend({
	model: Authority,
	view: AuthorityView,

	comparator: function(model) {
	    return -model.get('funded');
	}

    });


    //* window.TopEditor = Backbone.Model.extend({
    window.TopEditorView = Backbone.View.extend({

	tagName:  "div",
	className: "listItem", // fixme

	initialize: function() {
	    var self = this;
	    this.model.bind('change', function () { self.render() });
	    this.model.view = this;
	},

	render: function() {
	    $(this.el).html(rMake('#top-editor-template', this.model.toJSON()));
	    return this;
	},	
    });


    window.TopEditorsList = Backbone.Collection.extend({
	view: TopEditorView,

	comparator: function(model) {
	    return -model.get('funded');
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
	    this.$("a.editor").click(function(event) {
		self.topEditors();
	    });
	},

	constructor: function (options) {
	    options.list = (options.order == 'top'? 
			    (options.influence == 'judge' ? new AuthoritiesList 
			     : new TopEditorsList)
			    : new SocialList);
	    this.order = options.order;
	    this.influence = options.influence;
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

	topEditors: function() {
	    window.app.hashTopEditors(this.edition.number);
	},

	recentSocials: function() {
	    window.app.hashRecentSocials(this.edition.number);
	},

	render: function() {
	    this.constructor.__super__.render();
	    var args = 
		{topSelected: this.order == 'top'? 'selected' : 'unselected',
		 recentSelected: this.order == 'recent'? 'selected' : 'unselected'};

	    // List Header
	    if (this.order == 'top') {
		if (this.influence == 'judge') {
		    $(rMake('#list-header', {text: 'Top Judges',
					     subtext: 'The judges awarding the most money are on top.'}))
			.insertAfter(this.$('.editionTabs'));
		}
		else {
		    $(rMake('#list-header', {text: 'Top Editors',
					     subtext: 'The editors enabling the most award money are on top.'}))
			.insertAfter(this.$('.editionTabs'));
		}
	    }
	    else {
		$(rMake('#list-header', {text: 'Recent Network Activity',
					 subtext: 'Recent follows and unfollows appear on top.',
					 help: 'follow-unfollow'}))
		    .insertAfter(this.$('.editionTabs'));
	    }

	    this.$('#editionTabsMinor').html(rMake('#network-order-tab-template', args));
	    if (this.order == 'top') {
		var iargs = 
		    {judgeSelected: this.influence == 'judge'? 'selected' : 'unselected',
		     editorSelected: this.influence == 'editor'? 'selected' : 'unselected'};
		this.$('#influenceTab').html(rMake('#influence-tab-template', iargs));
	    }
	    if (this.list.length == 0) {
		if (this.order == 'top') {
		    this.appendElt(this.make("div", {'class': 'empty listItem'}, 
					     this.edition.finished? 
					     "No funding occurred during this edition."
					     : "No funding has occurred during this edition."));
		}
		else {
		    this.appendElt(this.make("div", {'class': 'empty listItem'}, 
					     this.edition.finished? 
					     "The network did not change during this edition."
					     : "The network has not changed during this edition."));
		}
	    }
	    return this;
	}

    });

    //* EditorFundings
    window.EditorFundingsView = Backbone.View.extend({
	tagName: "div",
	id: "editorFundingsBody",

	initialize: function() {
	    var self = this;
	    this.model.bind('change', function () { self.render() });
	    this.model.view = this;

	    $(this.el).append(this.make('div', {'class': 'list'}));

	    this.list = new GenList({
		parent: this,
		list: new FundingsList,
	    });
	    this.list.refresh(this.model.get('list'));
	},

	render: function() {
	    $(this.el).prepend(rMake('#editor-fundings-header-template', 
				     {editor: this.model.get('editor'),
				      edition: this.model.get('edition')}));
	}
    });

    //* Person
    window.PersonView = Backbone.View.extend({

	tagName: "div",
	id: "personBody",

	initialize: function() {
	    var self = this;
	    this.model.bind('change', function () { self.render() });
	    this.model.view = this;
	    // fixme refactor
	    $(this.el).append(this.make('div', {'class': 'list'}));
	    // add fundings list

	    if (this.user().isEditor) {
		this.list = new GenList({parent: this, 
					 list: new SocialList});
		this.list.refresh(this.model.get('userInfo').socials);
	    }
	    else {
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
				     this.parent.$('.list').append(el);
				 }});
		this.list.refresh(this.model.get('userInfo').votes);
	    }
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
				  // TODO Don't show 'success' for e.g. already about to unfollow
				  flashLog({type: 'success',
					    content: data || 'I got confused'});
			      });
	    });
	},

	render: function() {
	    var u = _.clone(this.user());
	    u.isFollowing = this.model.get('isFollowing');
	    var html = rMake('#person-template', u);

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

	    html += rMake('#person-recent-header');

	    $(this.el).prepend(html);
	    this.bindEvents(this);
	    return this;
	},
	
    });

    //* Nominate / Submit
    window.NominateView = Backbone.View.extend({

	tagName: "div",
	id: "nominateBody",

	initialize: function() {
	    var self = this;
	    this.model.bind('change', function () { self.render() });
	    this.model.view = this;
	    self.render();
	},

	submit: function (event) {
	    event.preventDefault();
	    var link = self.$('input[name=url]').attr('value');
	    doRequest({fun: 'voteFor',
		       link: link,
		       edition: NEXT,
		       fullLink: '',
		       on: true},
		      function (data) {
			  if (data.submit) {
			      // show full submit form
			      app.setMainView(FullSubmitView,
					      {title: data.suggestedTitle,
					       link: link});
			  }
			  else if (data.returnVal == 'SUCCESS') {
			      window.flashLog({type: 'success',
					       content: 'Your support is being counted.'});
			  }
			  else {
			      window.flashLog({type: 'error',
					       content: data.returnval});
			  }
		      });
	},

	bindEvents: function(self) {
	    this.$('input[type=submit]').click(this.submit);
	    this.$('form').submit(this.submit);
	},

	render: function() {
	    if (window.app.loginView.isLoggedInJudge()) {
		if (window.app.loginView.isCreatingAccount()) {
		    $(this.el).html(
			rMake('#header-template', 
			      {text: 'You must finish creating your account before nominating anything'}));
		}
		else if (this.model.get('isSignup')) {
		    $(this.el).html(
			rMake('#header-template', 
			      {text: 'Nominations will begin after the signup round is published.'}));
		}
		else {
		    $(this.el).html(rMake('#nominate-story-template', 
					  this.model.toJSON()));
		    this.bindEvents(this);
		}
		$(this.el).append(rMake('#bookmarklet-template'));
	    }
	    else {
		$(this.el).html(rMake('#header-template', {text: 'Only judges can nominate works for funding'}));
	    }

	    return this;
	},
	
    });

    window.FullSubmitView = Backbone.View.extend({

	tagName: "div",
	id: "fullSubmitBody",

	initialize: function() {
	    var self = this;
	    this.model.bind('change', function () { self.render() });
	    this.model.view = this;
	    this.render();
	},

	submit: function (event) {
	    event.preventDefault();
	    var link = self.$('input[name=url]').attr('value');
	    doRequest({fun: 'submitStory',
		       url: self.$('input[name=url]').attr('value'),
		       title: self.$('input[name=title]').attr('value') },
		      function (data) {
			  window.flashLog({type: 'success',
					   content: 'Your support is being counted.'});
			  app.clearMainView();
		      });
	},

	bindEvents: function(self) {
	    this.$('form').submit(this.submit);
	    this.$('input[type=submit]').click(this.submit);
	},

	render: function() {
	    $(this.el).html(rMake('#full-submit-template', 
				  this.model.toJSON()));
	    this.bindEvents(this);
	    return this;
	},
	
    });

    //* CreateAccountView

    window.CreateAccountView = Backbone.View.extend({

	tagName: "div",
	id: "createAccountBody",

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
		var webPage = self.$('form input[name=webPage]').attr('value');
		var consent = self.$('form input[name=consent]').attr('checked');
		doRequest({ fun: 'welcomeUser', 
			    nickname: nickname, 
			    webPage: webPage,
			    consent: consent }, 
			  // 1) user data comes back in request.requester and then
			  // 2) loginView.model is set by doRequest handler.
			  function(data) {
			      // Make sure the user info has been saved
			      setTimeout(function() {
				  window.app.setLocation(self.andThen);
			      }, 200); 
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

	tagName:  "div",
	className: "listItem", // fixme

	initialize: function() {
	    var self = this;
	    this.model.bind('change', function () { self.render() });
	    this.model.view = this;
	},

	render: function() {
	    $(this.el).html(rMake('#story-funding-template', 
				  this.model.toJSON()));
	    return this;
	},	
    });


    window.StoryFundingsList = Backbone.Collection.extend({
	view: StoryFundingView,

	comparator: function(model) {
	    return model.get('timeLong');
	}

    });

    window.FullStoryView = Backbone.View.extend({

	tagName: "div",
	id: "fullStoryBody",

	initialize: function() {
	    var self = this;
	    this.model.bind('change', function () { self.render() });
	    this.model.view = this;	    
	    // fixme refactor
	    $(this.el).append(this.make('div', {'class': 'list'}));
	    // add StoryFundingsList
	    this.list = 
		new GenList({parent: this, 
			     list: new StoryFundingsList});
	    this.list.refresh(this.model.get('funds'));

	},

	render: function() {
	    var _copy = this.model.get('info');
	    $(this.el).prepend(rMake('#full-story-template', 
				     // story info
				     _copy));
	    
	    this.bindEvents(this);
	    $('.fadeIn').hide().fadeIn(2000);
	},

	bindEvents: function(self) {
	    this.$('#is-funding').click(function (event) {
		var on = $(this).is(':checked');
		doPostRequest({fun: 'voteFor', 
			       link: self.model.get('info').link.url,
			       edition: self.model.get('info').editionId,
			       fullLink: "",
			       on: on},
			      function(data) {
				  if (data.returnVal === 'SUCCESS') {
				      flashLog({type: 'success',
						content: on? 'Your vote was counted' :
					       'Your vote was cancelled'});
				  }
				  else {
				      flashLog({type: 'error',
						content: (on? 'Failed to count your vote: ' :
							  'Failed to cancel your vote: ') + 
						(data.returnVal || 'try again later')});
				  }
			      });
	    });
	}

    });


    //* VolumeView

    window.CollapsedEditionView = Backbone.View.extend({

	tagName:  "div",
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
	view: CollapsedEditionView,

	comparator: function(edition) {
	    return - parseInt(edition.get('number'));
	}

    });

    window.VolumeView = Backbone.View.extend({

	events: {
	},

	tagName: 'div',
	id: 'volumeBody',
	
	initialize: function(attrs) {
	    this.current = attrs.current;
	    // fixme refactor
	    $(this.el).append(this.make('div', {'class': 'volumeHeader'}));
	    $(this.el).append(this.make('div', {'class': 'list'}));
	    this.glist = 
		new GenList({parent: this, 
			     list: new EditionList});
	    this.total = attrs.data.length;
	    if(attrs.data) {
		attrs.data.sort(
		    function(left, right) {
			return parseInt(left.number) < parseInt(right.number) ? -1 :
			    parseInt(right.number) < parseInt(left.number) ? 1 : 0;
		    });
	    }
	    if (this.current) {
		// chop off unpublished editions
		attrs.data.splice(this.current.number, 
				  this.total - this.current.number + 1);
	    }
	    if(attrs.data) {
		attrs.data.sort(
		    function(left, right) {
			return parseInt(left.number) < parseInt(right.number) ? 1 :
			    parseInt(right.number) < parseInt(left.number) ? -1 : 0;
		    });
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

	    this.$('.volumeHeader').html(rMake('#volume-template',
				     {published: published, remaining: remaining}));
	    app.selectMenuItem('#recent');
	    return this;
	},

    });

    //* donors


    window.DonorsView = Backbone.View.extend({

	listClass: Backbone.Collection.extend({
	    view: Backbone.View.extend({
		className: "listItem", 

		render: function() {
		    $(this.el).html(rMake('#donation-template', this.model.toJSON()));
		    return this;
		}
	    })
	}),

	initialize: function() {
	    var self = this;
	    this.model.bind('change', function () { self.render() });
	    this.model.view = this;
	    // fixme refactor
	    $(this.el).append(this.make('div', {'class': 'list'}));
	    // add fundings list
	    this.list = new GenList({parent: this, 
				     list: new this.listClass});
	    this.list.refresh(this.model.get('list'));
	},

	render: function() {
	    $(this.el).prepend(rMake('#donations-template', this.model.toJSON()));
	    return this;
	}
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
	    return this.isEditor() && !this.isCurrentUser(user) && !user.isEditor;
	},


	isCurrentUser: function(user) {
	    return this.loggedIn() && this.model.get('email') == user.email;
	},

	isLoggedInJudge: function() {
	    return this.loggedIn() && !this.isEditor();
	},

	isCreatingAccount: function() {
	    return this.model.get('isInitialized') == false;
	},

	checkCreatingAccount: function() {
	    if (this.isCreatingAccount() && !window.location.hash.match(/#createAccount/)) {
		flashLog({type: 'redNotice',
			  content: rMake('#please-finish-registering', 
					 {andThen: encodeURIComponent(window.location)})});
	    }
	},

	initialize: function() {
	    var self = this.model.view = this;
	    this.model.bind('change', function () { 
		self.render();
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
		this.$('a.logout').text('[Sign out]')
		.css('padding-left', '5px');
	    }
	    else {
		this.$('a.login').text('[Sign In / Join]');
		this.$('a.logout').text('')
		.css('padding-left', '0');
	    }
	    if (this.isLoggedInJudge()) {
		$('.judgeOnly').css('display', 'block');
	    }
	    this.checkCreatingAccount();
	}
    });



    //* app


    window.App = Backbone.Controller.extend({

	personLinkTemplate: _.template($('#person-link-template').html()),

	routes: {
	    "briefExplanation": "briefExplanation",
	    "donors": "donors",
	    "defaultNext": "defaultNext",
	    "defaultCurrent": "defaultCurrent",
	    "editorFundings/:edition/:editor": "editorFundings",
	    "network/:ed": "network",
	    "nominate": "nominate",
	    "nominate/:url": "nominate",
	    "createAccount/:andThen": "createAccount",
	    "recentSocials/:ed": "recentSocials",
	    "topAuthorities/:ed": "topAuthorities",
	    "topEditors/:ed": "topEditors",
	    "recentFundings/:ed": "recentFundings",
	    "topStories/:ed": "topStories",
	    "person/:pe": "person",
	    "volume": "volume",
	    "story/:ed/:id": "story",
	},

	clearMainView: function () {
	    this.clearAllMenuSelections();
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

	selectMenuItem: function(item) {
	    $(item).removeClass('unselected').addClass('selected');
	},

	clearAllMenuSelections: function() {
	    $('#recent').removeClass('selected').addClass('unselected');
	    $('#next').removeClass('selected').addClass('unselected');
	    $('#current').removeClass('selected').addClass('unselected');
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
	},

	currentOrFinalEdition: -5,

	_edition: function(edNum, fun, order, view, getAttrs, inits) {	    
	    var self = this;
	    var fetch = function(data) { 
		if (data && data.edition) {
		    // fixme main list doesn't immediately update 
		    // with welcome message after join
		    var initParams = 
			{order: order,
			 edition: data.edition,
			 numEditions: data.numEditions,
			 isCurrent: data.isCurrent,
			 isNext: data.isNext,
			 storiesSelected: view === StoriesView ?
 			 'selected' : 'unselected',
			 networkSelected: view === NetworkView ?
			 'selected' : 'unselected',
			 getAttrs: getAttrs,
			 data: data.list};
		    for (var i in inits) {
			initParams[i] = inits[i];
		    }
		    self.setEditionView(view, initParams);
		}
	    };
	    doRequest({ fun: fun, 
			edition: edNum || this.currentOrFinalEdition}, 
		      fetch);
	},

	topAuthorities: function(edNum) {
	    this._edition(edNum, 'topJudges', 'top', NetworkView,
			  this.influenceTransform,
			  {influence: 'judge'});
	},

	topEditors: function(edNum) {
	    this._edition(edNum, 'topEditors', 'top', NetworkView,
			  this.influenceTransform,
			  {influence: 'editor'});
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

	// fixme this is broken at the end of a volume -- Is it still??
	// default view for edition
	edition: function(edNum) {
	    return this.topStories(edNum);
	},

	_defaultAction: function(fun) {
	    var self = this;
	    var fetch = function(data) { 
		if (data && data.edition) {
		    var view = data.isStoryList? StoriesView : NetworkView;
		    var initParams =
			{order: data.order,
			 edition: data.edition,
			 numEditions: data.numEditions,
			 isCurrent: data.isCurrent,
			 isNext: data.isNext,
			 storiesSelected: (data.isStoryList? 'selected' : 'unselected'),
			 networkSelected: (data.isNetworkList? 'selected' : 'unselected'),
			 data: data.list,
			 influence: data.influence};
		    self.setEditionView(view, initParams);
		}
	    }
	    doRequest({fun: fun}, fetch);
	},

	defaultAction: function() {
	    this._defaultAction('defaultAction');
	},

	defaultNext: function() {
	    this._defaultAction('defaultNext');
	},

	defaultCurrent: function() {
	    this._defaultAction('defaultCurrent');
	},

	story: function(edNum, linkId) {
	    var self = this;
	    doRequest({fun: 'story', 
		       edition: edNum, 
		       linkId: linkId}, 
		      function(data) {
			  self.setMainView(FullStoryView, data);
		      });
	},

	person: function(id) {
	    var self = this;
	    doRequest({ fun: 'relatedUser', id: id}, 
		      function(data) {
			  self.setMainView(PersonView, data);
		      });
	},

	editorFundings: function(edition, editor) {
	    var self = this;
	    doRequest({ fun: 'editorFundings', edition: edition, editor: editor},
		      function(data) {
			  self.setMainView(EditorFundingsView, data);
		      });
	},

	briefExplanation: function() {
	    var clas = Backbone.View.extend({
		initialize: function() {
		    this.render();
		},

		render: function() {
		    $(this.el).html(rMake('#brief-explanation-template'));
		    return this;
		}
	    });
	    this.setMainView(new clas);
	    doRequest({ fun: 'ping'}, function() {}); // initialize
	},

	nominate: function(url) {
	    var self = this;
	    doRequest({ fun: 'ping'},
		      function(data) {
			  self.setMainView(NominateView, {
			      isSignup: data.isSignup,
			      url: url? decodeURIComponent(url) : ''});
		      });
	},

	createAccount: function(andThen) {
	    var andThen = decodeURIComponent(andThen);
	    var self = this;
	    doRequest({ fun: 'sendUser'},
		      function(data) {
			  if (self.loginView.isCreatingAccount()) {
			      rnaTrace('creating');
			      flashClear();
			      self.setMainView(new CreateAccountView({andThen: andThen}));
			  }
			  else {
			      rnaTrace('not creating');
			      window.location = andThen;
			  }
		      });
	},

	volume: function() {
	    var self = this;

	    doRequest({ fun: 'allEditions'}, 
		      function(data) {
			      self.setVolumeView({current: data.current, 
						  data: data.editions});
		      });
	},

	donors: function() {
	    var self = this;
	    doRequest({ fun: 'getDonations'}, 
		      function(data) {
			  self.setMainView(DonorsView, data);
		      });
	},

	setLocation: function(loc) {
	    window.location = loc;
	},

	setHash: function(hash) {
	    rnaTrace('setHash: ' + hash);
	    var oldFragment = Backbone.history.getFragment();
	    window.location.hash = hash;
	    if (oldFragment == Backbone.history.getFragment()) {
		Backbone.history.loadUrl();
	    }
	    $(window).scrollTop(0);
	},
	
	hashPerson: function(id) { 
	    this.setHash('person/' + (id || ''));
	},

	hashStory: function(edition, story) { 
	    this.setHash( 'story/' + edition + '/' + story);
	},

	hashNetwork: function(ed) { 
	    this.setHash( 'network/' + (ed === 0 ? ed : (ed || ''))); 
	},

	hashNominate: function(url) {
	    if (url) {
		this.setHash('nominate/'+url);
	    }
	    else {
		this.setHash('nominate');
	    }
	},

	hashTopStories: function(ed) {
	    this.setHash( 'topStories/' + (ed === 0 ? ed : (ed || '')));
	},

	hashTopAuthorities: function(ed) {
	    this.setHash( 'topAuthorities/' + (ed === 0 ? ed : (ed || '')));
	},

	hashTopEditors: function(ed) {
	    this.setHash( 'topEditors/' + (ed === 0 ? ed : (ed || '')));
	},

	hashRecentSocials: function(ed) {
	    this.setHash( 'recentSocials/' + (ed === 0 ? ed : (ed || '')));
	},

	hashRecentFundings: function(ed) {
	    this.setHash('recentFundings/' + (ed === 0 ? ed : (ed || '')));
	},

	hashCreateAccount: function() {
	    this.setHash('createAccount/');
	},

	hashDefaultNext: function() {
	    this.setHash('defaultNext');
	},

	hashDefaultCurrent: function() {
	    this.setHash('defaultCurrent');
	},

	hashRecent: function() {
	    this.setHash('volume');
	},


	influenceTransform: function(influence) {
	    var a = _.clone(influence.user);
	    a.user = _.clone(influence.user); // for passing to link renderer
	    a.funded = influence.funded;
	    a.fundedStr = influence.fundedStr;
	    // <this> is the NetworkView
	    a.edition = this.edition;
	    return a;
	},

    });

    //* init
    window.initRNA();
    window.app = new App;
    Backbone.history.start();
    defaultAction();

    $(window).bind('hashchange', function () { 
	defaultAction();
    });

    // fixme
    $('#next').click(function (event) {
	app.hashDefaultNext();
    });

    $('#current').click(function (event) {
	app.hashDefaultCurrent();
    });

    $('#briefExplanationLink').click(function (event) {
	app.setHash('briefExplanation');
    });

    $('#recent').click(function (event) {
	app.hashRecent();
    });

    $('a, input').live('click', function() {
	flashClear();
	app.clearAllMenuSelections();
    });

    $('a.nominate').live('click', function() {
	app.hashNominate();
    });


    log({info: 'loaded'});
});
