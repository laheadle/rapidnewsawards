package rapidnews.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

import javax.jdo.PersistenceManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import rapidnews.shared.Reader;

public class MakeDataServlet extends HttpServlet {
	  public void doGet(HttpServletRequest request, HttpServletResponse response)
	  						throws ServletException, IOException {
		  PrintWriter out = response.getWriter();
		  Reader mg = new Reader("Megan Garber", "megangarber");
		  Reader jy = new Reader("Josh Young", "jny2");
		  Reader so = new Reader("Steve Outing", "steveouting");
		  DAO.instance.ofy().put(Arrays.asList(mg, jy, so));
		  out.println("created 3 readers");
	  }
}
