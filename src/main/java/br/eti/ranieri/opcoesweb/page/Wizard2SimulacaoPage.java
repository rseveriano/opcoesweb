package br.eti.ranieri.opcoesweb.page;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import br.eti.ranieri.opcoesweb.simulacao.ConfigSimulacao;
import br.eti.ranieri.opcoesweb.simulacao.DataOpcoes;
import br.eti.ranieri.opcoesweb.simulacao.DetectorEventoQueAlteraPreco;
import br.eti.ranieri.opcoesweb.simulacao.SimuladorManual;
import br.eti.ranieri.opcoesweb.simulacao.DataOpcoes.CodigoVariaveis;

public class Wizard2SimulacaoPage extends PaginaBase {

    private static final Locale ptBR = new Locale("pt", "BR");
    private static final NumberFormat integerFormat = NumberFormat.getIntegerInstance(ptBR);
    private static final NumberFormat moneyFormat = NumberFormat.getCurrencyInstance(ptBR);
    private static final DateTimeFormatter ddmmyyFormat = DateTimeFormat.forPattern("dd/MM/yy").withLocale(ptBR);
    private static final DateTimeFormatter ddmmFormatter = DateTimeFormat.forPattern("dd/MM").withLocale(ptBR);

    @SpringBean
    DetectorEventoQueAlteraPreco detectorAlteracao;
    @SpringBean
    SimuladorManual simulador;

    private final Map<LocalDate, CodigoVariaveis> opcaoPorData = new HashMap<LocalDate, CodigoVariaveis>();

    public Wizard2SimulacaoPage(ConfigSimulacao config) {
	add(new FeedbackPanel("feedback"));

	add(new Label("acao", new Model(config.acao.getCodigo())));
	add(new Label("custodiaInicial", new Model(integerFormat.format(config.custodiaInicial))));
	add(new Label("dataInicial", new Model(ddmmyyFormat.print(config.dataInicial))));
	add(new Label("dataFinal", new Model(ddmmyyFormat.print(config.dataFinal))));
	add(new Label("prejuizoMaximo", new Model(moneyFormat.format(config.prejuizoMaximo))));
	add(new Label("corretagemIntegral", new Model(moneyFormat.format(config.corretagemIntegral))));
	add(new Label("corretagemOpcoes", new Model(moneyFormat.format(config.corretagemOpcoes))));
	add(new Label("corretagemFracionario", new Model(moneyFormat.format(config.corretagemFracionario))));

	Form form = new Form("formulario") {
	    @Override
	    protected void onSubmit() {
	        System.out.println("opcaoPorData:\n" + opcaoPorData);
	    }
	};
	form.add(new DayListView("diaLoop", simulador.getOpcoesPorData(config)));
	add(form);
    }

    private class DayListView extends ListView<DataOpcoes> {

	public DayListView(String id, List<DataOpcoes> datasOpcoes) {
	    super(id, datasOpcoes);
	}

	@Override
	protected void populateItem(ListItem<DataOpcoes> item) {
	    final DataOpcoes dataOpcoes = item.getModelObject();
	    item.add(new Label("dia", ddmmFormatter.print(dataOpcoes.getData())));
	    RadioGroup radioGroup = new RadioGroup("radioGroup", new Model<CodigoVariaveis>() {
		@Override
		public void setObject(CodigoVariaveis codigoVariaveis) {
		    super.setObject(codigoVariaveis);
		    opcaoPorData.put(codigoVariaveis.data, codigoVariaveis);
		}
	    });
	    item.add(radioGroup.add(new OptionListView("opcaoLoop", dataOpcoes.getCodigosVariaveis())));
	}
    }

    private class OptionListView extends ListView<CodigoVariaveis> {

	public OptionListView(String id, List<CodigoVariaveis> codigosVariaveis) {
	    super(id, codigosVariaveis);
	}

	@Override
	protected void populateItem(ListItem<CodigoVariaveis> item) {
	    item.add(new Radio("radio", new Model(item.getModelObject())));
	    
	    WebMarkupContainer detalhes = new WebMarkupContainer("detalhes");
	    item.add(detalhes);
	    detalhes.setOutputMarkupId(true);
	    String idDetalhes = detalhes.getMarkupId();
	    
	    CodigoVariaveis codigoVariaveis = item.getModelObject();
	    detalhes.add(new Label("precoAcao", codigoVariaveis.precoAcao));
	    detalhes.add(new Label("precoReal", codigoVariaveis.precoReal));
	    detalhes.add(new Label("strike", codigoVariaveis.strike));
	    detalhes.add(new Label("volume", codigoVariaveis.volume));
	    detalhes.add(new Label("taxaVE", codigoVariaveis.taxaVE));
	    detalhes.add(new Label("theVE", codigoVariaveis.theVE));
	    detalhes.add(new Label("vdx", codigoVariaveis.vdx));
	    detalhes.add(new Label("naoVende", codigoVariaveis.naoVende));
	    detalhes.add(new Label("bosi", codigoVariaveis.bosi));
	    
	    WebMarkupContainer codigoLink = new WebMarkupContainer("codigoLink");
	    codigoLink.setOutputMarkupId(true);

	    String codigo = simulador.isOpcaoQueMantemCarteira(codigoVariaveis) ? codigoVariaveis.codigo
		    : StringUtils.substring(codigoVariaveis.codigo, 4);
	    codigoLink.add(new SimpleAttributeModifier("href", "$" + idDetalhes));
	    codigoLink.add(new SimpleAttributeModifier("title", codigoVariaveis.codigo.toUpperCase()));
	    
	    Label codigoText = new Label("codigoText", codigo);
	    item.add(codigoLink.add(codigoText));
	}
    }

}
