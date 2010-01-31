package rapidnews.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import rapidnews.shared.Edition;
import rapidnews.shared.Periodical;
import rapidnews.shared.Reader;

import com.googlecode.objectify.Objectify;

public class MakeDataServlet extends HttpServlet {
	  public void doGet(HttpServletRequest request, HttpServletResponse response)
	  						throws ServletException, IOException {
		  PrintWriter out = response.getWriter();

		  Reader mg = new Reader("Megan Garber", "megangarber");
		  Reader jy = new Reader("Josh Young", "jny2");
		  Reader so = new Reader("Steve Outing", "steveouting");
		  DAO.instance.ofy().put(Arrays.asList(mg, jy, so));
		  out.println("created 3 readers");
		  
		  Periodical m = new Periodical("Journalism");
		  Objectify ofy = DAO.instance.fact().beginTransaction();
		  ofy.put(m);
		  
		  long oneDay = 24 * 60 * 60 * 1000;
		  Date end = new Date(new Date().getTime() + oneDay);

		  Edition[] editions = {
				  new Edition(m, end),
				  new Edition(m, new Date(end.getTime() + oneDay)),
				  new Edition(m, new Date(end.getTime() + 2 * oneDay)),
				  new Edition(m, new Date(end.getTime() + 3 * oneDay))
		  }; 

		  DAO.instance.ofy().put(Arrays.asList(editions));
		  
		  
		  m.setcurrentEditionKey(editions[0].getOKey());
		  ofy.put(m);
		  ofy.getTxn().commit();

		  
		  out.println("created 4 editions");
	  }
}
