package br.eti.ranieri.opcoesweb;

import org.apache.wicket.Request;
import org.apache.wicket.Response;
import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;

import br.eti.ranieri.opcoesweb.page.HomePage;

/**
 * Application object for your web application. If you want to run this application without deploying, run the Start class.
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
        addComponentInstantiationListener(new SpringComponentInjector(this));
    }

    @Override
    public Session newSession(Request request, Response response) {
    	return new OpcoesWebHttpSession(request);
    }

	/**
     * @see org.apache.wicket.Application#getHomePage()
     */
    public Class<HomePage> getHomePage() {
        return HomePage.class;
    }
}
