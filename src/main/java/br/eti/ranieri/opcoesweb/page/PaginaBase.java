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

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;

import br.eti.ranieri.opcoesweb.OpcoesWebHttpSession;

/**
 *
 * @author ranieri
 */
public abstract class PaginaBase extends WebPage {
    public PaginaBase() {
        add(new BookmarkablePageLink("configurar", ConfigurarOnlinePage.class));
        add(new BookmarkablePageLink("exibirOnline", ExibirOnlinePage.class));
        add(new BookmarkablePageLink("importadorSerieHistorica", ImportarSerieHistoricaPage.class));
        add(new BookmarkablePageLink("exibirOffline", ExibirOfflinePage.class));
        add(new BookmarkablePageLink("calcularBlackScholesAdhoc", CalcularBlackScholesAdhocPage.class));
        add(new BookmarkablePageLink("wizardSimulacao", Wizard1SimulacaoPage.class));
    }
    
    public OpcoesWebHttpSession getSessaoHttp() {
    	return (OpcoesWebHttpSession) getSession();
    }
}
