package br.eti.ranieri.opcoesweb;

import java.util.Map;

import org.apache.wicket.Request;
import org.apache.wicket.protocol.http.WebSession;

import br.eti.ranieri.opcoesweb.estado.Acao;
import br.eti.ranieri.opcoesweb.estado.ConfiguracaoImportacao;
import br.eti.ranieri.opcoesweb.estado.ConfiguracaoOnline;
import br.eti.ranieri.opcoesweb.estado.CotacaoAcaoOpcoes;

public class OpcoesWebHttpSession extends WebSession {

	private final ConfiguracaoOnline configuracaoOnline = new ConfiguracaoOnline();
	private final ConfiguracaoImportacao configuracaoImportacao = new ConfiguracaoImportacao();
	private Map<Acao, CotacaoAcaoOpcoes> cacheCotacoesOnline;

	public OpcoesWebHttpSession(Request request) {
		super(request);
	}

	public static OpcoesWebHttpSession get() {
		return (OpcoesWebHttpSession) WebSession.get();
	}

	public ConfiguracaoOnline getConfiguracaoOnline() {
		return configuracaoOnline;
	}

	public ConfiguracaoImportacao getConfiguracaoImportacao() {
		return configuracaoImportacao;
	}

	public Map<Acao, CotacaoAcaoOpcoes> getCacheCotacoesOnline() {
		return cacheCotacoesOnline;
	}

	public void setCacheCotacoesOnline(Map<Acao, CotacaoAcaoOpcoes> cacheCotacoesOnline) {
		this.cacheCotacoesOnline = cacheCotacoesOnline;
	}
}
