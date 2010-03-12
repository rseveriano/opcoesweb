package br.eti.ranieri.opcoesweb.persistencia;

import java.util.Map;
import java.util.Map.Entry;

import br.eti.ranieri.opcoesweb.estado.Serie;
import br.eti.ranieri.opcoesweb.estado.Variavel;

import com.google.appengine.api.datastore.Entity;
import com.google.common.collect.Maps;

public class Opcao {
    public static final String SERIE = "serie";
    public static final String CODIGO = "codigo";
    public static final String VAR_PREFIX = "VAR_";
    
    private Serie serie;
    private String codigo;
    private Map<Variavel, Number> variaveis;
    
    public Opcao(Entity entity) {
	this.serie = Serie.values()[(Integer) entity.getProperty(SERIE)];
	this.codigo = (String) entity.getProperty(CODIGO);
	
	this.variaveis = Maps.newHashMap();
	for (Variavel variavel : Variavel.values()) {
	    variaveis.put(variavel, (Double) entity.getProperty(VAR_PREFIX + variavel.name()));
	}
    }

    public Serie getSerie() {
        return serie;
    }


    public String getCodigo() {
        return codigo;
    }

    public Map<Variavel, Number> getVariaveis() {
	return variaveis;
    }

    public Entity toEntity() {
	Entity entity = new Entity(getClass().getName());
	
	entity.setProperty(SERIE, serie.ordinal());
	entity.setProperty(CODIGO, codigo);
	for (Entry<Variavel, Number> entry : variaveis.entrySet()) {
	    entity.setProperty(VAR_PREFIX + entry.getKey().name(), entry.getValue());
	}
	
	return entity;
    }
}
