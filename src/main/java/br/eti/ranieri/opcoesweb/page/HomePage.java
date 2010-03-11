package br.eti.ranieri.opcoesweb.page;

import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.basic.Label;

/**
 * Homepage
 */
public class HomePage extends PaginaBase {

    /**
	 * Constructor that is invoked when page is invoked without a session.
	 * 
	 * @param parameters
	 *            Page parameters
	 */
    public HomePage(final PageParameters parameters) {
        add(new Label("mensagem", "Bem-vindo ao sistema de apoio à decisão de operações com opções."));
    }
}
