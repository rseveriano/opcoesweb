package br.eti.ranieri.opcoesweb.estado;

import java.io.Serializable;
import java.util.Map;

public class CotacaoOpcao implements Serializable, Comparable<CotacaoOpcao> {

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

	public int compareTo(CotacaoOpcao outro) {
	    if (this.serie.ordinal() != outro.serie.ordinal()) {
		if (this.serie.ordinal() == Serie.values().length - 1 && outro.serie.ordinal() == 0)
		    return -1;
		return this.serie.ordinal() - outro.serie.ordinal();
	    }
	    
	    if (this.codigo.substring(0, 4).equals(outro.codigo) == false) {
		return this.codigo.compareTo(outro.codigo);
	    }
	    
	    Double thisStrike = this.variaveis.get(Variavel.STRIKE).doubleValue();
	    Double outroStrike = outro.variaveis.get(Variavel.STRIKE).doubleValue();
	    return thisStrike.compareTo(outroStrike);
	}
}
