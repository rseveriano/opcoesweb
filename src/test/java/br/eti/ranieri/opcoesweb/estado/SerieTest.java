package br.eti.ranieri.opcoesweb.estado;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.joda.time.LocalDate;

public class SerieTest extends TestCase {

	public void testIsSerieDaOpcao() {

		assertTrue(Serie.A.isSerieDaOpcao("PETRA38"));
		assertTrue(Serie.A.isSerieDaOpcao("VALEA112"));

		assertTrue(Serie.B.isSerieDaOpcao("PETRB12"));
		assertFalse(Serie.B.isSerieDaOpcao("VALE5"));

		assertTrue(Serie.C.isSerieDaOpcao("VALEC30"));
		assertFalse(Serie.C.isSerieDaOpcao("PETRC"));

		assertTrue(Serie.D.isSerieDaOpcao("PETRD32"));
		assertFalse(Serie.D.isSerieDaOpcao("VALE80"));

		assertTrue(Serie.E.isSerieDaOpcao("PETRE38"));
		assertFalse(Serie.E.isSerieDaOpcao("VALEEJ"));

		assertTrue(Serie.F.isSerieDaOpcao("PETRF38"));
		assertTrue(Serie.F.isSerieDaOpcao("VALEF12"));

		assertTrue(Serie.G.isSerieDaOpcao("PETRG38"));
		assertTrue(Serie.G.isSerieDaOpcao("VALEG12"));

		assertTrue(Serie.H.isSerieDaOpcao("PETRH38"));
		assertTrue(Serie.H.isSerieDaOpcao("VALEH12"));

		assertTrue(Serie.I.isSerieDaOpcao("PETRI38"));
		assertTrue(Serie.I.isSerieDaOpcao("VALEI12"));

		assertTrue(Serie.J.isSerieDaOpcao("PETRJ38"));
		assertTrue(Serie.J.isSerieDaOpcao("VALEJ12"));

		assertTrue(Serie.K.isSerieDaOpcao("PETRK38"));
		assertTrue(Serie.K.isSerieDaOpcao("VALEK12"));

		assertTrue(Serie.L.isSerieDaOpcao("PETRL38"));
		assertTrue(Serie.L.isSerieDaOpcao("VALEL12"));
	}

	public void testSeriePorData() {

		LocalDate data = new LocalDate(2009, 1, 1);
		for (int i = 1; i <= 365; i++) {
			
			if (i < 20) {
				// até 20/01/09
				assertEquals(Serie.getSerieAtualPorData(data), Serie.A);
				assertEquals(Serie.getProximaSeriePorData(data), Serie.B);
			} else if (i < 48) {
				// até 16/02/09
				assertEquals(Serie.getSerieAtualPorData(data), Serie.B);
				assertEquals(Serie.getProximaSeriePorData(data), Serie.C);
			} else if (i < 76) {
				// até 16/03/09
				assertEquals(Serie.getSerieAtualPorData(data), Serie.C);
				assertEquals(Serie.getProximaSeriePorData(data), Serie.D);
			} else if (i < 111) {
				// até 20/04/09
				assertEquals(Serie.getSerieAtualPorData(data), Serie.D);
				assertEquals(Serie.getProximaSeriePorData(data), Serie.E);
			} else if (i < 139) {
				// até 18/05/09
				assertEquals(Serie.getSerieAtualPorData(data), Serie.E);
				assertEquals(Serie.getProximaSeriePorData(data), Serie.F);
			} else if (i < 167) {
				// até 15/06/09
				assertEquals(Serie.getSerieAtualPorData(data), Serie.F);
				assertEquals(Serie.getProximaSeriePorData(data), Serie.G);
			} else if (i < 202) {
				// até 20/07/09
				assertEquals(Serie.getSerieAtualPorData(data), Serie.G);
				assertEquals(Serie.getProximaSeriePorData(data), Serie.H);
			} else if (i < 230) {
				// até 17/08/09
				assertEquals(Serie.getSerieAtualPorData(data), Serie.H);
				assertEquals(Serie.getProximaSeriePorData(data), Serie.I);
			} else if (i < 265) {
				// até 21/09/09
				assertEquals(Serie.getSerieAtualPorData(data), Serie.I);
				assertEquals(Serie.getProximaSeriePorData(data), Serie.J);
			} else if (i < 293) {
				// até 19/10/09
				assertEquals(Serie.getSerieAtualPorData(data), Serie.J);
				assertEquals(Serie.getProximaSeriePorData(data), Serie.K);
			} else if (i < 321) {
				// até 16/11/09
				assertEquals(Serie.getSerieAtualPorData(data), Serie.K);
				assertEquals(Serie.getProximaSeriePorData(data), Serie.L);
			} else if (i < 356) {
				// até 21/12/09
				assertEquals(Serie.getSerieAtualPorData(data), Serie.L);
				assertEquals(Serie.getProximaSeriePorData(data), Serie.A);
			} else {
				// a partir de 22/12/09
				assertEquals(Serie.getSerieAtualPorData(data), Serie.A);
				assertEquals(Serie.getProximaSeriePorData(data), Serie.B);
			}
			
			data = data.plusDays(1);
		}
	}

	public void testDataVencimento() {
		LocalDate terceiraSegundaJaneiro = new LocalDate(2009,1,19);
		LocalDate terceiraSegundaFevereiro = new LocalDate(2009,2,16);

		assertEquals(terceiraSegundaJaneiro, Serie.A.getDataVencimento(new LocalDate(2009,1,2)));
		assertEquals(terceiraSegundaJaneiro, Serie.A.getDataVencimento(new LocalDate(2009,1,19)));
		assertEquals(terceiraSegundaFevereiro, Serie.B.getDataVencimento(new LocalDate(2009,1,2)));
		assertEquals(terceiraSegundaFevereiro, Serie.B.getDataVencimento(new LocalDate(2009,1,19)));

		LocalDate terceiraSegundaMaio = new LocalDate(2009,5,18);
		LocalDate terceiraSegundaJunho = new LocalDate(2009,6,15);

		assertEquals(terceiraSegundaMaio, Serie.E.getDataVencimento(new LocalDate(2009,5,2)));
		assertEquals(terceiraSegundaJunho, Serie.F.getDataVencimento(new LocalDate(2009,5,2)));
		assertEquals(terceiraSegundaJunho, Serie.F.getDataVencimento(new LocalDate(2009,5,19)));
		assertEquals(terceiraSegundaJunho, Serie.F.getDataVencimento(new LocalDate(2009,6,1)));

		try {
			Serie.E.getDataVencimento(new LocalDate(2009,3,1));
			fail();
		} catch (IllegalArgumentException e) {
		}
		
		try {
			Serie.F.getDataVencimento(new LocalDate(2009,6,17));
			fail();
		} catch (IllegalArgumentException e) {
		}
		
		LocalDate terceiraSegundaDezembro = new LocalDate(2008,12,15);
		assertEquals(terceiraSegundaDezembro, Serie.L.getDataVencimento(new LocalDate(2008,11,25)));
		assertEquals(terceiraSegundaJaneiro, Serie.A.getDataVencimento(new LocalDate(2008,11,25)));
	}

	public void testVencimentosPorPeriodo() {
		LocalDate inicio = new LocalDate(2009, 1, 1);
		LocalDate fim = new LocalDate(2009, 12, 31);
		
		Map<Serie, LocalDate> vencimentosPorPeriodo = Serie.getVencimentosPorPeriodo(inicio, fim);
		Set<Serie> series = vencimentosPorPeriodo.keySet();
		Set<LocalDate> vencimentos = new HashSet<LocalDate>(vencimentosPorPeriodo.values());
		assertEquals(12, vencimentos.size());
		
		LocalDate janeiro, dezembro;

		Set<LocalDate> vencimentosEsperados = new HashSet<LocalDate>();
		vencimentosEsperados.add(janeiro = new LocalDate(2009,1,19));
		vencimentosEsperados.add(new LocalDate(2009,2,16));
		vencimentosEsperados.add(new LocalDate(2009,3,16));
		vencimentosEsperados.add(new LocalDate(2009,4,20));
		vencimentosEsperados.add(new LocalDate(2009,5,18));
		vencimentosEsperados.add(new LocalDate(2009,6,15));
		vencimentosEsperados.add(new LocalDate(2009,7,20));
		vencimentosEsperados.add(new LocalDate(2009,8,17));
		vencimentosEsperados.add(new LocalDate(2009,9,21));
		vencimentosEsperados.add(new LocalDate(2009,10,19));
		vencimentosEsperados.add(new LocalDate(2009,11,16));
		vencimentosEsperados.add(dezembro = new LocalDate(2009,12,21));

		Set<Serie> seriesEsperadas = new HashSet<Serie>(Arrays.asList(Serie.values()));

		assertEquals(vencimentosEsperados, vencimentos);
		assertEquals(seriesEsperadas, series);
		
		inicio = new LocalDate(2009,1,18);
		fim = new LocalDate(2009,12,22);
		series = vencimentosPorPeriodo.keySet();
		vencimentosPorPeriodo = Serie.getVencimentosPorPeriodo(inicio, fim);
		vencimentos = new HashSet<LocalDate>(vencimentosPorPeriodo.values());
		assertEquals(vencimentosEsperados, vencimentos);

		inicio = new LocalDate(2009,1,19);
		fim = new LocalDate(2009,12,21);
		vencimentosPorPeriodo = Serie.getVencimentosPorPeriodo(inicio, fim);
		series = vencimentosPorPeriodo.keySet();
		vencimentos = new HashSet<LocalDate>(vencimentosPorPeriodo.values());
		assertEquals(vencimentosEsperados, vencimentos);
		
		inicio = new LocalDate(2009,1,20);
		fim = new LocalDate(2009,12,20);
		vencimentosEsperados.remove(janeiro);
		vencimentosEsperados.remove(dezembro);
		vencimentosPorPeriodo = Serie.getVencimentosPorPeriodo(inicio, fim);
		series = vencimentosPorPeriodo.keySet();
		vencimentos = new HashSet<LocalDate>(vencimentosPorPeriodo.values());
		assertEquals(vencimentosEsperados, vencimentos);
		
		inicio = new LocalDate(2009,1,20);
		fim = new LocalDate(2009,2,15);
		vencimentosPorPeriodo = Serie.getVencimentosPorPeriodo(inicio, fim);
		series = vencimentosPorPeriodo.keySet();
		vencimentos = new HashSet<LocalDate>(vencimentosPorPeriodo.values());
		assertEquals(new HashSet<LocalDate>(), vencimentos);
	}
}