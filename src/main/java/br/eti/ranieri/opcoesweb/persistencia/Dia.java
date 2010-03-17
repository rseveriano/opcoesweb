package br.eti.ranieri.opcoesweb.persistencia;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import br.eti.ranieri.opcoesweb.estado.Acao;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

public class Dia implements Comparable<Dia> {

	public static final String KIND = Dia.class.getName();

	public static final String TIMESTAMP = "timestamp";
	public static final String PRECO = "preco";
	public static final String VARIACAO = "variacao";
	public static final String OPCOES_SERIE1 = "opcoesSerie1";
	public static final String OPCOES_SERIE2 = "opcoesSerie2";

	private static final Logger logger = LoggerFactory.getLogger(Dia.class);

	private Key key;
	private LocalDate dia;
	private Acao acao;
	private Double preco;
	private Double variacao;

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

	public Dia(Entity entity) {
		this(entity.getKey());

		this.preco = (Double) entity.getProperty(PRECO);
		this.variacao = (Double) entity.getProperty(VARIACAO);
	}

	public Dia(Key key) {
		this.key = key;
		this.dia = extractDiaFromKey(key);
		this.acao = extractAcaoFromKey(key);
	}

	public Dia(LocalDate dia, Acao acao) {
		this.dia = dia;
		this.acao = acao;
	}

	public Key getKey() {
		return key;
	}

	public LocalDate getDia() {
		return dia;
	}

	public Acao getAcao() {
		return acao;
	}

	public Double getPreco() {
		return preco;
	}

	public void setPreco(Double preco) {
		this.preco = preco;
	}

	public Double getVariacao() {
		return variacao;
	}
	
	public void setVariacao(Double variacao) {
		this.variacao = variacao;
	}

	public Key toKey(Key parent) {
		Assert.notNull(dia, "Dia não pode ser nulo");
		Assert.notNull(acao, "Ação não pode ser nula");

		return KeyFactory.createKey(parent, KIND, //
				DATE_FORMAT.format(dia.toDateTimeAtStartOfDay(DateTimeZone.UTC).toDate())
						+ ","
						+ acao.ordinal());
	}

	public Entity toEntity(Key parent) {
		this.key = toKey(parent);
		Entity entity = new Entity(key);

		Assert.notNull(acao, "Ação não pode ser nula");
		Assert.notNull(preco, "Preço não pode ser nulo");
		Assert.notNull(variacao, "Variação não pode ser nula");

		entity.setProperty(TIMESTAMP, localDateToYYYYMMDD(this.dia));
		entity.setProperty(PRECO, preco);
		entity.setProperty(VARIACAO, variacao);

		return entity;
	}

	public int compareTo(Dia outroDia) {
		if (this.acao.ordinal() != outroDia.acao.ordinal()) {
			return this.acao.ordinal() - outroDia.acao.ordinal();
		}
		return this.dia.compareTo(outroDia.dia);
	}

	public static Integer localDateToYYYYMMDD(LocalDate date) {
		Assert.notNull(date, "Data não pode ser nula");
		return date.getYear() * 10000 + date.getMonthOfYear() * 100 + date.getDayOfMonth();
	}

	public static LocalDate extractDiaFromKey(Key key) {
		Assert.notNull(key, "Chave não pode ser nula");
		StringTokenizer tokenizer = new StringTokenizer(key.getName(), ",");
		try {
			Date date = DATE_FORMAT.parse(tokenizer.nextToken());
			return new DateTime(date.getTime(), DateTimeZone.UTC).toLocalDate();
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

}
