package org.rapidnewsawards.server;

/*
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.assistedinject.FactoryProvider;

public class Config {

	public static class RNAModule extends AbstractModule {
		@Override 
		protected void configure() {
			bind(PerishableFactory.class).toProvider(
					FactoryProvider.newFactory(PerishableFactory.class, Calendar.class));
		}
	}	
	
	public static Injector injector;

	public static void init() {
		injector = Guice.createInjector(new RNAModule());	
	}

	static {
		init();
	}

}
*/