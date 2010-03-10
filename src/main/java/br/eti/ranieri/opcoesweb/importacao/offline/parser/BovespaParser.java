package br.eti.ranieri.opcoesweb.importacao.offline.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.eti.ranieri.opcoesweb.importacao.offline.CodigoBDI;
import br.eti.ranieri.opcoesweb.importacao.offline.CotacaoBDI;
import br.eti.ranieri.opcoesweb.importacao.offline.TipoMercadoBDI;

@Service("bovespaParser")
public class BovespaParser {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private FiltroDadosBovespa filtro;

	public List<CotacaoBDI> parseBDI(InputStream stream) throws BovespaParserException {

		List<CotacaoBDI> cotacoes = new ArrayList<CotacaoBDI>();
		LocalDate dataPregao = null;

		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					stream, "ISO-8859-1"));

			String linha = reader.readLine();
			while (linha != null) {

				if (linha.length() != 350) {
					if (logger.isWarnEnabled()) {
						logger.warn("Registro abaixo com tamanho "
								+ linha.length() + ", "
								+ "diferente dos 350 caracteres esperados:\n["
								+ linha + "]");
					}
					throw new BovespaParserException(
							"Registro do arquivo Bovespa com tamanho diferente de 350");
				}

				if (linha.startsWith("00")) {
					dataPregao = parseHeader(linha);
				} else if (linha.startsWith("02")) {
					CotacaoBDI cotacao = parseAcao(dataPregao, linha);
					if (cotacao != null)
						cotacoes.add(cotacao);
				}

				linha = reader.readLine();

			}

		} catch (UnsupportedEncodingException e) {
			throw new BovespaParserException(
					"Codificacao incorreta para arquivo Bovespa", e);
		} catch (IOException e) {
			throw new BovespaParserException(
					"Erro de leitura do arquivo Bovespa", e);
		}
		return cotacoes;
	}

	public List<CotacaoBDI> parseHistorico(InputStream stream) throws BovespaParserException {
		
		List<CotacaoBDI> cotacoes = new ArrayList<CotacaoBDI>();
		
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "ISO-8859-1"));
			
			String linha = reader.readLine();
			while (linha != null) {
				
				if (linha.length() != 245) {
					logger.warn("Registro abaixo com tamanho {}, diferente dos 245 caracteres esperados:\n[{}]", linha.length(), linha);
					throw new BovespaParserException("Registro do arquivo Bovespa com tamanho diferente de 245");
				}
				
				if (linha.startsWith("01")) {
					CotacaoBDI cotacao = parseAcao(linha);
					if (cotacao != null)
						cotacoes.add(cotacao);
				}
				
				linha = reader.readLine();
			}
		} catch (UnsupportedEncodingException e) {
			throw new BovespaParserException("Codificacao incorreta para arquivo Bovespa", e);
		} catch (IOException e) {
			throw new BovespaParserException(
					"Erro de leitura do arquivo Bovespa", e);
		}
		return cotacoes;
	}

	private LocalDate parseDateTime(String yyyyMMdd) {
		DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyyMMdd");
		formatter.withLocale(new Locale("pt", "BR")).withZone(
				DateTimeZone.forID("Brazil/East"));
		return formatter.parseDateTime(yyyyMMdd).toLocalDate();
	}

	private LocalDate parseHeader(String linha) {
		String dataPregao = linha.substring(30, 38);
		return parseDateTime(dataPregao);
	}

	private CotacaoBDI parseAcao(String linha) {
		LocalDate dataPregao = parseDateTime(linha.substring(2, 10));
		CodigoBDI codigoBDI = CodigoBDI.getPorId(linha.substring(10, 12));
		// Codigo utilizado para comprar: PETR4
		String codigoNegociacao = linha.substring(12, 24).trim();
		TipoMercadoBDI tipoMercado = TipoMercadoBDI.getPorCodigo(Integer.parseInt(linha.substring(24, 27)));

		if (filtro.possoIncluirAtivo(codigoBDI, tipoMercado, codigoNegociacao)) {
			return popularHistorico(dataPregao, codigoBDI, tipoMercado, codigoNegociacao, linha);
		} else {
			logger.debug("Ignorando ativo {}.", codigoNegociacao);
			return null;
		}
	}

	private CotacaoBDI parseAcao(LocalDate dataPregao, String linha) {
		CodigoBDI codigoBdi = CodigoBDI.getPorId(linha.substring(2, 4));
		TipoMercadoBDI tipoMercado = TipoMercadoBDI.getPorCodigo(Integer.parseInt(linha
				.substring(69, 72)));
		// Codigo utilizado para comprar: PETR4
		String codigoNegociacao = linha.substring(57, 69).trim();

		if (filtro.possoIncluirAtivo(codigoBdi, tipoMercado, codigoNegociacao)) {

			return popularBDI(dataPregao, codigoBdi, tipoMercado, codigoNegociacao,
					linha);

		} else {
			if (logger.isDebugEnabled())
				logger.debug("Ignorando ativo " + codigoNegociacao);
			return null;
		}
	}

	private CotacaoBDI popularBDI(LocalDate dataPregao, CodigoBDI codigoBdi,
			TipoMercadoBDI tipoMercado, String codigoNegociacao,
			String linha) {

		long fechamento = Long.parseLong(linha.substring(134, 145));
		long volume = Long.parseLong(linha.substring(193, 210));
		char sinalOscilacao = linha.substring(145, 146).charAt(0);
		long oscilacao = 0;
		if (sinalOscilacao == '+') {
			oscilacao = Long.parseLong(linha.substring(146,151));
		} else if (sinalOscilacao == '-') {
			oscilacao = - Long.parseLong(linha.substring(146,151));
		} else if (sinalOscilacao == '=') {
			oscilacao = Long.parseLong(linha.substring(146,151));
		}
		// Para opções
		long precoExercicio = Long.parseLong(linha.substring(210, 221));
		// yyyyMMdd
		long dataVencimento = Long.parseLong(linha.substring(221, 229));

		return new CotacaoBDI(dataPregao, codigoBdi, tipoMercado, codigoNegociacao, fechamento, volume,
				oscilacao, precoExercicio, dataVencimento);
	}

	private CotacaoBDI popularHistorico(LocalDate dataPregao, CodigoBDI codigoBdi,
			TipoMercadoBDI tipoMercado, String codigoNegociacao,
			String linha) {
		
		long fechamento = Long.parseLong(linha.substring(108, 121));
		long volume = Long.parseLong(linha.substring(170, 188));
		long oscilacao = 0;
		// Para opções
		long precoExercicio = Long.parseLong(linha.substring(188, 201));
		// yyyyMMdd
		long dataVencimento = Long.parseLong(linha.substring(202, 210));

		return new CotacaoBDI(dataPregao, codigoBdi, tipoMercado, codigoNegociacao, fechamento, volume,
				oscilacao, precoExercicio, dataVencimento);
	}
}
