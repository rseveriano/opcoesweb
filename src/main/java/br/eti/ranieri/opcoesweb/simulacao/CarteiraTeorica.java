package br.eti.ranieri.opcoesweb.simulacao;

import static br.eti.ranieri.opcoesweb.estado.Variavel.PRECO_REAL;
import static br.eti.ranieri.opcoesweb.estado.Variavel.STRIKE;

import java.util.List;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.eti.ranieri.opcoesweb.estado.CotacaoAcao;
import br.eti.ranieri.opcoesweb.estado.CotacaoOpcao;
import br.eti.ranieri.opcoesweb.estado.Variavel;

public class CarteiraTeorica {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	Integer custodiaAcoes;
	double saldo;
	CotacaoOpcao opcaoVendida;
	int quantidadeOpcaoVendida;
	ConfigSimulacao config;
	transient Double cacheValorTotal;
	HistoricoOperacional historico;
	
	public CarteiraTeorica(ConfigSimulacao config, HistoricoOperacional historico) {
		this.config = config;
		this.custodiaAcoes = config.custodiaInicial;
		this.historico = historico;
	}

	public void serExercida(LocalDate data, double precoAcao) {

		// Nao devo nada a ninguem
		if (opcaoVendida == null)
			return;
		// Minha opcao vence mes que vem
		if (opcaoVendida.getSerie().ordinal() == data.getMonthOfYear() % 12)
			return;

		// No exercicio, as acoes sao vendidas pelo Strike da opcao se ela
		// estiver ITM ou ATM, opcoes sao zeradas e as acoes sao recompradas
		// caso tenham sido exercidas.
		double strike = opcaoVendida.getVariaveis().get(Variavel.STRIKE).doubleValue();
		if (precoAcao >= strike) {
			custodiaAcoes -= quantidadeOpcaoVendida;
			saldo += quantidadeOpcaoVendida * strike - config.corretagemIntegral;
			// recompra as acoes no integral
			int recompraIntegral = ((int) Math.floor(saldo / precoAcao)) / 100;
			if (recompraIntegral > 0) {
				custodiaAcoes += recompraIntegral * 100;
				saldo -= recompraIntegral * 100 * precoAcao + config.corretagemIntegral;
			}
			// recompra as acoes no fracionario
			int recompraFracionario = (int) Math.max(0, Math.floor(saldo / precoAcao));
			while (recompraFracionario > 0 && saldo - recompraFracionario * precoAcao - config.corretagemFracionario < 0) {
				recompraFracionario--;
			}
			if (recompraFracionario > 0) {
				custodiaAcoes += recompraFracionario;
				saldo -= recompraFracionario * precoAcao + config.corretagemFracionario;
			}
		}
		
		// Atualiza custodia
		opcaoVendida = null;
		quantidadeOpcaoVendida = 0;
	}

	public double calcularValor(double precoAcao, double precoOpcao) {
		double valorAcoes = this.custodiaAcoes * precoAcao;
		double valorOpcoes = precoOpcao * this.quantidadeOpcaoVendida + this.config.corretagemOpcoes;
		return (cacheValorTotal = this.saldo + valorAcoes - valorOpcoes);
	}

	public boolean rolar(double precoAcao,
			CotacaoOpcao novaOpcaoTeorica, double precoVelhaOpcaoTeorica) {

		if (opcaoVendida == null) {
			// Primeira venda coberta
			opcaoVendida = novaOpcaoTeorica;
			quantidadeOpcaoVendida = (custodiaAcoes / 100) * 100;

			// Valor da venda das opcoes
			double valorOpcao = opcaoVendida.getVariaveis().get(PRECO_REAL).doubleValue();
			double vendaLiquida = valorOpcao * quantidadeOpcaoVendida - config.corretagemOpcoes;

			saldo += vendaLiquida;

			// Quantidade estimada de compra no fracionario
			int compraFracionario = (int) Math.max(0, Math.floor(saldo / precoAcao));
			// Corretagem para a compra estimada
			double corretagemFracionario = config.corretagemFracionario * Math.ceil(compraFracionario / 99.0);
			// Reduz a quantidade comprada no fracionario
			// para o saldo nao ficar negativo
			while(compraFracionario > 0 && saldo - compraFracionario*precoAcao - corretagemFracionario < 0) {
				compraFracionario--;
				corretagemFracionario = config.corretagemFracionario * Math.ceil(compraFracionario / 99.0);
			}
			// Se, depois das reducoes, sobrou vontade de comprar
			if (compraFracionario > 0) {
				custodiaAcoes += compraFracionario;
				saldo -= compraFracionario*precoAcao + corretagemFracionario;
			}

		} else {
//			if (novaOpcaoTeorica.getSerie().equals(opcaoVendida.getSerie())) {
//				// Pediu para rolar para a mesma opcao em que estou vendido
//				return true;
//			}
			// /////////////////////////////////////////
			// COMPRA DA OPCAO LANCADA
			//
			saldo -= precoVelhaOpcaoTeorica * quantidadeOpcaoVendida + config.corretagemOpcoes;

			// /////////////////////////////////////////
			// VENDA DA NOVA OPCAO
			//
			opcaoVendida = novaOpcaoTeorica;
			quantidadeOpcaoVendida = (custodiaAcoes / 100) * 100;
			
			double valorOpcao = novaOpcaoTeorica.getVariaveis().get(PRECO_REAL).doubleValue();
			double vendaLiquida = valorOpcao * quantidadeOpcaoVendida - config.corretagemOpcoes;
			
			saldo += vendaLiquida;
			
			// /////////////////////////////////////////
			// COMPRA NO FRACIONARIO
			//
			int compraFracionario = (int) Math.max(0, Math.floor(saldo / precoAcao));
			double corretagemFracionario = config.corretagemFracionario * Math.ceil(compraFracionario / 99.0);
			while(compraFracionario > 0 && saldo - compraFracionario*precoAcao - corretagemFracionario < 0) {
				compraFracionario--;
				corretagemFracionario = config.corretagemFracionario * Math.ceil(compraFracionario / 99.0);
			}
			if (compraFracionario > 0) {
				custodiaAcoes += compraFracionario;
				saldo -= compraFracionario*precoAcao + corretagemFracionario;
			}

		}

		return saldo >= config.prejuizoMaximo;
	}

}
