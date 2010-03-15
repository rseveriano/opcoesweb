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

package br.eti.ranieri.opcoesweb.importacao.offline;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.eti.ranieri.opcoesweb.blackscholes.BlackScholes;
import br.eti.ranieri.opcoesweb.estado.Acao;
import br.eti.ranieri.opcoesweb.estado.ConfiguracaoImportacao;
import br.eti.ranieri.opcoesweb.estado.CotacaoAcaoOpcoes;
import br.eti.ranieri.opcoesweb.estado.Serie;
import br.eti.ranieri.opcoesweb.importacao.TaxaSelic;
import br.eti.ranieri.opcoesweb.importacao.offline.parser.BovespaParser;
import br.eti.ranieri.opcoesweb.persistencia.Persistencia;

/**
 * 
 * @author ranieri
 */
@Service
public class ImportadorOffline {

	private final Logger log = LoggerFactory.getLogger(getClass());
	@Autowired
	private BovespaParser parser;
	@Autowired
	private BlackScholes blackScholes;
	@Autowired
	private Persistencia persistencia;
	@Autowired
	private TaxaSelic taxaSelic;

	public void importar(URL url, ConfiguracaoImportacao configuracaoImportacao) throws Exception {

		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
			throw new Exception("Requisição HTTP retornou código de erro: " + connection.getResponseCode());
		}
		
		List<CotacaoBDI> cotacoes = null;
		ZipInputStream zip = null;
		try {
			zip = new ZipInputStream(connection.getInputStream());
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				if ("BDIN".equals(entry.getName())) {
					cotacoes = parser.parseBDI(zip);
					zip.closeEntry();
					break;
				} else if (entry.getName().startsWith("COTAHIST_")
						&& entry.getName().endsWith(".TXT")) {
					cotacoes = parser.parseHistorico(zip);
					zip.closeEntry();
					break;
				}
			}
			zip.close();
		} catch (ZipException e) {
			log.error("Formato invalido de arquivo zip", e);
		} catch (IOException e) {
			log.error("Erro de leitura do arquivo zip", e);
		} finally {
			if (zip != null)
				IOUtils.closeQuietly(zip);
		}

		calcularBlackScholes(cotacoes, configuracaoImportacao);
	}

	private void calcularBlackScholes(List<CotacaoBDI> cotacoes,
			ConfiguracaoImportacao configuracaoImportacao) throws Exception {
		if (cotacoes == null)
			return;

		// Organiza as cotacoes por data e acao. As cotacoes da
		// acao e das opcoes ficam, por enquanto, na mesma lista
		SortedMap<LocalDate, Map<Acao, List<CotacaoBDI>>> diaAcaoOpcoes = new TreeMap<LocalDate, Map<Acao, List<CotacaoBDI>>>();
		for (CotacaoBDI cotacao : cotacoes) {
			LocalDate data = cotacao.getDataPregao();

			Map<Acao, List<CotacaoBDI>> cotacoesPorAcao = new HashMap<Acao, List<CotacaoBDI>>();
			if (diaAcaoOpcoes.containsKey(data)) {
				cotacoesPorAcao = diaAcaoOpcoes.get(data);
			} else {
				diaAcaoOpcoes.put(data, cotacoesPorAcao);
			}

			Acao acao = null;
			if (cotacao.getCodigoNegociacao().startsWith("PETR")) {
				acao = Acao.PETROBRAS;
			} else if (cotacao.getCodigoNegociacao().startsWith("VALE")) {
				acao = Acao.VALE;
			} else {
				log.error("Codigo de negociacao [{}] nao esta "
						+ "vinculada a VALE e nem a PETROBRAS.", cotacao
						.getCodigoNegociacao());
				continue;
			}

			List<CotacaoBDI> cotacoesAcaoOpcoes = new ArrayList<CotacaoBDI>();
			if (cotacoesPorAcao.containsKey(acao)) {
				cotacoesAcaoOpcoes = cotacoesPorAcao.get(acao);
			} else {
				cotacoesPorAcao.put(acao, cotacoesAcaoOpcoes);
			}

			cotacoesAcaoOpcoes.add(cotacao);
		}

		// Agora separa, para cada dia e para cada acao, as
		// cotacoes da acao, das opcoes que vencem este mes
		// e das opcoes que vencem no proximo mes.
		// 
		// Para cada dia e para cada acao, calcula o Black&Scholes
		// em cada dupla acao e lista de opcoes
		for (LocalDate data : diaAcaoOpcoes.keySet()) {

			Serie serieAtualOpcoes = Serie.getSerieAtualPorData(data);
			Serie proximaSerieOpcoes = Serie.getProximaSeriePorData(data);
			Double selic = taxaSelic.getSelic(data);

			for (Acao acao : diaAcaoOpcoes.get(data).keySet()) {

				CotacaoBDI cotacaoAcao = null;
				List<CotacaoBDI> cotacoesOpcoesSerie1 = new ArrayList<CotacaoBDI>();
				List<CotacaoBDI> cotacoesOpcoesSerie2 = new ArrayList<CotacaoBDI>();

				for (CotacaoBDI cotacao : diaAcaoOpcoes.get(data).get(acao)) {
					if (CodigoBDI.LOTE_PADRAO.equals(cotacao.getCodigoBdi())
							&& TipoMercadoBDI.MERCADO_A_VISTA.equals(cotacao
									.getTipoMercado())) {
						if (cotacaoAcao != null)
							log.error("Sobrescreveu cotacao [{}] com [{}].",
									cotacaoAcao, cotacao);
						cotacaoAcao = cotacao;
					} else if (CodigoBDI.OPCOES_DE_COMPRA.equals(cotacao
							.getCodigoBdi())
							&& TipoMercadoBDI.OPCOES_DE_COMPRA.equals(cotacao
									.getTipoMercado())) {
						if (serieAtualOpcoes.isSerieDaOpcao(cotacao
								.getCodigoNegociacao())) {
							cotacoesOpcoesSerie1.add(cotacao);
						} else if (proximaSerieOpcoes.isSerieDaOpcao(cotacao
								.getCodigoNegociacao())) {
							cotacoesOpcoesSerie2.add(cotacao);
						}
					}
				}

				if (cotacaoAcao == null) {
					log.error("Nao foi encontrada cotacao de "
							+ "acao [{}] no dia [{}].", acao.getCodigo(), data);
					continue;
				}
				if (cotacoesOpcoesSerie1.size() == 0) {
					log.error("Nao foram encontradas cotacoes de opcoes "
							+ "de [{}] no dia [{}] para vencer neste mes.",
							acao.getCodigo(), data);
					continue;
				}
				if (cotacoesOpcoesSerie2.size() == 0) {
					log.error("Nao foram encontradas cotacoes de opcoes "
							+ "de [{}] no dia [{}] para vencer proximo mes.",
							acao.getCodigo(), data);
					continue;
				}

				CotacaoBDI opcaoTeorica1 = new CotacaoBDI(data, //
						CodigoBDI.OPCOES_DE_COMPRA, //
						TipoMercadoBDI.OPCOES_DE_COMPRA, //
						"Teorica", 0, 0, 0, //
						cotacaoAcao.getFechamento(), //
						cotacoesOpcoesSerie1.iterator().next()
								.getDataVencimento());

				CotacaoBDI opcaoTeorica2 = new CotacaoBDI(data, //
						CodigoBDI.OPCOES_DE_COMPRA, //
						TipoMercadoBDI.OPCOES_DE_COMPRA, //
						"Teorica", 0, 0, 0, //
						cotacaoAcao.getFechamento(), //
						cotacoesOpcoesSerie2.iterator().next()
								.getDataVencimento());

				Integer opcoesPorDia = configuracaoImportacao
						.getQuantidadeOpcoesPorAcaoPorDia();

				CotacaoAcaoOpcoes cotacao = blackScholes.calcularIndices(
						cotacaoAcao, serieAtualOpcoes, cotacoesOpcoesSerie1,
						opcaoTeorica1, proximaSerieOpcoes,
						cotacoesOpcoesSerie2, opcaoTeorica2, opcoesPorDia,
						selic);

				persistencia.incluirCotacaoHistorica(data, acao, cotacao);
			}
		}
	}
}
