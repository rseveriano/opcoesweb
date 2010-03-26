package br.eti.ranieri.opcoesweb;

import org.apache.wicket.Application;
import org.apache.wicket.Request;
import org.apache.wicket.Response;
import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.HttpSessionStore;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.session.ISessionStore;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;

import br.eti.ranieri.opcoesweb.page.ConfigurarOnlinePage;
import br.eti.ranieri.opcoesweb.page.HomePage;

import com.google.appengine.api.utils.SystemProperty;

/**
 * Application object for your web application. If you want to run this
 * application without deploying, run the Start class.
 * 
 * @see br.eti.ranieri.opcoesweb.Start#main(String[])
 */
public class WicketApplication extends WebApplication {

	/**
	 * Constructor
	 */
	public WicketApplication() {
	}

	@Override
	protected void init() {
		super.init();
		this.getResourceSettings().setResourcePollFrequency(null);

		addComponentInstantiationListener(new SpringComponentInjector(this));
		
		mountBookmarkablePage("home", HomePage.class);
		mountBookmarkablePage("configurarOnline", ConfigurarOnlinePage.class);

//		ISecuritySettings securitySettings = getSecuritySettings();
//
//		securitySettings.setAuthorizationStrategy(new IAuthorizationStrategy() {
//			public boolean isActionAuthorized(Component component, Action action) {
//				return true;
//			}
//
//			public <T extends Component> boolean isInstantiationAuthorized(Class<T> componentClass) {
//				if (PaginaBase.class.isAssignableFrom(componentClass)) {
//					return OpcoesWebHttpSession.get().isAutenticado();
//				}
//				return true;
//			}
//		});
//
//		securitySettings.setUnauthorizedComponentInstantiationListener( //
//				new IUnauthorizedComponentInstantiationListener() {
//					public void onUnauthorizedInstantiation(Component component) {
//						throw new RestartResponseAtInterceptPageException(LoginPage.class);
//					}
//				});
	}

	@Override
	public String getConfigurationType() {
		// App Engine set the system property which identify the runtime environment
		if (SystemProperty.Environment.Value.Production.equals(SystemProperty.environment.value())) {
			return Application.DEPLOYMENT;
		}
		return super.getConfigurationType();
	}

	@Override
	public Session newSession(Request request, Response response) {
		return new OpcoesWebHttpSession(request);
	}

	@Override
	protected ISessionStore newSessionStore() {
		return new HttpSessionStore(this);
	}

	/**
	 * @see org.apache.wicket.Application#getHomePage()
	 */
	public Class<HomePage> getHomePage() {
		return HomePage.class;
	}
}
