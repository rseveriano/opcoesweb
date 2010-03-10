package br.eti.ranieri.opcoesweb.importacao.offline;

public enum CodigoBDI {

	LOTE_PADRAO("02"), OPCOES_DE_COMPRA("78"), MERCADO_FRACIONARIO("96");

	private String id;

	private CodigoBDI(String id) {
		this.id = id;
	}

	public static CodigoBDI getPorId(String id) {
		for (CodigoBDI codigo : values()) {
			if (codigo.id.equals(id))
				return codigo;
		}
		return null;
	}
}
