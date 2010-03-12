package br.eti.ranieri.opcoesweb.persistencia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

import org.joda.time.LocalDate;
import org.springframework.util.Assert;

import br.eti.ranieri.opcoesweb.estado.Acao;
import br.eti.ranieri.opcoesweb.estado.CotacaoAcaoOpcoes;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Kinds:
 * 
 * Ano   (key(yyyy), meses[])
 * Mes   (key(yyyy-MM), dias[])
 * Dia   (key(yyyy-MM-dd,acao), acao, preco, variacao, opcao1[], opcao2[])
 * Opcao (key, serie, codigo, strike, juros, volatilidade, ...) expando
 *  
 * @author ranieri
 *
 */
public class Persistencia {

	public List<Integer> getAnos() {

	    SortedSet<Integer> anos = Sets.newTreeSet();

	    DatastoreService service = DatastoreServiceFactory.getDatastoreService();
	    Query query = new Query(Ano.KIND);
	    for (Entity entity : service.prepare(query).asIterable()) {
		anos.add(new Long(entity.getKey().getId()).intValue());
	    }

	    return Lists.newArrayList(anos);
	}

	public List<Integer> getMeses(Integer ano) {
		Assert.notNull(ano, "Ano deve ser nao-nulo");
		
		SortedSet<Integer> meses = Sets.newTreeSet();
		
		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		Ano year = new Ano(ano);
		try {
		    Entity yearEntity = service.get(year.toKey());
		    for (Mes mes : new Ano(yearEntity).getMeses()) {
			meses.add(mes.getMes());
		    }
		} catch (EntityNotFoundException e) {
		    return new ArrayList<Integer>();
		}

		return Lists.newArrayList(meses);
	}

	public List<Integer> getDias(Integer ano, Integer mes) {
		Assert.notNull(ano, "Ano deve ser nao-nulo");
		Assert.notNull(mes, "Mes deve ser nao-nulo");

		SortedSet<Integer> dias = Sets.newTreeSet();
		
		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		Mes month = new Mes(ano, mes);
		try {
		    Entity monthEntity = service.get(month.toKey());
		    for (Dia dia : new Mes(monthEntity).getDias()) {
			dias.add(dia.getDia().getDayOfMonth());
		    }
		} catch (EntityNotFoundException e) {
		    return new ArrayList<Integer>();
		}
		
		return Lists.newArrayList(dias);
	}

	public Map<Acao, CotacaoAcaoOpcoes> obterPorData(Integer ano, Integer mes,
			Integer dia) {

		Assert.notNull(ano, "Ano deve ser nao-nulo");
		Assert.notNull(mes, "Mes deve ser nao-nulo");
		Assert.notNull(dia, "Dia deve ser nao-nulo");
		
		Map<Acao, CotacaoAcaoOpcoes> retorno = Maps.newHashMap();
		
		// Chaves dos Dias, cada um de uma Acao
		Set<Key> keys = Sets.newHashSet();
		LocalDate localDate = new LocalDate(ano, mes, dia);
		for (Acao acao : Acao.values()) {
		    keys.add(new Dia(localDate, acao).toKey());
		}
		
		// Procura os Dias pelas chaves, os reconstroi e os converte em CotacaoAcaoOpcoes
		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		for (Entry<Key, Entity> entry : service.get(keys).entrySet()) {
		    CotacaoAcaoOpcoes cotacao = new Dia(entry.getValue()).toCotacaoAcaoOpcoes();
		    retorno.put(cotacao.getCotacaoAcao().getAcao(), cotacao);
		}
		
		return retorno;
	}

	public Map<Acao, CotacaoAcaoOpcoes> obterUltima() {
	    DatastoreService service = DatastoreServiceFactory.getDatastoreService();
	    PreparedQuery query = service.prepare(new Query(Dia.KIND).addSort(Dia.TIMESTAMP, SortDirection.DESCENDING));
	    List<Entity> entities = query.asList(FetchOptions.Builder.withLimit(1));
	    if (entities == null || entities.size() == 0) {
		return Maps.newHashMap();
	    }
	    LocalDate date = new Dia(entities.get(0).getKey()).getDia();
	    return obterPorData(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth());
	}

	public List<Entry<LocalDate,CotacaoAcaoOpcoes>> getCotacoes(Acao acao, LocalDate dataInicial,
			LocalDate dataFinal) {

		List<Entry<LocalDate,CotacaoAcaoOpcoes>> cotacoes = new ArrayList<Entry<LocalDate,CotacaoAcaoOpcoes>>();
		for (LocalDate data : cotacoesHistoricas.keySet()) {
			if (data.isBefore(dataInicial) == false && data.isAfter(dataFinal) == false) {
				cotacoes.add(new SimpleEntry<LocalDate, CotacaoAcaoOpcoes>(data,
						cotacoesHistoricas.get(data).get(acao)));
			}
		}
		return cotacoes;
	}

	public synchronized void incluirCotacaoHistorica(LocalDate data, Acao acao,
			CotacaoAcaoOpcoes cotacoes) {

		Assert.notNull(data);
		Assert.notNull(acao);
		Assert.notNull(cotacoes);

		Map<Acao, CotacaoAcaoOpcoes> mapa = cotacoesHistoricas.get(data);
		if (mapa == null) {
			mapa = new HashMap<Acao, CotacaoAcaoOpcoes>();
			cotacoesHistoricas.put(data, mapa);
		}
		mapa.put(acao, cotacoes);
	}

}
