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
		success: 'Ok',
		info: '',
	    }
	    
	    var span = this.$('span.flag');
	    span.removeClass('error success info notice').addClass(type);
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
	    console.log(options.info || options);
	}
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
	    var empty = !data || data == "" || data.match(/^[ \t\r\n]+/$);
	    log(attrs.fun + ' returned ' + 
		(empty? 'null' : JSON.stringify(data)));
	    try {
		var arg = empty? undefined : JSON.parse(data);
		if (!self.state.interrupted) {
		    self.state = function () { 
			var s = success(arg, self.state);
			if (s == undefined) {
			    return self.state;
			}
			return s;
		    }(); // ensure state
		}
		else {
		    self.state = self.state.supercede(success, arg);
		}

	    }
	    catch (e) {
		if (err) {
		    err(e);
		}
		else {
		    flashError(e.toString());
		}
	    }
	};
	method.apply($, ['JSONrpc', attrs, reactTo]);
    };

    // ajax call where method is either $.get or $.post
    window.requester = new Requester;

    window.doRequest = function(attrs, success, err) {
	requester.request($.get, attrs, success, err);
    };


    window.doPostRequest = function(attrs, success, err) {
	requester.request($.post, attrs, success, err);
    };

    window.redirectForLogin = function(command) {
	var newUrl = window.location.href.replace(/html(#.*)?/,
	    'html#createAccount/' 
	    + encodeURIComponent(window.location.href));
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

    window.Utils = {

	processEdition: function(edition) {
	    var _edition = _.clone(edition);
	    _edition.endStr = window.Utils.displayDate(edition.end);
	    return _edition;
	},

	displayDate: function(dateStr) {
	    var d = new Date(dateStr + ' UTC');
	    return '' + (d.getMonth() + 1) + '/' +
		d.getDate() + ' at ' + (d.toLocaleTimeString())
	    .replace(/:[0-9][0-9]$/, '');
	},
    };

}