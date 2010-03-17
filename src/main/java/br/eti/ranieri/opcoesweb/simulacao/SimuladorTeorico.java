package br.eti.ranieri.opcoesweb.simulacao;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import br.eti.ranieri.opcoesweb.blackscholes.BlackScholes;
import br.eti.ranieri.opcoesweb.estado.CotacaoAcaoOpcoes;
import br.eti.ranieri.opcoesweb.estado.CotacaoOpcao;
import br.eti.ranieri.opcoesweb.estado.Serie;
import br.eti.ranieri.opcoesweb.estado.Variavel;
import br.eti.ranieri.opcoesweb.persistencia.Persistencia;

@Service
public class SimuladorTeorico {

	@Autowired
	private Persistencia persistencia;
	@Autowired
	private BlackScholes blackScholes;

	public void simular(ConfigSimulacao config,
			List<EventoQueAlteraPreco> eventos) {

		for (EventoQueAlteraPreco e : eventos) {
			System.out.println(e.getDataFormatada() + ":" + e.isSelecionado());
		}
		List<Entry<LocalDate, CotacaoAcaoOpcoes>> cotacoes = persistencia
				.getCotacoes(config.acao, config.dataInicial, config.dataFinal);
		// Datas de exercicio das opções
		Map<Serie, LocalDate> vencimentos = Serie.getVencimentosPorPeriodo(
				config.dataInicial, config.dataFinal);

		CarteiraTeorica melhorCarteira = new CarteiraTeorica(config, new HistoricoOperacional());
		Random random = new Random();
		
		int ciclosCompletos = 0, ciclosTotais = 0;
		long agora = System.currentTimeMillis();
		long depois = agora + 1000 * config.tempoMaximoSimulacao;
//		for (int i = 0; i < 3; i++) {
		while (System.currentTimeMillis() < depois) {

			HistoricoOperacional historico = new HistoricoOperacional();
			CarteiraTeorica carteira = new CarteiraTeorica(config, historico);
			CotacaoAcaoOpcoes cotacaoHoje, cotacaoOntem = null;
			ciclosTotais++;

			boolean simulacaoAbortada = false;
			for (Entry<LocalDate,CotacaoAcaoOpcoes> entry : cotacoes) {
				LocalDate data = entry.getKey();
				cotacaoHoje = entry.getValue();
				Double precoAcao = cotacaoHoje.getCotacaoAcao().getPrecoAcao();
				
				LocalDate dataVencimentoOpcaoVendida = null;
				if (carteira.opcaoVendida != null)
					dataVencimentoOpcaoVendida = vencimentos.get(carteira.opcaoVendida.getSerie());
				
				int acao = random.nextInt(3);
				// acao == 0: não faz nada ou é exercido e fica fora
				// acao == 1: rola nesta série ou é exercido e lança a próxima
				// acao == 2: rola na próxima série
				if (acao == 0) {
					// o vencimento é hoje ou já passou (por conta de feriado)
					if (dataVencimentoOpcaoVendida != null && dataVencimentoOpcaoVendida.isAfter(data) == false) {
						carteira.serExercida(data, precoAcao);
						historico.registrarExercicio(data, cotacaoHoje.getCotacaoAcao(), carteira.opcaoVendida);
					}
					
				} else if (acao == 1) {
					// o vencimento é hoje ou já passou (por conta de feriado)
					if (dataVencimentoOpcaoVendida != null && dataVencimentoOpcaoVendida.isAfter(data) == false) {
						carteira.serExercida(data, precoAcao);
						historico.registrarExercicio(data, cotacaoHoje.getCotacaoAcao(), carteira.opcaoVendida);
						// opcao teorica para onde se rolará
						CotacaoOpcao teoricaNova = localizarOpcaoTeorica(cotacaoHoje.getOpcoesSerie2());
						double precoTeoricaVelha = calcularPrecoTeoricoAtual(precoAcao, teoricaNova, carteira.opcaoVendida);
						CotacaoOpcao teoricaVelha = carteira.opcaoVendida;
						if (carteira.rolar(precoAcao, teoricaNova, precoTeoricaVelha) == false) {
							simulacaoAbortada = true;
						}
						historico.registrarRolamento(data, teoricaNova, teoricaVelha, precoTeoricaVelha, -1);
					} else {
						// opcao teorica para onde se rolará
						CotacaoOpcao teoricaNova = localizarOpcaoTeorica(cotacaoHoje.getOpcoesSerie1());
						double precoTeoricaVelha = calcularPrecoTeoricoAtual(precoAcao, teoricaNova, carteira.opcaoVendida);
						CotacaoOpcao teoricaVelha = carteira.opcaoVendida;
						if (carteira.rolar(precoAcao, teoricaNova, precoTeoricaVelha) == false) {
							simulacaoAbortada = true;
						}
						historico.registrarRolamento(data, teoricaNova, teoricaVelha, precoTeoricaVelha, -1);
					}
					
				} else if (acao == 2) {
					// opcao teorica para onde se rolará
					CotacaoOpcao teoricaNova = localizarOpcaoTeorica(cotacaoHoje.getOpcoesSerie2());
					double precoTeoricaVelha = calcularPrecoTeoricoAtual(precoAcao, teoricaNova, carteira.opcaoVendida);
					CotacaoOpcao teoricaVelha = carteira.opcaoVendida;
					if (carteira.rolar(precoAcao, teoricaNova, precoTeoricaVelha) == false) {
						simulacaoAbortada = true;
					}
					historico.registrarRolamento(data, teoricaNova, teoricaVelha, precoTeoricaVelha, -1);
					
				} else {
					throw new IllegalStateException("acao nao programada");
				}
				
				cotacaoOntem = cotacaoHoje;
				
				if (simulacaoAbortada)
					break;
			}
			if (simulacaoAbortada == false) {
				melhorCarteira = escolherMelhorCarteira(melhorCarteira, carteira, cotacaoOntem);
				ciclosCompletos++;
			}
		}
		
		// Exibe as informações da melhor carteira simulada
		String serie = "";
		double precoOpcao = 0.0;
		if (melhorCarteira.opcaoVendida != null) {
			serie = melhorCarteira.opcaoVendida.getSerie().getDescricao();
			precoOpcao = melhorCarteira.opcaoVendida.getVariaveis().get(Variavel.PRECO_REAL).doubleValue();
		}
		System.out
				.printf(
						"Melhor carteira tem:\n" +
						"\t%d ações\n" +
						"\t%d opções da %s vendida a %.2f\n" +
						"\tsaldo = %.2f\n" +
						"\tTOTAL = %.2f\n" +
						"\tCarteiras bem sucedidas %d / Total de simulações %d\n",
						melhorCarteira.custodiaAcoes,
						melhorCarteira.quantidadeOpcaoVendida,
						serie, precoOpcao,
						melhorCarteira.saldo,
						melhorCarteira.cacheValorTotal,
						ciclosCompletos, ciclosTotais);
		System.out.println("-----------------------");
		System.out.println(melhorCarteira.historico);
		System.out.println("=======================");
		
	}

	protected CotacaoOpcao localizarOpcaoTeorica(List<CotacaoOpcao> opcoes) {
		for (CotacaoOpcao opcao : opcoes) {
			if (opcao.getVariaveis().get(Variavel.VOLUME).doubleValue() == 0.0)
				return opcao;
		}
		throw new IllegalStateException("Nao encontrei opcao teorica dentre as " + opcoes);
	}

	protected CotacaoOpcao localizarOpcaoPorCodigoSerie(List<CotacaoOpcao> opcoes1, List<CotacaoOpcao> opcoes2, String codigo, Serie serie) {
		for (CotacaoOpcao opcao : opcoes1) {
			if (opcao.getCodigo().equals(codigo) && opcao.getSerie().equals(serie))
				return opcao;
		}
		for (CotacaoOpcao opcao : opcoes2) {
			if (opcao.getCodigo().equals(codigo) && opcao.getSerie().equals(serie))
				return opcao;
		}
		throw new IllegalStateException("Nao foi possivel encontrar opcao [" + codigo + "] da serie " + serie);
	}

	protected double calcularPrecoTeoricoAtual(double precoAcao, CotacaoOpcao teoricaNova, CotacaoOpcao teoricaAntiga) {
		if (teoricaAntiga == null)
			return 0.0;
		Assert.notNull(teoricaNova);
		double strike = teoricaAntiga.getVariaveis().get(Variavel.STRIKE).doubleValue();
		double tempoParaExpirar = teoricaNova.getVariaveis().get(Variavel.FRACAO_ANO_ATE_EXPIRAR).doubleValue();
		double juros = teoricaNova.getVariaveis().get(Variavel.JUROS).doubleValue();
		double volatilidade = teoricaNova.getVariaveis().get(Variavel.VOLATILIDADE).doubleValue();
		return blackScholes.calcularPrecoOpcaoTeorica(strike, tempoParaExpirar, precoAcao, juros, volatilidade);
	}

	protected CarteiraTeorica escolherMelhorCarteira(CarteiraTeorica carteira1, CarteiraTeorica carteira2, CotacaoAcaoOpcoes ultimaCotacao) {
		
		double precoOpcaoTeorica1 = 0, precoOpcaoTeorica2 = 0;
		double precoAcao = ultimaCotacao.getCotacaoAcao().getPrecoAcao();

		if (carteira1.opcaoVendida != null)
			precoOpcaoTeorica1 = calcularPrecoTeoricoAtual(
					precoAcao,
					localizarOpcaoPorCodigoSerie(
							ultimaCotacao.getOpcoesSerie1(),
							ultimaCotacao.getOpcoesSerie2(),
							carteira1.opcaoVendida.getCodigo(),
							carteira1.opcaoVendida.getSerie()),
					carteira1.opcaoVendida);
		if (carteira2.opcaoVendida != null)
			precoOpcaoTeorica2 = calcularPrecoTeoricoAtual(
					precoAcao,
					localizarOpcaoPorCodigoSerie(
							ultimaCotacao.getOpcoesSerie1(),
							ultimaCotacao.getOpcoesSerie2(),
							carteira2.opcaoVendida.getCodigo(),
							carteira2.opcaoVendida.getSerie()),
					carteira1.opcaoVendida);
		
		double valor1 = carteira1.calcularValor(precoAcao, precoOpcaoTeorica1);
		double valor2 = carteira2.calcularValor(precoAcao, precoOpcaoTeorica2);
		return (valor1 > valor2) ? carteira1 : carteira2;
	}

}
