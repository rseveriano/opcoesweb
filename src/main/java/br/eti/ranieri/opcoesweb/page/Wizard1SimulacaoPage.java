package br.eti.ranieri.opcoesweb.page;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.RadioChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.MaximumValidator;
import org.apache.wicket.validation.validator.MinimumValidator;
import org.apache.wicket.validation.validator.RangeValidator;
import org.joda.time.LocalDate;

import br.eti.ranieri.opcoesweb.estado.Acao;
import br.eti.ranieri.opcoesweb.jodatime.LocalDateField;
import br.eti.ranieri.opcoesweb.simulacao.ConfigSimulacao;

public class Wizard1SimulacaoPage extends PaginaBase {

    private final ConfigSimulacao config = new ConfigSimulacao();

    public Wizard1SimulacaoPage() {
	add(new FeedbackPanel("feedback"));
	Form form = new Form("formulario") {
	    @Override
	    protected void onSubmit() {
		setResponsePage(new Wizard2SimulacaoPage(config));
	    }
	};
	add(form);

	form.setModel(new CompoundPropertyModel(config));
	form.add(new RadioChoice("acoes", new PropertyModel(config, "acao"), new LoadableDetachableModel<List<Acao>>() {
	    @Override
	    protected List<Acao> load() {
	        return Arrays.asList(Acao.values());
	    }
	}, new IChoiceRenderer<Acao>() {
	    public Object getDisplayValue(Acao acao) {
	        return acao.getCodigo();
	    }
	    public String getIdValue(Acao acao, int index) {
	        return String.valueOf(index);
	    }
	}).setSuffix("").setRequired(true));

	final LocalDateField dataInicialField = new LocalDateField("dataInicial");
	form.add(dataInicialField.setRequired(true));

	final LocalDateField dataFinalField = new LocalDateField("dataFinal");
	form.add(dataFinalField.setRequired(true));

	form.add(new TextField("custodiaInicial").setRequired(true).add(
		new MinimumValidator<Integer>(1)));
	form.add(new TextField("prejuizoMaximo").setRequired(true).add(
		new MaximumValidator<Double>(0.0)));

	form.add(new TextField("corretagemIntegral").setRequired(true).add(
		new MinimumValidator<Double>(0.01)));
	form.add(new TextField("corretagemOpcoes").setRequired(true).add(
		new MinimumValidator<Double>(0.01)));
	form.add(new TextField("corretagemFracionario").setRequired(true).add(
		new MinimumValidator<Double>(0.01)));

	form.add(new AbstractFormValidator() {
	    public FormComponent<?>[] getDependentFormComponents() {
		return new FormComponent<?>[] { dataInicialField, dataFinalField };
	    }

	    public void validate(Form<?> form) {
		LocalDate dataInicial = dataInicialField.getConvertedInput();
		LocalDate dataFinal = dataFinalField.getConvertedInput();
		if (dataFinal != null && dataInicial != null) {
		    if (dataFinal.isBefore(dataInicial))
			this.error(dataInicialField, "ValidacaoInicialAntesDeFinal");
		    if (dataFinal.isAfter(new LocalDate()))
			this.error(dataFinalField, "ValidacaoFinalAntesDeHoje");
		}
	    }
	});
    }
}
