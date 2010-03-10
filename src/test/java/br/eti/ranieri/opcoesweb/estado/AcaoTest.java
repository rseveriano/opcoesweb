package br.eti.ranieri.opcoesweb.estado;

import junit.framework.TestCase;

public class AcaoTest extends TestCase {

	public void testFromCodigoBDI() {
		assertEquals(Acao.PETROBRAS, Acao.fromCodigoBDI("PETR4"));
		assertEquals(Acao.PETROBRAS, Acao.fromCodigoBDI("petr4"));
		assertEquals(Acao.PETROBRAS, Acao.fromCodigoBDI("Petr4"));
		
		assertEquals(Acao.VALE, Acao.fromCodigoBDI("VALE5"));
		assertEquals(Acao.VALE, Acao.fromCodigoBDI("vale5"));
		assertEquals(Acao.VALE, Acao.fromCodigoBDI("Vale5"));
		
		assertNull(Acao.fromCodigoBDI("petr3"));
		assertNull(Acao.fromCodigoBDI("petr4f"));
		assertNull(Acao.fromCodigoBDI("vale3"));
		assertNull(Acao.fromCodigoBDI("vale5f"));
		assertNull(Acao.fromCodigoBDI("prga3"));
		assertNull(Acao.fromCodigoBDI("pibb11"));
	}
}
