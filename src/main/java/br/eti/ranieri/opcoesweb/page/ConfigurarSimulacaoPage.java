package br.eti.ranieri.opcoesweb.page;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.RadioChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.validator.MaximumValidator;
import org.apache.wicket.validation.validator.MinimumValidator;
import org.apache.wicket.validation.validator.RangeValidator;
import org.joda.time.Days;
import org.joda.time.LocalDate;

import br.eti.ranieri.opcoesweb.estado.Acao;
import br.eti.ranieri.opcoesweb.jodatime.LocalDateField;
import br.eti.ranieri.opcoesweb.simulacao.ConfigSimulacao;
import br.eti.ranieri.opcoesweb.simulacao.DetectorEventoQueAlteraPreco;
import br.eti.ranieri.opcoesweb.simulacao.EventoQueAlteraPreco;
import br.eti.ranieri.opcoesweb.simulacao.Simulador;
import br.eti.ranieri.opcoesweb.simulacao.SimuladorTeorico;

public class ConfigurarSimulacaoPage extends PaginaBase {

	private static final int DURACAO_MAXIMA_EM_DIAS = 330;

	@SpringBean
	Simulador simulador;
	@SpringBean
	SimuladorTeorico simuladorTeorico;
	@SpringBean
	DetectorEventoQueAlteraPreco detectorAlteracao;

	private final ConfigSimulacao config = new ConfigSimulacao();
	private boolean secaoEventosVisivel = false;
	private WebMarkupContainer secaoAlteracaoPreco;
	private EventoModel eventoModel;
	
	public ConfigurarSimulacaoPage() {
		add(new FeedbackPanel("feedback"));
		Form form = new Form("formulario") {
			@Override
			protected void onSubmit() {
				if (secaoEventosVisivel == false) {
					secaoAlteracaoPreco.setVisible(secaoEventosVisivel = true);
				} else {
//					simulador.simular(config, eventoModel.getObject());
					simulador.simular(config);
				}
			}
		};
		add(form);
		
		form.setModel(new CompoundPropertyModel(config));
		form.add(new RadioChoice("acoes", new PropertyModel(config, "acao"), Arrays.asList(Acao.values())).setSuffix("").setRequired(true));

		final LocalDateField dataInicialField = new LocalDateField("dataInicial");
		form.add(dataInicialField.setRequired(true));

		final LocalDateField dataFinalField = new LocalDateField("dataFinal");
		form.add(dataFinalField.setRequired(true));

		form.add(new TextField("custodiaInicial").setRequired(true).add(new MinimumValidator<Integer>(1)));
		form.add(new TextField("prejuizoMaximo").setRequired(true).add(new MaximumValidator<Double>(0.0)));

		form.add(new TextField("tempoMaximoSimulacao").setRequired(true).add(new RangeValidator<Integer>(1,120)));
		form.add(new TextField("corretagemIntegral").setRequired(true).add(new MinimumValidator<Double>(0.01)));
		form.add(new TextField("corretagemOpcoes").setRequired(true).add(new MinimumValidator<Double>(0.01)));
		form.add(new TextField("corretagemFracionario").setRequired(true).add(new MinimumValidator<Double>(0.01)));

		secaoAlteracaoPreco = new WebMarkupContainer("secaoAlteracaoPreco");
		secaoAlteracaoPreco.setVisible(secaoEventosVisivel);
		secaoAlteracaoPreco.add(new ListView<EventoQueAlteraPreco>("eventos", eventoModel = new EventoModel()) {

			@Override
			protected void populateItem(ListItem<EventoQueAlteraPreco> item) {
				IModel<EventoQueAlteraPreco> model = item.getModel();
				EventoQueAlteraPreco evento = model.getObject();
				item.add(new CheckBox("checagem", new PropertyModel(model, "selecionado")));
				item.add(new Label("data", evento.getDataFormatada()));
				item.add(new Label("descricao", evento.getDescricao()));
			}
		});
		form.add(secaoAlteracaoPreco);

		form.add(new AbstractFormValidator() {
			public FormComponent<?>[] getDependentFormComponents() {
				return new FormComponent<?>[] {dataInicialField, dataFinalField};
			}
			public void validate(Form<?> form) {
				LocalDate dataInicial = dataInicialField.getConvertedInput();
				LocalDate dataFinal = dataFinalField.getConvertedInput();
				if (dataFinal != null && dataInicial != null) {
					if (dataFinal.isBefore(dataInicial))
						this.error(dataInicialField, "ValidacaoInicialAntesDeFinal");
					if (dataFinal.isAfter(new LocalDate()))
						this.error(dataFinalField, "ValidacaoFinalAntesDeHoje");
					if (Days.daysBetween(dataInicial, dataFinal).getDays() > DURACAO_MAXIMA_EM_DIAS) {
						Map<String, Object> vars = super.variablesMap();
						vars.put("duracaoMaxima", DURACAO_MAXIMA_EM_DIAS);
						this.error(dataFinalField, "ValidacaoPeriodoMuitoLongo", vars);
					}
				}
			}
		});
	}

	class EventoModel extends LoadableDetachableModel<List<EventoQueAlteraPreco>> {

		@Override
		protected List<EventoQueAlteraPreco> load() {
			return detectorAlteracao.getEventosQueAlteramPreco(config);
		}

	}
}
