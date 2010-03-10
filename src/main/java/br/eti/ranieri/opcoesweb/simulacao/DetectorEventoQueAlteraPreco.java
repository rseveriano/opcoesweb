package br.eti.ranieri.opcoesweb.simulacao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

import br.eti.ranieri.opcoesweb.estado.CotacaoAcaoOpcoes;
import br.eti.ranieri.opcoesweb.estado.CotacaoOpcao;
import br.eti.ranieri.opcoesweb.estado.Variavel;
import br.eti.ranieri.opcoesweb.persistencia.Persistencia;

@Service
public class DetectorEventoQueAlteraPreco {

    @Autowired
    Persistencia persistencia;

    public List<EventoQueAlteraPreco> getEventosQueAlteramPreco(ConfigSimulacao config) {

	List<EventoQueAlteraPreco> eventos = new ArrayList<EventoQueAlteraPreco>();

	List<Entry<LocalDate, CotacaoAcaoOpcoes>> cotacoes = persistencia.getCotacoes(config.acao,
		config.dataInicial, config.dataFinal);

	for (int i = 0, n = cotacoes.size(); i < n - 1; i++) {
	    LocalDate hoje = cotacoes.get(i).getKey();
	    LocalDate amanha = cotacoes.get(i + 1).getKey();
	    CotacaoAcaoOpcoes cotacaoHoje = cotacoes.get(i).getValue();
	    CotacaoAcaoOpcoes cotacaoAmanha = cotacoes.get(i + 1).getValue();
	    AlteracaoCustodia variacao = calcularConcensoQueStrikeMudou(hoje, cotacaoHoje,
		    cotacaoAmanha);
	    if (variacao != null) {
		eventos.add(new EventoQueAlteraPreco(amanha, variacao));
	    }
	}
	return eventos;
    }

    AlteracaoCustodia calcularConcensoQueStrikeMudou(LocalDate hoje, CotacaoAcaoOpcoes cotacaoHoje,
	    CotacaoAcaoOpcoes cotacaoAmanha) {

	SortedSet<String> intersecao = new TreeSet<String>();
	SortedSet<String> intersecaoHoje = new TreeSet<String>();
	SortedSet<String> intersecaoAmanha = new TreeSet<String>();

	Map<String, CotacaoOpcao> opcaoPorCodigoHoje, opcaoPorCodigoAmanha;
	Function<CotacaoOpcao, String> funcaoExtratoraCodigos = new Function<CotacaoOpcao, String>() {
	    public String apply(CotacaoOpcao cotacao) {
		return cotacao.getCodigo();
	    };
	};
	Collection<String> codigosHojeSerie1 = Collections2.transform(
		cotacaoHoje.getOpcoesSerie1(), funcaoExtratoraCodigos);
	Collection<String> codigosHojeSerie2 = Collections2.transform(
		cotacaoHoje.getOpcoesSerie2(), funcaoExtratoraCodigos);
	Collection<String> codigosAmanhaSerie1 = Collections2.transform(cotacaoAmanha
		.getOpcoesSerie1(), funcaoExtratoraCodigos);
	intersecao.addAll(codigosHojeSerie1);
	// Intersecao entre opcoes de hoje e de amanha
	intersecao.retainAll(codigosAmanhaSerie1);

	// Se a intersecao e' vazia, de hoje para amanha houve exercicio
	if (intersecao.size() < 2) {
	    intersecao.addAll(codigosHojeSerie2);
	    // Intersecao entre opcoes hoje com vencimento no proximo
	    // mes e as opcoes de amanha com vencimento no mes corrente
	    intersecao.retainAll(codigosAmanhaSerie1);

	    intersecaoHoje.addAll(codigosHojeSerie2);
	    intersecaoAmanha.addAll(codigosAmanhaSerie1);
	    opcaoPorCodigoHoje = criarMapaOpcaoPorCodigo(cotacaoHoje.getOpcoesSerie2());
	    opcaoPorCodigoAmanha = criarMapaOpcaoPorCodigo(cotacaoAmanha.getOpcoesSerie1());
	} else {
	    intersecaoHoje.addAll(codigosHojeSerie1);
	    intersecaoAmanha.addAll(codigosAmanhaSerie1);
	    opcaoPorCodigoHoje = criarMapaOpcaoPorCodigo(cotacaoHoje.getOpcoesSerie1());
	    opcaoPorCodigoAmanha = criarMapaOpcaoPorCodigo(cotacaoAmanha.getOpcoesSerie1());
	}

	if (intersecao.size() == 0) {
	    throw new IllegalArgumentException(
		    "Nao encontrei opcoes de mesmo codigo entre hoje e amanha");
	}

	// Calcula a diferenca entre os strikes das opcoes comuns
	intersecaoHoje.retainAll(intersecao);
	intersecaoAmanha.retainAll(intersecao);

	Map<Long, Integer> diferencasPorPreco = new HashMap<Long, Integer>();
	Map<Long, Integer> razoesPorPreco = new HashMap<Long, Integer>();

	for (Iterator<String> iterHoje = intersecaoHoje.iterator(), iterAmanha = intersecaoAmanha
		.iterator(); iterHoje.hasNext() && iterAmanha.hasNext();) {

	    String codigoOpcaoHoje = iterHoje.next();
	    String codigoOpcaoAmanha = iterAmanha.next();
	    assert (codigoOpcaoHoje.equals(codigoOpcaoAmanha));

	    CotacaoOpcao opcaoHoje = opcaoPorCodigoHoje.get(codigoOpcaoHoje);
	    CotacaoOpcao opcaoAmanha = opcaoPorCodigoAmanha.get(codigoOpcaoAmanha);

	    double strikeHoje = opcaoHoje.getVariaveis().get(Variavel.STRIKE).doubleValue();
	    double strikeAmanha = opcaoAmanha.getVariaveis().get(Variavel.STRIKE).doubleValue();
	    double diferenca = strikeHoje - strikeAmanha;
	    double razao = strikeHoje / strikeAmanha;

	    long diferencaNormalizada = Math.round(diferenca * 100);
	    Integer quantidade = diferencasPorPreco.get(diferencaNormalizada);
	    diferencasPorPreco.put(diferencaNormalizada, (quantidade == null) ? 1 : quantidade + 1);

	    long razaoNormalizada = Math.round(razao * 1000);
	    quantidade = razoesPorPreco.get(razaoNormalizada);
	    razoesPorPreco.put(razaoNormalizada, (quantidade == null) ? 1 : quantidade + 1);
	}

	Comparator<Entry<Long, Integer>> comparadorQuantidades = new Comparator<Entry<Long, Integer>>() {
	    public int compare(Entry<Long, Integer> entry1, Entry<Long, Integer> entry2) {
		return entry1.getValue() - entry2.getValue();
	    }
	};
	Entry<Long, Integer> concensoDiferenca = Collections.max(diferencasPorPreco.entrySet(),
		comparadorQuantidades);
	Entry<Long, Integer> concensoRazao = Collections.max(razoesPorPreco.entrySet(),
		comparadorQuantidades);

	if (concensoDiferenca.getValue() > diferencasPorPreco.size() / 2) {
	    if (concensoDiferenca.getKey() != 0L)
		return new AlteracaoCustodia(concensoDiferenca.getKey() / 100.0,
			TipoAlteracaoCustodia.PROVENTOS);
	    return null;
	} else if (concensoRazao.getValue() > razoesPorPreco.size() / 2) {
	    if (concensoRazao.getKey() > 1000L)
		return new AlteracaoCustodia(concensoRazao.getKey() / 1000.0,
			TipoAlteracaoCustodia.SPLIT);
	    else if (concensoRazao.getKey() < 1000L)
		return new AlteracaoCustodia(concensoRazao.getKey() / 1000.0,
			TipoAlteracaoCustodia.MERGE);
	    return null;
	}

	return null;
    }

    Map<String, CotacaoOpcao> criarMapaOpcaoPorCodigo(List<CotacaoOpcao> cotacoes) {
	Map<String, CotacaoOpcao> mapa = new HashMap<String, CotacaoOpcao>();
	for (CotacaoOpcao cotacao : cotacoes) {
	    mapa.put(cotacao.getCodigo(), cotacao);
	}
	return mapa;
    }
}
