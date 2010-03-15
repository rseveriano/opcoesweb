package br.eti.ranieri.opcoesweb.persistencia;

import java.util.Map;
import java.util.Map.Entry;

import br.eti.ranieri.opcoesweb.estado.CotacaoOpcao;
import br.eti.ranieri.opcoesweb.estado.Serie;
import br.eti.ranieri.opcoesweb.estado.Variavel;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.common.collect.Maps;

public class Opcao {
	public static final String KIND = Opcao.class.getName();
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

	public Opcao(CotacaoOpcao cotacao) {
		this.serie = cotacao.getSerie();
		this.codigo = cotacao.getCodigo();
		this.variaveis = Maps.newHashMap(cotacao.getVariaveis());
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

	public Entity toEntity(Key parent) {
		Entity entity = new Entity(KIND, parent);

		entity.setProperty(SERIE, serie.ordinal());
		entity.setProperty(CODIGO, codigo);
		for (Entry<Variavel, Number> entry : variaveis.entrySet()) {
			entity.setProperty(VAR_PREFIX + entry.getKey().name(), entry.getValue());
		}

		return entity;
	}
}
