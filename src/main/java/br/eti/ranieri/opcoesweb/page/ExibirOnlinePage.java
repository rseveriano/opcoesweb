/*
 *  Copyright 2009 ranieri.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package br.eti.ranieri.opcoesweb.page;

import static br.eti.ranieri.opcoesweb.estado.Acao.PETROBRAS;
import static br.eti.ranieri.opcoesweb.estado.Acao.VALE;

import java.util.Map;

import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.joda.time.DateTime;

import br.eti.ranieri.opcoesweb.estado.Acao;
import br.eti.ranieri.opcoesweb.estado.CotacaoAcaoOpcoes;
import br.eti.ranieri.opcoesweb.importacao.online.ImportadorOnline;

/**
 * 
 * @author ranieri
 */
public class ExibirOnlinePage extends PaginaBase {

	@SpringBean(name = "importadorOnline")
	private ImportadorOnline importador;

	public ExibirOnlinePage() {
		add(new FeedbackPanel("feedback"));
		add(new Link("atualizar") {

			@Override
			public void onClick() {
				importar();
			}
		});

		boolean configurado = getSessaoHttp().getConfiguracaoOnline()
				.isConfigurado();
		if (configurado == false) {
			info("Para exibir cotações online, é necessária configuração.");
			setResponsePage(ConfigurarOnlinePage.class);
		} else {
			Map<Acao, CotacaoAcaoOpcoes> cotacoesOnline = getSessaoHttp()
					.getCacheCotacoesOnline();
			add(new PainelAcaoOpcoes("tabelaPetrobras", cotacoesOnline == null ? null : cotacoesOnline
					.get(PETROBRAS), new DateTime()));
			add(new PainelAcaoOpcoes("tabelaVale", cotacoesOnline == null ? null : cotacoesOnline.get(VALE), new DateTime()));
		}
	}

	private void importar() {
		Map<Acao, CotacaoAcaoOpcoes> cotacoesOnline = importador
				.importar(getSessaoHttp().getConfiguracaoOnline());
		getSessaoHttp().setCacheCotacoesOnline(cotacoesOnline);

		DateTime agora = new DateTime();
		addOrReplace(new PainelAcaoOpcoes("tabelaPetrobras", cotacoesOnline
				.get(PETROBRAS), agora));

		addOrReplace(new PainelAcaoOpcoes("tabelaVale", cotacoesOnline
				.get(VALE), agora));
	}

}
