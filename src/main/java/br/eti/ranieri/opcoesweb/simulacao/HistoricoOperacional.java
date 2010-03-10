package br.eti.ranieri.opcoesweb.simulacao;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.joda.time.LocalDate;

import br.eti.ranieri.opcoesweb.estado.CotacaoAcao;
import br.eti.ranieri.opcoesweb.estado.CotacaoOpcao;
import br.eti.ranieri.opcoesweb.estado.Variavel;

public class HistoricoOperacional {

	private List<String> mensagens = new ArrayList<String>();

	public void registrarExercicio(LocalDate data, CotacaoAcao acao,
			CotacaoOpcao opcao) {

		if (opcao == null)
			return;

		double precoReal = opcao.getVariaveis().get(Variavel.PRECO_REAL)
				.doubleValue();
		double strike = opcao.getVariaveis().get(Variavel.STRIKE).doubleValue();
		double precoAcao = acao.getPrecoAcao();

		if (precoAcao > strike) {
			mensagens
					.add(String
							.format(
									"%s: Exercicio de %s a R$ %.2f e strike de R$ %.2f, acao a R$ %.2f",
									data.toString(), opcao.getCodigo(),
									precoReal, strike, precoAcao));
		} else {
			mensagens.add(String.format(
					"%s: Opcao %s micou no strike R$ %.2f, acao a R$ %.2f",
					data.toString(), opcao.getCodigo(), strike, precoAcao));
		}
	}

	@Override
	public String toString() {
		return StringUtils.join(mensagens, '\n');
	}

	public void registrarLancamento(LocalDate data, CotacaoOpcao opcaoVendida,
			int quantidadeOpcaoVendida, int compraFracionario) {

		mensagens
				.add(String
						.format(
								"%s: Lancamento %d %s a R$ %.2f, e compra de %d acoes no fracionario",
								data.toString(), quantidadeOpcaoVendida,
								opcaoVendida.getCodigo(), opcaoVendida
										.getVariaveis()
										.get(Variavel.PRECO_REAL),
								compraFracionario));
	}

	public void registrarRolamento(LocalDate data,
			CotacaoOpcao opcaoLancadaAtualizada,
			CotacaoOpcao teoricaVelha, double precoTeoricaVelha,
			int quantidadeCompradaFracionario) {

		if (teoricaVelha != null) {
			mensagens.add(String.format(
					"%s: Rolamento de %s por R$ %.2f no strike %.2f / para %s por R$ %.2f no strike %.2f || compra de %d no fracionario",
					data.toString(), //
					teoricaVelha.getSerie().getDescricao(), //
					precoTeoricaVelha, //
					teoricaVelha.getVariaveis().get(Variavel.STRIKE), //
					opcaoLancadaAtualizada.getSerie().getDescricao(), //
					opcaoLancadaAtualizada.getVariaveis().get(Variavel.PRECO_REAL), //
					opcaoLancadaAtualizada.getVariaveis().get(Variavel.STRIKE), //
					quantidadeCompradaFracionario));
		} else {
			mensagens.add(String.format("%s: Lançamento de %s por R$ %.2f no strike %.2f || compra de %d no fracionario",
					data.toString(), //
					opcaoLancadaAtualizada.getSerie().getDescricao(), //
					opcaoLancadaAtualizada.getVariaveis().get(Variavel.PRECO_REAL), //
					opcaoLancadaAtualizada.getVariaveis().get(Variavel.STRIKE), //
					quantidadeCompradaFracionario));
		}
	}
}
