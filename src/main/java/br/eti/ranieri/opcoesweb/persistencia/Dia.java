package br.eti.ranieri.opcoesweb.persistencia;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;

import org.joda.time.LocalDate;
import org.springframework.util.Assert;

import br.eti.ranieri.opcoesweb.estado.Acao;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.common.collect.Lists;

public class Dia {
    public static final String ACAO = "acao";
    public static final String PRECO = "preco";
    public static final String VARIACAO = "variacao";
    public static final String OPCOES_SERIE1 = "opcoesSerie1";
    public static final String OPCOES_SERIE2 = "opcoesSerie2";
    
    private LocalDate dia;
    private Acao acao;
    private Double preco;
    private Double variacao;
    private Collection<Opcao> opcoesSerie1;
    private Collection<Opcao> opcoesSerie2;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public Key toKey() {
	Assert.notNull(dia, "Dia não pode ser nulo");
	Assert.notNull(acao, "Ação não pode ser nula");

	return KeyFactory.createKey(getClass().getName(), //
		DATE_FORMAT.format(dia.toDateTimeAtCurrentTime().toDate()) + acao.getCodigo());
    }
    
    public Entity toEntity() {
	Key key = toKey();
	Entity entity = new Entity(key);
	
	Assert.notNull(acao, "Ação não pode ser nula");
	Assert.notNull(preco, "Preço não pode ser nulo");
	Assert.notNull(variacao, "Variação não pode ser nula");
	
	entity.setProperty(ACAO, acao.ordinal());
	entity.setProperty(PRECO, preco);
	entity.setProperty(VARIACAO, variacao);
	
	List<Key> keysOpcoes1 = Lists.newArrayList();
	if (opcoesSerie1 != null) {
	    for (Opcao opcao : opcoesSerie1) {
		keysOpcoes1.add(opcao.toKey());
	    }
	}
	
	return entity;
    }
}
