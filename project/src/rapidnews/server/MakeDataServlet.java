package rapidnews.server;

import java.io.IOException;
import java.io.PrintWriter;

import javax.jdo.PersistenceManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MakeDataServlet extends HttpServlet {
	  public void doGet(HttpServletRequest request, HttpServletResponse response)
	  						throws ServletException, IOException {
		  PrintWriter out = response.getWriter();
		  PersistenceManager pm = PMF.get().getPersistenceManager();

		  Reader mg = new Reader("Megan Garber", "megangarber");
		  Reader jy = new Reader("Josh Young", "jny2");
		  Reader so = new Reader("Steve Outing", "steveouting");

		  try {
			  pm.makePersistent(mg);
			  pm.makePersistent(jy);
			  pm.makePersistent(so);
		  } finally {
			  pm.close();
		  }
		  out.println("created 3 readers");
	  }
}
