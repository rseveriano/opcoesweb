package br.eti.ranieri.opcoesweb;

import org.apache.wicket.Component;
import org.apache.wicket.Request;
import org.apache.wicket.Response;
import org.apache.wicket.RestartResponseAtInterceptPageException;
import org.apache.wicket.Session;
import org.apache.wicket.authorization.Action;
import org.apache.wicket.authorization.IAuthorizationStrategy;
import org.apache.wicket.authorization.IUnauthorizedComponentInstantiationListener;
import org.apache.wicket.protocol.http.HttpSessionStore;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.session.ISessionStore;
import org.apache.wicket.settings.ISecuritySettings;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;

import br.eti.ranieri.opcoesweb.page.HomePage;
import br.eti.ranieri.opcoesweb.page.LoginPage;
import br.eti.ranieri.opcoesweb.page.PaginaBase;

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

		ISecuritySettings securitySettings = getSecuritySettings();

		securitySettings.setAuthorizationStrategy(new IAuthorizationStrategy() {
			public boolean isActionAuthorized(Component component, Action action) {
				return true;
			}

			public <T extends Component> boolean isInstantiationAuthorized(Class<T> componentClass) {
				if (PaginaBase.class.isAssignableFrom(componentClass)) {
					return OpcoesWebHttpSession.get().isAutenticado();
				}
				return true;
			}
		});

		securitySettings.setUnauthorizedComponentInstantiationListener( //
				new IUnauthorizedComponentInstantiationListener() {
					public void onUnauthorizedInstantiation(Component component) {
						throw new RestartResponseAtInterceptPageException(LoginPage.class);
					}
				});
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
