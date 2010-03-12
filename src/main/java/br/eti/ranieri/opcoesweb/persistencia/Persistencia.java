package br.eti.ranieri.opcoesweb.persistencia;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import br.eti.ranieri.opcoesweb.estado.Acao;
import br.eti.ranieri.opcoesweb.estado.CotacaoAcaoOpcoes;
import br.eti.ranieri.opcoesweb.estado.CotacaoOpcao;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Kinds:
 * 
 * Ano (key(yyyy), meses[])
 * 
 * Mes (key(yyyy-MM), dias[])
 * 
 * Dia (key(yyyy-MM-dd,acao), acao, preco, variacao, opcao1[], opcao2[])
 * 
 * Opcao (key, serie, codigo, strike, juros, volatilidade, ...) expando
 * 
 * @author ranieri
 * 
 */
public class Persistencia {

	private final Logger logger = LoggerFactory.getLogger(getClass());

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

	public Map<Acao, CotacaoAcaoOpcoes> obterPorData(Integer ano, Integer mes, Integer dia) {

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

		// Procura os Dias pelas chaves, os reconstroi e os converte em
		// CotacaoAcaoOpcoes
		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		for (Entry<Key, Entity> entry : service.get(keys).entrySet()) {
			CotacaoAcaoOpcoes cotacao = new Dia(entry.getValue()).toCotacaoAcaoOpcoes();
			retorno.put(cotacao.getCotacaoAcao().getAcao(), cotacao);
		}

		return retorno;
	}

	public Map<Acao, CotacaoAcaoOpcoes> obterUltima() {
		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		PreparedQuery query = service.prepare(new Query(Dia.KIND).addSort(Dia.TIMESTAMP,
				SortDirection.DESCENDING));
		List<Entity> entities = query.asList(FetchOptions.Builder.withLimit(1));
		if (entities == null || entities.size() == 0) {
			return Maps.newHashMap();
		}
		LocalDate date = new Dia(entities.get(0).getKey()).getDia();
		return obterPorData(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth());
	}

	public List<Entry<LocalDate, CotacaoAcaoOpcoes>> getCotacoes(Acao acao, LocalDate dataInicial,
			LocalDate dataFinal) {

		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		List<Entry<LocalDate, CotacaoAcaoOpcoes>> cotacoes = Lists.newArrayList();
		while (dataInicial.isBefore(dataFinal) || dataInicial.equals(dataFinal)) {

			try {
				Entity entity = service.get(new Dia(dataInicial, acao).toKey());
				cotacoes.add( //
						new SimpleEntry<LocalDate, CotacaoAcaoOpcoes>(dataInicial, //
								new Dia(entity).toCotacaoAcaoOpcoes()));
			} catch (EntityNotFoundException e) {
				if (logger.isDebugEnabled())
					logger.debug("Não encontrei cotacao para ação " + acao.getCodigo()
							+ " na data " + dataInicial, e);
			}

			dataInicial = dataInicial.plusDays(1);
		}

		return cotacoes;
	}

	public void incluirCotacaoHistorica(LocalDate data, Acao acao, CotacaoAcaoOpcoes cotacoes) {

		Assert.notNull(data);
		Assert.notNull(acao);
		Assert.notNull(cotacoes);

		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		Transaction transaction = service.beginTransaction();

		boolean persistAno = false;
		boolean persistMes = false;

		Ano ano;
		try {
			ano = new Ano(service.get(transaction, new Ano(data.getYear()).toKey()));
		} catch (EntityNotFoundException e) {
			ano = new Ano(data.getYear());
			persistAno = true;
		}

		Mes mes;
		try {
			mes = new Mes(service.get(transaction, new Mes(data.getYear(), data.getMonthOfYear())
					.toKey()));
		} catch (EntityNotFoundException e) {
			mes = new Mes(data.getYear(), data.getMonthOfYear());
			persistMes = true;
		}

		Dia dia;
		try {
			dia = new Dia(service.get(transaction, new Dia(data, acao).toKey()));
		} catch (EntityNotFoundException e) {
			dia = new Dia(data, acao);
		}

		// pendura o mes no ano
		if (ano.getMeses() != null) {
			if (new TreeSet<Mes>(ano.getMeses()).contains(mes) == false) {
				ano.getMeses().add(mes);
				persistAno = true;
			}
		} else {
			ano.addMes(mes);
			persistAno = true;
		}

		// pendura o dia no mes
		if (mes.getDias() != null) {
			if (new TreeSet<Dia>(mes.getDias()).contains(dia) == false) {
				mes.getDias().add(dia);
				persistMes = true;
			}
		} else {
			mes.addDia(dia);
			persistMes = true;
		}

		// atualiza o Dia pois será persistido incondicionalmente
		dia.setPreco(cotacoes.getCotacaoAcao().getPrecoAcao());
		dia.setVariacao(cotacoes.getCotacaoAcao().getVariacaoAcao());
		// apaga as opcoes antigas antes de escrever as novas
		if (dia.getOpcoesSerie1() != null) {
			service.delete(transaction, Collections2.transform(dia.getOpcoesSerie1(),
					new OpcaoToKeyFunction()));
		}
		if (dia.getOpcoesSerie2() != null) {
			service.delete(transaction, Collections2.transform(dia.getOpcoesSerie2(),
					new OpcaoToKeyFunction()));
		}
		// inclui novas opcoes
		for (CotacaoOpcao cotacao : cotacoes.getOpcoesSerie1()) {
			dia.addOpcaoSerie1(new Opcao(cotacao));
		}
		for (CotacaoOpcao cotacao : cotacoes.getOpcoesSerie2()) {
			dia.addOpcaoSerie2(new Opcao(cotacao));
		}

		// Salva quem for necessario
		if (persistAno) {
			service.put(transaction, ano.toEntity());
		}
		if (persistMes) {
			service.put(transaction, mes.toEntity());
		}
		Collection<Entity> entitiesOpcoesSerie1 = Collections2.transform(dia.getOpcoesSerie1(),
				new OpcaoToEntityFunction());
		Collection<Entity> entitiesOpcoesSerie2 = Collections2.transform(dia.getOpcoesSerie2(),
				new OpcaoToEntityFunction());
		service.put(transaction, entitiesOpcoesSerie1);
		service.put(transaction, entitiesOpcoesSerie2);
		service.put(transaction, dia.toEntity(entitiesOpcoesSerie1, entitiesOpcoesSerie2));

		transaction.commit();
	}

	private static class OpcaoToEntityFunction implements Function<Opcao, Entity> {
		public Entity apply(Opcao opcao) {
			return opcao.toEntity();
		}
	}

	private static class OpcaoToKeyFunction implements Function<Opcao, Key> {
		public Key apply(Opcao opcao) {
			return opcao.getKey();
		}
	}
}
