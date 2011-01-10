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
				    already: data.returnVal == 'ALREADY_VOTED'});
		}
		else {
		    self.model.unset('submitting', {silent: true});
		    self.model.set({link: link, title: ''});
		}
	    };

	    var setUser = function(data) {
		if (data) {
		    self.model.set({user: data, submitting: true});
		    doRequest({fun: 'voteFor', 
			       link: link,
			       fullLink: window.location.toString(), 
			       on: true},
			      setLink,
			      function (err) {
				  flashError(err.toString());
			      });

		}
		else {
		    self.model.set({user: {cid: 'guest'}});
		    window.changeURL('sendLoginURL');
		}
	    };

	    doRequest({ fun: 'sendUserInfo' },
		     setUser);
	},	

	show: function (txt) {
	    $(this.el).append(this.make('span', {}, txt));
	},
	
	clear: function () {
	    // clear it
	    $(this.el).html('');
	},

	render: function() {
	    var user = this.model.get('user');
	    var done = this.model.get('done');
	    var submitting = this.model.get('submitting');
	    
	    this.clear();

	    if (!user) {
		this.show('fetching user info...');
	    }
	    else if (user.nickname) {
		if (done) {
		    if (this.model.get('already') == true) { 
			this.show('you already voted for this story');
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
		    this.$('input[type=submit]').click(function (event) {
			event.preventDefault();
			doRequest({fun: 'submitStory',
				   url: self.$('input[name=url]').attr('value'),
				   title: self.$('input[name=title]').attr('value') },
				  function (data) {
				      self.model.set({done: true});
				  });
		    });
		    return this;
		}
	    }
	},	
    });
    
    window.initRNA();
    window.app = new SubmissionView;
    window.app.tryVote();
    log({info: 'vote loaded'});
});
