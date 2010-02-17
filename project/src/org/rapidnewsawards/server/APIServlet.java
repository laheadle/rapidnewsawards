package org.rapidnewsawards.server;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/*
 *   <!--
  http://api.twitter.com/1/clog9/lists/memberships.json
  http://api.twitter.com/1/rapidnewsawards/editors/members.json
  -->
 */
public class APIServlet extends HttpServlet {
	  public void doGet(HttpServletRequest request, HttpServletResponse response)
	  						throws ServletException, IOException {
		  PrintWriter out = response.getWriter();
		  String s = request.getRequestURL().toString();
		  response.setContentType("application/json");
		  if (s.contains("clog9/lists/memberships.json")) {
			  String output = 
			  "{'lists':[" +
			  	"{'mode':'public', " +
			  	"'description':'Root Editors'," +
			  	"'uri':'/laheadle/editors'," +
			  	"'subscriber_count':1," +
			  	"'full_name':'@laheadle/editors',"+
			  	"'user':{'screen_name': 'laheadle', 'id': 1},"+
			  	"'slug':'editors',"+
			  	"'name':'editors',"+
			  	"'id':1,"+
			  	"'member_count':3}" +
			  	"], " +
			  "}";
			  out.println(output);
		  }
		  else if (s.contains("/rapidnewsawards/editors/members.json")) {
			  out.println(
					  "{'users':[ " +
					                 "{ 'screen_name': 'megangarber', " +
					                 "   'id': 2 }, " +
					                 "{ 'screen_name': 'jny2'," +
					                 "'  id': 3  }, " +
					                 "{ 'screen_name': 'steveouting'," +
					                 "  'id': 4  } " +
					            "]" + 
					   "}");			  
		  }
	  }
}
