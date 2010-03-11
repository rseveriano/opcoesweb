package br.eti.ranieri.opcoesweb.persistencia;

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
}
