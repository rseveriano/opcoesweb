package br.eti.ranieri.opcoesweb.importacao;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TaxaSelic {

	private static final String ANO_INICIAL = "anoInicial";
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final Map<Integer, Double> taxasSelic = new HashMap<Integer, Double>();

	public TaxaSelic() {
	    Properties properties = new Properties();
	    try {
		properties.load(getClass().getResourceAsStream("TaxaSelic.properties"));
	    } catch (IOException e) {
		logger.error("Nao consegui abrir o arquivo TaxaSelic.properties", e);
	    }
	    String anoInicialProperty = properties.getProperty(ANO_INICIAL);
	    if (anoInicialProperty == null) {
		logger.error("Propriedade {} nao definida.", ANO_INICIAL);
	    } else {
		boolean anosDisponiveis = true;
		for (int i = Integer.parseInt(anoInicialProperty); anosDisponiveis; i++) {
		    for (int j = 1; j < 13; j++) {
			String juroProperty = properties.getProperty(String.format("%d%02d", i, j));
			if (juroProperty != null) {
			    taxasSelic.put(j * 10000 + i, Double.parseDouble(juroProperty));
			} else {
			    anosDisponiveis = false;
			    break;
			}
		    }
		}
	    }
	}

	public Double getSelic(LocalDate data) throws IllegalArgumentException {
		if (data == null) {
			throw new IllegalArgumentException(
					"Parametro data deve ser passado");
		}
		Double selic = taxasSelic.get(data.getMonthOfYear() * 10000
				+ data.getYear());
		if (selic == null) {
			logger.error("Taxa SELIC nao cadastrada para data " + data);
			// retorna a ultima taxa
			SortedSet<Integer> datas = new TreeSet<Integer>(new Comparator<Integer>() {
			    public int compare(Integer i1, Integer i2) {
				int ano1 = i1 % 10000;
				int ano2 = i2 % 10000;
				if (ano1 != ano2)
				    return ano1 - ano2;
			        return (i1 / 10000) - (i2 / 10000);
			    }
			});
			datas.addAll(taxasSelic.keySet());
			return taxasSelic.get(datas.last());
		}
		return selic;
	}
}