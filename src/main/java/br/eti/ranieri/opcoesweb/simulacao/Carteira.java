package br.eti.ranieri.opcoesweb.simulacao;

import static br.eti.ranieri.opcoesweb.estado.Variavel.PRECO_REAL;
import static br.eti.ranieri.opcoesweb.estado.Variavel.STRIKE;
import static br.eti.ranieri.opcoesweb.estado.Variavel.TAXA_VE;
import static br.eti.ranieri.opcoesweb.estado.Variavel.THE_VE;
import static br.eti.ranieri.opcoesweb.estado.Variavel.VOLUME;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.eti.ranieri.opcoesweb.estado.CotacaoAcao;
import br.eti.ranieri.opcoesweb.estado.CotacaoAcaoOpcoes;
import br.eti.ranieri.opcoesweb.estado.CotacaoOpcao;
import br.eti.ranieri.opcoesweb.estado.Serie;
import br.eti.ranieri.opcoesweb.estado.Variavel;

class Carteira {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	Integer custodiaAcoes;
	double saldo;
	CotacaoOpcao opcaoVendida;
	int quantidadeOpcaoVendida;
	ConfigSimulacao config;

	public Carteira(ConfigSimulacao config) {
		this.config = config;
		this.custodiaAcoes = config.custodiaInicial;
	}

	CotacaoOpcao encontrarMelhorOpcao(List<CotacaoOpcao> opcoes, final boolean maiorTaxaVE, final boolean menorExercicioQuandoEmpatar) {

		CotacaoOpcao melhorOpcao = Collections.max(opcoes,
				new Comparator<CotacaoOpcao>() {
			public int compare(CotacaoOpcao o1, CotacaoOpcao o2) {
				// Coloca as opcoes Teorica's como as piores
				long vol1 = o1.getVariaveis().get(VOLUME).longValue();
				long vol2 = o2.getVariaveis().get(VOLUME).longValue();
				if (vol1 == 0L) {
					return (vol2 == 0L) ? 0 : -1;
				} else if (vol2 == 0L) {
					return 1;
				}
				
				if (maiorTaxaVE) {
					int compareTaxaVE = Double.compare(o1.getVariaveis()
							.get(TAXA_VE).doubleValue(), o2
							.getVariaveis().get(TAXA_VE).doubleValue());
					if (compareTaxaVE == 0) {
						// se empatou na TaxaVE, pode
						// pegar a de menor exercicio
						return (menorExercicioQuandoEmpatar ? -1 : 1)
								* Double.compare(o1.getVariaveis().get(
										STRIKE).doubleValue(), o2
										.getVariaveis().get(STRIKE)
										.doubleValue());
					}
					return compareTaxaVE;
				}
				// se nao comparar pela maior taxa de valor extrinsico,
				// compara pela maior taxas de decaimento, mas ignora
				// seu sinal, afinal TheVE pode ser negativo para informar
				// ao usuario que nao ha VE suficiente para assumir o risco
				int compareTheVE = Double.compare(Math.abs(o1
						.getVariaveis().get(THE_VE).doubleValue()),
						Math.abs(o2.getVariaveis().get(THE_VE)
								.doubleValue()));
				if (compareTheVE == 0) {
					// se empatou na TheVE, pode
					// pegar a de menor exercicio
					return (menorExercicioQuandoEmpatar ? -1 : 1)
							* Double.compare(o1.getVariaveis().get(
									STRIKE).doubleValue(), o2
									.getVariaveis().get(STRIKE)
									.doubleValue());
				}
				return compareTheVE;
			}
		});
		return melhorOpcao;
	}

	static CotacaoOpcao encontrarPeloCodigo(Carteira carteira, List<CotacaoOpcao> opcoes1, List<CotacaoOpcao> opcoes2, Double precoAcao, List<CotacaoOpcao> opcoesOntem) {
		if (carteira.opcaoVendida == null)
			throw new IllegalStateException("So encontro opcao que foi vendida");
		Serie serie1 = null, serie2 = null;
		for (CotacaoOpcao opcao : opcoes1) {
			serie1 = opcao.getSerie();
			if (opcao.getCodigo().equals(carteira.opcaoVendida.getCodigo()))
				return opcao;
		}
		for (CotacaoOpcao opcao : opcoes2) {
			serie2 = opcao.getSerie();
			if (opcao.getCodigo().equals(carteira.opcaoVendida.getCodigo()))
				return opcao;
		}
		if (carteira.opcaoVendida.getSerie().equals(serie1) || carteira.opcaoVendida.getSerie().equals(serie2))
			return null;
		// Tecnica do desespero: ja que nao achei a opcao nas negociacoes
		// de hoje, tento acha-la nas negociacoes de ontem e crio uma cotacao
		// virtual pois, muito provavelmente, a opcao esta sendo exercida.
		if (opcoesOntem != null) {
			for (CotacaoOpcao opcao : opcoesOntem) {
				serie1 = opcao.getSerie();
				if (opcao.getCodigo().equals(carteira.opcaoVendida.getCodigo())) {
					Map<Variavel, Number> variaveis = new HashMap<Variavel, Number>(opcao.getVariaveis());
					// sobrescreve as variaveis com valores que
					// fazem sentido no dia do exercicio
					double strike = variaveis.get(Variavel.STRIKE).doubleValue();
					variaveis.put(Variavel.VALOR_EXTRINSICO, new Double(0));
					if (precoAcao != null)
						variaveis.put(Variavel.PRECO_REAL, new Double(Math.max(0.01, precoAcao - strike)));
					return new CotacaoOpcao(opcao.getSerie(), opcao.getCodigo(), variaveis);
				}
			}
		}
		
		throw new IllegalStateException("Opcao vendida foi exercida");
	}

	public boolean rolar(LocalDate data, CotacaoAcao acao, List<CotacaoOpcao> opcoes1, List<CotacaoOpcao> opcoes2, boolean maiorTaxaVE, boolean menorExercicioQuandoEmpatar, HistoricoOperacional historico, List<CotacaoOpcao> opcoesOntem) {

		CotacaoOpcao melhorOpcao = encontrarMelhorOpcao(opcoes1, maiorTaxaVE, menorExercicioQuandoEmpatar);

		if (opcaoVendida == null) {
			// Primeira venda coberta
			opcaoVendida = melhorOpcao;
			quantidadeOpcaoVendida = (custodiaAcoes/100)*100;

			// Valor da venda das opcoes
			double valorOpcao = opcaoVendida.getVariaveis().get(PRECO_REAL).doubleValue();
			double vendaLiquida = valorOpcao * quantidadeOpcaoVendida - config.corretagemOpcoes;

			saldo += vendaLiquida;

			// Quantidade estimada de compra no fracionario
			int compraFracionario = (int) Math.max(0, Math.floor(saldo / acao.getPrecoAcao()));
			// Corretagem para a compra estimada
			double corretagemFracionario = config.corretagemFracionario * Math.ceil(compraFracionario / 99.0);
			// Reduz a quantidade comprada no fracionario
			// para o saldo nao ficar negativo
			while(compraFracionario > 0 && saldo - compraFracionario*acao.getPrecoAcao() - corretagemFracionario < 0) {
				compraFracionario--;
				corretagemFracionario = config.corretagemFracionario * Math.ceil(compraFracionario / 99.0);
			}
			// Se, depois das reducoes, sobrou vontade de comprar
			if (compraFracionario > 0) {
				custodiaAcoes += compraFracionario;
				saldo = saldo - compraFracionario*acao.getPrecoAcao() - corretagemFracionario;
			}

			historico.registrarLancamento(data, opcaoVendida, quantidadeOpcaoVendida, compraFracionario);

		} else {
			if (melhorOpcao.getCodigo().equals(opcaoVendida.getCodigo())) {
				// Pediu para rolar para a mesma opcao em que estou vendido
				return false;
			}
			// /////////////////////////////////////////
			// COMPRA DA OPCAO LANCADA
			//
			CotacaoOpcao opcaoLancadaAtualizada = null;
			try {
				opcaoLancadaAtualizada = encontrarPeloCodigo(this, opcoes1, opcoes2, acao.getPrecoAcao(), opcoesOntem);
			} catch (IllegalStateException e) {
				logger.warn("Nao foi encontrada uma opcao compativel: {}", e.getMessage());
				return true;
			}
			if (opcaoLancadaAtualizada == null)
				return false;
			saldo -= opcaoLancadaAtualizada.getVariaveis().get(PRECO_REAL).doubleValue() * quantidadeOpcaoVendida + config.corretagemOpcoes;

			// /////////////////////////////////////////
			// VENDA DA NOVA OPCAO
			//
			opcaoVendida = melhorOpcao;
			quantidadeOpcaoVendida = (custodiaAcoes / 100) * 100;
			
			double valorOpcao = melhorOpcao.getVariaveis().get(PRECO_REAL).doubleValue();
			double vendaLiquida = valorOpcao * quantidadeOpcaoVendida - config.corretagemOpcoes;
			
			saldo += vendaLiquida;
			
			// /////////////////////////////////////////
			// COMPRA NO FRACIONARIO
			//
			int compraFracionario = (int) Math.max(0, Math.floor(saldo / acao.getPrecoAcao()));
			double corretagemFracionario = config.corretagemFracionario * Math.ceil(compraFracionario / 99.0);
			while(compraFracionario > 0 && saldo - compraFracionario*acao.getPrecoAcao() - corretagemFracionario < 0) {
				compraFracionario--;
				corretagemFracionario = config.corretagemFracionario * Math.ceil(compraFracionario / 99.0);
			}
			if (compraFracionario > 0) {
				custodiaAcoes += compraFracionario;
				saldo = saldo - compraFracionario*acao.getPrecoAcao() - corretagemFracionario;
			}

			historico.registrarRolamento(data, melhorOpcao, opcaoLancadaAtualizada, opcaoLancadaAtualizada.getVariaveis().get(Variavel.PRECO_REAL).doubleValue(), compraFracionario);
		}

		return saldo < config.prejuizoMaximo;
	}

	static double calcularValorTotal(Carteira carteira, CotacaoAcaoOpcoes ultimaCotacao) {
		double valorAcoes = carteira.custodiaAcoes * ultimaCotacao.getCotacaoAcao().getPrecoAcao();
		double valorOpcoes = 0;
		CotacaoOpcao opcao = null;
		try {
			opcao = encontrarPeloCodigo(carteira, ultimaCotacao.getOpcoesSerie1(), ultimaCotacao.getOpcoesSerie2(), null, null);
		} catch (IllegalStateException e) {
			if (carteira.opcaoVendida != null) {
				return Double.NEGATIVE_INFINITY;
			}
		}
		if (opcao != null)
			valorOpcoes = opcao.getVariaveis().get(PRECO_REAL).doubleValue() * carteira.quantidadeOpcaoVendida + carteira.config.corretagemOpcoes;
		return carteira.saldo + valorAcoes - valorOpcoes;
	}

	public boolean melhorQue(Carteira outraCarteira, CotacaoAcaoOpcoes ultimaCotacao) {
		return Double.compare(calcularValorTotal(this, ultimaCotacao), calcularValorTotal(outraCarteira, ultimaCotacao)) == 1;
	}

	public void serExercida(LocalDate data, CotacaoAcao acao, List<CotacaoOpcao> opcoes1, List<CotacaoOpcao> opcoes2, HistoricoOperacional historico, List<CotacaoOpcao> opcoesOntem) {
		// Nao devo nada a ninguem
		if (opcaoVendida == null)
			return;
		// Minha opcao vence mes que vem
		if (opcaoVendida.getSerie().ordinal() == data.getMonthOfYear() % 12)
			return;
		
		// No exercicio, as acoes sao vendidas pelo Strike da opcao se ela
		// estiver ITM ou ATM, opcoes sao zeradas e as acoes sao recompradas
		// caso tenham sido exercidas.
		CotacaoOpcao opcao = encontrarPeloCodigo(this, opcoes1, opcoes2, acao.getPrecoAcao(), opcoesOntem);
		if (opcao == null)
			opcao = opcaoVendida;
		double strike = opcao.getVariaveis().get(STRIKE).doubleValue();
		if (acao.getPrecoAcao() >= strike) {
			custodiaAcoes -= quantidadeOpcaoVendida;
			saldo += quantidadeOpcaoVendida * strike - config.corretagemIntegral;
			// recompra as acoes no integral
			int recompraIntegral = ((int) Math.floor(saldo / acao.getPrecoAcao())) / 100;
			if (recompraIntegral > 0) {
				custodiaAcoes += recompraIntegral * 100;
				saldo -= recompraIntegral * 100 * acao.getPrecoAcao() + config.corretagemIntegral;
			}
			// recompra as acoes no fracionario
			int recompraFracionario = (int) Math.max(0, Math.floor(saldo / acao.getPrecoAcao()));
			while (recompraFracionario > 0 && saldo - recompraFracionario * acao.getPrecoAcao() - config.corretagemFracionario < 0) {
				recompraFracionario--;
			}
			if (recompraFracionario > 0) {
				custodiaAcoes += recompraFracionario;
				saldo -= recompraFracionario * acao.getPrecoAcao() + config.corretagemFracionario;
			}
		}
		opcaoVendida = null;
		quantidadeOpcaoVendida = 0;

		historico.registrarExercicio(data, acao, opcao);
	}
}
