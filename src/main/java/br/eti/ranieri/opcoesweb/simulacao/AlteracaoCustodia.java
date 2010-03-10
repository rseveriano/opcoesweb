package br.eti.ranieri.opcoesweb.simulacao;

class AlteracaoCustodia {
	private double valor;
	private TipoAlteracaoCustodia tipo;

	public AlteracaoCustodia(double valor, TipoAlteracaoCustodia tipo) {
		this.valor = valor;
		this.tipo = tipo;
	}

	public double getValor() {
		return valor;
	}

	public TipoAlteracaoCustodia getTipo() {
		return tipo;
	}
}