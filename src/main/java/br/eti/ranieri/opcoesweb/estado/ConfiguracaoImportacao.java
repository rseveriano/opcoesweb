package br.eti.ranieri.opcoesweb.estado;

import org.apache.wicket.IClusterable;

public class ConfiguracaoImportacao implements IClusterable  {

	public static final Integer QUANTIDADE_MAXIMA_OPCOES_POR_ACAO_POR_DIA = 7;

	private Integer quantidadeOpcoesPorAcaoPorDia;

	public Integer getQuantidadeOpcoesPorAcaoPorDia() {
		return quantidadeOpcoesPorAcaoPorDia == null ? QUANTIDADE_MAXIMA_OPCOES_POR_ACAO_POR_DIA
				: quantidadeOpcoesPorAcaoPorDia;
	}

	public void setQuantidadeOpcoesPorAcaoPorDia(
			Integer quantidadeOpcoesPorAcaoPorDia) {
		this.quantidadeOpcoesPorAcaoPorDia = quantidadeOpcoesPorAcaoPorDia;
	}
	
}
