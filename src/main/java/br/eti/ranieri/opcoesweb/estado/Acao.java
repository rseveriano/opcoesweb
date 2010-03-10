package br.eti.ranieri.opcoesweb.estado;


public enum Acao {

	PETROBRAS("PETR4") //
	, VALE("VALE5") //
	;

	private final String codigo;

	private Acao(String codigo) {
		this.codigo = codigo;
	}

	public String getCodigo() {
		return codigo;
	}

	public static Acao fromCodigoBDI(String codigoNegociacaoBDI) {
		for (Acao acao : values()) {
			if (acao.getCodigo().equalsIgnoreCase(codigoNegociacaoBDI))
				return acao;
		}
		return null;
	}
}
