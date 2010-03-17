package br.eti.ranieri.opcoesweb.importacao.online;

import java.io.StringReader;
import java.util.List;

import org.apache.commons.digester.Digester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.eti.ranieri.opcoesweb.importacao.offline.CotacaoBDI;

import com.google.common.collect.Lists;

@Service
public class XmlBanifParser {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private Digester digester;

	public XmlBanifParser() {
		digester = new Digester();
		digester.setValidating(false);
		digester.setUseContextClassLoader(true);

		digester.addObjectCreate("resultados", BanifXmlResultados.class);
		digester.addObjectCreate("resultados/papel", BanifXmlPapel.class);
		digester.addBeanPropertySetter("resultados/papel/is_tipo", "tipo");
		digester.addBeanPropertySetter("resultados/papel/co_ativo_bolsa",
				"codigoNegociacao");
		digester.addBeanPropertySetter("resultados/papel/cotacao",
				"ultimaCotacao");
		digester.addBeanPropertySetter("resultados/papel/is_dt_vencimento",
				"dataVencimento");
		digester.addBeanPropertySetter("resultados/papel/va_oscilacao",
				"oscilacao");
		digester.addBeanPropertySetter("resultados/papel/volume_negociado",
				"volumeFinanceiro");
		digester.addBeanPropertySetter("resultados/papel/va_exercicio",
				"valorExercicio");
		digester.addBeanPropertySetter("resultados/papel/ultimatransacao",
				"ultimaTransacao");
		digester.addSetNext("resultados/papel", "addPapel", BanifXmlPapel.class
				.getName());
	}

	public List<CotacaoBDI> parse(String xml) {

		List<CotacaoBDI> cotacoes = Lists.newArrayList();
		if (xml == null || "".equals(xml))
			return cotacoes;

		try {
			BanifXmlResultados resultados = (BanifXmlResultados) digester
					.parse(new StringReader(xml));
			for (BanifXmlPapel papel : resultados.getPapeis()) {
//				if (papel.isCotacaoDeHoje()) {
				cotacoes.add(papel.getCotacaoBDI());
//				}
			}
			return cotacoes;
		} catch (Exception e) {
			logger.warn("XML [[[\n" + xml + "\n]]] gerou erro de parse");
			logger.error("Erro no parse do XML de cotacoes do banif", e);
		}
		return cotacoes;
	}
}
