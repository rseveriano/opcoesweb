package br.eti.ranieri.opcoesweb.persistencia;

import java.util.Set;

import org.springframework.util.Assert;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.common.collect.Sets;

public class Ano {

	public static final String KIND = Ano.class.getName();
	public static final String MES_PREFIX = "MES_";

	private Integer ano;
	private Set<Integer> meses;

	public Ano(Entity entity) {
		this.ano = extractYearFromKey(entity.getKey());

		this.meses = Sets.newHashSet();
		for (int i = 1; i <= 12; i++) {
			Object property = entity.getProperty(String.format("%s%02d", MES_PREFIX, i));
			if (property != null) {
				meses.add((Integer) property);
			}
		}
	}

	public Ano(Integer ano) {
		this.ano = ano;
	}

	public Integer getAno() {
		return ano;
	}

	public Set<Integer> getMeses() {
		return meses;
	}

	public void addMes(Integer mes) {
		if (meses == null) {
			meses = Sets.newHashSet(mes);
		} else {
			meses.add(mes);
		}
	}

	public Key toKey() {
		Assert.notNull(ano, "Ano não pode ser nulo");
		return KeyFactory.createKey(getClass().getName(), ano);
	}

	public Entity toEntity() {
		Key key = toKey();
		Entity entity = new Entity(key);

		for (Integer mes : meses) {
			entity.setProperty(String.format("%s%02d", MES_PREFIX, mes), mes);
		}

		return entity;
	}

	public Integer extractYearFromKey(Key key) {
		Assert.notNull(key, "Chave não pode ser nula");
		return new Long(key.getId()).intValue();
	}

}
