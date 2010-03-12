package br.eti.ranieri.opcoesweb.persistencia;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import br.eti.ranieri.opcoesweb.estado.Acao;
import br.eti.ranieri.opcoesweb.estado.CotacaoAcao;
import br.eti.ranieri.opcoesweb.estado.CotacaoAcaoOpcoes;
import br.eti.ranieri.opcoesweb.estado.CotacaoOpcao;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Transaction;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class Dia implements Comparable<Dia> {

	public static final String KIND = Dia.class.getName();

	public static final String TIMESTAMP = "timestamp";
	public static final String PRECO = "preco";
	public static final String VARIACAO = "variacao";
	public static final String OPCOES_SERIE1 = "opcoesSerie1";
	public static final String OPCOES_SERIE2 = "opcoesSerie2";

	private static final Logger logger = LoggerFactory.getLogger(Dia.class);

	private LocalDate dia;
	private Acao acao;
	private Double preco;
	private Double variacao;
	private Collection<Opcao> opcoesSerie1;
	private Collection<Opcao> opcoesSerie2;

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

	public Dia(Entity entity) {
		this(entity.getKey());

		this.preco = (Double) entity.getProperty(PRECO);
		this.variacao = (Double) entity.getProperty(VARIACAO);

		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		this.opcoesSerie1 = Collections2.transform( //
				keysToEntities(service, (Collection<Key>) entity.getProperty(OPCOES_SERIE1)), //
				new EntityToOpcaoFunction());
		this.opcoesSerie2 = Collections2.transform( //
				keysToEntities(service, (Collection<Key>) entity.getProperty(OPCOES_SERIE2)), //
				new EntityToOpcaoFunction());
	}

	public Dia(Key key) {
		this.dia = extractDiaFromKey(key);
		this.acao = extractAcaoFromKey(key);
	}

	public Dia(LocalDate dia, Acao acao) {
		this.dia = dia;
		this.acao = acao;
	}

	public LocalDate getDia() {
		return dia;
	}

	public Acao getAcao() {
		return acao;
	}

	public void setPreco(Double preco) {
		this.preco = preco;
	}

	public void setVariacao(Double variacao) {
		this.variacao = variacao;
	}

	public Collection<Opcao> getOpcoesSerie1() {
		return opcoesSerie1;
	}

	public void addOpcaoSerie1(Opcao opcao) {
		if (opcoesSerie1 == null) {
			opcoesSerie1 = Sets.newHashSet(opcao);
		} else {
			opcoesSerie1.add(opcao);
		}
	}

	public Collection<Opcao> getOpcoesSerie2() {
		return opcoesSerie2;
	}

	public void addOpcaoSerie2(Opcao opcao) {
		if (opcoesSerie2 == null) {
			opcoesSerie2 = Sets.newHashSet(opcao);
		} else {
			opcoesSerie2.add(opcao);
		}
	}

	public Key toKey() {
		Assert.notNull(dia, "Dia não pode ser nulo");
		Assert.notNull(acao, "Ação não pode ser nula");

		return KeyFactory.createKey(KIND, //
				DATE_FORMAT.format(dia.toDateTimeAtCurrentTime().toDate()) //
						+ "," //
						+ acao.ordinal());
	}

	public Entity toEntity(Collection<Entity> entitiesOpcoesSerie1,
			Collection<Entity> entitiesOpcoesSerie2) {
		Key key = toKey();
		Entity entity = new Entity(key);

		Assert.notNull(acao, "Ação não pode ser nula");
		Assert.notNull(preco, "Preço não pode ser nulo");
		Assert.notNull(variacao, "Variação não pode ser nula");

		entity.setProperty(TIMESTAMP, localDateToYYYYMMDD(this.dia));
		entity.setProperty(PRECO, preco);
		entity.setProperty(VARIACAO, variacao);

		entity.setProperty(OPCOES_SERIE1, Collections2.transform(entitiesOpcoesSerie1,
				new EntityToKeyFunction()));
		entity.setProperty(OPCOES_SERIE2, Collections2.transform(entitiesOpcoesSerie2,
				new EntityToKeyFunction()));

		return entity;
	}

	public CotacaoAcaoOpcoes toCotacaoAcaoOpcoes() {
		CotacaoAcao cotacaoAcao = new CotacaoAcao(this.acao, this.preco, this.variacao);
		List<CotacaoOpcao> opcoesSerie1 = Lists.newArrayList(new TreeSet<CotacaoOpcao>(Collections2
				.transform(this.opcoesSerie1, new OpcaoToCotacaoOpcaoFunction())));
		List<CotacaoOpcao> opcoesSerie2 = Lists.newArrayList(new TreeSet<CotacaoOpcao>(Collections2
				.transform(this.opcoesSerie2, new OpcaoToCotacaoOpcaoFunction())));

		return new CotacaoAcaoOpcoes(cotacaoAcao, opcoesSerie1, opcoesSerie2);
	}

	public int compareTo(Dia outroDia) {
		if (this.acao.ordinal() != outroDia.acao.ordinal()) {
			return this.acao.ordinal() - outroDia.acao.ordinal();
		}
		return this.dia.compareTo(outroDia.dia);
	}

	private Collection<Entity> keysToEntities(DatastoreService service, Collection<Key> keys) {
		Transaction transaction = service.getCurrentTransaction(null);
		if (transaction != null) {
			return service.get(transaction, keys).values();
		} else {
			return service.get(keys).values();
		}
	}

	public static Integer localDateToYYYYMMDD(LocalDate date) {
		Assert.notNull(date, "Data não pode ser nula");
		return date.getYear() * 10000 + date.getMonthOfYear() * 100 + date.getDayOfMonth();
	}

	public static LocalDate extractDiaFromKey(Key key) {
		Assert.notNull(key, "Chave não pode ser nula");
		StringTokenizer tokenizer = new StringTokenizer(key.getName(), ",");
		try {
			return new LocalDate(DATE_FORMAT.parse(tokenizer.nextToken()).getTime());
		} catch (ParseException e) {
			logger.error("Erro na extracao de data na chave [" + key.getName() + "]", e);
			throw new RuntimeException(e);
		}
	}

	public static Acao extractAcaoFromKey(Key key) {
		Assert.notNull(key, "Chave não pode ser nula");
		StringTokenizer tokenizer = new StringTokenizer(key.getName(), ",");
		tokenizer.nextToken();
		Assert.isTrue(tokenizer.hasMoreTokens(), "Chave [" + key.getName()
				+ "] deveria ser composta de duas partes separadas por virgula");
		return Acao.values()[Integer.parseInt(tokenizer.nextToken())];
	}

	private static class OpcaoToCotacaoOpcaoFunction implements Function<Opcao, CotacaoOpcao> {
		public CotacaoOpcao apply(Opcao opcao) {
			return new CotacaoOpcao(opcao.getSerie(), opcao.getCodigo(), opcao.getVariaveis());
		}
	}

	private static class EntityToOpcaoFunction implements Function<Entity, Opcao> {
		public Opcao apply(Entity entity) {
			return new Opcao(entity);
		}
	}

	private static class EntityToKeyFunction implements Function<Entity, Key> {
		public Key apply(Entity entity) {
			return entity.getKey();
		}
	}

}
