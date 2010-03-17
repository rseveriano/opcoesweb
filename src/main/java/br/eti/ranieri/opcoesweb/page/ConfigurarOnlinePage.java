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

import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.CompoundPropertyModel;

import br.eti.ranieri.opcoesweb.OpcoesWebHttpSession;
import br.eti.ranieri.opcoesweb.estado.ConfiguracaoOnline;

/**
 * 
 * @author ranieri
 */
public class ConfigurarOnlinePage extends PaginaBase {

	public ConfigurarOnlinePage() {
		add(new FeedbackPanel("feedback"));

		final ConfiguracaoOnline configuracao = OpcoesWebHttpSession.get().getConfiguracaoOnline();
		Form<ConfiguracaoOnline> formulario = new Form<ConfiguracaoOnline>("formulario",
				new CompoundPropertyModel<ConfiguracaoOnline>(configuracao)) {

			@Override
			protected void onSubmit() {
				setResponsePage(ExibirOnlinePage.class);
			}
		};
		add(formulario);

		formulario.add(new RequiredTextField<String>("jsessionid"));
		formulario.add(new Button("submeter"));
	}
}
