package br.eti.ranieri.opcoesweb.importacao.offline.parser;

import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import br.eti.ranieri.opcoesweb.estado.Acao;
import br.eti.ranieri.opcoesweb.importacao.offline.CodigoBDI;
import br.eti.ranieri.opcoesweb.importacao.offline.TipoMercadoBDI;

@Service("filtroDadosBovespa")
public class FiltroDadosBovespa {

	private final Pattern petrValePattern = Pattern
			.compile("PETR[A-L]\\w+|VALE[A-L]\\d+", Pattern.CASE_INSENSITIVE);

	public boolean possoIncluirIndice(int idIndice) {
		// IBOVESPA = 1
		// IBRX-50 = 6
		return idIndice == 1 || idIndice == 6;
	}

	public boolean possoIncluirAtivo(CodigoBDI codigoBDI,
			TipoMercadoBDI tipoMercado, String codigoNegociacaoAcao) {

		if (TipoMercadoBDI.MERCADO_A_VISTA.equals(tipoMercado) && CodigoBDI.LOTE_PADRAO.equals(codigoBDI)) {
			return Acao.fromCodigoBDI(codigoNegociacaoAcao) != null;
		} else if (TipoMercadoBDI.OPCOES_DE_COMPRA.equals(tipoMercado) && CodigoBDI.OPCOES_DE_COMPRA.equals(codigoBDI)) {
			return petrValePattern.matcher(codigoNegociacaoAcao).matches();
		}
		return false;
	}

}
