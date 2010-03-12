package br.eti.ranieri.opcoesweb.persistencia;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.util.Assert;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.common.collect.Lists;

public class Ano {

    public static final String KIND = Ano.class.getName();
    public static final String MESES = "meses";

    private Integer ano;
    private Collection<Mes> meses;

    public Ano(Entity entity) {
	this.ano = extractYearFromKey(entity.getKey());
	
	this.meses = new ArrayList<Mes>();
	Collection<Key> keysMeses = (Collection<Key>) entity.getProperty(MESES);
	if (keysMeses != null) {
	    for (Key key : keysMeses) {
		this.meses.add(new Mes(key));
	    }
	}
    }

    public Ano(Integer ano) {
	this.ano = ano;
    }

    public Integer getAno() {
	return ano;
    }
    
    public Collection<Mes> getMeses() {
	return meses;
    }
    
    public Key toKey() {
	Assert.notNull(ano, "Ano não pode ser nulo");
	return KeyFactory.createKey(getClass().getName(), ano);
    }

    public Entity toEntity() {
	Key key = toKey();
	Entity entity = new Entity(key);

	List<Key> keysMeses = Lists.newArrayList();
	if (meses != null) {
	    for (Mes mes : meses) {
		keysMeses.add(mes.toKey());
	    }
	}
	entity.setProperty(MESES, keysMeses);

	return entity;
    }
    
    public Integer extractYearFromKey(Key key) {
	Assert.notNull(key, "Chave não pode ser nula");
	return new Long(key.getId()).intValue();
    }
}
