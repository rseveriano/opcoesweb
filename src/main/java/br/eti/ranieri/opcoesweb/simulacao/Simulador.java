package br.eti.ranieri.opcoesweb.simulacao;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.eti.ranieri.opcoesweb.estado.CotacaoAcaoOpcoes;
import br.eti.ranieri.opcoesweb.estado.CotacaoOpcao;
import br.eti.ranieri.opcoesweb.estado.Serie;
import br.eti.ranieri.opcoesweb.persistencia.Persistencia;

@Service
public class Simulador {

	@Autowired
	Persistencia persistencia;

	public void simular(ConfigSimulacao config) {
		List<Entry<LocalDate,CotacaoAcaoOpcoes>> cotacoes = persistencia.getCotacoes(
				config.acao, config.dataInicial, config.dataFinal);

		Carteira melhorCarteira = new Carteira(config);
		Random random = new Random();

		// Datas de exercicio das opções
		Map<Serie, LocalDate> vencimentos = Serie.getVencimentosPorPeriodo(
				config.dataInicial, config.dataFinal);
		HistoricoOperacional historico, melhorHistorico = null;

		long agora = System.currentTimeMillis();
		long depois = agora + 1000 * config.tempoMaximoSimulacao;
		while (System.currentTimeMillis() < depois) {
			Carteira carteira = new Carteira(config);
			historico = new HistoricoOperacional();
			List<CotacaoOpcao> opcoesOntem = null;
			boolean abortar = false;
			for (Entry<LocalDate,CotacaoAcaoOpcoes> entry : cotacoes) {
				LocalDate data = entry.getKey();
				CotacaoAcaoOpcoes cotacao = entry.getValue();

				LocalDate dataVencimentoOpcaoVendida = null;
				if (carteira.opcaoVendida != null)
					dataVencimentoOpcaoVendida = vencimentos.get(carteira.opcaoVendida.getSerie());

				int acao = random.nextInt(3);
				// acao == 0: não faz nada ou é exercido e fica fora
				// acao == 1: rola nesta série ou é exercido e lança a próxima
				// acao == 2: rola na próxima série
				if (acao == 0) {
					if (dataVencimentoOpcaoVendida != null && dataVencimentoOpcaoVendida.isBefore(data)) {
						try {
							carteira.serExercida(data,
									cotacao.getCotacaoAcao(), cotacao
											.getOpcoesSerie1(), cotacao
											.getOpcoesSerie2(), historico,
									opcoesOntem);
						} catch (IllegalStateException e) {
							break;
						}
					}
					opcoesOntem = cotacao.getOpcoesSerie1();
				} else if (acao == 1) {
					if (dataVencimentoOpcaoVendida != null && dataVencimentoOpcaoVendida.isBefore(data)) {
						try {
							carteira.serExercida(data, cotacao.getCotacaoAcao(), cotacao.getOpcoesSerie1(), cotacao.getOpcoesSerie2(), historico, opcoesOntem);
						} catch (IllegalStateException e) {
							break;
						}
						if (abortar = carteira.rolar(data, cotacao.getCotacaoAcao(), cotacao.getOpcoesSerie2(), cotacao.getOpcoesSerie1(), random.nextBoolean(), random.nextBoolean(), historico, opcoesOntem))
							break;
						opcoesOntem = cotacao.getOpcoesSerie2();
					} else {
						if (abortar = carteira.rolar(data, cotacao.getCotacaoAcao(), cotacao.getOpcoesSerie1(), cotacao.getOpcoesSerie2(), random.nextBoolean(), random.nextBoolean(), historico, opcoesOntem))
							break;
						opcoesOntem = cotacao.getOpcoesSerie1();
					}
				} else if (acao == 2) {
					if (abortar = carteira.rolar(data, cotacao.getCotacaoAcao(), cotacao.getOpcoesSerie2(), cotacao.getOpcoesSerie1(), random.nextBoolean(), random.nextBoolean(), historico, opcoesOntem))
						break;
					opcoesOntem = cotacao.getOpcoesSerie2();
				}
			}
			if (abortar == false && carteira.melhorQue(melhorCarteira, cotacoes.get(cotacoes.size() - 1).getValue())) {
				melhorCarteira = carteira;
				melhorHistorico = historico;
			}
		}
		
		String opcaoNoFinal = "NENHUMA";
		if (melhorCarteira.opcaoVendida != null)
			opcaoNoFinal = melhorCarteira.opcaoVendida.getCodigo();
		System.out.println("Melhor carteira: saldo=" + melhorCarteira.saldo
				+ ", acoes=" + melhorCarteira.custodiaAcoes
				+ ", " +melhorCarteira.quantidadeOpcaoVendida
				+ " opcoes " + opcaoNoFinal);
		System.out.println(melhorHistorico);
	}

}
