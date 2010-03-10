package br.eti.ranieri.opcoesweb.simulacao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.eti.ranieri.opcoesweb.estado.CotacaoAcaoOpcoes;
import br.eti.ranieri.opcoesweb.estado.CotacaoOpcao;
import br.eti.ranieri.opcoesweb.estado.Variavel;
import br.eti.ranieri.opcoesweb.persistencia.Persistencia;
import br.eti.ranieri.opcoesweb.simulacao.CotacoesPorOpcao.PrecoDataCodigo;
import br.eti.ranieri.opcoesweb.simulacao.DataOpcoes.CodigoVariaveis;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

@Service
public class SimuladorManual {

    private static final CotacaoOpcao OPCAO_INEXISTENTE_QUE_MANTEM_A_CARTEIRA = new CotacaoOpcao(null, "hold", null);

    @Autowired
    Persistencia persistencia;

    public List<DataOpcoes> getOpcoesPorData(ConfigSimulacao config) {
	
	List<DataOpcoes> dataOpcoes = new ArrayList<DataOpcoes>();
	List<Entry<LocalDate, CotacaoAcaoOpcoes>> cotacoes = persistencia.getCotacoes(config.acao,
		config.dataInicial, config.dataFinal);
	for (Entry<LocalDate, CotacaoAcaoOpcoes> cotacao : cotacoes) {
	    double precoAcao = cotacao.getValue().getCotacaoAcao().getPrecoAcao();
	    Collection<CotacaoOpcao> opcoes = new ArrayList<CotacaoOpcao>();
	    opcoes.addAll(cotacao.getValue().getOpcoesSerie1());
	    opcoes.addAll(cotacao.getValue().getOpcoesSerie2());
	    opcoes = Collections2.filter(opcoes, new Predicate<CotacaoOpcao>() {
		public boolean apply(CotacaoOpcao opcao) {
		    return opcao.getVariaveis().get(Variavel.VOLUME).doubleValue() > 0.0;
		}
	    });
	    List<CotacaoOpcao> opcaoList = new ArrayList<CotacaoOpcao>(opcoes);
	    Collections.sort(opcaoList, new Comparator<CotacaoOpcao>() {
		public int compare(CotacaoOpcao opcao1, CotacaoOpcao opcao2) {
		    int serieCmp = opcao1.getSerie().compareTo(opcao2.getSerie());
		    if (serieCmp == 0)
			return Double.compare( //
				opcao1.getVariaveis().get(Variavel.STRIKE).doubleValue(), //
				opcao2.getVariaveis().get(Variavel.STRIKE).doubleValue());
		    return serieCmp;
		}
	    });
	    opcaoList.add(0, SimuladorManual.OPCAO_INEXISTENTE_QUE_MANTEM_A_CARTEIRA);
	    dataOpcoes.add(new DataOpcoes(precoAcao, cotacao.getKey(), opcaoList));
	}

	return dataOpcoes;
    }

    @Deprecated
    public List<LocalDate> getDatesInRange(ConfigSimulacao config) {
	List<LocalDate> datas = new ArrayList<LocalDate>();
	List<Entry<LocalDate, CotacaoAcaoOpcoes>> cotacoes = persistencia.getCotacoes(config.acao,
		config.dataInicial, config.dataFinal);
	for (Entry<LocalDate, CotacaoAcaoOpcoes> cotacao : cotacoes) {
	    datas.add(cotacao.getKey());
	}
	return datas;
    }

    @Deprecated
    public List<CotacoesPorOpcao> getListaCotacoesPorOpcao(ConfigSimulacao config) {
	List<CotacoesPorOpcao> lista = new ArrayList<CotacoesPorOpcao>();
	List<Entry<LocalDate, CotacaoAcaoOpcoes>> entries = persistencia.getCotacoes(config.acao,
		config.dataInicial, config.dataFinal);

	SortedMap<CotacaoOpcao, Map<LocalDate, PrecoDataCodigo>> precosPorDatasPorOpcao = new TreeMap<CotacaoOpcao, Map<LocalDate, PrecoDataCodigo>>(
		new Comparator<CotacaoOpcao>() {
		    public int compare(CotacaoOpcao o1, CotacaoOpcao o2) {
			int cmp = o1.getSerie().compareTo(o2.getSerie());
			if (cmp == 0)
			    return Double.compare(o1.getVariaveis().get(Variavel.STRIKE)
				    .doubleValue(), o2.getVariaveis().get(Variavel.STRIKE)
				    .doubleValue());
			return cmp;
		    }
		});

	for (Entry<LocalDate, CotacaoAcaoOpcoes> entry : entries) {
	    LocalDate data = entry.getKey();
	    CotacaoAcaoOpcoes cotacaoAcaoOpcoes = entry.getValue();

	    popularMapa(data, precosPorDatasPorOpcao, cotacaoAcaoOpcoes.getOpcoesSerie1());
	    popularMapa(data, precosPorDatasPorOpcao, cotacaoAcaoOpcoes.getOpcoesSerie2());
	}

	for (CotacaoOpcao opcao : precosPorDatasPorOpcao.keySet()) {
	    Map<LocalDate, PrecoDataCodigo> precosPorData = precosPorDatasPorOpcao.get(opcao);
	    
	    List<PrecoDataCodigo> precos = new ArrayList<PrecoDataCodigo>();
	    for (Entry<LocalDate, CotacaoAcaoOpcoes> entry : entries) {
		precos.add(precosPorData.get(entry.getKey()));
	    }

	    lista.add(new CotacoesPorOpcao(opcao, precos));
	}

	return lista;
    }

    @Deprecated
    void popularMapa(LocalDate data, SortedMap<CotacaoOpcao, Map<LocalDate, PrecoDataCodigo>> precosPorDatasPorOpcao,
	    List<CotacaoOpcao> opcoes) {

	for (CotacaoOpcao cotacaoOpcao : opcoes) {
	    // Não popula o mapa se for opção teórica
	    if (cotacaoOpcao.getVariaveis().get(Variavel.VOLUME).doubleValue() == 0.0)
		continue;
	    Map<LocalDate, PrecoDataCodigo> precosPorData = precosPorDatasPorOpcao.get(cotacaoOpcao);
	    if (precosPorData == null) {
		precosPorData = new HashMap<LocalDate, PrecoDataCodigo>();
		precosPorDatasPorOpcao.put(cotacaoOpcao, precosPorData);
	    }
	    double preco = cotacaoOpcao.getVariaveis().get(Variavel.PRECO_REAL).doubleValue();
	    String codigo = cotacaoOpcao.getCodigo();
	    precosPorData.put(data, new PrecoDataCodigo(preco, data, codigo));
	}
    }

    public boolean isOpcaoQueMantemCarteira(CodigoVariaveis codigoVariaveis) {
	return OPCAO_INEXISTENTE_QUE_MANTEM_A_CARTEIRA.getCodigo().equals(codigoVariaveis.codigo);
    }
}
