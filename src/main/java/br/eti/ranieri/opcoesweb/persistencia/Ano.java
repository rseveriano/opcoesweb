package br.eti.ranieri.opcoesweb.persistencia;

import java.util.Collection;
import java.util.List;

import org.springframework.util.Assert;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.common.collect.Lists;

public class Ano {

    public static final String MESES = "meses";

    private Integer ano;
    private Collection<Mes> meses;

    public Entity toEntity() {

	Assert.notNull(ano, "Ano não pode ser nulo");

	Key key = KeyFactory.createKey(getClass().getName(), ano);
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
}
