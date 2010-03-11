package br.eti.ranieri.opcoesweb.page;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebRequest;

public class LogoutPage extends WebPage {

    public LogoutPage() {
	getSession().invalidateNow();
	String contextPath = ((WebRequest) getRequest()).getHttpServletRequest().getContextPath();
	getResponse().redirect(contextPath);
    }

}
