package br.eti.ranieri.opcoesweb.estado;

import static br.eti.ranieri.opcoesweb.format.FormatadorNumerico.FORMATADOR_DECIMAL;
import static br.eti.ranieri.opcoesweb.format.FormatadorNumerico.FORMATADOR_DINHEIRO;
import static br.eti.ranieri.opcoesweb.format.FormatadorNumerico.FORMATADOR_MILHAR;
import static br.eti.ranieri.opcoesweb.format.FormatadorNumerico.FORMATADOR_PORCENTAGEM;
import static br.eti.ranieri.opcoesweb.format.FormatadorNumerico.FORMATADOR_PORMILHAGEM;
import br.eti.ranieri.opcoesweb.format.FormatadorNumerico;

public enum Variavel {
	STRIKE(FORMATADOR_DINHEIRO) //
	, JUROS(FORMATADOR_PORCENTAGEM) //
	, FRACAO_ANO_ATE_EXPIRAR(FORMATADOR_DECIMAL) //
	, VOLATILIDADE(FORMATADOR_PORCENTAGEM) //
	, PRECO_TEORICO(FORMATADOR_DINHEIRO) //
	, PRECO_REAL(FORMATADOR_DINHEIRO) //
	, VOLUME(FORMATADOR_MILHAR) //
	, DELTA(FORMATADOR_DECIMAL) //
	, GAMA(FORMATADOR_DECIMAL) //
	, TETA(FORMATADOR_DECIMAL) //
	, VALOR_INTRINSICO(FORMATADOR_DINHEIRO) //
	, VALOR_EXTRINSICO(FORMATADOR_DINHEIRO) //
	, TAXA_VE(FORMATADOR_PORCENTAGEM, true) //
	, NAO_VENDE(FORMATADOR_DINHEIRO, true) //
	, THE_VE(FORMATADOR_PORMILHAGEM, true) //
	, VDX(FORMATADOR_DECIMAL, true) //
	, BOSI(FORMATADOR_PORCENTAGEM, true) //
	//, OCO(FORMATADOR_PORCENTAGEM, true) //
	, ATM(FORMATADOR_PORCENTAGEM, true) //
	;

	private FormatadorNumerico formatador;
	private boolean indicador;

	private Variavel(FormatadorNumerico formatador) {
		this(formatador, false);
	}

	private Variavel(FormatadorNumerico formatador, boolean indicador) {
		this.formatador = formatador;
		this.indicador = indicador;
	}
	public FormatadorNumerico getFormatador() {
		return formatador;
	}

	public boolean isIndicador() {
		return indicador;
	}
}
