package br.eti.ranieri.opcoesweb.importacao.offline;

public enum TipoMercadoBDI {

	MERCADO_A_VISTA(10), MERCADO_FRACIONARIO(20), OPCOES_DE_COMPRA(70);

	private int id;

	private TipoMercadoBDI(int id) {
		this.id = id;
	}

	public static TipoMercadoBDI getPorCodigo(int id) {
		for (TipoMercadoBDI tipo : values()) {
			if (tipo.id == id)
				return tipo;
		}
		return null;
	}
}
