package br.eti.ranieri.opcoesweb.persistencia;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
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
import com.google.appengine.api.datastore.Transaction;
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
		
		for (Dia dia : Iterables.transform(service.prepare(query).asIterable(), new EntityToDiaFunction())) {
			
			CotacaoAcao cotacaoAcao = new CotacaoAcao(dia.getAcao(), dia.getPreco(), dia.getVariacao());
			
			Multimap<Serie, Opcao> opcaoPorSerieMap = ArrayListMultimap.create(2,
					ConfiguracaoImportacao.QUANTIDADE_MAXIMA_OPCOES_POR_ACAO_POR_DIA + 1);
			List<CotacaoOpcao> opcoesSerie1 = Lists.newArrayList();
			List<CotacaoOpcao> opcoesSerie2 = Lists.newArrayList();
			
			Query optionQuery = new Query(Opcao.KIND, dia.getKey());
			for (Entity optionEntity : service.prepare(optionQuery).asIterable()) {
				Opcao opcao = new Opcao(optionEntity);
				opcaoPorSerieMap.put(opcao.getSerie(), opcao);
			}
			
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
			
			Iterator<Serie> iterator = series.iterator();
			Serie serie1 = iterator.next();
			for (Opcao opcao : opcaoPorSerieMap.get(serie1)) {
				opcoesSerie1.add(new CotacaoOpcao(serie1, opcao.getCodigo(), opcao.getVariaveis()));
			}
			Serie serie2 = iterator.next();
			for (Opcao opcao : opcaoPorSerieMap.get(serie2)) {
				opcoesSerie2.add(new CotacaoOpcao(serie2, opcao.getCodigo(), opcao.getVariaveis()));
			}

			retorno.put(dia.getAcao(), new CotacaoAcaoOpcoes(cotacaoAcao, opcoesSerie1, opcoesSerie2));
		}

		return retorno;
	}

	/**
	 * TODO: refatore a partir daqui...
	 */
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
		
		boolean persistAno = false;
		boolean persistMes = false;

		Transaction anoTransaction = service.beginTransaction();
		Ano ano;
		try {
			Entity anoEntity = service.get(anoTransaction, new Ano(data.getYear()).toKey());
			System.out.println("Encontrei um ano com key = " + anoEntity.getKey().getId());
			ano = new Ano(anoEntity);
		} catch (EntityNotFoundException e) {
			System.out.println("Nao encontrei um ano, construirei com o numero " + data.getYear());
			ano = new Ano(data.getYear());
			persistAno = true;
		}

		Transaction mesTransaction = service.beginTransaction();
		Mes mes;
		try {
			Entity mesEntity = service.get(mesTransaction, new Mes(data.getYear(), data.getMonthOfYear())
					.toKey());
			System.out.println("Encontrei um mes com key = " + mesEntity.getKey().getId());
			mes = new Mes(mesEntity);
		} catch (EntityNotFoundException e) {
			mes = new Mes(data.getYear(), data.getMonthOfYear());
			persistMes = true;
		}

		Transaction diaTransaction = service.beginTransaction();
		Dia dia;
		try {
			dia = new Dia(service.get(diaTransaction, new Dia(data, acao).toKey()));
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
			service.delete(Collections2.transform(dia.getOpcoesSerie1(),
					new OpcaoToKeyFunction()));
		}
		if (dia.getOpcoesSerie2() != null) {
			service.delete(Collections2.transform(dia.getOpcoesSerie2(),
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
			service.put(ano.toEntity());
		}
		if (persistMes) {
			service.put(mes.toEntity());
		}
		Collection<Entity> entitiesOpcoesSerie1 = Collections2.transform(dia.getOpcoesSerie1(),
				new OpcaoToEntityFunction());
		Collection<Entity> entitiesOpcoesSerie2 = Collections2.transform(dia.getOpcoesSerie2(),
				new OpcaoToEntityFunction());
		service.put(entitiesOpcoesSerie1);
		service.put(entitiesOpcoesSerie2);
		service.put(dia.toEntity(entitiesOpcoesSerie1, entitiesOpcoesSerie2));
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
}
