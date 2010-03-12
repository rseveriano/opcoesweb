package br.eti.ranieri.opcoesweb.persistencia;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.util.Assert;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.common.collect.Lists;

public class Mes {

    public static final String DIAS = "dias";

    private Integer ano;
    private Integer mes;
    private Collection<Dia> dias;

    public Mes(Entity entity) {
	this(entity.getKey());
	
	this.dias = new ArrayList<Dia>();
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

    public static Integer extractYearFromKey(Key key) {
	Assert.notNull(key, "Chave não pode ser nula");
	return new Long(key.getId() / 100).intValue();
    }

    public static Integer extractMonthFromKey(Key key) {
	Assert.notNull(key, "Chave não pode ser nula");
	return new Long(key.getId() % 100).intValue();
    }

}
