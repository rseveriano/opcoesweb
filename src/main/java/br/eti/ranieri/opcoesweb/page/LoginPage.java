package br.eti.ranieri.opcoesweb.page;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.StatelessForm;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.CompoundPropertyModel;

import br.eti.ranieri.opcoesweb.OpcoesWebHttpSession;

public class LoginPage extends WebPage {

    public LoginPage() {
	add(new FormularioLogin("formulario").add(new FeedbackPanel("feedback")));
    }

    private static class FormularioLogin extends StatelessForm {
	private String usuario;
	private String senha;

	public FormularioLogin(String id) {
	    super(id);
	    setModel(new CompoundPropertyModel(this));
	    add(new TextField("usuario"));
	    add(new PasswordTextField("senha"));
	}

	@Override
	protected void onSubmit() {
	    if ("ranieri.severiano".equals(usuario) && "k6cP4vy".equals(senha)) {
		OpcoesWebHttpSession.get().setAutenticado(true);
		if (!continueToOriginalDestination()) {
		    setResponsePage(getApplication().getHomePage());
		}
	    } else {
		error("Usuário e/ou senha incorretos!");
	    }
	}

	public String getUsuario() {
	    return usuario;
	}

	public void setUsuario(String usuario) {
	    this.usuario = usuario;
	}

	public String getSenha() {
	    return senha;
	}

	public void setSenha(String senha) {
	    this.senha = senha;
	}

    }
}
