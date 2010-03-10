package br.eti.ranieri.opcoesweb.page;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.LoggerFactory;

import br.eti.ranieri.opcoesweb.importacao.offline.ImportadorOffline;

public class ImportarSerieHistoricaPage extends PaginaBase {

	@SpringBean
	private ImportadorOffline importador;
	
	private Model<String> localizacao = new Model<String>();
	
	public ImportarSerieHistoricaPage() {
		add(new FeedbackPanel("feedback"));
		Form form = new Form("form") {
			@Override
			protected void onSubmit() {
				try {
					importador.importar(localizacao.getObject(), getSessaoHttp().getConfiguracaoImportacao());
					info("Importação realizada com sucesso.");
				} catch (Exception e) {
					LoggerFactory.getLogger(getClass()).error("Erro na importacao", e);
					error(e.getMessage());
				}
			}
		};
		add(form);
		
		form.add(new TextField("localizacao", localizacao).setRequired(true));
	}

}
