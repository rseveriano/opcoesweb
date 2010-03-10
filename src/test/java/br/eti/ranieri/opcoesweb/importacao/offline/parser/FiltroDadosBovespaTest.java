package br.eti.ranieri.opcoesweb.importacao.offline.parser;

import br.eti.ranieri.opcoesweb.importacao.offline.CodigoBDI;
import br.eti.ranieri.opcoesweb.importacao.offline.TipoMercadoBDI;
import junit.framework.TestCase;

public class FiltroDadosBovespaTest extends TestCase {

	public void testPossoIncluirAtivo() {
		FiltroDadosBovespa filtro = new FiltroDadosBovespa();
		
		assertTrue(filtro.possoIncluirAtivo(CodigoBDI.LOTE_PADRAO, TipoMercadoBDI.MERCADO_A_VISTA, "PETR4"));
		assertTrue(filtro.possoIncluirAtivo(CodigoBDI.LOTE_PADRAO, TipoMercadoBDI.MERCADO_A_VISTA, "vale5"));

		assertTrue(filtro.possoIncluirAtivo(CodigoBDI.OPCOES_DE_COMPRA, TipoMercadoBDI.OPCOES_DE_COMPRA, "PETRA16"));
		assertTrue(filtro.possoIncluirAtivo(CodigoBDI.OPCOES_DE_COMPRA, TipoMercadoBDI.OPCOES_DE_COMPRA, "ValeB28"));
		
		assertFalse(filtro.possoIncluirAtivo(CodigoBDI.LOTE_PADRAO, TipoMercadoBDI.MERCADO_A_VISTA, "vale3"));
		assertFalse(filtro.possoIncluirAtivo(CodigoBDI.LOTE_PADRAO, TipoMercadoBDI.MERCADO_A_VISTA, "petr3"));
		
		assertFalse(filtro.possoIncluirAtivo(CodigoBDI.OPCOES_DE_COMPRA, TipoMercadoBDI.OPCOES_DE_COMPRA, "VALE82"));
		assertFalse(filtro.possoIncluirAtivo(CodigoBDI.OPCOES_DE_COMPRA, TipoMercadoBDI.OPCOES_DE_COMPRA, "petrB"));
		
		assertFalse(filtro.possoIncluirAtivo(CodigoBDI.MERCADO_FRACIONARIO, TipoMercadoBDI.MERCADO_FRACIONARIO, "vale5"));
		assertFalse(filtro.possoIncluirAtivo(CodigoBDI.MERCADO_FRACIONARIO, TipoMercadoBDI.MERCADO_A_VISTA, "vale5"));
		assertFalse(filtro.possoIncluirAtivo(CodigoBDI.LOTE_PADRAO, TipoMercadoBDI.MERCADO_FRACIONARIO, "vale5"));
		
		assertFalse(filtro.possoIncluirAtivo(CodigoBDI.LOTE_PADRAO, TipoMercadoBDI.MERCADO_A_VISTA, "VALEB26"));
		assertFalse(filtro.possoIncluirAtivo(CodigoBDI.OPCOES_DE_COMPRA, TipoMercadoBDI.OPCOES_DE_COMPRA, "VALE5"));
	}
}
