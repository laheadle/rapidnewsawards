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
		window.doRequest({fun: 'donate', 
				  webpage: self.get('webpage'),
				  name: self.get('name'),
				  statement: self.get('statement'),
				  donation: self.get('donation')},
				 function (data) {
				     alert('thank you!');
				 });
	    });
	}

    });
    
    window.initRNA();
    window.app = new DonateView;
    log({info: 'donate loaded'});
});
