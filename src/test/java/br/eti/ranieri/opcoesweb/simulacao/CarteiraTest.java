package br.eti.ranieri.opcoesweb.simulacao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import junit.framework.TestCase;

import org.joda.time.LocalDate;

import br.eti.ranieri.opcoesweb.estado.Acao;
import br.eti.ranieri.opcoesweb.estado.CotacaoAcao;
import br.eti.ranieri.opcoesweb.estado.CotacaoAcaoOpcoes;
import br.eti.ranieri.opcoesweb.estado.CotacaoOpcao;
import br.eti.ranieri.opcoesweb.estado.Serie;
import br.eti.ranieri.opcoesweb.estado.Variavel;

public class CarteiraTest extends TestCase {

    public void testEncontrarPeloCodigo() {

        ConfigSimulacao config = new ConfigSimulacao();
        config.acao = Acao.PETROBRAS;
        config.corretagemFracionario = 5.0;
        config.corretagemOpcoes = 10.0;
        config.corretagemIntegral = 20.0;
        config.custodiaInicial = 500;
        config.dataInicial = new LocalDate();
        config.dataFinal = new LocalDate();
        config.prejuizoMaximo = -2000.0;
        config.tempoMaximoSimulacao = 20;

        Carteira c1 = new Carteira(config);
        List<CotacaoOpcao> semOpcoes = new ArrayList<CotacaoOpcao>();

        try {
	        Carteira.encontrarPeloCodigo(c1, semOpcoes, semOpcoes, null, null);
	        fail();
        } catch (IllegalStateException e) {
        }
    }

	public void testEncontrarMelhorOpcao() {

		ConfigSimulacao config = new ConfigSimulacao();

		Carteira c = new Carteira(config);
		Random random = new Random();
		List<CotacaoOpcao> opcoes = new ArrayList<CotacaoOpcao>();

		// Coloca 3 opcoes randomicas cujos indicadores sao baixos
		for (int i = 0; i < 3; i++) {
			opcoes.add(criarOpcao(Serie.A,
					"PETRA" + String.valueOf(20 + i * 2), null, 20 + i * 2.0,
					random.nextInt(9999) + 1L, random.nextDouble() * 0.04,
					random.nextDouble() * 0.008));
		}
		// Coloca a opcao Teorica com volume zero e maiores indicadores
		opcoes.add(criarOpcao(Serie.A, "Teórica", null, null, 0L, 0.07, 0.015));

		// Coloca a opcao real com maior Taxa VE entre as nao-teoricas
		CotacaoOpcao opcaoMaiorTaxaVE = criarOpcao(Serie.A, "PETRA26",
				null, 26.0, 10000L, 0.06, 0.009);
		opcoes.add(opcaoMaiorTaxaVE);

		// Coloca a opcao real com maior TheVE entre as nao-teoricas
		CotacaoOpcao opcaoMaiorTheVE = criarOpcao(Serie.A, "PETRA28",
				null, 28.0, 10000L, 0.05, 0.01);
		opcoes.add(opcaoMaiorTheVE);

		CotacaoOpcao melhor = c.encontrarMelhorOpcao(opcoes, true, true);
		assertEquals(opcaoMaiorTaxaVE, melhor);

		melhor = c.encontrarMelhorOpcao(opcoes, false, true);
		assertEquals(opcaoMaiorTheVE, melhor);

		// Verifica o caso de empate em TaxaVE
		opcaoMaiorTaxaVE.getVariaveis().put(Variavel.TAXA_VE, new Double(0.05));
		melhor = c.encontrarMelhorOpcao(opcoes, true, true);
		assertEquals(opcaoMaiorTaxaVE, melhor);
		melhor = c.encontrarMelhorOpcao(opcoes, true, false);
		assertEquals(opcaoMaiorTheVE, melhor);
		
		// Verifica o caso de empate em TheVE
		opcaoMaiorTaxaVE.getVariaveis().put(Variavel.TAXA_VE, new Double(0.06));
		opcaoMaiorTaxaVE.getVariaveis().put(Variavel.THE_VE, new Double(0.01));
		melhor = c.encontrarMelhorOpcao(opcoes, false, true);
		assertEquals(opcaoMaiorTaxaVE, melhor);
		melhor = c.encontrarMelhorOpcao(opcoes, false, false);
		assertEquals(opcaoMaiorTheVE, melhor);
	}

    public void testCalcularValorTotalMelhorQue() {

        ConfigSimulacao config = new ConfigSimulacao();
        config.acao = Acao.PETROBRAS;
        config.corretagemFracionario = 5.0;
        config.corretagemOpcoes = 10.0;
        config.corretagemIntegral = 20.0;
        config.custodiaInicial = 500;
        config.dataInicial = new LocalDate(2009, 1, 2);
        config.dataFinal = new LocalDate(2009, 3, 28);
        config.prejuizoMaximo = -2000.0;
        config.tempoMaximoSimulacao = 20;

        final List<CotacaoOpcao> semOpcoes = new ArrayList<CotacaoOpcao>();
        final CotacaoAcao cotacaoAcao = new CotacaoAcao(Acao.PETROBRAS, 30.0, 0.0);

        // Sem opcao lancada
        Carteira c1 = new Carteira(config);
        double valor = Carteira.calcularValorTotal(c1, new CotacaoAcaoOpcoes(cotacaoAcao, semOpcoes, semOpcoes));
        assertEquals(500 * 30.0, valor);

        // Com opcao lancada
        Carteira c2 = new Carteira(config);

        CotacaoOpcao opcaoOtm = criarOpcao(Serie.A, "PETRA34", 0.07, null, null, null, null);

        c2.opcaoVendida = opcaoOtm;
        c2.quantidadeOpcaoVendida = 500;

        List<CotacaoOpcao> opcoesComOtm = new ArrayList<CotacaoOpcao>();
        opcoesComOtm.add(new CotacaoOpcao(Serie.A, "PETRA28", new HashMap<Variavel,Number>()));
        opcoesComOtm.add(new CotacaoOpcao(Serie.A, "PETRA30", new HashMap<Variavel,Number>()));
        opcoesComOtm.add(new CotacaoOpcao(Serie.A, "PETRA32", new HashMap<Variavel,Number>()));
        opcoesComOtm.add(opcaoOtm);
        opcoesComOtm.add(new CotacaoOpcao(Serie.A, "PETRA36", new HashMap<Variavel,Number>()));

        valor = Carteira.calcularValorTotal(c2, new CotacaoAcaoOpcoes(cotacaoAcao, opcoesComOtm, semOpcoes));
        assertEquals(500 * (30.0 - 0.07) - 10, valor);

        // Com saldo remanescente
        Carteira c3 = new Carteira(config);
        c3.saldo = -1157.58;
        valor = Carteira.calcularValorTotal(c3, new CotacaoAcaoOpcoes(cotacaoAcao, semOpcoes, semOpcoes));
        assertEquals(500 * 30.0 - 1157.58, valor);

        // Com opcao lancada e com saldo remanescente
        Carteira c4 = new Carteira(config);
        c4.saldo = -1157.58;
        c4.opcaoVendida = opcaoOtm;
        c4.quantidadeOpcaoVendida = 400;
        valor = Carteira.calcularValorTotal(c4, new CotacaoAcaoOpcoes(cotacaoAcao, semOpcoes, opcoesComOtm));
        assertEquals(500 * 30.0 - 400 * 0.07 - 10 - 1157.58, valor);

        assertTrue(c1.melhorQue(c2, new CotacaoAcaoOpcoes(cotacaoAcao, opcoesComOtm, semOpcoes)));
        assertTrue(c2.melhorQue(c3, new CotacaoAcaoOpcoes(cotacaoAcao, opcoesComOtm, semOpcoes)));
        assertTrue(c3.melhorQue(c4, new CotacaoAcaoOpcoes(cotacaoAcao, opcoesComOtm, semOpcoes)));

        assertFalse(c2.melhorQue(c1, new CotacaoAcaoOpcoes(cotacaoAcao, opcoesComOtm, semOpcoes)));
        assertFalse(c3.melhorQue(c2, new CotacaoAcaoOpcoes(cotacaoAcao, opcoesComOtm, semOpcoes)));
        assertFalse(c4.melhorQue(c3, new CotacaoAcaoOpcoes(cotacaoAcao, opcoesComOtm, semOpcoes)));
    }

    public void testSerExercida() {
        final ConfigSimulacao config = new ConfigSimulacao();
        config.acao = Acao.PETROBRAS;
        config.corretagemFracionario = 5.0;
        config.corretagemOpcoes = 10.0;
        config.corretagemIntegral = 20.0;
        config.custodiaInicial = 500;
        config.dataInicial = new LocalDate(2009, 1, 2);
        config.dataFinal = new LocalDate(2009, 3, 28);
        config.prejuizoMaximo = -2000.0;
        config.tempoMaximoSimulacao = 20;

        final List<CotacaoOpcao> semOpcoes = new ArrayList<CotacaoOpcao>();

		final CotacaoOpcao petrA30 = criarOpcao(Serie.A, "PETRA30", 0.07, 30.0, 15672168L, null, null);
		final CotacaoOpcao petrA36 = criarOpcao(Serie.A, "PETRA36", 0.01, 36.0, 672168L, null, null);
		
		final List<CotacaoOpcao> opcoesA = new ArrayList<CotacaoOpcao>();
		opcoesA.add(new CotacaoOpcao(Serie.A, "PETRA28", new HashMap<Variavel, Number>()));
		opcoesA.add(petrA30);
		opcoesA.add(petrA36);
		opcoesA.add(new CotacaoOpcao(Serie.A, "Teórica", new HashMap<Variavel, Number>()));
		
		final CotacaoOpcao petrB30 = criarOpcao(Serie.B, "PETRB30", 0.70, 30.0, 582376L, null, null);
		final List<CotacaoOpcao> opcoesB = new ArrayList<CotacaoOpcao>();
		opcoesB.add(new CotacaoOpcao(Serie.B, "PETRB28", new HashMap<Variavel, Number>()));
		opcoesB.add(petrB30);
		opcoesB.add(new CotacaoOpcao(Serie.B, "Teórica", new HashMap<Variavel, Number>()));
		
		final CotacaoAcao petr32 = new CotacaoAcao(Acao.PETROBRAS, 32.0, 0.0);

        // Testa o nao rolamento, quando nao ha opcao vendida
        Carteira c = new Carteira(config);
        HistoricoOperacional historico = new HistoricoOperacional();
        c.serExercida(new LocalDate(2009, 1, 19), petr32, semOpcoes, semOpcoes, historico, null);
        assertEquals(new Integer(500), c.custodiaAcoes);
        assertNull(c.opcaoVendida);
        assertEquals(0, c.quantidadeOpcaoVendida);
        assertEquals(0.0, c.saldo);
        
        // Testa o nao rolamento pois a opcao e' da proxima serie
        c = new Carteira(config);
        historico = new HistoricoOperacional();
        c.opcaoVendida = petrB30;
        c.quantidadeOpcaoVendida = 500;
        c.serExercida(new LocalDate(2009, 1, 19), petr32, semOpcoes, opcoesB, historico, null);
        assertEquals(new Integer(500), c.custodiaAcoes);
        assertEquals(petrB30, c.opcaoVendida);
        assertEquals(500, c.quantidadeOpcaoVendida);
        assertEquals(0.0, c.saldo);
        
        // Testa o rolamento de toda a posicao em acoes
        c = new Carteira(config);
        historico = new HistoricoOperacional();
        c.opcaoVendida = petrA30;
        c.quantidadeOpcaoVendida = 500;
        c.serExercida(new LocalDate(2009, 1, 19), petr32, opcoesA, semOpcoes, historico, null);
        // vende 500 acoes a R$ 30,00 = 15.000,00 - 20,00 (corretagem) = 14.980,00
        // compra 400 acoes no integral a R$ 32,00 = 12.800,00 + 20,00 (corretagem) = 12.820,00
        // compra de 67 acoes no fracionario a R$ 32,00 = 2.144,00 + 5,00 (corretagem) = 2.149,00
        // saldo = 14.980,00 - 12.820,00 - 2.149,00 = 11,00
        assertEquals(new Integer(467), c.custodiaAcoes);
        assertNull(c.opcaoVendida);
        assertEquals(0, c.quantidadeOpcaoVendida);
        assertTrue(Math.abs(11.0 - c.saldo) < 1E-5);
        
        // Testa nao rolamento por ter opcao OTM
        c = new Carteira(config);
        historico = new HistoricoOperacional();
        c.opcaoVendida = petrA36;
        c.quantidadeOpcaoVendida = 500;
        c.serExercida(new LocalDate(2009, 1, 19), petr32, opcoesA, opcoesB, historico, null);
        assertEquals(new Integer(500), c.custodiaAcoes);
        assertNull(c.opcaoVendida);
        assertEquals(0, c.quantidadeOpcaoVendida);
        assertEquals(0.0, c.saldo);
        
        // Testa rolamento parcial
        config.custodiaInicial = 537;
        c = new Carteira(config);
        historico = new HistoricoOperacional();
        c.opcaoVendida = petrA30;
        c.quantidadeOpcaoVendida = 500;
        c.saldo = 25.83;
        c.serExercida(new LocalDate(2009, 1, 19), petr32, opcoesA, opcoesB, historico, null);
        // vende 500 acoes a R$ 30,00 = 15.000,00 - 20,00 (corretagem) = 14.980,00
        // compra 400 acoes no integral a R$ 32,00 = 12.800,00 + 20,00 (corretagem) = 12.820,00
        // compra de 68 acoes no fracionario a R$ 32,00 = 2.176,00 + 5,00 (corretagem) = 2.181,00
        // saldo = 14.980,00 + 25.83 - 12.820,00 - 2.181,00 = 4,83
        assertEquals(new Integer(537-500+400+68), c.custodiaAcoes);
        assertNull(c.opcaoVendida);
        assertEquals(0, c.quantidadeOpcaoVendida);
        assertTrue(Math.abs(4.83 - c.saldo) < 1E-5);
    }
    
    public void testRolar() {
    	final ConfigSimulacao config = new ConfigSimulacao();
        config.acao = Acao.PETROBRAS;
        config.corretagemFracionario = 5.0;
        config.corretagemOpcoes = 10.0;
        config.corretagemIntegral = 20.0;
        config.custodiaInicial = 500;
        config.dataInicial = new LocalDate(2009, 1, 2);
        config.dataFinal = new LocalDate(2009, 3, 28);
        config.prejuizoMaximo = -2000.0;
        config.tempoMaximoSimulacao = 20;

        final LocalDate hoje = new LocalDate();

        final CotacaoOpcao petrA28 = criarOpcao(Serie.A, "PETRA28", 2.15, 28.0,  8672168L, 0.047, 0.0010);
		final CotacaoOpcao petrA30 = criarOpcao(Serie.A, "PETRA30", 0.97, 30.0, 15672168L, 0.060, 0.0011);
		final CotacaoOpcao petrA32 = criarOpcao(Serie.A, "PETRA32", 0.42, 32.0,  9672168L, 0.055, 0.0012);
		final CotacaoOpcao petrA36 = criarOpcao(Serie.A, "PETRA36", 0.17, 36.0,   672168L, 0.021, 0.0003);

		final List<CotacaoOpcao> opcoesA = new ArrayList<CotacaoOpcao>();
		opcoesA.add(petrA28);
		opcoesA.add(petrA30);
		opcoesA.add(petrA32);
		opcoesA.add(petrA36);
		opcoesA.add(criarOpcao(Serie.A, "Teórica", 30.07, 30.07, 0L, 0.06, 0.0011));

		final CotacaoOpcao petrB30 = criarOpcao(Serie.B, "PETRB30", 0.70, 30.0,  98276L, 0.070, 0.0012);
		final CotacaoOpcao petrB32 = criarOpcao(Serie.B, "PETRB32", 1.62, 32.0, 582376L, 0.075, 0.0010);
		final CotacaoOpcao petrB34 = criarOpcao(Serie.B, "PETRB34", 0.83, 34.0,  82393L, 0.070, 0.0012);

		final List<CotacaoOpcao> opcoesB = new ArrayList<CotacaoOpcao>();
		opcoesB.add(petrB30);
		opcoesB.add(petrB32);
		opcoesB.add(petrB34);
		opcoesB.add(criarOpcao(Serie.B, "Teórica", 32.07, 32.07, 0L, 0.076, 0.00121));

		final CotacaoAcao petr32 = new CotacaoAcao(Acao.PETROBRAS, 32.0, 0.0);

        // Testar rolamento sem opcao vendida anteriormente
        Carteira c = new Carteira(config);
        HistoricoOperacional historico = new HistoricoOperacional();
        c.custodiaAcoes = 511;
        c.saldo = 15.00;
        assertFalse(c.rolar(hoje, petr32, opcoesA, opcoesB, true, true, historico, null));
        // venda de 500 PETRA30: 500 * 0,97 - 10,00 (corretagem) = 475,00
        // compra de 15 PETR4F: 15 * 32,00 + 5,00 (corretagem) = 485,00
        // saldo = 15,00 + 475,00 - 485,00 = 5,00
        assertEquals(new Integer(511 + 15), c.custodiaAcoes);
        assertEquals(petrA30, c.opcaoVendida);
        assertEquals(500, c.quantidadeOpcaoVendida);
        assertTrue(Math.abs(5.00 - c.saldo) < 1E-5);
        
        // Testar rolamento para cima na mesma serie com maior TheVE
        assertFalse(c.rolar(hoje, petr32, opcoesA, opcoesB, false, true, historico, null));
        // compra de 500 PETRA30: 500 * 0,97 + 10,00 (corretagem) = 495,00
        // venda de 500 PETRA32: 500 * 0,42 - 10,00 (corretagem) = 200,00
        // saldo = 5,00 - 495,00 + 200,00 = -290,00
        assertEquals(new Integer(511 + 15), c.custodiaAcoes);
        assertEquals(petrA32, c.opcaoVendida);
        assertEquals(500, c.quantidadeOpcaoVendida);
        assertTrue(Math.abs(-290.00 - c.saldo) < 1E-5);
        
        // Testar rolamento para frente
        assertFalse(c.rolar(hoje, petr32, opcoesB, opcoesA, true, false, historico, null));
        // compra de 500 PETRA32: 500 * 0,42 + 10,00 = 220,00
        // venda de 500 PETRB32: 500 * 1,62 - 10,00 = 800,00
        // compra de 8 PETRF: 8 * 32,00 + 5 = 261,00
        // saldo = -290,00 - 220,00 + 800,00 - 261,00 = 29,00
        assertEquals(new Integer(511 + 15 + 8), c.custodiaAcoes);
        assertEquals(petrB32, c.opcaoVendida);
        assertEquals(500, c.quantidadeOpcaoVendida);
        assertTrue(Math.abs(29.00 - c.saldo) < 1E-5);
    }

    private CotacaoOpcao criarOpcao(Serie serie, String codigo, Double precoReal, Double strike, Long volume, Double taxaVE, Double theVE) {
    	Map<Variavel, Number> variaveis = new HashMap<Variavel, Number>();
    	if (precoReal != null)
    		variaveis.put(Variavel.PRECO_REAL, precoReal);
    	if (strike != null)
    		variaveis.put(Variavel.STRIKE, strike);
    	if (volume != null)
    		variaveis.put(Variavel.VOLUME, volume);
    	if (taxaVE != null)
    		variaveis.put(Variavel.TAXA_VE, taxaVE);
    	if (theVE != null)
    		variaveis.put(Variavel.THE_VE, theVE);
		return new CotacaoOpcao(serie, codigo, variaveis);
    }
}
