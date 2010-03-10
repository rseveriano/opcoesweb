package br.eti.ranieri.opcoesweb.importacao.online;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.eti.ranieri.opcoesweb.importacao.offline.CodigoBDI;
import br.eti.ranieri.opcoesweb.importacao.offline.CotacaoBDI;
import br.eti.ranieri.opcoesweb.importacao.offline.TipoMercadoBDI;

public class BanifXmlPapel {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private CotacaoBDI cotacao = new CotacaoBDI();
	private boolean cotacaoDeHoje = false;

	public CotacaoBDI getCotacaoBDI() {
		return cotacao;
	}

	public boolean isCotacaoDeHoje() {
		return cotacaoDeHoje;
	}

	public void setTipo(String tipo) {
		if ("ACAO".equals(tipo)) {
			cotacao.setCodigoBdi(CodigoBDI.LOTE_PADRAO);
			cotacao.setTipoMercado(TipoMercadoBDI.MERCADO_A_VISTA);
		} else if ("OPCAO".equals(tipo)) {
			cotacao.setCodigoBdi(CodigoBDI.OPCOES_DE_COMPRA);
			cotacao.setTipoMercado(TipoMercadoBDI.OPCOES_DE_COMPRA);
		}
	}

	public void setCodigoNegociacao(String codigo) {
		if (StringUtils.isNotBlank(codigo))
			cotacao.setCodigoNegociacao(codigo);
	}

	public void setUltimaCotacao(String ultimaCotacao) {
		if (StringUtils.isNotBlank(ultimaCotacao))
			cotacao.setFechamento(parseString(ultimaCotacao, false));
	}

	public void setDataVencimento(String data) {
		// Converte para "yyMMdd", uma vez que o BDI tb usa esse formato
		if (StringUtils.length(data) == 10) {
			cotacao.setDataVencimento(Long.parseLong(data.substring(8, 10)
					+ data.substring(3, 5) + data.substring(0, 2)));
		}
	}

	public void setOscilacao(String oscilacao) {
		if (StringUtils.isNotBlank(oscilacao))
			cotacao.setOscilacao(parseString(oscilacao, true));
	}

	public void setVolumeFinanceiro(String volume) {
		if (StringUtils.isNotBlank(volume))
			cotacao.setVolume(parseString(volume, false));
	}

	public void setValorExercicio(String valor) {
		if (StringUtils.isNotBlank(valor))
			cotacao.setPrecoExercicio(parseString(valor, false));
	}

	public void setUltimaTransacao(String valor) {
		if (StringUtils.isNotBlank(valor)) {
			DateTimeFormatter parser = DateTimeFormat
					.forPattern("dd/MM/yyyy' - 'HH:mm:ss");
			DateTime data;
			try {
				data = parser.parseDateTime(valor);
			} catch (IllegalArgumentException e) {
				logger.error("Erro no parse da data: " + valor, e);
				return;
			}
			DateTime hoje = new DateTime();
			if (data.getDayOfYear() == hoje.getDayOfYear()
					&& data.getYear() == hoje.getYear()) {
				cotacaoDeHoje = true;
			}
		}
	}

	private long parseString(String valor, boolean incluirSinal) {
		StringBuilder buf = new StringBuilder();
		for (char c : valor.toCharArray()) {
			if (Character.isDigit(c))
				buf.append(c);
			else if (incluirSinal && c == '-')
				buf.append(c);
		}
		if (buf.length() == 0 || "-".equals(buf.toString()))
			return 0;
		return Long.parseLong(buf.toString());
	}
}
