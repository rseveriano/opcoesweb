/*
 *  Copyright 2009 ranieri.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package br.eti.ranieri.opcoesweb.page;

import static br.eti.ranieri.opcoesweb.estado.Variavel.ATM;
import static br.eti.ranieri.opcoesweb.estado.Variavel.BOSI;
import static br.eti.ranieri.opcoesweb.estado.Variavel.DELTA;
import static br.eti.ranieri.opcoesweb.estado.Variavel.GAMA;
import static br.eti.ranieri.opcoesweb.estado.Variavel.JUROS;
import static br.eti.ranieri.opcoesweb.estado.Variavel.NAO_VENDE;
import static br.eti.ranieri.opcoesweb.estado.Variavel.VDX;
import static br.eti.ranieri.opcoesweb.estado.Variavel.PRECO_REAL;
import static br.eti.ranieri.opcoesweb.estado.Variavel.PRECO_TEORICO;
import static br.eti.ranieri.opcoesweb.estado.Variavel.STRIKE;
import static br.eti.ranieri.opcoesweb.estado.Variavel.TAXA_VE;
import static br.eti.ranieri.opcoesweb.estado.Variavel.TETA;
import static br.eti.ranieri.opcoesweb.estado.Variavel.THE_VE;
import static br.eti.ranieri.opcoesweb.estado.Variavel.VALOR_EXTRINSICO;
import static br.eti.ranieri.opcoesweb.estado.Variavel.VALOR_INTRINSICO;
import static br.eti.ranieri.opcoesweb.estado.Variavel.VOLATILIDADE;
import static br.eti.ranieri.opcoesweb.estado.Variavel.VOLUME;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.util.ListModel;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import br.eti.ranieri.opcoesweb.estado.CotacaoOpcao;
import br.eti.ranieri.opcoesweb.estado.Variavel;

/**
 * 
 * @author ranieri
 */
public class PainelOpcoes extends Panel {

	private static final Locale ptBR = new Locale("pt", "BR");
	private final transient DateTimeFormatter vencimentoFormatter = DateTimeFormat.forPattern("'Vencimento 'dd/MM/yyyy").withLocale(ptBR);

	public PainelOpcoes(String id, List<CotacaoOpcao> opcoes, LocalDate atualizacao) {
		this(id, opcoes, atualizacao, DateTimeFormat.forPattern("'Atualizado em 'dd/MM/yyyy").withLocale(ptBR).print(atualizacao));
	}

	public PainelOpcoes(String id, List<CotacaoOpcao> opcoes, DateTime atualizacao) {
		this(id, opcoes, atualizacao != null ? atualizacao.toLocalDate() : null, DateTimeFormat.forPattern("'Atualizado em 'HH:mm dd/MM/yyyy").withLocale(ptBR).print(atualizacao));
	}

	private PainelOpcoes(String id, List<CotacaoOpcao> opcoes, LocalDate atualizacao, String atualizacaoFormatada) {
		super(id);

		if (opcoes == null) {
			this.setVisible(false);
			opcoes = new ArrayList<CotacaoOpcao>();
		}

		add(new Label("descricaoSerie", (opcoes.size() == 0) ? "" : opcoes
				.iterator().next().getSerie().getDescricao()));
		add(new Label("vencimento", (opcoes.size() == 0) ? ""
				: vencimentoFormatter.print(opcoes.iterator().next().getSerie()
						.getDataVencimento(atualizacao))));

		if (atualizacao == null) {
			add(new Label("atualizacao", "Atualização não conhecida"));
		} else {
			add(new Label("atualizacao", atualizacaoFormatada));
		}

		add(new LineListView("opcaoLoop", listarCodigos(opcoes)));
		add(new LineListView("strikeLoop", opcoes, STRIKE));
		add(new LineListView("jurosLoop", opcoes, JUROS));
		add(new LineListView("sigmaLoop", opcoes, VOLATILIDADE));
		add(new LineListView("teoricoLoop", opcoes, PRECO_TEORICO));
		add(new LineListView("realLoop", opcoes, PRECO_REAL));
		add(new LineListView("volumeLoop", opcoes, VOLUME));
		add(new LineListView("deltaLoop", opcoes, DELTA));
		add(new LineListView("gammaLoop", opcoes, GAMA));
		add(new LineListView("thetaLoop", opcoes, TETA));
		add(new LineListView("viLoop", opcoes, VALOR_INTRINSICO));
		add(new LineListView("veLoop", opcoes, VALOR_EXTRINSICO));
		add(new LineListView("vePercentLoop", opcoes, TAXA_VE));
		add(new LineListView("naoVendeLoop", opcoes, NAO_VENDE));
		add(new LineListView("theveLoop", opcoes, THE_VE));
		add(new LineListView("vdxLoop", opcoes, VDX));
		add(new LineListView("bosiLoop", opcoes, BOSI));
		add(new LineListView("atmLoop", opcoes, ATM));
	}

	private List<String> listarCodigos(List<CotacaoOpcao> opcoes) {
		List<String> codigos = new ArrayList<String>();
		for (CotacaoOpcao opcao : opcoes) {
			codigos.add(opcao.getCodigo());
		}
		return codigos;
	}

//	private List<String> listarVariaveis(List<CotacaoOpcao> opcoes, Variavel variavel) {
//		List<String> variaveis = new ArrayList<String>();
//		for (CotacaoOpcao opcao : opcoes) {
//			variaveis.add(variavel.getFormatador().formatar(opcao.getVariaveis().get(variavel)));
//		}
//		return variaveis;
//	}

	private final class LineListView extends ListView<String> {

		private static final long serialVersionUID = 1089281651634311378L;

		private String maiorValorFormatado;

		public LineListView(String id, List<String> list) {
			super(id, list);
		}
		
		public LineListView(String id, List<CotacaoOpcao> opcoes, Variavel variavel) {
			super(id);

			List<String> valoresFormatados = new ArrayList<String>();
			SummaryStatistics estatistica = new SummaryStatistics();
			for (CotacaoOpcao opcao : opcoes) {
				Number valor = opcao.getVariaveis().get(variavel);
				if (opcao.getVariaveis().get(VOLUME).doubleValue() > 0.0) {
					estatistica.addValue(valor.doubleValue());
				}
				valoresFormatados.add(variavel.getFormatador().formatar(valor));
			}
			maiorValorFormatado = variavel.getFormatador().formatar(estatistica.getMax());

			super.setModel(new ListModel<String>(valoresFormatados));
		}

		@Override
		protected void populateItem(ListItem<String> item) {
			if (StringUtils.equals(maiorValorFormatado, item.getModelObject())) {
				item.add(new SimpleAttributeModifier("class", "maior"));
			}
			item.add(new Label("valor", item.getModelObject()));
		}
	}
}
