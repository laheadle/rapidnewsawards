// Load the application once the DOM is ready, using `jQuery.ready`:
$(function(){

    window.SubmissionView = Backbone.View.extend({

	el: $('#main'),

	formContent: _.template($('#submission-template').html()),

	events: {},

	initialize: function() {
	    var self = this;
	    this.model = new Backbone.Model;
	    this.model.bind('change', function () { self.render() });
	    this.model.view = this;
	    this.model.set({title: 'title', url: 'url'});
	},

	tryVote: function() {
	    var self = this;
	    var link = window.location.search.replace(/\?href=/, ''); // gross!

	    var setLink = function (data) {
		if (data) {
		    self.model.set({done: true,
				    result: data.returnVal});
		}
		else {
		    self.model.unset('submitting', {silent: true});
		    self.model.set({link: link, title: ''});
		}
	    };

	    var setUser = function(data) {
		if (data) {
		    self.model.set({user: data, submitting: true});
		    if (self.isLoggedInandInitialized()) {
			doRequest({fun: 'voteFor', 
				   link: link,
				   edition: -1,
				   fullLink: window.location.toString(), 
				   on: true},
				  setLink);
		    }
		}
		else {
		    self.model.set({user: {cid: 'guest'}});
		    window.redirectForLogin('sendLoginURL');
		}
	    };

	    doRequest({ fun: 'sendUser' },
		     setUser);
	},	

	show: function (txt) {
	    $(this.el).append(this.make('span', {'class': 'showText'}, txt));
	},
	
	clear: function () {
	    // clear it
	    $(this.el).html('');
	},

	isLoggedInandInitialized: function() {
	    var user = this.model.get('user');
	    return user.nickname && user.isInitialized;
	},

	render: function() {
	    var user = this.model.get('user');
	    var done = this.model.get('done');
	    var submitting = this.model.get('submitting');
	    
	    this.clear();

	    if (!user) {
		this.show('fetching user info...');
	    }
	    else if (this.isLoggedInandInitialized()) {
		if (done) {
		    if (this.model.get('result') === 'ALREADY_VOTED') { 
			this.show('you already voted for this story');
		    }
		    else if (this.model.get('result') === 'ONLY_JUDGES_CAN_VOTE') { 
			this.show('Only judges can vote');
		    }
		    else {
			this.show('your vote was counted!');
		    }
		}
		else if (submitting == true) {
		    this.show('submitting your vote...');
		}
		else {
		    // present submission form
		    var el = this.make('form', {}, this.formContent());
		    $(this.el).append(el);
		    this.$('input[name=url]').attr('value', this.model.get('link'));
		    this.$('input[name=title]').attr('value', this.model.get('title'));

		    var self = this;
		    this.$('input[name=submitStory]').click(function (event) {
			event.preventDefault();
			doRequest({fun: 'submitStory',
				   url: self.$('input[name=url]').attr('value'),
				   title: self.$('input[name=title]').attr('value') },
				  function (data) {
				      self.model.set({done: true});
				  });
		    });
		    this.$('input[name=guessTitle]').click(function (event) {
			event.preventDefault();
			flashInfo('Guessing...');
			doRequest({fun: 'grabTitle',
				   url: self.$('input[name=url]').attr('value')},
				  function (data) {
				      flashInfo('');
				      self.model.set({title: data});
				  });
		    });
		    return this;
		}
	    }
	    else {
		this.show("You must finish registering your account before you can vote.");
	    }
	},	
    });
    
    window.initRNA();
    window.app = new SubmissionView;
    window.app.tryVote();
    log({info: 'vote loaded'});
});
