package br.eti.ranieri.opcoesweb.persistencia;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

import org.joda.time.LocalDate;
import org.springframework.util.Assert;

import br.eti.ranieri.opcoesweb.estado.Acao;
import br.eti.ranieri.opcoesweb.estado.ConfiguracaoImportacao;
import br.eti.ranieri.opcoesweb.estado.CotacaoAcao;
import br.eti.ranieri.opcoesweb.estado.CotacaoAcaoOpcoes;
import br.eti.ranieri.opcoesweb.estado.CotacaoOpcao;
import br.eti.ranieri.opcoesweb.estado.Serie;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
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
			meses.addAll(new Ano(yearEntity).getMeses());
		} catch (EntityNotFoundException e) {
			return new ArrayList<Integer>();
		}

		return Lists.newArrayList(meses);
	}

	public List<Integer> getDias(Integer ano, Integer mes) {
		Assert.notNull(ano, "Ano deve ser nao-nulo");
		Assert.notNull(mes, "Mes deve ser nao-nulo");

		SortedSet<Integer> dias = Sets.newTreeSet();

		Integer antesDoMes = Dia.localDateToYYYYMMDD(new LocalDate(ano, mes, 1).minusDays(1));
		Integer depoisDoMes = Dia.localDateToYYYYMMDD(new LocalDate(ano, mes, 1).plusMonths(1));

		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		Query query = new Query(Dia.KIND).setKeysOnly() //
				.addFilter(Dia.TIMESTAMP, FilterOperator.GREATER_THAN, antesDoMes) //
				.addFilter(Dia.TIMESTAMP, FilterOperator.LESS_THAN, depoisDoMes);
		Iterables.addAll(dias, Iterables.transform(service.prepare(query).asIterable(),
				new KeyToDayOfMonthFunction()));

		return Lists.newArrayList(dias);
	}

	public Map<Acao, CotacaoAcaoOpcoes> obterPorData(Integer year, Integer month, Integer dayOfMonth) {

		Assert.notNull(year, "Ano deve ser nao-nulo");
		Assert.notNull(month, "Mes deve ser nao-nulo");
		Assert.notNull(dayOfMonth, "Dia deve ser nao-nulo");

		Map<Acao, CotacaoAcaoOpcoes> retorno = Maps.newHashMap();

		Integer yyyyMMdd = Dia.localDateToYYYYMMDD(new LocalDate(year, month, dayOfMonth));
		Key yearKey = new Ano(year).toKey();

		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		Query query = new Query(Dia.KIND, yearKey) //
				.addFilter(Dia.TIMESTAMP, FilterOperator.EQUAL, yyyyMMdd);

		for (Dia dia : Iterables.transform(service.prepare(query).asIterable(),
				new EntityToDiaFunction())) {

			Multimap<Serie, Opcao> opcaoPorSerieMap = ArrayListMultimap.create(2,
					ConfiguracaoImportacao.QUANTIDADE_MAXIMA_OPCOES_POR_ACAO_POR_DIA + 1);

			Query optionQuery = new Query(Opcao.KIND, dia.getKey());
			for (Entity optionEntity : service.prepare(optionQuery).asIterable()) {
				Opcao opcao = new Opcao(optionEntity);
				opcaoPorSerieMap.put(opcao.getSerie(), opcao);
			}

			retorno.put(dia.getAcao(), toCotacaoAcaoOpcoes(dia, opcaoPorSerieMap, yyyyMMdd));
		}

		return retorno;
	}

	public Map<Acao, CotacaoAcaoOpcoes> obterUltima() {
		Ano ultimoAno = new Ano(new TreeSet<Integer>(getAnos()).last());
		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		PreparedQuery query = service.prepare(new Query(Dia.KIND, ultimoAno.toKey()) //
				.setKeysOnly().addSort(Dia.TIMESTAMP, SortDirection.DESCENDING));
		List<Entity> entities = query.asList(FetchOptions.Builder.withLimit(1));
		if (entities == null || entities.size() == 0) {
			System.err.println("Nao havia nenhum registro na ordem decrescente");
			return Maps.newHashMap();
		}
		LocalDate date = new Dia(entities.get(0).getKey()).getDia();
		System.err.println("Vamos obter a ultima cotacao pela data " + date);
		return obterPorData(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth());
	}

	public List<Entry<LocalDate, CotacaoAcaoOpcoes>> getCotacoes(Acao acao, LocalDate dataInicial,
			LocalDate dataFinal) {

		List<Entry<LocalDate, CotacaoAcaoOpcoes>> retorno = Lists.newArrayList();
		Integer yyyyMMddInicial = Dia.localDateToYYYYMMDD(dataInicial);
		Integer yyyyMMddFinal = Dia.localDateToYYYYMMDD(dataFinal);

		DatastoreService service = DatastoreServiceFactory.getDatastoreService();

		Query query = new Query(Dia.KIND) //
				.addFilter(Dia.TIMESTAMP, FilterOperator.GREATER_THAN_OR_EQUAL, yyyyMMddInicial) //
				.addFilter(Dia.TIMESTAMP, FilterOperator.LESS_THAN_OR_EQUAL, yyyyMMddFinal) //
				.addSort(Dia.TIMESTAMP, SortDirection.ASCENDING);

		for (Dia dia : Iterables.transform(service.prepare(query).asIterable(),
				new EntityToDiaFunction())) {

			if (acao.equals(dia.getAcao()) == false) {
				continue;
			}

			Integer yyyyMMdd = Dia.localDateToYYYYMMDD(dia.getDia());
			Multimap<Serie, Opcao> opcaoPorSerieMap = ArrayListMultimap.create(2,
					ConfiguracaoImportacao.QUANTIDADE_MAXIMA_OPCOES_POR_ACAO_POR_DIA + 1);

			Query optionQuery = new Query(Opcao.KIND, dia.getKey());
			for (Entity optionEntity : service.prepare(optionQuery).asIterable()) {
				Opcao opcao = new Opcao(optionEntity);
				opcaoPorSerieMap.put(opcao.getSerie(), opcao);
			}

			retorno.add(new SimpleEntry<LocalDate, CotacaoAcaoOpcoes>(dia.getDia(),
					toCotacaoAcaoOpcoes(dia, opcaoPorSerieMap, yyyyMMdd)));
		}

		return retorno;
	}

	public void incluirCotacaoHistorica(LocalDate data, Acao acao, CotacaoAcaoOpcoes cotacoes) {

		Assert.notNull(data);
		Assert.notNull(acao);
		Assert.notNull(cotacoes);

		DatastoreService service = DatastoreServiceFactory.getDatastoreService();

		// Salva o ano com o mes dentro, se não existir ainda
		Ano ano;
		try {
			ano = new Ano(service.get(new Ano(data.getYear()).toKey()));
			if (ano.getMeses() == null || ano.getMeses().contains(data.getMonthOfYear()) == false) {
				ano.addMes(data.getMonthOfYear());
				service.put(ano.toEntity());
			}
		} catch (EntityNotFoundException e) {
			ano = new Ano(data.getYear());
			ano.addMes(data.getMonthOfYear());
			service.put(ano.toEntity());
		}

		// Salva o dia sobrescrevendo preço e variação, sobrescreve se existir
		Dia dia;
		try {
			Key diaKey = new Dia(data, acao).toKey(ano.getKey());
			Entity diaEntity = service.get(diaKey);
			dia = new Dia(diaEntity);
		} catch (EntityNotFoundException e) {
			dia = new Dia(data, acao);
		}

		dia.setPreco(cotacoes.getCotacaoAcao().getPrecoAcao());
		dia.setVariacao(cotacoes.getCotacaoAcao().getVariacaoAcao());
		service.put(dia.toEntity(ano.getKey()));

		// Remove as opções antigas
		Query optionToDeleteQuery = new Query(Opcao.KIND, dia.getKey()).setKeysOnly();
		service.delete(Iterables.transform(service.prepare(optionToDeleteQuery).asIterable(),
				new EntityToKeyFunction()));

		// Salva as novas opções
		service.put(Collections2.transform(cotacoes.getOpcoesSerie1(),
				new CotacaoOpcaoToEntityFunction(dia.getKey())));
		service.put(Collections2.transform(cotacoes.getOpcoesSerie2(),
				new CotacaoOpcaoToEntityFunction(dia.getKey())));
	}

	private CotacaoAcaoOpcoes toCotacaoAcaoOpcoes(Dia dia, Multimap<Serie, Opcao> opcaoPorSerieMap,
			Integer yyyyMMdd) {
		SortedSet<Serie> series = Sets.newTreeSet(new Comparator<Serie>() {
			public int compare(Serie serie1, Serie serie2) {
				int delta = serie1.ordinal() - serie2.ordinal();
				return (Math.abs(delta) > 1) ? -delta : delta;
			}
		});
		series.addAll(opcaoPorSerieMap.keySet());
		Assert.isTrue(series.size() == 2, String.format( //
				"Foram encontrados as series [%s] no dia %d", //
				series.toString(), yyyyMMdd));

		List<CotacaoOpcao> opcoesSerie1 = Lists.newArrayList();
		List<CotacaoOpcao> opcoesSerie2 = Lists.newArrayList();

		Iterator<Serie> iterator = series.iterator();
		Serie serie1 = iterator.next();
		for (Opcao opcao : opcaoPorSerieMap.get(serie1)) {
			opcoesSerie1.add(new CotacaoOpcao(serie1, opcao.getCodigo(), opcao.getVariaveis()));
		}
		Serie serie2 = iterator.next();
		for (Opcao opcao : opcaoPorSerieMap.get(serie2)) {
			opcoesSerie2.add(new CotacaoOpcao(serie2, opcao.getCodigo(), opcao.getVariaveis()));
		}

		CotacaoAcao cotacaoAcao = new CotacaoAcao(dia.getAcao(), dia.getPreco(), dia.getVariacao());

		return new CotacaoAcaoOpcoes(cotacaoAcao, opcoesSerie1, opcoesSerie2);
	}

	private static class CotacaoOpcaoToEntityFunction implements Function<CotacaoOpcao, Entity> {
		private final Key dayKey;

		private CotacaoOpcaoToEntityFunction(Key dayKey) {
			this.dayKey = dayKey;
		}

		public Entity apply(CotacaoOpcao cotacao) {
			return new Opcao(cotacao).toEntity(dayKey);
		}
	}

	private static class KeyToDayOfMonthFunction implements Function<Entity, Integer> {
		public Integer apply(Entity entity) {
			return new Dia(entity.getKey()).getDia().getDayOfMonth();
		}
	}

	private static class EntityToDiaFunction implements Function<Entity, Dia> {
		public Dia apply(Entity entity) {
			return new Dia(entity);
		}
	}

	private static class EntityToKeyFunction implements Function<Entity, Key> {
		public Key apply(Entity entity) {
			return entity.getKey();
		}
	}
}
