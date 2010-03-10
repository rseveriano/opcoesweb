package br.eti.ranieri.opcoesweb.estado;

import java.io.Serializable;

public class CotacaoAcao implements Serializable {
	private Acao acao;
	private Double precoAcao;
	private Double variacaoAcao;

	public CotacaoAcao(Acao acao, Double precoAcao, Double variacaoAcao) {
		this.acao = acao;
		this.precoAcao = precoAcao;
		this.variacaoAcao = variacaoAcao;
	}

	public Acao getAcao() {
		return acao;
	}

	public Double getPrecoAcao() {
		return precoAcao;
	}

	public Double getVariacaoAcao() {
		return variacaoAcao;
	}

}
