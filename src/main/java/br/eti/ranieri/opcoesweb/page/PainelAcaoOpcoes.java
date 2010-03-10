package br.eti.ranieri.opcoesweb.page;

import java.util.List;
import java.util.Locale;

import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import br.eti.ranieri.opcoesweb.estado.CotacaoAcao;
import br.eti.ranieri.opcoesweb.estado.CotacaoAcaoOpcoes;
import br.eti.ranieri.opcoesweb.estado.CotacaoOpcao;
import br.eti.ranieri.opcoesweb.format.FormatadorNumerico;

public class PainelAcaoOpcoes extends Panel {

	private static final Locale LOCALE = new Locale("pt", "BR");

	public PainelAcaoOpcoes(String id, CotacaoAcaoOpcoes cotacao, LocalDate atualizacao) {
		this(id, cotacao, atualizacao, null);
	}

	public PainelAcaoOpcoes(String id, CotacaoAcaoOpcoes cotacao, DateTime atualizacao) {
		this(id, cotacao, null, atualizacao);
	}

	private PainelAcaoOpcoes(String id, CotacaoAcaoOpcoes cotacao, LocalDate atualizacaoLocalDate, DateTime atualizacaoDateTime) {
		super(id);

		String codigoAcao, precoAcao;
		List<CotacaoOpcao> serie1;
		List<CotacaoOpcao> serie2;
		Double variacao = 0.0;

		if (cotacao == null) {
			this.setVisible(false);
			codigoAcao = precoAcao = "";
			serie1 = serie2 = null;
		} else {
			CotacaoAcao cotacaoAcao = cotacao.getCotacaoAcao();
			codigoAcao = cotacaoAcao.getAcao().getCodigo();
			precoAcao = FormatadorNumerico.formatarDinheiro(cotacaoAcao
					.getPrecoAcao());
			variacao = cotacaoAcao.getVariacaoAcao() == null ? 0.0 : cotacaoAcao.getVariacaoAcao();
			variacao *= 100;
			serie1 = cotacao.getOpcoesSerie1();
			serie2 = cotacao.getOpcoesSerie2();
		}

		WebMarkupContainer tarjaAcao = new WebMarkupContainer("tarjaAcao");
		tarjaAcao.add(new Label("codigoAcao", codigoAcao));
		tarjaAcao.add(new Label("precoAcao", precoAcao));
		tarjaAcao.add(new Label("variacaoAcao", String.format(LOCALE, "(%+,.2f%%)", variacao)));

		tarjaAcao
				.add(new SimpleAttributeModifier("class", new String[] {
						"variacaoNegativa", "variacaoNeutra",
						"variacaoPositiva" }[((int) Math.signum(variacao)) + 1]));
		add(tarjaAcao);

		if (atualizacaoLocalDate != null) {
			add(new PainelOpcoes("painelOpcoesSerie1", serie1, atualizacaoLocalDate));
			add(new PainelOpcoes("painelOpcoesSerie2", serie2, atualizacaoLocalDate));
		} else {
			add(new PainelOpcoes("painelOpcoesSerie1", serie1, atualizacaoDateTime));
			add(new PainelOpcoes("painelOpcoesSerie2", serie2, atualizacaoDateTime));
		}
	}
}
