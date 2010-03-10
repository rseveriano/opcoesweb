package br.eti.ranieri.opcoesweb.blackscholes;

import static br.eti.ranieri.opcoesweb.estado.Variavel.ATM;
import static br.eti.ranieri.opcoesweb.estado.Variavel.BOSI;
import static br.eti.ranieri.opcoesweb.estado.Variavel.DELTA;
import static br.eti.ranieri.opcoesweb.estado.Variavel.FRACAO_ANO_ATE_EXPIRAR;
import static br.eti.ranieri.opcoesweb.estado.Variavel.GAMA;
import static br.eti.ranieri.opcoesweb.estado.Variavel.JUROS;
import static br.eti.ranieri.opcoesweb.estado.Variavel.NAO_VENDE;
import static br.eti.ranieri.opcoesweb.estado.Variavel.PRECO_REAL;
import static br.eti.ranieri.opcoesweb.estado.Variavel.PRECO_TEORICO;
import static br.eti.ranieri.opcoesweb.estado.Variavel.STRIKE;
import static br.eti.ranieri.opcoesweb.estado.Variavel.TAXA_VE;
import static br.eti.ranieri.opcoesweb.estado.Variavel.TETA;
import static br.eti.ranieri.opcoesweb.estado.Variavel.THE_VE;
import static br.eti.ranieri.opcoesweb.estado.Variavel.VALOR_EXTRINSICO;
import static br.eti.ranieri.opcoesweb.estado.Variavel.VALOR_INTRINSICO;
import static br.eti.ranieri.opcoesweb.estado.Variavel.VDX;
import static br.eti.ranieri.opcoesweb.estado.Variavel.VOLATILIDADE;
import static br.eti.ranieri.opcoesweb.estado.Variavel.VOLUME;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MathException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.distribution.NormalDistributionImpl;
import org.apache.commons.math.optimization.ConvergenceChecker;
import org.apache.commons.math.optimization.CostException;
import org.apache.commons.math.optimization.CostFunction;
import org.apache.commons.math.optimization.NelderMead;
import org.apache.commons.math.optimization.PointCostPair;
import org.apache.commons.math.random.GaussianRandomGenerator;
import org.apache.commons.math.random.JDKRandomGenerator;
import org.apache.commons.math.random.UncorrelatedRandomVectorGenerator;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.eti.ranieri.opcoesweb.estado.Acao;
import br.eti.ranieri.opcoesweb.estado.CotacaoAcao;
import br.eti.ranieri.opcoesweb.estado.CotacaoAcaoOpcoes;
import br.eti.ranieri.opcoesweb.estado.CotacaoOpcao;
import br.eti.ranieri.opcoesweb.estado.Serie;
import br.eti.ranieri.opcoesweb.estado.Variavel;
import br.eti.ranieri.opcoesweb.importacao.offline.CotacaoBDI;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

@Service("blackSholes")
public class BlackScholes {

    private static final double DIAS_POR_ANO = 365.25;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final NormalDistributionImpl phi = new NormalDistributionImpl();

    private class DetectorConvergencia implements ConvergenceChecker {

        private double custoAnterior = -1.0;
        private int iteracoes = 0;
        private int maxIteracoes;

        public DetectorConvergencia(int maxIteracoes) {
            this.maxIteracoes = maxIteracoes;
        }

        public boolean converged(PointCostPair[] pontosCustos) {
            iteracoes++;
            double custo = pontosCustos[0].getCost();
            if (custoAnterior == -1.0) {
                custoAnterior = custo;
                return false;
            } else {
                custoAnterior = custo;
                return iteracoes == maxIteracoes / 2;
            }
        }
    }

    private class FuncaoDispersao implements UnivariateRealFunction, CostFunction {

        private List<CotacaoBDI> opcoes;
        private double r;
        private double s;

        public FuncaoDispersao(double s, double r, List<CotacaoBDI> opcoes) {
            this.s = s;
            this.r = r;
            this.opcoes = opcoes;
        }

        public double value(double sigma) throws FunctionEvaluationException {

            double somaVolumesDeltaPrecos = 0.0;

            for (int i = 0, count = opcoes.size(); i < count; i++) {
                CotacaoBDI opcao = opcoes.get(i);

                // preço de fechamento da opção
                double fechamentoOpcao = opcao.getFechamento() / 100.0;
                // volume da opção
                double volumeOpcao = opcao.getVolume() / 100.0;
                // preço de exercício
                double x = opcao.getPrecoExercicio() / 100.0;
                // Quantidade de dias até o vencimento, em fraçao de ano.
                // Adiciono um dia pois, no dia do vencimento, t não pode
                // ser zero, gerando divisão por zero
                double t = (1 + Days.daysBetween(opcao.getDataPregao(),
                        opcao.getLocalDateVencimento()).getDays()) / DIAS_POR_ANO;

                double d1 = (Math.log(s / x) + (r + sigma * sigma / 2) * t) / (sigma * Math.sqrt(t));
                double d2 = d1 - sigma * Math.sqrt(t);

                // valor teórico da opção
                double valorTeorico = s * probabilidadeAcumulada(d1) - x * Math.exp(-r * t) * probabilidadeAcumulada(d2);

                somaVolumesDeltaPrecos += Math.abs(valorTeorico - fechamentoOpcao) * volumeOpcao;
            }

            return somaVolumesDeltaPrecos;
        }

        public double cost(double[] x) throws CostException {
            try {
                return value(x[0]);
            } catch (FunctionEvaluationException e) {
                throw new CostException(e);
            }
        }
    }

    private double calcularSigma(double s, double r, List<CotacaoBDI> opcoes) {

        FuncaoDispersao funcao = new FuncaoDispersao(s, r, opcoes);
        // ////////////////////////////////////////////////
        // Metodo de minimizacao de funcao custo é eficaz
        // ////////////////////////////////////////////////
        double sigmaNelder = Double.NaN;
        try {
            int iteracoes = 100;
            PointCostPair ponto = new NelderMead().minimize(funcao, iteracoes,
                    new DetectorConvergencia(iteracoes / 2),
                    new UncorrelatedRandomVectorGenerator(1,
                    new GaussianRandomGenerator(
                    new JDKRandomGenerator())));
            sigmaNelder = ponto.getPoint()[0];
        } catch (Exception e) {
            logger.error(
                    "Erro na minimizacao de funcao dispersao de volatilidade",
                    e);
        }
        return sigmaNelder;
    }

    private Map<CotacaoBDI, Map<Variavel, Number>> calcularMapaVariaveisPorOpcao(double s, double r, List<CotacaoBDI> opcoes, CotacaoBDI opcaoTeorica) {

    	// mapa de retorno
    	Map<CotacaoBDI, Map<Variavel, Number>> mapa = new HashMap<CotacaoBDI, Map<Variavel, Number>>();

        // volatilidade implicita
        double sigma = calcularSigma(s, r, opcoes);

        double volumeTotalOpcoes = 0.0;
        for (CotacaoBDI opcao : opcoes) {
            volumeTotalOpcoes += opcao.getVolume() / 100.0;
        }

        for (int i = 0, count = opcoes.size(); i < count; i++) {
            CotacaoBDI opcao = opcoes.get(i);

            // preço de fechamento da opção
            double fechamentoOpcao = opcao.getFechamento() / 100.0;
            // volume da opção
            double volumeOpcao = opcao.getVolume() / 100.0;
            // preço de exercício
            double x = opcao.getPrecoExercicio() / 100.0;
            // Quantidade de dias até o vencimento, em fraçao de ano.
            // Adiciono um dia pois, no dia do vencimento, t não pode
            // ser zero, gerando divisão por zero
            double t = (1 + Days.daysBetween(opcao.getDataPregao(),
                    opcao.getLocalDateVencimento()).getDays()) / DIAS_POR_ANO;

            double d1 = (Math.log(s / x) + (r + sigma * sigma / 2) * t) / (sigma * Math.sqrt(t));
            double d2 = d1 - sigma * Math.sqrt(t);

            // valor teórico da opção
            double valorTeorico = s * probabilidadeAcumulada(d1) - x * Math.exp(-r * t) * probabilidadeAcumulada(d2);

            // grega da variação da opção para cada $1 da ação
            double delta = probabilidadeAcumulada(d1);
            // grega da variação do delta para cada $1 da ação
            double gama = densidadeFuncaoNormal(d1) / (s * sigma * Math.sqrt(t));
            // grega da variação da opção pelo tempo
            double theta = (-(s * densidadeFuncaoNormal(d1) * sigma) / (2 * Math.sqrt(t)) - r * x * Math.exp(-r * t) * probabilidadeAcumulada(d2)) / 365.0;

            CotacaoBDI opcaoAnterior = (i == 0) ? null : opcoes.get(i-1);
            CotacaoBDI opcaoPosterior = (i == count - 1) ? null : opcoes.get(i+1);

            mapa.put(opcao, criarMapaVariaveis(s, r, t, sigma,
                    valorTeorico, fechamentoOpcao, volumeOpcao,
                    volumeTotalOpcoes, x, delta, gama, theta,
                    opcaoAnterior, opcaoPosterior));
        }
        
        calcularEstatisticas(mapa);
        calcularOpcaoTeorica(mapa, opcaoTeorica, s, r, sigma);
        
        return mapa;
    }

    private List<CotacaoBDI> limitarOpcoesPorVolumeEOrdenarPorStrike(List<CotacaoBDI> opcoes, int quantidadeOpcoesVisivel) {

    	// Remove as cotacoes de volume zero
    	opcoes = new ArrayList<CotacaoBDI>(Collections2.filter(opcoes, new Predicate<CotacaoBDI>() {
    		public boolean apply(CotacaoBDI cotacao) {
    			return cotacao.getVolume() > 0L;
    		}
    	}));
    	
    	// Ordena as cotacoes por volume, da mais para a menos volumosa
    	Collections.sort(opcoes, new Comparator<CotacaoBDI>() {
    		public int compare(CotacaoBDI bdi1, CotacaoBDI bdi2) {
    			return Long.signum(bdi2.getVolume() - bdi1.getVolume());
    		}
    	});
    	
    	// Mantem as N primeiras cotacoes
    	opcoes = opcoes.subList(0, Math.min(opcoes.size(), quantidadeOpcoesVisivel));
    	
    	// Ordena agora por strike
    	Collections.sort(opcoes, new Comparator<CotacaoBDI>() {
    		public int compare(CotacaoBDI bdi1, CotacaoBDI bdi2) {
    			return Long.signum(bdi1.getPrecoExercicio() - bdi2.getPrecoExercicio());
    		}
    	});
    	
    	return opcoes;
    }

    private List<CotacaoOpcao> limitarOrdenarCalcularConverter(double s, double r,
			Serie serie, List<CotacaoBDI> cotacoesBDI, CotacaoBDI opcaoTeorica, int quantidadeOpcoesVisivel) {

		cotacoesBDI = limitarOpcoesPorVolumeEOrdenarPorStrike(cotacoesBDI, quantidadeOpcoesVisivel);

		Map<CotacaoBDI, Map<Variavel, Number>> mapaSerieAtual = calcularMapaVariaveisPorOpcao(
				s, r, cotacoesBDI, opcaoTeorica);

		List<CotacaoOpcao> opcoes = new ArrayList<CotacaoOpcao>();
		for (CotacaoBDI cotacaoBDI : cotacoesBDI) {
			opcoes.add(new CotacaoOpcao(serie,
					cotacaoBDI.getCodigoNegociacao(), mapaSerieAtual
							.get(cotacaoBDI)));
		}
		opcoes.add(new CotacaoOpcao(serie, opcaoTeorica.getCodigoNegociacao(), mapaSerieAtual.get(opcaoTeorica)));

		return opcoes;
	}

    public CotacaoAcaoOpcoes calcularIndices(CotacaoBDI acao,
			Serie serieAtualOpcoes, List<CotacaoBDI> cotacoesOpcoesSerieAtual,
			CotacaoBDI opcaoTeoricaSerieAtual, Serie proximaSerieOpcoes,
			List<CotacaoBDI> cotacoesOpcoesProximaSerie,
			CotacaoBDI opcaoTeoricaProximaSerie, int quantidadeOpcoesVisivel, Double selic) {

        // preço da ação cujas opções serão precificadas
        double s = acao.getFechamento() / 100.0;
        // taxa de juros livre de risco
        double r = selic;

        List<CotacaoOpcao> opcoesSerieAtual = limitarOrdenarCalcularConverter(s, r, serieAtualOpcoes, cotacoesOpcoesSerieAtual, opcaoTeoricaSerieAtual, quantidadeOpcoesVisivel);        
        List<CotacaoOpcao> opcoesProximaSerie = limitarOrdenarCalcularConverter(s, r, proximaSerieOpcoes, cotacoesOpcoesProximaSerie, opcaoTeoricaProximaSerie, quantidadeOpcoesVisivel);

        return new CotacaoAcaoOpcoes(new CotacaoAcao(Acao.fromCodigoBDI(acao
				.getCodigoNegociacao()), acao.getFechamento() / 100.0, acao
				.getOscilacao() / 10000.0), opcoesSerieAtual, opcoesProximaSerie);
    }

    /**
     * @param serie serie da opção
     * @param x preço de exercício
     * @param hoje data do pregão virtual de hoje
     * @param dataExercicio data que a opção vencerá
     * @param s preço da ação
     * @param r taxa de juros livre de riscos
     * @param sigma volatilidade implícita
     */
    public CotacaoOpcao calcularOpcaoTeoricaAdhoc(Serie serie, double x, LocalDate hoje, LocalDate dataExercicio, double s, double r, double sigma) {

	long exercicio = dataExercicio.getDayOfMonth() + dataExercicio.getMonthOfYear() * 100L + dataExercicio.getYear() * 10000L;
	CotacaoBDI bdi = new CotacaoBDI(hoje, null, null, "Teórica", 0L, 0L, 0L, (long) Math.round(x*100), exercicio);
	Map<CotacaoBDI, Map<Variavel, Number>> mapaPorOpcao = new HashMap<CotacaoBDI, Map<Variavel,Number>>();
	calcularOpcaoTeorica(mapaPorOpcao, bdi, s, r, sigma);
        return new CotacaoOpcao(serie, "Teórica", mapaPorOpcao.get(bdi));
    }

    /**
     * @param x preço de exercício
     * @param t fração de ano de hoje até a data do exercício
     * @param s preço da ação
     * @param r taxa de juros livre de riscos
     * @param sigma volatilidade implicita
     */
    public double calcularPrecoOpcaoTeorica(double x, double t, double s, double r, double sigma) {
    	double sqrt_t = Math.sqrt(t);
		double d1 = (Math.log(s / x) + (r + sigma * sigma / 2) * t) / (sigma * sqrt_t);
        double d2 = d1 - sigma * sqrt_t;
        return s * probabilidadeAcumulada(d1) - x * Math.exp(-r * t) * probabilidadeAcumulada(d2);
    }

    private void calcularOpcaoTeorica(Map<CotacaoBDI, Map<Variavel, Number>> mapaPorOpcao, CotacaoBDI opcaoTeorica, double s, double r, double sigma) {

        if (opcaoTeorica == null)
            return;

        // preço de exercício
        double x = opcaoTeorica.getPrecoExercicio() / 100.0;
        // Quantidade de dias até o vencimento, em fraçao de ano.
        // Adiciono um dia pois, no dia do vencimento, t não pode
        // ser zero, gerando divisão por zero
        double t = (1 + Days.daysBetween(opcaoTeorica.getDataPregao(),
                    opcaoTeorica.getLocalDateVencimento()).getDays()) / DIAS_POR_ANO;

        double sqrt_t = Math.sqrt(t);
	double d1 = (Math.log(s / x) + (r + sigma * sigma / 2) * t) / (sigma * sqrt_t);
        double d2 = d1 - sigma * sqrt_t;

        double densidadeFuncaoNormal_d1 = densidadeFuncaoNormal(d1);
        double probabilidadeAcumulada_d1 = probabilidadeAcumulada(d1);
        double probabilidadeAcumulada_d2 = probabilidadeAcumulada(d2);
        double exp_r_t = Math.exp(-r * t);

        // valor teórico da opção
	double valorTeorico = s * probabilidadeAcumulada_d1 - x * exp_r_t * probabilidadeAcumulada_d2;
        // grega da variação da opção para cada $1 da ação
        double delta = probabilidadeAcumulada_d1;
        // grega da variação do delta para cada $1 da ação
	double gama = densidadeFuncaoNormal_d1 / (s * sigma * sqrt_t);
        // grega da variação da opção pelo tempo
        double theta = (-(s * densidadeFuncaoNormal_d1 * sigma) / (2 * sqrt_t) - r * x * exp_r_t * probabilidadeAcumulada_d2) / DIAS_POR_ANO;

        mapaPorOpcao.put(opcaoTeorica, criarMapaVariaveis(s, r, t, sigma, valorTeorico, valorTeorico, 0.0, 0.0, x, delta, gama, theta, null, null));
    }

    private void calcularEstatisticas(
            Map<CotacaoBDI, Map<Variavel, Number>> mapaPorOpcao) {

        for (Variavel variavel : Variavel.values()) {
            if (variavel.isIndicador()) {
                SummaryStatistics estatistica = new SummaryStatistics();
                for (Map<Variavel, Number> mapa : mapaPorOpcao.values()) {
                    estatistica.addValue(mapa.get(variavel).doubleValue());
                }
                for (Map<Variavel, Number> mapa : mapaPorOpcao.values()) {
                    Number valor = mapa.get(variavel);
                }
            }
        }
    }

    private Map<Variavel, Number> criarMapaVariaveis(
            double precoAcao, double juros, double tempoParaExpirar,
            double volatilidade,
            double valorTeorico,
            double precoOpcao, double volumeOpcao, double volumeTotalOpcoes,
            double precoExercicio, double delta, double gama, double theta,
            CotacaoBDI opcaoAnterior,
            CotacaoBDI opcaoPosterior) {

        double vi = Math.max(precoAcao - precoExercicio, 0.0);
        double ve = precoOpcao - vi;

        double taxaVE = ve / precoAcao;

        double nv = ve - delta - gama;

        double bosi = volumeTotalOpcoes != 0.0 ? gama * ve * volumeOpcao / volumeTotalOpcoes : 0.0;

        double theVE = taxaVE - (ve + theta) / precoAcao;
        // Se o mini-NV indica que o VE nao é capaz de segurar uma
        // alta de R$ 1, entao o theVE deve ser insignificante
        if (ve - delta - gama / 2 < 0.0) {
            theVE = -theVE;
        }

        double vdx = (precoOpcao / precoAcao) * (120 - DIAS_POR_ANO * tempoParaExpirar) * (precoExercicio - precoAcao);

        double atm = 0.0;
        if (taxaVE > 0.0) {
            atm = Math.max(0, 1 - Math.abs(precoExercicio - precoAcao) / 2.0);
        }

        Map<Variavel, Number> variaveis = new HashMap<Variavel, Number>();
        variaveis.put(STRIKE, precoExercicio);
        variaveis.put(JUROS, juros);
        variaveis.put(FRACAO_ANO_ATE_EXPIRAR, tempoParaExpirar);
        variaveis.put(VOLATILIDADE, volatilidade);
        variaveis.put(PRECO_TEORICO, valorTeorico);
        variaveis.put(PRECO_REAL, precoOpcao);
        variaveis.put(VOLUME, volumeOpcao);
        variaveis.put(DELTA, delta);
        variaveis.put(GAMA, gama);
        variaveis.put(TETA, theta);
        variaveis.put(VALOR_INTRINSICO, vi);
        variaveis.put(VALOR_EXTRINSICO, ve);
        variaveis.put(TAXA_VE, taxaVE);
        variaveis.put(NAO_VENDE, nv);
        variaveis.put(THE_VE, theVE);
        variaveis.put(VDX, vdx);
        variaveis.put(BOSI, bosi);
        variaveis.put(ATM, atm);

        return variaveis;
    }

    private double probabilidadeAcumulada(double x) {
        double prob = Double.NaN;
        try {
            prob = phi.cumulativeProbability(x);
        } catch (MathException e) {
            logger.warn(
                    "Erro no calculo da probabilidade acumulada da distribuicao normal",
                    e);
        }
        return prob;
    }

    private double densidadeFuncaoNormal(double x) {
        return 1.0 / Math.sqrt(2 * Math.PI) * Math.exp(-x * x / 2);
    }
}
