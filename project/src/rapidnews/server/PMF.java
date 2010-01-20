package rapidnews.server;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

public final class PMF {
    private static final PersistenceManagerFactory pmfInstance =
        JDOHelper.getPersistenceManagerFactory("transactions-optional");

    private static PersistenceManager pmInstance = null;

    private PMF() {}

    public static PersistenceManagerFactory get() {
        return pmfInstance;
    }

	public static PersistenceManager getPersistenceManager() {
		if (pmInstance == null || pmInstance.isClosed())
			pmInstance = get().getPersistenceManager();
		return pmInstance;
	}
}
