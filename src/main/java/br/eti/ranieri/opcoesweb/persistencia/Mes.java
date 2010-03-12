package br.eti.ranieri.opcoesweb.persistencia;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.util.Assert;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class Mes implements Comparable<Mes> {

	public static final String DIAS = "dias";

	private Integer ano;
	private Integer mes;
	private Set<Dia> dias;

	public Mes(Entity entity) {
		this(entity.getKey());

		this.dias = Sets.newHashSet();
		Collection<Key> keysDias = (Collection<Key>) entity.getProperty(DIAS);
		if (keysDias != null) {
			for (Key key : keysDias) {
				this.dias.add(new Dia(key));
			}
		}
	}

	public Mes(Integer ano, Integer mes) {
		this.ano = ano;
		this.mes = mes;
	}

	public Mes(Key key) {
		this.ano = extractYearFromKey(key);
		this.mes = extractMonthFromKey(key);
	}

	public Integer getAno() {
		return ano;
	}

	public Integer getMes() {
		return mes;
	}

	public Collection<Dia> getDias() {
		return dias;
	}

	public void addDia(Dia dia) {
		if (dias == null) {
			dias = Sets.newHashSet(dia);
		} else {
			dias.add(dia);
		}
	}

	public Key toKey() {
		Assert.notNull(ano, "Ano não pode ser nulo");
		Assert.notNull(mes, "Mes não pode ser nulo");

		return KeyFactory.createKey(getClass().getName(), ano * 100 + mes);
	}

	public Entity toEntity() {
		Key key = toKey();
		Entity entity = new Entity(key);

		List<Key> keysDias = Lists.newArrayList();
		if (dias != null) {
			for (Dia dia : dias) {
				keysDias.add(dia.toKey());
			}
		}
		entity.setProperty(DIAS, keysDias);

		return entity;
	}

	public int compareTo(Mes outroMes) {
		if (this.ano != outroMes.ano) {
			return this.ano - outroMes.ano;
		}
		return this.mes - outroMes.mes;
	}

	public static Integer extractYearFromKey(Key key) {
		Assert.notNull(key, "Chave não pode ser nula");
		return new Long(key.getId() / 100).intValue();
	}

	public static Integer extractMonthFromKey(Key key) {
		Assert.notNull(key, "Chave não pode ser nula");
		return new Long(key.getId() % 100).intValue();
	}

}
