// Load the application once the DOM is ready, using `jQuery.ready`:
$(function(){

    window.DonateView = Backbone.View.extend({

	el: $('#main'),

	events: {},

	get: function(val) {
	    return this.$('input[name='+val+']').attr('value');
	},

	initialize: function() {
	    var self = this;
	    $('#submit').click(function() {
		event.preventDefault();
		window.doRequest({fun: 'donate', 
				  webPage: self.get('webPage'),
				  name: self.get('name'),
				  statement: self.get('statement'),
				  donation: self.get('donation')},
				 function (data) {
				     window.flashLog({type:'success', content: 'Thank You!'});
				 },
				 function() { 
				     window.flashLog({type:'error', content: 'Error'});
				 });
	    });
	}

    });
    
    window.initRNA();
    window.app = new DonateView;
    log({info: 'donate loaded'});
});
