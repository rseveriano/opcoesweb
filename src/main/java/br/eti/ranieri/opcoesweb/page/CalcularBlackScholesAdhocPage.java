package br.eti.ranieri.opcoesweb.page;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RadioChoice;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.joda.time.LocalDate;

import br.eti.ranieri.opcoesweb.blackscholes.BlackScholes;
import br.eti.ranieri.opcoesweb.estado.CotacaoOpcao;
import br.eti.ranieri.opcoesweb.estado.Serie;
import br.eti.ranieri.opcoesweb.estado.Variavel;
import br.eti.ranieri.opcoesweb.jodatime.LocalDateField;

public class CalcularBlackScholesAdhocPage extends PaginaBase {

    private static final Locale ptBR = new Locale("pt", "BR");

    @SpringBean
    BlackScholes blackScholes;

    private Serie serie;
    private double precoExercicio;
    private LocalDate dataPregao;
    private LocalDate dataExercicio;
    private double precoAcao;
    private double taxaJuros;
    private double volatilidade;
    
    private CotacaoOpcao opcaoTeorica;

    public CalcularBlackScholesAdhocPage() {
	add(new FeedbackPanel("feedback"));
	
	Form form = new Form("formulario", new CompoundPropertyModel(this));
	
	form.add(new RadioChoice("serie", Arrays.asList(Serie.values())).setSuffix(",").setRequired(true));
	form.add(new RequiredTextField("precoExercicio"));
	form.add(new LocalDateField("dataPregao").setRequired(true));
	form.add(new LocalDateField("dataExercicio").setRequired(true));
	form.add(new RequiredTextField("precoAcao"));
	form.add(new RequiredTextField("taxaJuros"));
	form.add(new RequiredTextField("volatilidade"));
	
	final WebMarkupContainer resultadoSimulacao = new WebMarkupContainer("resultadoSimulacao");
	resultadoSimulacao.setOutputMarkupId(true);

	form.add(new AjaxButton("submeter", form) {

	    @Override
	    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
		opcaoTeorica = blackScholes.calcularOpcaoTeoricaAdhoc(serie, precoExercicio,
			dataPregao, dataExercicio, precoAcao, taxaJuros, volatilidade);
		resultadoSimulacao.add(new SimpleAttributeModifier("style", "display: block"));
		target.addComponent(resultadoSimulacao);
	    }
	});
	
	add(form);
	
	resultadoSimulacao.add(new SimpleAttributeModifier("style", "display:none"));
	resultadoSimulacao.add(new Label("precoTeorico", new ResultadoModel(Variavel.PRECO_REAL)));
	resultadoSimulacao.add(new Label("taxaVE", new ResultadoModel(Variavel.TAXA_VE)));
	resultadoSimulacao.add(new Label("theVE", new ResultadoModel(Variavel.THE_VE)));
	resultadoSimulacao.add(new Label("naoVende", new ResultadoModel(Variavel.NAO_VENDE)));
	resultadoSimulacao.add(new Label("atm", new ResultadoModel(Variavel.ATM)));
	add(resultadoSimulacao);
    }

    private class ResultadoModel extends Model<String> {
	private Variavel variavel;
	public ResultadoModel(Variavel variavel) {
	    this.variavel = variavel;
	}
	@Override
	public String getObject() {
	    if (opcaoTeorica == null) {
		return "";
	    }
	    Number valor = opcaoTeorica.getVariaveis().get(variavel);
	    return variavel.getFormatador().formatar(valor);
	}
    }
}
