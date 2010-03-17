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
package br.eti.ranieri.opcoesweb.importacao.online;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.eti.ranieri.opcoesweb.blackscholes.BlackScholes;
import br.eti.ranieri.opcoesweb.estado.Acao;
import br.eti.ranieri.opcoesweb.estado.ConfiguracaoOnline;
import br.eti.ranieri.opcoesweb.estado.CotacaoAcaoOpcoes;
import br.eti.ranieri.opcoesweb.estado.CotacaoOpcao;
import br.eti.ranieri.opcoesweb.estado.Serie;
import br.eti.ranieri.opcoesweb.importacao.TaxaSelic;
import br.eti.ranieri.opcoesweb.importacao.offline.CodigoBDI;
import br.eti.ranieri.opcoesweb.importacao.offline.CotacaoBDI;
import br.eti.ranieri.opcoesweb.importacao.offline.TipoMercadoBDI;
import br.eti.ranieri.opcoesweb.persistencia.Persistencia;

import com.google.appengine.api.urlfetch.HTTPHeader;
import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

/**
 * 
 * @author ranieri
 */
@Service("importadorOnline")
public class ImportadorOnlineBanif implements ImportadorOnline {

	private static final String BANIF_URL = "https://www.banifinvest.com.br/tr/bi/cotacoes/cotacoes_ajax_resultado.jsp?busca=";

	private transient final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private XmlBanifParser xmlBanifParser;
	@Autowired
	private Persistencia persistencia;
	@Autowired
	private BlackScholes blackScholes;
	@Autowired
	private TaxaSelic taxaSelic;

	public Map<Acao, CotacaoAcaoOpcoes> importar(ConfiguracaoOnline configuracao) {

		Map<Acao, CotacaoAcaoOpcoes> cotacaoPorAcao = new HashMap<Acao, CotacaoAcaoOpcoes>();

		try {
			// codigos de negociacao das acoes separados por ponto-e-virgulas
			// por exemplo: PETR4;VALE5;TLNP4
			String codigos = StringUtils.join(Lists.transform(Arrays
					.asList(Acao.values()), new Function<Acao, String>() {
				public String apply(Acao acao) {
					return acao.getCodigo();
				}
			}), ";");

			String xml = baixarCotacoes(configuracao, codigos);

			// Transforma XML em Cotacao's de ações apenas
			List<CotacaoBDI> cotacoesAcoes = xmlBanifParser.parse(xml);

			for (Acao acao : Acao.values()) {
				CotacaoBDI ultimaCotacaoAcao = null;
				// Tenta encontrar o preco do ultimo negocio desta acao
				for (CotacaoBDI cotacaoAcaoAgora : cotacoesAcoes) {
					if (acao.getCodigo().equals(
							cotacaoAcaoAgora.getCodigoNegociacao())) {
						ultimaCotacaoAcao = cotacaoAcaoAgora;
						break;
					}
				}
				// Nao encontrei cotacao para esta acao
				if (ultimaCotacaoAcao == null)
					continue;

				CotacaoAcaoOpcoes acaoOpcoes = converterCalcularBlackScholes(
						configuracao, acao, ultimaCotacaoAcao);
				cotacaoPorAcao.put(acao, acaoOpcoes);
			}
		} catch (Exception e) {
			logger.error("Erro na obtencao do XML de acoes do Banif", e);
		}

		return cotacaoPorAcao;
	}

	CotacaoAcaoOpcoes converterCalcularBlackScholes(
			ConfiguracaoOnline configuracao, Acao acao,
			CotacaoBDI ultimaCotacaoAcao) throws Exception {

		LocalDate hoje = new LocalDate();

		Serie serieAtualOpcoes = Serie.getSerieAtualPorData(hoje);
		List<CotacaoBDI> opcoesSerie1 = obterOpcoes(configuracao, acao,
				serieAtualOpcoes, hoje);

		Serie proximaSerieOpcoes = Serie.getProximaSeriePorData(hoje);
		List<CotacaoBDI> opcoesSerie2 = obterOpcoes(configuracao, acao,
				proximaSerieOpcoes, hoje);

		CotacaoBDI opcaoTeorica1 = new CotacaoBDI(hoje,
				CodigoBDI.OPCOES_DE_COMPRA, TipoMercadoBDI.OPCOES_DE_COMPRA,
				"Teórica", 0, 0, 0, ultimaCotacaoAcao.getFechamento(),
				opcoesSerie1.iterator().next().getDataVencimento());
		CotacaoBDI opcaoTeorica2 = new CotacaoBDI(hoje,
				CodigoBDI.OPCOES_DE_COMPRA, TipoMercadoBDI.OPCOES_DE_COMPRA,
				"Teórica", 0, 0, 0, ultimaCotacaoAcao.getFechamento(),
				opcoesSerie2.iterator().next().getDataVencimento());

		Double selic = taxaSelic.getSelic(hoje);

		return blackScholes.calcularIndices(ultimaCotacaoAcao,
				serieAtualOpcoes, opcoesSerie1, opcaoTeorica1,
				proximaSerieOpcoes, opcoesSerie2, opcaoTeorica2, 10, selic);
	}

	List<CotacaoBDI> obterOpcoes(ConfiguracaoOnline configuracao, Acao acao,
			Serie serie, LocalDate agora) throws Exception {
		// Obtem a ultima cotacao historica para
		// saber quais opções tinham maior volume
		Map<Acao, CotacaoAcaoOpcoes> obterUltima = persistencia.obterUltima();
		System.err.println("persistencia.obterUltima() = " + obterUltima);
		CotacaoAcaoOpcoes acaoOpcoes = obterUltima.get(acao);
		// Decide se o parametro Serie se refere a
		// getOpcoesSerie1() ou getOpcoesSerie2()
		Collection<CotacaoOpcao> opcoes;
		if (serie.equals(acaoOpcoes.getOpcoesSerie1().iterator().next().getSerie())) {
			opcoes = acaoOpcoes.getOpcoesSerie1();
		} else {
			opcoes = acaoOpcoes.getOpcoesSerie2();
		}
		opcoes = Collections2.filter(opcoes, new Predicate<CotacaoOpcao>() {
			public boolean apply(CotacaoOpcao opcao) {
				return "Teórica".equals(opcao.getCodigo()) == false;
			}
		});
		// Removida a opção Teórica, baixo as cotações
		// a partir do código das opções remanescentes
		String codigos = StringUtils.join(Lists.transform(
				new ArrayList<CotacaoOpcao>(opcoes),
				new Function<CotacaoOpcao, String>() {
					public String apply(CotacaoOpcao cotacao) {
						return cotacao.getCodigo();
					}
				}), ";");

		String xml = baixarCotacoes(configuracao, codigos);
		// Transforma XML em Cotacao's de opções apenas
		List<CotacaoBDI> cotacoesOpcoes = xmlBanifParser.parse(xml);

		// Define a data de hoje para todas as cotações recem-baixadas
		for (CotacaoBDI cotacao : cotacoesOpcoes) {
			cotacao.setDataPregao(agora);
		}

		return cotacoesOpcoes;
	}

	/**
	 * Baixa as cotações de ações ou opções cujos códigos são separados por ponto-e-vírgula.
	 * 
	 * @return o XML de resposta do Banif ou null, se a resposta não for HTTP 200 OK.
	 */
	String baixarCotacoes(ConfiguracaoOnline configuracao, String codigos)
			throws Exception {

		URLFetchService service = URLFetchServiceFactory.getURLFetchService();
		
		HTTPRequest request = new HTTPRequest(new URL(BANIF_URL + codigos), HTTPMethod.GET);
		request.addHeader(new HTTPHeader("Cookie", "jsessionid=" + configuracao.getJsessionid()));
		
		HTTPResponse response = service.fetch(request);
		if (response.getResponseCode() != HttpURLConnection.HTTP_OK) {
			return null;
		}
		
		String encoding = "ISO-8859-1";
		for (HTTPHeader header : response.getHeaders()) {
			if ("Content-Type".equalsIgnoreCase(header.getName())) {
				if (header.getValue() != null && header.getValue().toLowerCase().contains("CHARSET=UTF-8")) {
					encoding = "UTF-8";
				}
			}
		}
		
		return new String(response.getContent(), encoding);
	}

}
