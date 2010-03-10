package br.eti.ranieri.opcoesweb.importacao.offline.parser;

public class BovespaParserException extends Exception {

	public BovespaParserException(String mensagem) {
		super(mensagem);
	}
	
	public BovespaParserException(String mensagem, Throwable causa) {
		super(mensagem, causa);
	}
}
