package br.eti.ranieri.opcoesweb.page;

import java.net.URL;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.validator.RangeValidator;
import org.apache.wicket.validation.validator.UrlValidator;
import org.slf4j.LoggerFactory;

import br.eti.ranieri.opcoesweb.OpcoesWebHttpSession;
import br.eti.ranieri.opcoesweb.estado.ConfiguracaoImportacao;
import br.eti.ranieri.opcoesweb.importacao.offline.ImportadorOffline;

public class ImportarSerieHistoricaPage extends PaginaBase {

	@SpringBean
	private ImportadorOffline importador;

	private Model<String> urlModel = new Model<String>();
	private Model<Integer> opcoesPorAcaoModel = new Model<Integer>();

	public ImportarSerieHistoricaPage() {
		add(new FeedbackPanel("feedback"));
		Form form = new Form("form") {
			@Override
			protected void onSubmit() {
				try {
					ConfiguracaoImportacao configuracaoImportacao = OpcoesWebHttpSession.get()
							.getConfiguracaoImportacao();

					Integer quantidade = opcoesPorAcaoModel.getObject();
					if (quantidade != null) {
						configuracaoImportacao.setQuantidadeOpcoesPorAcaoPorDia(quantidade);
					}

					importador.importar(new URL(urlModel.getObject()), configuracaoImportacao);
					info("Importação realizada com sucesso.");
				} catch (Exception e) {
					LoggerFactory.getLogger(getClass()).error("Erro na importacao", e);
					error(e.getMessage());
				}
			}
		};
		add(form);

		form.add(new TextField<String>("localizacao", urlModel) //
				.setRequired(true) //
				.add(new UrlValidator()));

		form.add(new Label("opcoesPorAcaoAtual", new LoadableDetachableModel<String>() {
			@Override
			protected String load() {
				return String.format("%d", OpcoesWebHttpSession.get().getConfiguracaoImportacao()
						.getQuantidadeOpcoesPorAcaoPorDia());
			}
		}));

		form.add(new TextField<Integer>("opcoesPorAcao", opcoesPorAcaoModel, Integer.class) //
				.add(new RangeValidator<Integer>(1, 20)));
	}

}
