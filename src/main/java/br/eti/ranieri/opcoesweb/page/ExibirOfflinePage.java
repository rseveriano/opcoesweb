package br.eti.ranieri.opcoesweb.page;

import static br.eti.ranieri.opcoesweb.estado.Acao.PETROBRAS;
import static br.eti.ranieri.opcoesweb.estado.Acao.VALE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;

import br.eti.ranieri.opcoesweb.estado.Acao;
import br.eti.ranieri.opcoesweb.estado.CotacaoAcaoOpcoes;
import br.eti.ranieri.opcoesweb.persistencia.Persistencia;

public class ExibirOfflinePage extends PaginaBase {

	@SpringBean
	private Persistencia persistencia;

	private Integer anoSelecionado;
	private Integer mesSelecionado;
	private Integer diaSelecionado;

	public ExibirOfflinePage() {
		this(null, null, null);
	}

	public ExibirOfflinePage(Integer ano, Integer mes, Integer dia) {
		this.anoSelecionado = ano;
		this.mesSelecionado = mes;
		this.diaSelecionado = dia;

		add(new FeedbackPanel("feedback"));

		List<Integer> anos = persistencia.getAnos();
		add(new ListView<Integer>("yearLoop", anos) {

			@Override
			protected void populateItem(final ListItem<Integer> item) {
				Link link = new Link("link") {

					@Override
					public void onClick() {
						anoSelecionado = item.getModelObject();
						mesSelecionado = diaSelecionado = null;
						setResponsePage(new ExibirOfflinePage(anoSelecionado,
								mesSelecionado, diaSelecionado));
					}
				};
				link.add(new Label("label", String.format("%d", item
						.getModelObject())));
				if (item.getModelObject().equals(anoSelecionado)) {
					item.add(new AttributeModifier("class", true, new Model(
							"selecionado")));
				}
				item.add(link);
			}

		});

		List<Integer> meses = new ArrayList<Integer>();
		if (anoSelecionado != null) {
			meses = persistencia.getMeses(anoSelecionado);
		}
		ListView<Integer> monthLoop = new ListView<Integer>("monthLoop", meses) {
			@Override
			protected void populateItem(final ListItem<Integer> item) {
				Link link = new Link("link") {

					@Override
					public void onClick() {
						mesSelecionado = item.getModelObject();
						diaSelecionado = null;
						setResponsePage(new ExibirOfflinePage(anoSelecionado,
								mesSelecionado, diaSelecionado));
					}
				};
				String mes3Letras = DateTimeFormat.forPattern("MMM")
						.withLocale(new Locale("pt", "BR")).print(
								new DateTime().withMonthOfYear(item
										.getModelObject()));
				link.add(new Label("label", mes3Letras));
				if (item.getModelObject().equals(mesSelecionado)) {
					item.add(new AttributeModifier("class", true, new Model(
							"selecionado")));
				}
				item.add(link);
			}
		};
		monthLoop.setVisible(anoSelecionado != null);
		add(monthLoop);

		List<Integer> dias = new ArrayList<Integer>();
		if (anoSelecionado != null && mesSelecionado != null) {
			dias = persistencia.getDias(anoSelecionado, mesSelecionado);
		}
		ListView<Integer> dayLoop = new ListView<Integer>("dayLoop", dias) {
			@Override
			protected void populateItem(final ListItem<Integer> item) {
				Link link = new Link("link") {

					@Override
					public void onClick() {
						diaSelecionado = item.getModelObject();
						setResponsePage(new ExibirOfflinePage(anoSelecionado,
								mesSelecionado, diaSelecionado));
					}
				};
				link.add(new Label("label", String.format("%02d", item
						.getModelObject())));
				if (item.getModelObject().equals(diaSelecionado)) {
					item.add(new AttributeModifier("class", true, new Model(
							"selecionado")));
				}
				item.add(link);
			}
		};
		dayLoop.setVisible(anoSelecionado != null && mesSelecionado != null);
		add(dayLoop);

		Map<Acao, CotacaoAcaoOpcoes> historico;
		LocalDate atualizacao = null;
		if (anoSelecionado == null || mesSelecionado == null
				|| diaSelecionado == null) {
			historico = new HashMap<Acao, CotacaoAcaoOpcoes>();
		} else {
			historico = persistencia.obterPorData(anoSelecionado,
					mesSelecionado, diaSelecionado);
			atualizacao = new LocalDate(anoSelecionado, mesSelecionado, diaSelecionado);
		}
		addOrReplace(new PainelAcaoOpcoes("tabelaPetrobras", historico.get(PETROBRAS), atualizacao));
		addOrReplace(new PainelAcaoOpcoes("tabelaVale", historico.get(VALE), atualizacao));
	}

}
