// Load the application once the DOM is ready, using `jQuery.ready`:
$(function(){

    window.DonateView = Backbone.View.extend({

	el: $('#main'),

	events: {},

	get: function(val) {
	    return this.$('input[name='+val+']').attr('value');
	},

	getCheck: function(val) {
	    return this.$('input[name='+val+']').attr('checked')
	},

	initialize: function() {
	    var self = this;
	    $('#submit').click(function(event) {
		event.preventDefault();
		window.flashLog({type:'notice', content: 'Submitting...'});
		window.doRequest({fun: 'donate', 
				  webPage: self.get('webPage'),
				  name: self.get('name'),
				  statement: self.get('statement'),
				  donation: self.get('donation'),
				  consent: self.getCheck('consent') ? 'true' : 'false'
				 },
				 function (data) {
				     window.flashLog({type:'success', 
						      content: 'Your information was saved. Thank You.'});
				 },
				 function(response) { 
				     window.flashLog({type:'error', content: response.message});
				 });
	    });
	}

    });
    
    window.initRNA();
    window.app = new DonateView;
    log({info: 'donate loaded'});
});
