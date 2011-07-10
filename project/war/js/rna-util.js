window.initRNA = function () {
    window.FlashView = Backbone.View.extend({
	el: $("#flash"),

	model: new Backbone.Model,

	initialize: function() {
	    var self = this;
	    this.model.bind('change', function () { self.render() });
	    this.model.view = this;
	},

	flag: function(type, header) {
	    var text = {
		error: 'Error',
		notice: 'Notice',
		redNotice: 'Notice',
		success: 'Ok',
		info: '',
	    }
	    
	    var span = this.$('span.flag');
	    span.removeClass('error success info notice redNotice').addClass(type);
	    span.text(header || text[type]);
	},

	render: function() {
	    var type = this.model.get('type');
	    var header = this.model.get('header');
	    this.flag(type, header);
	    this.$('span.content').html(this.model.get('content') || "");
	}
    });


    // takes a string
    window.flashError = function(msg) {
	window.flashLog({type: 'error', content: msg});
    };

    // takes a string
    window.flashInfo = function(msg) {
	window.flashLog({type: 'info', content: msg});
    };

    window.flashLog = function(msg) {
	if (flashView.model.get('content') === msg.content) {
	    return;
	}
	window.flashView.model.clear();
	window.flashView.model.set(msg);
    };

    window.flashClear = function() {
	window.flashLog({type: 'info', content: ''});
    };

    window.flashView = new FlashView;
    
    // global functions
    window.log = function(options) {
	if (options.error) {
	    alert(options.error.toString());
	}
	else {
	    rnaTrace(options.info || options);
	}
    };

    window.rnaTrace = function(s) {
	try { console.log(s) } catch (e) { }
    };

    var Requester = function() {
	this.state = {
	    interrupted: false
	};
    };
    
    Requester.prototype.request = function (method, attrs, success, err) {
	log({info: 'doRequest: ' + JSON.stringify(attrs)});
	var self = this;
	var reactTo = function (data) {
	    var empty = !data || data == "" || data.match(/^[ \t\r\n]+$/);
	    log(attrs.fun + ' returned ' + 
		(empty? 'null' : JSON.stringify(data)));
	    var bitOfProblem = 'bit of a problem...working on it.';
	    var response = empty? {status: false, message: bitOfProblem} : JSON.parse(data);
	    var payload = response.payload;

	    app.loginView.model.set(response.requester || {cid: 'guest'});
	    $('#loadMessage').html('');
	    if (response.status === 'BAD_REQUEST') { 
		if (err) { err(response); } else { flashError(response.message); }
		return;
	    }
	    if (response.status === 'TRY_AGAIN') { 
		flashInfo('The server is busy.  Pleasy try again');
		return;
	    }
	    else if (response.status !== 'OK') {
		flashError("Server Error.  We are looking into it, please try again in a bit.");
		return;
	    }
		if (!self.state.interrupted) {
		    self.state = function (state, payload) { 
			var s = success(payload, state);
			if (s == undefined) {
			    return state;
			}
			return s;
		    }(self.state, payload); // ensure state
		}
		else {
		    self.state = self.state.supercede(success, payload);
		}
	};
	method.apply($, ['JSONrpc', attrs, reactTo]);
    };

    window.reportError = function(attrs, error) {
	throw error;
    }

    // ajax call where method is either $.get or $.post
    window.requester = new Requester;

    window.doRequest = function(attrs, success, err) {
	requester.request($.get, attrs, success, err);
    };


    window.doPostRequest = function(attrs, success, err) {
	requester.request($.post, attrs, success, err);
    };

    window.redirectForLogin = function(command) {
	var newUrl = ( window.location.href.match(/#/) ?
		       window.location.href
		       .replace(/(#.*)$/,
				'#createAccount/' 
				+ encodeURIComponent(window.location.href))
		       :
		       window.location.href +
		       '#createAccount/' 
		       + encodeURIComponent(window.location.href));

	
	doRequest({ fun: command,
		    url: newUrl }, 
		  function(data) { 
		      if (data) {
			  window.location = data;
		      }
		      else {
			  log({error: 'Failed To Log in -- please notify laheadle@gmail.com'});
		      }
		  });
    };

    window.redirectForLogout = function(command) {
	var newUrl = window.location.href;
	doRequest({ fun: command,
		    url: newUrl }, 
		  function(data) { 
		      if (data) {
			  window.location = data;
		      }
		      else {
			  log({error: 'could not change window location'});
		      }
		  });
    };

    window.rMake = function (templateSelector, args) {
	return _.template($(templateSelector).html())(args || {});
    };

    $.ajaxSetup({timeout: 30000});
    $('body').ajaxError(function() { flashError("Communication error.  Please Check your network connection."); });

    // via http://stackoverflow.com/questions/68485/how-to-show-loading-spinner-in-jquery
    $('#loading')
	.hide()  // hide it initially
	.ajaxStart(function() {
            $(this).show();
	})
	.ajaxStop(function() {
            $(this).hide();
	});

}