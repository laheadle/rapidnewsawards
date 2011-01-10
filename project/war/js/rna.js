window.initRNA = function () {
    window.FlashView = Backbone.View.extend({
	el: $("#flash"),

	model: new Backbone.Model,

	initialize: function() {
	    var self = this;
	    this.model.bind('change', function () { self.render() });
	    this.model.view = this;
	},

	flag: function(type) {
	    var text = {
		error: 'Error',
		success: 'Ok',
		info: '',
	    }
	    
	    var span = this.$('span.flag');
	    span.removeClass('error success info').addClass(type);
	    span.text(text[type]);
	},

	render: function() {
	    var type = this.model.get('type');
	    this.flag(type);
	    this.$('span.content').text(this.model.get('content') || "");
	}
    });


    // takes a string
    window.flashError = function(err) {
	window.flashLog({type: 'error', content: msg});
    };

    window.flashLog = function(msg) {
	window.flashView.model.set(msg);
    };

    window.flashClear = function() {
	window.flashView.model.clear();
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

    window.doRequest = function(attrs, success, err) {
	log({info: 'doRequest: ' + JSON.stringify(attrs)});
	$.get('JSONrpc', attrs, 
	      function (data) { 
		  data && log(attrs.fun + ' returned ' + JSON.stringify(data));
		  try {
		      if (!data || data == "" || data.match(/^[ \t\r\n]+/$)) {
			  success(undefined);
		      }
		      else {
			  success(JSON.parse(data));
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
	      });
    };


    window.doPostRequest = function(attrs, success, err) {
	log({info: 'doRequest: ' + JSON.stringify(attrs)});
	$.post('JSONrpc', attrs, 
	       function (data) { 
		   data && log(attrs.fun + ' returned ' + JSON.stringify(data));
		   try {
		       if (!data || data == "" || data.match(/^[ \t\r\n]+/$)) {
			   success(undefined);
		       }
		       else {
			   success(JSON.parse(data));
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
	       });
    };


    window.changeURL = function(command) {
	doRequest({ fun: command,
		    url: window.location.href }, 
		  function(data) { 
		      if (data) {
			  window.location = data;
		      }
		      else {
			  log({error: 'could not change window location'});
		      }
		  });
    };


}