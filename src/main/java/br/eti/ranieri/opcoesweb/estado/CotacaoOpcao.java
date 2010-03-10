package br.eti.ranieri.opcoesweb.estado;

import java.io.Serializable;
import java.util.Map;

public class CotacaoOpcao implements Serializable {

	private Serie serie;
	private String codigo;
	private Map<Variavel, Number> variaveis;

	public CotacaoOpcao(Serie serie, String codigo,
			Map<Variavel, Number> variaveis) {

		this.serie = serie;
		this.codigo = codigo;
		this.variaveis = variaveis;
	}

	public Serie getSerie() {
		return serie;
	}

	public String getCodigo() {
		return codigo;
	}

	public Map<Variavel, Number> getVariaveis() {
		return variaveis;
	}

}
