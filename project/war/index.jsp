<%@ page import="org.rapidnewsawards.core.Donation" %>
<%@ page import="org.rapidnewsawards.server.DAO" %>
<%@ page import="org.rapidnewsawards.core.Periodical" %>
<!DOCTYPE HTML>
<html>
<head>
<title>Newskraft.org</title>
</head>

<body>

<%
	String outStr = "";
		outStr += "<h1> Newskraft.org Starts July 16th! </h1>";
		outStr += "For more info, see this <a href=\"http://rapidnewsawards.org/series.html\"> series of articles. </a>";
		outStr += "<h3> Questions?  Want to be a hypothetical donor? </h3>";
		outStr += "Email Lyn Headley: laheadle@gmail.com";

		int count = 0;
		for(Donation d : DAO.instance.ofy().query(Donation.class).order("date")) {
			if (count++ == 0) {
				outStr += "<h3> Confirmed Donors </h3>";
				outStr += "<table>";
			}
			
			String nameLink = "";
			if (d.webPage != null && !d.webPage.equals("")) {
				nameLink = String.format("<a href=\"%s\"> %s </a>", d.webPage, d.name);
			}
			else {
				nameLink = d.name;
			}
			outStr += String.format("<tr> <td> %s </td> <td> %s </td> </tr>", 
					nameLink, Periodical.moneyPrint(d.amount));
		}
%>

<%= outStr %>
</body>
</html>
